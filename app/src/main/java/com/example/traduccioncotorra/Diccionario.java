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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Diccionario extends Fragment {

    private EditText etBuscarDefinicion;
    private ImageButton menuButtonConfig;
    private TextView noCoincidencesMessage;
    private TextView tvPalabrasDisponibles;
    private LinearLayout containerPalabras;

    // Lista para mantener referencia a todos los cards generados
    private List<CardView> todosLosCards;
    private Map<CardView, String> cardPalabraMap;

    // Simulación de diccionario con frases básicas
    private Map<String, Map<String, String>> diccionario;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_diccionario, container, false);

        // Inicializar listas primero
        todosLosCards = new ArrayList<>();
        cardPalabraMap = new HashMap<>();

        // Inicializar el diccionario
        inicializarDiccionario();

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

    private void inicializarDiccionario() {
        // Usar LinkedHashMap para mantener el orden de inserción
        diccionario = new LinkedHashMap<>();

        // Frases básicas en diferentes categorías
        agregarFrase("Hola", "Hello", "Bonjour", "Hallo");
        agregarFrase("Adiós", "Goodbye", "Au revoir", "Auf Wiedersehen");
        agregarFrase("Por favor", "Please", "S'il vous plaît", "Bitte");
        agregarFrase("Gracias", "Thank you", "Merci", "Danke");
        agregarFrase("Sí", "Yes", "Oui", "Ja");
        agregarFrase("No", "No", "Non", "Nein");
        agregarFrase("Buenos días", "Good morning", "Bonjour", "Guten Morgen");
        agregarFrase("Buenas noches", "Good night", "Bonne nuit", "Gute Nacht");
        agregarFrase("¿Cómo estás?", "How are you?", "Comment allez-vous?", "Wie geht es dir?");
        agregarFrase("Perdón", "Sorry", "Pardon", "Entschuldigung");
        agregarFrase("Ayuda", "Help", "Aide", "Hilfe");
        agregarFrase("Agua", "Water", "Eau", "Wasser");
        agregarFrase("Comida", "Food", "Nourriture", "Essen");
        agregarFrase("Baño", "Bathroom", "Toilettes", "Toilette");
        agregarFrase("Salida", "Exit", "Sortie", "Ausgang");
        agregarFrase("Entrada", "Entrance", "Entrée", "Eingang");
        agregarFrase("Hotel", "Hotel", "Hôtel", "Hotel");
        agregarFrase("Restaurante", "Restaurant", "Restaurant", "Restaurant");
        agregarFrase("Hospital", "Hospital", "Hôpital", "Krankenhaus");
        agregarFrase("Farmacia", "Pharmacy", "Pharmacie", "Apotheke");
    }

    private void agregarFrase(String espanol, String ingles, String frances, String aleman) {
        Map<String, String> traducciones = new HashMap<>();
        traducciones.put("Español", espanol);
        traducciones.put("Inglés", ingles);
        traducciones.put("Francés", frances);
        traducciones.put("Alemán", aleman);

        diccionario.put(espanol.toLowerCase(), traducciones);
    }

    private void generarTarjetasDinamicamente() {
        // Verificar que el contexto y el contenedor existan
        if (getContext() == null) {
            return;
        }

        if (containerPalabras == null) {
            return;
        }

        // Limpiar el contenedor por si acaso
        containerPalabras.removeAllViews();
        todosLosCards.clear();
        cardPalabraMap.clear();

        // Crear una tarjeta por cada entrada del diccionario
        for (Map.Entry<String, Map<String, String>> entry : diccionario.entrySet()) {
            String palabraClave = entry.getKey();
            Map<String, String> traducciones = entry.getValue();

            try {
                // Crear la tarjeta
                CardView card = crearTarjeta(traducciones);

                // Guardar referencia
                todosLosCards.add(card);
                cardPalabraMap.put(card, palabraClave);

                // Agregar al contenedor
                containerPalabras.addView(card);

                // Configurar listeners
                configurarCardListeners(card, palabraClave);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private CardView crearTarjeta(Map<String, String> traducciones) {
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
                // Usar TypedValue para obtener el atributo correctamente
                android.util.TypedValue outValue = new android.util.TypedValue();
                getContext().getTheme().resolveAttribute(
                        android.R.attr.selectableItemBackground,
                        outValue,
                        true
                );
                cardView.setForeground(getContext().getDrawable(outValue.resourceId));
            }
        } catch (Exception e) {
            // Si falla, continuar sin el foreground
            e.printStackTrace();
        }

        // Crear el LinearLayout interno
        LinearLayout innerLayout = new LinearLayout(getContext());
        innerLayout.setOrientation(LinearLayout.VERTICAL);
        innerLayout.setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16));

        // TextView del título (español)
        TextView tvTitulo = new TextView(getContext());
        tvTitulo.setText(traducciones.get("Español"));
        tvTitulo.setTextSize(18);
        tvTitulo.setTextColor(Color.BLACK);
        tvTitulo.setTypeface(null, Typeface.BOLD);
        innerLayout.addView(tvTitulo);

        // TextView de las traducciones
        TextView tvTraducciones = new TextView(getContext());
        String textoTraducciones = String.format(
                "🇬🇧 %s  |  🇫🇷 %s  |  🇩🇪 %s",
                traducciones.get("Inglés"),
                traducciones.get("Francés"),
                traducciones.get("Alemán")
        );
        tvTraducciones.setText(textoTraducciones);
        tvTraducciones.setTextSize(14);
        tvTraducciones.setTextColor(Color.parseColor("#666666"));
        LinearLayout.LayoutParams tvParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        tvParams.topMargin = dpToPx(4);
        tvTraducciones.setLayoutParams(tvParams);
        innerLayout.addView(tvTraducciones);

        // Agregar el layout interno al card
        cardView.addView(innerLayout);

        return cardView;
    }

    private void configurarCardListeners(CardView card, String palabra) {
        // Click normal para mostrar definición
        card.setOnClickListener(v -> {
            mostrarDefinicionCompleta(palabra);
        });

        // Long click para agregar a favoritos
        card.setOnLongClickListener(v -> {
            marcarComoFavorito(palabra);
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
                tvPalabrasDisponibles.setText("Frases básicas disponibles");
            }
            return;
        }

        String queryLower = query.toLowerCase();
        boolean hayCoincidencias = false;
        int coincidenciasCount = 0;

        // Filtrar cada card
        for (Map.Entry<CardView, String> entry : cardPalabraMap.entrySet()) {
            CardView card = entry.getKey();
            String palabra = entry.getValue();

            // Buscar en el diccionario
            Map<String, String> traducciones = diccionario.get(palabra);
            boolean coincide = false;

            if (traducciones != null) {
                // Buscar coincidencias en cualquier idioma
                for (String traduccion : traducciones.values()) {
                    if (traduccion.toLowerCase().contains(queryLower)) {
                        coincide = true;
                        break;
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

    private void mostrarDefinicionCompleta(String palabra) {
        String palabraLower = palabra.toLowerCase();

        if (diccionario.containsKey(palabraLower)) {
            Map<String, String> traducciones = diccionario.get(palabraLower);

            StringBuilder definicion = new StringBuilder("📚 Traducciones de: " + palabra + "\n\n");
            definicion.append("🇪🇸 Español: ").append(traducciones.get("Español")).append("\n");
            definicion.append("🇬🇧 Inglés: ").append(traducciones.get("Inglés")).append("\n");
            definicion.append("🇫🇷 Francés: ").append(traducciones.get("Francés")).append("\n");
            definicion.append("🇩🇪 Alemán: ").append(traducciones.get("Alemán"));

            Toast.makeText(getContext(), definicion.toString(), Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(getContext(),
                    "No se encontró la palabra: " + palabra,
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void marcarComoFavorito(String palabra) {
        if (getContext() != null) {
            Toast.makeText(getContext(),
                    "⭐ \"" + palabra + "\" agregada a favoritos\n" +
                            "(Mantén presionada una palabra para agregarla)",
                    Toast.LENGTH_LONG).show();
        }
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
}