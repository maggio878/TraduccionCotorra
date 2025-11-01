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
    private static final int DB_VERSION = 3;
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
            // Migración de versión 1 a 2
            db.execSQL("ALTER TABLE User ADD COLUMN isAdmin INTEGER DEFAULT 0");
        }

        if (oldVersion < 3) {
            // Migración de versión 2 a 3: Crear tablas nuevas

            // Tabla Language
            db.execSQL("CREATE TABLE IF NOT EXISTS Language (" +
                    "Language_Id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "Name TEXT NOT NULL, " +
                    "Code TEXT NOT NULL UNIQUE, " +
                    "IsActive INTEGER DEFAULT 1" +
                    ")");

            // Tabla TranslationType
            db.execSQL("CREATE TABLE IF NOT EXISTS TranslationType (" +
                    "IdTypeTranslation INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "Name TEXT NOT NULL" +
                    ")");

            // Tabla Category
            db.execSQL("CREATE TABLE IF NOT EXISTS Category (" +
                    "CategoryId INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "Name TEXT NOT NULL, " +
                    "Description TEXT" +
                    ")");

            // Tabla TranslationHistory
            db.execSQL("CREATE TABLE IF NOT EXISTS TranslationHistory (" +
                    "HistoryId INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "UserId INTEGER NOT NULL, " +
                    "TextoOriginal TEXT NOT NULL, " +
                    "TextoTraducido TEXT NOT NULL, " +
                    "IdiomaOrigen TEXT NOT NULL, " +
                    "IdiomaDestino TEXT NOT NULL, " +
                    "FechaTraduccion TEXT DEFAULT CURRENT_TIMESTAMP, " +
                    "FOREIGN KEY (UserId) REFERENCES User(UserId)" +
                    ")");

            // Tabla UserConfiguration
            db.execSQL("CREATE TABLE IF NOT EXISTS UserConfiguration (" +
                    "ConfigurationId INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "UserId INTEGER NOT NULL UNIQUE, " +
                    "PrimaryLanguageId INTEGER NOT NULL, " +
                    "Theme TEXT DEFAULT 'light', " +
                    "Sounds_Enable INTEGER DEFAULT 1, " +
                    "OfflineEnable INTEGER DEFAULT 0, " +
                    "NotificationsEnable INTEGER DEFAULT 1, " +
                    "FontSize INTEGER DEFAULT 14, " +
                    "LastUpdated TEXT DEFAULT CURRENT_TIMESTAMP, " +
                    "FOREIGN KEY (UserId) REFERENCES User(UserId), " +
                    "FOREIGN KEY (PrimaryLanguageId) REFERENCES Language(Language_Id)" +
                    ")");


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

        // Agrega más bloques para futuras versiones
        // if (oldVersion < 4) {
        //     db.execSQL("ALTER TABLE TranslationHistory ADD COLUMN NuevaColumna TEXT");
        // }
    }


}