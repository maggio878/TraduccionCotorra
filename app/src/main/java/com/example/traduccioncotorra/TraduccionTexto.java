package com.example.traduccioncotorra;

import android.os.Bundle;
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

public class TraduccionTexto extends Fragment {

    private EditText txtTextoIngresado;
    private TextView txtvTextoTraducido;
    private AppCompatSpinner spinnerSourceLanguage;
    private AppCompatSpinner spinnerResultLanguage;
    private ImageButton btnFavorite;
    private ImageButton menuButtonConfig;

    private String idiomaOrigen = "Español";
    private String idiomaDestino = "Inglés";
    private boolean esFavorito = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Inflar el layout del fragment
        View view = inflater.inflate(R.layout.fragment_traduccion_texto, container, false);

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

    private void inicializarVistas(View view) {
        txtTextoIngresado = view.findViewById(R.id.txt_TextoIngresado);
        txtvTextoTraducido = view.findViewById(R.id.txtv_TextoTraducido);
        spinnerSourceLanguage = view.findViewById(R.id.spinner_source_language);
        spinnerResultLanguage = view.findViewById(R.id.spinner_result_language);
        btnFavorite = view.findViewById(R.id.btn_favorite);
        menuButtonConfig = view.findViewById(R.id.menu_button_config);
    }

    private void configurarSpinners() {
        // Lista de idiomas disponibles
        String[] idiomas = {"Español", "Inglés", "Francés", "Alemán", "Italiano", "Portugués"};

        // Crear adapter para los spinners
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                idiomas
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        // Configurar spinner de idioma origen
        spinnerSourceLanguage.setAdapter(adapter);
        spinnerSourceLanguage.setSelection(0); // Español por defecto

        // Configurar spinner de idioma destino
        spinnerResultLanguage.setAdapter(adapter);
        spinnerResultLanguage.setSelection(1); // Inglés por defecto

        // Listeners para los spinners
        spinnerSourceLanguage.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                idiomaOrigen = idiomas[position];
                traducirTexto();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        spinnerResultLanguage.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                idiomaDestino = idiomas[position];
                traducirTexto();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void configurarListeners() {
        // Listener para el botón de favoritos
        btnFavorite.setOnClickListener(v -> {
            esFavorito = !esFavorito;
            if (esFavorito) {
                btnFavorite.setImageResource(R.drawable.favorite); // Asume que tienes este drawable
                Toast.makeText(getContext(), "Agregado a favoritos", Toast.LENGTH_SHORT).show();
            } else {
                btnFavorite.setImageResource(R.drawable.ic_star_outline);
                Toast.makeText(getContext(), "Removido de favoritos", Toast.LENGTH_SHORT).show();
            }
        });
        menuButtonConfig.setOnClickListener(v -> {
            abrirConfiguracion();
        });

        // Traducir mientras el usuario escribe (opcional, puedes comentar si prefieres un botón)
        txtTextoIngresado.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                traducirTexto();
            }
        });
    }

    private void traducirTexto() {
        String textoOriginal = txtTextoIngresado.getText().toString().trim();

        if (textoOriginal.isEmpty()) {
            txtvTextoTraducido.setText("");
            return;
        }

        // Validar que los idiomas sean diferentes
        if (idiomaOrigen.equals(idiomaDestino)) {
            txtvTextoTraducido.setText(textoOriginal);
            Toast.makeText(getContext(),
                    "Los idiomas de origen y destino son iguales",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        // Simulación de traducción (sin API real)
        String textoTraducido = simularTraduccion(textoOriginal, idiomaOrigen, idiomaDestino);
        txtvTextoTraducido.setText(textoTraducido);
    }

    private String simularTraduccion(String texto, String origen, String destino) {
        // Esta es una simulación simple para demostración
        // En un proyecto real, aquí usarías una API de traducción

        return "[" + destino + "] " + texto + " (traducción simulada)";
    }
    private void abrirConfiguracion() {
        // Navegar al fragment de configuración
        Configuracion configuracionFragment = new Configuracion();

        getParentFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, configuracionFragment)
                .addToBackStack(null) // Permite volver atrás
                .commit();
    }
}