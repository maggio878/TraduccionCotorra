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
    private static final long DEBOUNCE_DELAY = 2000;
    private String lastTranslatedText = "";
    private String lastInputText = "";

    private TranslatorOptions translatorOptions;
    private Translator translator;
    private ProgressDialog progressDialog;
    private static final String TAG = "TRADUCCION_TAG";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_traduccion_texto, container, false);

        // Inicializar DAOs
        languageDAO = new LanguageDAO(requireContext());
        historialDAO = new HistorialDAO(requireContext());
        userDAO = new UserDAO(requireContext());
        translationTypeDAO = new TranslationTypeDAO(requireContext());
        favoriteDAO = new FavoriteTranslationDAO(requireContext());
        idiomasCache = new IdiomasCache(requireContext()); // ⭐ NUEVO

        userId = userDAO.obtenerUserIdActual(requireContext());

        TranslationTypeDAO.TranslationType tipoTexto = translationTypeDAO.obtenerTipoPorNombre("Texto");
        if (tipoTexto != null) {
            translationTypeId = tipoTexto.idTypeTranslation;
            Log.d(TAG, "Tipo de traducción 'Texto' ID: " + translationTypeId);
        }

        if (userId == -1) {
            Log.w(TAG, "Usuario no identificado. Historial no se guardará.");
            Toast.makeText(getContext(),
                    "⚠️ Usuario no identificado. El historial no se guardará.",
                    Toast.LENGTH_SHORT).show();
        } else {
            Log.d(TAG, "Usuario actual: " + userId);
        }

        debounceHandler = new Handler();

        cargarIdiomasDesdeDB();

        progressDialog = new ProgressDialog(getContext());
        progressDialog.setTitle("Por favor espere");
        progressDialog.setCanceledOnTouchOutside(false);

        inicializarVistas(view);
        configurarSpinners();
        configurarListeners();

        if (getArguments() != null) {
            String nombreUsuario = getArguments().getString("USUARIO");
            if (nombreUsuario != null) {
                Toast.makeText(getContext(), "Bienvenido " + nombreUsuario,
                        Toast.LENGTH_SHORT).show();
            }
        }

        return view;
    }

    private void cargarIdiomasDesdeDB() {
        idiomasDisponibles = languageDAO.obtenerIdiomasActivos();

        if (idiomasDisponibles.isEmpty()) {
            Toast.makeText(getContext(),
                    "⚠️ No hay idiomas configurados. Ve a Configuración → Administrar Catálogos",
                    Toast.LENGTH_LONG).show();
        } else {
            Log.d(TAG, "Idiomas cargados: " + idiomasDisponibles.size());
            for (LanguageDAO.Language idioma : idiomasDisponibles) {
                Log.d(TAG, "Idioma: " + idioma.name + " - API Code: " + idioma.apiCode);
            }
        }
    }

    private void inicializarVistas(View view) {
        txtTextoIngresado = view.findViewById(R.id.txt_TextoIngresado);
        txtvTextoTraducido = view.findViewById(R.id.txtv_TextoTraducido);
        spinnerSourceLanguage = view.findViewById(R.id.spinner_source_language);
        spinnerResultLanguage = view.findViewById(R.id.spinner_result_language);
        btnFavorite = view.findViewById(R.id.btn_favorite);
        menuButtonConfig = view.findViewById(R.id.menu_button_config);
    }

    private void configurarSpinners() {
        if (idiomasDisponibles.isEmpty()) {
            Toast.makeText(getContext(),
                    "No hay idiomas disponibles. Configura idiomas primero.",
                    Toast.LENGTH_LONG).show();
            spinnerSourceLanguage.setEnabled(false);
            spinnerResultLanguage.setEnabled(false);
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
                if (position < idiomasDisponibles.size()) {
                    LanguageDAO.Language idioma = idiomasDisponibles.get(position);
                    sourceLanguageId = idioma.languageId;
                    sourceLanguageCode = idioma.apiCode;
                    sourceLanguageTitle = idioma.name;
                    Log.d(TAG, "Idioma origen seleccionado: " + sourceLanguageTitle + " (ID: " + sourceLanguageId + ", API: " + sourceLanguageCode + ")");
                    crearTraductor();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        spinnerResultLanguage.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position < idiomasDisponibles.size()) {
                    LanguageDAO.Language idioma = idiomasDisponibles.get(position);
                    targetLanguageId = idioma.languageId;
                    targetLanguageCode = idioma.apiCode;
                    targetLanguageTitle = idioma.name;
                    Log.d(TAG, "Idioma destino seleccionado: " + targetLanguageTitle + " (ID: " + targetLanguageId + ", API: " + targetLanguageCode + ")");
                    crearTraductor();
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
        btnFavorite.setOnClickListener(v -> {
            String textoOriginal = txtTextoIngresado.getText().toString().trim();
            String textoTraducido = txtvTextoTraducido.getText().toString().trim();

            if (textoOriginal.isEmpty() || textoTraducido.isEmpty()) {
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
                int sourceLanguageId = obtenerIdIdiomaPorCodigo(sourceLanguageCode);
                int targetLanguageId = obtenerIdIdiomaPorCodigo(targetLanguageCode);
                int translationTypeId = 1;

                if (sourceLanguageId == -1 || targetLanguageId == -1) {
                    Toast.makeText(getContext(),
                            "Error al identificar idiomas",
                            Toast.LENGTH_SHORT).show();
                    return;
                }

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
                            "Agregado a favoritos",
                            Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getContext(),
                            "Error al agregar a favoritos",
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
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                txtvTextoTraducido.setText("");

                if (s.toString().isEmpty()) {
                    txtvTextoTraducido.setText("");
                    cancelarDebounce();
                } else {
                    traducirTexto();
                }
            }
        });
    }

    private void cancelarDebounce() {
        if (debounceHandler != null && debounceRunnable != null) {
            debounceHandler.removeCallbacks(debounceRunnable);
        }
    }

    private void crearTraductor() {
        if (sourceLanguageCode == null || targetLanguageCode == null) {
            Log.e(TAG, "Códigos de idioma no inicializados");
            return;
        }

        Log.d(TAG, "crearTraductor: " + sourceLanguageCode + " -> " + targetLanguageCode);

        if (sourceLanguageCode.equals(targetLanguageCode)) {
            Toast.makeText(getContext(),
                    "Por favor seleccione idiomas diferentes",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        translatorOptions = new TranslatorOptions.Builder()
                .setSourceLanguage(sourceLanguageCode)
                .setTargetLanguage(targetLanguageCode)
                .build();

        translator = Translation.getClient(translatorOptions);

        descargarModeloIdiomaConCache();
    }

    // ⭐ NUEVO MÉTODO: Verifica caché antes de descargar
    private void descargarModeloIdiomaConCache() {
        if (idiomasCache.idiomaYaDescargado(sourceLanguageCode, targetLanguageCode)) {
            Log.d(TAG, "✅ Modelo de idioma ya descargado previamente (" +
                    sourceLanguageCode + " -> " + targetLanguageCode + ")");

            if (!txtTextoIngresado.getText().toString().isEmpty()) {
                traducirTexto();
            }
            return;
        }

        Log.d(TAG, "⬇️ Modelo no encontrado en caché. Descargando...");
        descargarModeloIdioma();
    }

    private void descargarModeloIdioma() {
        progressDialog.setMessage("Descargando modelo de idioma...\n(Solo la primera vez)");
        progressDialog.show();

        DownloadConditions conditions = new DownloadConditions.Builder()
                .requireWifi()
                .build();

        translator.downloadModelIfNeeded(conditions)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        progressDialog.dismiss();

                        // ⭐ MARCAR COMO DESCARGADO EN CACHÉ
                        idiomasCache.marcarIdiomaDescargado(sourceLanguageCode, targetLanguageCode);

                        Log.d(TAG, "✅ Modelo descargado y guardado en caché: " +
                                sourceLanguageCode + " -> " + targetLanguageCode);

                        Toast.makeText(getContext(),
                                "✅ Modelo de idioma listo",
                                Toast.LENGTH_SHORT).show();

                        if (!txtTextoIngresado.getText().toString().isEmpty()) {
                            traducirTexto();
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        progressDialog.dismiss();
                        Log.e(TAG, "❌ Error al descargar modelo", e);
                        Toast.makeText(getContext(),
                                "Error al descargar modelo: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void traducirTexto() {
        String textoOriginal = txtTextoIngresado.getText().toString().trim();

        if (textoOriginal.isEmpty()) {
            txtvTextoTraducido.setText("");
            return;
        }

        if (translator == null) {
            crearTraductor();
            return;
        }

        if (sourceLanguageCode.equals(targetLanguageCode)) {
            txtvTextoTraducido.setText(textoOriginal);
            return;
        }

        Log.d(TAG, "Traduciendo: " + textoOriginal);

        translator.translate(textoOriginal)
                .addOnSuccessListener(new OnSuccessListener<String>() {
                    @Override
                    public void onSuccess(String textoTraducido) {
                        Log.d(TAG, "Traducción exitosa: " + textoTraducido);
                        txtvTextoTraducido.setText(textoTraducido);
                        verificarEstadoFavorito();

                        programarGuardadoConDebounce(textoOriginal, textoTraducido);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "Error al traducir", e);
                        Toast.makeText(getContext(),
                                "Error al traducir: " + e.getMessage(),
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
                    Log.d(TAG, "⏱️ Debounce completado. Guardando en historial...");
                    guardarEnHistorial(inputText, resultText);
                }
            }
        };

        debounceHandler.postDelayed(debounceRunnable, DEBOUNCE_DELAY);
        Log.d(TAG, "⏱️ Debounce iniciado. Esperando " + DEBOUNCE_DELAY + "ms...");
    }

    private void guardarEnHistorial(String textoOriginal, String textoTraducido) {
        if (userId == -1) {
            Log.w(TAG, "No se puede guardar historial: usuario no identificado");
            return;
        }

        if (textoOriginal == null || textoOriginal.trim().isEmpty() ||
                textoTraducido == null || textoTraducido.trim().isEmpty()) {
            Log.w(TAG, "No se puede guardar historial: texto vacío");
            return;
        }

        if (sourceLanguageId == -1 || targetLanguageId == -1) {
            Log.w(TAG, "No se puede guardar historial: IDs de idiomas no definidos");
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
                    Log.d(TAG, "✅ Historial guardado exitosamente. ID: " + resultado);
                    Log.d(TAG, "   UserId: " + userId);
                    Log.d(TAG, "   SourceLangId: " + sourceLanguageId + " (" + sourceLanguageTitle + ")");
                    Log.d(TAG, "   TargetLangId: " + targetLanguageId + " (" + targetLanguageTitle + ")");
                    Log.d(TAG, "   TypeId: " + translationTypeId);
                } else {
                    Log.d(TAG, "⚠️ Historial no guardado (posible duplicado reciente)");
                }

            } catch (Exception e) {
                Log.e(TAG, "❌ Error al guardar historial", e);
                e.printStackTrace();
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

    private int obtenerIdIdiomaPorCodigo(String languageCode) {
        List<LanguageDAO.Language> idiomas = languageDAO.obtenerTodosLosIdiomas();

        for (LanguageDAO.Language idioma : idiomas) {
            if (idioma.code.equalsIgnoreCase(languageCode)) {
                return idioma.languageId;
            }
        }

        return -1;
    }

    private void verificarEstadoFavorito() {
        String textoOriginal = txtTextoIngresado.getText().toString().trim();
        String textoTraducido = txtvTextoTraducido.getText().toString().trim();

        if (!textoOriginal.isEmpty() && !textoTraducido.isEmpty()) {
            esFavorito = favoriteDAO.existeFavorito(userId, textoOriginal, textoTraducido);

            if (esFavorito) {
                btnFavorite.setImageResource(R.drawable.favorite);
            } else {
                btnFavorite.setImageResource(R.drawable.ic_star_outline);
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (translator != null) {
            translator.close();
            translator = null;
        }
    }
}