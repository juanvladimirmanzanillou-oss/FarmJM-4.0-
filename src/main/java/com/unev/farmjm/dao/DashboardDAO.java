package com.unev.farmjm.dao;

import com.unev.farmjm.db.ConexionBD;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Consultas del dashboard. Cuando {@code idUsuario} no es null, los datos se limitan a ese usuario
 * (vista del Farmacéutico); si es null se calculan para todo el sistema (Administrador y Supervisor).
 */
public class DashboardDAO {

    public record Movimiento(LocalDateTime fecha, String usuario, String modulo, String accion) { }

    private static final String FILTRO = " AND f.id_usuario = ?";

    public double ventasMes(Integer idUsuario) throws SQLException {
        return numero("SELECT COALESCE(SUM(f.total),0) FROM factura f WHERE YEAR(f.fecha)=YEAR(CURDATE()) " +
                "AND MONTH(f.fecha)=MONTH(CURDATE())" + (idUsuario == null ? "" : FILTRO), idUsuario);
    }

    public double ventasHoy(Integer idUsuario) throws SQLException {
        return numero("SELECT COALESCE(SUM(f.total),0) FROM factura f WHERE DATE(f.fecha)=CURDATE()"
                + (idUsuario == null ? "" : FILTRO), idUsuario);
    }

    public int facturasHoy(Integer idUsuario) throws SQLException {
        return (int) numero("SELECT COUNT(*) FROM factura f WHERE DATE(f.fecha)=CURDATE()"
                + (idUsuario == null ? "" : FILTRO), idUsuario);
    }

    public int medicamentosActivos() throws SQLException {
        return (int) numero("SELECT COUNT(*) FROM producto p JOIN medicamento m ON m.id_producto=p.id_producto WHERE p.activo=1", null);
    }

    public int stockBajo() throws SQLException {
        return (int) numero("SELECT COUNT(*) FROM producto p JOIN medicamento m ON m.id_producto=p.id_producto " +
                "WHERE p.activo=1 AND p.stock <= 10", null);
    }

    public int lotesPorVencer() throws SQLException {
        return (int) numero("SELECT COUNT(*) FROM lote l JOIN producto p ON p.id_producto=l.id_producto " +
                "WHERE p.activo=1 AND l.cantidad > 0 AND l.fecha_vencimiento <= DATE_ADD(CURDATE(), INTERVAL 90 DAY)", null);
    }

    public int usuariosActivos() throws SQLException {
        return (int) numero("SELECT COUNT(*) FROM usuario WHERE activo = 1", null);
    }

    public int clientesActivos() throws SQLException {
        return (int) numero("SELECT COUNT(*) FROM cliente WHERE activo = 1", null);
    }

    public int accesosFallidos24h() throws SQLException {
        return (int) numero("SELECT COUNT(*) FROM bitacora_acceso WHERE resultado='FALLIDO' " +
                "AND fecha >= DATE_SUB(NOW(), INTERVAL 1 DAY)", null);
    }

    /** Ventas de los últimos 7 días (incluye los días sin ventas con valor 0). */
    public Map<LocalDate, Double> ventasUltimos7Dias(Integer idUsuario) throws SQLException {
        Map<LocalDate, Double> m = new LinkedHashMap<>();
        for (int i = 6; i >= 0; i--) m.put(LocalDate.now().minusDays(i), 0.0);
        String sql = "SELECT DATE(f.fecha), SUM(f.total) FROM factura f " +
                "WHERE f.fecha >= DATE_SUB(CURDATE(), INTERVAL 6 DAY)" + (idUsuario == null ? "" : FILTRO) +
                " GROUP BY DATE(f.fecha)";
        try (Connection con = ConexionBD.getConexion(); PreparedStatement ps = con.prepareStatement(sql)) {
            if (idUsuario != null) ps.setInt(1, idUsuario);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) m.put(rs.getDate(1).toLocalDate(), rs.getDouble(2));
            }
        }
        return m;
    }

    /** Unidades vendidas por categoría en los últimos 30 días. */
    public Map<String, Integer> unidadesPorCategoria(Integer idUsuario) throws SQLException {
        Map<String, Integer> m = new LinkedHashMap<>();
        String sql = "SELECT c.nombre, SUM(d.cantidad) AS unidades FROM detalle_factura d " +
                "JOIN factura f ON f.id_factura = d.id_factura " +
                "JOIN producto p ON p.id_producto = d.id_producto " +
                "JOIN categoria c ON c.id_categoria = p.id_categoria " +
                "WHERE f.fecha >= DATE_SUB(CURDATE(), INTERVAL 30 DAY)" + (idUsuario == null ? "" : FILTRO) +
                " GROUP BY c.nombre ORDER BY unidades DESC";
        try (Connection con = ConexionBD.getConexion(); PreparedStatement ps = con.prepareStatement(sql)) {
            if (idUsuario != null) ps.setInt(1, idUsuario);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) m.put(rs.getString(1), rs.getInt(2));
            }
        }
        return m;
    }

    public List<Movimiento> movimientos(Integer idUsuario, int limite) throws SQLException {
        String sql = "SELECT l.fecha, u.username, l.modulo, l.accion FROM log_actividad l " +
                "JOIN usuario u ON u.id_usuario = l.id_usuario " +
                (idUsuario == null ? "" : "WHERE l.id_usuario = ? ") + "ORDER BY l.fecha DESC, l.id_log DESC LIMIT ?";
        List<Movimiento> l = new ArrayList<>();
        try (Connection con = ConexionBD.getConexion(); PreparedStatement ps = con.prepareStatement(sql)) {
            int i = 1;
            if (idUsuario != null) ps.setInt(i++, idUsuario);
            ps.setInt(i, limite);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    l.add(new Movimiento(rs.getTimestamp(1).toLocalDateTime(), rs.getString(2), rs.getString(3), rs.getString(4)));
                }
            }
        }
        return l;
    }

    private double numero(String sql, Integer idUsuario) throws SQLException {
        try (Connection con = ConexionBD.getConexion(); PreparedStatement ps = con.prepareStatement(sql)) {
            if (idUsuario != null) ps.setInt(1, idUsuario);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getDouble(1);
            }
        }
    }
}
