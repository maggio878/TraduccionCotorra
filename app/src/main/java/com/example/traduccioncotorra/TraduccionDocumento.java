package com.example.traduccioncotorra;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatSpinner;
import androidx.fragment.app.Fragment;
import com.example.traduccioncotorra.DB.HistorialDAO;
import com.example.traduccioncotorra.DB.LanguageDAO;
import com.example.traduccioncotorra.DB.TranslationTypeDAO;
import com.example.traduccioncotorra.DB.UserDAO;
import com.example.traduccioncotorra.DB.FavoriteTranslationDAO;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.button.MaterialButton;
import com.google.mlkit.common.model.DownloadConditions;
import com.google.mlkit.nl.translate.Translation;
import com.google.mlkit.nl.translate.Translator;
import com.google.mlkit.nl.translate.TranslatorOptions;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.text.PDFTextStripper;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class TraduccionDocumento extends Fragment {

    private static final String TAG = "TRADUCCION_DOC";
    private static final String PREFS_NAME = "DocumentosRecientes";
    private static final String KEY_RECIENTES = "recientes_list";
    private static final int MAX_RECIENTES = 5;
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB

    // Views
    private EditText etBuscarDocumentos;
    private ImageButton menuButtonConfig;
    private LinearLayout containerDocumentosRecientes;
    private TextView tvNoDocumentos;
    private MaterialButton btnSeleccionarDocumento;
    private AppCompatSpinner spinnerSourceLanguage;
    private AppCompatSpinner spinnerTargetLanguage;
    private MaterialButton btnTraducir;
    private EditText etTextoExtraido;
    private EditText etTextoTraducido;
    private ImageButton btnFavorito;
    private LinearLayout layoutTraduccion;

    // DAOs
    private LanguageDAO languageDAO;
    private HistorialDAO historialDAO;
    private UserDAO userDAO;
    private TranslationTypeDAO translationTypeDAO;
    private FavoriteTranslationDAO favoriteDAO;
    private int userId;
    private int translationTypeId = 3; // 3 = Documento

    // Idiomas
    private List<LanguageDAO.Language> idiomasDisponibles;
    private int sourceLanguageId = -1;
    private int targetLanguageId = -1;
    private String sourceLanguageCode;
    private String targetLanguageCode;

    // Traducción
    private Translator translator;
    private ProgressDialog progressDialog;

    // Documento actual
    private String nombreDocumentoActual = "";
    private String textoExtraido = "";
    private String textoTraducido = "";
    private boolean esFavorito = false;

    // Launcher para seleccionar archivo
    private ActivityResultLauncher<Intent> documentPickerLauncher;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_traduccion_documento, container, false);

        // Inicializar PDFBox
        PDFBoxResourceLoader.init(requireContext());

        // Inicializar DAOs
        inicializarDAOs();

        // Inicializar launcher
        inicializarDocumentPicker();

        // Inicializar vistas
        inicializarVistas(view);

        // Cargar idiomas
        cargarIdiomasDesdeDB();

        // Configurar spinners
        configurarSpinners();

        // Configurar listeners
        configurarListeners();

        // Cargar documentos recientes
        cargarDocumentosRecientes();

        return view;
    }

    private void inicializarDAOs() {
        languageDAO = new LanguageDAO(requireContext());
        historialDAO = new HistorialDAO(requireContext());
        userDAO = new UserDAO(requireContext());
        translationTypeDAO = new TranslationTypeDAO(requireContext());
        favoriteDAO = new FavoriteTranslationDAO(requireContext());
        userId = userDAO.obtenerUserIdActual(requireContext());

        // Obtener ID del tipo "Documento"
        TranslationTypeDAO.TranslationType tipoDoc = translationTypeDAO.obtenerTipoPorNombre("Documento");
        if (tipoDoc != null) {
            translationTypeId = tipoDoc.idTypeTranslation;
            Log.d(TAG, "Tipo de traducción 'Documento' ID: " + translationTypeId);
        }

        progressDialog = new ProgressDialog(getContext());
        progressDialog.setTitle("Por favor espere");
        progressDialog.setCanceledOnTouchOutside(false);
    }

    private void inicializarDocumentPicker() {
        documentPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null) {
                            procesarDocumento(uri);
                        }
                    }
                }
        );
    }

    private void inicializarVistas(View view) {
        etBuscarDocumentos = view.findViewById(R.id.et_buscar_documentos);
        menuButtonConfig = view.findViewById(R.id.menu_button_config);
        containerDocumentosRecientes = view.findViewById(R.id.container_documentos_recientes);
        tvNoDocumentos = view.findViewById(R.id.tv_no_documentos);
        btnSeleccionarDocumento = view.findViewById(R.id.btn_seleccionar_documento);
        spinnerSourceLanguage = view.findViewById(R.id.spinner_source_language);
        spinnerTargetLanguage = view.findViewById(R.id.spinner_target_language);
        btnTraducir = view.findViewById(R.id.btn_traducir_documento);
        etTextoExtraido = view.findViewById(R.id.et_texto_extraido);
        etTextoTraducido = view.findViewById(R.id.et_texto_traducido);
        btnFavorito = view.findViewById(R.id.btn_favorito_documento);
        layoutTraduccion = view.findViewById(R.id.layout_traduccion_documento);

        // Ocultar layout de traducción inicialmente
        layoutTraduccion.setVisibility(View.GONE);
    }

    private void cargarIdiomasDesdeDB() {
        idiomasDisponibles = languageDAO.obtenerIdiomasActivos();

        if (idiomasDisponibles.isEmpty()) {
            Toast.makeText(getContext(),
                    "⚠️ No hay idiomas configurados",
                    Toast.LENGTH_LONG).show();
        }
    }

    private void configurarSpinners() {
        if (idiomasDisponibles.isEmpty()) {
            spinnerSourceLanguage.setEnabled(false);
            spinnerTargetLanguage.setEnabled(false);
            return;
        }

        // Crear array de nombres
        String[] nombresIdiomas = new String[idiomasDisponibles.size()];
        for (int i = 0; i < idiomasDisponibles.size(); i++) {
            nombresIdiomas[i] = idiomasDisponibles.get(i).name;
        }

        // Adapters
        ArrayAdapter<String> adapterSource = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                nombresIdiomas
        );
        adapterSource.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        ArrayAdapter<String> adapterTarget = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                nombresIdiomas
        );
        adapterTarget.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        spinnerSourceLanguage.setAdapter(adapterSource);
        spinnerTargetLanguage.setAdapter(adapterTarget);

        // Selección por defecto: Español -> Inglés
        int posEspanol = buscarPosicionIdioma("Español");
        int posIngles = buscarPosicionIdioma("Inglés");

        if (posEspanol != -1) {
            spinnerSourceLanguage.setSelection(posEspanol);
            LanguageDAO.Language idioma = idiomasDisponibles.get(posEspanol);
            sourceLanguageId = idioma.languageId;
            sourceLanguageCode = idioma.apiCode;
        }

        if (posIngles != -1) {
            spinnerTargetLanguage.setSelection(posIngles);
            LanguageDAO.Language idioma = idiomasDisponibles.get(posIngles);
            targetLanguageId = idioma.languageId;
            targetLanguageCode = idioma.apiCode;
        }

        // Listeners
        spinnerSourceLanguage.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                LanguageDAO.Language idioma = idiomasDisponibles.get(position);
                sourceLanguageId = idioma.languageId;
                sourceLanguageCode = idioma.apiCode;
                crearTraductor();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        spinnerTargetLanguage.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                LanguageDAO.Language idioma = idiomasDisponibles.get(position);
                targetLanguageId = idioma.languageId;
                targetLanguageCode = idioma.apiCode;
                crearTraductor();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        crearTraductor();
    }

    private int buscarPosicionIdioma(String nombreIdioma) {
        for (int i = 0; i < idiomasDisponibles.size(); i++) {
            if (idiomasDisponibles.get(i).name.equalsIgnoreCase(nombreIdioma)) {
                return i;
            }
        }
        return -1;
    }

    private void configurarListeners() {
        // Botón seleccionar documento
        btnSeleccionarDocumento.setOnClickListener(v -> abrirSelectorDocumento());

        // Botón traducir
        btnTraducir.setOnClickListener(v -> traducirDocumento());

        // Botón favorito
        btnFavorito.setOnClickListener(v -> toggleFavorito());

        // Botón configuración
        menuButtonConfig.setOnClickListener(v -> abrirConfiguracion());

        // Búsqueda de documentos
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
    }

    private void abrirSelectorDocumento() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        String[] mimeTypes = {
                "application/pdf",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        };
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
        documentPickerLauncher.launch(intent);
    }

    private void procesarDocumento(Uri uri) {
        progressDialog.setMessage("Extrayendo texto del documento...");
        progressDialog.show();

        new Thread(() -> {
            try {
                // Verificar tamaño del archivo
                long fileSize = obtenerTamanoArchivo(uri);
                if (fileSize > MAX_FILE_SIZE) {
                    requireActivity().runOnUiThread(() -> {
                        progressDialog.dismiss();
                        Toast.makeText(getContext(),
                                "❌ El archivo es demasiado grande. Máximo: 5MB",
                                Toast.LENGTH_LONG).show();
                    });
                    return;
                }

                // Obtener nombre del archivo
                nombreDocumentoActual = obtenerNombreArchivo(uri);

                // Extraer texto según el tipo
                String texto = "";
                String mimeType = requireContext().getContentResolver().getType(uri);

                if (mimeType != null) {
                    if (mimeType.equals("application/pdf")) {
                        texto = extraerTextoPDF(uri);
                    } else if (mimeType.contains("wordprocessingml")) {
                        texto = extraerTextoWord(uri);
                    }
                }

                if (texto.isEmpty()) {
                    requireActivity().runOnUiThread(() -> {
                        progressDialog.dismiss();
                        Toast.makeText(getContext(),
                                "❌ No se pudo extraer texto del documento",
                                Toast.LENGTH_SHORT).show();
                    });
                    return;
                }

                textoExtraido = texto;

                String finalTexto = texto;
                requireActivity().runOnUiThread(() -> {
                    progressDialog.dismiss();
                    etTextoExtraido.setText(textoExtraido);
                    layoutTraduccion.setVisibility(View.VISIBLE);
                    Toast.makeText(getContext(),
                            "✅ Texto extraído. " + finalTexto.length() + " caracteres",
                            Toast.LENGTH_SHORT).show();
                });

            } catch (Exception e) {
                Log.e(TAG, "Error al procesar documento", e);
                requireActivity().runOnUiThread(() -> {
                    progressDialog.dismiss();
                    Toast.makeText(getContext(),
                            "❌ Error: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private String extraerTextoPDF(Uri uri) throws Exception {
        InputStream inputStream = requireContext().getContentResolver().openInputStream(uri);
        PDDocument document = PDDocument.load(inputStream);

        PDFTextStripper stripper = new PDFTextStripper();
        String texto = stripper.getText(document);

        document.close();
        inputStream.close();

        return texto.trim();
    }

    private String extraerTextoWord(Uri uri) throws Exception {
        InputStream inputStream = requireContext().getContentResolver().openInputStream(uri);
        XWPFDocument document = new XWPFDocument(inputStream);

        StringBuilder texto = new StringBuilder();
        List<XWPFParagraph> paragraphs = document.getParagraphs();

        for (XWPFParagraph paragraph : paragraphs) {
            texto.append(paragraph.getText()).append("\n");
        }

        document.close();
        inputStream.close();

        return texto.toString().trim();
    }

    private long obtenerTamanoArchivo(Uri uri) {
        try {
            InputStream inputStream = requireContext().getContentResolver().openInputStream(uri);
            long size = inputStream.available();
            inputStream.close();
            return size;
        } catch (Exception e) {
            return 0;
        }
    }

    private String obtenerNombreArchivo(Uri uri) {
        String nombre = uri.getLastPathSegment();
        if (nombre == null) {
            nombre = "documento_" + System.currentTimeMillis();
        }
        return nombre;
    }

    private void crearTraductor() {
        if (sourceLanguageCode == null || targetLanguageCode == null) {
            return;
        }

        if (sourceLanguageCode.equals(targetLanguageCode)) {
            return;
        }

        TranslatorOptions options = new TranslatorOptions.Builder()
                .setSourceLanguage(sourceLanguageCode)
                .setTargetLanguage(targetLanguageCode)
                .build();

        translator = Translation.getClient(options);

        // Descargar modelo si es necesario
        DownloadConditions conditions = new DownloadConditions.Builder()
                .requireWifi()
                .build();

        translator.downloadModelIfNeeded(conditions)
                .addOnSuccessListener(unused -> {
                    Log.d(TAG, "Modelo de traducción listo");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error al descargar modelo", e);
                });
    }

    private void traducirDocumento() {
        if (textoExtraido.isEmpty()) {
            Toast.makeText(getContext(),
                    "Primero selecciona un documento",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        if (translator == null) {
            Toast.makeText(getContext(),
                    "Configurando traductor...",
                    Toast.LENGTH_SHORT).show();
            crearTraductor();
            return;
        }

        progressDialog.setMessage("Traduciendo documento...");
        progressDialog.show();

        translator.translate(textoExtraido)
                .addOnSuccessListener(resultado -> {
                    progressDialog.dismiss();
                    textoTraducido = resultado;
                    etTextoTraducido.setText(textoTraducido);

                    Toast.makeText(getContext(),
                            "✅ Documento traducido",
                            Toast.LENGTH_SHORT).show();

                    // Guardar en historial
                    guardarEnHistorial();

                    // Agregar a recientes
                    agregarARecientes(nombreDocumentoActual);

                    // Verificar si ya es favorito
                    verificarEstadoFavorito();
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Log.e(TAG, "Error al traducir", e);
                    Toast.makeText(getContext(),
                            "❌ Error: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void guardarEnHistorial() {
        if (userId == -1) {
            return;
        }

        new Thread(() -> {
            try {
                long resultado = historialDAO.insertarHistorialSiNoExiste(
                        userId,
                        sourceLanguageId,
                        targetLanguageId,
                        translationTypeId,
                        textoExtraido,
                        textoTraducido
                );

                if (resultado != -1) {
                    Log.d(TAG, "✅ Historial guardado. ID: " + resultado);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error al guardar historial", e);
            }
        }).start();
    }

    private void toggleFavorito() {
        if (textoExtraido.isEmpty() || textoTraducido.isEmpty()) {
            Toast.makeText(getContext(),
                    "Primero traduce un documento",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        if (esFavorito) {
            // Eliminar de favoritos
            int resultado = favoriteDAO.eliminarFavoritoPorTextos(
                    userId, textoExtraido, textoTraducido);

            if (resultado > 0) {
                esFavorito = false;
                btnFavorito.setImageResource(R.drawable.ic_star_outline);
                Toast.makeText(getContext(),
                        "Removido de favoritos",
                        Toast.LENGTH_SHORT).show();
            }
        } else {
            // Agregar a favoritos
            FavoriteTranslationDAO.FavoriteTranslation favorito =
                    new FavoriteTranslationDAO.FavoriteTranslation(
                            userId,
                            sourceLanguageId,
                            targetLanguageId,
                            translationTypeId,
                            textoExtraido,
                            textoTraducido
                    );

            long resultado = favoriteDAO.insertarFavorito(favorito);

            if (resultado != -1) {
                esFavorito = true;
                btnFavorito.setImageResource(R.drawable.favorite);
                Toast.makeText(getContext(),
                        "⭐ Agregado a favoritos",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void verificarEstadoFavorito() {
        if (textoExtraido.isEmpty() || textoTraducido.isEmpty()) {
            return;
        }

        esFavorito = favoriteDAO.existeFavorito(userId, textoExtraido, textoTraducido);

        if (esFavorito) {
            btnFavorito.setImageResource(R.drawable.favorite);
        } else {
            btnFavorito.setImageResource(R.drawable.ic_star_outline);
        }
    }

    private void agregarARecientes(String nombreDocumento) {
        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, 0);
        Set<String> recientes = new HashSet<>(prefs.getStringSet(KEY_RECIENTES, new HashSet<>()));

        // Crear entrada con timestamp
        String entrada = nombreDocumento + "|" + System.currentTimeMillis();

        // Eliminar si ya existe (para actualizarlo al principio)
        recientes.removeIf(s -> s.startsWith(nombreDocumento + "|"));

        // Agregar nuevo
        recientes.add(entrada);

        // Mantener solo los 5 más recientes
        List<String> lista = new ArrayList<>(recientes);
        lista.sort((a, b) -> {
            long timeA = Long.parseLong(a.split("\\|")[1]);
            long timeB = Long.parseLong(b.split("\\|")[1]);
            return Long.compare(timeB, timeA);
        });

        if (lista.size() > MAX_RECIENTES) {
            lista = lista.subList(0, MAX_RECIENTES);
        }

        // Guardar
        prefs.edit().putStringSet(KEY_RECIENTES, new HashSet<>(lista)).apply();

        // Recargar UI
        cargarDocumentosRecientes();
    }

    private void cargarDocumentosRecientes() {
        containerDocumentosRecientes.removeAllViews();

        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, 0);
        Set<String> recientes = prefs.getStringSet(KEY_RECIENTES, new HashSet<>());

        if (recientes.isEmpty()) {
            tvNoDocumentos.setVisibility(View.VISIBLE);
            return;
        }

        tvNoDocumentos.setVisibility(View.GONE);

        // Ordenar por timestamp
        List<String> lista = new ArrayList<>(recientes);
        lista.sort((a, b) -> {
            long timeA = Long.parseLong(a.split("\\|")[1]);
            long timeB = Long.parseLong(b.split("\\|")[1]);
            return Long.compare(timeB, timeA);
        });

        // Crear tarjetas
        for (String entrada : lista) {
            String[] partes = entrada.split("\\|");
            String nombre = partes[0];
            long timestamp = Long.parseLong(partes[1]);

            View tarjeta = crearTarjetaDocumento(nombre, timestamp);
            containerDocumentosRecientes.addView(tarjeta);
        }
    }

    private View crearTarjetaDocumento(String nombre, long timestamp) {
        LinearLayout tarjeta = new LinearLayout(getContext());
        tarjeta.setOrientation(LinearLayout.VERTICAL);
        tarjeta.setPadding(24, 16, 24, 16);
        tarjeta.setBackgroundResource(android.R.drawable.dialog_holo_light_frame);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(8, 8, 8, 8);
        tarjeta.setLayoutParams(params);

        // Icono y nombre
        LinearLayout header = new LinearLayout(getContext());
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(android.view.Gravity.CENTER_VERTICAL);

        ImageView icono = new ImageView(getContext());
        if (nombre.toLowerCase().endsWith(".pdf")) {
            icono.setImageResource(android.R.drawable.ic_menu_report_image);
        } else {
            icono.setImageResource(android.R.drawable.ic_menu_edit);
        }
        LinearLayout.LayoutParams iconoParams = new LinearLayout.LayoutParams(48, 48);
        iconoParams.setMargins(0, 0, 16, 0);
        icono.setLayoutParams(iconoParams);
        header.addView(icono);

        TextView tvNombre = new TextView(getContext());
        tvNombre.setText(nombre);
        tvNombre.setTextSize(14);
        tvNombre.setTypeface(null, android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams nombreParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
        );
        tvNombre.setLayoutParams(nombreParams);
        header.addView(tvNombre);

        tarjeta.addView(header);

        // Fecha
        TextView tvFecha = new TextView(getContext());
        tvFecha.setText(formatearFecha(timestamp));
        tvFecha.setTextSize(11);
        tvFecha.setTextColor(getResources().getColor(android.R.color.darker_gray));
        tvFecha.setPadding(64, 4, 0, 0);
        tarjeta.addView(tvFecha);

        // Click listener
        tarjeta.setOnClickListener(v -> {
            Toast.makeText(getContext(),
                    "Documento: " + nombre,
                    Toast.LENGTH_SHORT).show();
        });

        return tarjeta;
    }

    private String formatearFecha(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }

    private void filtrarDocumentos(String query) {
        // Implementar filtrado si es necesario
        if (query.isEmpty()) {
            cargarDocumentosRecientes();
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (translator != null) {
            translator.close();
        }
    }
}