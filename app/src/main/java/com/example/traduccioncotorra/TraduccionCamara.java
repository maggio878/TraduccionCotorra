package com.example.traduccioncotorra;

import android.os.Bundle;
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
import com.google.android.material.button.MaterialButton;

public class TraduccionCamara extends Fragment {

    private EditText etTranslatedText;
    private ImageView ivFavorite;
    private AppCompatSpinner spinnerTargetLanguage;
    private MaterialButton btnConfirmTranslation;
    private ImageButton menuButtonConfig;

    private String idiomaDestino = "Inglés";
    private boolean esFavorito = false;
    private String textoExtraido = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_traduccion_camara, container, false);

        // Inicializar vistas
        inicializarVistas(view);

        // Configurar spinner
        configurarSpinner();

        // Configurar listeners
        configurarListeners();

        return view;
    }

    private void inicializarVistas(View view) {
        etTranslatedText = view.findViewById(R.id.etTranslatedText);
        ivFavorite = view.findViewById(R.id.ivFavorite);
        spinnerTargetLanguage = view.findViewById(R.id.spinner_target_language);
        btnConfirmTranslation = view.findViewById(R.id.btn_confirm_translation);
        menuButtonConfig = view.findViewById(R.id.menu_button_config);
    }

    private void configurarSpinner() {
        // Lista de idiomas disponibles
        String[] idiomas = {"Español", "Inglés", "Francés", "Alemán", "Italiano", "Portugués"};

        // Crear adapter para el spinner
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                idiomas
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        // Configurar spinner de idioma destino
        spinnerTargetLanguage.setAdapter(adapter);
        spinnerTargetLanguage.setSelection(1); // Inglés por defecto

        // Listener para el spinner
        spinnerTargetLanguage.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                idiomaDestino = idiomas[position];
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
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
            Toast.makeText(getContext(), "Menú de configuración", Toast.LENGTH_SHORT).show();
        });
    }

    private void simularCapturaDeCamara() {
        // Mostrar mensaje de que se está procesando
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
        // Simulación de texto extraído de una imagen
        // En producción, aquí usarías ML Kit de Google o Tesseract OCR
        String[] textosSimulados = {
                "Hello World",
                "Welcome to our restaurant",
                "Exit only",
                "Open from 9 AM to 6 PM",
                "Please wash your hands",
                "No parking"
        };

        // Seleccionar un texto aleatorio
        int indice = (int) (Math.random() * textosSimulados.length);
        return textosSimulados[indice];
    }

    private String simularTraduccion(String texto, String idiomaDestino) {
        // Simulación simple de traducción
        // En producción, aquí usarías Google Translate API o similar
        return "[" + idiomaDestino + "] " + texto + " (traducción simulada desde cámara)";
    }
}