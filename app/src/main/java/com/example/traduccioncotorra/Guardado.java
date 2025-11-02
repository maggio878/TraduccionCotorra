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
import com.example.traduccioncotorra.DB.FavoriteTranslationDAO;
import com.example.traduccioncotorra.DB.LanguageDAO;
import com.example.traduccioncotorra.DB.TranslationTypeDAO;
import com.example.traduccioncotorra.DB.UserDAO;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class Guardado extends Fragment {

    private EditText etBuscarFavorito;
    private ImageButton menuButtonConfig;
    private ViewGroup contenedorFavoritos;
    private TextView noCoincidenciasMessage;

    // DAOs
    private FavoriteTranslationDAO favoriteDAO;
    private LanguageDAO languageDAO;
    private TranslationTypeDAO translationTypeDAO;
    private UserDAO userDAO;
    private int userId;

    private List<FavoriteTranslationDAO.FavoriteTranslation> todasLasTraducciones;
    private List<FavoriteTranslationDAO.FavoriteTranslation> traduccionesFiltradas;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_guardados, container, false);

        // Inicializar DAOs
        favoriteDAO = new FavoriteTranslationDAO(requireContext());
        languageDAO = new LanguageDAO(requireContext());
        translationTypeDAO = new TranslationTypeDAO(requireContext());
        userDAO = new UserDAO(requireContext());

        // Obtener userId actual
        userId = userDAO.obtenerUserIdActual(requireContext());

        // Inicializar vistas
        inicializarVistas(view);

        // Cargar favoritos desde la BD
        cargarFavoritosDesdeDB();

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

    private void cargarFavoritosDesdeDB() {
        if (userId != -1) {
            todasLasTraducciones = favoriteDAO.obtenerFavoritosPorUsuario(userId);
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
        contenedorFavoritos.addView(scrollView);

        // Crear tarjetas para cada favorito
        for (FavoriteTranslationDAO.FavoriteTranslation favorito : traduccionesFiltradas) {
            View tarjeta = crearTarjetaFavorito(favorito);
            linearLayout.addView(tarjeta);
        }
    }

    private View crearTarjetaFavorito(FavoriteTranslationDAO.FavoriteTranslation favorito) {
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

        // Obtener nombres de idiomas
        String idiomaOrigen = obtenerNombreIdioma(favorito.sourceLanguageId);
        String idiomaDestino = obtenerNombreIdioma(favorito.targetLanguageId);

        // Idiomas
        TextView tvIdiomas = new TextView(getContext());
        tvIdiomas.setText(idiomaOrigen + " → " + idiomaDestino);
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
        tvOriginal.setText(favorito.originalText);
        tvOriginal.setTextSize(16);
        tvOriginal.setTextColor(getResources().getColor(android.R.color.black));
        tvOriginal.setPadding(0, 8, 0, 4);
        tarjeta.addView(tvOriginal);

        // Texto traducido
        TextView tvTraducido = new TextView(getContext());
        tvTraducido.setText("→ " + favorito.translatedText);
        tvTraducido.setTextSize(16);
        tvTraducido.setTextColor(getResources().getColor(android.R.color.holo_blue_dark));
        tvTraducido.setPadding(0, 4, 0, 8);
        tarjeta.addView(tvTraducido);

        // Fecha
        TextView tvFecha = new TextView(getContext());
        tvFecha.setText(formatearFecha(favorito.savedDate));
        tvFecha.setTextSize(11);
        tvFecha.setTextColor(getResources().getColor(android.R.color.darker_gray));
        tvFecha.setAlpha(0.6f);
        tarjeta.addView(tvFecha);

        // Click en la tarjeta para copiar
        tarjeta.setOnClickListener(v -> {
            Toast.makeText(getContext(),
                    "Traducción copiada:\n" + favorito.translatedText,
                    Toast.LENGTH_SHORT).show();
        });

        return tarjeta;
    }

    private void filtrarFavoritos(String busqueda) {
        if (busqueda.isEmpty()) {
            traduccionesFiltradas = todasLasTraducciones;
        } else {
            traduccionesFiltradas = favoriteDAO.buscarFavoritos(userId, busqueda);
        }

        mostrarFavoritos();
    }

    private void confirmarEliminar(FavoriteTranslationDAO.FavoriteTranslation favorito) {
        new AlertDialog.Builder(getContext())
                .setTitle("Eliminar favorito")
                .setMessage("¿Estás seguro de eliminar esta traducción de favoritos?")
                .setPositiveButton("Eliminar", (dialog, which) -> {
                    int resultado = favoriteDAO.eliminarFavorito(favorito.idFavoriteTranslation);
                    if (resultado > 0) {
                        cargarFavoritosDesdeDB();
                        mostrarFavoritos();
                        Toast.makeText(getContext(),
                                "Traducción eliminada de favoritos",
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
                "Eliminar todos los favoritos",
                "Ver solo frecuentes",
                "Exportar favoritos",
                "Ordenar por fecha"
        };

        new AlertDialog.Builder(getContext())
                .setTitle("Opciones de Favoritos")
                .setItems(opciones, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            confirmarEliminarTodos();
                            break;
                        case 1:
                            mostrarSoloFrecuentes();
                            break;
                        case 2:
                            Toast.makeText(getContext(),
                                    "Exportar favoritos - Función en desarrollo",
                                    Toast.LENGTH_SHORT).show();
                            break;
                        case 3:
                            cargarFavoritosDesdeDB();
                            mostrarFavoritos();
                            Toast.makeText(getContext(), "Ordenado por fecha (más reciente primero)",
                                    Toast.LENGTH_SHORT).show();
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
                    int resultado = favoriteDAO.eliminarTodosFavoritos(userId);
                    if (resultado > 0) {
                        cargarFavoritosDesdeDB();
                        mostrarFavoritos();
                        Toast.makeText(getContext(),
                                "Todos los favoritos han sido eliminados (" + resultado + " elementos)",
                                Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getContext(),
                                "No hay favoritos para eliminar",
                                Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void mostrarSoloFrecuentes() {
        traduccionesFiltradas = favoriteDAO.obtenerFavoritosFrecuentes(userId);
        mostrarFavoritos();

        int cantidad = traduccionesFiltradas.size();
        Toast.makeText(getContext(),
                "Mostrando " + cantidad + " traduccion" + (cantidad == 1 ? "" : "es") + " frecuente" + (cantidad == 1 ? "" : "s"),
                Toast.LENGTH_SHORT).show();
    }

    private void mostrarMensajeVacio(boolean mostrar) {
        if (noCoincidenciasMessage != null) {
            noCoincidenciasMessage.setVisibility(mostrar ? View.VISIBLE : View.GONE);
            if (mostrar) {
                noCoincidenciasMessage.setText("No hay favoritos guardados");
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

    private String obtenerNombreIdioma(int languageId) {
        LanguageDAO.Language idioma = languageDAO.obtenerIdiomaPorId(languageId);
        return idioma != null ? idioma.name : "Desconocido";
    }

    /**
     * Método público para agregar nuevos favoritos desde otros fragments
     */
    public void agregarFavorito(int sourceLanguageId, int targetLanguageId,
                                int translationTypeId, String textoOriginal,
                                String textoTraducido) {
        FavoriteTranslationDAO.FavoriteTranslation nuevo =
                new FavoriteTranslationDAO.FavoriteTranslation(
                        userId,
                        sourceLanguageId,
                        targetLanguageId,
                        translationTypeId,
                        textoOriginal,
                        textoTraducido
                );

        long resultado = favoriteDAO.insertarFavorito(nuevo);

        if (resultado != -1) {
            cargarFavoritosDesdeDB();
            mostrarFavoritos();
            Toast.makeText(getContext(),
                    "Traducción agregada a favoritos",
                    Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getContext(),
                    "Error al agregar a favoritos",
                    Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Recargar favoritos cuando el fragment vuelve a estar visible
        cargarFavoritosDesdeDB();
        mostrarFavoritos();
    }
}