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
import java.util.List;

import com.example.traduccioncotorra.Models.ModelLanguage;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.mlkit.common.model.DownloadConditions;
import com.google.mlkit.common.model.RemoteModelManager;
import com.google.mlkit.nl.translate.TranslateLanguage;
import com.google.mlkit.nl.translate.TranslateRemoteModel;
import com.google.mlkit.nl.translate.Translation;
import com.google.mlkit.nl.translate.Translator;
import com.google.mlkit.nl.translate.TranslatorOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


public class TraduccionTexto extends Fragment {

    private EditText txtTextoIngresado;
    private TextView txtvTextoTraducido;
    private AppCompatSpinner spinnerSourceLanguage;
    private AppCompatSpinner spinnerResultLanguage;
    private ImageButton btnFavorite;
    private ImageButton menuButtonConfig;

    private String sourceLanguageCode = TranslateLanguage.SPANISH; // Código de idioma origen
    private String targetLanguageCode = TranslateLanguage.ENGLISH; // Código de idioma destino
    private String sourceLanguageTitle = "Español";
    private String targetLanguageTitle = "Inglés";
    private boolean esFavorito = false;

    // DAO para obtener idiomas
    private LanguageDAO languageDAO;
    private List<LanguageDAO.Language> idiomasDisponibles;
    // Variables para traducción
    private TranslatorOptions translatorOptions;
    private Translator translator;
    private ProgressDialog progressDialog;
    private ArrayList<ModelLanguage> languageArrayList;
    private static final String TAG = "TRADUCCION_TAG";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Inflar el layout del fragment
        View view = inflater.inflate(R.layout.fragment_traduccion_texto, container, false);

        // Inicializar DAO
        languageDAO = new LanguageDAO(requireContext());

        // Cargar idiomas de la BD
        cargarIdiomasDesdeDB();
        // Inicializar progress dialog
        progressDialog = new ProgressDialog(getContext());
        progressDialog.setTitle("Por favor espere");
        progressDialog.setCanceledOnTouchOutside(false);

        // Inicializar vistas
        inicializarVistas(view);

        // Cargar idiomas disponibles
        loadAvailableLanguages();

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
    private void cargarIdiomasDesdeDB() {
        // Obtener solo idiomas activos
        idiomasDisponibles = languageDAO.obtenerIdiomasActivos();

        // Si no hay idiomas en la BD, mostrar mensaje
        if (idiomasDisponibles.isEmpty()) {
            Toast.makeText(getContext(),
                    "⚠️ No hay idiomas configurados. Ve a Configuración → Administrar Catálogos",
                    Toast.LENGTH_LONG).show();
        }
    }

    private void loadAvailableLanguages() {
        languageArrayList = new ArrayList<>();
        List<String> languageCodeList = TranslateLanguage.getAllLanguages();

        for (String languageCode : languageCodeList) {
            String languageTitle = new Locale(languageCode).getDisplayLanguage();
            Log.d(TAG, "loadAvailableLanguages: languageCode: " + languageCode);
            Log.d(TAG, "loadAvailableLanguages: languageTitle: " + languageTitle);

            ModelLanguage modelLanguage = new ModelLanguage(languageCode, languageTitle);
            languageArrayList.add(modelLanguage);
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
        // Crear adapter personalizado para mostrar los nombres de idiomas
        ArrayAdapter<ModelLanguage> adapterSource = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                languageArrayList
        );
        adapterSource.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        ArrayAdapter<ModelLanguage> adapterTarget = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                languageArrayList
        );
        adapterTarget.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        // Configurar spinner de idioma origen
        spinnerSourceLanguage.setAdapter(adapterSource);

        // Configurar spinner de idioma destino
        spinnerResultLanguage.setAdapter(adapterTarget);

        // Establecer selección por defecto (Español -> Inglés)
        for (int i = 0; i < languageArrayList.size(); i++) {
            if (languageArrayList.get(i).getLanguageCode().equals(TranslateLanguage.SPANISH)) {
                spinnerSourceLanguage.setSelection(i);
            }
            if (languageArrayList.get(i).getLanguageCode().equals(TranslateLanguage.ENGLISH)) {
                spinnerResultLanguage.setSelection(i);
            }
        }

        // Listeners para los spinners
        spinnerSourceLanguage.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                ModelLanguage selectedLanguage = (ModelLanguage) parent.getItemAtPosition(position);
                sourceLanguageCode = selectedLanguage.getLanguageCode();
                sourceLanguageTitle = selectedLanguage.getLanguageTitle();
                Log.d(TAG, "onItemSelected Source: " + sourceLanguageCode);

                // Crear nuevo traductor con los idiomas actualizados
                crearTraductor();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        spinnerResultLanguage.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                ModelLanguage selectedLanguage = (ModelLanguage) parent.getItemAtPosition(position);
                targetLanguageCode = selectedLanguage.getLanguageCode();
                targetLanguageTitle = selectedLanguage.getLanguageTitle();
                Log.d(TAG, "onItemSelected Target: " + targetLanguageCode);

                // Crear nuevo traductor con los idiomas actualizados
                crearTraductor();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

     // Busca la posición de un idioma por nombre

    private int buscarPosicionIdioma(String nombreIdioma) {
        for (int i = 0; i < idiomasDisponibles.size(); i++) {
            if (idiomasDisponibles.get(i).name.equalsIgnoreCase(nombreIdioma)) {
                return i;
            }
        }
        return -1;
    }
    //Obtiene el ID del idioma seleccionado (útil para guardar en BD)
    private int obtenerIdIdiomaOrigen() {
        for (LanguageDAO.Language idioma : idiomasDisponibles) {
            if (idioma.name.equals(idiomaOrigen)) {
                return idioma.languageId;
            }
        }
        return -1;
    }
    private int obtenerIdIdiomaDestino() {
        for (LanguageDAO.Language idioma : idiomasDisponibles) {
            if (idioma.name.equals(idiomaDestino)) {
                return idioma.languageId;
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

        // TextWatcher para traducir en tiempo real mientras el usuario escribe
        txtTextoIngresado.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                // Traducir después de que el usuario termine de escribir
                txtvTextoTraducido.setText(""); // Limpiar traducción anterior

                if (s.toString().isEmpty()) {
                    txtvTextoTraducido.setText("");
                } else {
                    traducirTexto();
                }
            }
        });
    }

    private void crearTraductor() {
        Log.d(TAG, "crearTraductor: Creando traductor de " + sourceLanguageCode + " a " + targetLanguageCode);

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
                .requireWifi() // Requiere WiFi para descargar (opcional)
                .build();

        translator.downloadModelIfNeeded(conditions)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        progressDialog.dismiss();
                        Log.d(TAG, "onSuccess: Modelo descargado exitosamente");
                        Toast.makeText(getContext(),
                                "Modelo de idioma listo",
                                Toast.LENGTH_SHORT).show();

                        // Traducir si hay texto
                        if (!txtTextoIngresado.getText().toString().isEmpty()) {
                            traducirTexto();
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        progressDialog.dismiss();
                        Log.e(TAG, "onFailure: Error al descargar modelo", e);
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

        // Validar que el traductor esté inicializado
        if (translator == null) {
            crearTraductor();
            return;
        }

        // Validar que los idiomas sean diferentes
        if (sourceLanguageCode.equals(targetLanguageCode)) {
            txtvTextoTraducido.setText(textoOriginal);
            return;
        }

        Log.d(TAG, "traducirTexto: Traduciendo de " + sourceLanguageCode + " a " + targetLanguageCode);

        // Realizar traducción con ML Kit
        translator.translate(textoOriginal)
                .addOnSuccessListener(new OnSuccessListener<String>() {
                    @Override
                    public void onSuccess(String textoTraducido) {
                        Log.d(TAG, "onSuccess: Texto traducido: " + textoTraducido);
                        txtvTextoTraducido.setText(textoTraducido);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "onFailure: Error al traducir", e);
                        Toast.makeText(getContext(),
                                "Error al traducir: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void abrirConfiguracion() {
        // Navegar al fragment de configuración
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
        // Cerrar el traductor para liberar recursos
        if (translator != null) {
            translator.close();
            translator = null;
        }
    }
}