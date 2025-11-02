package com.example.traduccioncotorra;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.RadioGroup;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class ReportesFragment extends Fragment {

    private ImageButton btnVolver;
    private RadioGroup radioGroupReportes;
    private RadioButton radioIdiomas;
    private RadioButton radioTipos;
    private TextView tvTituloGrafico;
    private LinearLayout containerGrafico;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Inflar el layout fragment_reportes.xml
        View view = inflater.inflate(R.layout.fragment_reportes, container, false);

        // Inicializar vistas
        inicializarVistas(view);

        // Configurar listeners
        configurarListeners();

        // Mostrar gráfico inicial (idiomas)
        mostrarGraficoIdiomas();

        return view;
    }

    private void inicializarVistas(View view) {
        btnVolver = view.findViewById(R.id.btn_volver);
        radioGroupReportes = view.findViewById(R.id.radio_group_reportes);
        radioIdiomas = view.findViewById(R.id.radio_idiomas);
        radioTipos = view.findViewById(R.id.radio_tipos);
        tvTituloGrafico = view.findViewById(R.id.tv_titulo_grafico);
        containerGrafico = view.findViewById(R.id.container_grafico);
    }

    private void configurarListeners() {
        // Botón volver
        btnVolver.setOnClickListener(v -> {
            requireActivity().onBackPressed();
        });

        // RadioGroup para cambiar entre gráficos
        radioGroupReportes.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radio_idiomas) {
                mostrarGraficoIdiomas();
            } else if (checkedId == R.id.radio_tipos) {
                mostrarGraficoTipos();
            }
        });
    }

    private void mostrarGraficoIdiomas() {
        tvTituloGrafico.setText("Idiomas más usados");
        containerGrafico.removeAllViews();

        // TODO: Aquí cargarás los datos de tu base de datos
        // Por ahora mostramos un ejemplo
        TextView placeholder = new TextView(requireContext());
        placeholder.setText("Gráfico de idiomas\n(Conectar con tu BD)");
        placeholder.setTextSize(16);
        placeholder.setPadding(20, 20, 20, 20);
        containerGrafico.addView(placeholder);
    }

    private void mostrarGraficoTipos() {
        tvTituloGrafico.setText("Tipos de traducción más usados");
        containerGrafico.removeAllViews();

        // TODO: Aquí cargarás los datos de tu base de datos
        TextView placeholder = new TextView(requireContext());
        placeholder.setText("Gráfico de tipos de traducción\n(Conectar a la BD)");
        placeholder.setTextSize(16);
        placeholder.setPadding(20, 20, 20, 20);
        containerGrafico.addView(placeholder);
    }
}