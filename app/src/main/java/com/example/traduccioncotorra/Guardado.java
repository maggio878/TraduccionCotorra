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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;


public class Guardado extends Fragment {

    private EditText etBuscarFavorito;
    private ImageButton menuButtonConfig;
    private ViewGroup contenedorFavoritos;
    private TextView noCoincidenciasMessage;

    private List<TraduccionFavorita> todasLasTraducciones;
    private List<TraduccionFavorita> traduccionesFiltradas;
    // Clase interna para representar una traducción favorita
    public static class TraduccionFavorita {
        String textoOriginal;
        String textoTraducido;
        String idiomaOrigen;
        String idiomaDestino;
        long fechaGuardado;

        TraduccionFavorita(String original, String traducido, String origen,
                           String destino, long fecha) {
            this.textoOriginal = original;
            this.textoTraducido = traducido;
            this.idiomaOrigen = origen;
            this.idiomaDestino = destino;
            this.fechaGuardado = fecha;
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_guardados, container, false);

        // Inicializar vistas
        inicializarVistas(view);

        // Cargar datos de ejemplo
        cargarDatosEjemplo();

        // Configurar listeners
        configurarListeners();

        // Mostrar favoritos
        mostrarFavoritos();

        return view;
    }

    private void inicializarVistas(View view) {
        etBuscarFavorito = view.findViewById(R.id.et_buscar_favorito);
        menuButtonConfig = view.findViewById(R.id.menu_button_config);
        contenedorFavoritos = view.findViewById(R.id.favorito_container);
        noCoincidenciasMessage = view.findViewById(R.id.no_coincidences_message);
    }

    private void cargarDatosEjemplo() {
        todasLasTraducciones = new ArrayList<>();

        // Agregar traducciones de ejemplo
        todasLasTraducciones.add(new TraduccionFavorita(
                "Hello, how are you?",
                "Hola, ¿cómo estás?",
                "Inglés",
                "Español",
                System.currentTimeMillis() - 3600000 // Hace 1 hora
        ));

        todasLasTraducciones.add(new TraduccionFavorita(
                "Good morning",
                "Buenos días",
                "Inglés",
                "Español",
                System.currentTimeMillis() - 7200000 // Hace 2 horas
        ));

        todasLasTraducciones.add(new TraduccionFavorita(
                "Thank you very much",
                "Muchas gracias",
                "Inglés",
                "Español",
                System.currentTimeMillis() - 86400000 // Hace 1 día
        ));

        todasLasTraducciones.add(new TraduccionFavorita(
                "Where is the bathroom?",
                "¿Dónde está el baño?",
                "Inglés",
                "Español",
                System.currentTimeMillis() - 172800000 // Hace 2 días
        ));

        todasLasTraducciones.add(new TraduccionFavorita(
                "I don't understand",
                "No entiendo",
                "Inglés",
                "Español",
                System.currentTimeMillis() - 259200000 // Hace 3 días
        ));

        todasLasTraducciones.add(new TraduccionFavorita(
                "Bonjour, comment allez-vous?",
                "Hola, ¿cómo está usted?",
                "Francés",
                "Español",
                System.currentTimeMillis() - 345600000 // Hace 4 días
        ));

        // Inicializar lista filtrada
        traduccionesFiltradas = new ArrayList<>(todasLasTraducciones);
    }

    private void configurarListeners() {
        // Botón de configuración
        if (menuButtonConfig != null) {
            menuButtonConfig.setOnClickListener(v -> mostrarMenuOpciones());
        }

        // Búsqueda en tiempo real
        if (etBuscarFavorito != null) {
            etBuscarFavorito.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    filtrarFavoritos(s.toString());
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });
        }
    }

    private void mostrarFavoritos() {
        if (contenedorFavoritos == null) return;

        // Limpiar contenedor
        contenedorFavoritos.removeAllViews();

        if (traduccionesFiltradas.isEmpty()) {
            mostrarMensajeVacio(true);
            return;
        }

        mostrarMensajeVacio(false);

        // Crear un ScrollView para el contenedor
        ScrollView scrollView = new ScrollView(getContext());
        LinearLayout linearLayout = new LinearLayout(getContext());
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        scrollView.addView(linearLayout);
        contenedorFavoritos.addView(scrollView);

        // Crear tarjetas para cada favorito
        for (int i = 0; i < traduccionesFiltradas.size(); i++) {
            TraduccionFavorita favorito = traduccionesFiltradas.get(i);
            View tarjeta = crearTarjetaFavorito(favorito, i);
            linearLayout.addView(tarjeta);
        }
    }

    private View crearTarjetaFavorito(TraduccionFavorita favorito, int posicion) {
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
        tvIdiomas.setText(favorito.idiomaOrigen + " → " + favorito.idiomaDestino);
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
        btnEliminar.setOnClickListener(v -> confirmarEliminar(favorito));
        header.addView(btnEliminar);

        tarjeta.addView(header);

        // Texto original
        TextView tvOriginal = new TextView(getContext());
        tvOriginal.setText(favorito.textoOriginal);
        tvOriginal.setTextSize(16);
        tvOriginal.setTextColor(getResources().getColor(android.R.color.black));
        tvOriginal.setPadding(0, 8, 0, 4);
        tarjeta.addView(tvOriginal);

        // Texto traducido
        TextView tvTraducido = new TextView(getContext());
        tvTraducido.setText("→ " + favorito.textoTraducido);
        tvTraducido.setTextSize(16);
        tvTraducido.setTextColor(getResources().getColor(android.R.color.holo_blue_dark));
        tvTraducido.setPadding(0, 4, 0, 8);
        tarjeta.addView(tvTraducido);

        // Fecha
        TextView tvFecha = new TextView(getContext());
        tvFecha.setText(formatearFecha(favorito.fechaGuardado));
        tvFecha.setTextSize(11);
        tvFecha.setTextColor(getResources().getColor(android.R.color.darker_gray));
        tvFecha.setAlpha(0.6f);
        tarjeta.addView(tvFecha);

        // Click en la tarjeta para copiar
        tarjeta.setOnClickListener(v -> {
            Toast.makeText(getContext(),
                    "Traducción copiada:\n" + favorito.textoTraducido,
                    Toast.LENGTH_SHORT).show();
        });

        return tarjeta;
    }

    private void filtrarFavoritos(String busqueda) {
        if (busqueda.isEmpty()) {
            traduccionesFiltradas = new ArrayList<>(todasLasTraducciones);
        } else {
            traduccionesFiltradas.clear();
            String busquedaLower = busqueda.toLowerCase();

            for (TraduccionFavorita traduccion : todasLasTraducciones) {
                if (traduccion.textoOriginal.toLowerCase().contains(busquedaLower) ||
                        traduccion.textoTraducido.toLowerCase().contains(busquedaLower) ||
                        traduccion.idiomaOrigen.toLowerCase().contains(busquedaLower) ||
                        traduccion.idiomaDestino.toLowerCase().contains(busquedaLower)) {
                    traduccionesFiltradas.add(traduccion);
                }
            }
        }

        mostrarFavoritos();
    }

    private void confirmarEliminar(TraduccionFavorita favorito) {
        new AlertDialog.Builder(getContext())
                .setTitle("Eliminar favorito")
                .setMessage("¿Estás seguro de eliminar esta traducción de favoritos?")
                .setPositiveButton("Eliminar", (dialog, which) -> {
                    todasLasTraducciones.remove(favorito);
                    traduccionesFiltradas.remove(favorito);
                    mostrarFavoritos();
                    Toast.makeText(getContext(),
                            "Traducción eliminada de favoritos",
                            Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void mostrarMenuOpciones() {
        String[] opciones = {
                "Eliminar todos los favoritos",
                "Exportar favoritos",
                "Ordenar por fecha",
                "Ordenar por idioma"
        };

        new AlertDialog.Builder(getContext())
                .setTitle("Opciones de Favoritos")
                .setItems(opciones, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            confirmarEliminarTodos();
                            break;
                        case 1:
                            Toast.makeText(getContext(),
                                    "Exportar favoritos - Función en desarrollo",
                                    Toast.LENGTH_SHORT).show();
                            break;
                        case 2:
                            ordenarPorFecha();
                            break;
                        case 3:
                            ordenarPorIdioma();
                            break;
                    }
                })
                .show();
    }

    private void confirmarEliminarTodos() {
        new AlertDialog.Builder(getContext())
                .setTitle("Eliminar todos")
                .setMessage("¿Estás seguro de eliminar TODOS los favoritos? Esta acción no se puede deshacer.")
                .setPositiveButton("Eliminar todo", (dialog, which) -> {
                    todasLasTraducciones.clear();
                    traduccionesFiltradas.clear();
                    mostrarFavoritos();
                    Toast.makeText(getContext(),
                            "Todos los favoritos han sido eliminados",
                            Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void ordenarPorFecha() {
        traduccionesFiltradas.sort((t1, t2) ->
                Long.compare(t2.fechaGuardado, t1.fechaGuardado));
        mostrarFavoritos();
        Toast.makeText(getContext(), "Ordenado por fecha (más reciente primero)",
                Toast.LENGTH_SHORT).show();
    }

    private void ordenarPorIdioma() {
        traduccionesFiltradas.sort((t1, t2) -> {
            int comparacion = t1.idiomaOrigen.compareTo(t2.idiomaOrigen);
            if (comparacion == 0) {
                return t1.idiomaDestino.compareTo(t2.idiomaDestino);
            }
            return comparacion;
        });
        mostrarFavoritos();
        Toast.makeText(getContext(), "Ordenado por idioma",
                Toast.LENGTH_SHORT).show();
    }

    private void mostrarMensajeVacio(boolean mostrar) {
        if (noCoincidenciasMessage != null) {
            noCoincidenciasMessage.setVisibility(mostrar ? View.VISIBLE : View.GONE);
        }
    }

    private String formatearFecha(long timestamp) {
        long diferencia = System.currentTimeMillis() - timestamp;
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

    // Método público para agregar nuevos favoritos desde otros fragments
    public void agregarFavorito(String textoOriginal, String textoTraducido,
                                String idiomaOrigen, String idiomaDestino) {
        TraduccionFavorita nueva = new TraduccionFavorita(
                textoOriginal,
                textoTraducido,
                idiomaOrigen,
                idiomaDestino,
                System.currentTimeMillis()
        );

        todasLasTraducciones.add(0, nueva); // Agregar al inicio
        traduccionesFiltradas = new ArrayList<>(todasLasTraducciones);
        mostrarFavoritos();

        Toast.makeText(getContext(),
                "Traducción agregada a favoritos",
                Toast.LENGTH_SHORT).show();
    }


}