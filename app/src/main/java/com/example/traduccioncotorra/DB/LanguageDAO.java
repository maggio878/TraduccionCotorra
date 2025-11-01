package com.example.traduccioncotorra.DB;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import java.util.ArrayList;
import java.util.List;

public class LanguageDAO {
    private ManagerDB managerDB;
    private static final String TABLE_NAME = "Language";

    public LanguageDAO(Context context) {
        managerDB = new ManagerDB(context);
    }

    /**
     * Clase interna para representar un idioma
     */
    public static class Language {
        public int languageId;
        public String name;
        public String code;
        public int isActive;

        public Language(int languageId, String name, String code, int isActive) {
            this.languageId = languageId;
            this.name = name;
            this.code = code;
            this.isActive = isActive;
        }

        public Language(String name, String code) {
            this.name = name;
            this.code = code;
            this.isActive = 1;
        }
    }

    /**
     * CREATE - Insertar un nuevo idioma
     */
    public long insertarIdioma(Language language) {
        long resultado = -1;
        try {
            managerDB.AbrirConexion();

            ContentValues valores = new ContentValues();
            valores.put("Name", language.name);
            valores.put("Code", language.code);
            valores.put("IsActive", language.isActive);

            resultado = managerDB.insertar(TABLE_NAME, valores);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            managerDB.CerrarConexion();
        }
        return resultado;
    }

    /**
     * READ - Obtener todos los idiomas
     */
    public List<Language> obtenerTodosLosIdiomas() {
        List<Language> idiomas = new ArrayList<>();
        Cursor cursor = null;

        try {
            managerDB.AbrirConexion();

            String query = "SELECT * FROM " + TABLE_NAME + " ORDER BY Name ASC";
            cursor = managerDB.consultar(query, null);

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    Language idioma = new Language(
                            cursor.getInt(cursor.getColumnIndexOrThrow("LanguageId")),
                            cursor.getString(cursor.getColumnIndexOrThrow("Name")),
                            cursor.getString(cursor.getColumnIndexOrThrow("Code")),
                            cursor.getInt(cursor.getColumnIndexOrThrow("IsActive"))
                    );
                    idiomas.add(idioma);
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

        return idiomas;
    }

    /**
     * READ - Obtener solo idiomas activos
     */
    public List<Language> obtenerIdiomasActivos() {
        List<Language> idiomas = new ArrayList<>();
        Cursor cursor = null;

        try {
            managerDB.AbrirConexion();

            String query = "SELECT * FROM " + TABLE_NAME + " WHERE IsActive = 1 ORDER BY Name ASC";
            cursor = managerDB.consultar(query, null);

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    Language idioma = new Language(
                            cursor.getInt(cursor.getColumnIndexOrThrow("LanguageId")),
                            cursor.getString(cursor.getColumnIndexOrThrow("Name")),
                            cursor.getString(cursor.getColumnIndexOrThrow("Code")),
                            cursor.getInt(cursor.getColumnIndexOrThrow("IsActive"))
                    );
                    idiomas.add(idioma);
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

        return idiomas;
    }

    /**
     * READ - Obtener idioma por ID
     */
    public Language obtenerIdiomaPorId(int languageId) {
        Language idioma = null;
        Cursor cursor = null;

        try {
            managerDB.AbrirConexion();

            String query = "SELECT * FROM " + TABLE_NAME + " WHERE Language_Id = ?";
            cursor = managerDB.consultar(query, new String[]{String.valueOf(languageId)});

            if (cursor != null && cursor.moveToFirst()) {
                idioma = new Language(
                        cursor.getInt(cursor.getColumnIndexOrThrow("Language_Id")),
                        cursor.getString(cursor.getColumnIndexOrThrow("Name")),
                        cursor.getString(cursor.getColumnIndexOrThrow("Code")),
                        cursor.getInt(cursor.getColumnIndexOrThrow("IsActive"))
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

        return idioma;
    }

    /**
     * READ - Buscar idiomas por nombre
     */
    public List<Language> buscarIdiomasPorNombre(String nombre) {
        List<Language> idiomas = new ArrayList<>();
        Cursor cursor = null;

        try {
            managerDB.AbrirConexion();

            String query = "SELECT * FROM " + TABLE_NAME + " WHERE Name LIKE ? ORDER BY Name ASC";
            cursor = managerDB.consultar(query, new String[]{"%" + nombre + "%"});

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    Language idioma = new Language(
                            cursor.getInt(cursor.getColumnIndexOrThrow("Language_Id")),
                            cursor.getString(cursor.getColumnIndexOrThrow("Name")),
                            cursor.getString(cursor.getColumnIndexOrThrow("Code")),
                            cursor.getInt(cursor.getColumnIndexOrThrow("IsActive"))
                    );
                    idiomas.add(idioma);
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

        return idiomas;
    }

    /**
     * UPDATE - Actualizar un idioma
     */
    public int actualizarIdioma(Language language) {
        int filasActualizadas = 0;

        try {
            managerDB.AbrirConexion();

            ContentValues valores = new ContentValues();
            valores.put("Name", language.name);
            valores.put("Code", language.code);
            valores.put("IsActive", language.isActive);

            filasActualizadas = managerDB.actualizar(
                    TABLE_NAME,
                    valores,
                    "Language_Id = ?",
                    new String[]{String.valueOf(language.languageId)}
            );

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            managerDB.CerrarConexion();
        }

        return filasActualizadas;
    }

    /**
     * UPDATE - Activar/Desactivar idioma
     */
    public int cambiarEstadoIdioma(int languageId, boolean activar) {
        int filasActualizadas = 0;

        try {
            managerDB.AbrirConexion();

            ContentValues valores = new ContentValues();
            valores.put("IsActive", activar ? 1 : 0);

            filasActualizadas = managerDB.actualizar(
                    TABLE_NAME,
                    valores,
                    "Language_Id = ?",
                    new String[]{String.valueOf(languageId)}
            );

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            managerDB.CerrarConexion();
        }

        return filasActualizadas;
    }

    /**
     * DELETE - Eliminar idioma permanentemente
     */
    public int eliminarIdioma(int languageId) {
        int filasEliminadas = 0;

        try {
            managerDB.AbrirConexion();

            filasEliminadas = managerDB.eliminar(
                    TABLE_NAME,
                    "Language_Id = ?",
                    new String[]{String.valueOf(languageId)}
            );

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            managerDB.CerrarConexion();
        }

        return filasEliminadas;
    }

    /**
     * Verificar si un código de idioma ya existe
     */
    public boolean existeCodigoIdioma(String code) {
        boolean existe = false;
        Cursor cursor = null;

        try {
            managerDB.AbrirConexion();

            String query = "SELECT COUNT(*) FROM " + TABLE_NAME + " WHERE Code = ?";
            cursor = managerDB.consultar(query, new String[]{code});

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
     * Obtener array de nombres de idiomas activos (para spinners)
     */
    public String[] obtenerNombresIdiomasActivos() {
        List<Language> idiomas = obtenerIdiomasActivos();
        String[] nombres = new String[idiomas.size()];

        for (int i = 0; i < idiomas.size(); i++) {
            nombres[i] = idiomas.get(i).name;
        }

        return nombres;
    }

    /**
     * Obtener ID de idioma por nombre
     */
    public int obtenerIdIdiomaPorNombre(String nombre) {
        int id = -1;
        Cursor cursor = null;

        try {
            managerDB.AbrirConexion();

            String query = "SELECT Language_Id FROM " + TABLE_NAME + " WHERE Name = ?";
            cursor = managerDB.consultar(query, new String[]{nombre});

            if (cursor != null && cursor.moveToFirst()) {
                id = cursor.getInt(0);
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            managerDB.CerrarConexion();
        }

        return id;
    }
}