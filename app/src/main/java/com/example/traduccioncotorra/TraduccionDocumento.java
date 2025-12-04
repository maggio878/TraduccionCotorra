package com.example.traduccioncotorra;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
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
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import com.example.traduccioncotorra.DB.FavoriteTranslationDAO;
import com.example.traduccioncotorra.DB.HistorialDAO;
import com.example.traduccioncotorra.DB.LanguageDAO;
import com.example.traduccioncotorra.DB.TranslationTypeDAO;
import com.example.traduccioncotorra.DB.UserDAO;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
    private int translationTypeId = 3;

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

    // ⭐ Almacenar documentos con sus traducciones
    private Map<String, DocumentoReciente> documentosMap = new HashMap<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_traduccion_documento, container, false);

        PDFBoxResourceLoader.init(requireContext());

        inicializarDAOs();
        inicializarDocumentPicker();
        inicializarVistas(view);
        cargarIdiomasDesdeDB();
        configurarSpinners();
        configurarListeners();
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

        // ⭐ Mejorar colores de texto
        if (etTextoExtraido != null) {
            etTextoExtraido.setTextColor(Color.parseColor("#212121"));
            etTextoExtraido.setHintTextColor(Color.parseColor("#9E9E9E"));
        }

        if (etTextoTraducido != null) {
            etTextoTraducido.setTextColor(Color.parseColor("#1976D2"));
            etTextoTraducido.setHintTextColor(Color.parseColor("#9E9E9E"));
        }

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

        String[] nombresIdiomas = new String[idiomasDisponibles.size()];
        for (int i = 0; i < idiomasDisponibles.size(); i++) {
            nombresIdiomas[i] = idiomasDisponibles.get(i).name;
        }

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
        btnSeleccionarDocumento.setOnClickListener(v -> abrirSelectorDocumento());
        btnTraducir.setOnClickListener(v -> traducirDocumento());
        btnFavorito.setOnClickListener(v -> toggleFavorito());
        menuButtonConfig.setOnClickListener(v -> abrirConfiguracion());

        // ⭐ Búsqueda funcional
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

                nombreDocumentoActual = obtenerNombreArchivo(uri);

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

                    guardarEnHistorial();
                    agregarARecientes(nombreDocumentoActual, textoExtraido, textoTraducido);
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

    // ⭐ NUEVO: Agregar a recientes con traducción completa
    private void agregarARecientes(String nombreDocumento, String textoOriginal, String textoTrad) {
        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, 0);
        Set<String> recientes = new HashSet<>(prefs.getStringSet(KEY_RECIENTES, new HashSet<>()));

        // Preview (primeros 80 caracteres)
        String preview = textoOriginal.length() > 80 ?
                textoOriginal.substring(0, 80) + "..." : textoOriginal;

        String entrada = nombreDocumento + "|" + System.currentTimeMillis() + "|" + preview;

        // Guardar la traducción completa en el mapa
        documentosMap.put(nombreDocumento, new DocumentoReciente(
                nombreDocumento,
                System.currentTimeMillis(),
                textoOriginal,
                textoTrad
        ));

        recientes.removeIf(s -> s.startsWith(nombreDocumento + "|"));
        recientes.add(entrada);

        List<String> lista = new ArrayList<>(recientes);
        lista.sort((a, b) -> {
            long timeA = Long.parseLong(a.split("\\|")[1]);
            long timeB = Long.parseLong(b.split("\\|")[1]);
            return Long.compare(timeB, timeA);
        });

        if (lista.size() > MAX_RECIENTES) {
            lista = lista.subList(0, MAX_RECIENTES);
        }

        prefs.edit().putStringSet(KEY_RECIENTES, new HashSet<>(lista)).apply();
        cargarDocumentosRecientes();
    }

    private void cargarDocumentosRecientes() {
        containerDocumentosRecientes.removeAllViews();

        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, 0);
        Set<String> recientes = prefs.getStringSet(KEY_RECIENTES, new HashSet<>());

        if (recientes.isEmpty()) {
            tvNoDocumentos.setVisibility(View.VISIBLE);
            tvNoDocumentos.setText("📭 No hay documentos recientes\n\nSelecciona un documento PDF o Word para comenzar");
            return;
        }

        tvNoDocumentos.setVisibility(View.GONE);

        List<String> lista = new ArrayList<>(recientes);
        lista.sort((a, b) -> {
            try {
                long timeA = Long.parseLong(a.split("\\|")[1]);
                long timeB = Long.parseLong(b.split("\\|")[1]);
                return Long.compare(timeB, timeA);
            } catch (Exception e) {
                return 0;
            }
        });

        for (String entrada : lista) {
            String[] partes = entrada.split("\\|");
            if (partes.length < 2) continue;

            String nombre = partes[0];
            long timestamp = Long.parseLong(partes[1]);
            String preview = partes.length > 2 ? partes[2] : "";

            View tarjeta = crearTarjetaDocumento(nombre, timestamp, preview);
            containerDocumentosRecientes.addView(tarjeta);
        }
    }

    // ⭐ NUEVO: Tarjeta de documento mejorada con Material Design
    private View crearTarjetaDocumento(String nombre, long timestamp, String preview) {
        CardView cardView = new CardView(getContext());

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        cardParams.setMargins(dpToPx(8), dpToPx(8), dpToPx(8), dpToPx(8));
        cardView.setLayoutParams(cardParams);

        cardView.setRadius(dpToPx(16));
        cardView.setCardElevation(dpToPx(4));
        cardView.setCardBackgroundColor(Color.WHITE);
        cardView.setMaxCardElevation(dpToPx(6));
        cardView.setUseCompatPadding(true);
        cardView.setClickable(true);
        cardView.setFocusable(true);

        // Ripple effect
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            android.content.res.ColorStateList rippleColor =
                    android.content.res.ColorStateList.valueOf(Color.parseColor("#20000000"));

            android.graphics.drawable.RippleDrawable rippleDrawable =
                    new android.graphics.drawable.RippleDrawable(rippleColor, null, null);

            cardView.setForeground(rippleDrawable);
        }

        LinearLayout tarjeta = new LinearLayout(getContext());
        tarjeta.setOrientation(LinearLayout.VERTICAL);
        tarjeta.setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16));
        tarjeta.setBackgroundColor(Color.TRANSPARENT);

        // Header con icono y nombre
        LinearLayout header = new LinearLayout(getContext());
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(android.view.Gravity.CENTER_VERTICAL);

        // Icono del documento
        ImageView icono = new ImageView(getContext());
        if (nombre.toLowerCase().endsWith(".pdf")) {
            icono.setImageResource(android.R.drawable.ic_menu_report_image);
            icono.setColorFilter(Color.parseColor("#D32F2F")); // Rojo para PDF
        } else {
            icono.setImageResource(android.R.drawable.ic_menu_edit);
            icono.setColorFilter(Color.parseColor("#1976D2")); // Azul para Word
        }
        LinearLayout.LayoutParams iconoParams = new LinearLayout.LayoutParams(
                dpToPx(40), dpToPx(40)
        );
        iconoParams.setMargins(0, 0, dpToPx(12), 0);
        icono.setLayoutParams(iconoParams);
        header.addView(icono);

        // Contenedor de texto
        LinearLayout textoContainer = new LinearLayout(getContext());
        textoContainer.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams textoParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
        );
        textoContainer.setLayoutParams(textoParams);

        // Nombre del documento
        TextView tvNombre = new TextView(getContext());
        tvNombre.setText(nombre);
        tvNombre.setTextSize(16);
        tvNombre.setTextColor(Color.parseColor("#212121"));
        tvNombre.setTypeface(null, Typeface.BOLD);
        tvNombre.setMaxLines(1);
        tvNombre.setEllipsize(android.text.TextUtils.TruncateAt.END);
        textoContainer.addView(tvNombre);

        // Preview del texto
        if (!preview.isEmpty()) {
            TextView tvPreview = new TextView(getContext());
            tvPreview.setText(preview);
            tvPreview.setTextSize(12);
            tvPreview.setTextColor(Color.parseColor("#757575"));
            tvPreview.setMaxLines(2);
            tvPreview.setEllipsize(android.text.TextUtils.TruncateAt.END);
            LinearLayout.LayoutParams previewParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            previewParams.topMargin = dpToPx(4);
            tvPreview.setLayoutParams(previewParams);
            textoContainer.addView(tvPreview);
        }

        // Fecha
        TextView tvFecha = new TextView(getContext());
        tvFecha.setText("🕒 " + formatearFecha(timestamp));
        tvFecha.setTextSize(11);
        tvFecha.setTextColor(Color.parseColor("#9E9E9E"));
        LinearLayout.LayoutParams fechaParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        fechaParams.topMargin = dpToPx(4);
        tvFecha.setLayoutParams(fechaParams);
        textoContainer.addView(tvFecha);

        header.addView(textoContainer);

        // ⭐ Botones de acción
        LinearLayout botonesContainer = new LinearLayout(getContext());
        botonesContainer.setOrientation(LinearLayout.VERTICAL);

        // Botón copiar traducción
        ImageButton btnCopiar = new ImageButton(getContext());
        btnCopiar.setImageResource(android.R.drawable.ic_menu_set_as);
        btnCopiar.setBackgroundColor(Color.TRANSPARENT);
        btnCopiar.setColorFilter(Color.parseColor("#1976D2"));

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            android.util.TypedValue outValue = new android.util.TypedValue();
            getContext().getTheme().resolveAttribute(
                    android.R.attr.selectableItemBackgroundBorderless,
                    outValue,
                    true
            );
            btnCopiar.setBackground(getContext().getDrawable(outValue.resourceId));
        }

        LinearLayout.LayoutParams btnCopiarParams = new LinearLayout.LayoutParams(
                dpToPx(40), dpToPx(40)
        );
        btnCopiar.setLayoutParams(btnCopiarParams);
        btnCopiar.setOnClickListener(v -> {
            copiarTraduccion(nombre);
        });
        botonesContainer.addView(btnCopiar);

        // Botón eliminar
        ImageButton btnEliminar = new ImageButton(getContext());
        btnEliminar.setImageResource(android.R.drawable.ic_menu_delete);
        btnEliminar.setBackgroundColor(Color.TRANSPARENT);
        btnEliminar.setColorFilter(Color.parseColor("#D32F2F"));

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            android.util.TypedValue outValue = new android.util.TypedValue();
            getContext().getTheme().resolveAttribute(
                    android.R.attr.selectableItemBackgroundBorderless,
                    outValue,
                    true
            );
            btnEliminar.setBackground(getContext().getDrawable(outValue.resourceId));
        }

        LinearLayout.LayoutParams btnEliminarParams = new LinearLayout.LayoutParams(
                dpToPx(40), dpToPx(40)
        );
        btnEliminarParams.topMargin = dpToPx(4);
        btnEliminar.setLayoutParams(btnEliminarParams);
        btnEliminar.setOnClickListener(v -> {
            eliminarDocumentoReciente(nombre);
        });
        botonesContainer.addView(btnEliminar);

        header.addView(botonesContainer);

        tarjeta.addView(header);
        cardView.addView(tarjeta);

        // Click en la tarjeta para ver preview
        cardView.setOnClickListener(v -> {
            mostrarVistaPrevia(nombre, preview);
        });

        return cardView;
    }

    // ⭐ NUEVO: Copiar traducción al portapapeles
    private void copiarTraduccion(String nombreDocumento) {
        DocumentoReciente doc = documentosMap.get(nombreDocumento);

        if (doc != null && doc.textoTraducido != null && !doc.textoTraducido.isEmpty()) {
            ClipboardManager clipboard = (ClipboardManager)
                    getContext().getSystemService(Context.CLIPBOARD_SERVICE);

            ClipData clip = ClipData.newPlainText("Traducción", doc.textoTraducido);

            if (clipboard != null) {
                clipboard.setPrimaryClip(clip);
                Toast.makeText(getContext(),
                        "📋 Traducción copiada al portapapeles",
                        Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(getContext(),
                    "⚠️ No hay traducción disponible para este documento",
                    Toast.LENGTH_SHORT).show();
        }
    }

    // ⭐ NUEVO: Mostrar vista previa del documento
    private void mostrarVistaPrevia(String nombre, String preview) {
        DocumentoReciente doc = documentosMap.get(nombre);

        String mensaje = preview.isEmpty() ?
                "No hay vista previa disponible" :
                "📄 " + nombre + "\n\n" + preview;

        new android.app.AlertDialog.Builder(getContext())
                .setTitle("Vista previa del documento")
                .setMessage(mensaje)
                .setPositiveButton("Cerrar", null)
                .setNeutralButton("Copiar traducción", (dialog, which) -> {
                    copiarTraduccion(nombre);
                })
                .show();
    }

    // ⭐ NUEVO: Eliminar documento de recientes
    private void eliminarDocumentoReciente(String nombreDocumento) {
        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, 0);
        Set<String> recientes = new HashSet<>(prefs.getStringSet(KEY_RECIENTES, new HashSet<>()));

        recientes.removeIf(s -> s.startsWith(nombreDocumento + "|"));
        documentosMap.remove(nombreDocumento);

        prefs.edit().putStringSet(KEY_RECIENTES, recientes).apply();

        cargarDocumentosRecientes();

        Toast.makeText(getContext(),
                "🗑️ Documento eliminado de recientes",
                Toast.LENGTH_SHORT).show();
    }

    // ⭐ NUEVO: Filtrar documentos (SearchBar funcional)
    private void filtrarDocumentos(String query) {
        containerDocumentosRecientes.removeAllViews();

        if (query.isEmpty()) {
            cargarDocumentosRecientes();
            return;
        }

        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, 0);
        Set<String> recientes = prefs.getStringSet(KEY_RECIENTES, new HashSet<>());

        if (recientes.isEmpty()) {
            tvNoDocumentos.setVisibility(View.VISIBLE);
            tvNoDocumentos.setText("📭 No hay documentos recientes");
            return;
        }

        String queryLower = query.toLowerCase();
        List<String> listaFiltrada = new ArrayList<>();

        for (String entrada : recientes) {
            String[] partes = entrada.split("\\|");
            String nombre = partes[0];
            String preview = partes.length > 2 ? partes[2] : "";

            if (nombre.toLowerCase().contains(queryLower) ||
                    preview.toLowerCase().contains(queryLower)) {
                listaFiltrada.add(entrada);
            }
        }

        if (listaFiltrada.isEmpty()) {
            tvNoDocumentos.setVisibility(View.VISIBLE);
            tvNoDocumentos.setText("🔍 No se encontraron documentos que coincidan con:\n\"" + query + "\"");
            return;
        }

        tvNoDocumentos.setVisibility(View.GONE);

        listaFiltrada.sort((a, b) -> {
            try {
                long timeA = Long.parseLong(a.split("\\|")[1]);
                long timeB = Long.parseLong(b.split("\\|")[1]);
                return Long.compare(timeB, timeA);
            } catch (Exception e) {
                return 0;
            }
        });

        for (String entrada : listaFiltrada) {
            String[] partes = entrada.split("\\|");
            String nombre = partes[0];
            long timestamp = Long.parseLong(partes[1]);
            String preview = partes.length > 2 ? partes[2] : "";

            View tarjeta = crearTarjetaDocumento(nombre, timestamp, preview);
            containerDocumentosRecientes.addView(tarjeta);
        }
    }

    // ⭐ Formatear fecha relativa
    private String formatearFecha(long timestamp) {
        long diferencia = System.currentTimeMillis() - timestamp;
        long segundos = diferencia / 1000;
        long minutos = segundos / 60;
        long horas = minutos / 60;
        long dias = horas / 24;

        if (dias > 7) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            return sdf.format(new Date(timestamp));
        } else if (dias > 0) {
            return "Hace " + dias + (dias == 1 ? " día" : " días");
        } else if (horas > 0) {
            return "Hace " + horas + (horas == 1 ? " hora" : " horas");
        } else if (minutos > 0) {
            return "Hace " + minutos + (minutos == 1 ? " minuto" : " minutos");
        } else {
            return "Hace unos segundos";
        }
    }

    private int dpToPx(int dp) {
        if (getResources() != null) {
            float density = getResources().getDisplayMetrics().density;
            return Math.round(dp * density);
        }
        return dp;
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

    // ⭐ Clase interna para almacenar documentos recientes con sus traducciones
    private static class DocumentoReciente {
        String nombre;
        long timestamp;
        String textoOriginal;
        String textoTraducido;

        DocumentoReciente(String nombre, long timestamp, String textoOriginal, String textoTraducido) {
            this.nombre = nombre;
            this.timestamp = timestamp;
            this.textoOriginal = textoOriginal;
            this.textoTraducido = textoTraducido;
        }
    }
}