package com.example.traduccioncotorra;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.traduccioncotorra.DB.CategoryDAO;
import com.example.traduccioncotorra.DB.LanguageDAO;
import com.example.traduccioncotorra.DB.TranslationTypeDAO;
import com.google.android.material.button.MaterialButton;
import java.util.List;

public class AdminCatalogos extends Fragment {

    private LinearLayout contenedorCatalogos;
    private LanguageDAO languageDAO;
    private TranslationTypeDAO translationTypeDAO;
    private CategoryDAO categoryDAO;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin_catalogos, container, false);

        // Inicializar DAOs
        languageDAO = new LanguageDAO(requireContext());
        translationTypeDAO = new TranslationTypeDAO(requireContext());
        categoryDAO = new CategoryDAO(requireContext());

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

        TextView tvNombre = new TextView(getContext());
        tvNombre.setText(idioma.name + " (" + idioma.code + ")");
        tvNombre.setTextSize(16);
        tvNombre.setTextColor(getResources().getColor(android.R.color.black));
        infoLayout.addView(tvNombre);

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

    private void mostrarDialogoAgregarIdioma() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Agregar Idioma");

        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 20, 50, 20);

        final EditText etNombre = new EditText(getContext());
        etNombre.setHint("Nombre del idioma (ej: Español)");
        layout.addView(etNombre);

        final EditText etCodigo = new EditText(getContext());
        etCodigo.setHint("Código (ej: es, en, fr)");
        etCodigo.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        layout.addView(etCodigo);

        builder.setView(layout);

        builder.setPositiveButton("Agregar", (dialog, which) -> {
            String nombre = etNombre.getText().toString().trim();
            String codigo = etCodigo.getText().toString().trim();

            if (nombre.isEmpty() || codigo.isEmpty()) {
                Toast.makeText(getContext(), "Todos los campos son requeridos", Toast.LENGTH_SHORT).show();
                return;
            }

            if (languageDAO.existeCodigoIdioma(codigo)) {
                Toast.makeText(getContext(), "El código ya existe", Toast.LENGTH_SHORT).show();
                return;
            }

            LanguageDAO.Language nuevoIdioma = new LanguageDAO.Language(nombre, codigo);
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

    private void mostrarDialogoEditarIdioma(LanguageDAO.Language idioma) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Editar Idioma");

        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 20, 50, 20);

        final EditText etNombre = new EditText(getContext());
        etNombre.setHint("Nombre del idioma");
        etNombre.setText(idioma.name);
        layout.addView(etNombre);

        final EditText etCodigo = new EditText(getContext());
        etCodigo.setHint("Código");
        etCodigo.setText(idioma.code);
        layout.addView(etCodigo);

        builder.setView(layout);

        builder.setPositiveButton("Guardar", (dialog, which) -> {
            idioma.name = etNombre.getText().toString().trim();
            idioma.code = etCodigo.getText().toString().trim();

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