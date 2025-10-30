package com.example.traduccioncotorra.DB;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class ConexionDB extends SQLiteOpenHelper {
    private static final String DB_NAME = "TraduccionCotorraBD.db";
    private static final int DB_VERSION = 2;
    private final Context context;
    private String DB_PATH;

    public ConexionDB(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
        this.context = context;
        // Ruta donde Android guarda las bases de datos
        DB_PATH = context.getDatabasePath(DB_NAME).getPath();
    }
    public void createDataBase() throws IOException {
        boolean dbExist = checkDataBase();

        if (!dbExist) {
            // Esto crea la base de datos vacía en el sistema
            this.getReadableDatabase();
            this.close();

            try {
                copyDataBase();
            } catch (IOException e) {
                throw new Error("Error copiando base de datos");
            }
        }
    }

    private boolean checkDataBase() {
        File dbFile = new File(DB_PATH);
        return dbFile.exists();
    }

    private void copyDataBase() throws IOException {
        // Abre la base de datos local como stream de entrada
        InputStream input = context.getAssets().open(DB_NAME);

        // Ruta a la base de datos recién creada
        String outFileName = DB_PATH;

        // Abre el archivo de base de datos vacío como stream de salida
        OutputStream output = new FileOutputStream(outFileName);

        // Transfiere bytes desde el archivo de entrada al de salida
        byte[] buffer = new byte[1024];
        int length;
        while ((length = input.read(buffer)) > 0) {
            output.write(buffer, 0, length);
        }

        // Cierra los streams
        output.flush();
        output.close();
        input.close();
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // No es necesario crear tablas aquí porque ya vienen en el archivo
        // Este método solo se llama si la BD no existe
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Maneja upgrades incrementalmente para migrar datos
        if (oldVersion < 2) {

            db.execSQL("ALTER TABLE User ADD COLUMN isAdmin INTEGER DEFAULT 0");
            // Opcional: Migra datos existentes (ej. establece valores por defecto)
            // db.execSQL("UPDATE User SET isAdmin = 0 WHERE isAdmin IS NULL");
        }
        // Agrega más bloques para futuras versiones
        // if (oldVersion < 3) {
        //     db.execSQL("ALTER TABLE User ADD COLUMN NuevaColumna TEXT");
        // }
        // Nota: No uses context.deleteDatabase() ni copyDataBase() aquí
    }


}