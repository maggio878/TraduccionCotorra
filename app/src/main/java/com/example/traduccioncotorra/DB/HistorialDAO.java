package com.example.traduccioncotorra.DB;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import java.util.ArrayList;
import java.util.List;

public class HistorialDAO {
    private ManagerDB managerDB;
    private static final String TABLE_NAME = "TranslationHistory";

    public HistorialDAO(Context context) {
        managerDB = new ManagerDB(context);
    }

    /**
     * Clase interna para representar un registro del historial
     */
    public static class HistorialItem {
        public int historyId;
        public int userId;
        public String textoOriginal;
        public String textoTraducido;
        public String idiomaOrigen;
        public String idiomaDestino;
        public String fechaTraduccion;

        public HistorialItem(int historyId, int userId, String textoOriginal,
                             String textoTraducido, String idiomaOrigen,
                             String idiomaDestino, String fechaTraduccion) {
            this.historyId = historyId;
            this.userId = userId;
            this.textoOriginal = textoOriginal;
            this.textoTraducido = textoTraducido;
            this.idiomaOrigen = idiomaOrigen;
            this.idiomaDestino = idiomaDestino;
            this.fechaTraduccion = fechaTraduccion;
        }
    }

    /**
     * Inserta una nueva traducción en el historial
     */
    public long insertarHistorial(int userId, String textoOriginal, String textoTraducido,
                                  String idiomaOrigen, String idiomaDestino) {
        long resultado = -1;
        try {
            managerDB.AbrirConexion();

            ContentValues valores = new ContentValues();
            valores.put("UserId", userId);
            valores.put("TextoOriginal", textoOriginal);
            valores.put("TextoTraducido", textoTraducido);
            valores.put("IdiomaOrigen", idiomaOrigen);
            valores.put("IdiomaDestino", idiomaDestino);
            // FechaTraduccion se asigna automáticamente

            resultado = managerDB.insertar(TABLE_NAME, valores);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            managerDB.CerrarConexion();
        }
        return resultado;
    }

    /**
     * Obtiene todo el historial de un usuario
     */
    public List<HistorialItem> obtenerHistorialPorUsuario(int userId) {
        List<HistorialItem> historial = new ArrayList<>();
        Cursor cursor = null;

        try {
            managerDB.AbrirConexion();

            String query = "SELECT * FROM " + TABLE_NAME +
                    " WHERE UserId = ? ORDER BY FechaTraduccion DESC";
            cursor = managerDB.consultar(query, new String[]{String.valueOf(userId)});

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    HistorialItem item = new HistorialItem(
                            cursor.getInt(cursor.getColumnIndexOrThrow("HistoryId")),
                            cursor.getInt(cursor.getColumnIndexOrThrow("UserId")),
                            cursor.getString(cursor.getColumnIndexOrThrow("TextoOriginal")),
                            cursor.getString(cursor.getColumnIndexOrThrow("TextoTraducido")),
                            cursor.getString(cursor.getColumnIndexOrThrow("IdiomaOrigen")),
                            cursor.getString(cursor.getColumnIndexOrThrow("IdiomaDestino")),
                            cursor.getString(cursor.getColumnIndexOrThrow("FechaTraduccion"))
                    );
                    historial.add(item);
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

        return historial;
    }

    /**
     * Busca en el historial por texto (original o traducido)
     */
    public List<HistorialItem> buscarEnHistorial(int userId, String query) {
        List<HistorialItem> historial = new ArrayList<>();
        Cursor cursor = null;

        try {
            managerDB.AbrirConexion();

            String sql = "SELECT * FROM " + TABLE_NAME +
                    " WHERE UserId = ? AND (TextoOriginal LIKE ? OR TextoTraducido LIKE ?)" +
                    " ORDER BY FechaTraduccion DESC";

            String searchPattern = "%" + query + "%";
            cursor = managerDB.consultar(sql,
                    new String[]{String.valueOf(userId), searchPattern, searchPattern});

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    HistorialItem item = new HistorialItem(
                            cursor.getInt(cursor.getColumnIndexOrThrow("HistoryId")),
                            cursor.getInt(cursor.getColumnIndexOrThrow("UserId")),
                            cursor.getString(cursor.getColumnIndexOrThrow("TextoOriginal")),
                            cursor.getString(cursor.getColumnIndexOrThrow("TextoTraducido")),
                            cursor.getString(cursor.getColumnIndexOrThrow("IdiomaOrigen")),
                            cursor.getString(cursor.getColumnIndexOrThrow("IdiomaDestino")),
                            cursor.getString(cursor.getColumnIndexOrThrow("FechaTraduccion"))
                    );
                    historial.add(item);
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

        return historial;
    }

    /**
     * Elimina un elemento específico del historial
     */
    public int eliminarHistorial(int historyId) {
        int filasEliminadas = 0;

        try {
            managerDB.AbrirConexion();

            filasEliminadas = managerDB.eliminar(
                    TABLE_NAME,
                    "HistoryId = ?",
                    new String[]{String.valueOf(historyId)}
            );

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            managerDB.CerrarConexion();
        }

        return filasEliminadas;
    }

    /**
     * Elimina todo el historial de un usuario
     */
    public int eliminarTodoHistorial(int userId) {
        int filasEliminadas = 0;

        try {
            managerDB.AbrirConexion();

            filasEliminadas = managerDB.eliminar(
                    TABLE_NAME,
                    "UserId = ?",
                    new String[]{String.valueOf(userId)}
            );

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            managerDB.CerrarConexion();
        }

        return filasEliminadas;
    }

    /**
     * Obtiene el conteo de elementos en el historial
     */
    public int contarHistorial(int userId) {
        int count = 0;
        Cursor cursor = null;

        try {
            managerDB.AbrirConexion();

            String query = "SELECT COUNT(*) FROM " + TABLE_NAME + " WHERE UserId = ?";
            cursor = managerDB.consultar(query, new String[]{String.valueOf(userId)});

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