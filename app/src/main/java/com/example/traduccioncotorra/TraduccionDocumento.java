package com.example.traduccioncotorra;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class TraduccionDocumento extends Fragment {

    private EditText etBuscarDocumentos;
    private ImageButton menuButtonConfig;
    private LinearLayout[] documentosRecientes;

    // Simulación de documentos recientes
    private String[] nombresDocumentos = {
            "Contrato.pdf",
            "Recibo.jpg",
            "Notas.txt",
            "Presentación.pptx"
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_traduccion_documento, container, false);

        // Inicializar vistas
        inicializarVistas(view);

        // Configurar listeners
        configurarListeners();

        return view;
    }

    private void inicializarVistas(View view) {
        etBuscarDocumentos = view.findViewById(R.id.et_buscar_documentos);
        menuButtonConfig = view.findViewById(R.id.menu_button_config);

    }

    private void configurarListeners() {
        // Listener para el botón de configuración
        menuButtonConfig.setOnClickListener(v -> {
            abrirConfiguracion();
        });

        // Listener para el campo de búsqueda
        etBuscarDocumentos.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filtrarDocumentos(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Configurar clicks en los documentos recientes
        configurarClicksDocumentos();
    }

    private void configurarClicksDocumentos() {
        // Simular clicks en los documentos
        // En una implementación real, estos serían generados dinámicamente
        View view = getView();
        if (view != null) {
            // Buscar todos los LinearLayout que son documentos
            View gridLayout = view.findViewById(R.id.recent_documents_container);
            if (gridLayout != null) {
                // Configurar clicks simulados para los documentos
                configurarClickDocumento(view, "Contrato.pdf",
                        "Este es el contenido simulado del contrato en español...");
                configurarClickDocumento(view, "Recibo.jpg",
                        "Receipt - Total: $150.00 - Thank you for your purchase");
                configurarClickDocumento(view, "Notas.txt",
                        "Reunión el lunes a las 10:00 AM en la sala principal");
                configurarClickDocumento(view, "Presentación.pptx",
                        "Slide 1: Introduction to our new product line");
            }
        }
    }

    private void configurarClickDocumento(View parentView, String nombreDoc, String contenido) {
        Toast.makeText(getContext(),
                "Documentos listos para traducir. Toca un documento para abrirlo.",
                Toast.LENGTH_SHORT).show();
    }

    private void filtrarDocumentos(String query) {
        if (query.isEmpty()) {
            // Mostrar todos los documentos
            Toast.makeText(getContext(),
                    "Mostrando todos los documentos",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        // Simular filtrado
        boolean hayCoincidencias = false;
        for (String documento : nombresDocumentos) {
            if (documento.toLowerCase().contains(query.toLowerCase())) {
                hayCoincidencias = true;
                break;
            }
        }

        if (hayCoincidencias) {
            Toast.makeText(getContext(),
                    "Filtrando documentos que contienen: " + query,
                    Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getContext(),
                    "No se encontraron documentos con: " + query,
                    Toast.LENGTH_SHORT).show();
        }
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