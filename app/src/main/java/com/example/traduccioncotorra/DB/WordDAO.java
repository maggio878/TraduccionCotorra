package com.example.traduccioncotorra.DB;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import java.util.ArrayList;
import java.util.List;

public class WordDAO {
    private ManagerDB managerDB;
    private static final String TABLE_NAME = "Word";

    public WordDAO(Context context) {
        managerDB = new ManagerDB(context);
    }

    /**
     * Clase interna para representar una palabra
     */
    public static class Word {
        public int wordId;
        public int categoryId;
        public String word;
        public String description;
        public int usageFrequency;

        public Word(int wordId, int categoryId, String word, String description, int usageFrequency) {
            this.wordId = wordId;
            this.categoryId = categoryId;
            this.word = word;
            this.description = description;
            this.usageFrequency = usageFrequency;
        }

        public Word(int categoryId, String word, String description) {
            this.categoryId = categoryId;
            this.word = word;
            this.description = description;
            this.usageFrequency = 0;
        }

        public Word(String word, String description) {
            this.word = word;
            this.description = description;
            this.usageFrequency = 0;
        }
    }

    /**
     * CREATE - Insertar una nueva palabra
     */
    public long insertarPalabra(Word word) {
        long resultado = -1;
        try {
            managerDB.AbrirConexion();

            ContentValues valores = new ContentValues();
            if (word.categoryId > 0) {
                valores.put("CategoryId", word.categoryId);
            }
            valores.put("Word", word.word);
            valores.put("Description", word.description);
            valores.put("UsageFrequency", word.usageFrequency);

            resultado = managerDB.insertar(TABLE_NAME, valores);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            managerDB.CerrarConexion();
        }
        return resultado;
    }

    /**
     * READ - Obtener todas las palabras
     */
    public List<Word> obtenerTodasLasPalabras() {
        List<Word> palabras = new ArrayList<>();
        Cursor cursor = null;

        try {
            managerDB.AbrirConexion();

            String query = "SELECT * FROM " + TABLE_NAME + " ORDER BY Word ASC";
            cursor = managerDB.consultar(query, null);

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    int categoryIdIndex = cursor.getColumnIndex("CategoryId");
                    int categoryId = categoryIdIndex != -1 && !cursor.isNull(categoryIdIndex)
                            ? cursor.getInt(categoryIdIndex) : 0;

                    Word palabra = new Word(
                            cursor.getInt(cursor.getColumnIndexOrThrow("WordId")),
                            categoryId,
                            cursor.getString(cursor.getColumnIndexOrThrow("Word")),
                            cursor.getString(cursor.getColumnIndexOrThrow("Description")),
                            cursor.getInt(cursor.getColumnIndexOrThrow("UsageFrequency"))
                    );
                    palabras.add(palabra);
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

        return palabras;
    }

    /**
     * READ - Obtener palabra por ID
     */
    public Word obtenerPalabraPorId(int wordId) {
        Word palabra = null;
        Cursor cursor = null;

        try {
            managerDB.AbrirConexion();

            String query = "SELECT * FROM " + TABLE_NAME + " WHERE WordId = ?";
            cursor = managerDB.consultar(query, new String[]{String.valueOf(wordId)});

            if (cursor != null && cursor.moveToFirst()) {
                int categoryIdIndex = cursor.getColumnIndex("CategoryId");
                int categoryId = categoryIdIndex != -1 && !cursor.isNull(categoryIdIndex)
                        ? cursor.getInt(categoryIdIndex) : 0;

                palabra = new Word(
                        cursor.getInt(cursor.getColumnIndexOrThrow("WordId")),
                        categoryId,
                        cursor.getString(cursor.getColumnIndexOrThrow("Word")),
                        cursor.getString(cursor.getColumnIndexOrThrow("Description")),
                        cursor.getInt(cursor.getColumnIndexOrThrow("UsageFrequency"))
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

        return palabra;
    }

    /**
     * READ - Obtener palabras por categoría
     */
    public List<Word> obtenerPalabrasPorCategoria(int categoryId) {
        List<Word> palabras = new ArrayList<>();
        Cursor cursor = null;

        try {
            managerDB.AbrirConexion();

            String query = "SELECT * FROM " + TABLE_NAME + " WHERE CategoryId = ? ORDER BY Word ASC";
            cursor = managerDB.consultar(query, new String[]{String.valueOf(categoryId)});

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    Word palabra = new Word(
                            cursor.getInt(cursor.getColumnIndexOrThrow("WordId")),
                            cursor.getInt(cursor.getColumnIndexOrThrow("CategoryId")),
                            cursor.getString(cursor.getColumnIndexOrThrow("Word")),
                            cursor.getString(cursor.getColumnIndexOrThrow("Description")),
                            cursor.getInt(cursor.getColumnIndexOrThrow("UsageFrequency"))
                    );
                    palabras.add(palabra);
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

        return palabras;
    }

    /**
     * READ - Buscar palabras por texto
     */
    public List<Word> buscarPalabras(String busqueda) {
        List<Word> palabras = new ArrayList<>();
        Cursor cursor = null;

        try {
            managerDB.AbrirConexion();

            String query = "SELECT * FROM " + TABLE_NAME +
                    " WHERE Word LIKE ? OR Description LIKE ? ORDER BY Word ASC";
            String patron = "%" + busqueda + "%";
            cursor = managerDB.consultar(query, new String[]{patron, patron});

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    int categoryIdIndex = cursor.getColumnIndex("CategoryId");
                    int categoryId = categoryIdIndex != -1 && !cursor.isNull(categoryIdIndex)
                            ? cursor.getInt(categoryIdIndex) : 0;

                    Word palabra = new Word(
                            cursor.getInt(cursor.getColumnIndexOrThrow("WordId")),
                            categoryId,
                            cursor.getString(cursor.getColumnIndexOrThrow("Word")),
                            cursor.getString(cursor.getColumnIndexOrThrow("Description")),
                            cursor.getInt(cursor.getColumnIndexOrThrow("UsageFrequency"))
                    );
                    palabras.add(palabra);
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

        return palabras;
    }

    /**
     * UPDATE - Actualizar una palabra
     */
    public int actualizarPalabra(Word word) {
        int filasActualizadas = 0;

        try {
            managerDB.AbrirConexion();

            ContentValues valores = new ContentValues();
            if (word.categoryId > 0) {
                valores.put("CategoryId", word.categoryId);
            } else {
                valores.putNull("CategoryId");
            }
            valores.put("Word", word.word);
            valores.put("Description", word.description);
            valores.put("UsageFrequency", word.usageFrequency);

            filasActualizadas = managerDB.actualizar(
                    TABLE_NAME,
                    valores,
                    "WordId = ?",
                    new String[]{String.valueOf(word.wordId)}
            );

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            managerDB.CerrarConexion();
        }

        return filasActualizadas;
    }

    /**
     * UPDATE - Incrementar frecuencia de uso
     */
    public int incrementarFrecuencia(int wordId) {
        int filasActualizadas = 0;

        try {
            managerDB.AbrirConexion();

            String query = "UPDATE " + TABLE_NAME +
                    " SET UsageFrequency = UsageFrequency + 1 WHERE WordId = ?";
            managerDB.getBaseDeDatos().execSQL(query, new Object[]{wordId});
            filasActualizadas = 1;

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            managerDB.CerrarConexion();
        }

        return filasActualizadas;
    }

    /**
     * DELETE - Eliminar palabra
     */
    public int eliminarPalabra(int wordId) {
        int filasEliminadas = 0;

        try {
            managerDB.AbrirConexion();

            filasEliminadas = managerDB.eliminar(
                    TABLE_NAME,
                    "WordId = ?",
                    new String[]{String.valueOf(wordId)}
            );

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            managerDB.CerrarConexion();
        }

        return filasEliminadas;
    }

    /**
     * Verificar si una palabra ya existe
     */
    public boolean existePalabra(String word) {
        boolean existe = false;
        Cursor cursor = null;

        try {
            managerDB.AbrirConexion();

            String query = "SELECT COUNT(*) FROM " + TABLE_NAME + " WHERE Word = ?";
            cursor = managerDB.consultar(query, new String[]{word});

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
     * Contar palabras totales
     */
    public int contarPalabras() {
        int count = 0;
        Cursor cursor = null;

        try {
            managerDB.AbrirConexion();

            String query = "SELECT COUNT(*) FROM " + TABLE_NAME;
            cursor = managerDB.consultar(query, null);

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

    /**
     * Obtener palabras más usadas
     */
    public List<Word> obtenerPalabrasMasUsadas(int limite) {
        List<Word> palabras = new ArrayList<>();
        Cursor cursor = null;

        try {
            managerDB.AbrirConexion();

            String query = "SELECT * FROM " + TABLE_NAME +
                    " ORDER BY UsageFrequency DESC LIMIT ?";
            cursor = managerDB.consultar(query, new String[]{String.valueOf(limite)});

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    int categoryIdIndex = cursor.getColumnIndex("CategoryId");
                    int categoryId = categoryIdIndex != -1 && !cursor.isNull(categoryIdIndex)
                            ? cursor.getInt(categoryIdIndex) : 0;

                    Word palabra = new Word(
                            cursor.getInt(cursor.getColumnIndexOrThrow("WordId")),
                            categoryId,
                            cursor.getString(cursor.getColumnIndexOrThrow("Word")),
                            cursor.getString(cursor.getColumnIndexOrThrow("Description")),
                            cursor.getInt(cursor.getColumnIndexOrThrow("UsageFrequency"))
                    );
                    palabras.add(palabra);
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

        return palabras;
    }
}