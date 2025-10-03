package com.example.traduccioncotorra;

import android.os.Bundle;
import android.view.MenuItem;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

public class PanelNavegador extends AppCompatActivity
{
    // Declarar la barra de navegación inferior
    private BottomNavigationView bottomNavigationView;

    // FragmentManager para gestionar los fragments
    private FragmentManager fragmentManager;

    // Referencias a los fragments
    private TraduccionTexto traduccionTextoFragment;
    private traduccion_documento traduccionDocumentoFragment;
    private TraduccionCamara traduccionCamaraFragment;
    private FavoritosFragment favoritosFragment;
    private DiccionarioFragment diccionarioFragment;

    // Fragment actualmente visible
    private Fragment activeFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_container);

        // Inicializar el FragmentManager
        fragmentManager = getSupportFragmentManager();

        // Inicializar la barra de navegación
        initializeBottomNavigation();

        // Inicializar los fragments
        initializeFragments();

        // Mostrar el primer fragment por defecto (Traducción de texto)
        if (savedInstanceState == null) {
            showFragment(traduccionTextoFragment);
        }
    }

    /**
     * Inicializa la barra de navegación inferior y configura sus listeners
     */
    private void initializeBottomNavigation() {
        bottomNavigationView = findViewById(R.id.bottom_navigation);

        // Configurar el listener para los clicks en los items
        bottomNavigationView.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int itemId = item.getItemId();

                // Determinar qué fragment mostrar según el item seleccionado
                if (itemId == R.id.nav_texto) {
                    showFragment(traduccionTextoFragment);
                    return true;
                } else if (itemId == R.id.nav_documento) {
                    showFragment(traduccionDocumentoFragment);
                    return true;
                } else if (itemId == R.id.nav_camara) {
                    showFragment(traduccionCamaraFragment);
                    return true;
                } else if (itemId == R.id.nav_favoritos) {
                    showFragment(favoritosFragment);
                    return true;
                } else if (itemId == R.id.nav_diccionario) {
                    showFragment(diccionarioFragment);
                    return true;
                }

                return false;
            }
        });
    }

    /**
     * Inicializa todos los fragments y los añade al FragmentManager
     */
    private void initializeFragments() {
        // Crear instancias de todos los fragments
        traduccionTextoFragment = new TraduccionTexto();
        traduccionDocumentoFragment = new traduccion_documento();
        traduccionCamaraFragment = new TraduccionCamara();
        favoritosFragment = new FavoritosFragment();
        diccionarioFragment = new DiccionarioFragment();

        // Añadir todos los fragments al contenedor, pero ocultarlos
        FragmentTransaction transaction = fragmentManager.beginTransaction();

        transaction.add(R.id.fragment_container, traduccionTextoFragment, "TEXTO");
        transaction.add(R.id.fragment_container, traduccionDocumentoFragment, "DOCUMENTO");
        transaction.add(R.id.fragment_container, traduccionCamaraFragment, "CAMARA");
        transaction.add(R.id.fragment_container, favoritosFragment, "FAVORITOS");
        transaction.add(R.id.fragment_container, diccionarioFragment, "DICCIONARIO");

        // Ocultar todos excepto el primero
        transaction.hide(traduccionDocumentoFragment);
        transaction.hide(traduccionCamaraFragment);
        transaction.hide(favoritosFragment);
        transaction.hide(diccionarioFragment);

        transaction.commit();

        // Establecer el fragment activo inicial
        activeFragment = traduccionTextoFragment;
    }

    /**
     * Muestra un fragment específico y oculta el anterior
     * Este método es más eficiente que replace() porque mantiene el estado
     *
     * @param fragment El fragment a mostrar
     */
    private void showFragment(Fragment fragment) {
        if (fragment == activeFragment) {
            return; // Ya está visible
        }

        FragmentTransaction transaction = fragmentManager.beginTransaction();

        // Ocultar el fragment actual
        transaction.hide(activeFragment);

        // Mostrar el nuevo fragment
        transaction.show(fragment);

        // Aplicar la transacción
        transaction.commit();

        // Actualizar el fragment activo
        activeFragment = fragment;
    }

    /**
     * Método para navegar programáticamente a un fragment específico
     * Útil cuando necesitas cambiar de fragment desde el código
     *
     * @param fragmentTag Tag del fragment ("TEXTO", "DOCUMENTO", etc.)
     */
    public void navigateToFragment(String fragmentTag) {
        Fragment fragment = fragmentManager.findFragmentByTag(fragmentTag);
        if (fragment != null) {
            showFragment(fragment);

            // Actualizar el item seleccionado en la barra de navegación
            switch (fragmentTag) {
                case "TEXTO":
                    bottomNavigationView.setSelectedItemId(R.id.nav_texto);
                    break;
                case "DOCUMENTO":
                    bottomNavigationView.setSelectedItemId(R.id.nav_documento);
                    break;
                case "CAMARA":
                    bottomNavigationView.setSelectedItemId(R.id.nav_camara);
                    break;
                case "FAVORITOS":
                    bottomNavigationView.setSelectedItemId(R.id.nav_favoritos);
                    break;
                case "DICCIONARIO":
                    bottomNavigationView.setSelectedItemId(R.id.nav_diccionario);
                    break;
            }
        }
    }

}
