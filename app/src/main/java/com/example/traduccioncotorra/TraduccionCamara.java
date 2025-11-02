package com.example.traduccioncotorra;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatSpinner;
import androidx.fragment.app.Fragment;
import com.example.traduccioncotorra.DB.LanguageDAO;
import com.google.android.material.button.MaterialButton;
import java.util.List;

public class TraduccionCamara extends Fragment {

    private EditText etTranslatedText;
    private ImageView ivFavorite;
    private AppCompatSpinner spinnerTargetLanguage;
    private MaterialButton btnConfirmTranslation;
    private ImageButton menuButtonConfig;

    // ⭐ NUEVO: Variables para idiomas desde BD
    private String idiomaDestino;
    private String idiomaDestinoApiCode;
    private boolean esFavorito = false;
    private String textoExtraido = "";

    private LanguageDAO languageDAO;
    private List<LanguageDAO.Language> idiomasDisponibles;
    private static final String TAG = "TRADUCCION_CAMARA";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_traduccion_camara, container, false);

        // Inicializar DAO
        languageDAO = new LanguageDAO(requireContext());

        // Cargar idiomas de la BD
        cargarIdiomasDesdeDB();

        // Inicializar vistas
        inicializarVistas(view);

        // Configurar spinner
        configurarSpinner();

        // Configurar listeners
        configurarListeners();

        return view;
    }

    /**
     * ⭐ NUEVO: Cargar idiomas desde la base de datos
     */
    private void cargarIdiomasDesdeDB() {
        idiomasDisponibles = languageDAO.obtenerIdiomasActivos();

        if (idiomasDisponibles.isEmpty()) {
            Toast.makeText(getContext(),
                    "⚠️ No hay idiomas configurados",
                    Toast.LENGTH_LONG).show();
        } else {
            Log.d(TAG, "Idiomas cargados: " + idiomasDisponibles.size());
        }
    }

    private void inicializarVistas(View view) {
        etTranslatedText = view.findViewById(R.id.etTranslatedText);
        ivFavorite = view.findViewById(R.id.ivFavorite);
        spinnerTargetLanguage = view.findViewById(R.id.spinner_target_language);
        btnConfirmTranslation = view.findViewById(R.id.btn_confirm_translation);
        menuButtonConfig = view.findViewById(R.id.menu_button_config);
    }

    /**
     * ⭐ REFACTORIZADO: Configurar spinner desde la BD
     */
    private void configurarSpinner() {
        if (idiomasDisponibles.isEmpty()) {
            spinnerTargetLanguage.setEnabled(false);
            Toast.makeText(getContext(),
                    "No hay idiomas disponibles. Configura idiomas primero.",
                    Toast.LENGTH_LONG).show();
            return;
        }

        // Crear array de nombres de idiomas
        String[] nombresIdiomas = new String[idiomasDisponibles.size()];
        for (int i = 0; i < idiomasDisponibles.size(); i++) {
            nombresIdiomas[i] = idiomasDisponibles.get(i).name;
        }

        // Crear adapter
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                nombresIdiomas
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        // Configurar spinner
        spinnerTargetLanguage.setAdapter(adapter);

        // ⭐ Buscar y seleccionar "Inglés" por defecto
        int posicionIngles = buscarPosicionIdioma("Inglés");
        if (posicionIngles != -1) {
            spinnerTargetLanguage.setSelection(posicionIngles);
            idiomaDestino = idiomasDisponibles.get(posicionIngles).name;
            idiomaDestinoApiCode = idiomasDisponibles.get(posicionIngles).apiCode;
        } else if (nombresIdiomas.length > 0) {
            // Si no hay inglés, usar el primer idioma
            spinnerTargetLanguage.setSelection(0);
            idiomaDestino = idiomasDisponibles.get(0).name;
            idiomaDestinoApiCode = idiomasDisponibles.get(0).apiCode;
        }

        // Listener para el spinner
        spinnerTargetLanguage.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position < idiomasDisponibles.size()) {
                    LanguageDAO.Language idioma = idiomasDisponibles.get(position);
                    idiomaDestino = idioma.name;
                    idiomaDestinoApiCode = idioma.apiCode;
                    Log.d(TAG, "Idioma destino seleccionado: " + idiomaDestino + " (" + idiomaDestinoApiCode + ")");
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
        // Listener para el botón de traducir (simula tomar foto y traducir)
        btnConfirmTranslation.setOnClickListener(v -> {
            simularCapturaDeCamara();
        });

        // Listener para el botón de favoritos
        ivFavorite.setOnClickListener(v -> {
            if (textoExtraido.isEmpty()) {
                Toast.makeText(getContext(),
                        "Primero captura una imagen para traducir",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            esFavorito = !esFavorito;
            if (esFavorito) {
                ivFavorite.setImageResource(R.drawable.favorite);
                Toast.makeText(getContext(), "Agregado a favoritos", Toast.LENGTH_SHORT).show();
            } else {
                ivFavorite.setImageResource(R.drawable.ic_star_outline);
                Toast.makeText(getContext(), "Removido de favoritos", Toast.LENGTH_SHORT).show();
            }
        });

        // Listener para el botón de configuración
        menuButtonConfig.setOnClickListener(v -> {
            abrirConfiguracion();
        });
    }

    private void simularCapturaDeCamara() {
        Toast.makeText(getContext(), "Capturando imagen y extrayendo texto...",
                Toast.LENGTH_SHORT).show();

        // Simular extracción de texto con OCR
        textoExtraido = simularOCR();

        // Simular traducción
        String textoTraducido = simularTraduccion(textoExtraido, idiomaDestino);

        // Mostrar el texto traducido
        etTranslatedText.setText(textoTraducido);

        Toast.makeText(getContext(),
                "Texto extraído y traducido a " + idiomaDestino,
                Toast.LENGTH_SHORT).show();
    }

    private String simularOCR() {
        String[] textosSimulados = {
                "Hello World",
                "Welcome to our restaurant",
                "Exit only",
                "Open from 9 AM to 6 PM",
                "Please wash your hands",
                "No parking"
        };

        int indice = (int) (Math.random() * textosSimulados.length);
        return textosSimulados[indice];
    }

    private String simularTraduccion(String texto, String idiomaDestino) {
        // En producción, aquí usaríamos ML Kit con el apiCode correcto
        // Ejemplo: translator.translate(texto) usando idiomaDestinoApiCode
        return "[" + idiomaDestino + "] " + texto + " (traducción simulada desde cámara)";
    }

    private void abrirConfiguracion() {
        Configuracion configuracionFragment = new Configuracion();

        getParentFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, configuracionFragment)
                .addToBackStack(null)
                .commit();
    }
}