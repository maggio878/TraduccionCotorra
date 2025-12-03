package com.example.traduccioncotorra;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.traduccioncotorra.DB.ManagerDB;
import com.example.traduccioncotorra.DB.UserDAO;
import com.example.traduccioncotorra.Models.Usuario;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.example.traduccioncotorra.DB.UserConfigurationDAO;


public class MainActivity extends AppCompatActivity {

    private TextInputLayout tilUsuario;
    private TextInputLayout tilPassword;
    private TextInputEditText etUsuario;
    private TextInputEditText etPassword;
    private MaterialButton btnLogin;
    private TextView tvRegistrarse;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        probarConexionBD();
        verificarTablaUser();

        // Configurar edge to edge
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Inicializar vistas
        inicializarVistas();

        // Configurar listeners
        configurarListeners();
    }

    private void inicializarVistas() {
        tilUsuario = findViewById(R.id.til_usuario);
        tilPassword = findViewById(R.id.til_password);
        etUsuario = findViewById(R.id.et_usuario);
        etPassword = findViewById(R.id.et_password);
        btnLogin = findViewById(R.id.btn_login);
        tvRegistrarse = findViewById(R.id.tv_registrarse);
    }

    private void configurarListeners() {
        // Listener del botón de login
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                intentarIniciarSesion();
            }
        });

        // Listener del TextView "Regístrate"
        tvRegistrarse.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, RegistroActivity.class);
                startActivity(intent);
            }
        });

        // Limpiar errores cuando el usuario empieza a escribir
        etUsuario.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                tilUsuario.setError(null);
            }
        });

        etPassword.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                tilPassword.setError(null);
            }
        });
    }

    private void intentarIniciarSesion() {
        // Obtener valores de los campos
        String usuario = etUsuario.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        // Limpiar errores previos
        tilUsuario.setError(null);
        tilPassword.setError(null);

        // Validar campos vacíos
        boolean esValido = true;

        if (usuario.isEmpty()) {
            tilUsuario.setError("El usuario es requerido");
            esValido = false;
        }

        if (password.isEmpty()) {
            tilPassword.setError("La contraseña es requerida");
            esValido = false;
        }

        // Si hay campos vacíos, no continuar
        if (!esValido) {
            Toast.makeText(this, "Por favor completa todos los campos",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        // Validar credenciales con la base de datos
        if (validarCredenciales(usuario, password)) {
            // Login exitoso
            Toast.makeText(this, "¡Bienvenido " + usuario + "!",
                    Toast.LENGTH_SHORT).show();

            // Ir a la siguiente actividad/fragment
            Intent intent = new Intent(MainActivity.this, MainNavigationActivity.class);
            intent.putExtra("USUARIO", usuario);
            startActivity(intent);
            finish();

        } else {
            // Login fallido
            tilPassword.setError("Usuario o contraseña incorrectos");
            Toast.makeText(this, "Credenciales inválidas",
                    Toast.LENGTH_LONG).show();
        }
    }

    private boolean validarCredenciales(String usuario, String password) {
        try {
            UserDAO userDAO = new UserDAO(this);
            Usuario user = userDAO.validarLogin(usuario, password);

            if (user != null) {
                Log.d("LOGIN", "Usuario válido: " + user.getFullName());

                // ⭐ NUEVO: Guardar el userId en SharedPreferences
                userDAO.guardarUserIdActual(this, user.getUserId());

                // ⭐ NUEVO: Crear configuración por defecto si no existe
                UserConfigurationDAO configDAO = new UserConfigurationDAO(this);
                if (!configDAO.tieneConfiguracion(user.getUserId())) {
                    configDAO.crearConfiguracionPorDefecto(user.getUserId(), 1); // 1 = Español
                    Log.d("LOGIN", "Configuración por defecto creada para el usuario");
                }
                return true;
            }

            Log.d("LOGIN", "Credenciales inválidas");
            return false;

        } catch (Exception e) {
            Log.e("LOGIN", "Error al validar credenciales", e);
       //     Toast.makeText(this, "Error al conectar con la base de datos", Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    // ══════════════════════════════════════════════════════════════════
    // MÉTODOS DE PRUEBA - Borrar o comentar después de verificar
    // ══════════════════════════════════════════════════════════════════

    private void probarConexionBD() {
        try {
            ManagerDB manager = new ManagerDB(this);
            manager.AbrirConexion();

          //  Toast.makeText(this, "✅ BD conectada correctamente", Toast.LENGTH_SHORT).show();
            Log.d("TEST_BD", "✅ Conexión exitosa a la base de datos");

            manager.CerrarConexion();

        } catch (Exception e) {
       //     Toast.makeText(this, "❌ Error BD: " + e.getMessage(), Toast.LENGTH_LONG).show();
            Log.e("TEST_BD", "❌ Error al conectar BD: ", e);
        }
    }

    private void verificarTablaUser() {
        try {
            ManagerDB manager = new ManagerDB(this);
            manager.AbrirConexion();

            Cursor cursor = manager.consultar("SELECT COUNT(*) FROM User", null);

            if (cursor != null && cursor.moveToFirst()) {
                int total = cursor.getInt(0);
              //  Toast.makeText(this, "✅ Tabla User OK. Total usuarios: " + total, Toast.LENGTH_LONG).show();
                Log.d("TEST_BD", "✅ Tabla User existe. Total usuarios: " + total);
                cursor.close();
            }

            manager.CerrarConexion();

        } catch (Exception e) {
      //      Toast.makeText(this, "❌ Tabla User no existe: " + e.getMessage(), Toast.LENGTH_LONG).show();
            Log.e("TEST_BD", "❌ Error al verificar tabla User: ", e);
        }
    }

    /**
     * Test 3: Inserta un usuario de prueba
     * CUIDADO: Solo ejecutar UNA VEZ, comentar después
     */
    private void probarRegistro() {
        try {
            UserDAO userDAO = new UserDAO(this);

            // Verificar si ya existe
            if (userDAO.existeUsername("test123")) {
              //  Toast.makeText(this, "⚠️ Usuario test123 ya existe", Toast.LENGTH_SHORT).show();
                Log.d("TEST_BD", "Usuario test123 ya existe en la BD");
                return;
            }

            // Insertar usuario de prueba
            long resultado = userDAO.insertarUsuario(
                    "test123",              // username
                    "test@test.com",        // email
                    "123456",               // password
                    "Usuario de Prueba"     // fullname
            );

            if (resultado != -1) {
                Toast.makeText(this, "✅ Usuario test registrado con ID: " + resultado, Toast.LENGTH_LONG).show();
                Log.d("TEST_BD", "✅ Usuario de prueba insertado con ID: " + resultado);
            } else {
                Toast.makeText(this, "❌ Error al registrar usuario de prueba", Toast.LENGTH_SHORT).show();
                Log.e("TEST_BD", "❌ Error al insertar usuario de prueba");
            }

        } catch (Exception e) {
            Toast.makeText(this, "❌ Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            Log.e("TEST_BD", "❌ Error al probar registro: ", e);
        }
    }


}