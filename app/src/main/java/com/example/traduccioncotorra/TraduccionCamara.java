package com.example.traduccioncotorra;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.appcompat.widget.AppCompatSpinner;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.traduccioncotorra.TraduccionOverlayView;
import com.example.traduccioncotorra.TraduccionOverlayView.TranslationBox;
import com.example.traduccioncotorra.DB.LanguageDAO;
import com.example.traduccioncotorra.DB.HistorialDAO;
import com.example.traduccioncotorra.DB.UserDAO;
import com.example.traduccioncotorra.DB.TranslationTypeDAO;
import com.example.traduccioncotorra.DB.FavoriteTranslationDAO;
import com.google.android.material.button.MaterialButton;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.common.model.DownloadConditions;
import com.google.mlkit.nl.languageid.LanguageIdentification;
import com.google.mlkit.nl.languageid.LanguageIdentifier;
import com.google.mlkit.nl.translate.Translation;
import com.google.mlkit.nl.translate.Translator;
import com.google.mlkit.nl.translate.TranslatorOptions;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TraduccionCamara extends Fragment {

    private static final String TAG = "TRADUCCION_CAMARA";
    private static final int CAMERA_PERMISSION_REQUEST = 100;
    private static final long ANALYSIS_DELAY_MS = 1500; // Analizar cada 1.5 segundos

    // ========== VISTAS ==========
    private RelativeLayout translationLayout;
    private PreviewView previewView;
    private TraduccionOverlayView overlayView;
    private AppCompatSpinner spinnerTargetLanguage;
    private MaterialButton btnToggleAnalysis;
    private ImageButton menuButtonConfig;

    // ========== DAOs ==========
    private LanguageDAO languageDAO;
    private HistorialDAO historialDAO;
    private UserDAO userDAO;
    private TranslationTypeDAO translationTypeDAO;

    // ========== ESTADO ==========
    private int userId = -1;
    private int translationTypeId = 2;
    private boolean isAnalyzing = false;
    private long lastAnalysisTime = 0;

    // ========== ML KIT Y CÁMARA ==========
    private TextRecognizer textRecognizer;
    private LanguageIdentifier languageIdentifier;
    private Translator translator;
    private ExecutorService cameraExecutor;
    private ProcessCameraProvider cameraProvider;
    private Handler mainHandler;

    // ========== IDIOMAS ==========
    private List<LanguageDAO.Language> idiomasDisponibles;
    private String targetLanguageApiCode = "es";
    private boolean modeloDescargado = false;

    // ========== DIMENSIONES DE IMAGEN ==========
    private int imageWidth = 0;
    private int imageHeight = 0;

    // Cache de traducciones para evitar repeticiones
    private Map<String, String> translationCache = new HashMap<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_traduccion_camara_overlay, container, false);

        mainHandler = new Handler(Looper.getMainLooper());

        inicializarVistas(view);
        crearVistaCamera();
        inicializarDAOs();
        inicializarMLKit();
        cargarIdiomasDesdeDB();
        configurarSpinner();
        configurarListeners();
        verificarPermisosCamara();

        return view;
    }

    private void inicializarVistas(View view) {
        translationLayout = view.findViewById(R.id.translation_layout);
        spinnerTargetLanguage = view.findViewById(R.id.spinner_target_language);
        btnToggleAnalysis = view.findViewById(R.id.btn_toggle_analysis);
        menuButtonConfig = view.findViewById(R.id.menu_button_config);
    }

    private void crearVistaCamera() {
        // PreviewView para la cámara
        previewView = new PreviewView(requireContext());
        RelativeLayout.LayoutParams previewParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.MATCH_PARENT
        );
        previewView.setLayoutParams(previewParams);

        // Overlay personalizado para dibujar traducciones
        overlayView = new TraduccionOverlayView(requireContext());
        RelativeLayout.LayoutParams overlayParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.MATCH_PARENT
        );
        overlayView.setLayoutParams(overlayParams);

        // ✅ Configurar callback para limpiar cache cuando se mueve el recuadro
        overlayView.setOnBoxChangedListener(this::limpiarCacheTraduccion);

        // Agregar vistas (orden: PreviewView primero, Overlay encima)
        translationLayout.addView(previewView, 0);
        translationLayout.addView(overlayView, 1);

        Log.d(TAG, "✅ Vista de cámara con overlay creada");
    }

    private void inicializarDAOs() {
        languageDAO = new LanguageDAO(requireContext());
        historialDAO = new HistorialDAO(requireContext());
        userDAO = new UserDAO(requireContext());
        translationTypeDAO = new TranslationTypeDAO(requireContext());

        userId = userDAO.obtenerUserIdActual(requireContext());

        new Thread(() -> {
            TranslationTypeDAO.TranslationType type = translationTypeDAO.obtenerTipoPorNombre("Cámara");
            if (type != null) {
                translationTypeId = type.idTypeTranslation;
            }
            Log.d(TAG, "✅ User: " + userId + ", Type: " + translationTypeId);
        }).start();
    }

    private void inicializarMLKit() {
        textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
        languageIdentifier = LanguageIdentification.getClient();
        cameraExecutor = Executors.newSingleThreadExecutor();
        Log.d(TAG, "✅ ML Kit inicializado");
    }

    private void cargarIdiomasDesdeDB() {
        idiomasDisponibles = languageDAO.obtenerIdiomasActivos();

        if (idiomasDisponibles.isEmpty()) {
            Toast.makeText(getContext(),
                    "⚠️ No hay idiomas configurados",
                    Toast.LENGTH_LONG).show();
        } else {
            Log.d(TAG, "✅ Idiomas cargados: " + idiomasDisponibles.size());
        }
    }

    private void configurarSpinner() {
        if (idiomasDisponibles.isEmpty()) {
            spinnerTargetLanguage.setEnabled(false);
            return;
        }

        String[] nombresIdiomas = new String[idiomasDisponibles.size()];
        for (int i = 0; i < idiomasDisponibles.size(); i++) {
            nombresIdiomas[i] = idiomasDisponibles.get(i).name;
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, nombresIdiomas);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTargetLanguage.setAdapter(adapter);

        // Buscar "Español" por defecto
        for (int i = 0; i < idiomasDisponibles.size(); i++) {
            if (idiomasDisponibles.get(i).name.equalsIgnoreCase("Español")) {
                spinnerTargetLanguage.setSelection(i);
                targetLanguageApiCode = idiomasDisponibles.get(i).apiCode;
                break;
            }
        }

        // Configurar traductor inicial
        configurarTraductor(targetLanguageApiCode);

        spinnerTargetLanguage.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                targetLanguageApiCode = idiomasDisponibles.get(position).apiCode;
                Log.d(TAG, "🔄 Idioma destino: " + idiomasDisponibles.get(position).name);

                // Reconfigurar traductor
                configurarTraductor(targetLanguageApiCode);

                // Limpiar cache y overlay
                limpiarCacheTraduccion();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void configurarTraductor(String targetLanguageCode) {
        // Solo cerrar el traductor anterior si existe
        if (translator != null) {
            translator.close();
            translator = null;
        }

        modeloDescargado = true; // Ya no necesitamos pre-descargar

        Log.d(TAG, "✅ Traductor configurado para destino: " + targetLanguageCode);
        Toast.makeText(requireContext(),
                "✅ Listo para traducir a " + obtenerNombreIdioma(targetLanguageCode),
                Toast.LENGTH_SHORT).show();
    }

    private void configurarListeners() {
        btnToggleAnalysis.setOnClickListener(v -> {
            isAnalyzing = !isAnalyzing;

            if (isAnalyzing) {
                btnToggleAnalysis.setText("⏸️ Pausar");
                btnToggleAnalysis.setBackgroundColor(
                        ContextCompat.getColor(requireContext(), R.color.green_menu)
                );
                Toast.makeText(requireContext(),
                        "📸 Traducción activada\nDetección automática de idioma",
                        Toast.LENGTH_SHORT).show();
            } else {
                btnToggleAnalysis.setText("▶️ Iniciar Traducción");
                btnToggleAnalysis.setBackgroundColor(
                        ContextCompat.getColor(requireContext(), R.color.orange_menu)
                );
                overlayView.clear();
                Toast.makeText(requireContext(),
                        "⏸️ Traducción pausada",
                        Toast.LENGTH_SHORT).show();
            }
        });

        menuButtonConfig.setOnClickListener(v -> abrirConfiguracion());
    }

    private void verificarPermisosCamara() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(),
                    new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST);
        } else {
            iniciarCamara();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == CAMERA_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                iniciarCamara();
            } else {
                Toast.makeText(requireContext(),
                        "❌ Se necesita permiso de cámara",
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    private void iniciarCamara() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(requireContext());

        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();
                vincularCasoDeUsoDeCamara();
            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "❌ Error cámara: " + e.getMessage());
            }
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    private void vincularCasoDeUsoDeCamara() {
        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        imageAnalysis.setAnalyzer(cameraExecutor, this::analizarImagen);

        CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

        try {
            cameraProvider.unbindAll();
            cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);
            Log.d(TAG, "✅ Cámara vinculada con análisis continuo");
        } catch (Exception e) {
            Log.e(TAG, "❌ Error vincular: " + e.getMessage());
        }
    }

    /**
     * ⭐ ANÁLISIS CONTINUO DE CADA FRAME
     */
    @OptIn(markerClass = ExperimentalGetImage.class)
    private void analizarImagen(@NonNull ImageProxy imageProxy) {
        if (!isAnalyzing || !modeloDescargado) {
            imageProxy.close();
            return;
        }

        // Limitar frecuencia de análisis
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastAnalysisTime < ANALYSIS_DELAY_MS) {
            imageProxy.close();
            return;
        }
        lastAnalysisTime = currentTime;

        if (imageProxy.getImage() == null) {
            imageProxy.close();
            return;
        }

        // ✅ GUARDAR dimensiones reales de la imagen
        imageWidth = imageProxy.getWidth();
        imageHeight = imageProxy.getHeight();

        InputImage image = InputImage.fromMediaImage(
                imageProxy.getImage(),
                imageProxy.getImageInfo().getRotationDegrees()
        );

        // OCR
        textRecognizer.process(image)
                .addOnSuccessListener(visionText -> {
                    procesarTextoDetectado(visionText);
                    imageProxy.close();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Error OCR: " + e.getMessage());
                    imageProxy.close();
                });
    }

    /**
     * ⭐ PROCESAR SOLO TEXTO COMPLETAMENTE DENTRO DEL RECUADRO (CORREGIDO)
     */
    private void procesarTextoDetectado(Text visionText) {
        List<Text.TextBlock> blocks = visionText.getTextBlocks();

        if (blocks.isEmpty()) {
            mainHandler.post(() -> overlayView.clear());
            return;
        }

        // ✅ Obtener el recuadro de captura en coordenadas de imagen
        Rect captureRect = overlayView.getCaptureBoxInImageCoords();

        Log.d(TAG, "📦 Recuadro captura: " + captureRect.toString());

        overlayView.setImageDimensions(imageWidth, imageHeight);

        List<Text.TextBlock> blocksWithinBox = new ArrayList<>();
        StringBuilder textoCompleto = new StringBuilder();

        // ✅ PASO 1: Filtrar bloques que están COMPLETAMENTE dentro del recuadro
        for (Text.TextBlock block : blocks) {
            String originalText = block.getText();
            Rect boundingBox = block.getBoundingBox();

            if (originalText.isEmpty() || boundingBox == null) {
                continue;
            }

            // ✅ VERIFICAR QUE EL BLOQUE ESTÉ COMPLETAMENTE DENTRO DEL RECUADRO
            if (isRectCompletelyInside(boundingBox, captureRect)) {
                blocksWithinBox.add(block);

                if (textoCompleto.length() > 0) {
                    textoCompleto.append(" ");
                }
                textoCompleto.append(originalText);

                Log.d(TAG, "✅ Texto dentro del recuadro: " + originalText);
            } else {
                Log.d(TAG, "⏭️ Texto ignorado (fuera del recuadro): " + originalText);
            }
        }

        // ✅ PASO 2: Procesar solo si hay texto válido
        if (textoCompleto.length() > 0) {
            String textoFinal = textoCompleto.toString().trim();

            // Verificar cache
            if (translationCache.containsKey(textoFinal)) {
                String cachedTranslation = translationCache.get(textoFinal);

                List<TranslationBox> translationBoxes = new ArrayList<>();
                translationBoxes.add(new TranslationBox(
                        captureRect, textoFinal, cachedTranslation
                ));

                mainHandler.post(() -> overlayView.setTranslationBoxes(translationBoxes));

                Log.d(TAG, "💾 Usando traducción en cache");
            } else {
                // Detectar idioma y traducir
                List<TranslationBox> translationBoxes = new ArrayList<>();
                detectarIdiomaYTraducir(textoFinal, captureRect, translationBoxes);
            }
        } else {
            // No hay texto dentro del recuadro
            mainHandler.post(() -> overlayView.clear());
            Log.d(TAG, "📭 No hay texto dentro del recuadro");
        }
    }

    /**
     * ✅ NUEVO: Verificar que un rectángulo esté COMPLETAMENTE dentro de otro
     */
    private boolean isRectCompletelyInside(Rect inner, Rect outer) {
        // Agregar un margen de 10 píxeles para ser más estricto
        int margin = 10;

        Rect strictOuter = new Rect(
                outer.left + margin,
                outer.top + margin,
                outer.right - margin,
                outer.bottom - margin
        );

        // El rectángulo interior debe estar completamente dentro del exterior
        return strictOuter.contains(inner.left, inner.top) &&
                strictOuter.contains(inner.right, inner.bottom);
    }

    /**
     * ⭐ DETECTAR IDIOMA Y TRADUCIR (MEJORADO)
     */
    private void detectarIdiomaYTraducir(String texto, Rect boundingBox,
                                         List<TranslationBox> boxes) {
        languageIdentifier.identifyLanguage(texto)
                .addOnSuccessListener(languageCode -> {
                    if (languageCode.equals("und")) {
                        Log.w(TAG, "⚠️ Idioma no detectado para: " + texto);

                        // Mostrar mensaje en el overlay
                        boxes.clear();
                        boxes.add(new TranslationBox(
                                boundingBox, texto, "⚠️ No se pudo detectar el idioma"
                        ));
                        mainHandler.post(() -> overlayView.setTranslationBoxes(new ArrayList<>(boxes)));
                        return;
                    }

                    Log.d(TAG, "🌍 Idioma detectado: " + languageCode + " (" + texto + ")");

                    // Si el idioma detectado es el mismo que el destino, no traducir
                    if (languageCode.equals(targetLanguageApiCode)) {
                        Log.d(TAG, "⏭️ Texto ya está en idioma destino");

                        boxes.clear();
                        boxes.add(new TranslationBox(
                                boundingBox, texto, "✓ Ya está en " + obtenerNombreIdioma(targetLanguageApiCode)
                        ));
                        mainHandler.post(() -> overlayView.setTranslationBoxes(new ArrayList<>(boxes)));
                        return;
                    }

                    // Traducir con el idioma detectado
                    traducirConIdiomaOrigen(texto, languageCode, boundingBox, boxes);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Error detectando idioma: " + e.getMessage());

                    // Mostrar error en el overlay
                    boxes.clear();
                    boxes.add(new TranslationBox(
                            boundingBox, texto, "❌ Error al detectar idioma"
                    ));
                    mainHandler.post(() -> overlayView.setTranslationBoxes(new ArrayList<>(boxes)));
                });
    }

    /**
     * ⭐ TRADUCIR CON IDIOMA DE ORIGEN ESPECÍFICO (MEJORADO CON MEJOR MANEJO DE ERRORES)
     */
    private void traducirConIdiomaOrigen(String texto, String sourceLanguage,
                                         Rect boundingBox,
                                         List<TranslationBox> boxes) {
        // Crear traductor temporal para este par de idiomas
        TranslatorOptions options = new TranslatorOptions.Builder()
                .setSourceLanguage(sourceLanguage)
                .setTargetLanguage(targetLanguageApiCode)
                .build();

        Translator tempTranslator = Translation.getClient(options);

        DownloadConditions conditions = new DownloadConditions.Builder()
                .requireWifi()
                .build();

        Log.d(TAG, "🔄 Iniciando traducción: " + sourceLanguage + " → " + targetLanguageApiCode);

        tempTranslator.downloadModelIfNeeded(conditions)
                .addOnSuccessListener(unused -> {
                    Log.d(TAG, "✅ Modelo descargado/disponible");

                    // Traducir
                    tempTranslator.translate(texto)
                            .addOnSuccessListener(translatedText -> {
                                Log.d(TAG, "✅ Traducción exitosa: " + texto + " → " + translatedText);

                                // Guardar en cache
                                translationCache.put(texto, translatedText);

                                // ✅ LIMPIAR lista antes de agregar nueva traducción
                                boxes.clear();

                                // Agregar la traducción
                                boxes.add(new TranslationBox(
                                        boundingBox, texto, translatedText
                                ));

                                // ✅ Actualizar UI en el hilo principal
                                mainHandler.post(() -> {
                                    overlayView.setTranslationBoxes(new ArrayList<>(boxes));
                                });

                                // Guardar en historial
                                guardarEnHistorial(texto, translatedText, sourceLanguage);

                                // Cerrar traductor temporal
                                tempTranslator.close();
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "❌ Error en traducción: " + e.getMessage());

                                boxes.clear();
                                boxes.add(new TranslationBox(
                                        boundingBox, texto, "❌ Error al traducir"
                                ));
                                mainHandler.post(() -> overlayView.setTranslationBoxes(new ArrayList<>(boxes)));

                                tempTranslator.close();
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Modelo no disponible: " + sourceLanguage + "→" + targetLanguageApiCode);
                    Log.e(TAG, "❌ Detalles: " + e.getMessage());

                    boxes.clear();
                    boxes.add(new TranslationBox(
                            boundingBox,
                            texto,
                            "⚠️ Conecta a WiFi para descargar modelo\n" +
                                    sourceLanguage + " → " + targetLanguageApiCode
                    ));

                    mainHandler.post(() -> {
                        overlayView.setTranslationBoxes(new ArrayList<>(boxes));

                        Toast.makeText(requireContext(),
                                "❌ Modelo " + sourceLanguage + "→" + targetLanguageApiCode + " no disponible",
                                Toast.LENGTH_SHORT).show();
                    });

                    tempTranslator.close();
                });
    }

    /**
     * ⭐ GUARDAR EN HISTORIAL CON IDIOMA DE ORIGEN DETECTADO
     */
    private void guardarEnHistorial(String textoOriginal, String textoTraducido, String sourceLanguageCode) {
        if (userId == -1 || textoOriginal.isEmpty() || textoTraducido.isEmpty()) {
            return;
        }

        new Thread(() -> {
            LanguageDAO.Language sourceLanguage = languageDAO.obtenerIdiomaPorApiCode(sourceLanguageCode);
            LanguageDAO.Language targetLanguage = languageDAO.obtenerIdiomaPorApiCode(targetLanguageApiCode);

            if (sourceLanguage == null || targetLanguage == null) {
                Log.w(TAG, "⚠️ Idioma no encontrado en DB: " + sourceLanguageCode);
                return;
            }

            long result = historialDAO.insertarHistorialSiNoExiste(
                    userId,
                    sourceLanguage.languageId,
                    targetLanguage.languageId,
                    translationTypeId,
                    textoOriginal,
                    textoTraducido
            );

            Log.d(TAG, result > 0 ? "💾 Historial: " + result : "⚠️ Duplicado");
        }).start();
    }

    /**
     * ✅ NUEVO: Obtener nombre legible del idioma
     */
    private String obtenerNombreIdioma(String apiCode) {
        for (LanguageDAO.Language lang : idiomasDisponibles) {
            if (lang.apiCode.equals(apiCode)) {
                return lang.name;
            }
        }
        return apiCode.toUpperCase();
    }

    /**
     * ✅ NUEVO: Limpiar cache cuando se mueve el recuadro
     */
    public void limpiarCacheTraduccion() {
        translationCache.clear();
        overlayView.clear();
        Log.d(TAG, "🧹 Cache limpiado");
    }

    private void abrirConfiguracion() {
        Configuracion configuracionFragment = new Configuracion();
        getParentFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, configuracionFragment)
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (cameraExecutor != null) cameraExecutor.shutdown();
        if (textRecognizer != null) textRecognizer.close();
        if (languageIdentifier != null) languageIdentifier.close();
        if (translator != null) translator.close();
        if (cameraProvider != null) cameraProvider.unbindAll();

        translationCache.clear();

        Log.d(TAG, "🛑 Recursos liberados");
    }
}