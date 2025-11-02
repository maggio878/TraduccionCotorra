package com.example.traduccioncotorra;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.traduccioncotorra.DB.CategoryDAO;
import com.example.traduccioncotorra.DB.LanguageDAO;
import com.example.traduccioncotorra.DB.TranslationTypeDAO;
import com.example.traduccioncotorra.DB.WordDAO;
import com.example.traduccioncotorra.DB.WordLanguageDAO;
import com.google.android.material.button.MaterialButton;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdminCatalogos extends Fragment {

    private LinearLayout contenedorCatalogos;
    private LanguageDAO languageDAO;
    private TranslationTypeDAO translationTypeDAO;
    private CategoryDAO categoryDAO;
    private WordDAO wordDAO;
    private WordLanguageDAO wordLanguageDAO;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin_catalogos, container, false);

        // Inicializar DAOs
        languageDAO = new LanguageDAO(requireContext());
        translationTypeDAO = new TranslationTypeDAO(requireContext());
        categoryDAO = new CategoryDAO(requireContext());
        wordDAO = new WordDAO(requireContext());
        wordLanguageDAO = new WordLanguageDAO(requireContext());

        // Inicializar vistas
        inicializarVistas(view);

        // Cargar catálogos
        mostrarCatalogos();

        return view;
    }

    private void inicializarVistas(View view) {
        contenedorCatalogos = view.findViewById(R.id.contenedor_catalogos);

        // Configurar botón volver
        view.findViewById(R.id.btn_volver).setOnClickListener(v -> {
            requireActivity().onBackPressed();
        });
    }

    private void mostrarCatalogos() {
        contenedorCatalogos.removeAllViews();

        // Sección: Idiomas
        agregarSeccionIdiomas();

        // Separador
        agregarSeparador();

        // Sección: Tipos de Traducción
        agregarSeccionTiposTraduccion();

        // Separador
        agregarSeparador();

        // Sección: Categorías
        agregarSeccionCategorias();

        // Separador
        agregarSeparador();

        // Sección: Diccionario (Palabras)
        agregarSeccionDiccionario();
    }

    // ==================== SECCIÓN IDIOMAS ====================

    private void agregarSeccionIdiomas() {
        // Título
        TextView titulo = new TextView(getContext());
        titulo.setText("IDIOMAS");
        titulo.setTextSize(18);
        titulo.setTextColor(getResources().getColor(R.color.blue_menu));
        titulo.setPadding(0, 16, 0, 16);
        contenedorCatalogos.addView(titulo);

        // Botón agregar idioma
        MaterialButton btnAgregarIdioma = new MaterialButton(getContext());
        btnAgregarIdioma.setText("+ Agregar Idioma");
        btnAgregarIdioma.setBackgroundTintList(getResources().getColorStateList(R.color.green_menu));
        btnAgregarIdioma.setTextColor(getResources().getColor(android.R.color.white));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, 16);
        btnAgregarIdioma.setLayoutParams(params);
        btnAgregarIdioma.setOnClickListener(v -> mostrarDialogoAgregarIdioma());
        contenedorCatalogos.addView(btnAgregarIdioma);

        // Lista de idiomas
        List<LanguageDAO.Language> idiomas = languageDAO.obtenerTodosLosIdiomas();

        if (idiomas.isEmpty()) {
            TextView tvVacio = new TextView(getContext());
            tvVacio.setText("No hay idiomas registrados");
            tvVacio.setTextColor(getResources().getColor(android.R.color.darker_gray));
            tvVacio.setPadding(16, 8, 16, 8);
            contenedorCatalogos.addView(tvVacio);
        } else {
            for (LanguageDAO.Language idioma : idiomas) {
                contenedorCatalogos.addView(crearTarjetaIdioma(idioma));
            }
        }
    }

    private View crearTarjetaIdioma(LanguageDAO.Language idioma) {
        LinearLayout tarjeta = new LinearLayout(getContext());
        tarjeta.setOrientation(LinearLayout.HORIZONTAL);
        tarjeta.setPadding(16, 12, 16, 12);
        tarjeta.setBackgroundResource(android.R.drawable.dialog_holo_light_frame);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 4, 0, 4);
        tarjeta.setLayoutParams(params);

        // Información del idioma
        LinearLayout infoLayout = new LinearLayout(getContext());
        infoLayout.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams infoParams = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
        );
        infoLayout.setLayoutParams(infoParams);

        // Nombre y código
        TextView tvNombre = new TextView(getContext());
        tvNombre.setText(idioma.name + " (" + idioma.code + ")");
        tvNombre.setTextSize(16);
        tvNombre.setTextColor(getResources().getColor(android.R.color.black));
        infoLayout.addView(tvNombre);

        // ⭐ NUEVO: Mostrar ApiCode
        TextView tvApiCode = new TextView(getContext());
        String apiCodeTexto = "API: " + (idioma.apiCode != null ? idioma.apiCode : "N/A");
        tvApiCode.setText(apiCodeTexto);
        tvApiCode.setTextSize(11);
        tvApiCode.setTextColor(getResources().getColor(android.R.color.darker_gray));
        infoLayout.addView(tvApiCode);

        // Estado
        TextView tvEstado = new TextView(getContext());
        tvEstado.setText(idioma.isActive == 1 ? "✓ Activo" : "✗ Inactivo");
        tvEstado.setTextSize(12);
        tvEstado.setTextColor(idioma.isActive == 1 ?
                getResources().getColor(R.color.green_menu) :
                getResources().getColor(android.R.color.holo_red_dark));
        infoLayout.addView(tvEstado);

        tarjeta.addView(infoLayout);

        // Botones
        LinearLayout botonesLayout = new LinearLayout(getContext());
        botonesLayout.setOrientation(LinearLayout.HORIZONTAL);

        MaterialButton btnEditar = new MaterialButton(getContext());
        btnEditar.setText("Editar");
        btnEditar.setTextSize(12);
        btnEditar.setPadding(8, 4, 8, 4);
        btnEditar.setOnClickListener(v -> mostrarDialogoEditarIdioma(idioma));
        botonesLayout.addView(btnEditar);

        MaterialButton btnEliminar = new MaterialButton(getContext());
        btnEliminar.setText("Eliminar");
        btnEliminar.setTextSize(12);
        btnEliminar.setPadding(8, 4, 8, 4);
        btnEliminar.setBackgroundTintList(getResources().getColorStateList(android.R.color.holo_red_dark));
        btnEliminar.setOnClickListener(v -> confirmarEliminarIdioma(idioma));
        botonesLayout.addView(btnEliminar);

        tarjeta.addView(botonesLayout);

        return tarjeta;
    }

    /**
     * ⭐ ACTUALIZADO: Diálogo con campo ApiCode
     */
    private void mostrarDialogoAgregarIdioma() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Agregar Idioma");

        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 20, 50, 20);

        // Campo nombre
        final EditText etNombre = new EditText(getContext());
        etNombre.setHint("Nombre del idioma (ej: Español)");
        layout.addView(etNombre);

        // Campo código
        final EditText etCodigo = new EditText(getContext());
        etCodigo.setHint("Código (ej: es, en, fr)");
        etCodigo.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        layout.addView(etCodigo);

        // ⭐ NUEVO: Campo ApiCode
        final EditText etApiCode = new EditText(getContext());
        etApiCode.setHint("Código API ML Kit (ej: es, en, fr)");
        etApiCode.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        layout.addView(etApiCode);

        // Nota informativa
        TextView tvNota = new TextView(getContext());
        tvNota.setText("💡 El código API debe coincidir con los códigos de ML Kit Translation");
        tvNota.setTextSize(11);
        tvNota.setTextColor(getResources().getColor(android.R.color.darker_gray));
        tvNota.setPadding(0, 8, 0, 0);
        layout.addView(tvNota);

        builder.setView(layout);

        builder.setPositiveButton("Agregar", (dialog, which) -> {
            String nombre = etNombre.getText().toString().trim();
            String codigo = etCodigo.getText().toString().trim();
            String apiCode = etApiCode.getText().toString().trim();

            // Validaciones
            if (nombre.isEmpty() || codigo.isEmpty()) {
                Toast.makeText(getContext(), "Nombre y código son requeridos", Toast.LENGTH_SHORT).show();
                return;
            }

            // Si ApiCode está vacío, usar Code por defecto
            if (apiCode.isEmpty()) {
                apiCode = codigo;
            }

            if (languageDAO.existeCodigoIdioma(codigo)) {
                Toast.makeText(getContext(), "El código ya existe", Toast.LENGTH_SHORT).show();
                return;
            }

            if (languageDAO.existeApiCode(apiCode)) {
                Toast.makeText(getContext(), "El código API ya existe", Toast.LENGTH_SHORT).show();
                return;
            }

            // Crear idioma con ApiCode
            LanguageDAO.Language nuevoIdioma = new LanguageDAO.Language(nombre, codigo, apiCode);
            long resultado = languageDAO.insertarIdioma(nuevoIdioma);

            if (resultado != -1) {
                Toast.makeText(getContext(), "Idioma agregado exitosamente", Toast.LENGTH_SHORT).show();
                mostrarCatalogos();
            } else {
                Toast.makeText(getContext(), "Error al agregar idioma", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancelar", null);
        builder.show();
    }

    /**
      Diálogo de edición con ApiCode
     */
    private void mostrarDialogoEditarIdioma(LanguageDAO.Language idioma) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Editar Idioma");

        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 20, 50, 20);

        // Campo nombre
        final EditText etNombre = new EditText(getContext());
        etNombre.setHint("Nombre del idioma");
        etNombre.setText(idioma.name);
        layout.addView(etNombre);

        // Campo código
        final EditText etCodigo = new EditText(getContext());
        etCodigo.setHint("Código");
        etCodigo.setText(idioma.code);
        layout.addView(etCodigo);

        // ⭐ NUEVO: Campo ApiCode
        final EditText etApiCode = new EditText(getContext());
        etApiCode.setHint("Código API ML Kit");
        etApiCode.setText(idioma.apiCode != null ? idioma.apiCode : idioma.code);
        layout.addView(etApiCode);

        builder.setView(layout);

        builder.setPositiveButton("Guardar", (dialog, which) -> {
            idioma.name = etNombre.getText().toString().trim();
            idioma.code = etCodigo.getText().toString().trim();
            idioma.apiCode = etApiCode.getText().toString().trim();

            // Si ApiCode está vacío, usar Code
            if (idioma.apiCode.isEmpty()) {
                idioma.apiCode = idioma.code;
            }

            int resultado = languageDAO.actualizarIdioma(idioma);

            if (resultado > 0) {
                Toast.makeText(getContext(), "Idioma actualizado", Toast.LENGTH_SHORT).show();
                mostrarCatalogos();
            } else {
                Toast.makeText(getContext(), "Error al actualizar", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancelar", null);
        builder.show();
    }

    private void confirmarEliminarIdioma(LanguageDAO.Language idioma) {
        new AlertDialog.Builder(getContext())
                .setTitle("Eliminar Idioma")
                .setMessage("¿Estás seguro de eliminar " + idioma.name + "?")
                .setPositiveButton("Eliminar", (dialog, which) -> {
                    int resultado = languageDAO.eliminarIdioma(idioma.languageId);
                    if (resultado > 0) {
                        Toast.makeText(getContext(), "Idioma eliminado", Toast.LENGTH_SHORT).show();
                        mostrarCatalogos();
                    } else {
                        Toast.makeText(getContext(), "Error al eliminar", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    // ==================== SECCIÓN TIPOS DE TRADUCCIÓN ====================
    // (El código de esta sección permanece igual)

    private void agregarSeccionTiposTraduccion() {
        TextView titulo = new TextView(getContext());
        titulo.setText("TIPOS DE TRADUCCIÓN");
        titulo.setTextSize(18);
        titulo.setTextColor(getResources().getColor(R.color.blue_menu));
        titulo.setPadding(0, 16, 0, 16);
        contenedorCatalogos.addView(titulo);

        MaterialButton btnAgregar = new MaterialButton(getContext());
        btnAgregar.setText("+ Agregar Tipo");
        btnAgregar.setBackgroundTintList(getResources().getColorStateList(R.color.green_menu));
        btnAgregar.setTextColor(getResources().getColor(android.R.color.white));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, 16);
        btnAgregar.setLayoutParams(params);
        btnAgregar.setOnClickListener(v -> mostrarDialogoAgregarTipo());
        contenedorCatalogos.addView(btnAgregar);

        List<TranslationTypeDAO.TranslationType> tipos = translationTypeDAO.obtenerTodosLosTipos();

        for (TranslationTypeDAO.TranslationType tipo : tipos) {
            contenedorCatalogos.addView(crearTarjetaTipo(tipo));
        }
    }

    private View crearTarjetaTipo(TranslationTypeDAO.TranslationType tipo) {
        LinearLayout tarjeta = new LinearLayout(getContext());
        tarjeta.setOrientation(LinearLayout.HORIZONTAL);
        tarjeta.setPadding(16, 12, 16, 12);
        tarjeta.setBackgroundResource(android.R.drawable.dialog_holo_light_frame);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 4, 0, 4);
        tarjeta.setLayoutParams(params);

        TextView tvNombre = new TextView(getContext());
        tvNombre.setText(tipo.name);
        tvNombre.setTextSize(16);
        LinearLayout.LayoutParams tvParams = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
        );
        tvNombre.setLayoutParams(tvParams);
        tarjeta.addView(tvNombre);

        MaterialButton btnEditar = new MaterialButton(getContext());
        btnEditar.setText("Editar");
        btnEditar.setTextSize(12);
        btnEditar.setOnClickListener(v -> mostrarDialogoEditarTipo(tipo));
        tarjeta.addView(btnEditar);

        MaterialButton btnEliminar = new MaterialButton(getContext());
        btnEliminar.setText("Eliminar");
        btnEliminar.setTextSize(12);
        btnEliminar.setBackgroundTintList(getResources().getColorStateList(android.R.color.holo_red_dark));
        btnEliminar.setOnClickListener(v -> confirmarEliminarTipo(tipo));
        tarjeta.addView(btnEliminar);

        return tarjeta;
    }

    private void mostrarDialogoAgregarTipo() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Agregar Tipo de Traducción");

        final EditText etNombre = new EditText(getContext());
        etNombre.setHint("Nombre (ej: Texto, Cámara, Documento)");
        etNombre.setPadding(50, 20, 50, 20);

        builder.setView(etNombre);

        builder.setPositiveButton("Agregar", (dialog, which) -> {
            String nombre = etNombre.getText().toString().trim();

            if (nombre.isEmpty()) {
                Toast.makeText(getContext(), "El nombre es requerido", Toast.LENGTH_SHORT).show();
                return;
            }

            TranslationTypeDAO.TranslationType nuevoTipo = new TranslationTypeDAO.TranslationType(nombre);
            long resultado = translationTypeDAO.insertarTipoTraduccion(nuevoTipo);

            if (resultado != -1) {
                Toast.makeText(getContext(), "Tipo agregado", Toast.LENGTH_SHORT).show();
                mostrarCatalogos();
            }
        });

        builder.setNegativeButton("Cancelar", null);
        builder.show();
    }

    private void mostrarDialogoEditarTipo(TranslationTypeDAO.TranslationType tipo) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Editar Tipo");

        final EditText etNombre = new EditText(getContext());
        etNombre.setText(tipo.name);
        etNombre.setPadding(50, 20, 50, 20);

        builder.setView(etNombre);

        builder.setPositiveButton("Guardar", (dialog, which) -> {
            tipo.name = etNombre.getText().toString().trim();
            translationTypeDAO.actualizarTipoTraduccion(tipo);
            Toast.makeText(getContext(), "Tipo actualizado", Toast.LENGTH_SHORT).show();
            mostrarCatalogos();
        });

        builder.setNegativeButton("Cancelar", null);
        builder.show();
    }

    private void confirmarEliminarTipo(TranslationTypeDAO.TranslationType tipo) {
        new AlertDialog.Builder(getContext())
                .setTitle("Eliminar Tipo")
                .setMessage("¿Eliminar " + tipo.name + "?")
                .setPositiveButton("Eliminar", (dialog, which) -> {
                    translationTypeDAO.eliminarTipoTraduccion(tipo.idTypeTranslation);
                    Toast.makeText(getContext(), "Tipo eliminado", Toast.LENGTH_SHORT).show();
                    mostrarCatalogos();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    // ==================== SECCIÓN CATEGORÍAS ====================
    // (El código de esta sección permanece igual)

    private void agregarSeccionCategorias() {
        TextView titulo = new TextView(getContext());
        titulo.setText("CATEGORÍAS DEL DICCIONARIO");
        titulo.setTextSize(18);
        titulo.setTextColor(getResources().getColor(R.color.blue_menu));
        titulo.setPadding(0, 16, 0, 16);
        contenedorCatalogos.addView(titulo);

        MaterialButton btnAgregar = new MaterialButton(getContext());
        btnAgregar.setText("+ Agregar Categoría");
        btnAgregar.setBackgroundTintList(getResources().getColorStateList(R.color.green_menu));
        btnAgregar.setTextColor(getResources().getColor(android.R.color.white));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, 16);
        btnAgregar.setLayoutParams(params);
        btnAgregar.setOnClickListener(v -> mostrarDialogoAgregarCategoria());
        contenedorCatalogos.addView(btnAgregar);

        List<CategoryDAO.Category> categorias = categoryDAO.obtenerTodasLasCategorias();

        for (CategoryDAO.Category categoria : categorias) {
            contenedorCatalogos.addView(crearTarjetaCategoria(categoria));
        }
    }

    private View crearTarjetaCategoria(CategoryDAO.Category categoria) {
        LinearLayout tarjeta = new LinearLayout(getContext());
        tarjeta.setOrientation(LinearLayout.HORIZONTAL);
        tarjeta.setPadding(16, 12, 16, 12);
        tarjeta.setBackgroundResource(android.R.drawable.dialog_holo_light_frame);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 4, 0, 4);
        tarjeta.setLayoutParams(params);

        LinearLayout infoLayout = new LinearLayout(getContext());
        infoLayout.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams infoParams = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
        );
        infoLayout.setLayoutParams(infoParams);

        TextView tvNombre = new TextView(getContext());
        tvNombre.setText(categoria.name);
        tvNombre.setTextSize(16);
        infoLayout.addView(tvNombre);

        if (categoria.description != null && !categoria.description.isEmpty()) {
            TextView tvDesc = new TextView(getContext());
            tvDesc.setText(categoria.description);
            tvDesc.setTextSize(12);
            tvDesc.setTextColor(getResources().getColor(android.R.color.darker_gray));
            infoLayout.addView(tvDesc);
        }

        tarjeta.addView(infoLayout);

        MaterialButton btnEditar = new MaterialButton(getContext());
        btnEditar.setText("Editar");
        btnEditar.setTextSize(12);
        btnEditar.setOnClickListener(v -> mostrarDialogoEditarCategoria(categoria));
        tarjeta.addView(btnEditar);

        MaterialButton btnEliminar = new MaterialButton(getContext());
        btnEliminar.setText("Eliminar");
        btnEliminar.setTextSize(12);
        btnEliminar.setBackgroundTintList(getResources().getColorStateList(android.R.color.holo_red_dark));
        btnEliminar.setOnClickListener(v -> confirmarEliminarCategoria(categoria));
        tarjeta.addView(btnEliminar);

        return tarjeta;
    }

    private void mostrarDialogoAgregarCategoria() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Agregar Categoría");

        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 20, 50, 20);

        final EditText etNombre = new EditText(getContext());
        etNombre.setHint("Nombre (ej: Saludos, Compras)");
        layout.addView(etNombre);

        final EditText etDescripcion = new EditText(getContext());
        etDescripcion.setHint("Descripción (opcional)");
        layout.addView(etDescripcion);

        builder.setView(layout);

        builder.setPositiveButton("Agregar", (dialog, which) -> {
            String nombre = etNombre.getText().toString().trim();
            String descripcion = etDescripcion.getText().toString().trim();

            if (nombre.isEmpty()) {
                Toast.makeText(getContext(), "El nombre es requerido", Toast.LENGTH_SHORT).show();
                return;
            }

            CategoryDAO.Category nuevaCategoria = new CategoryDAO.Category(nombre, descripcion);
            long resultado = categoryDAO.insertarCategoria(nuevaCategoria);

            if (resultado != -1) {
                Toast.makeText(getContext(), "Categoría agregada", Toast.LENGTH_SHORT).show();
                mostrarCatalogos();
            }
        });

        builder.setNegativeButton("Cancelar", null);
        builder.show();
    }

    private void mostrarDialogoEditarCategoria(CategoryDAO.Category categoria) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Editar Categoría");

        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 20, 50, 20);

        final EditText etNombre = new EditText(getContext());
        etNombre.setText(categoria.name);
        layout.addView(etNombre);

        final EditText etDescripcion = new EditText(getContext());
        etDescripcion.setText(categoria.description);
        layout.addView(etDescripcion);

        builder.setView(layout);

        builder.setPositiveButton("Guardar", (dialog, which) -> {
            categoria.name = etNombre.getText().toString().trim();
            categoria.description = etDescripcion.getText().toString().trim();
            categoryDAO.actualizarCategoria(categoria);
            Toast.makeText(getContext(), "Categoría actualizada", Toast.LENGTH_SHORT).show();
            mostrarCatalogos();
        });

        builder.setNegativeButton("Cancelar", null);
        builder.show();
    }

    private void confirmarEliminarCategoria(CategoryDAO.Category categoria) {
        new AlertDialog.Builder(getContext())
                .setTitle("Eliminar Categoría")
                .setMessage("¿Eliminar " + categoria.name + "?")
                .setPositiveButton("Eliminar", (dialog, which) -> {
                    categoryDAO.eliminarCategoria(categoria.categoryId);
                    Toast.makeText(getContext(), "Categoría eliminada", Toast.LENGTH_SHORT).show();
                    mostrarCatalogos();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    // ==================== ⭐ SECCIÓN DICCIONARIO (PALABRAS) ====================
    private void agregarSeccionDiccionario() {
        TextView titulo = new TextView(getContext());
        titulo.setText("DICCIONARIO (PALABRAS)");
        titulo.setTextSize(18);
        titulo.setTextColor(getResources().getColor(R.color.blue_menu));
        titulo.setPadding(0, 16, 0, 16);
        contenedorCatalogos.addView(titulo);

        MaterialButton btnAgregar = new MaterialButton(getContext());
        btnAgregar.setText("+ Agregar Palabra");
        btnAgregar.setBackgroundTintList(getResources().getColorStateList(R.color.green_menu));
        btnAgregar.setTextColor(getResources().getColor(android.R.color.white));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, 16);
        btnAgregar.setLayoutParams(params);
        btnAgregar.setOnClickListener(v -> mostrarDialogoAgregarPalabra());
        contenedorCatalogos.addView(btnAgregar);

        List<WordDAO.Word> palabras = wordDAO.obtenerTodasLasPalabras();

        if (palabras.isEmpty()) {
            TextView tvVacio = new TextView(getContext());
            tvVacio.setText("No hay palabras en el diccionario");
            tvVacio.setTextColor(getResources().getColor(android.R.color.darker_gray));
            tvVacio.setPadding(16, 8, 16, 8);
            contenedorCatalogos.addView(tvVacio);
        } else {
            for (WordDAO.Word palabra : palabras) {
                contenedorCatalogos.addView(crearTarjetaPalabra(palabra));
            }
        }
    }

    private View crearTarjetaPalabra(WordDAO.Word palabra) {
        LinearLayout tarjeta = new LinearLayout(getContext());
        tarjeta.setOrientation(LinearLayout.VERTICAL);
        tarjeta.setPadding(16, 12, 16, 12);
        tarjeta.setBackgroundResource(android.R.drawable.dialog_holo_light_frame);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 4, 0, 4);
        tarjeta.setLayoutParams(params);

        // Primera fila: Palabra y botones
        LinearLayout primeraFila = new LinearLayout(getContext());
        primeraFila.setOrientation(LinearLayout.HORIZONTAL);

        LinearLayout infoLayout = new LinearLayout(getContext());
        infoLayout.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams infoParams = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
        );
        infoLayout.setLayoutParams(infoParams);

        TextView tvPalabra = new TextView(getContext());
        tvPalabra.setText(palabra.word);
        tvPalabra.setTextSize(16);
        tvPalabra.setTextColor(getResources().getColor(android.R.color.black));
        tvPalabra.setTypeface(null, android.graphics.Typeface.BOLD);
        infoLayout.addView(tvPalabra);

        if (palabra.description != null && !palabra.description.isEmpty()) {
            TextView tvDesc = new TextView(getContext());
            tvDesc.setText(palabra.description);
            tvDesc.setTextSize(12);
            tvDesc.setTextColor(getResources().getColor(android.R.color.darker_gray));
            infoLayout.addView(tvDesc);
        }

        // Mostrar categoría
        if (palabra.categoryId > 0) {
            CategoryDAO.Category categoria = categoryDAO.obtenerCategoriaPorId(palabra.categoryId);
            if (categoria != null) {
                TextView tvCategoria = new TextView(getContext());
                tvCategoria.setText("📁 " + categoria.name);
                tvCategoria.setTextSize(11);
                tvCategoria.setTextColor(getResources().getColor(R.color.blue_menu));
                infoLayout.addView(tvCategoria);
            }
        }

        primeraFila.addView(infoLayout);

        // Botones
        LinearLayout botonesLayout = new LinearLayout(getContext());
        botonesLayout.setOrientation(LinearLayout.VERTICAL);

        MaterialButton btnEditar = new MaterialButton(getContext());
        btnEditar.setText("Editar");
        btnEditar.setTextSize(11);
        btnEditar.setPadding(8, 4, 8, 4);
        btnEditar.setOnClickListener(v -> mostrarDialogoEditarPalabra(palabra));
        botonesLayout.addView(btnEditar);

        MaterialButton btnTraducciones = new MaterialButton(getContext());
        btnTraducciones.setText("Traducciones");
        btnTraducciones.setTextSize(11);
        btnTraducciones.setPadding(8, 4, 8, 4);
        btnTraducciones.setBackgroundTintList(getResources().getColorStateList(R.color.blue_menu));
        btnTraducciones.setOnClickListener(v -> mostrarDialogoTraducciones(palabra));
        botonesLayout.addView(btnTraducciones);

        MaterialButton btnEliminar = new MaterialButton(getContext());
        btnEliminar.setText("Eliminar");
        btnEliminar.setTextSize(11);
        btnEliminar.setPadding(8, 4, 8, 4);
        btnEliminar.setBackgroundTintList(getResources().getColorStateList(android.R.color.holo_red_dark));
        btnEliminar.setOnClickListener(v -> confirmarEliminarPalabra(palabra));
        botonesLayout.addView(btnEliminar);

        primeraFila.addView(botonesLayout);
        tarjeta.addView(primeraFila);

        // Segunda fila: Traducciones
        Map<String, String> traducciones = wordLanguageDAO.obtenerTraduccionesComoMapa(palabra.wordId);
        if (!traducciones.isEmpty()) {
            TextView tvTraducciones = new TextView(getContext());
            StringBuilder textTrad = new StringBuilder("🌐 ");
            int count = 0;
            for (Map.Entry<String, String> entry : traducciones.entrySet()) {
                if (count > 0) textTrad.append(" | ");
                textTrad.append(entry.getKey()).append(": ").append(entry.getValue());
                count++;
            }
            tvTraducciones.setText(textTrad.toString());
            tvTraducciones.setTextSize(12);
            tvTraducciones.setTextColor(getResources().getColor(android.R.color.darker_gray));
            tvTraducciones.setPadding(0, 8, 0, 0);
            tarjeta.addView(tvTraducciones);
        }

        return tarjeta;
    }

    private void mostrarDialogoAgregarPalabra() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Agregar Palabra al Diccionario");

        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 20, 50, 20);

        // Campo: Palabra
        final EditText etPalabra = new EditText(getContext());
        etPalabra.setHint("Palabra o frase (ej: Hola, Buenos días)");
        layout.addView(etPalabra);

        // Campo: Descripción
        final EditText etDescripcion = new EditText(getContext());
        etDescripcion.setHint("Descripción (opcional)");
        layout.addView(etDescripcion);

        // Spinner: Categoría
        TextView tvCategoria = new TextView(getContext());
        tvCategoria.setText("Categoría:");
        tvCategoria.setPadding(0, 16, 0, 8);
        layout.addView(tvCategoria);

        final Spinner spinnerCategoria = new Spinner(getContext());
        List<CategoryDAO.Category> categorias = categoryDAO.obtenerTodasLasCategorias();
        List<String> nombresCategorias = new ArrayList<>();
        nombresCategorias.add("Sin categoría");
        for (CategoryDAO.Category cat : categorias) {
            nombresCategorias.add(cat.name);
        }
        ArrayAdapter<String> adapterCat = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_item, nombresCategorias);
        adapterCat.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategoria.setAdapter(adapterCat);
        layout.addView(spinnerCategoria);

        builder.setView(layout);

        builder.setPositiveButton("Siguiente: Agregar Traducciones", (dialog, which) -> {
            String palabra = etPalabra.getText().toString().trim();
            String descripcion = etDescripcion.getText().toString().trim();

            if (palabra.isEmpty()) {
                Toast.makeText(getContext(), "La palabra es requerida", Toast.LENGTH_SHORT).show();
                return;
            }

            if (wordDAO.existePalabra(palabra)) {
                Toast.makeText(getContext(), "La palabra ya existe", Toast.LENGTH_SHORT).show();
                return;
            }

            // Obtener categoría seleccionada
            int categoryId = 0;
            int posCat = spinnerCategoria.getSelectedItemPosition();
            if (posCat > 0 && posCat <= categorias.size()) {
                categoryId = categorias.get(posCat - 1).categoryId;
            }

            // Insertar palabra
            WordDAO.Word nuevaPalabra = categoryId > 0 ?
                    new WordDAO.Word(categoryId, palabra, descripcion) :
                    new WordDAO.Word(palabra, descripcion);

            long wordId = wordDAO.insertarPalabra(nuevaPalabra);

            if (wordId != -1) {
                Toast.makeText(getContext(), "Palabra agregada", Toast.LENGTH_SHORT).show();

                // Obtener la palabra recién creada
                nuevaPalabra.wordId = (int) wordId;

                // Mostrar diálogo para agregar traducciones
                mostrarDialogoAgregarTraducciones(nuevaPalabra);
            } else {
                Toast.makeText(getContext(), "Error al agregar palabra", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancelar", null);
        builder.show();
    }

    private void mostrarDialogoEditarPalabra(WordDAO.Word palabra) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Editar Palabra");

        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 20, 50, 20);

        final EditText etPalabra = new EditText(getContext());
        etPalabra.setText(palabra.word);
        layout.addView(etPalabra);

        final EditText etDescripcion = new EditText(getContext());
        etDescripcion.setText(palabra.description);
        layout.addView(etDescripcion);

        // Spinner: Categoría
        TextView tvCategoria = new TextView(getContext());
        tvCategoria.setText("Categoría:");
        tvCategoria.setPadding(0, 16, 0, 8);
        layout.addView(tvCategoria);

        final Spinner spinnerCategoria = new Spinner(getContext());
        List<CategoryDAO.Category> categorias = categoryDAO.obtenerTodasLasCategorias();
        List<String> nombresCategorias = new ArrayList<>();
        nombresCategorias.add("Sin categoría");
        int seleccionActual = 0;
        for (int i = 0; i < categorias.size(); i++) {
            nombresCategorias.add(categorias.get(i).name);
            if (categorias.get(i).categoryId == palabra.categoryId) {
                seleccionActual = i + 1;
            }
        }
        ArrayAdapter<String> adapterCat = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_item, nombresCategorias);
        adapterCat.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategoria.setAdapter(adapterCat);
        spinnerCategoria.setSelection(seleccionActual);
        layout.addView(spinnerCategoria);

        builder.setView(layout);

        builder.setPositiveButton("Guardar", (dialog, which) -> {
            palabra.word = etPalabra.getText().toString().trim();
            palabra.description = etDescripcion.getText().toString().trim();

            // Obtener categoría seleccionada
            int posCat = spinnerCategoria.getSelectedItemPosition();
            if (posCat > 0 && posCat <= categorias.size()) {
                palabra.categoryId = categorias.get(posCat - 1).categoryId;
            } else {
                palabra.categoryId = 0;
            }

            int resultado = wordDAO.actualizarPalabra(palabra);

            if (resultado > 0) {
                Toast.makeText(getContext(), "Palabra actualizada", Toast.LENGTH_SHORT).show();
                mostrarCatalogos();
            } else {
                Toast.makeText(getContext(), "Error al actualizar", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancelar", null);
        builder.show();
    }

    private void mostrarDialogoTraducciones(WordDAO.Word palabra) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Traducciones: " + palabra.word);

        // Crear layout scrollable
        LinearLayout mainLayout = new LinearLayout(getContext());
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setPadding(50, 20, 50, 20);

        // Obtener idiomas y traducciones existentes
        List<LanguageDAO.Language> idiomas = languageDAO.obtenerIdiomasActivos();
        List<WordLanguageDAO.WordLanguage> traduccionesExistentes =
                wordLanguageDAO.obtenerTraduccionesPorPalabra(palabra.wordId);

        // Crear mapa de traducciones existentes
        Map<Integer, WordLanguageDAO.WordLanguage> mapaTrads = new HashMap<>();
        for (WordLanguageDAO.WordLanguage trad : traduccionesExistentes) {
            mapaTrads.put(trad.languageId, trad);
        }

        // Crear campos para cada idioma
        Map<Integer, EditText> camposTraduccion = new HashMap<>();

        for (LanguageDAO.Language idioma : idiomas) {
            TextView tvIdioma = new TextView(getContext());
            tvIdioma.setText(idioma.name + ":");
            tvIdioma.setPadding(0, 12, 0, 4);
            mainLayout.addView(tvIdioma);

            EditText etTraduccion = new EditText(getContext());
            etTraduccion.setHint("Traducción en " + idioma.name);

            // Si existe traducción, cargarla
            if (mapaTrads.containsKey(idioma.languageId)) {
                etTraduccion.setText(mapaTrads.get(idioma.languageId).translation);
            }

            mainLayout.addView(etTraduccion);
            camposTraduccion.put(idioma.languageId, etTraduccion);
        }

        builder.setView(mainLayout);

        builder.setPositiveButton("Guardar", (dialog, which) -> {
            int actualizados = 0;
            int insertados = 0;

            for (Map.Entry<Integer, EditText> entry : camposTraduccion.entrySet()) {
                int languageId = entry.getKey();
                String traduccion = entry.getValue().getText().toString().trim();

                if (!traduccion.isEmpty()) {
                    if (mapaTrads.containsKey(languageId)) {
                        // Actualizar traducción existente
                        WordLanguageDAO.WordLanguage trad = mapaTrads.get(languageId);
                        trad.translation = traduccion;
                        int resultado = wordLanguageDAO.actualizarTraduccion(trad);
                        if (resultado > 0) actualizados++;
                    } else {
                        // Insertar nueva traducción
                        WordLanguageDAO.WordLanguage nuevaTrad =
                                new WordLanguageDAO.WordLanguage(palabra.wordId, languageId, traduccion);
                        long resultado = wordLanguageDAO.insertarTraduccion(nuevaTrad);
                        if (resultado != -1) insertados++;
                    }
                }
            }

            Toast.makeText(getContext(),
                    "Traducciones guardadas: " + insertados + " nuevas, " + actualizados + " actualizadas",
                    Toast.LENGTH_LONG).show();
            mostrarCatalogos();
        });

        builder.setNegativeButton("Cancelar", null);
        builder.show();
    }

    private void mostrarDialogoAgregarTraducciones(WordDAO.Word palabra) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Agregar Traducciones: " + palabra.word);

        LinearLayout mainLayout = new LinearLayout(getContext());
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setPadding(50, 20, 50, 20);

        TextView tvInstruccion = new TextView(getContext());
        tvInstruccion.setText("Agrega traducciones en los idiomas que desees:");
        tvInstruccion.setPadding(0, 0, 0, 16);
        mainLayout.addView(tvInstruccion);

        // Obtener idiomas activos
        List<LanguageDAO.Language> idiomas = languageDAO.obtenerIdiomasActivos();
        Map<Integer, EditText> camposTraduccion = new HashMap<>();

        for (LanguageDAO.Language idioma : idiomas) {
            TextView tvIdioma = new TextView(getContext());
            tvIdioma.setText(idioma.name + ":");
            tvIdioma.setPadding(0, 12, 0, 4);
            mainLayout.addView(tvIdioma);

            EditText etTraduccion = new EditText(getContext());
            etTraduccion.setHint("Traducción en " + idioma.name);
            mainLayout.addView(etTraduccion);
            camposTraduccion.put(idioma.languageId, etTraduccion);
        }

        builder.setView(mainLayout);

        builder.setPositiveButton("Guardar", (dialog, which) -> {
            int insertados = 0;

            for (Map.Entry<Integer, EditText> entry : camposTraduccion.entrySet()) {
                int languageId = entry.getKey();
                String traduccion = entry.getValue().getText().toString().trim();

                if (!traduccion.isEmpty()) {
                    WordLanguageDAO.WordLanguage nuevaTrad =
                            new WordLanguageDAO.WordLanguage(palabra.wordId, languageId, traduccion);
                    long resultado = wordLanguageDAO.insertarTraduccion(nuevaTrad);
                    if (resultado != -1) insertados++;
                }
            }

            if (insertados > 0) {
                Toast.makeText(getContext(),
                        insertados + " traducciones agregadas",
                        Toast.LENGTH_SHORT).show();
                mostrarCatalogos();
            } else {
                Toast.makeText(getContext(),
                        "No se agregaron traducciones. Puedes agregarlas después.",
                        Toast.LENGTH_SHORT).show();
                mostrarCatalogos();
            }
        });

        builder.setNegativeButton("Omitir", (dialog, which) -> {
            mostrarCatalogos();
        });

        builder.show();
    }

    private void confirmarEliminarPalabra(WordDAO.Word palabra) {
        new AlertDialog.Builder(getContext())
                .setTitle("Eliminar Palabra")
                .setMessage("¿Estás seguro de eliminar \"" + palabra.word + "\"?\n\n" +
                        "Esto también eliminará todas sus traducciones.")
                .setPositiveButton("Eliminar", (dialog, which) -> {
                    // Primero eliminar traducciones (por CASCADE debería ser automático)
                    wordLanguageDAO.eliminarTodasLasTraducciones(palabra.wordId);

                    // Luego eliminar palabra
                    int resultado = wordDAO.eliminarPalabra(palabra.wordId);

                    if (resultado > 0) {
                        Toast.makeText(getContext(), "Palabra eliminada", Toast.LENGTH_SHORT).show();
                        mostrarCatalogos();
                    } else {
                        Toast.makeText(getContext(), "Error al eliminar", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    // ==================== UTILIDADES ====================

    private void agregarSeparador() {
        View separador = new View(getContext());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                2
        );
        params.setMargins(0, 24, 0, 8);
        separador.setLayoutParams(params);
        separador.setBackgroundColor(getResources().getColor(android.R.color.darker_gray));
        contenedorCatalogos.addView(separador);
    }
}