package com.example.traduccioncotorra.DB;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import java.util.ArrayList;
import java.util.List;

public class FavoriteTranslationDAO {
    private ManagerDB managerDB;
    private static final String TABLE_NAME = "FavoriteTranslation";

    public FavoriteTranslationDAO(Context context) {
        managerDB = new ManagerDB(context);
    }

    /**
     * Clase interna para representar una traducción favorita
     */
    public static class FavoriteTranslation {
        public int idFavoriteTranslation;
        public int userId;
        public int sourceLanguageId;
        public int targetLanguageId;
        public int translationTypeId;
        public String originalText;
        public String translatedText;
        public String savedDate;
        public int isFrequent;

        // Constructor completo
        public FavoriteTranslation(int idFavoriteTranslation, int userId,
                                   int sourceLanguageId, int targetLanguageId,
                                   int translationTypeId, String originalText,
                                   String translatedText, String savedDate, int isFrequent) {
            this.idFavoriteTranslation = idFavoriteTranslation;
            this.userId = userId;
            this.sourceLanguageId = sourceLanguageId;
            this.targetLanguageId = targetLanguageId;
            this.translationTypeId = translationTypeId;
            this.originalText = originalText;
            this.translatedText = translatedText;
            this.savedDate = savedDate;
            this.isFrequent = isFrequent;
        }

        // Constructor para insertar (sin ID ni fecha)
        public FavoriteTranslation(int userId, int sourceLanguageId,
                                   int targetLanguageId, int translationTypeId,
                                   String originalText, String translatedText) {
            this.userId = userId;
            this.sourceLanguageId = sourceLanguageId;
            this.targetLanguageId = targetLanguageId;
            this.translationTypeId = translationTypeId;
            this.originalText = originalText;
            this.translatedText = translatedText;
            this.isFrequent = 0;
        }
    }

    /**
     * CREATE - Insertar nueva traducción favorita
     */
    public long insertarFavorito(FavoriteTranslation favorito) {
        long resultado = -1;
        try {
            managerDB.AbrirConexion();

            ContentValues valores = new ContentValues();
            valores.put("UserId", favorito.userId);
            valores.put("SourceLanguageId", favorito.sourceLanguageId);
            valores.put("TargetLanguageId", favorito.targetLanguageId);
            valores.put("TranslationTypeId", favorito.translationTypeId);
            valores.put("OriginalText", favorito.originalText);
            valores.put("TranslatedText", favorito.translatedText);
            valores.put("IsFrequent", favorito.isFrequent);
            // SavedDate se asigna automáticamente

            resultado = managerDB.insertar(TABLE_NAME, valores);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            managerDB.CerrarConexion();
        }
        return resultado;
    }

    /**
     * READ - Obtener todos los favoritos de un usuario
     */
    public List<FavoriteTranslation> obtenerFavoritosPorUsuario(int userId) {
        List<FavoriteTranslation> favoritos = new ArrayList<>();
        Cursor cursor = null;

        try {
            managerDB.AbrirConexion();

            String query = "SELECT * FROM " + TABLE_NAME +
                    " WHERE UserId = ? ORDER BY SavedDate DESC";
            cursor = managerDB.consultar(query, new String[]{String.valueOf(userId)});

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    FavoriteTranslation favorito = new FavoriteTranslation(
                            cursor.getInt(cursor.getColumnIndexOrThrow("IdFavoriteTranslation")),
                            cursor.getInt(cursor.getColumnIndexOrThrow("UserId")),
                            cursor.getInt(cursor.getColumnIndexOrThrow("SourceLanguageId")),
                            cursor.getInt(cursor.getColumnIndexOrThrow("TargetLanguageId")),
                            cursor.getInt(cursor.getColumnIndexOrThrow("TranslationTypeId")),
                            cursor.getString(cursor.getColumnIndexOrThrow("OriginalText")),
                            cursor.getString(cursor.getColumnIndexOrThrow("TranslatedText")),
                            cursor.getString(cursor.getColumnIndexOrThrow("SavedDate")),
                            cursor.getInt(cursor.getColumnIndexOrThrow("IsFrequent"))
                    );
                    favoritos.add(favorito);
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

        return favoritos;
    }

    /**
     * READ - Buscar favoritos por texto (original o traducido)
     */
    public List<FavoriteTranslation> buscarFavoritos(int userId, String query) {
        List<FavoriteTranslation> favoritos = new ArrayList<>();
        Cursor cursor = null;

        try {
            managerDB.AbrirConexion();

            String sql = "SELECT * FROM " + TABLE_NAME +
                    " WHERE UserId = ? AND (OriginalText LIKE ? OR TranslatedText LIKE ?)" +
                    " ORDER BY SavedDate DESC";

            String searchPattern = "%" + query + "%";
            cursor = managerDB.consultar(sql,
                    new String[]{String.valueOf(userId), searchPattern, searchPattern});

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    FavoriteTranslation favorito = new FavoriteTranslation(
                            cursor.getInt(cursor.getColumnIndexOrThrow("IdFavoriteTranslation")),
                            cursor.getInt(cursor.getColumnIndexOrThrow("UserId")),
                            cursor.getInt(cursor.getColumnIndexOrThrow("SourceLanguageId")),
                            cursor.getInt(cursor.getColumnIndexOrThrow("TargetLanguageId")),
                            cursor.getInt(cursor.getColumnIndexOrThrow("TranslationTypeId")),
                            cursor.getString(cursor.getColumnIndexOrThrow("OriginalText")),
                            cursor.getString(cursor.getColumnIndexOrThrow("TranslatedText")),
                            cursor.getString(cursor.getColumnIndexOrThrow("SavedDate")),
                            cursor.getInt(cursor.getColumnIndexOrThrow("IsFrequent"))
                    );
                    favoritos.add(favorito);
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

        return favoritos;
    }

    /**
     * READ - Verificar si una traducción ya existe en favoritos
     */
    public boolean existeFavorito(int userId, String originalText, String translatedText) {
        boolean existe = false;
        Cursor cursor = null;

        try {
            managerDB.AbrirConexion();

            String query = "SELECT COUNT(*) FROM " + TABLE_NAME +
                    " WHERE UserId = ? AND OriginalText = ? AND TranslatedText = ?";
            cursor = managerDB.consultar(query,
                    new String[]{String.valueOf(userId), originalText, translatedText});

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
     * READ - Obtener favorito por ID
     */
    public FavoriteTranslation obtenerFavoritoPorId(int idFavorite) {
        FavoriteTranslation favorito = null;
        Cursor cursor = null;

        try {
            managerDB.AbrirConexion();

            String query = "SELECT * FROM " + TABLE_NAME +
                    " WHERE IdFavoriteTranslation = ?";
            cursor = managerDB.consultar(query, new String[]{String.valueOf(idFavorite)});

            if (cursor != null && cursor.moveToFirst()) {
                favorito = new FavoriteTranslation(
                        cursor.getInt(cursor.getColumnIndexOrThrow("IdFavoriteTranslation")),
                        cursor.getInt(cursor.getColumnIndexOrThrow("UserId")),
                        cursor.getInt(cursor.getColumnIndexOrThrow("SourceLanguageId")),
                        cursor.getInt(cursor.getColumnIndexOrThrow("TargetLanguageId")),
                        cursor.getInt(cursor.getColumnIndexOrThrow("TranslationTypeId")),
                        cursor.getString(cursor.getColumnIndexOrThrow("OriginalText")),
                        cursor.getString(cursor.getColumnIndexOrThrow("TranslatedText")),
                        cursor.getString(cursor.getColumnIndexOrThrow("SavedDate")),
                        cursor.getInt(cursor.getColumnIndexOrThrow("IsFrequent"))
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

        return favorito;
    }

    /**
     * UPDATE - Marcar/desmarcar como frecuente
     */
    public int actualizarFrecuente(int idFavorite, boolean esFrecuente) {
        int filasActualizadas = 0;

        try {
            managerDB.AbrirConexion();

            ContentValues valores = new ContentValues();
            valores.put("IsFrequent", esFrecuente ? 1 : 0);

            filasActualizadas = managerDB.actualizar(
                    TABLE_NAME,
                    valores,
                    "IdFavoriteTranslation = ?",
                    new String[]{String.valueOf(idFavorite)}
            );

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            managerDB.CerrarConexion();
        }

        return filasActualizadas;
    }

    /**
     * DELETE - Eliminar favorito por ID
     */
    public int eliminarFavorito(int idFavorite) {
        int filasEliminadas = 0;

        try {
            managerDB.AbrirConexion();

            filasEliminadas = managerDB.eliminar(
                    TABLE_NAME,
                    "IdFavoriteTranslation = ?",
                    new String[]{String.valueOf(idFavorite)}
            );

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            managerDB.CerrarConexion();
        }

        return filasEliminadas;
    }

    /**
     * DELETE - Eliminar favorito por textos (usado para el botón toggle)
     */
    public int eliminarFavoritoPorTextos(int userId, String originalText, String translatedText) {
        int filasEliminadas = 0;

        try {
            managerDB.AbrirConexion();

            filasEliminadas = managerDB.eliminar(
                    TABLE_NAME,
                    "UserId = ? AND OriginalText = ? AND TranslatedText = ?",
                    new String[]{String.valueOf(userId), originalText, translatedText}
            );

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            managerDB.CerrarConexion();
        }

        return filasEliminadas;
    }

    /**
     * DELETE - Eliminar todos los favoritos de un usuario
     */
    public int eliminarTodosFavoritos(int userId) {
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
     * UTILITY - Contar favoritos de un usuario
     */
    public int contarFavoritos(int userId) {
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

    /**
     * UTILITY - Obtener favoritos frecuentes
     */
    public List<FavoriteTranslation> obtenerFavoritosFrecuentes(int userId) {
        List<FavoriteTranslation> favoritos = new ArrayList<>();
        Cursor cursor = null;

        try {
            managerDB.AbrirConexion();

            String query = "SELECT * FROM " + TABLE_NAME +
                    " WHERE UserId = ? AND IsFrequent = 1 ORDER BY SavedDate DESC";
            cursor = managerDB.consultar(query, new String[]{String.valueOf(userId)});

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    FavoriteTranslation favorito = new FavoriteTranslation(
                            cursor.getInt(cursor.getColumnIndexOrThrow("IdFavoriteTranslation")),
                            cursor.getInt(cursor.getColumnIndexOrThrow("UserId")),
                            cursor.getInt(cursor.getColumnIndexOrThrow("SourceLanguageId")),
                            cursor.getInt(cursor.getColumnIndexOrThrow("TargetLanguageId")),
                            cursor.getInt(cursor.getColumnIndexOrThrow("TranslationTypeId")),
                            cursor.getString(cursor.getColumnIndexOrThrow("OriginalText")),
                            cursor.getString(cursor.getColumnIndexOrThrow("TranslatedText")),
                            cursor.getString(cursor.getColumnIndexOrThrow("SavedDate")),
                            cursor.getInt(cursor.getColumnIndexOrThrow("IsFrequent"))
                    );
                    favoritos.add(favorito);
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

        return favoritos;
    }
}