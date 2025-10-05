package com.example.traduccioncotorra;

import android.os.Bundle;
import android.view.MenuItem;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainNavigationActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;
    private String nombreUsuario;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Obtener el nombre de usuario del Intent (enviado desde el login)
        if (getIntent().hasExtra("USUARIO")) {
            nombreUsuario = getIntent().getStringExtra("USUARIO");
        }

        // Inicializar el BottomNavigationView
        bottomNavigationView = findViewById(R.id.bottom_navigation);

        // Cargar el fragment inicial (Traducción de Texto)
        if (savedInstanceState == null) {
            cargarFragment(new TraduccionTexto(), true);
        }

        // Configurar el listener para el menú inferior
        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                Fragment fragmentSeleccionado = null;

                int itemId = item.getItemId();

                if (itemId == R.id.nav_texto) {
                    fragmentSeleccionado = new TraduccionTexto();
                } else if (itemId == R.id.nav_camara) {
                    fragmentSeleccionado = new TraduccionCamara();
                } else if (itemId == R.id.nav_documento) {
                    fragmentSeleccionado = new TraduccionDocumento();
                } else if (itemId == R.id.nav_diccionario) {
                    fragmentSeleccionado = new Diccionario();
                } else if (itemId == R.id.nav_favoritos) {
                    fragmentSeleccionado = new Guardado();
                }

                // Cargar el fragment seleccionado
                if (fragmentSeleccionado != null) {
                    cargarFragment(fragmentSeleccionado, false);
                    return true;
                }

                return false;
            }
        });
    }

    private void cargarFragment(Fragment fragment, boolean esInicial) {
        // Crear un Bundle con el nombre de usuario para pasarlo al fragment
        if (nombreUsuario != null && esInicial) {
            Bundle bundle = new Bundle();
            bundle.putString("USUARIO", nombreUsuario);
            fragment.setArguments(bundle);
        }

        // Reemplazar el fragment actual con el nuevo
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, fragment);
        transaction.commit();
    }


}