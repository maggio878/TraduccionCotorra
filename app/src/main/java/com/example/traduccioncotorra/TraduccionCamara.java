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

    // ✅ OPTIMIZADO: Delay ajustado para mejor estabilidad
    private static final long ANALYSIS_DELAY_MS = 800; // Ajustado a 800ms

    // ✅ Contador para forzar análisis periódico
    private int analysisCounter = 0;
    private static final int FORCE_ANALYSIS_EVERY = 4; // Cada 4 frames

    // ✅ NUEVO: Variables para estabilización
    private String ultimoTextoDetectado = "";
    private String ultimaTraduccion = "";
    private long ultimoCambioTexto = 0;
    private static final long TEXTO_ESTABLE_MS = 2000; // Mantener traducción por 2 segundos

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
        // ✅ Usar reconocedor universal en lugar de solo Latin
        textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
        languageIdentifier = LanguageIdentification.getClient();
        cameraExecutor = Executors.newSingleThreadExecutor();
        Log.d(TAG, "✅ ML Kit inicializado con reconocimiento universal");
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
                overlayView.setAnalyzing(false);
                limpiarCacheTraduccion();
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
     * ⭐ ANÁLISIS CONTINUO OPTIMIZADO
     */
    @OptIn(markerClass = ExperimentalGetImage.class)
    private void analizarImagen(@NonNull ImageProxy imageProxy) {
        if (!isAnalyzing || !modeloDescargado) {
            imageProxy.close();
            return;
        }

        // ✅ Sistema de análisis más inteligente
        long currentTime = System.currentTimeMillis();
        analysisCounter++;

        // Analizar cada 800ms O forzar cada 4 frames
        boolean shouldAnalyze = (currentTime - lastAnalysisTime >= ANALYSIS_DELAY_MS) ||
                (analysisCounter >= FORCE_ANALYSIS_EVERY);

        if (!shouldAnalyze) {
            imageProxy.close();
            return;
        }

        // Resetear contador y tiempo
        lastAnalysisTime = currentTime;
        analysisCounter = 0;

        if (imageProxy.getImage() == null) {
            imageProxy.close();
            return;
        }

        // ✅ GUARDAR dimensiones reales de la imagen
        imageWidth = imageProxy.getWidth();
        imageHeight = imageProxy.getHeight();

        // ✅ Indicar visualmente que está analizando
        mainHandler.post(() -> overlayView.setAnalyzing(true));

        InputImage image = InputImage.fromMediaImage(
                imageProxy.getImage(),
                imageProxy.getImageInfo().getRotationDegrees()
        );

        // OCR
        textRecognizer.process(image)
                .addOnSuccessListener(visionText -> {
                    procesarTextoDetectado(visionText);
                    mainHandler.post(() -> overlayView.setAnalyzing(false));
                    imageProxy.close();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Error OCR: " + e.getMessage());
                    mainHandler.post(() -> overlayView.setAnalyzing(false));
                    imageProxy.close();
                });
    }

    /**
     * ⭐ PROCESAR TEXTO CON ESTABILIZACIÓN (CORREGIDO)
     */
    private void procesarTextoDetectado(Text visionText) {
        List<Text.TextBlock> blocks = visionText.getTextBlocks();

        if (blocks.isEmpty()) {
            // Solo limpiar si ha pasado tiempo suficiente
            long tiempoSinTexto = System.currentTimeMillis() - ultimoCambioTexto;
            if (tiempoSinTexto > TEXTO_ESTABLE_MS) {
                mainHandler.post(() -> overlayView.clear());
                ultimoTextoDetectado = "";
                ultimaTraduccion = "";
            }
            return;
        }

        // ✅ Obtener el recuadro de captura en coordenadas de imagen
        Rect captureRect = overlayView.getCaptureBoxInImageCoords();

        overlayView.setImageDimensions(imageWidth, imageHeight);

        // ✅ Recolectar TODO el texto dentro o que intersecta el recuadro
        List<String> textosDetectados = new ArrayList<>();

        for (Text.TextBlock block : blocks) {
            for (Text.Line line : block.getLines()) {
                String lineText = line.getText();
                Rect lineBoundingBox = line.getBoundingBox();

                if (lineText.isEmpty() || lineBoundingBox == null) {
                    continue;
                }

                if (isTextInCaptureArea(lineBoundingBox, captureRect)) {
                    textosDetectados.add(lineText);
                }
            }
        }

        // ✅ Procesar el texto combinado
        if (!textosDetectados.isEmpty()) {
            String textoCompleto = String.join(" ", textosDetectados).trim();

            // ✅ ESTABILIZACIÓN: Solo procesar si el texto cambió significativamente
            if (textoHaCambiado(textoCompleto)) {
                Log.d(TAG, "📄 Texto nuevo detectado: " + textoCompleto);
                ultimoTextoDetectado = textoCompleto;
                ultimoCambioTexto = System.currentTimeMillis();

                // Verificar cache
                if (translationCache.containsKey(textoCompleto)) {
                    String cachedTranslation = translationCache.get(textoCompleto);
                    ultimaTraduccion = cachedTranslation;

                    List<TranslationBox> translationBoxes = new ArrayList<>();
                    translationBoxes.add(new TranslationBox(
                            captureRect, textoCompleto, cachedTranslation
                    ));

                    mainHandler.post(() -> overlayView.setTranslationBoxes(translationBoxes));

                    Log.d(TAG, "💾 Usando traducción en cache");
                } else {
                    // Detectar idioma y traducir
                    List<TranslationBox> translationBoxes = new ArrayList<>();
                    detectarIdiomaYTraducir(textoCompleto, captureRect, translationBoxes);
                }
            } else {
                // ✅ Texto no cambió, mantener traducción actual
                if (!ultimaTraduccion.isEmpty()) {
                    List<TranslationBox> translationBoxes = new ArrayList<>();
                    translationBoxes.add(new TranslationBox(
                            captureRect, ultimoTextoDetectado, ultimaTraduccion
                    ));
                    mainHandler.post(() -> overlayView.setTranslationBoxes(translationBoxes));
                }
                Log.d(TAG, "⏸️ Texto sin cambios, manteniendo traducción actual");
            }
        } else {
            // No hay texto dentro del recuadro
            long tiempoSinTexto = System.currentTimeMillis() - ultimoCambioTexto;
            if (tiempoSinTexto > TEXTO_ESTABLE_MS) {
                mainHandler.post(() -> overlayView.clear());
                ultimoTextoDetectado = "";
                ultimaTraduccion = "";
                Log.d(TAG, "📭 No hay texto dentro del recuadro (limpiando)");
            }
        }
    }

    /**
     * ✅ NUEVO: Verificación más inteligente de texto en área de captura
     */
    private boolean isTextInCaptureArea(Rect textRect, Rect captureRect) {
        // Calcular el área de intersección
        Rect intersection = new Rect();
        boolean intersects = intersection.setIntersect(textRect, captureRect);

        if (!intersects) {
            return false;
        }

        // Calcular qué porcentaje del texto está dentro del recuadro
        float textArea = textRect.width() * textRect.height();
        float intersectionArea = intersection.width() * intersection.height();
        float overlapPercentage = (intersectionArea / textArea) * 100;

        // ✅ Aceptar si al menos el 30% del texto está dentro
        return overlapPercentage >= 30;
    }

    /**
     * ✅ NUEVO: Verificar si el texto cambió significativamente
     */
    private boolean textoHaCambiado(String nuevoTexto) {
        if (ultimoTextoDetectado.isEmpty()) {
            return true;
        }

        // Normalizar textos (quitar espacios extra, minúsculas)
        String textoNormalizado = nuevoTexto.trim().toLowerCase().replaceAll("\\s+", " ");
        String ultimoNormalizado = ultimoTextoDetectado.trim().toLowerCase().replaceAll("\\s+", " ");

        // Calcular similitud (Levenshtein simplificado)
        int diferencia = calcularDiferencia(textoNormalizado, ultimoNormalizado);
        float similitud = 1.0f - ((float) diferencia / Math.max(textoNormalizado.length(), ultimoNormalizado.length()));

        // Solo considerar cambio si la similitud es menor al 85%
        boolean cambio = similitud < 0.85f;

        if (cambio) {
            Log.d(TAG, "🔄 Cambio detectado: similitud=" + (int)(similitud * 100) + "%");
        }

        return cambio;
    }

    /**
     * ✅ NUEVO: Calcular diferencia entre textos (distancia de Levenshtein)
     */
    private int calcularDiferencia(String s1, String s2) {
        int len1 = s1.length();
        int len2 = s2.length();

        int[][] dp = new int[len1 + 1][len2 + 1];

        for (int i = 0; i <= len1; i++) {
            dp[i][0] = i;
        }

        for (int j = 0; j <= len2; j++) {
            dp[0][j] = j;
        }

        for (int i = 1; i <= len1; i++) {
            for (int j = 1; j <= len2; j++) {
                int cost = (s1.charAt(i - 1) == s2.charAt(j - 1)) ? 0 : 1;
                dp[i][j] = Math.min(
                        Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                        dp[i - 1][j - 1] + cost
                );
            }
        }

        return dp[len1][len2];
    }

    /**
     * ⭐ DETECTAR IDIOMA CON CACHE
     */
    private void detectarIdiomaYTraducir(String texto, Rect boundingBox,
                                         List<TranslationBox> boxes) {

        Log.d(TAG, "🔍 Detectando idioma para: " + texto);

        languageIdentifier.identifyLanguage(texto)
                .addOnSuccessListener(languageCode -> {
                    if (languageCode.equals("und")) {
                        Log.w(TAG, "⚠️ Idioma no detectado, intentando con inglés por defecto");
                        traducirConIdiomaOrigen(texto, "en", boundingBox, boxes);
                        return;
                    }

                    Log.d(TAG, "🌍 Idioma detectado: " + languageCode);

                    if (languageCode.equals(targetLanguageApiCode)) {
                        Log.d(TAG, "⏭️ Texto ya está en idioma destino");

                        // ✅ Guardar en estado
                        ultimaTraduccion = "✓ Ya está en " + obtenerNombreIdioma(targetLanguageApiCode);

                        boxes.clear();
                        boxes.add(new TranslationBox(
                                boundingBox, texto, ultimaTraduccion
                        ));
                        mainHandler.post(() -> overlayView.setTranslationBoxes(new ArrayList<>(boxes)));
                        return;
                    }

                    traducirConIdiomaOrigen(texto, languageCode, boundingBox, boxes);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Error detectando idioma: " + e.getMessage());
                    traducirConIdiomaOrigen(texto, "en", boundingBox, boxes);
                });
    }

    /**
     * ⭐ TRADUCIR CON ACTUALIZACIÓN DE ESTADO
     */
    private void traducirConIdiomaOrigen(String texto, String sourceLanguage,
                                         Rect boundingBox,
                                         List<TranslationBox> boxes) {

        if (texto.trim().isEmpty()) {
            Log.w(TAG, "⚠️ Texto vacío, ignorando traducción");
            return;
        }

        TranslatorOptions options = new TranslatorOptions.Builder()
                .setSourceLanguage(sourceLanguage)
                .setTargetLanguage(targetLanguageApiCode)
                .build();

        Translator tempTranslator = Translation.getClient(options);

        DownloadConditions conditions = new DownloadConditions.Builder()
                .build();

        Log.d(TAG, "🔄 Iniciando traducción: " + sourceLanguage + " → " + targetLanguageApiCode);

        // ✅ Mostrar indicador de carga SOLO si no hay traducción previa
        if (ultimaTraduccion.isEmpty()) {
            boxes.clear();
            boxes.add(new TranslationBox(
                    boundingBox, texto, "⏳ Traduciendo..."
            ));
            mainHandler.post(() -> overlayView.setTranslationBoxes(new ArrayList<>(boxes)));
        }

        tempTranslator.downloadModelIfNeeded(conditions)
                .addOnSuccessListener(unused -> {
                    Log.d(TAG, "✅ Modelo descargado/disponible");

                    tempTranslator.translate(texto)
                            .addOnSuccessListener(translatedText -> {
                                Log.d(TAG, "✅ Traducción exitosa: " + texto + " → " + translatedText);

                                // ✅ Guardar estado
                                translationCache.put(texto, translatedText);
                                ultimaTraduccion = translatedText;

                                boxes.clear();
                                boxes.add(new TranslationBox(
                                        boundingBox, texto, translatedText
                                ));

                                mainHandler.post(() -> {
                                    overlayView.setTranslationBoxes(new ArrayList<>(boxes));
                                });

                                guardarEnHistorial(texto, translatedText, sourceLanguage);

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

                    boxes.clear();
                    boxes.add(new TranslationBox(
                            boundingBox,
                            texto,
                            "⚠️ Descargando modelo...\n" +
                                    sourceLanguage + " → " + targetLanguageApiCode
                    ));

                    mainHandler.post(() -> {
                        overlayView.setTranslationBoxes(new ArrayList<>(boxes));

                        Toast.makeText(requireContext(),
                                "⏳ Descargando modelo " + sourceLanguage + "→" + targetLanguageApiCode,
                                Toast.LENGTH_SHORT).show();
                    });

                    tempTranslator.close();
                });
    }

    /**
     * ⭐ GUARDAR EN HISTORIAL
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
     * ✅ Obtener nombre legible del idioma
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
     * ✅ Limpiar cache y estado
     */
    public void limpiarCacheTraduccion() {
        translationCache.clear();
        overlayView.clear();
        ultimoTextoDetectado = "";
        ultimaTraduccion = "";
        ultimoCambioTexto = 0;
        Log.d(TAG, "🧹 Cache y estado limpiado");
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