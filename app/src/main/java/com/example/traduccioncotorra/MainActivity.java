package com.example.traduccioncotorra;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class MainActivity extends AppCompatActivity {

    private TextInputLayout tilUsuario;
    private TextInputLayout tilPassword;
    private TextInputEditText etUsuario;
    private TextInputEditText etPassword;
    private MaterialButton btnLogin;

    // Credenciales simuladas (en lugar de base de datos)
    private static final String USUARIO_VALIDO = "cotorra";
    private static final String PASSWORD_VALIDA = "1234";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

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
    }

    private void configurarListeners() {
        // Listener del botón de login
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                intentarIniciarSesion();
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

        // Validar credenciales (simuladas)
        if (validarCredenciales(usuario, password)) {
            // Login exitoso
            Toast.makeText(this, "¡Bienvenido " + usuario + "!",
                    Toast.LENGTH_SHORT).show();

            // Ir a la siguiente actividad/fragment
            Intent intent = new Intent(MainActivity.this, MainNavigationActivity.class);
            intent.putExtra("USUARIO", usuario); // Enviar el nombre de usuario
            startActivity(intent);
            finish(); // Cerrar el login para que no pueda volver con el botón atrás

        } else {
            // Login fallido
            tilPassword.setError("Usuario o contraseña incorrectos");
            Toast.makeText(this, "Credenciales inválidas",
                    Toast.LENGTH_LONG).show();
        }
    }

    private boolean validarCredenciales(String usuario, String password) {
        // Validación simulada (sin base de datos)
        // En producción, esto se haría contra una base de datos
        return usuario.equals(USUARIO_VALIDO) && password.equals(PASSWORD_VALIDA);
    }
}