package com.example.traduccioncotorra;

import android.graphics.Color;
import android.graphics.Typeface;
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
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import com.example.traduccioncotorra.DB.WordDAO;
import com.example.traduccioncotorra.DB.WordLanguageDAO;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Diccionario extends Fragment {

    private EditText etBuscarDefinicion;
    private ImageButton menuButtonConfig;
    private TextView noCoincidencesMessage;
    private TextView tvPalabrasDisponibles;
    private LinearLayout containerPalabras;

    // DAOs para acceder a la base de datos
    private WordDAO wordDAO;
    private WordLanguageDAO wordLanguageDAO;

    // Lista para mantener referencia a todos los cards generados
    private List<CardView> todosLosCards;
    private Map<CardView, Integer> cardPalabraMap; // Ahora usa WordId en lugar de palabra

    // Diccionario cargado desde la BD
    private List<WordDAO.Word> palabras;
    private Map<Integer, Map<String, String>> traducciones; // WordId -> {idioma -> traducción}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_diccionario, container, false);

        // Inicializar listas primero
        todosLosCards = new ArrayList<>();
        cardPalabraMap = new HashMap<>();

        // Inicializar DAOs
        wordDAO = new WordDAO(requireContext());
        wordLanguageDAO = new WordLanguageDAO(requireContext());

        // Cargar diccionario desde la base de datos
        cargarDiccionarioDesdeDB();

        // Inicializar vistas
        inicializarVistas(view);

        // Generar las tarjetas dinámicamente
        generarTarjetasDinamicamente();

        // Configurar listeners
        configurarListeners();

        return view;
    }

    private void inicializarVistas(View view) {
        etBuscarDefinicion = view.findViewById(R.id.et_buscar_definicion);
        menuButtonConfig = view.findViewById(R.id.menu_button_config);
        noCoincidencesMessage = view.findViewById(R.id.no_coincidences_message);
        tvPalabrasDisponibles = view.findViewById(R.id.tv_palabras_disponibles);
        containerPalabras = view.findViewById(R.id.container_palabras);
    }

    /**
     * Cargar el diccionario desde la base de datos
     */
    private void cargarDiccionarioDesdeDB() {
        try {
            // Obtener todas las palabras
            palabras = wordDAO.obtenerTodasLasPalabras();

            // Obtener todas las traducciones
            traducciones = new HashMap<>();
            for (WordDAO.Word palabra : palabras) {
                Map<String, String> traduccionesPalabra = wordLanguageDAO.obtenerTraduccionesComoMapa(palabra.wordId);
                traducciones.put(palabra.wordId, traduccionesPalabra);
            }

            // Actualizar el contador de palabras
            if (tvPalabrasDisponibles != null) {
                tvPalabrasDisponibles.setText(palabras.size() + " frases disponibles");
            }

            // Mostrar mensaje si no hay palabras
            if (palabras.isEmpty()) {
                Toast.makeText(getContext(),
                        "No hay palabras en el diccionario. Ve a Configuración → Admin Catálogos para agregar palabras.",
                        Toast.LENGTH_LONG).show();
            }

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getContext(),
                    "Error al cargar el diccionario: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void generarTarjetasDinamicamente() {
        // Verificar que el contexto y el contenedor existan
        if (getContext() == null || containerPalabras == null) {
            return;
        }

        // Limpiar el contenedor por si acaso
        containerPalabras.removeAllViews();
        todosLosCards.clear();
        cardPalabraMap.clear();

        // Crear una tarjeta por cada palabra del diccionario
        for (WordDAO.Word palabra : palabras) {
            try {
                // Obtener traducciones de esta palabra
                Map<String, String> traduccionesPalabra = traducciones.get(palabra.wordId);

                if (traduccionesPalabra == null || traduccionesPalabra.isEmpty()) {
                    continue; // Saltar palabras sin traducciones
                }

                // Crear la tarjeta
                CardView card = crearTarjeta(palabra, traduccionesPalabra);

                // Guardar referencia
                todosLosCards.add(card);
                cardPalabraMap.put(card, palabra.wordId);

                // Agregar al contenedor
                containerPalabras.addView(card);

                // Configurar listeners
                configurarCardListeners(card, palabra.wordId);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Actualizar contador
        if (tvPalabrasDisponibles != null) {
            tvPalabrasDisponibles.setText("Encontradas " + todosLosCards.size() + " frases");
        }
    }

    private CardView crearTarjeta(WordDAO.Word palabra, Map<String, String> traduccionesPalabra) {
        // Crear el CardView
        CardView cardView = new CardView(getContext());
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        cardParams.bottomMargin = dpToPx(8);
        cardView.setLayoutParams(cardParams);
        cardView.setRadius(dpToPx(12));
        cardView.setCardElevation(dpToPx(2));
        cardView.setClickable(true);
        cardView.setFocusable(true);

        // Configurar el efecto ripple de forma segura
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                android.util.TypedValue outValue = new android.util.TypedValue();
                getContext().getTheme().resolveAttribute(
                        android.R.attr.selectableItemBackground,
                        outValue,
                        true
                );
                cardView.setForeground(getContext().getDrawable(outValue.resourceId));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Crear el LinearLayout interno
        LinearLayout innerLayout = new LinearLayout(getContext());
        innerLayout.setOrientation(LinearLayout.VERTICAL);
        innerLayout.setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16));

        // TextView del título (palabra principal)
        TextView tvTitulo = new TextView(getContext());
        tvTitulo.setText(palabra.word);
        tvTitulo.setTextSize(18);
        tvTitulo.setTextColor(Color.BLACK);
        tvTitulo.setTypeface(null, Typeface.BOLD);
        innerLayout.addView(tvTitulo);

        // TextView de la descripción (si existe)
        if (palabra.description != null && !palabra.description.isEmpty()) {
            TextView tvDescripcion = new TextView(getContext());
            tvDescripcion.setText(palabra.description);
            tvDescripcion.setTextSize(12);
            tvDescripcion.setTextColor(Color.parseColor("#888888"));
            LinearLayout.LayoutParams descParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            descParams.topMargin = dpToPx(2);
            tvDescripcion.setLayoutParams(descParams);
            innerLayout.addView(tvDescripcion);
        }

        // TextView de las traducciones
        TextView tvTraducciones = new TextView(getContext());
        String textoTraducciones = construirTextoTraducciones(traduccionesPalabra);
        tvTraducciones.setText(textoTraducciones);
        tvTraducciones.setTextSize(14);
        tvTraducciones.setTextColor(Color.parseColor("#666666"));
        LinearLayout.LayoutParams tvParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        tvParams.topMargin = dpToPx(8);
        tvTraducciones.setLayoutParams(tvParams);
        innerLayout.addView(tvTraducciones);

        // Agregar el layout interno al card
        cardView.addView(innerLayout);

        return cardView;
    }

    /**
     * Construir el texto de traducciones con emojis de banderas
     */
    private String construirTextoTraducciones(Map<String, String> traduccionesPalabra) {
        StringBuilder texto = new StringBuilder();

        // Mapeo de idiomas a emojis de banderas
        Map<String, String> emojis = new HashMap<>();
        emojis.put("Español", "🇪🇸");
        emojis.put("Inglés", "🇬🇧");
        emojis.put("Francés", "🇫🇷");
        emojis.put("Alemán", "🇩🇪");
        emojis.put("Italiano", "🇮🇹");
        emojis.put("Portugués", "🇵🇹");

        int count = 0;
        for (Map.Entry<String, String> entry : traduccionesPalabra.entrySet()) {
            if (count > 0) {
                texto.append("  |  ");
            }

            String idioma = entry.getKey();
            String traduccion = entry.getValue();
            String emoji = emojis.getOrDefault(idioma, "🌐");

            texto.append(emoji).append(" ").append(traduccion);
            count++;
        }

        return texto.toString();
    }

    private void configurarCardListeners(CardView card, int wordId) {
        // Click normal para mostrar definición
        card.setOnClickListener(v -> {
            mostrarDefinicionCompleta(wordId);

            // Incrementar frecuencia de uso
            wordDAO.incrementarFrecuencia(wordId);
        });

        // Long click para agregar a favoritos
        card.setOnLongClickListener(v -> {
            marcarComoFavorito(wordId);
            return true;
        });
    }

    private void configurarListeners() {
        // Listener para el botón de configuración
        if (menuButtonConfig != null) {
            menuButtonConfig.setOnClickListener(v -> {
                abrirConfiguracion();
            });
        }

        // Listener para el campo de búsqueda
        if (etBuscarDefinicion != null) {
            etBuscarDefinicion.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    filtrarPalabras(s.toString());
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });
        }
    }

    private void filtrarPalabras(String query) {
        if (query.isEmpty()) {
            // Mostrar todos los cards
            mostrarTodosLosCards();
            if (noCoincidencesMessage != null) {
                noCoincidencesMessage.setVisibility(View.GONE);
            }
            if (tvPalabrasDisponibles != null) {
                tvPalabrasDisponibles.setText(palabras.size() + " frases disponibles");
            }
            return;
        }

        String queryLower = query.toLowerCase();
        boolean hayCoincidencias = false;
        int coincidenciasCount = 0;

        // Filtrar cada card
        for (Map.Entry<CardView, Integer> entry : cardPalabraMap.entrySet()) {
            CardView card = entry.getKey();
            int wordId = entry.getValue();

            boolean coincide = false;

            // Buscar en la palabra principal
            WordDAO.Word palabra = obtenerPalabraPorId(wordId);
            if (palabra != null) {
                if (palabra.word.toLowerCase().contains(queryLower) ||
                        (palabra.description != null && palabra.description.toLowerCase().contains(queryLower))) {
                    coincide = true;
                }
            }

            // Buscar en las traducciones
            if (!coincide) {
                Map<String, String> traduccionesPalabra = traducciones.get(wordId);
                if (traduccionesPalabra != null) {
                    for (String traduccion : traduccionesPalabra.values()) {
                        if (traduccion.toLowerCase().contains(queryLower)) {
                            coincide = true;
                            break;
                        }
                    }
                }
            }

            // Mostrar u ocultar el card según coincidencia
            if (coincide) {
                card.setVisibility(View.VISIBLE);
                hayCoincidencias = true;
                coincidenciasCount++;
            } else {
                card.setVisibility(View.GONE);
            }
        }

        // Actualizar UI según resultados
        if (hayCoincidencias) {
            if (noCoincidencesMessage != null) {
                noCoincidencesMessage.setVisibility(View.GONE);
            }
            if (containerPalabras != null) {
                containerPalabras.setVisibility(View.VISIBLE);
            }
            if (tvPalabrasDisponibles != null) {
                tvPalabrasDisponibles.setText("Encontradas " + coincidenciasCount + " coincidencias");
            }
        } else {
            if (noCoincidencesMessage != null) {
                noCoincidencesMessage.setVisibility(View.VISIBLE);
                noCoincidencesMessage.setText("No hay palabras que coincidan con: \"" + query + "\"");
            }
            if (containerPalabras != null) {
                containerPalabras.setVisibility(View.GONE);
            }
            if (tvPalabrasDisponibles != null) {
                tvPalabrasDisponibles.setVisibility(View.GONE);
            }
        }
    }

    private void mostrarTodosLosCards() {
        for (CardView card : todosLosCards) {
            card.setVisibility(View.VISIBLE);
        }
        if (containerPalabras != null) {
            containerPalabras.setVisibility(View.VISIBLE);
        }
        if (tvPalabrasDisponibles != null) {
            tvPalabrasDisponibles.setVisibility(View.VISIBLE);
        }
    }

    private void mostrarDefinicionCompleta(int wordId) {
        WordDAO.Word palabra = obtenerPalabraPorId(wordId);
        if (palabra == null) {
            Toast.makeText(getContext(), "Palabra no encontrada", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, String> traduccionesPalabra = traducciones.get(wordId);
        if (traduccionesPalabra == null || traduccionesPalabra.isEmpty()) {
            Toast.makeText(getContext(), "No hay traducciones disponibles", Toast.LENGTH_SHORT).show();
            return;
        }

        StringBuilder definicion = new StringBuilder("📚 Traducciones de: " + palabra.word + "\n\n");

        if (palabra.description != null && !palabra.description.isEmpty()) {
            definicion.append("ℹ️ ").append(palabra.description).append("\n\n");
        }

        // Agregar traducciones con emojis
        Map<String, String> emojis = new HashMap<>();
        emojis.put("Español", "🇪🇸");
        emojis.put("Inglés", "🇬🇧");
        emojis.put("Francés", "🇫🇷");
        emojis.put("Alemán", "🇩🇪");
        emojis.put("Italiano", "🇮🇹");
        emojis.put("Portugués", "🇵🇹");

        for (Map.Entry<String, String> entry : traduccionesPalabra.entrySet()) {
            String idioma = entry.getKey();
            String traduccion = entry.getValue();
            String emoji = emojis.getOrDefault(idioma, "🌐");

            definicion.append(emoji).append(" ").append(idioma).append(": ")
                    .append(traduccion).append("\n");
        }

        Toast.makeText(getContext(), definicion.toString(), Toast.LENGTH_LONG).show();
    }

    private void marcarComoFavorito(int wordId) {
        WordDAO.Word palabra = obtenerPalabraPorId(wordId);
        if (palabra != null && getContext() != null) {
            Toast.makeText(getContext(),
                    "⭐ \"" + palabra.word + "\" agregada a favoritos\n" +
                            "(Mantén presionada una palabra para agregarla)",
                    Toast.LENGTH_LONG).show();
        }
    }

    private WordDAO.Word obtenerPalabraPorId(int wordId) {
        for (WordDAO.Word palabra : palabras) {
            if (palabra.wordId == wordId) {
                return palabra;
            }
        }
        return null;
    }

    private void abrirConfiguracion() {
        try {
            Configuracion configuracionFragment = new Configuracion();

            getParentFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, configuracionFragment)
                    .addToBackStack(null)
                    .commit();
        } catch (Exception e) {
            e.printStackTrace();
            if (getContext() != null) {
                Toast.makeText(getContext(), "Error al abrir configuración", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Método auxiliar para convertir dp a píxeles
    private int dpToPx(int dp) {
        if (getResources() != null) {
            float density = getResources().getDisplayMetrics().density;
            return Math.round(dp * density);
        }
        return dp;
    }

    /**
     * Recargar el diccionario (útil después de agregar/editar palabras)
     */
    public void recargarDiccionario() {
        cargarDiccionarioDesdeDB();
        generarTarjetasDinamicamente();
        Toast.makeText(getContext(), "Diccionario actualizado", Toast.LENGTH_SHORT).show();
    }
}