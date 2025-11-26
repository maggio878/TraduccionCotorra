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
                translationCache.clear();
                overlayView.clear();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void configurarTraductor(String targetLanguageCode) {
        if (translator != null) {
            translator.close();
            translator = null;
            modeloDescargado = false;
        }

        // Por ahora, fuente fija en inglés (luego lo haremos automático)
        TranslatorOptions options = new TranslatorOptions.Builder()
                .setSourceLanguage("en") // Inglés como fuente por ahora
                .setTargetLanguage(targetLanguageCode)
                .build();

        translator = Translation.getClient(options);

        DownloadConditions conditions = new DownloadConditions.Builder()
                .requireWifi()
                .build();

        Toast.makeText(requireContext(),
                "⏳ Descargando modelo de traducción...",
                Toast.LENGTH_SHORT).show();

        translator.downloadModelIfNeeded(conditions)
                .addOnSuccessListener(unused -> {
                    modeloDescargado = true;
                    Log.d(TAG, "✅ Modelo descargado: en -> " + targetLanguageCode);
                    Toast.makeText(requireContext(),
                            "✅ Modelo listo",
                            Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    modeloDescargado = false;
                    Log.e(TAG, "❌ Error modelo: " + e.getMessage());
                    Toast.makeText(requireContext(),
                            "❌ Conecta WiFi para descargar modelo",
                            Toast.LENGTH_LONG).show();
                });
    }

    private void configurarListeners() {
        btnToggleAnalysis.setOnClickListener(v -> {
            if (!modeloDescargado) {
                Toast.makeText(requireContext(),
                        "⏳ Espera a que se descargue el modelo...",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            isAnalyzing = !isAnalyzing;

            if (isAnalyzing) {
                btnToggleAnalysis.setText("⏸️ Pausar");
                btnToggleAnalysis.setBackgroundColor(
                        ContextCompat.getColor(requireContext(), R.color.green_menu)
                );
                Toast.makeText(requireContext(),
                        "📸 Traducción activada\nApunta a texto en INGLÉS",
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
     * ⭐ PROCESAR TEXTO DETECTADO Y CREAR OVERLAYS
     */
    private void procesarTextoDetectado(Text visionText) {
        List<Text.TextBlock> blocks = visionText.getTextBlocks();

        if (blocks.isEmpty()) {
            mainHandler.post(() -> overlayView.clear());
            return;
        }

        Log.d(TAG, "📝 Bloques detectados: " + blocks.size());

        List<TraduccionOverlayView.TranslationBox> translationBoxes = new ArrayList<>();

        for (Text.TextBlock block : blocks) {
            String originalText = block.getText();
            Rect boundingBox = block.getBoundingBox();

            if (originalText.isEmpty() || boundingBox == null) {
                continue;
            }

            // Verificar cache
            if (translationCache.containsKey(originalText)) {
                String cachedTranslation = translationCache.get(originalText);
                translationBoxes.add(new TraduccionOverlayView.TranslationBox(  // ✅ CORREGIDO
                        boundingBox, originalText, cachedTranslation
                ));
            } else {
                // Traducir texto
                traducirYAgregarBox(originalText, boundingBox, translationBoxes);
            }
        }

        // Actualizar overlay cuando todas las traducciones estén listas
        mainHandler.postDelayed(() -> {
            overlayView.setTranslationBoxes(translationBoxes);
        }, 500);
    }

    /**
     * ⭐ TRADUCIR Y AGREGAR CAJA DE TRADUCCIÓN
     */
    /**
     * ⭐ TRADUCIR Y AGREGAR CAJA DE TRADUCCIÓN
     */
    private void traducirYAgregarBox(String texto, Rect boundingBox,
                                     List<TraduccionOverlayView.TranslationBox> boxes) {
        if (translator == null) {
            return;
        }

        translator.translate(texto)
                .addOnSuccessListener(translatedText -> {
                    Log.d(TAG, "✅ Traducido: " + texto + " -> " + translatedText);

                    // Guardar en cache
                    translationCache.put(texto, translatedText);

                    // Agregar a la lista
                    boxes.add(new TraduccionOverlayView.TranslationBox(  // ✅ CORREGIDO
                            boundingBox, texto, translatedText
                    ));

                    // Guardar en historial (solo la primera vez)
                    if (translationCache.size() == 1) {
                        guardarEnHistorial(texto, translatedText);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Error traducción: " + e.getMessage());
                });
    }

    private void guardarEnHistorial(String textoOriginal, String textoTraducido) {
        if (userId == -1 || textoOriginal.isEmpty() || textoTraducido.isEmpty()) {
            return;
        }

        new Thread(() -> {
            // Idioma origen: inglés (por ahora hardcoded)
            LanguageDAO.Language sourceLanguage = languageDAO.obtenerIdiomaPorApiCode("en");
            LanguageDAO.Language targetLanguage = languageDAO.obtenerIdiomaPorApiCode(targetLanguageApiCode);

            if (sourceLanguage == null || targetLanguage == null) {
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