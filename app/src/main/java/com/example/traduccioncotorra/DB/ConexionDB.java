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
    private static final int DB_VERSION = 4;
    private final Context context;
    private String DB_PATH;

    public ConexionDB(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
        this.context = context;
        DB_PATH = context.getDatabasePath(DB_NAME).getPath();
    }

    public void createDataBase() throws IOException {
        boolean dbExist = checkDataBase();

        if (!dbExist) {
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
        InputStream input = context.getAssets().open(DB_NAME);
        String outFileName = DB_PATH;
        OutputStream output = new FileOutputStream(outFileName);

        byte[] buffer = new byte[1024];
        int length;
        while ((length = input.read(buffer)) > 0) {
            output.write(buffer, 0, length);
        }

        output.flush();
        output.close();
        input.close();
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // No es necesario crear tablas aquí porque ya vienen en el archivo
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Migración incremental

        if (oldVersion < 2) {
            // Migración de versión 1 a 2
            db.execSQL("ALTER TABLE User ADD COLUMN isAdmin INTEGER DEFAULT 0");
        }

        if (oldVersion < 3) {

            // Insertar datos iniciales en Language
            db.execSQL("INSERT INTO Language (Name, Code, IsActive) VALUES " +
                    "('Español', 'es', 1), " +
                    "('Inglés', 'en', 1), " +
                    "('Francés', 'fr', 1), " +
                    "('Alemán', 'de', 1), " +
                    "('Italiano', 'it', 1), " +
                    "('Portugués', 'pt', 1)");

            // Insertar datos iniciales en TranslationType
            db.execSQL("INSERT INTO TranslationType (Name) VALUES " +
                    "('Texto'), ('Cámara'), ('Documento')");

            // Insertar datos iniciales en Category
            db.execSQL("INSERT INTO Category (Name, Description) VALUES " +
                    "('Saludos', 'Expresiones de saludo y despedida'), " +
                    "('Compras', 'Vocabulario para ir de compras'), " +
                    "('Vida Diaria', 'Palabras y frases cotidianas'), " +
                    "('Restaurante', 'Vocabulario para restaurantes y comida'), " +
                    "('Viajes', 'Frases útiles para viajar'), " +
                    "('Emergencias', 'Vocabulario para situaciones de emergencia')");
        }


        if (oldVersion < 4) {
            // Agregar columna ApiCode a Language
            db.execSQL("ALTER TABLE Language ADD COLUMN ApiCode TEXT");

            db.execSQL("UPDATE Language SET ApiCode = Code WHERE Code IS NOT NULL");
        }
    }
}