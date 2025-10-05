package com.example.traduccioncotorra;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

public class TraduccionDocumento extends Fragment {

    private EditText etBuscarDocumentos;
    private ImageButton menuButtonConfig;
    private ActivityResultLauncher<Intent> seleccionadorArchivos;

    // Variables para almacenar datos
    private String ultimoTextoExtraido = "";
    private String nombreArchivoSeleccionado = "";

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Configurar el launcher para seleccionar archivos
        seleccionadorArchivos = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null) {
                            procesarArchivo(uri);
                        }
                    }
                }
        );
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_traduccion_documento, container, false);

        // Inicializar vistas
        inicializarVistas(view);

        // Configurar listeners
        configurarListeners();

        // Obtener argumentos
        if (getArguments() != null) {
            String nombreUsuario = getArguments().getString("USUARIO");
            if (nombreUsuario != null) {
                Toast.makeText(getContext(), "Documentos - " + nombreUsuario,
                        Toast.LENGTH_SHORT).show();
            }
        }

        return view;
    }

    private void inicializarVistas(View view) {
        etBuscarDocumentos = view.findViewById(R.id.et_buscar_documentos);
        menuButtonConfig = view.findViewById(R.id.menu_button_config);
    }

    private void configurarListeners() {
        // Botón de configuración
        if (menuButtonConfig != null) {
            menuButtonConfig.setOnClickListener(v -> {
                Toast.makeText(getContext(), "Configuración", Toast.LENGTH_SHORT).show();
            });
        }

        // Búsqueda de documentos
        if (etBuscarDocumentos != null) {
            etBuscarDocumentos.setOnEditorActionListener((v, actionId, event) -> {
                String busqueda = etBuscarDocumentos.getText().toString().trim();
                if (!busqueda.isEmpty()) {
                    buscarDocumento(busqueda);
                }
                return true;
            });
        }

        // Configurar clicks en los documentos de ejemplo (si existen en el layout)
        configurarDocumentosEjemplo(getView());
    }

    private void configurarDocumentosEjemplo(View view) {
        if (view == null) return;

        // Aquí puedes agregar listeners a los documentos de ejemplo del grid
        // Por ejemplo, al hacer clic en cualquier documento, abrir el selector de archivos
        View documentoEjemplo1 = view.findViewById(R.id.recent_documents_container);
        if (documentoEjemplo1 != null) {
            documentoEjemplo1.setOnClickListener(v -> abrirSelectorArchivos());
        }
    }

    // Método para abrir el selector de archivos del sistema
    private void abrirSelectorArchivos() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*"); // Todos los archivos

        // Tipos de archivo permitidos
        String[] mimeTypes = {
                "text/plain",           // .txt
                "application/pdf",      // .pdf
                "image/*",              // imágenes
                "application/msword",   // .doc
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document" // .docx
        };
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        try {
            seleccionadorArchivos.launch(Intent.createChooser(intent, "Selecciona un documento"));
        } catch (Exception e) {
            Toast.makeText(getContext(), "Error al abrir selector de archivos",
                    Toast.LENGTH_SHORT).show();
        }
    }

    // Procesar el archivo seleccionado
    private void procesarArchivo(Uri uri) {
        String nombreArchivo = obtenerNombreArchivo(uri);
        nombreArchivoSeleccionado = nombreArchivo;

        Toast.makeText(getContext(), "Archivo seleccionado: " + nombreArchivo,
                Toast.LENGTH_SHORT).show();

        // Intentar extraer texto del archivo
        if (nombreArchivo.endsWith(".txt")) {
            extraerTextoDeArchivo(uri);
        } else if (nombreArchivo.endsWith(".pdf")) {
            Toast.makeText(getContext(),
                    "Procesamiento de PDF - Implementar con librería PDF",
                    Toast.LENGTH_LONG).show();
            // Aquí necesitarías una librería como PDFBox o similar
        } else if (esImagen(nombreArchivo)) {
            Toast.makeText(getContext(),
                    "Procesamiento de imagen - Implementar con OCR (ML Kit)",
                    Toast.LENGTH_LONG).show();
            // Aquí necesitarías ML Kit Text Recognition
        } else {
            Toast.makeText(getContext(),
                    "Formato no soportado por ahora",
                    Toast.LENGTH_SHORT).show();
        }
    }

    // Extraer texto de archivo .txt
    private void extraerTextoDeArchivo(Uri uri) {
        try {
            InputStream inputStream = getActivity().getContentResolver().openInputStream(uri);
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder texto = new StringBuilder();
            String linea;

            while ((linea = reader.readLine()) != null) {
                texto.append(linea).append("\n");
            }

            reader.close();
            inputStream.close();

            ultimoTextoExtraido = texto.toString();

            Toast.makeText(getContext(),
                    "Texto extraído (" + ultimoTextoExtraido.length() + " caracteres)",
                    Toast.LENGTH_SHORT).show();

            // Aquí podrías navegar a otro fragment para mostrar y traducir el texto
            // o mostrar un diálogo con el texto extraído

        } catch (Exception e) {
            Toast.makeText(getContext(),
                    "Error al leer el archivo: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
        }
    }

    // Obtener nombre del archivo desde URI
    private String obtenerNombreArchivo(Uri uri) {
        String nombre = uri.getLastPathSegment();
        if (nombre == null || nombre.isEmpty()) {
            nombre = "documento_sin_nombre";
        }
        return nombre;
    }

    // Verificar si es una imagen
    private boolean esImagen(String nombreArchivo) {
        String extension = nombreArchivo.toLowerCase();
        return extension.endsWith(".jpg") || extension.endsWith(".jpeg") ||
                extension.endsWith(".png") || extension.endsWith(".gif") ||
                extension.endsWith(".bmp");
    }

    // Buscar en documentos recientes (simulado)
    private void buscarDocumento(String busqueda) {
        Toast.makeText(getContext(),
                "Buscando: " + busqueda + " en documentos recientes",
                Toast.LENGTH_SHORT).show();

        // Aquí implementarías la búsqueda en una base de datos local
        // o en SharedPreferences donde guardas los documentos recientes
    }

    // Método público para obtener el último texto extraído
    public String getUltimoTextoExtraido() {
        return ultimoTextoExtraido;
    }
}