package com.example.traduccioncotorra.Utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import java.io.File;

public class IdiomasCache {
    private static final String TAG = "IDIOMAS_CACHE";
    private static final String PREFS_NAME = "idiomas_cache_prefs";
    private static final String KEY_IDIOMA_DESCARGADO = "idioma_descargado_";
    private static final String KEY_VERSION = "version_";
    private static final String KEY_PRIMERA_VEZ = "primera_vez_descarga";

    private Context context;
    private SharedPreferences prefs;

    public IdiomasCache(Context context) {
        this.context = context;
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public boolean idiomaYaDescargado(String codigoIdiomaOrigen, String codigoIdiomaDestino) {
        String clave = codigoIdiomaOrigen + "_" + codigoIdiomaDestino;
        boolean descargado = prefs.getBoolean(KEY_IDIOMA_DESCARGADO + clave, false);

        Log.d(TAG, "Verificando idioma " + clave + ": " + (descargado ? "✅ Ya descargado" : "❌ No descargado"));
        return descargado;
    }

    public void marcarIdiomaDescargado(String codigoIdiomaOrigen, String codigoIdiomaDestino) {
        String clave = codigoIdiomaOrigen + "_" + codigoIdiomaDestino;

        prefs.edit()
                .putBoolean(KEY_IDIOMA_DESCARGADO + clave, true)
                .putString(KEY_VERSION + clave, "1.0")
                .putLong(clave + "_timestamp", System.currentTimeMillis())
                .apply();

        Log.d(TAG, "✅ Idioma " + clave + " marcado como descargado");
    }

    public boolean esPrimeraVez() {
        return prefs.getBoolean(KEY_PRIMERA_VEZ, true);
    }

    public void marcarNoEsPrimeraVez() {
        prefs.edit().putBoolean(KEY_PRIMERA_VEZ, false).apply();
    }

    public void eliminarIdiomaCache(String codigoIdiomaOrigen, String codigoIdiomaDestino) {
        String clave = codigoIdiomaOrigen + "_" + codigoIdiomaDestino;

        prefs.edit()
                .remove(KEY_IDIOMA_DESCARGADO + clave)
                .remove(KEY_VERSION + clave)
                .remove(clave + "_timestamp")
                .apply();

        Log.d(TAG, "🗑️ Marca de idioma " + clave + " eliminada");
    }

    public void limpiarTodaCache() {
        prefs.edit().clear().apply();
        Log.d(TAG, "🗑️ Toda la caché eliminada");
    }

    public int getCantidadIdiomasDescargados() {
        int count = 0;
        for (String key : prefs.getAll().keySet()) {
            if (key.startsWith(KEY_IDIOMA_DESCARGADO) && prefs.getBoolean(key, false)) {
                count++;
            }
        }
        return count;
    }

    public String getInfoCache() {
        int cantidad = getCantidadIdiomasDescargados();
        StringBuilder info = new StringBuilder();
        info.append("Paquetes de idiomas descargados: ").append(cantidad).append("\n\n");

        if (cantidad > 0) {
            info.append("Idiomas:\n");
            for (String key : prefs.getAll().keySet()) {
                if (key.startsWith(KEY_IDIOMA_DESCARGADO)) {
                    String idioma = key.replace(KEY_IDIOMA_DESCARGADO, "");
                    info.append("• ").append(idioma).append("\n");
                }
            }
        }

        return info.toString();
    }
}