package com.example.traduccioncotorra;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.traduccioncotorra.DB.HistorialDAO;
import com.example.traduccioncotorra.DB.UserDAO;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class Historial extends Fragment {

    private EditText etBuscarHistorial;
    private ImageButton menuButtonConfig;
    private ViewGroup contenedorHistorial;
    private TextView noCoincidenciasMessage;

    private HistorialDAO historialDAO;
    private UserDAO userDAO;
    private List<HistorialDAO.HistorialItem> todasLasTraducciones;
    private List<HistorialDAO.HistorialItem> traduccionesFiltradas;
    private int userId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_historial, container, false);

        // Inicializar DAOs
        historialDAO = new HistorialDAO(requireContext());
        userDAO = new UserDAO(requireContext());

        // Obtener userId actual
        userId = userDAO.obtenerUserIdActual(requireContext());

        // Inicializar vistas
        inicializarVistas(view);

        // Cargar datos del historial
        cargarHistorial();

        // Configurar listeners
        configurarListeners();

        // Mostrar historial
        mostrarHistorial();

        return view;
    }

    private void inicializarVistas(View view) {
        etBuscarHistorial = view.findViewById(R.id.et_buscar_historial);
        menuButtonConfig = view.findViewById(R.id.menu_button_config);
        contenedorHistorial = view.findViewById(R.id.historial_container);
        noCoincidenciasMessage = view.findViewById(R.id.no_coincidences_message);
    }

    private void cargarHistorial() {
        if (userId != -1) {
            todasLasTraducciones = historialDAO.obtenerHistorialPorUsuario(userId);
            traduccionesFiltradas = todasLasTraducciones;
        } else {
            Toast.makeText(getContext(), "Error: Usuario no identificado", Toast.LENGTH_SHORT).show();
        }
    }

    private void configurarListeners() {
        // Botón de configuración
        if (menuButtonConfig != null) {
            menuButtonConfig.setOnClickListener(v -> mostrarMenuOpciones());
        }

        // Búsqueda en tiempo real
        if (etBuscarHistorial != null) {
            etBuscarHistorial.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    filtrarHistorial(s.toString());
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });
        }
    }

    private void mostrarHistorial() {
        if (contenedorHistorial == null) return;

        // Limpiar contenedor
        contenedorHistorial.removeAllViews();

        if (traduccionesFiltradas == null || traduccionesFiltradas.isEmpty()) {
            mostrarMensajeVacio(true);
            return;
        }

        mostrarMensajeVacio(false);

        // Crear un ScrollView para el contenedor
        ScrollView scrollView = new ScrollView(getContext());
        LinearLayout linearLayout = new LinearLayout(getContext());
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        scrollView.addView(linearLayout);
        contenedorHistorial.addView(scrollView);

        // Crear tarjetas para cada item del historial
        for (int i = 0; i < traduccionesFiltradas.size(); i++) {
            HistorialDAO.HistorialItem item = traduccionesFiltradas.get(i);
            View tarjeta = crearTarjetaHistorial(item, i);
            linearLayout.addView(tarjeta);
        }
    }

    private View crearTarjetaHistorial(HistorialDAO.HistorialItem item, int posicion) {
        // Crear contenedor principal
        LinearLayout tarjeta = new LinearLayout(getContext());
        tarjeta.setOrientation(LinearLayout.VERTICAL);
        tarjeta.setPadding(24, 24, 24, 24);
        tarjeta.setBackgroundResource(android.R.drawable.dialog_holo_light_frame);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(8, 8, 8, 8);
        tarjeta.setLayoutParams(params);

        // Header con idiomas y botón eliminar
        LinearLayout header = new LinearLayout(getContext());
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setPadding(0, 0, 0, 12);

        // Idiomas
        TextView tvIdiomas = new TextView(getContext());
        tvIdiomas.setText(item.idiomaOrigen + " → " + item.idiomaDestino);
        tvIdiomas.setTextSize(12);
        tvIdiomas.setTextColor(getResources().getColor(android.R.color.darker_gray));
        LinearLayout.LayoutParams idiomasParams = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
        );
        tvIdiomas.setLayoutParams(idiomasParams);
        header.addView(tvIdiomas);

        // Botón eliminar
        ImageButton btnEliminar = new ImageButton(getContext());
        btnEliminar.setImageResource(android.R.drawable.ic_menu_delete);
        btnEliminar.setBackgroundResource(android.R.drawable.btn_default);
        btnEliminar.setPadding(8, 8, 8, 8);
        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        btnEliminar.setLayoutParams(btnParams);
        btnEliminar.setOnClickListener(v -> confirmarEliminar(item));
        header.addView(btnEliminar);

        tarjeta.addView(header);

        // Texto original
        TextView tvOriginal = new TextView(getContext());
        tvOriginal.setText(item.textoOriginal);
        tvOriginal.setTextSize(16);
        tvOriginal.setTextColor(getResources().getColor(android.R.color.black));
        tvOriginal.setPadding(0, 8, 0, 4);
        tarjeta.addView(tvOriginal);

        // Texto traducido
        TextView tvTraducido = new TextView(getContext());
        tvTraducido.setText("→ " + item.textoTraducido);
        tvTraducido.setTextSize(16);
        tvTraducido.setTextColor(getResources().getColor(android.R.color.holo_blue_dark));
        tvTraducido.setPadding(0, 4, 0, 8);
        tarjeta.addView(tvTraducido);

        // Fecha
        TextView tvFecha = new TextView(getContext());
        tvFecha.setText(formatearFecha(item.fechaTraduccion));
        tvFecha.setTextSize(11);
        tvFecha.setTextColor(getResources().getColor(android.R.color.darker_gray));
        tvFecha.setAlpha(0.6f);
        tarjeta.addView(tvFecha);

        // Click en la tarjeta para copiar
        tarjeta.setOnClickListener(v -> {
            Toast.makeText(getContext(),
                    "Traducción copiada:\n" + item.textoTraducido,
                    Toast.LENGTH_SHORT).show();
        });

        return tarjeta;
    }

    private void filtrarHistorial(String busqueda) {
        if (busqueda.isEmpty()) {
            traduccionesFiltradas = todasLasTraducciones;
        } else {
            traduccionesFiltradas = historialDAO.buscarEnHistorial(userId, busqueda);
        }

        mostrarHistorial();
    }

    private void confirmarEliminar(HistorialDAO.HistorialItem item) {
        new AlertDialog.Builder(getContext())
                .setTitle("Eliminar del historial")
                .setMessage("¿Estás seguro de eliminar esta traducción del historial?")
                .setPositiveButton("Eliminar", (dialog, which) -> {
                    int resultado = historialDAO.eliminarHistorial(item.historyId);
                    if (resultado > 0) {
                        cargarHistorial();
                        mostrarHistorial();
                        Toast.makeText(getContext(),
                                "Traducción eliminada del historial",
                                Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getContext(),
                                "Error al eliminar",
                                Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void mostrarMenuOpciones() {
        String[] opciones = {
                "Eliminar todo el historial",
                "Exportar historial",
                "Ordenar por fecha"
        };

        new AlertDialog.Builder(getContext())
                .setTitle("Opciones de Historial")
                .setItems(opciones, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            confirmarEliminarTodo();
                            break;
                        case 1:
                            Toast.makeText(getContext(),
                                    "Exportar historial - Función en desarrollo",
                                    Toast.LENGTH_SHORT).show();
                            break;
                        case 2:
                            cargarHistorial();
                            mostrarHistorial();
                            Toast.makeText(getContext(), "Ordenado por fecha (más reciente primero)",
                                    Toast.LENGTH_SHORT).show();
                            break;
                    }
                })
                .show();
    }

    private void confirmarEliminarTodo() {
        new AlertDialog.Builder(getContext())
                .setTitle("Eliminar todo")
                .setMessage("¿Estás seguro de eliminar TODO el historial? Esta acción no se puede deshacer.")
                .setPositiveButton("Eliminar todo", (dialog, which) -> {
                    int resultado = historialDAO.eliminarTodoHistorial(userId);
                    if (resultado > 0) {
                        cargarHistorial();
                        mostrarHistorial();
                        Toast.makeText(getContext(),
                                "Todo el historial ha sido eliminado",
                                Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void mostrarMensajeVacio(boolean mostrar) {
        if (noCoincidenciasMessage != null) {
            noCoincidenciasMessage.setVisibility(mostrar ? View.VISIBLE : View.GONE);
            if (mostrar) {
                noCoincidenciasMessage.setText("No hay traducciones en el historial");
            }
        }
    }

    private String formatearFecha(String fechaString) {
        try {
            SimpleDateFormat sdfInput = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            Date fecha = sdfInput.parse(fechaString);

            if (fecha != null) {
                long diferencia = System.currentTimeMillis() - fecha.getTime();
                long segundos = diferencia / 1000;
                long minutos = segundos / 60;
                long horas = minutos / 60;
                long dias = horas / 24;

                if (dias > 0) {
                    return "Hace " + dias + (dias == 1 ? " día" : " días");
                } else if (horas > 0) {
                    return "Hace " + horas + (horas == 1 ? " hora" : " horas");
                } else if (minutos > 0) {
                    return "Hace " + minutos + (minutos == 1 ? " minuto" : " minutos");
                } else {
                    return "Hace unos segundos";
                }
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return fechaString;
    }
}
