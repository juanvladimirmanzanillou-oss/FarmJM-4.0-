package com.unev.farmjm.dao;

import com.unev.farmjm.db.ConexionBD;

import java.sql.*;
import java.time.LocalDateTime;

/** Devoluciones y notas de crédito (NCF B04) guardadas en MySQL. */
public class DevolucionDAO {

    /** Id de la factura con ese número, o 0 si no existe. */
    public int idFactura(String numero) throws SQLException {
        try (Connection con = ConexionBD.getConexion();
             PreparedStatement ps = con.prepareStatement("SELECT id_factura FROM factura WHERE numero = ?")) {
            ps.setString(1, numero);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    /** {cantidad vendida, cantidad ya devuelta, precio unitario} de ese producto en esa factura. */
    public double[] resumen(int idFactura, int idProducto) throws SQLException {
        String sql = "SELECT COALESCE(SUM(cantidad),0), COALESCE(MAX(precio_unitario),0) FROM detalle_factura " +
                     "WHERE id_factura = ? AND id_producto = ?";
        double vendida, precio;
        try (Connection con = ConexionBD.getConexion(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idFactura);
            ps.setInt(2, idProducto);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                vendida = rs.getDouble(1);
                precio = rs.getDouble(2);
            }
            try (PreparedStatement p2 = con.prepareStatement(
                    "SELECT COALESCE(SUM(cantidad),0) FROM devolucion WHERE id_factura = ? AND id_producto = ?")) {
                p2.setInt(1, idFactura);
                p2.setInt(2, idProducto);
                try (ResultSet rs = p2.executeQuery()) {
                    rs.next();
                    return new double[]{vendida, rs.getDouble(1), precio};
                }
            }
        }
    }

    /** Registra la devolución, devuelve las unidades al stock y retorna el NCF de la nota de crédito. */
    public String registrar(int idFactura, int idProducto, int cantidad, String motivo, double monto) throws SQLException {
        try (Connection con = ConexionBD.getConexion()) {
            con.setAutoCommit(false);
            try {
                String ncf;
                try (PreparedStatement ps = con.prepareStatement(
                        "SELECT COALESCE(MAX(CAST(SUBSTRING(ncf_nota_credito, 4) AS UNSIGNED)), 0) + 1 FROM devolucion WHERE ncf_nota_credito LIKE 'B04%'");
                     ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    ncf = String.format("B04%08d", rs.getInt(1));
                }
                try (PreparedStatement ps = con.prepareStatement(
                        "INSERT INTO devolucion (id_factura, id_producto, cantidad, motivo, monto, ncf_nota_credito, fecha) VALUES (?,?,?,?,?,?,?)")) {
                    ps.setInt(1, idFactura);
                    ps.setInt(2, idProducto);
                    ps.setInt(3, cantidad);
                    ps.setString(4, motivo);
                    ps.setDouble(5, Math.round(monto * 100.0) / 100.0);
                    ps.setString(6, ncf);
                    ps.setTimestamp(7, Timestamp.valueOf(LocalDateTime.now()));
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = con.prepareStatement("UPDATE producto SET stock = stock + ? WHERE id_producto = ?")) {
                    ps.setInt(1, cantidad);
                    ps.setInt(2, idProducto);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = con.prepareStatement(
                        "UPDATE inventario SET existencia = existencia + ?, ultima_actualizacion = NOW() WHERE id_producto = ?")) {
                    ps.setInt(1, cantidad);
                    ps.setInt(2, idProducto);
                    ps.executeUpdate();
                }
                con.commit();
                return ncf;
            } catch (SQLException | RuntimeException e) {
                con.rollback();
                throw e;
            }
        }
    }
}
