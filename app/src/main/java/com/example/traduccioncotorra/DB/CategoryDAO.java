package com.example.traduccioncotorra.DB;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import java.util.ArrayList;
import java.util.List;

public class CategoryDAO {
    private ManagerDB managerDB;
    private static final String TABLE_NAME = "Category";

    public CategoryDAO(Context context) {
        managerDB = new ManagerDB(context);
    }

    /**
     * Clase interna para representar una categoría
     */
    public static class Category {
        public int categoryId;
        public String name;
        public String description;

        public Category(int categoryId, String name, String description) {
            this.categoryId = categoryId;
            this.name = name;
            this.description = description;
        }

        public Category(String name, String description) {
            this.name = name;
            this.description = description;
        }
    }

    /**
     * CREATE - Insertar una nueva categoría
     */
    public long insertarCategoria(Category category) {
        long resultado = -1;
        try {
            managerDB.AbrirConexion();

            ContentValues valores = new ContentValues();
            valores.put("Name", category.name);
            valores.put("Description", category.description);

            resultado = managerDB.insertar(TABLE_NAME, valores);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            managerDB.CerrarConexion();
        }
        return resultado;
    }

    /**
     * READ - Obtener todas las categorías
     */
    public List<Category> obtenerTodasLasCategorias() {
        List<Category> categorias = new ArrayList<>();
        Cursor cursor = null;

        try {
            managerDB.AbrirConexion();

            String query = "SELECT * FROM " + TABLE_NAME + " ORDER BY Name ASC";
            cursor = managerDB.consultar(query, null);

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    Category categoria = new Category(
                            cursor.getInt(cursor.getColumnIndexOrThrow("CategoryId")),
                            cursor.getString(cursor.getColumnIndexOrThrow("Name")),
                            cursor.getString(cursor.getColumnIndexOrThrow("Description"))
                    );
                    categorias.add(categoria);
                } while (cursor.moveToNext());
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            managerDB.CerrarConexion();
        }

        return categorias;
    }

    /**
     * READ - Obtener categoría por ID
     */
    public Category obtenerCategoriaPorId(int categoryId) {
        Category categoria = null;
        Cursor cursor = null;

        try {
            managerDB.AbrirConexion();

            String query = "SELECT * FROM " + TABLE_NAME + " WHERE CategoryId = ?";
            cursor = managerDB.consultar(query, new String[]{String.valueOf(categoryId)});

            if (cursor != null && cursor.moveToFirst()) {
                categoria = new Category(
                        cursor.getInt(cursor.getColumnIndexOrThrow("CategoryId")),
                        cursor.getString(cursor.getColumnIndexOrThrow("Name")),
                        cursor.getString(cursor.getColumnIndexOrThrow("Description"))
                );
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            managerDB.CerrarConexion();
        }

        return categoria;
    }

    /**
     * READ - Obtener categoría por nombre
     */
    public Category obtenerCategoriaPorNombre(String nombre) {
        Category categoria = null;
        Cursor cursor = null;

        try {
            managerDB.AbrirConexion();

            String query = "SELECT * FROM " + TABLE_NAME + " WHERE Name = ?";
            cursor = managerDB.consultar(query, new String[]{nombre});

            if (cursor != null && cursor.moveToFirst()) {
                categoria = new Category(
                        cursor.getInt(cursor.getColumnIndexOrThrow("CategoryId")),
                        cursor.getString(cursor.getColumnIndexOrThrow("Name")),
                        cursor.getString(cursor.getColumnIndexOrThrow("Description"))
                );
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            managerDB.CerrarConexion();
        }

        return categoria;
    }

    /**
     * READ - Buscar categorías por nombre
     */
    public List<Category> buscarCategoriasPorNombre(String nombre) {
        List<Category> categorias = new ArrayList<>();
        Cursor cursor = null;

        try {
            managerDB.AbrirConexion();

            String query = "SELECT * FROM " + TABLE_NAME + " WHERE Name LIKE ? ORDER BY Name ASC";
            cursor = managerDB.consultar(query, new String[]{"%" + nombre + "%"});

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    Category categoria = new Category(
                            cursor.getInt(cursor.getColumnIndexOrThrow("CategoryId")),
                            cursor.getString(cursor.getColumnIndexOrThrow("Name")),
                            cursor.getString(cursor.getColumnIndexOrThrow("Description"))
                    );
                    categorias.add(categoria);
                } while (cursor.moveToNext());
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            managerDB.CerrarConexion();
        }

        return categorias;
    }

    /**
     * UPDATE - Actualizar una categoría
     */
    public int actualizarCategoria(Category category) {
        int filasActualizadas = 0;

        try {
            managerDB.AbrirConexion();

            ContentValues valores = new ContentValues();
            valores.put("Name", category.name);
            valores.put("Description", category.description);

            filasActualizadas = managerDB.actualizar(
                    TABLE_NAME,
                    valores,
                    "CategoryId = ?",
                    new String[]{String.valueOf(category.categoryId)}
            );

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            managerDB.CerrarConexion();
        }

        return filasActualizadas;
    }

    /**
     * DELETE - Eliminar categoría
     */
    public int eliminarCategoria(int categoryId) {
        int filasEliminadas = 0;

        try {
            managerDB.AbrirConexion();

            filasEliminadas = managerDB.eliminar(
                    TABLE_NAME,
                    "CategoryId = ?",
                    new String[]{String.valueOf(categoryId)}
            );

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            managerDB.CerrarConexion();
        }

        return filasEliminadas;
    }

    /**
     * Verificar si un nombre de categoría ya existe
     */
    public boolean existeNombreCategoria(String nombre) {
        boolean existe = false;
        Cursor cursor = null;

        try {
            managerDB.AbrirConexion();

            String query = "SELECT COUNT(*) FROM " + TABLE_NAME + " WHERE Name = ?";
            cursor = managerDB.consultar(query, new String[]{nombre});

            if (cursor != null && cursor.moveToFirst()) {
                existe = cursor.getInt(0) > 0;
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            managerDB.CerrarConexion();
        }

        return existe;
    }

    /**
     * Obtener array de nombres de categorías (para spinners)
     */
    public String[] obtenerNombresCategorias() {
        List<Category> categorias = obtenerTodasLasCategorias();
        String[] nombres = new String[categorias.size()];

        for (int i = 0; i < categorias.size(); i++) {
            nombres[i] = categorias.get(i).name;
        }

        return nombres;
    }

    /**
     * Obtener ID de categoría por nombre
     */
    public int obtenerIdCategoriaPorNombre(String nombre) {
        int id = -1;
        Category categoria = obtenerCategoriaPorNombre(nombre);
        if (categoria != null) {
            id = categoria.categoryId;
        }
        return id;
    }

    /**
     * Contar palabras por categoría
     */
    public int contarPalabrasPorCategoria(int categoryId) {
        int count = 0;
        Cursor cursor = null;

        try {
            managerDB.AbrirConexion();

            String query = "SELECT COUNT(*) FROM Word WHERE CategoryId = ?";
            cursor = managerDB.consultar(query, new String[]{String.valueOf(categoryId)});

            if (cursor != null && cursor.moveToFirst()) {
                count = cursor.getInt(0);
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            managerDB.CerrarConexion();
        }

        return count;
    }
}