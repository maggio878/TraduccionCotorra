package com.example.traduccioncotorra.DB;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import java.io.IOException;

public class ManagerDB {
    private ConexionDB conexion;
    private SQLiteDatabase basededatos;
    private Context context;

    public ManagerDB(Context context){
        this.context = context;
        this.conexion = new ConexionDB(context);
    }

    public ManagerDB AbrirConexion() throws SQLException {
        try {
            // Crea/copia la base de datos si no existe
            conexion.createDataBase();
        } catch (IOException e) {
            throw new Error("No se pudo crear la base de datos");
        }

        // Abre la base de datos
        basededatos = conexion.getWritableDatabase();
        return this;
    }

    public void CerrarConexion(){
        if (basededatos != null && basededatos.isOpen()) {
            basededatos.close();
        }
        conexion.close();
    }

    public SQLiteDatabase getBaseDeDatos() {
        return basededatos;
    }

    public Cursor consultar(String query, String[] selectionArgs) {
        return basededatos.rawQuery(query, selectionArgs);
    }

    public long insertar(String tabla, ContentValues valores) {
        return basededatos.insert(tabla, null, valores);
    }

    public int actualizar(String tabla, ContentValues valores, String whereClause, String[] whereArgs) {
        return basededatos.update(tabla, valores, whereClause, whereArgs);
    }

    public int eliminar(String tabla, String whereClause, String[] whereArgs) {
        return basededatos.delete(tabla, whereClause, whereArgs);
    }
}