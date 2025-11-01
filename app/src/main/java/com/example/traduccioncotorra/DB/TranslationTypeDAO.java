package com.example.traduccioncotorra.DB;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import java.util.ArrayList;
import java.util.List;

public class TranslationTypeDAO {
    private ManagerDB managerDB;
    private static final String TABLE_NAME = "TranslationType";

    public TranslationTypeDAO(Context context) {
        managerDB = new ManagerDB(context);
    }

    /**
     * Clase interna para representar un tipo de traducción
     */
    public static class TranslationType {
        public int idTypeTranslation;
        public String name;

        public TranslationType(int idTypeTranslation, String name) {
            this.idTypeTranslation = idTypeTranslation;
            this.name = name;
        }

        public TranslationType(String name) {
            this.name = name;
        }
    }

    /**
     * CREATE - Insertar un nuevo tipo de traducción
     */
    public long insertarTipoTraduccion(TranslationType type) {
        long resultado = -1;
        try {
            managerDB.AbrirConexion();

            ContentValues valores = new ContentValues();
            valores.put("Name", type.name);

            resultado = managerDB.insertar(TABLE_NAME, valores);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            managerDB.CerrarConexion();
        }
        return resultado;
    }

    /**
     * READ - Obtener todos los tipos de traducción
     */
    public List<TranslationType> obtenerTodosLosTipos() {
        List<TranslationType> tipos = new ArrayList<>();
        Cursor cursor = null;

        try {
            managerDB.AbrirConexion();

            String query = "SELECT * FROM " + TABLE_NAME + " ORDER BY IdTypeTranslation ASC";
            cursor = managerDB.consultar(query, null);

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    TranslationType tipo = new TranslationType(
                            cursor.getInt(cursor.getColumnIndexOrThrow("IdTypeTranslation")),
                            cursor.getString(cursor.getColumnIndexOrThrow("Name"))
                    );
                    tipos.add(tipo);
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

        return tipos;
    }

    /**
     * READ - Obtener tipo de traducción por ID
     */
    public TranslationType obtenerTipoPorId(int idTypeTranslation) {
        TranslationType tipo = null;
        Cursor cursor = null;

        try {
            managerDB.AbrirConexion();

            String query = "SELECT * FROM " + TABLE_NAME + " WHERE IdTypeTranslation = ?";
            cursor = managerDB.consultar(query, new String[]{String.valueOf(idTypeTranslation)});

            if (cursor != null && cursor.moveToFirst()) {
                tipo = new TranslationType(
                        cursor.getInt(cursor.getColumnIndexOrThrow("IdTypeTranslation")),
                        cursor.getString(cursor.getColumnIndexOrThrow("Name"))
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

        return tipo;
    }

    /**
     * READ - Obtener tipo de traducción por nombre
     */
    public TranslationType obtenerTipoPorNombre(String nombre) {
        TranslationType tipo = null;
        Cursor cursor = null;

        try {
            managerDB.AbrirConexion();

            String query = "SELECT * FROM " + TABLE_NAME + " WHERE Name = ?";
            cursor = managerDB.consultar(query, new String[]{nombre});

            if (cursor != null && cursor.moveToFirst()) {
                tipo = new TranslationType(
                        cursor.getInt(cursor.getColumnIndexOrThrow("IdTypeTranslation")),
                        cursor.getString(cursor.getColumnIndexOrThrow("Name"))
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

        return tipo;
    }

    /**
     * UPDATE - Actualizar un tipo de traducción
     */
    public int actualizarTipoTraduccion(TranslationType type) {
        int filasActualizadas = 0;

        try {
            managerDB.AbrirConexion();

            ContentValues valores = new ContentValues();
            valores.put("Name", type.name);

            filasActualizadas = managerDB.actualizar(
                    TABLE_NAME,
                    valores,
                    "IdTypeTranslation = ?",
                    new String[]{String.valueOf(type.idTypeTranslation)}
            );

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            managerDB.CerrarConexion();
        }

        return filasActualizadas;
    }

    /**
     * DELETE - Eliminar tipo de traducción
     */
    public int eliminarTipoTraduccion(int idTypeTranslation) {
        int filasEliminadas = 0;

        try {
            managerDB.AbrirConexion();

            filasEliminadas = managerDB.eliminar(
                    TABLE_NAME,
                    "IdTypeTranslation = ?",
                    new String[]{String.valueOf(idTypeTranslation)}
            );

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            managerDB.CerrarConexion();
        }

        return filasEliminadas;
    }

    /**
     * Verificar si un nombre de tipo ya existe
     */
    public boolean existeNombreTipo(String nombre) {
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
     * Obtener ID por nombre (útil para referencias)
     */
    public int obtenerIdPorNombre(String nombre) {
        int id = -1;
        TranslationType tipo = obtenerTipoPorNombre(nombre);
        if (tipo != null) {
            id = tipo.idTypeTranslation;
        }
        return id;
    }
}