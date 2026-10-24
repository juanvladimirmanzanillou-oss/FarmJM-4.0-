package com.unev.farmjm.dao;

import com.unev.farmjm.db.ConexionBD;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Consultas para los reportes PDF. */
public class ReporteDAO {

    public record VentaFila(String numero, String ncf, LocalDateTime fecha, String usuario,
                            double subtotal, double descuento, double itbis, double total, String metodo) { }

    public record InventarioFila(String codigo, String nombre, int stock, String lote, LocalDate vence) { }

    /** Ventas entre dos fechas (inclusive). Si idUsuario no es null solo trae las de ese usuario. */
    public List<VentaFila> ventas(LocalDate desde, LocalDate hasta, Integer idUsuario) throws SQLException {
        String sql = "SELECT f.numero, f.ncf, f.fecha, u.username, f.subtotal, f.descuento_aplicado, f.itbis, f.total, " +
                "COALESCE((SELECT GROUP_CONCAT(p.metodo) FROM pago p WHERE p.id_factura = f.id_factura), '-') AS metodo " +
                "FROM factura f JOIN usuario u ON u.id_usuario = f.id_usuario " +
                "WHERE DATE(f.fecha) BETWEEN ? AND ? " + (idUsuario == null ? "" : "AND f.id_usuario = ? ") +
                "ORDER BY f.fecha";
        List<VentaFila> l = new ArrayList<>();
        try (Connection con = ConexionBD.getConexion(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(desde));
            ps.setDate(2, Date.valueOf(hasta));
            if (idUsuario != null) ps.setInt(3, idUsuario);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    l.add(new VentaFila(rs.getString(1), rs.getString(2), rs.getTimestamp(3).toLocalDateTime(),
                            rs.getString(4), rs.getDouble(5), rs.getDouble(6), rs.getDouble(7), rs.getDouble(8), rs.getString(9)));
                }
            }
        }
        return l;
    }

    /** Medicamentos con stock bajo (≤ 10) o con el lote más próximo vencido o por vencer (≤ 90 días). */
    public List<InventarioFila> inventarioCritico() throws SQLException {
        String sql = "SELECT p.codigo, p.nombre, p.stock, l.codigo_lote, l.fecha_vencimiento FROM producto p " +
                "JOIN medicamento m ON m.id_producto = p.id_producto " +
                "LEFT JOIN lote l ON l.id_producto = p.id_producto AND l.id_lote = " +
                "  (SELECT l2.id_lote FROM lote l2 WHERE l2.id_producto = p.id_producto ORDER BY l2.fecha_vencimiento, l2.id_lote LIMIT 1) " +
                "WHERE p.activo = 1 AND (p.stock <= 10 OR l.fecha_vencimiento <= DATE_ADD(CURDATE(), INTERVAL 90 DAY)) " +
                "ORDER BY l.fecha_vencimiento, p.nombre";
        List<InventarioFila> l = new ArrayList<>();
        try (Connection con = ConexionBD.getConexion(); PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Date v = rs.getDate(5);
                l.add(new InventarioFila(rs.getString(1), rs.getString(2), rs.getInt(3), rs.getString(4),
                        v == null ? null : v.toLocalDate()));
            }
        }
        return l;
    }
}
