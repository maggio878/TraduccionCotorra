package com.example.traduccioncotorra;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatSpinner;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;
import com.example.traduccioncotorra.DB.HistorialDAO;
import com.example.traduccioncotorra.DB.UserDAO;
import com.example.traduccioncotorra.DB.LanguageDAO;
import com.google.android.material.button.MaterialButton;
import java.util.List;

public class Configuracion extends Fragment {

    private AppCompatSpinner spinnerIdiomaPrincipal;
    private AppCompatSpinner spinnerIdiomasPreferidos;
    private SwitchCompat switchModoOffline;
    private SwitchCompat switchSonidos;
    private MaterialButton btnEliminarHistorial;
    private MaterialButton btnVerHistorial;
    private MaterialButton btnCambiarContrasena;
    private MaterialButton btnEliminarCuenta;
    private MaterialButton btnInformacionApp;
    private MaterialButton btnCuenta;
    private MaterialButton btnCerrarSesion;
    private MaterialButton btnAdminCatalogos;

    private String idiomaPrincipal = "Español";
    private boolean modoOffline = false;
    private boolean sonidosActivados = true;


    private UserDAO userDAO;
    private HistorialDAO historialDAO;
    private LanguageDAO languageDAO;
    private int userId;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_configuracion, container, false);

        // Inicializar DAOs
        userDAO = new UserDAO(requireContext());
        historialDAO = new HistorialDAO(requireContext());
        languageDAO = new LanguageDAO(requireContext());

        // Obtener userId actual
        userId = userDAO.obtenerUserIdActual(requireContext());

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
        btnVerHistorial = view.findViewById(R.id.btn_ver_historial);
        btnCambiarContrasena = view.findViewById(R.id.btn_cambiar_contrasena);
        btnEliminarCuenta = view.findViewById(R.id.btn_eliminar_cuenta);
        btnInformacionApp = view.findViewById(R.id.btn_informacion_app);
        btnCuenta = view.findViewById(R.id.btn_cuenta);
        btnCerrarSesion = view.findViewById(R.id.btn_cerrar_sesion);
        btnAdminCatalogos = view.findViewById(R.id.btn_admin_catalogos);

    }

    private void configurarSpinners() {
        // Obtener idiomas de la base de datos
        List<LanguageDAO.Language> idiomasDB = languageDAO.obtenerIdiomasActivos();

        // Si no hay idiomas, usar array por defecto
        String[] idiomas;
        if (idiomasDB.isEmpty()) {
            idiomas = new String[]{"Español", "Inglés", "Francés", "Alemán", "Italiano", "Portugués"};
            Toast.makeText(getContext(),
                    "⚠️ No hay idiomas en BD. Usando idiomas por defecto",
                    Toast.LENGTH_SHORT).show();
        } else {
            // Convertir lista de idiomas a array
            idiomas = new String[idiomasDB.size()];
            for (int i = 0; i < idiomasDB.size(); i++) {
                idiomas[i] = idiomasDB.get(i).name;
            }
        }

        // Adapter para idioma principal
        ArrayAdapter<String> adapterPrincipal = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                idiomas
        );
        adapterPrincipal.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerIdiomaPrincipal.setAdapter(adapterPrincipal);
        spinnerIdiomaPrincipal.setSelection(0);

        // Adapter para idiomas preferidos
        ArrayAdapter<String> adapterPreferidos = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                idiomas
        );
        adapterPreferidos.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerIdiomasPreferidos.setAdapter(adapterPreferidos);
        spinnerIdiomasPreferidos.setSelection(1);

        // Listener para idioma principal
        spinnerIdiomaPrincipal.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                idiomaPrincipal = idiomas[position];
                Toast.makeText(getContext(),
                        "Idioma principal: " + idiomaPrincipal,
                        Toast.LENGTH_SHORT).show();
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

        btnVerHistorial.setOnClickListener(v -> {
            abrirHistorial();
        });

        // Botón eliminar historial
        btnEliminarHistorial.setOnClickListener(v -> {
            mostrarDialogoEliminarHistorial();
        });

        // Botón cambiar contraseña
        btnCambiarContrasena.setOnClickListener(v -> {
            mostrarDialogoCambiarContrasena();
        });

        // Botón eliminar cuenta
        btnEliminarCuenta.setOnClickListener(v -> {
            mostrarDialogoEliminarCuenta();
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
            mostrarInformacionCuenta();
        });

        // Botón cerrar sesión
        btnCerrarSesion.setOnClickListener(v -> {
            cerrarSesion();
        });
        btnAdminCatalogos.setOnClickListener(v -> {
            abrirAdminCatalogos();
        });
    }
    private void abrirHistorial() {
        Historial historialFragment = new Historial();

        getParentFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, historialFragment)
                .addToBackStack(null)
                .commit();
    }
    private void mostrarDialogoEliminarHistorial() {
        new AlertDialog.Builder(getContext())
                .setTitle("Eliminar historial")
                .setMessage("¿Estás seguro de eliminar todo el historial de traducciones?\n" +
                        "Esta acción no se puede deshacer.")
                .setPositiveButton("Eliminar", (dialog, which) -> {
                    int resultado = historialDAO.eliminarTodoHistorial(userId);
                    if (resultado > 0) {
                        Toast.makeText(getContext(),
                                "Historial eliminado correctamente (" + resultado + " elementos)",
                                Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getContext(),
                                "No hay historial para eliminar",
                                Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }
    private void mostrarDialogoCambiarContrasena() {
        // Crear layout del diálogo
        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 20, 50, 20);

        // Campo contraseña actual
        final EditText etContrasenaActual = new EditText(getContext());
        etContrasenaActual.setHint("Contraseña actual");
        etContrasenaActual.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(etContrasenaActual);

        // Campo nueva contraseña
        final EditText etNuevaContrasena = new EditText(getContext());
        etNuevaContrasena.setHint("Nueva contraseña");
        etNuevaContrasena.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(etNuevaContrasena);

        // Campo confirmar nueva contraseña
        final EditText etConfirmarContrasena = new EditText(getContext());
        etConfirmarContrasena.setHint("Confirmar nueva contraseña");
        etConfirmarContrasena.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(etConfirmarContrasena);

        new AlertDialog.Builder(getContext())
                .setTitle("Cambiar contraseña")
                .setView(layout)
                .setPositiveButton("Cambiar", (dialog, which) -> {
                    String contrasenaActual = etContrasenaActual.getText().toString();
                    String nuevaContrasena = etNuevaContrasena.getText().toString();
                    String confirmarContrasena = etConfirmarContrasena.getText().toString();

                    // Validaciones
                    if (contrasenaActual.isEmpty() || nuevaContrasena.isEmpty() || confirmarContrasena.isEmpty()) {
                        Toast.makeText(getContext(), "Todos los campos son requeridos", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (nuevaContrasena.length() < 6) {
                        Toast.makeText(getContext(), "La nueva contraseña debe tener al menos 6 caracteres", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (!nuevaContrasena.equals(confirmarContrasena)) {
                        Toast.makeText(getContext(), "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Intentar cambiar la contraseña
                    boolean resultado = userDAO.cambiarContrasena(userId, contrasenaActual, nuevaContrasena);

                    if (resultado) {
                        Toast.makeText(getContext(), "Contraseña cambiada exitosamente", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(getContext(), "Error: La contraseña actual es incorrecta", Toast.LENGTH_LONG).show();
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }
    private void mostrarDialogoEliminarCuenta() {
        // Crear layout del diálogo
        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 20, 50, 20);

        final EditText etContrasena = new EditText(getContext());
        etContrasena.setHint("Confirma tu contraseña");
        etContrasena.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(etContrasena);

        new AlertDialog.Builder(getContext())
                .setTitle("Eliminar cuenta")
                .setMessage("⚠️ ADVERTENCIA ⚠️\n\nEsta acción eliminará permanentemente tu cuenta y todos tus datos.\n\n" +
                        "Esta acción NO se puede deshacer.\n\nPara continuar, ingresa tu contraseña:")
                .setView(layout)
                .setPositiveButton("Eliminar permanentemente", (dialog, which) -> {
                    String contrasena = etContrasena.getText().toString();

                    if (contrasena.isEmpty()) {
                        Toast.makeText(getContext(), "Debes ingresar tu contraseña", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Intentar eliminar la cuenta
                    boolean resultado = userDAO.eliminarUsuarioPermanente(userId, contrasena);

                    if (resultado) {
                        Toast.makeText(getContext(), "Cuenta eliminada exitosamente", Toast.LENGTH_SHORT).show();

                        // Cerrar sesión y volver al login
                        userDAO.cerrarSesion(requireContext());
                        Intent intent = new Intent(getActivity(), MainActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        requireActivity().finish();
                    } else {
                        Toast.makeText(getContext(), "Error: Contraseña incorrecta", Toast.LENGTH_LONG).show();
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void mostrarInformacionCuenta() {
        if (userId != -1) {
            com.example.traduccioncotorra.Models.Usuario usuario = userDAO.obtenerUsuarioPorId(userId);
            if (usuario != null) {
                Toast.makeText(getContext(),
                        "Mi Cuenta\n\n" +
                                "Usuario: " + usuario.getUsername() + "\n" +
                                "Email: " + usuario.getEmail() + "\n" +
                                "Nombre: " + usuario.getFullName() + "\n" +
                                "Miembro desde: " + usuario.getCreatedDate(),
                        Toast.LENGTH_LONG).show();
            }
        } else {
            Toast.makeText(getContext(), "Error al obtener información de la cuenta", Toast.LENGTH_SHORT).show();
        }
    }

    private void cerrarSesion() {
        new AlertDialog.Builder(getContext())
                .setTitle("Cerrar sesión")
                .setMessage("¿Estás seguro que deseas cerrar sesión?")
                .setPositiveButton("Sí, cerrar sesión", (dialog, which) -> {
                    userDAO.cerrarSesion(requireContext());

                    Toast.makeText(getContext(), "Sesión cerrada", Toast.LENGTH_SHORT).show();

                    Intent intent = new Intent(getActivity(), MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    requireActivity().finish();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }
    private void abrirAdminCatalogos() {
        AdminCatalogos adminCatalogosFragment = new AdminCatalogos();

        getParentFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, adminCatalogosFragment)
                .addToBackStack(null)
                .commit();
    }
}