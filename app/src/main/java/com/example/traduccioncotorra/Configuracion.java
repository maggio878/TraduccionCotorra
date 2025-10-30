package com.example.traduccioncotorra;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatSpinner;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;
import com.google.android.material.button.MaterialButton;

public class Configuracion extends Fragment {

    private AppCompatSpinner spinnerIdiomaPrincipal;
    private AppCompatSpinner spinnerIdiomasPreferidos;
    private SwitchCompat switchModoOffline;
    private SwitchCompat switchSonidos;
    private MaterialButton btnEliminarHistorial;
    private MaterialButton btnInformacionApp;
    private MaterialButton btnCuenta;

    private String idiomaPrincipal = "Español";
    private boolean modoOffline = false;
    private boolean sonidosActivados = true;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_configuracion, container, false);

        // Inicializar vistas
        inicializarVistas(view);

        // Configurar spinners
        configurarSpinners();

        // Configurar switches
        configurarSwitches();

        // Configurar botones
        configurarBotones();

        return view;
    }

    private void inicializarVistas(View view) {
        spinnerIdiomaPrincipal = view.findViewById(R.id.spinner_idioma_principal);
        spinnerIdiomasPreferidos = view.findViewById(R.id.spinner_idiomas_preferidos);
        switchModoOffline = view.findViewById(R.id.switch_modo_offline);
        switchSonidos = view.findViewById(R.id.switch_sonidos);
        btnEliminarHistorial = view.findViewById(R.id.btn_eliminar_historial);
        btnInformacionApp = view.findViewById(R.id.btn_informacion_app);
        btnCuenta = view.findViewById(R.id.btn_cuenta);

    }

    private void configurarSpinners() {
        // Lista de idiomas disponibles
        String[] idiomas = {"Español", "Inglés", "Francés", "Alemán", "Italiano", "Portugués"};

        // Adapter para idioma principal
        ArrayAdapter<String> adapterPrincipal = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                idiomas
        );
        adapterPrincipal.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerIdiomaPrincipal.setAdapter(adapterPrincipal);
        spinnerIdiomaPrincipal.setSelection(0); // Español por defecto

        // Adapter para idiomas preferidos
        ArrayAdapter<String> adapterPreferidos = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                idiomas
        );
        adapterPreferidos.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerIdiomasPreferidos.setAdapter(adapterPreferidos);
        spinnerIdiomasPreferidos.setSelection(1); // Inglés por defecto

        // Listener para idioma principal
        spinnerIdiomaPrincipal.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                idiomaPrincipal = idiomas[position];
                Toast.makeText(getContext(),
                        "Idioma principal: " + idiomaPrincipal,
                        Toast.LENGTH_SHORT).show();
                // Aquí guardaríamos en base de datos
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Listener para idiomas preferidos
        spinnerIdiomasPreferidos.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String idiomaPreferido = idiomas[position];
                Toast.makeText(getContext(),
                        "Idioma preferido agregado: " + idiomaPreferido,
                        Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void configurarSwitches() {
        // Switch modo offline
        switchModoOffline.setChecked(modoOffline);
        switchModoOffline.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                modoOffline = isChecked;
                if (isChecked) {
                    Toast.makeText(getContext(),
                            "Modo offline activado",
                            Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getContext(),
                            "Modo offline desactivado",
                            Toast.LENGTH_SHORT).show();
                }
                // Guardar preferencia
            }
        });

        // Switch sonidos
        switchSonidos.setChecked(sonidosActivados);
        switchSonidos.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                sonidosActivados = isChecked;
                if (isChecked) {
                    Toast.makeText(getContext(),
                            "Sonidos activados",
                            Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getContext(),
                            "Sonidos desactivados",
                            Toast.LENGTH_SHORT).show();
                }
                // Guardar preferencia
            }
        });
    }

    private void configurarBotones() {

        // Botón eliminar historial
        btnEliminarHistorial.setOnClickListener(v -> {
            mostrarDialogoConfirmacion();
        });

        // Botón información de la app
        btnInformacionApp.setOnClickListener(v -> {
            Toast.makeText(getContext(),
                    "Traducción Cotorra v1.0\n" +
                            "Tecnológico Nacional de México\n" +
                            "Campus Nogales\n\n" +
                            "Desarrollado por:\n" +
                            "Regina Belem Perez Benítez\n" +
                            "Jose Mario Luque Fernandez",
                    Toast.LENGTH_LONG).show();
        });

        // Botón cuenta
        btnCuenta.setOnClickListener(v -> {
            Toast.makeText(getContext(),
                    "Configuración de cuenta\n" +
                            "Usuario: cotorra\n" +
                            "Email: cotorra@traduccion.com",
                    Toast.LENGTH_LONG).show();
        });
    }

    private void mostrarDialogoConfirmacion() {
        Toast.makeText(getContext(),
                "¿Estás seguro de eliminar el historial?\n" +
                        "Esta acción no se puede deshacer.",
                Toast.LENGTH_LONG).show();

        // Simular confirmación después de 2 segundos
        new android.os.Handler().postDelayed(() -> {
            Toast.makeText(getContext(),
                    "Historial eliminado correctamente",
                    Toast.LENGTH_SHORT).show();
        }, 2000);
    }
}