package com.example.traduccioncotorra.DB;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.example.traduccioncotorra.Models.Usuario;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class UserDAO {
    private ManagerDB managerDB;
    private static final String TABLE_NAME = "User";

    public UserDAO(Context context) {
        managerDB = new ManagerDB(context);
    }

    /**
     * Inserta un nuevo usuario en la base de datos
     * @param user Objeto User con los datos del usuario
     * @return El ID del usuario insertado, o -1 si hay error
     */
    public long insertarUsuario(Usuario user) {
        long resultado = -1;
        try {
            managerDB.AbrirConexion();

            ContentValues valores = new ContentValues();
            valores.put("Username", user.getUsername());
            valores.put("Email", user.getEmail());
            valores.put("Password", user.getPassword());
            valores.put("FullName", user.getFullName());
            valores.put("IsActive", 1);
            // CreatedDate se asigna automáticamente con DEFAULT CURRENT_TIMESTAMP

            resultado = managerDB.insertar(TABLE_NAME, valores);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            managerDB.CerrarConexion();
        }
        return resultado;
    }

    /**
     * Inserta un usuario con parámetros individuales
     */
    public long insertarUsuario(String username, String email, String password, String fullName) {
        Usuario user = new Usuario(username, email, password, fullName);
        return insertarUsuario(user);
    }

    /**
     * Valida las credenciales de login
     * @param username Nombre de usuario
     * @param password Contraseña
     * @return User si las credenciales son válidas, null en caso contrario
     */
    public Usuario validarLogin(String username, String password) {
        Usuario user = null;
        Cursor cursor = null;

        try {
            managerDB.AbrirConexion();

            String query = "SELECT * FROM " + TABLE_NAME +
                    " WHERE Username = ? AND Password = ? AND IsActive = 1";
            String[] args = {username, password};

            cursor = managerDB.consultar(query, args);

            if (cursor != null && cursor.moveToFirst()) {
                user = new Usuario();
                user.setUserId(cursor.getInt(cursor.getColumnIndexOrThrow("UserId")));
                user.setUsername(cursor.getString(cursor.getColumnIndexOrThrow("Username")));
                user.setEmail(cursor.getString(cursor.getColumnIndexOrThrow("Email")));
                user.setPassword(cursor.getString(cursor.getColumnIndexOrThrow("Password")));
                user.setFullName(cursor.getString(cursor.getColumnIndexOrThrow("FullName")));
                user.setCreatedDate(cursor.getString(cursor.getColumnIndexOrThrow("CreatedDate")));
                user.setLastLogin(cursor.getString(cursor.getColumnIndexOrThrow("LastLogin")));
                user.setIsActive(cursor.getInt(cursor.getColumnIndexOrThrow("IsActive")));

                // Actualizar LastLogin
                actualizarUltimoLogin(user.getUserId());
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            managerDB.CerrarConexion();
        }

        return user;
    }

    /**
     * Verifica si un username ya existe
     */
    public boolean existeUsername(String username) {
        boolean existe = false;
        Cursor cursor = null;

        try {
            managerDB.AbrirConexion();

            String query = "SELECT COUNT(*) FROM " + TABLE_NAME + " WHERE Username = ?";
            cursor = managerDB.consultar(query, new String[]{username});

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
     * Verifica si un email ya existe
     */
    public boolean existeEmail(String email) {
        boolean existe = false;
        Cursor cursor = null;

        try {
            managerDB.AbrirConexion();

            String query = "SELECT COUNT(*) FROM " + TABLE_NAME + " WHERE Email = ?";
            cursor = managerDB.consultar(query, new String[]{email});

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
     * Actualiza la fecha del último login
     */
    private void actualizarUltimoLogin(int userId) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            String fechaActual = sdf.format(new Date());

            ContentValues valores = new ContentValues();
            valores.put("LastLogin", fechaActual);

            managerDB.actualizar(TABLE_NAME, valores, "UserId = ?",
                    new String[]{String.valueOf(userId)});

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Obtiene un usuario por su ID
     */
    public Usuario obtenerUsuarioPorId(int userId) {
        Usuario user = null;
        Cursor cursor = null;

        try {
            managerDB.AbrirConexion();

            String query = "SELECT * FROM " + TABLE_NAME + " WHERE UserId = ?";
            cursor = managerDB.consultar(query, new String[]{String.valueOf(userId)});

            if (cursor != null && cursor.moveToFirst()) {
                user = new Usuario();
                user.setUserId(cursor.getInt(cursor.getColumnIndexOrThrow("UserId")));
                user.setUsername(cursor.getString(cursor.getColumnIndexOrThrow("Username")));
                user.setEmail(cursor.getString(cursor.getColumnIndexOrThrow("Email")));
                user.setPassword(cursor.getString(cursor.getColumnIndexOrThrow("Password")));
                user.setFullName(cursor.getString(cursor.getColumnIndexOrThrow("FullName")));
                user.setCreatedDate(cursor.getString(cursor.getColumnIndexOrThrow("CreatedDate")));
                user.setLastLogin(cursor.getString(cursor.getColumnIndexOrThrow("LastLogin")));
                user.setIsActive(cursor.getInt(cursor.getColumnIndexOrThrow("IsActive")));
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            managerDB.CerrarConexion();
        }

        return user;
    }

    /**
     * Actualiza los datos de un usuario
     */
    public int actualizarUsuario(Usuario user) {
        int filasActualizadas = 0;

        try {
            managerDB.AbrirConexion();

            ContentValues valores = new ContentValues();
            valores.put("Username", user.getUsername());
            valores.put("Email", user.getEmail());
            valores.put("FullName", user.getFullName());

            filasActualizadas = managerDB.actualizar(TABLE_NAME, valores,
                    "UserId = ?",
                    new String[]{String.valueOf(user.getUserId())});

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            managerDB.CerrarConexion();
        }

        return filasActualizadas;
    }

    /**
     * Desactiva un usuario (no lo elimina)
     */
    public int desactivarUsuario(int userId) {
        int filasActualizadas = 0;

        try {
            managerDB.AbrirConexion();

            ContentValues valores = new ContentValues();
            valores.put("IsActive", 0);

            filasActualizadas = managerDB.actualizar(TABLE_NAME, valores,
                    "UserId = ?",
                    new String[]{String.valueOf(userId)});

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            managerDB.CerrarConexion();
        }

        return filasActualizadas;
    }
}