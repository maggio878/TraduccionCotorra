package com.example.traduccioncotorra;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

public class HomeActivity extends AppCompatActivity {

    private String nombreUsuario;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.home_layout), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Obtener el nombre de usuario del Intent
        nombreUsuario = getIntent().getStringExtra("USUARIO");

        // Cargar el fragment de traducción de texto al inicio
        if (savedInstanceState == null) {
            cargarFragmentTraduccionTexto();
        }
    }

    private void cargarFragmentTraduccionTexto() {
        // Crear instancia del fragment
        Fragment fragment = new TraduccionTexto();

        // Opcional: pasar el nombre de usuario al fragment
        Bundle bundle = new Bundle();
        bundle.putString("USUARIO", nombreUsuario);
        fragment.setArguments(bundle);

        // Reemplazar el contenedor con el fragment
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, fragment);
        transaction.commit();
    }

    // Método público para cambiar de fragment desde otros lugares
    public void cambiarFragment(Fragment nuevoFragment) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, nuevoFragment);
        transaction.addToBackStack(null); // Permite volver atrás
        transaction.commit();
    }
}