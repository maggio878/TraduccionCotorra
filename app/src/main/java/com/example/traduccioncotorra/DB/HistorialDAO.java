package com.example.traduccioncotorra.DB;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.util.Log;
import java.util.ArrayList;
import java.util.List;

public class HistorialDAO {
    private ManagerDB managerDB;
    private static final String TABLE_NAME = "TranslationHistory";
    private static final String TAG = "HISTORIAL_DAO";

    public HistorialDAO(Context context) {
        managerDB = new ManagerDB(context);
    }

    /**
     * Clase interna para representar un registro del historial
     */
    public static class HistorialItem {
        public int historyId;
        public int userId;
        public int sourceLanguageId;
        public int targetLanguageId;
        public int translationTypeId;
        public String inputText;
        public String resultText;
        public String translationDate;

        // Campos adicionales para mostrar (nombres en lugar de IDs)
        public String idiomaOrigen;
        public String idiomaDestino;
        public String tipoTraduccion;

        public HistorialItem(int historyId, int userId, int sourceLanguageId,
                             int targetLanguageId, int translationTypeId,
                             String inputText, String resultText, String translationDate) {
            this.historyId = historyId;
            this.userId = userId;
            this.sourceLanguageId = sourceLanguageId;
            this.targetLanguageId = targetLanguageId;
            this.translationTypeId = translationTypeId;
            this.inputText = inputText;
            this.resultText = resultText;
            this.translationDate = translationDate;
        }
    }

    /**
     * ⭐ CORREGIDO: Inserta usando IDs de idiomas y tipo de traducción
     */
    public long insertarHistorial(int userId, int sourceLanguageId, int targetLanguageId,
                                  int translationTypeId, String inputText, String resultText) {
        long resultado = -1;

        // Validaciones básicas
        if (inputText == null || inputText.trim().isEmpty()) {
            Log.w(TAG, "Intento de insertar historial con texto original vacío");
            return -1;
        }

        if (resultText == null || resultText.trim().isEmpty()) {
            Log.w(TAG, "Intento de insertar historial con texto traducido vacío");
            return -1;
        }

        try {
            managerDB.AbrirConexion();

            ContentValues valores = new ContentValues();
            valores.put("UserId", userId);
            valores.put("SourceLanguageId", sourceLanguageId);
            valores.put("TargetLanguageId", targetLanguageId);
            valores.put("TranslationTypeId", translationTypeId);
            valores.put("InputText", inputText.trim());
            valores.put("ResultText", resultText.trim());
            // TranslationDate se asigna automáticamente con CURRENT_TIMESTAMP

            resultado = managerDB.insertar(TABLE_NAME, valores);

            if (resultado != -1) {
                Log.d(TAG, "✅ Historial insertado exitosamente. ID: " + resultado);
                Log.d(TAG, "  SourceLangId: " + sourceLanguageId + " -> TargetLangId: " + targetLanguageId);
                Log.d(TAG, "  TranslationType: " + translationTypeId);
                Log.d(TAG, "  Texto: " + inputText.substring(0, Math.min(50, inputText.length())) + "...");
            } else {
                Log.e(TAG, "❌ Error al insertar historial. Resultado: -1");
            }

        } catch (Exception e) {
            Log.e(TAG, "❌ Excepción al insertar historial", e);
            e.printStackTrace();
        } finally {
            managerDB.CerrarConexion();
        }

        return resultado;
    }

    /**
     * ⭐ NUEVO: Verifica si ya existe una traducción similar reciente (últimos 2 minutos)
     */
    public boolean existeTraduccionReciente(int userId, String inputText,
                                            int sourceLanguageId, int targetLanguageId) {
        boolean existe = false;
        Cursor cursor = null;

        try {
            managerDB.AbrirConexion();

            // Buscar traducciones similares en los últimos 2 minutos
            String query = "SELECT COUNT(*) FROM " + TABLE_NAME +
                    " WHERE UserId = ? AND InputText = ? " +
                    " AND SourceLanguageId = ? AND TargetLanguageId = ? " +
                    " AND datetime(TranslationDate) > datetime('now', '-2 minutes')";

            cursor = managerDB.consultar(query,
                    new String[]{
                            String.valueOf(userId),
                            inputText,
                            String.valueOf(sourceLanguageId),
                            String.valueOf(targetLanguageId)
                    });

            if (cursor != null && cursor.moveToFirst()) {
                existe = cursor.getInt(0) > 0;
            }

        } catch (Exception e) {
            Log.e(TAG, "Error al verificar traducción reciente", e);
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
     * ⭐ NUEVO: Inserta solo si no existe una traducción reciente similar
     */
    public long insertarHistorialSiNoExiste(int userId, int sourceLanguageId, int targetLanguageId,
                                            int translationTypeId, String inputText, String resultText) {
        // Verificar si ya existe
        if (existeTraduccionReciente(userId, inputText, sourceLanguageId, targetLanguageId)) {
            Log.d(TAG, "⚠️ Traducción similar ya existe en los últimos 2 minutos. No se insertará.");
            return -1;
        }

        // Insertar normalmente
        return insertarHistorial(userId, sourceLanguageId, targetLanguageId,
                translationTypeId, inputText, resultText);
    }

    /**
     * ⭐ CORREGIDO: Obtiene historial con JOIN para mostrar nombres
     */
    public List<HistorialItem> obtenerHistorialPorUsuario(int userId) {
        List<HistorialItem> historial = new ArrayList<>();
        Cursor cursor = null;

        try {
            managerDB.AbrirConexion();

            // Query con JOINs para obtener nombres de idiomas y tipo
            String query = "SELECT h.*, " +
                    "ls.Name as SourceLangName, " +
                    "lt.Name as TargetLangName, " +
                    "tt.Name as TypeName " +
                    "FROM " + TABLE_NAME + " h " +
                    "LEFT JOIN Language ls ON h.SourceLanguageId = ls.LanguageId " +
                    "LEFT JOIN Language lt ON h.TargetLanguageId = lt.LanguageId " +
                    "LEFT JOIN TranslationType tt ON h.TranslationTypeId = tt.IdTypeTranslation " +
                    "WHERE h.UserId = ? " +
                    "ORDER BY h.TranslationDate DESC";

            cursor = managerDB.consultar(query, new String[]{String.valueOf(userId)});

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    HistorialItem item = new HistorialItem(
                            cursor.getInt(cursor.getColumnIndexOrThrow("HistoryId")),
                            cursor.getInt(cursor.getColumnIndexOrThrow("UserId")),
                            cursor.getInt(cursor.getColumnIndexOrThrow("SourceLanguageId")),
                            cursor.getInt(cursor.getColumnIndexOrThrow("TargetLanguageId")),
                            cursor.getInt(cursor.getColumnIndexOrThrow("TranslationTypeId")),
                            cursor.getString(cursor.getColumnIndexOrThrow("InputText")),
                            cursor.getString(cursor.getColumnIndexOrThrow("ResultText")),
                            cursor.getString(cursor.getColumnIndexOrThrow("TranslationDate"))
                    );

                    // Agregar nombres para mostrar
                    item.idiomaOrigen = cursor.getString(cursor.getColumnIndexOrThrow("SourceLangName"));
                    item.idiomaDestino = cursor.getString(cursor.getColumnIndexOrThrow("TargetLangName"));
                    item.tipoTraduccion = cursor.getString(cursor.getColumnIndexOrThrow("TypeName"));

                    historial.add(item);
                } while (cursor.moveToNext());
            }

            Log.d(TAG, "Historial obtenido: " + historial.size() + " elementos");

        } catch (Exception e) {
            Log.e(TAG, "Error al obtener historial", e);
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
     * Busca en el historial por texto (input o result)
     */
    public List<HistorialItem> buscarEnHistorial(int userId, String query) {
        List<HistorialItem> historial = new ArrayList<>();
        Cursor cursor = null;

        try {
            managerDB.AbrirConexion();

            String sql = "SELECT h.*, " +
                    "ls.Name as SourceLangName, " +
                    "lt.Name as TargetLangName, " +
                    "tt.Name as TypeName " +
                    "FROM " + TABLE_NAME + " h " +
                    "LEFT JOIN Language ls ON h.SourceLanguageId = ls.Language_Id " +
                    "LEFT JOIN Language lt ON h.TargetLanguageId = lt.Language_Id " +
                    "LEFT JOIN TranslationType tt ON h.TranslationTypeId = tt.IdTypeTranslation " +
                    "WHERE h.UserId = ? AND (h.InputText LIKE ? OR h.ResultText LIKE ?) " +
                    "ORDER BY h.TranslationDate DESC";

            String searchPattern = "%" + query + "%";
            cursor = managerDB.consultar(sql,
                    new String[]{String.valueOf(userId), searchPattern, searchPattern});

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    HistorialItem item = new HistorialItem(
                            cursor.getInt(cursor.getColumnIndexOrThrow("HistoryId")),
                            cursor.getInt(cursor.getColumnIndexOrThrow("UserId")),
                            cursor.getInt(cursor.getColumnIndexOrThrow("SourceLanguageId")),
                            cursor.getInt(cursor.getColumnIndexOrThrow("TargetLanguageId")),
                            cursor.getInt(cursor.getColumnIndexOrThrow("TranslationTypeId")),
                            cursor.getString(cursor.getColumnIndexOrThrow("InputText")),
                            cursor.getString(cursor.getColumnIndexOrThrow("ResultText")),
                            cursor.getString(cursor.getColumnIndexOrThrow("TranslationDate"))
                    );

                    item.idiomaOrigen = cursor.getString(cursor.getColumnIndexOrThrow("SourceLangName"));
                    item.idiomaDestino = cursor.getString(cursor.getColumnIndexOrThrow("TargetLangName"));
                    item.tipoTraduccion = cursor.getString(cursor.getColumnIndexOrThrow("TypeName"));

                    historial.add(item);
                } while (cursor.moveToNext());
            }

        } catch (Exception e) {
            Log.e(TAG, "Error al buscar en historial", e);
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

            Log.d(TAG, "Historial eliminado. ID: " + historyId + ", Filas: " + filasEliminadas);

        } catch (Exception e) {
            Log.e(TAG, "Error al eliminar historial", e);
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

            Log.d(TAG, "Todo el historial eliminado. UserId: " + userId + ", Filas: " + filasEliminadas);

        } catch (Exception e) {
            Log.e(TAG, "Error al eliminar todo el historial", e);
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
            Log.e(TAG, "Error al contar historial", e);
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