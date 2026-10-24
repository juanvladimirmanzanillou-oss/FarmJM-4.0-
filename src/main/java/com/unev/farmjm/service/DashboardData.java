package com.unev.farmjm.service;

import com.unev.farmjm.dao.DashboardDAO;

import java.util.List;
import java.util.Map;

/** Datos ya calculados que muestra el dashboard (se construyen en un hilo en segundo plano). */
public record DashboardData(String subtitulo,
                            List<Kpi> kpis,
                            String tituloBarras, Map<String, Double> barras,
                            String tituloPastel, Map<String, Integer> pastel,
                            List<DashboardDAO.Movimiento> movimientos) {

    /** Indicador. {@code nivel}: normal, ok, warn o bad (define el color de la tarjeta). */
    public record Kpi(String etiqueta, String valor, String nivel) { }
}
