package com.example.traduccioncotorra;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Diccionario extends Fragment {

    private EditText etBuscarDefinicion;
    private ImageButton menuButtonConfig;
    private TextView noCoincidencesMessage;

    // Simulación de diccionario con frases básicas
    private Map<String, Map<String, String>> diccionario;
    private List<String> palabrasCoincidentes;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_diccionario, container, false);

        // Inicializar el diccionario
        inicializarDiccionario();

        // Inicializar vistas
        inicializarVistas(view);

        // Configurar listeners
        configurarListeners();

        // Mostrar mensaje de bienvenida
        mostrarFrasesDisponibles();

        return view;
    }

    private void inicializarVistas(View view) {
        etBuscarDefinicion = view.findViewById(R.id.et_buscar_definicion);
        menuButtonConfig = view.findViewById(R.id.menu_button_config);
        noCoincidencesMessage = view.findViewById(R.id.no_coincidences_message);

        palabrasCoincidentes = new ArrayList<>();
    }

    private void inicializarDiccionario() {
        diccionario = new HashMap<>();

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

    private void configurarListeners() {
        // Listener para el botón de configuración
        menuButtonConfig.setOnClickListener(v -> {
            abrirConfiguracion();
        });

        // Listener para el campo de búsqueda
        etBuscarDefinicion.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                buscarDefiniciones(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void buscarDefiniciones(String query) {
        if (query.isEmpty()) {
            // Mostrar todas las frases disponibles
            noCoincidencesMessage.setVisibility(View.GONE);
            mostrarFrasesDisponibles();
            return;
        }

        palabrasCoincidentes.clear();
        String queryLower = query.toLowerCase();

        // Buscar coincidencias en el diccionario
        for (Map.Entry<String, Map<String, String>> entrada : diccionario.entrySet()) {
            String palabraEspanol = entrada.getKey();
            Map<String, String> traducciones = entrada.getValue();

            // Buscar en español
            if (palabraEspanol.contains(queryLower)) {
                palabrasCoincidentes.add(traducciones.get("Español"));
                continue;
            }

            // Buscar en otros idiomas
            for (String traduccion : traducciones.values()) {
                if (traduccion.toLowerCase().contains(queryLower)) {
                    palabrasCoincidentes.add(traducciones.get("Español"));
                    break;
                }
            }
        }

        // Mostrar resultados
        mostrarResultados(query);
    }

    private void mostrarResultados(String query) {
        if (palabrasCoincidentes.isEmpty()) {
            noCoincidencesMessage.setVisibility(View.VISIBLE);
            noCoincidencesMessage.setText("No hay palabras que coincidan con: \"" + query + "\"");
            Toast.makeText(getContext(),
                    "No se encontraron coincidencias",
                    Toast.LENGTH_SHORT).show();
        } else {
            noCoincidencesMessage.setVisibility(View.GONE);

            // Mostrar las primeras 3 coincidencias en un Toast
            StringBuilder mensaje = new StringBuilder("Encontradas " + palabrasCoincidentes.size() + " coincidencias:\n");
            int limite = Math.min(3, palabrasCoincidentes.size());
            for (int i = 0; i < limite; i++) {
                mensaje.append("• ").append(palabrasCoincidentes.get(i)).append("\n");
            }
            if (palabrasCoincidentes.size() > 3) {
                mensaje.append("...y ").append(palabrasCoincidentes.size() - 3).append(" más");
            }

            Toast.makeText(getContext(), mensaje.toString(), Toast.LENGTH_LONG).show();
        }
    }

    private void mostrarFrasesDisponibles() {
        Toast.makeText(getContext(),
                "Diccionario con " + diccionario.size() + " frases básicas disponibles\n" +
                        "Busca palabras en Español, Inglés, Francés o Alemán",
                Toast.LENGTH_LONG).show();
    }

    // Método para mostrar la definición completa de una palabra
    public void mostrarDefinicionCompleta(String palabra) {
        String palabraLower = palabra.toLowerCase();

        if (diccionario.containsKey(palabraLower)) {
            Map<String, String> traducciones = diccionario.get(palabraLower);

            StringBuilder definicion = new StringBuilder("Traducciones de: " + palabra + "\n\n");
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

    // Método para marcar una frase como favorita
    public void marcarComoFavorito(String palabra) {
        Toast.makeText(getContext(),
                "\"" + palabra + "\" agregada a favoritos",
                Toast.LENGTH_SHORT).show();

    }

    // Método para obtener una frase aleatoria del día
    public void mostrarFraseDelDia() {
        List<String> todasLasPalabras = new ArrayList<>(diccionario.keySet());
        if (!todasLasPalabras.isEmpty()) {
            int indiceAleatorio = (int) (Math.random() * todasLasPalabras.size());
            String palabraDelDia = todasLasPalabras.get(indiceAleatorio);

            Map<String, String> traducciones = diccionario.get(palabraDelDia);

            Toast.makeText(getContext(),
                    "📚 Frase del día:\n" +
                            traducciones.get("Español") + " = " + traducciones.get("Inglés"),
                    Toast.LENGTH_LONG).show();
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