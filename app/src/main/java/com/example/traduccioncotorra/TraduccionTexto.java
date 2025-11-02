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

    // ⭐ NUEVO: Variables para idiomas seleccionados
    private int sourceLanguageId = -1;
    private int targetLanguageId = -1;
    private String sourceLanguageCode;
    private String targetLanguageCode;
    private String sourceLanguageTitle;
    private String targetLanguageTitle;
    private boolean esFavorito = false;

    // DAO para obtener idiomas
    private LanguageDAO languageDAO;
    private List<LanguageDAO.Language> idiomasDisponibles;

    // ⭐ NUEVO: DAOs para historial
    private HistorialDAO historialDAO;
    private UserDAO userDAO;
    private TranslationTypeDAO translationTypeDAO;
    private int userId;
    private int translationTypeId = 1; // 1 = Texto (por defecto)

    // ⭐ NUEVO: Variables para debounce (evitar guardar mientras escribe)
    private Handler debounceHandler;
    private Runnable debounceRunnable;
    private static final long DEBOUNCE_DELAY = 2000; // 2 segundos después de dejar de escribir
    private String lastTranslatedText = "";
    private String lastInputText = "";

    // Variables para traducción
    private TranslatorOptions translatorOptions;
    private Translator translator;
    private ProgressDialog progressDialog;
    private static final String TAG = "TRADUCCION_TAG";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_traduccion_texto, container, false);

        // Inicializar DAO
        languageDAO = new LanguageDAO(requireContext());

        // ⭐ NUEVO: Inicializar DAOs para historial
        historialDAO = new HistorialDAO(requireContext());
        userDAO = new UserDAO(requireContext());
        translationTypeDAO = new TranslationTypeDAO(requireContext());
        userId = userDAO.obtenerUserIdActual(requireContext());

        // Obtener el ID del tipo "Texto"
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

        // ⭐ NUEVO: Inicializar handler para debounce
        debounceHandler = new Handler();

        // Cargar idiomas de la BD
        cargarIdiomasDesdeDB();

        // Inicializar progress dialog
        progressDialog = new ProgressDialog(getContext());
        progressDialog.setTitle("Por favor espere");
        progressDialog.setCanceledOnTouchOutside(false);

        // Inicializar vistas
        inicializarVistas(view);

        // Configurar spinners
        configurarSpinners();

        // Configurar listeners
        configurarListeners();

        // Obtener argumentos si existen
        if (getArguments() != null) {
            String nombreUsuario = getArguments().getString("USUARIO");
            if (nombreUsuario != null) {
                Toast.makeText(getContext(), "Bienvenido " + nombreUsuario,
                        Toast.LENGTH_SHORT).show();
            }
        }

        return view;
    }

    /**
     * ⭐ NUEVO: Cargar idiomas desde la base de datos
     */
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

    /**
     * ⭐ REFACTORIZADO: Configurar spinners desde la BD
     */
    private void configurarSpinners() {
        if (idiomasDisponibles.isEmpty()) {
            Toast.makeText(getContext(),
                    "No hay idiomas disponibles. Configura idiomas primero.",
                    Toast.LENGTH_LONG).show();
            spinnerSourceLanguage.setEnabled(false);
            spinnerResultLanguage.setEnabled(false);
            return;
        }

        // Crear array de nombres de idiomas
        String[] nombresIdiomas = new String[idiomasDisponibles.size()];
        for (int i = 0; i < idiomasDisponibles.size(); i++) {
            nombresIdiomas[i] = idiomasDisponibles.get(i).name;
        }

        // Crear adapters
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

        // Configurar spinners
        spinnerSourceLanguage.setAdapter(adapterSource);
        spinnerResultLanguage.setAdapter(adapterTarget);

        // ⭐ Establecer selección por defecto: Español -> Inglés
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

        // Crear traductor inicial
        crearTraductor();

        // Listeners para los spinners
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

    /**
     * ⭐ NUEVO: Buscar posición de un idioma por nombre
     */
    private int buscarPosicionIdioma(String nombreIdioma) {
        for (int i = 0; i < idiomasDisponibles.size(); i++) {
            if (idiomasDisponibles.get(i).name.equalsIgnoreCase(nombreIdioma)) {
                return i;
            }
        }
        return -1;
    }

    private void configurarListeners() {
        // Listener para el botón de favoritos
        btnFavorite.setOnClickListener(v -> {
            esFavorito = !esFavorito;
            if (esFavorito) {
                btnFavorite.setImageResource(R.drawable.favorite);
                Toast.makeText(getContext(), "Agregado a favoritos", Toast.LENGTH_SHORT).show();
            } else {
                btnFavorite.setImageResource(R.drawable.ic_star_outline);
                Toast.makeText(getContext(), "Removido de favoritos", Toast.LENGTH_SHORT).show();
            }
        });

        menuButtonConfig.setOnClickListener(v -> {
            abrirConfiguracion();
        });

        // TextWatcher para traducir en tiempo real
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
                    // ⭐ Cancelar debounce si el texto está vacío
                    cancelarDebounce();
                } else {
                    traducirTexto();
                }
            }
        });
    }

    /**
     * ⭐ NUEVO: Cancela el debounce pendiente
     */
    private void cancelarDebounce() {
        if (debounceHandler != null && debounceRunnable != null) {
            debounceHandler.removeCallbacks(debounceRunnable);
        }
    }

    private void crearTraductor() {
        // Validar que ambos códigos existan
        if (sourceLanguageCode == null || targetLanguageCode == null) {
            Log.e(TAG, "Códigos de idioma no inicializados");
            return;
        }

        Log.d(TAG, "crearTraductor: " + sourceLanguageCode + " -> " + targetLanguageCode);

        // Validar que los idiomas sean diferentes
        if (sourceLanguageCode.equals(targetLanguageCode)) {
            Toast.makeText(getContext(),
                    "Por favor seleccione idiomas diferentes",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        // Configurar opciones del traductor
        translatorOptions = new TranslatorOptions.Builder()
                .setSourceLanguage(sourceLanguageCode)
                .setTargetLanguage(targetLanguageCode)
                .build();

        // Crear traductor
        translator = Translation.getClient(translatorOptions);

        // Descargar modelo si es necesario
        descargarModeloIdioma();
    }

    private void descargarModeloIdioma() {
        progressDialog.setMessage("Descargando modelo de idioma...");
        progressDialog.show();

        DownloadConditions conditions = new DownloadConditions.Builder()
                .requireWifi()
                .build();

        translator.downloadModelIfNeeded(conditions)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        progressDialog.dismiss();
                        Log.d(TAG, "Modelo descargado: " + sourceLanguageCode + " -> " + targetLanguageCode);
                        Toast.makeText(getContext(),
                                "Modelo de idioma listo",
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
                        Log.e(TAG, "Error al descargar modelo", e);
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

                        // ⭐ ACTUALIZADO: Programar guardado con debounce
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

    /**
     * ⭐ NUEVO: Programa el guardado con debounce (espera 2 segundos después de dejar de escribir)
     */
    private void programarGuardadoConDebounce(String inputText, String resultText) {
        // Cancelar cualquier guardado pendiente
        cancelarDebounce();

        // Guardar referencias para el debounce
        lastInputText = inputText;
        lastTranslatedText = resultText;

        // Crear nuevo runnable
        debounceRunnable = new Runnable() {
            @Override
            public void run() {
                // Guardar solo si los textos siguen siendo los mismos (usuario dejó de escribir)
                if (inputText.equals(lastInputText) && resultText.equals(lastTranslatedText)) {
                    Log.d(TAG, "⏱️ Debounce completado. Guardando en historial...");
                    guardarEnHistorial(inputText, resultText);
                }
            }
        };

        // Programar ejecución después del delay
        debounceHandler.postDelayed(debounceRunnable, DEBOUNCE_DELAY);
        Log.d(TAG, "⏱️ Debounce iniciado. Esperando " + DEBOUNCE_DELAY + "ms...");
    }

    /**
     * ⭐ ACTUALIZADO: Guarda la traducción en el historial con IDs correctos
     */
    private void guardarEnHistorial(String textoOriginal, String textoTraducido) {
        // Validar que tengamos un usuario válido
        if (userId == -1) {
            Log.w(TAG, "No se puede guardar historial: usuario no identificado");
            return;
        }

        // Validar que los textos no estén vacíos
        if (textoOriginal == null || textoOriginal.trim().isEmpty() ||
                textoTraducido == null || textoTraducido.trim().isEmpty()) {
            Log.w(TAG, "No se puede guardar historial: texto vacío");
            return;
        }

        // Validar que tengamos IDs de idiomas válidos
        if (sourceLanguageId == -1 || targetLanguageId == -1) {
            Log.w(TAG, "No se puede guardar historial: IDs de idiomas no definidos");
            return;
        }

        // Guardar en un hilo separado para no bloquear la UI
        new Thread(() -> {
            try {
                // ⭐ Usar el método con IDs correctos
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (translator != null) {
            translator.close();
            translator = null;
        }
    }
}