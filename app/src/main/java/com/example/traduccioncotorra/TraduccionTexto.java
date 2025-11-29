package com.example.traduccioncotorra;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatSpinner;
import androidx.fragment.app.Fragment;
import com.example.traduccioncotorra.DB.LanguageDAO;
import com.example.traduccioncotorra.DB.HistorialDAO;
import com.example.traduccioncotorra.DB.UserDAO;
import com.example.traduccioncotorra.DB.TranslationTypeDAO;
import com.example.traduccioncotorra.DB.FavoriteTranslationDAO;
import com.example.traduccioncotorra.Utils.IdiomasCache;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.mlkit.common.model.DownloadConditions;
import com.google.mlkit.nl.translate.Translation;
import com.google.mlkit.nl.translate.Translator;
import com.google.mlkit.nl.translate.TranslatorOptions;
import android.os.Handler;

public class TraduccionTexto extends Fragment {

    private EditText txtTextoIngresado;
    private TextView txtvTextoTraducido;
    private AppCompatSpinner spinnerSourceLanguage;
    private AppCompatSpinner spinnerResultLanguage;
    private ImageButton btnFavorite;
    private ImageButton btnSwapLanguages; // ✅ NUEVO
    private ImageButton menuButtonConfig;

    private int sourceLanguageId = -1;
    private int targetLanguageId = -1;
    private String sourceLanguageCode;
    private String targetLanguageCode;
    private String sourceLanguageTitle;
    private String targetLanguageTitle;
    private boolean esFavorito = false;

    private LanguageDAO languageDAO;
    private List<LanguageDAO.Language> idiomasDisponibles;
    private HistorialDAO historialDAO;
    private UserDAO userDAO;
    private TranslationTypeDAO translationTypeDAO;
    private FavoriteTranslationDAO favoriteDAO;
    private IdiomasCache idiomasCache;
    private int userId;
    private int translationTypeId = 1;

    private Handler debounceHandler;
    private Runnable debounceRunnable;
    private static final long DEBOUNCE_DELAY = 800;
    private String lastTranslatedText = "";
    private String lastInputText = "";

    private Map<String, String> translationCache = new HashMap<>();

    private TranslatorOptions translatorOptions;
    private Translator translator;
    private ProgressDialog progressDialog;
    private static final String TAG = "TRADUCCION_TAG";

    private boolean translatorReady = false;
    private boolean isTranslating = false;

    // ✅ NUEVO: Flag para evitar loops en listeners de spinners
    private boolean isSwapping = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_traduccion_texto, container, false);

        languageDAO = new LanguageDAO(requireContext());
        historialDAO = new HistorialDAO(requireContext());
        userDAO = new UserDAO(requireContext());
        translationTypeDAO = new TranslationTypeDAO(requireContext());
        favoriteDAO = new FavoriteTranslationDAO(requireContext());
        idiomasCache = new IdiomasCache(requireContext());

        userId = userDAO.obtenerUserIdActual(requireContext());

        TranslationTypeDAO.TranslationType tipoTexto = translationTypeDAO.obtenerTipoPorNombre("Texto");
        if (tipoTexto != null) {
            translationTypeId = tipoTexto.idTypeTranslation;
        }

        debounceHandler = new Handler();

        cargarIdiomasDesdeDB();

        progressDialog = new ProgressDialog(getContext());
        progressDialog.setTitle("Por favor espere");
        progressDialog.setCanceledOnTouchOutside(false);

        inicializarVistas(view);
        configurarSpinners();
        configurarListeners();

        return view;
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

    private void inicializarVistas(View view) {
        txtTextoIngresado = view.findViewById(R.id.txt_TextoIngresado);
        txtvTextoTraducido = view.findViewById(R.id.txtv_TextoTraducido);
        spinnerSourceLanguage = view.findViewById(R.id.spinner_source_language);
        spinnerResultLanguage = view.findViewById(R.id.spinner_result_language);
        btnFavorite = view.findViewById(R.id.btn_favorite);
        btnSwapLanguages = view.findViewById(R.id.btn_swap_languages); // ✅ NUEVO
        menuButtonConfig = view.findViewById(R.id.menu_button_config);
    }

    private void configurarSpinners() {
        if (idiomasDisponibles.isEmpty()) {
            spinnerSourceLanguage.setEnabled(false);
            spinnerResultLanguage.setEnabled(false);
            btnSwapLanguages.setEnabled(false); // ✅ NUEVO
            return;
        }

        String[] nombresIdiomas = new String[idiomasDisponibles.size()];
        for (int i = 0; i < idiomasDisponibles.size(); i++) {
            nombresIdiomas[i] = idiomasDisponibles.get(i).name;
        }

        ArrayAdapter<String> adapterSource = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                nombresIdiomas
        );
        adapterSource.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        ArrayAdapter<String> adapterTarget = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                nombresIdiomas
        );
        adapterTarget.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        spinnerSourceLanguage.setAdapter(adapterSource);
        spinnerResultLanguage.setAdapter(adapterTarget);

        int posEspanol = buscarPosicionIdioma("Español");
        int posIngles = buscarPosicionIdioma("Inglés");

        if (posEspanol != -1) {
            spinnerSourceLanguage.setSelection(posEspanol);
            LanguageDAO.Language idioma = idiomasDisponibles.get(posEspanol);
            sourceLanguageId = idioma.languageId;
            sourceLanguageCode = idioma.apiCode;
            sourceLanguageTitle = idioma.name;
        } else if (!idiomasDisponibles.isEmpty()) {
            spinnerSourceLanguage.setSelection(0);
            LanguageDAO.Language idioma = idiomasDisponibles.get(0);
            sourceLanguageId = idioma.languageId;
            sourceLanguageCode = idioma.apiCode;
            sourceLanguageTitle = idioma.name;
        }

        if (posIngles != -1) {
            spinnerResultLanguage.setSelection(posIngles);
            LanguageDAO.Language idioma = idiomasDisponibles.get(posIngles);
            targetLanguageId = idioma.languageId;
            targetLanguageCode = idioma.apiCode;
            targetLanguageTitle = idioma.name;
        } else if (idiomasDisponibles.size() > 1) {
            spinnerResultLanguage.setSelection(1);
            LanguageDAO.Language idioma = idiomasDisponibles.get(1);
            targetLanguageId = idioma.languageId;
            targetLanguageCode = idioma.apiCode;
            targetLanguageTitle = idioma.name;
        }

        crearTraductor();

        spinnerSourceLanguage.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // ✅ Evitar procesamiento durante intercambio
                if (isSwapping) {
                    return;
                }

                if (position < idiomasDisponibles.size()) {
                    LanguageDAO.Language idioma = idiomasDisponibles.get(position);
                    sourceLanguageId = idioma.languageId;
                    sourceLanguageCode = idioma.apiCode;
                    sourceLanguageTitle = idioma.name;
                    Log.d(TAG, "📝 Idioma origen: " + sourceLanguageTitle);

                    translationCache.clear();
                    crearTraductor();

                    String textoActual = txtTextoIngresado.getText().toString().trim();
                    if (!textoActual.isEmpty()) {
                        traducirTexto();
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        spinnerResultLanguage.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // ✅ Evitar procesamiento durante intercambio
                if (isSwapping) {
                    return;
                }

                if (position < idiomasDisponibles.size()) {
                    LanguageDAO.Language idioma = idiomasDisponibles.get(position);
                    targetLanguageId = idioma.languageId;
                    targetLanguageCode = idioma.apiCode;
                    targetLanguageTitle = idioma.name;
                    Log.d(TAG, "🎯 Idioma destino: " + targetLanguageTitle);

                    translationCache.clear();
                    crearTraductor();

                    String textoActual = txtTextoIngresado.getText().toString().trim();
                    if (!textoActual.isEmpty()) {
                        traducirTexto();
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private int buscarPosicionIdioma(String nombreIdioma) {
        for (int i = 0; i < idiomasDisponibles.size(); i++) {
            if (idiomasDisponibles.get(i).name.equalsIgnoreCase(nombreIdioma)) {
                return i;
            }
        }
        return -1;
    }

    private void configurarListeners() {
        // ✅ NUEVO: Botón de intercambio de idiomas
        btnSwapLanguages.setOnClickListener(v -> {
            intercambiarIdiomas();
        });

        btnFavorite.setOnClickListener(v -> {
            String textoOriginal = txtTextoIngresado.getText().toString().trim();
            String textoTraducido = txtvTextoTraducido.getText().toString().trim();

            if (textoOriginal.isEmpty() || textoTraducido.isEmpty() ||
                    textoTraducido.startsWith("⏳") || textoTraducido.startsWith("❌")) {
                Toast.makeText(getContext(),
                        "Primero traduce un texto",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            if (esFavorito) {
                int resultado = favoriteDAO.eliminarFavoritoPorTextos(
                        userId, textoOriginal, textoTraducido);

                if (resultado > 0) {
                    esFavorito = false;
                    btnFavorite.setImageResource(R.drawable.ic_star_outline);
                    Toast.makeText(getContext(),
                            "Removido de favoritos",
                            Toast.LENGTH_SHORT).show();
                }
            } else {
                FavoriteTranslationDAO.FavoriteTranslation favorito =
                        new FavoriteTranslationDAO.FavoriteTranslation(
                                userId,
                                sourceLanguageId,
                                targetLanguageId,
                                translationTypeId,
                                textoOriginal,
                                textoTraducido
                        );

                long resultado = favoriteDAO.insertarFavorito(favorito);

                if (resultado != -1) {
                    esFavorito = true;
                    btnFavorite.setImageResource(R.drawable.favorite);
                    Toast.makeText(getContext(),
                            "⭐ Agregado a favoritos",
                            Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getContext(),
                            "⚠️ Ya está en favoritos",
                            Toast.LENGTH_SHORT).show();
                }
            }
        });

        menuButtonConfig.setOnClickListener(v -> {
            abrirConfiguracion();
        });

        txtTextoIngresado.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                cancelarDebounce();
            }

            @Override
            public void afterTextChanged(Editable s) {
                String texto = s.toString().trim();

                if (texto.isEmpty()) {
                    txtvTextoTraducido.setText("");
                    esFavorito = false;
                    btnFavorite.setImageResource(R.drawable.ic_star_outline);
                    return;
                }

                txtvTextoTraducido.setText("⏳ Escribiendo...");
                traducirTexto();
            }
        });
    }

    /**
     * ✅ NUEVO: Intercambiar idiomas origen y destino
     */
    private void intercambiarIdiomas() {
        // Verificar que los idiomas sean diferentes
        if (sourceLanguageCode.equals(targetLanguageCode)) {
            Toast.makeText(getContext(),
                    "Los idiomas son iguales",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d(TAG, "🔄 Intercambiando idiomas: " + sourceLanguageTitle + " ↔ " + targetLanguageTitle);

        // ✅ Activar flag para evitar que los listeners procesen el cambio
        isSwapping = true;

        // Guardar textos actuales
        String textoOriginal = txtTextoIngresado.getText().toString().trim();
        String textoTraducido = txtvTextoTraducido.getText().toString().trim();

        // Intercambiar IDs
        int tempId = sourceLanguageId;
        sourceLanguageId = targetLanguageId;
        targetLanguageId = tempId;

        // Intercambiar códigos
        String tempCode = sourceLanguageCode;
        sourceLanguageCode = targetLanguageCode;
        targetLanguageCode = tempCode;

        // Intercambiar títulos
        String tempTitle = sourceLanguageTitle;
        sourceLanguageTitle = targetLanguageTitle;
        targetLanguageTitle = tempTitle;

        // Buscar posiciones en los spinners
        int nuevaPosicionOrigen = buscarPosicionPorApiCode(sourceLanguageCode);
        int nuevaPosicionDestino = buscarPosicionPorApiCode(targetLanguageCode);

        // Actualizar spinners
        if (nuevaPosicionOrigen != -1) {
            spinnerSourceLanguage.setSelection(nuevaPosicionOrigen);
        }

        if (nuevaPosicionDestino != -1) {
            spinnerResultLanguage.setSelection(nuevaPosicionDestino);
        }

        // ✅ Desactivar flag después de un breve delay
        new Handler().postDelayed(() -> {
            isSwapping = false;
        }, 100);

        // Limpiar cache
        translationCache.clear();

        // Crear nuevo traductor
        crearTraductor();

        // Intercambiar textos solo si ambos tienen contenido válido
        if (!textoOriginal.isEmpty() && !textoTraducido.isEmpty() &&
                !textoTraducido.startsWith("⏳") && !textoTraducido.startsWith("❌") &&
                !textoTraducido.startsWith("✓")) {

            // Intercambiar los textos
            txtTextoIngresado.setText(textoTraducido);
            txtvTextoTraducido.setText(textoOriginal);

            // Actualizar último texto traducido
            lastInputText = textoTraducido;
            lastTranslatedText = textoOriginal;

            // Verificar favorito
            verificarEstadoFavorito();
        } else if (!textoOriginal.isEmpty()) {
            // Si solo hay texto original, traducir con los nuevos idiomas
            traducirTextoInmediato(textoOriginal);
        }

        Toast.makeText(getContext(),
                "🔄 Idiomas intercambiados: " + sourceLanguageTitle + " → " + targetLanguageTitle,
                Toast.LENGTH_SHORT).show();

        Log.d(TAG, "✅ Intercambio completado");
    }

    /**
     * ✅ NUEVO: Buscar posición por código API
     */
    private int buscarPosicionPorApiCode(String apiCode) {
        for (int i = 0; i < idiomasDisponibles.size(); i++) {
            if (idiomasDisponibles.get(i).apiCode.equals(apiCode)) {
                return i;
            }
        }
        return -1;
    }

    private void cancelarDebounce() {
        if (debounceHandler != null && debounceRunnable != null) {
            debounceHandler.removeCallbacks(debounceRunnable);
        }
    }

    private void crearTraductor() {
        if (sourceLanguageCode == null || targetLanguageCode == null) {
            Log.e(TAG, "❌ Códigos de idioma no inicializados");
            return;
        }

        if (sourceLanguageCode.equals(targetLanguageCode)) {
            Toast.makeText(getContext(),
                    "Por favor seleccione idiomas diferentes",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d(TAG, "🔧 Creando traductor: " + sourceLanguageCode + " → " + targetLanguageCode);

        if (translator != null) {
            translator.close();
            translator = null;
            translatorReady = false;
        }

        translatorOptions = new TranslatorOptions.Builder()
                .setSourceLanguage(sourceLanguageCode)
                .setTargetLanguage(targetLanguageCode)
                .build();

        translator = Translation.getClient(translatorOptions);

        descargarModeloIdiomaConCache();
    }

    private void descargarModeloIdiomaConCache() {
        if (idiomasCache.idiomaYaDescargado(sourceLanguageCode, targetLanguageCode)) {
            Log.d(TAG, "✅ Modelo ya en cache: " + sourceLanguageCode + " → " + targetLanguageCode);
            translatorReady = true;
            return;
        }

        Log.d(TAG, "📥 Descargando modelo: " + sourceLanguageCode + " → " + targetLanguageCode);
        descargarModeloIdioma();
    }

    private void descargarModeloIdioma() {
        progressDialog.setMessage("Descargando modelo de idioma...\n(Solo la primera vez)");
        progressDialog.show();

        DownloadConditions conditions = new DownloadConditions.Builder()
                .build();

        translator.downloadModelIfNeeded(conditions)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        progressDialog.dismiss();
                        translatorReady = true;

                        idiomasCache.marcarIdiomaDescargado(sourceLanguageCode, targetLanguageCode);

                        Log.d(TAG, "✅ Modelo descargado");
                        Toast.makeText(getContext(),
                                "✅ Modelo listo",
                                Toast.LENGTH_SHORT).show();

                        String textoActual = txtTextoIngresado.getText().toString().trim();
                        if (!textoActual.isEmpty()) {
                            traducirTextoInmediato(textoActual);
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        progressDialog.dismiss();
                        translatorReady = false;

                        Log.e(TAG, "❌ Error descargando modelo", e);
                        Toast.makeText(getContext(),
                                "❌ Error: Verifica tu conexión a internet",
                                Toast.LENGTH_LONG).show();

                        txtvTextoTraducido.setText("❌ Error al descargar modelo\nConecta a internet");
                    }
                });
    }

    private void traducirTexto() {
        cancelarDebounce();

        debounceRunnable = new Runnable() {
            @Override
            public void run() {
                String texto = txtTextoIngresado.getText().toString().trim();

                if (!texto.isEmpty()) {
                    traducirTextoInmediato(texto);
                }
            }
        };

        debounceHandler.postDelayed(debounceRunnable, DEBOUNCE_DELAY);
    }

    private void traducirTextoInmediato(String textoOriginal) {
        if (textoOriginal.isEmpty()) {
            return;
        }

        if (textoOriginal.length() > 5000) {
            txtvTextoTraducido.setText("⚠️ Texto muy largo (máximo 5000 caracteres)");
            return;
        }

        if (!translatorReady || translator == null) {
            Log.w(TAG, "⚠️ Traductor no listo");
            txtvTextoTraducido.setText("⏳ Preparando traductor...");
            descargarModeloIdiomaConCache();
            return;
        }

        if (sourceLanguageCode.equals(targetLanguageCode)) {
            txtvTextoTraducido.setText(textoOriginal);
            return;
        }

        String cacheKey = sourceLanguageCode + "-" + targetLanguageCode + "-" + textoOriginal;
        if (translationCache.containsKey(cacheKey)) {
            String traduccionCache = translationCache.get(cacheKey);
            txtvTextoTraducido.setText(traduccionCache);
            lastTranslatedText = traduccionCache;
            lastInputText = textoOriginal;
            Log.d(TAG, "💾 Usando cache");
            verificarEstadoFavorito();
            return;
        }

        if (isTranslating) {
            Log.d(TAG, "⏸️ Ya hay una traducción en progreso");
            return;
        }

        isTranslating = true;
        Log.d(TAG, "🔄 Traduciendo: " + textoOriginal);
        txtvTextoTraducido.setText("⏳ Traduciendo...");

        translator.translate(textoOriginal)
                .addOnSuccessListener(new OnSuccessListener<String>() {
                    @Override
                    public void onSuccess(String textoTraducido) {
                        isTranslating = false;

                        Log.d(TAG, "✅ Traducción exitosa: " + textoTraducido);
                        txtvTextoTraducido.setText(textoTraducido);
                        lastTranslatedText = textoTraducido;
                        lastInputText = textoOriginal;

                        translationCache.put(cacheKey, textoTraducido);

                        verificarEstadoFavorito();

                        programarGuardadoConDebounce(textoOriginal, textoTraducido);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        isTranslating = false;

                        Log.e(TAG, "❌ Error al traducir", e);
                        txtvTextoTraducido.setText("❌ Error al traducir");
                        Toast.makeText(getContext(),
                                "Error: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void programarGuardadoConDebounce(String inputText, String resultText) {
        cancelarDebounce();

        lastInputText = inputText;
        lastTranslatedText = resultText;

        debounceRunnable = new Runnable() {
            @Override
            public void run() {
                if (inputText.equals(lastInputText) && resultText.equals(lastTranslatedText)) {
                    Log.d(TAG, "💾 Guardando en historial...");
                    guardarEnHistorial(inputText, resultText);
                }
            }
        };

        debounceHandler.postDelayed(debounceRunnable, DEBOUNCE_DELAY);
    }

    private void guardarEnHistorial(String textoOriginal, String textoTraducido) {
        if (userId == -1 || sourceLanguageId == -1 || targetLanguageId == -1) {
            Log.w(TAG, "⚠️ No se puede guardar historial: datos incompletos");
            return;
        }

        if (textoOriginal == null || textoOriginal.trim().isEmpty() ||
                textoTraducido == null || textoTraducido.trim().isEmpty()) {
            Log.w(TAG, "⚠️ No se puede guardar historial: texto vacío");
            return;
        }

        new Thread(() -> {
            try {
                long resultado = historialDAO.insertarHistorialSiNoExiste(
                        userId,
                        sourceLanguageId,
                        targetLanguageId,
                        translationTypeId,
                        textoOriginal,
                        textoTraducido
                );

                if (resultado != -1) {
                    Log.d(TAG, "✅ Historial guardado: ID " + resultado);
                } else {
                    Log.d(TAG, "⚠️ Historial duplicado");
                }

            } catch (Exception e) {
                Log.e(TAG, "❌ Error al guardar historial", e);
            }
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

    private void verificarEstadoFavorito() {
        String textoOriginal = txtTextoIngresado.getText().toString().trim();
        String textoTraducido = txtvTextoTraducido.getText().toString().trim();

        if (!textoOriginal.isEmpty() && !textoTraducido.isEmpty() &&
                !textoTraducido.startsWith("⏳") && !textoTraducido.startsWith("❌")) {

            new Thread(() -> {
                esFavorito = favoriteDAO.existeFavorito(userId, textoOriginal, textoTraducido);

                requireActivity().runOnUiThread(() -> {
                    if (esFavorito) {
                        btnFavorite.setImageResource(R.drawable.favorite);
                    } else {
                        btnFavorite.setImageResource(R.drawable.ic_star_outline);
                    }
                });
            }).start();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (debounceHandler != null && debounceRunnable != null) {
            debounceHandler.removeCallbacks(debounceRunnable);
        }

        if (translator != null) {
            translator.close();
            translator = null;
        }

        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }

        translationCache.clear();

        Log.d(TAG, "🛑 Recursos liberados");
    }
}