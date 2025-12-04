package com.example.traduccioncotorra;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.RadioGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.traduccioncotorra.DB.HistorialDAO;
import com.example.traduccioncotorra.DB.UserDAO;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.github.mikephil.charting.formatter.PercentFormatter;

import java.util.ArrayList;
import java.util.List;

public class ReportesFragment extends Fragment {

    private ImageButton btnVolver;
    private RadioGroup radioGroupReportes;
    private PieChart pieChart;

    private HistorialDAO historialDAO;
    private UserDAO userDAO;
    private int userId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_reportes, container, false);

        // Inicializar DAOs
        historialDAO = new HistorialDAO(requireContext());
        userDAO = new UserDAO(requireContext());
        userId = userDAO.obtenerUserIdActual(requireContext());

        inicializarVistas(view);
        configurarListeners();

        // Verificar usuario
        if (userId == -1) {
            Toast.makeText(requireContext(),
                    "Error: Usuario no identificado",
                    Toast.LENGTH_SHORT).show();
            return view;
        }

        // Mostrar gráfico inicial
        mostrarGraficoIdiomas();

        return view;
    }

    private void inicializarVistas(View view) {
        btnVolver = view.findViewById(R.id.btn_volver);
        radioGroupReportes = view.findViewById(R.id.radio_group_reportes);
        pieChart = view.findViewById(R.id.pieChart);
        com.google.android.material.card.MaterialCardView cardSelector =
                view.findViewById(R.id.card_selector);
        com.google.android.material.card.MaterialCardView cardGrafico =
                view.findViewById(R.id.card_grafico);

        if (cardSelector != null) {
            cardSelector.setCardBackgroundColor(Color.WHITE);
        }

        if (cardGrafico != null) {
            cardGrafico.setCardBackgroundColor(Color.WHITE);
        }
    }

    private void configurarListeners() {
        btnVolver.setOnClickListener(v -> requireActivity().onBackPressed());

        radioGroupReportes.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radio_idiomas) {
                mostrarGraficoIdiomas();
            } else if (checkedId == R.id.radio_tipos) {
                mostrarGraficoTipos();
            }
        });
    }

    private void mostrarGraficoIdiomas() {
        // Obtener datos de la BD
        List<HistorialDAO.IdiomaEstadistica> estadisticas =
                historialDAO.obtenerIdiomasMasUsados(userId);

        // Verificar si hay datos
        if (estadisticas.isEmpty()) {
            mostrarMensajeSinDatos("No hay traducciones registradas aún.\n\n¡Empieza a traducir para ver tus estadísticas!");
            return;
        }

        // Convertir a PieEntry
        ArrayList<PieEntry> entries = new ArrayList<>();
        for (HistorialDAO.IdiomaEstadistica stat : estadisticas) {
            entries.add(new PieEntry(stat.cantidad, stat.nombreIdioma));
        }

        // Configurar dataset
        PieDataSet dataSet = new PieDataSet(entries, "Idiomas más usados");
        dataSet.setColors(ColorTemplate.MATERIAL_COLORS);
        dataSet.setValueTextSize(14f);
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setSliceSpace(3f);
        dataSet.setValueFormatter(new PercentFormatter(pieChart));

        PieData data = new PieData(dataSet);

        // Configurar gráfico
        pieChart.setData(data);
        pieChart.setUsePercentValues(true);
        pieChart.getDescription().setEnabled(false);
        pieChart.setDrawHoleEnabled(true);
        pieChart.setHoleColor(Color.TRANSPARENT);
        pieChart.setHoleRadius(35f);
        pieChart.setTransparentCircleRadius(40f);
        pieChart.setCenterText("Idiomas\nmás usados");
        pieChart.setCenterTextSize(16f);
        pieChart.setEntryLabelTextSize(12f);
        pieChart.setEntryLabelColor(Color.BLACK);
        pieChart.getLegend().setEnabled(true);
        pieChart.animateY(1400);
        pieChart.invalidate();
    }

    private void mostrarGraficoTipos() {
        // Obtener datos de la BD
        List<HistorialDAO.TipoEstadistica> estadisticas =
                historialDAO.obtenerTiposMasUsados(userId);

        if (estadisticas.isEmpty()) {
            mostrarMensajeSinDatos("No hay traducciones registradas aún.\n\n¡Empieza a traducir para ver tus estadísticas!");
            return;
        }

        ArrayList<PieEntry> entries = new ArrayList<>();
        for (HistorialDAO.TipoEstadistica stat : estadisticas) {
            entries.add(new PieEntry(stat.cantidad, stat.tipoTraduccion));
        }

        PieDataSet dataSet = new PieDataSet(entries, "Tipos de traducción");
        dataSet.setColors(ColorTemplate.COLORFUL_COLORS);
        dataSet.setValueTextSize(14f);
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setSliceSpace(3f);
        dataSet.setValueFormatter(new PercentFormatter(pieChart));

        PieData data = new PieData(dataSet);

        pieChart.setData(data);
        pieChart.setUsePercentValues(true);
        pieChart.getDescription().setEnabled(false);
        pieChart.setDrawHoleEnabled(true);
        pieChart.setHoleColor(Color.TRANSPARENT);
        pieChart.setCenterText("Tipos de\ntraducción");
        pieChart.setCenterTextSize(16f);
        pieChart.getLegend().setEnabled(true);
        pieChart.animateY(1400);
        pieChart.invalidate();
    }

    private void mostrarMensajeSinDatos(String mensaje) {
        pieChart.clear();
        pieChart.setCenterText(mensaje);
        pieChart.setCenterTextSize(14f);
        pieChart.setCenterTextColor(Color.GRAY);
        pieChart.invalidate();
    }
}