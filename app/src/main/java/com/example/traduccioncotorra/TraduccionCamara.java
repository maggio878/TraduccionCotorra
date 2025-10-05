package com.example.traduccioncotorra;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class TraduccionCamara extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_traduccion_camara, container, false);

        // Obtener argumentos si existen
        if (getArguments() != null) {
            String nombreUsuario = getArguments().getString("USUARIO");
            if (nombreUsuario != null) {
                Toast.makeText(getContext(), "Traducción por Cámara - Usuario: " + nombreUsuario,
                        Toast.LENGTH_SHORT).show();
            }
        }

        // Aquí puedes inicializar las vistas y configurar la funcionalidad de la cámara

        return view;
    }
}