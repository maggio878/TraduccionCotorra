package com.example.traduccioncotorra;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.example.traduccioncotorra.DB.UserDAO;
import com.example.traduccioncotorra.Models.Usuario;

public class RegistroActivity extends AppCompatActivity {

    private TextInputLayout tilNombre;
    private TextInputLayout tilEmail;
    private TextInputLayout tilUsuario;
    private TextInputLayout tilPassword;
    private TextInputLayout tilConfirmarPassword;

    private TextInputEditText etNombre;
    private TextInputEditText etEmail;
    private TextInputEditText etUsuario;
    private TextInputEditText etPassword;
    private TextInputEditText etConfirmarPassword;

    private MaterialButton btnRegistrarse;
    private TextView tvIrLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registro);

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
        tilNombre = findViewById(R.id.til_nombre);
        tilEmail = findViewById(R.id.til_email);
        tilUsuario = findViewById(R.id.til_usuario);
        tilPassword = findViewById(R.id.til_password);
        tilConfirmarPassword = findViewById(R.id.til_confirmar_password);

        etNombre = findViewById(R.id.et_nombre);
        etEmail = findViewById(R.id.et_email);
        etUsuario = findViewById(R.id.et_usuario);
        etPassword = findViewById(R.id.et_password);
        etConfirmarPassword = findViewById(R.id.et_confirmar_password);

        btnRegistrarse = findViewById(R.id.btn_registrarse);
        tvIrLogin = findViewById(R.id.tv_ir_login);
    }

    private void configurarListeners() {
        // Listener del botón registrarse
        btnRegistrarse.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                intentarRegistrar();
            }
        });

        // Listener del TextView "Inicia sesión"
        tvIrLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Volver al login
                finish(); // Cierra esta activity y vuelve a la anterior
            }
        });

        // Limpiar errores cuando el usuario empieza a escribir
        etNombre.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) tilNombre.setError(null);
        });

        etEmail.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) tilEmail.setError(null);
        });

        etUsuario.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) tilUsuario.setError(null);
        });

        etPassword.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) tilPassword.setError(null);
        });

        etConfirmarPassword.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) tilConfirmarPassword.setError(null);
        });
    }

    private void intentarRegistrar() {
        // Obtener valores de los campos
        String nombre = etNombre.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String usuario = etUsuario.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmarPassword = etConfirmarPassword.getText().toString().trim();

        // Limpiar errores previos
        limpiarErrores();

        // Validar campos
        boolean esValido = true;

        // Validar nombre
        if (nombre.isEmpty()) {
            tilNombre.setError("El nombre es requerido");
            esValido = false;
        } else if (nombre.length() < 3) {
            tilNombre.setError("El nombre debe tener al menos 3 caracteres");
            esValido = false;
        }

        // Validar email
        if (email.isEmpty()) {
            tilEmail.setError("El email es requerido");
            esValido = false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError("Email inválido");
            esValido = false;
        }

        // Validar usuario
        if (usuario.isEmpty()) {
            tilUsuario.setError("El usuario es requerido");
            esValido = false;
        } else if (usuario.length() < 4) {
            tilUsuario.setError("El usuario debe tener al menos 4 caracteres");
            esValido = false;
        }

        // Validar contraseña
        if (password.isEmpty()) {
            tilPassword.setError("La contraseña es requerida");
            esValido = false;
        } else if (password.length() < 6) {
            tilPassword.setError("La contraseña debe tener al menos 6 caracteres");
            esValido = false;
        }

        // Validar confirmar contraseña
        if (confirmarPassword.isEmpty()) {
            tilConfirmarPassword.setError("Confirma tu contraseña");
            esValido = false;
        } else if (!password.equals(confirmarPassword)) {
            tilConfirmarPassword.setError("Las contraseñas no coinciden");
            esValido = false;
        }

        // Si no es válido, mostrar mensaje y salir
        if (!esValido) {
            Toast.makeText(this, "Por favor corrige los errores", Toast.LENGTH_SHORT).show();
            return;
        }

        // Si todo es válido, proceder con el registro
        registrarUsuario(nombre, email, usuario, password);
    }

    private void registrarUsuario(String nombre, String email, String usuario, String password) {
        // Verificar si el usuario o email ya existen
        UserDAO userDAO = new UserDAO(this);

        if (userDAO.existeUsername(usuario)) {
            tilUsuario.setError("Este nombre de usuario ya está en uso");
            Toast.makeText(this, "El usuario ya existe", Toast.LENGTH_SHORT).show();
            return;
        }

        if (userDAO.existeEmail(email)) {
            tilEmail.setError("Este email ya está registrado");
            Toast.makeText(this, "El email ya está en uso", Toast.LENGTH_SHORT).show();
            return;
        }

        // Insertar el nuevo usuario en la base de datos
        long resultado = userDAO.insertarUsuario(usuario, email, password, nombre);

        if (resultado != -1) {
            // Registro exitoso
            Toast.makeText(this, "¡Registro exitoso! Bienvenido " + nombre,
                    Toast.LENGTH_LONG).show();

            // Volver al login después del registro exitoso
            Intent intent = new Intent(RegistroActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        } else {
            // Error al registrar
            Toast.makeText(this, "Error al registrar. Intenta de nuevo",
                    Toast.LENGTH_LONG).show();
        }
    }

    private void limpiarErrores() {
        tilNombre.setError(null);
        tilEmail.setError(null);
        tilUsuario.setError(null);
        tilPassword.setError(null);
        tilConfirmarPassword.setError(null);
    }
}