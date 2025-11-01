package com.example.traduccioncotorra.DB;


import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

public class UserConfigurationDAO {
    private ManagerDB managerDB;
    private static final String TABLE_NAME = "UserConfiguration";

    public UserConfigurationDAO(Context context) {
        managerDB = new ManagerDB(context);
    }

    /**
     * Clase interna para representar la configuración de un usuario
     */
    public static class UserConfiguration {
        public int configurationId;
        public int userId;
        public int primaryLanguageId;
        public String theme;
        public int soundsEnable;
        public int offlineEnable;
        public int notificationsEnable;
        public int fontSize;
        public String lastUpdated;

        public UserConfiguration() {
            // Constructor vacío
        }

        public UserConfiguration(int userId, int primaryLanguageId, String theme,
                                 int soundsEnable, int offlineEnable,
                                 int notificationsEnable, int fontSize) {
            this.userId = userId;
            this.primaryLanguageId = primaryLanguageId;
            this.theme = theme;
            this.soundsEnable = soundsEnable;
            this.offlineEnable = offlineEnable;
            this.notificationsEnable = notificationsEnable;
            this.fontSize = fontSize;
        }
    }

    /**
     * CREATE - Insertar configuración inicial del usuario
     */
    public long insertarConfiguracion(UserConfiguration config) {
        long resultado = -1;
        try {
            managerDB.AbrirConexion();

            ContentValues valores = new ContentValues();
            valores.put("UserId", config.userId);
            valores.put("PrimaryLanguageId", config.primaryLanguageId);
            valores.put("Theme", config.theme);
            valores.put("SoundsEnable", config.soundsEnable);
            valores.put("OfflineEnable", config.offlineEnable);
            valores.put("NotificationsEnable", config.notificationsEnable);
            valores.put("FontSize", config.fontSize);
            // LastUpdated se asigna automáticamente con CURRENT_TIMESTAMP

            resultado = managerDB.insertar(TABLE_NAME, valores);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            managerDB.CerrarConexion();
        }
        return resultado;
    }

    /**
     * CREATE - Insertar configuración por defecto para un usuario
     */
    public long crearConfiguracionPorDefecto(int userId, int primaryLanguageId) {
        UserConfiguration configDefault = new UserConfiguration(
                userId,
                primaryLanguageId,
                "light",  // Tema claro por defecto
                1,        // Sonidos activados
                0,        // Modo offline desactivado
                1,        // Notificaciones activadas
                14        // Tamaño de fuente 14
        );

        return insertarConfiguracion(configDefault);
    }

    /**
     * READ - Obtener configuración de un usuario
     */
    public UserConfiguration obtenerConfiguracionPorUsuario(int userId) {
        UserConfiguration config = null;
        Cursor cursor = null;

        try {
            managerDB.AbrirConexion();

            String query = "SELECT * FROM " + TABLE_NAME + " WHERE UserId = ?";
            cursor = managerDB.consultar(query, new String[]{String.valueOf(userId)});

            if (cursor != null && cursor.moveToFirst()) {
                config = new UserConfiguration();
                config.configurationId = cursor.getInt(cursor.getColumnIndexOrThrow("ConfigurationId"));
                config.userId = cursor.getInt(cursor.getColumnIndexOrThrow("UserId"));
                config.primaryLanguageId = cursor.getInt(cursor.getColumnIndexOrThrow("PrimaryLanguageId"));
                config.theme = cursor.getString(cursor.getColumnIndexOrThrow("Theme"));
                config.soundsEnable = cursor.getInt(cursor.getColumnIndexOrThrow("SoundsEnable"));
                config.offlineEnable = cursor.getInt(cursor.getColumnIndexOrThrow("OfflineEnable"));
                config.notificationsEnable = cursor.getInt(cursor.getColumnIndexOrThrow("NotificationsEnable"));
                config.fontSize = cursor.getInt(cursor.getColumnIndexOrThrow("FontSize"));
                config.lastUpdated = cursor.getString(cursor.getColumnIndexOrThrow("LastUpdated"));
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            managerDB.CerrarConexion();
        }

        return config;
    }

    /**
     * READ - Verificar si un usuario ya tiene configuración
     */
    public boolean tieneConfiguracion(int userId) {
        boolean existe = false;
        Cursor cursor = null;

        try {
            managerDB.AbrirConexion();

            String query = "SELECT COUNT(*) FROM " + TABLE_NAME + " WHERE UserId = ?";
            cursor = managerDB.consultar(query, new String[]{String.valueOf(userId)});

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
     * UPDATE - Actualizar configuración completa
     */
    public int actualizarConfiguracion(UserConfiguration config) {
        int filasActualizadas = 0;

        try {
            managerDB.AbrirConexion();

            ContentValues valores = new ContentValues();
            valores.put("PrimaryLanguageId", config.primaryLanguageId);
            valores.put("Theme", config.theme);
            valores.put("SoundsEnable", config.soundsEnable);
            valores.put("OfflineEnable", config.offlineEnable);
            valores.put("NotificationsEnable", config.notificationsEnable);
            valores.put("FontSize", config.fontSize);
            valores.put("LastUpdated", "CURRENT_TIMESTAMP");

            filasActualizadas = managerDB.actualizar(
                    TABLE_NAME,
                    valores,
                    "UserId = ?",
                    new String[]{String.valueOf(config.userId)}
            );

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            managerDB.CerrarConexion();
        }

        return filasActualizadas;
    }

    /**
     * UPDATE - Actualizar idioma principal
     */
    public int actualizarIdiomaPrincipal(int userId, int languageId) {
        int filasActualizadas = 0;

        try {
            managerDB.AbrirConexion();

            ContentValues valores = new ContentValues();
            valores.put("PrimaryLanguageId", languageId);

            filasActualizadas = managerDB.actualizar(
                    TABLE_NAME,
                    valores,
                    "UserId = ?",
                    new String[]{String.valueOf(userId)}
            );

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            managerDB.CerrarConexion();
        }

        return filasActualizadas;
    }

    /**
     * UPDATE - Actualizar tema (light/dark)
     */
    public int actualizarTema(int userId, String theme) {
        int filasActualizadas = 0;

        try {
            managerDB.AbrirConexion();

            ContentValues valores = new ContentValues();
            valores.put("Theme", theme);

            filasActualizadas = managerDB.actualizar(
                    TABLE_NAME,
                    valores,
                    "UserId = ?",
                    new String[]{String.valueOf(userId)}
            );

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            managerDB.CerrarConexion();
        }

        return filasActualizadas;
    }

    /**
     * UPDATE - Actualizar sonidos
     */
    public int actualizarSonidos(int userId, boolean activar) {
        int filasActualizadas = 0;

        try {
            managerDB.AbrirConexion();

            ContentValues valores = new ContentValues();
            valores.put("SoundsEnable", activar ? 1 : 0);

            filasActualizadas = managerDB.actualizar(
                    TABLE_NAME,
                    valores,
                    "UserId = ?",
                    new String[]{String.valueOf(userId)}
            );

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            managerDB.CerrarConexion();
        }

        return filasActualizadas;
    }

    /**
     * UPDATE - Actualizar modo offline
     */
    public int actualizarModoOffline(int userId, boolean activar) {
        int filasActualizadas = 0;

        try {
            managerDB.AbrirConexion();

            ContentValues valores = new ContentValues();
            valores.put("OfflineEnable", activar ? 1 : 0);

            filasActualizadas = managerDB.actualizar(
                    TABLE_NAME,
                    valores,
                    "UserId = ?",
                    new String[]{String.valueOf(userId)}
            );

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            managerDB.CerrarConexion();
        }

        return filasActualizadas;
    }

    /**
     * UPDATE - Actualizar notificaciones
     */
    public int actualizarNotificaciones(int userId, boolean activar) {
        int filasActualizadas = 0;

        try {
            managerDB.AbrirConexion();

            ContentValues valores = new ContentValues();
            valores.put("NotificationsEnable", activar ? 1 : 0);

            filasActualizadas = managerDB.actualizar(
                    TABLE_NAME,
                    valores,
                    "UserId = ?",
                    new String[]{String.valueOf(userId)}
            );

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            managerDB.CerrarConexion();
        }

        return filasActualizadas;
    }

    /**
     * UPDATE - Actualizar tamaño de fuente
     */
    public int actualizarTamanoFuente(int userId, int fontSize) {
        int filasActualizadas = 0;

        try {
            managerDB.AbrirConexion();

            ContentValues valores = new ContentValues();
            valores.put("FontSize", fontSize);

            filasActualizadas = managerDB.actualizar(
                    TABLE_NAME,
                    valores,
                    "UserId = ?",
                    new String[]{String.valueOf(userId)}
            );

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            managerDB.CerrarConexion();
        }

        return filasActualizadas;
    }

    /**
     * DELETE - Eliminar configuración de un usuario
     */
    public int eliminarConfiguracion(int userId) {
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
     * UTILITY - Obtener o crear configuración
     * Si el usuario no tiene configuración, crea una por defecto
     */
    public UserConfiguration obtenerOCrearConfiguracion(int userId, int primaryLanguageId) {
        UserConfiguration config = obtenerConfiguracionPorUsuario(userId);

        if (config == null) {
            // No existe configuración, crear una por defecto
            long resultado = crearConfiguracionPorDefecto(userId, primaryLanguageId);

            if (resultado != -1) {
                // Obtener la configuración recién creada
                config = obtenerConfiguracionPorUsuario(userId);
            }
        }

        return config;
    }
}
