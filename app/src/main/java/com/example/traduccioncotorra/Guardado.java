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
        // ⭐ Crear CardView en lugar de LinearLayout
        androidx.cardview.widget.CardView cardView = new androidx.cardview.widget.CardView(getContext());

        // ⭐ Configuración del CardView con estilo Material
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        cardParams.setMargins(dpToPx(8), dpToPx(8), dpToPx(8), dpToPx(8));
        cardView.setLayoutParams(cardParams);

        cardView.setRadius(dpToPx(16));
        cardView.setCardElevation(dpToPx(4));
        cardView.setCardBackgroundColor(android.graphics.Color.WHITE);
        cardView.setMaxCardElevation(dpToPx(6));
        cardView.setUseCompatPadding(true);
        cardView.setClickable(true);
        cardView.setFocusable(true);

        // ⭐ Efecto ripple personalizado
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            android.content.res.ColorStateList rippleColor =
                    android.content.res.ColorStateList.valueOf(
                            android.graphics.Color.parseColor("#20000000")
                    );

            android.graphics.drawable.RippleDrawable rippleDrawable =
                    new android.graphics.drawable.RippleDrawable(
                            rippleColor,
                            null,
                            null
                    );

            cardView.setForeground(rippleDrawable);
        }

        // Crear contenedor interno
        LinearLayout tarjeta = new LinearLayout(getContext());
        tarjeta.setOrientation(LinearLayout.VERTICAL);
        tarjeta.setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16));
        tarjeta.setBackgroundColor(android.graphics.Color.TRANSPARENT);

        // ⭐ Header con idiomas y botón eliminar
        LinearLayout header = new LinearLayout(getContext());
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setPadding(0, 0, 0, dpToPx(8));

        // Obtener nombres de idiomas
        String idiomaOrigen = obtenerNombreIdioma(favorito.sourceLanguageId);
        String idiomaDestino = obtenerNombreIdioma(favorito.targetLanguageId);

        // ⭐ Idiomas con mejor formato
        TextView tvIdiomas = new TextView(getContext());
        tvIdiomas.setText(idiomaOrigen + " → " + idiomaDestino);
        tvIdiomas.setTextSize(12);
        tvIdiomas.setTextColor(android.graphics.Color.parseColor("#757575"));
        tvIdiomas.setTypeface(null, android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams idiomasParams = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
        );
        tvIdiomas.setLayoutParams(idiomasParams);
        header.addView(tvIdiomas);

        // ⭐ Botón eliminar mejorado
        ImageButton btnEliminar = new ImageButton(getContext());
        btnEliminar.setImageResource(android.R.drawable.ic_menu_delete);
        btnEliminar.setBackgroundColor(android.graphics.Color.TRANSPARENT);

        // Efecto ripple para el botón
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            android.util.TypedValue outValue = new android.util.TypedValue();
            getContext().getTheme().resolveAttribute(
                    android.R.attr.selectableItemBackgroundBorderless,
                    outValue,
                    true
            );
            btnEliminar.setBackground(getContext().getDrawable(outValue.resourceId));
        }

        btnEliminar.setPadding(dpToPx(8), dpToPx(8), dpToPx(8), dpToPx(8));
        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
                dpToPx(40),
                dpToPx(40)
        );
        btnEliminar.setLayoutParams(btnParams);
        btnEliminar.setOnClickListener(v -> confirmarEliminar(favorito));
        header.addView(btnEliminar);

        tarjeta.addView(header);

        // ⭐ Texto original con mejor formato
        TextView tvOriginal = new TextView(getContext());
        tvOriginal.setText(favorito.originalText);
        tvOriginal.setTextSize(16);
        tvOriginal.setTextColor(android.graphics.Color.parseColor("#212121"));
        tvOriginal.setTypeface(null, android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams originalParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        originalParams.setMargins(0, dpToPx(4), 0, dpToPx(8));
        tvOriginal.setLayoutParams(originalParams);
        tarjeta.addView(tvOriginal);

        // ⭐ Texto traducido con mejor formato
        TextView tvTraducido = new TextView(getContext());
        tvTraducido.setText("→ " + favorito.translatedText);
        tvTraducido.setTextSize(16);
        tvTraducido.setTextColor(android.graphics.Color.parseColor("#1976D2")); // Azul material
        LinearLayout.LayoutParams traducidoParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        traducidoParams.setMargins(0, 0, 0, dpToPx(12));
        tvTraducido.setLayoutParams(traducidoParams);
        tarjeta.addView(tvTraducido);

        // ⭐ Footer con fecha y tipo de traducción
        LinearLayout footer = new LinearLayout(getContext());
        footer.setOrientation(LinearLayout.HORIZONTAL);

        // Fecha
        TextView tvFecha = new TextView(getContext());
        tvFecha.setText("🕒 " + formatearFecha(favorito.savedDate));
        tvFecha.setTextSize(11);
        tvFecha.setTextColor(android.graphics.Color.parseColor("#9E9E9E"));
        LinearLayout.LayoutParams fechaParams = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
        );
        tvFecha.setLayoutParams(fechaParams);
        footer.addView(tvFecha);

        // Tipo de traducción (opcional)
        TextView tvTipo = new TextView(getContext());
        String tipoTraduccion = obtenerTipoTraduccion(favorito.translationTypeId);
        tvTipo.setText(tipoTraduccion);
        tvTipo.setTextSize(11);
        tvTipo.setTextColor(android.graphics.Color.parseColor("#9E9E9E"));
        footer.addView(tvTipo);

        tarjeta.addView(footer);

        // Agregar el layout interno al CardView
        cardView.addView(tarjeta);

        // ⭐ Click en la tarjeta para copiar
        cardView.setOnClickListener(v -> {
            // Copiar al portapapeles
            android.content.ClipboardManager clipboard =
                    (android.content.ClipboardManager) getContext().getSystemService(android.content.Context.CLIPBOARD_SERVICE);

            android.content.ClipData clip = android.content.ClipData.newPlainText(
                    "Traducción",
                    favorito.translatedText
            );

            if (clipboard != null) {
                clipboard.setPrimaryClip(clip);
            }

            Toast.makeText(getContext(),
                    "📋 Copiado: " + favorito.translatedText,
                    Toast.LENGTH_SHORT).show();
        });

        return cardView;
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
    /**
     * Convertir dp a píxeles
     */
    private int dpToPx(int dp) {
        if (getResources() != null) {
            float density = getResources().getDisplayMetrics().density;
            return Math.round(dp * density);
        }
        return dp;
    }
    private String obtenerTipoTraduccion(int translationTypeId) {
        TranslationTypeDAO.TranslationType tipo = translationTypeDAO.obtenerTipoPorId(translationTypeId);

        if (tipo != null) {
            switch (tipo.name.toLowerCase()) {
                case "texto":
                    return "📝";
                case "cámara":
                    return "📷";
                case "documento":
                    return "📄";
                case "voz":
                    return "🎤";
                default:
                    return "🌐";
            }
        }

        return "🌐";
    }
}