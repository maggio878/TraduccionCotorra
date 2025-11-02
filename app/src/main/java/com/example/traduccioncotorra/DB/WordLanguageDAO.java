package com.example.traduccioncotorra.DB;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WordLanguageDAO {
    private ManagerDB managerDB;
    private static final String TABLE_NAME = "WordLanguage";

    public WordLanguageDAO(Context context) {
        managerDB = new ManagerDB(context);
    }

    /**
     * Clase interna para representar una traducción de palabra
     */
    public static class WordLanguage {
        public int wordLanguageId;
        public int wordId;
        public int languageId;
        public String translation;
        public String usageExample;
        public String updatedDate;

        public WordLanguage(int wordLanguageId, int wordId, int languageId,
                            String translation, String usageExample, String updatedDate) {
            this.wordLanguageId = wordLanguageId;
            this.wordId = wordId;
            this.languageId = languageId;
            this.translation = translation;
            this.usageExample = usageExample;
            this.updatedDate = updatedDate;
        }

        public WordLanguage(int wordId, int languageId, String translation, String usageExample) {
            this.wordId = wordId;
            this.languageId = languageId;
            this.translation = translation;
            this.usageExample = usageExample;
        }

        public WordLanguage(int wordId, int languageId, String translation) {
            this.wordId = wordId;
            this.languageId = languageId;
            this.translation = translation;
        }
    }

    /**
     * CREATE - Insertar una traducción
     */
    public long insertarTraduccion(WordLanguage wordLanguage) {
        long resultado = -1;
        try {
            managerDB.AbrirConexion();

            ContentValues valores = new ContentValues();
            valores.put("WordId", wordLanguage.wordId);
            valores.put("LanguageId", wordLanguage.languageId);
            valores.put("Translation", wordLanguage.translation);
            if (wordLanguage.usageExample != null && !wordLanguage.usageExample.isEmpty()) {
                valores.put("UsageExample", wordLanguage.usageExample);
            }

            resultado = managerDB.insertar(TABLE_NAME, valores);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            managerDB.CerrarConexion();
        }
        return resultado;
    }

    /**
     * READ - Obtener todas las traducciones de una palabra
     */
    public List<WordLanguage> obtenerTraduccionesPorPalabra(int wordId) {
        List<WordLanguage> traducciones = new ArrayList<>();
        Cursor cursor = null;

        try {
            managerDB.AbrirConexion();

            String query = "SELECT * FROM " + TABLE_NAME + " WHERE WordId = ?";
            cursor = managerDB.consultar(query, new String[]{String.valueOf(wordId)});

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    int usageExampleIndex = cursor.getColumnIndex("UsageExample");
                    String usageExample = usageExampleIndex != -1 && !cursor.isNull(usageExampleIndex)
                            ? cursor.getString(usageExampleIndex) : "";

                    WordLanguage traduccion = new WordLanguage(
                            cursor.getInt(cursor.getColumnIndexOrThrow("WordLanguageId")),
                            cursor.getInt(cursor.getColumnIndexOrThrow("WordId")),
                            cursor.getInt(cursor.getColumnIndexOrThrow("LanguageId")),
                            cursor.getString(cursor.getColumnIndexOrThrow("Translation")),
                            usageExample,
                            cursor.getString(cursor.getColumnIndexOrThrow("UpdatedDate"))
                    );
                    traducciones.add(traduccion);
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

        return traducciones;
    }

    /**
     * READ - Obtener traducción específica
     */
    public WordLanguage obtenerTraduccion(int wordId, int languageId) {
        WordLanguage traduccion = null;
        Cursor cursor = null;

        try {
            managerDB.AbrirConexion();

            String query = "SELECT * FROM " + TABLE_NAME +
                    " WHERE WordId = ? AND LanguageId = ?";
            cursor = managerDB.consultar(query,
                    new String[]{String.valueOf(wordId), String.valueOf(languageId)});

            if (cursor != null && cursor.moveToFirst()) {
                int usageExampleIndex = cursor.getColumnIndex("UsageExample");
                String usageExample = usageExampleIndex != -1 && !cursor.isNull(usageExampleIndex)
                        ? cursor.getString(usageExampleIndex) : "";

                traduccion = new WordLanguage(
                        cursor.getInt(cursor.getColumnIndexOrThrow("WordLanguageId")),
                        cursor.getInt(cursor.getColumnIndexOrThrow("WordId")),
                        cursor.getInt(cursor.getColumnIndexOrThrow("LanguageId")),
                        cursor.getString(cursor.getColumnIndexOrThrow("Translation")),
                        usageExample,
                        cursor.getString(cursor.getColumnIndexOrThrow("UpdatedDate"))
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

        return traduccion;
    }

    /**
     * READ - Obtener todas las traducciones de una palabra como Map
     * Key: nombre del idioma, Value: traducción
     */
    public Map<String, String> obtenerTraduccionesComoMapa(int wordId) {
        Map<String, String> traducciones = new HashMap<>();
        Cursor cursor = null;

        try {
            managerDB.AbrirConexion();

            String query = "SELECT wl.Translation, l.Name " +
                    "FROM " + TABLE_NAME + " wl " +
                    "INNER JOIN Language l ON wl.LanguageId = l.LanguageId " +
                    "WHERE wl.WordId = ?";
            cursor = managerDB.consultar(query, new String[]{String.valueOf(wordId)});

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    String languageName = cursor.getString(cursor.getColumnIndexOrThrow("Name"));
                    String translation = cursor.getString(cursor.getColumnIndexOrThrow("Translation"));
                    traducciones.put(languageName, translation);
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

        return traducciones;
    }

    /**
     * READ - Buscar palabras por traducción en cualquier idioma
     */
    public List<Integer> buscarPalabrasPorTraduccion(String busqueda) {
        List<Integer> wordIds = new ArrayList<>();
        Cursor cursor = null;

        try {
            managerDB.AbrirConexion();

            String query = "SELECT DISTINCT WordId FROM " + TABLE_NAME +
                    " WHERE Translation LIKE ?";
            String patron = "%" + busqueda + "%";
            cursor = managerDB.consultar(query, new String[]{patron});

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    wordIds.add(cursor.getInt(cursor.getColumnIndexOrThrow("WordId")));
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

        return wordIds;
    }

    /**
     * READ - Obtener todas las palabras con sus traducciones (para diccionario completo)
     */
    public Map<Integer, Map<String, String>> obtenerDiccionarioCompleto() {
        Map<Integer, Map<String, String>> diccionario = new HashMap<>();
        Cursor cursor = null;

        try {
            managerDB.AbrirConexion();

            String query = "SELECT w.WordId, w.Word, l.Name as LanguageName, wl.Translation " +
                    "FROM Word w " +
                    "INNER JOIN WordLanguage wl ON w.WordId = wl.WordId " +
                    "INNER JOIN Language l ON wl.LanguageId = l.LanguageId " +
                    "WHERE l.IsActive = 1 " +
                    "ORDER BY w.Word, l.Name";
            cursor = managerDB.consultar(query, null);

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    int wordId = cursor.getInt(cursor.getColumnIndexOrThrow("WordId"));
                    String languageName = cursor.getString(cursor.getColumnIndexOrThrow("LanguageName"));
                    String translation = cursor.getString(cursor.getColumnIndexOrThrow("Translation"));

                    if (!diccionario.containsKey(wordId)) {
                        diccionario.put(wordId, new HashMap<>());
                    }
                    diccionario.get(wordId).put(languageName, translation);

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

        return diccionario;
    }

    /**
     * UPDATE - Actualizar una traducción
     */
    public int actualizarTraduccion(WordLanguage wordLanguage) {
        int filasActualizadas = 0;

        try {
            managerDB.AbrirConexion();

            ContentValues valores = new ContentValues();
            valores.put("Translation", wordLanguage.translation);
            if (wordLanguage.usageExample != null && !wordLanguage.usageExample.isEmpty()) {
                valores.put("UsageExample", wordLanguage.usageExample);
            } else {
                valores.putNull("UsageExample");
            }

            filasActualizadas = managerDB.actualizar(
                    TABLE_NAME,
                    valores,
                    "WordId = ? AND LanguageId = ?",
                    new String[]{
                            String.valueOf(wordLanguage.wordId),
                            String.valueOf(wordLanguage.languageId)
                    }
            );

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            managerDB.CerrarConexion();
        }

        return filasActualizadas;
    }

    /**
     * DELETE - Eliminar una traducción específica
     */
    public int eliminarTraduccion(int wordId, int languageId) {
        int filasEliminadas = 0;

        try {
            managerDB.AbrirConexion();

            filasEliminadas = managerDB.eliminar(
                    TABLE_NAME,
                    "WordId = ? AND LanguageId = ?",
                    new String[]{String.valueOf(wordId), String.valueOf(languageId)}
            );

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            managerDB.CerrarConexion();
        }

        return filasEliminadas;
    }

    /**
     * DELETE - Eliminar todas las traducciones de una palabra
     */
    public int eliminarTodasLasTraducciones(int wordId) {
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
     * Verificar si existe una traducción
     */
    public boolean existeTraduccion(int wordId, int languageId) {
        boolean existe = false;
        Cursor cursor = null;

        try {
            managerDB.AbrirConexion();

            String query = "SELECT COUNT(*) FROM " + TABLE_NAME +
                    " WHERE WordId = ? AND LanguageId = ?";
            cursor = managerDB.consultar(query,
                    new String[]{String.valueOf(wordId), String.valueOf(languageId)});

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
     * Contar traducciones de una palabra
     */
    public int contarTraducciones(int wordId) {
        int count = 0;
        Cursor cursor = null;

        try {
            managerDB.AbrirConexion();

            String query = "SELECT COUNT(*) FROM " + TABLE_NAME + " WHERE WordId = ?";
            cursor = managerDB.consultar(query, new String[]{String.valueOf(wordId)});

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