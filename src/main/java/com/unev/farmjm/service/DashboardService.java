package com.unev.farmjm.service;

import com.unev.farmjm.dao.DashboardDAO;
import com.unev.farmjm.model.Usuario;
import com.unev.farmjm.security.Accion;
import com.unev.farmjm.security.Modulo;
import com.unev.farmjm.security.Permisos;
import com.unev.farmjm.service.DashboardData.Kpi;
import com.unev.farmjm.util.Fmt;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Calcula los indicadores del dashboard según el rol de quien inicia sesión. */
public class DashboardService {

    private final DashboardDAO dao = new DashboardDAO();

    public DashboardData cargar(Usuario u) {
        Permisos.exigir(u, Modulo.DASHBOARD, Accion.VER);
        String rol = u.getRol().getNombre();
        boolean propio = !Permisos.ADMINISTRADOR.equals(rol) && !Permisos.SUPERVISOR.equals(rol);
        Integer filtro = propio ? u.getIdUsuario() : null;
        try {
            List<Kpi> k = new ArrayList<>();
            int bajo = dao.stockBajo();
            int vencer = dao.lotesPorVencer();
            String subtitulo;
            if (Permisos.ADMINISTRADOR.equals(rol)) {
                subtitulo = "Vista de administración: ventas, inventario, usuarios y seguridad";
                int fallidos = dao.accesosFallidos24h();
                k.add(new Kpi("Ventas del mes", Fmt.dinero(dao.ventasMes(null)), "ok"));
                k.add(new Kpi("Facturas de hoy", String.valueOf(dao.facturasHoy(null)), "normal"));
                k.add(new Kpi("Medicamentos activos", String.valueOf(dao.medicamentosActivos()), "normal"));
                k.add(new Kpi("Stock bajo (≤ 10)", String.valueOf(bajo), bajo > 0 ? "warn" : "ok"));
                k.add(new Kpi("Lotes por vencer (≤ 90 d)", String.valueOf(vencer), vencer > 0 ? "warn" : "ok"));
                k.add(new Kpi("Usuarios activos", String.valueOf(dao.usuariosActivos()), "normal"));
                k.add(new Kpi("Accesos fallidos (24 h)", String.valueOf(fallidos), fallidos > 0 ? "bad" : "ok"));
            } else if (Permisos.SUPERVISOR.equals(rol)) {
                subtitulo = "Vista de supervisión: ventas del negocio e inventario crítico";
                k.add(new Kpi("Ventas del mes", Fmt.dinero(dao.ventasMes(null)), "ok"));
                k.add(new Kpi("Ventas de hoy", Fmt.dinero(dao.ventasHoy(null)), "normal"));
                k.add(new Kpi("Facturas de hoy", String.valueOf(dao.facturasHoy(null)), "normal"));
                k.add(new Kpi("Stock bajo (≤ 10)", String.valueOf(bajo), bajo > 0 ? "warn" : "ok"));
                k.add(new Kpi("Lotes por vencer (≤ 90 d)", String.valueOf(vencer), vencer > 0 ? "warn" : "ok"));
                k.add(new Kpi("Clientes activos", String.valueOf(dao.clientesActivos()), "normal"));
            } else {
                subtitulo = "Vista de mostrador: sus ventas y los productos que necesitan atención";
                k.add(new Kpi("Mis ventas de hoy", Fmt.dinero(dao.ventasHoy(filtro)), "ok"));
                k.add(new Kpi("Mis facturas de hoy", String.valueOf(dao.facturasHoy(filtro)), "normal"));
                k.add(new Kpi("Mis ventas del mes", Fmt.dinero(dao.ventasMes(filtro)), "normal"));
                k.add(new Kpi("Medicamentos disponibles", String.valueOf(dao.medicamentosActivos()), "normal"));
                k.add(new Kpi("Stock bajo (≤ 10)", String.valueOf(bajo), bajo > 0 ? "warn" : "ok"));
            }

            DateTimeFormatter f = DateTimeFormatter.ofPattern("EEE dd", new Locale("es", "DO"));
            Map<String, Double> barras = new LinkedHashMap<>();
            for (Map.Entry<LocalDate, Double> e : dao.ventasUltimos7Dias(filtro).entrySet()) {
                barras.put(e.getKey().format(f), e.getValue());
            }
            String alcance = propio ? "mis ventas" : "ventas";
            return new DashboardData(subtitulo, k,
                    "RD$ " + alcance + " de los últimos 7 días", barras,
                    "Unidades vendidas por categoría (30 días)", dao.unidadesPorCategoria(filtro),
                    dao.movimientos(filtro, 8));
        } catch (SQLException e) {
            throw Errores.bd("cargar el dashboard", e);
        }
    }
}
