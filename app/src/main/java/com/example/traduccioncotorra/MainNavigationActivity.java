package com.example.traduccioncotorra;

import android.os.Bundle;
import android.view.MenuItem;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainNavigationActivity extends AppCompatActivity {

    private String nombreUsuario;
    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Obtener el nombre de usuario del Intent
        nombreUsuario = getIntent().getStringExtra("USUARIO");

        // Inicializar el Bottom Navigation View
        bottomNavigationView = findViewById(R.id.bottom_navigation);

        // Configurar el listener para la navegación
        bottomNavigationView.setOnNavigationItemSelectedListener(navListener);

        // Cargar el fragment de traducción de texto por defecto (solo si es la primera vez)
        if (savedInstanceState == null) {
            cargarFragment(new TraduccionTexto(), "TRADUCCION_TEXTO");
        }
    }

    // Listener para el Bottom Navigation
    private BottomNavigationView.OnNavigationItemSelectedListener navListener =
            new BottomNavigationView.OnNavigationItemSelectedListener() {
                @Override
                public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                    Fragment fragmentSeleccionado = null;
                    String tag = "";

                    int itemId = item.getItemId();

                    if (itemId == R.id.nav_traduccion_texto) {
                        fragmentSeleccionado = new TraduccionTexto();
                        tag = "TRADUCCION_TEXTO";
                    } else if (itemId == R.id.nav_traduccion_camara) {
                        fragmentSeleccionado = new TraduccionCamara();
                        tag = "TRADUCCION_CAMARA";
                    } else if (itemId == R.id.nav_traduccion_documento) {
                        fragmentSeleccionado = new TraduccionDocumento();
                        tag = "TRADUCCION_DOCUMENTO";
                    } else if (itemId == R.id.nav_diccionario) {
                        fragmentSeleccionado = new Diccionario();
                        tag = "DICCIONARIO";
                    } else if (itemId == R.id.nav_favoritos) {
                        fragmentSeleccionado = new Guardado();
                        tag = "FAVORITOS";
                    }

                    if (fragmentSeleccionado != null) {
                        cargarFragment(fragmentSeleccionado, tag);
                    }

                    return true;
                }
            };

    // Método para cargar fragments
    private void cargarFragment(Fragment fragment, String tag) {
        // Pasar el nombre de usuario al fragment
        Bundle bundle = new Bundle();
        bundle.putString("USUARIO", nombreUsuario);
        fragment.setArguments(bundle);

        // Cargar el fragment
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, fragment, tag);
        transaction.commit();
    }

    // Método público para cambiar de fragment programáticamente
    public void cambiarFragment(Fragment nuevoFragment, String tag) {
        Bundle bundle = new Bundle();
        bundle.putString("USUARIO", nombreUsuario);
        nuevoFragment.setArguments(bundle);

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, nuevoFragment, tag);
        transaction.addToBackStack(null);
        transaction.commit();
    }

//    @Override
//    public void onBackPressed() {
//        // Si hay fragments en el back stack, hacer pop
//        if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
//            getSupportFragmentManager().popBackStack();
//        } else {
//            // Si no hay fragments en el stack, comportamiento normal
//            super.onBackPressed();
//        }
//    }
}