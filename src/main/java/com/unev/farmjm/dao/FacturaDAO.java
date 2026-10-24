package com.unev.farmjm.dao;

import com.unev.farmjm.db.ConexionBD;
import com.unev.farmjm.model.*;

import java.sql.*;

/** Guarda una venta completa (factura, detalle, pago y descuento de stock) en una sola transacción. */
public class FacturaDAO {

    public void guardar(Factura f, String metodoPago) throws SQLException {
        try (Connection con = ConexionBD.getConexion()) {
            con.setAutoCommit(false);
            try {
                String tipo = f.getComprobante().getTipo();
                String ncf = siguienteNcf(con, tipo);

                String sql = "INSERT INTO factura (numero, ncf, fecha, subtotal, descuento_aplicado, itbis, total, " +
                             "id_cliente, id_usuario, id_caja, id_comprobante, id_descuento) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)";
                try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, "TMP" + (System.nanoTime() % 1_000_000_000L));
                    ps.setString(2, ncf);
                    ps.setTimestamp(3, Timestamp.valueOf(f.getFecha()));
                    ps.setDouble(4, redondear(f.getSubtotal()));
                    ps.setDouble(5, redondear(f.getDescuentoAplicado()));
                    ps.setDouble(6, redondear(f.getItbis()));
                    ps.setDouble(7, redondear(f.getTotal()));
                    if (f.getCliente() == null) ps.setNull(8, Types.INTEGER); else ps.setInt(8, f.getCliente().getIdCliente());
                    ps.setInt(9, f.getUsuario().getIdUsuario());
                    ps.setInt(10, f.getCaja().getIdCaja());
                    ps.setInt(11, f.getComprobante().getIdComprobante());
                    if (f.getDescuento() == null) ps.setNull(12, Types.INTEGER); else ps.setInt(12, f.getDescuento().getIdDescuento());
                    ps.executeUpdate();
                    try (ResultSet k = ps.getGeneratedKeys()) {
                        k.next();
                        f.setIdFactura(k.getInt(1));
                    }
                }
                f.setNumero(String.format("F-%05d", f.getIdFactura()));
                try (PreparedStatement ps = con.prepareStatement("UPDATE factura SET numero = ? WHERE id_factura = ?")) {
                    ps.setString(1, f.getNumero());
                    ps.setInt(2, f.getIdFactura());
                    ps.executeUpdate();
                }

                for (DetalleFactura d : f.getDetalles()) {
                    try (PreparedStatement ps = con.prepareStatement(
                            "INSERT INTO detalle_factura (id_factura, id_producto, id_lote, cantidad, precio_unitario, subtotal) VALUES (?,?,?,?,?,?)")) {
                        ps.setInt(1, f.getIdFactura());
                        ps.setInt(2, d.getProducto().getIdProducto());
                        if (d.getLote() == null) ps.setNull(3, Types.INTEGER); else ps.setInt(3, d.getLote().getIdLote());
                        ps.setInt(4, d.getCantidad());
                        ps.setDouble(5, d.getPrecioUnitario());
                        ps.setDouble(6, redondear(d.getSubtotal()));
                        ps.executeUpdate();
                    }
                    try (PreparedStatement ps = con.prepareStatement(
                            "UPDATE producto SET stock = stock - ? WHERE id_producto = ? AND stock >= ?")) {
                        ps.setInt(1, d.getCantidad());
                        ps.setInt(2, d.getProducto().getIdProducto());
                        ps.setInt(3, d.getCantidad());
                        if (ps.executeUpdate() == 0) {
                            throw new SQLException("Stock insuficiente de " + d.getProducto().getNombre());
                        }
                    }
                    if (d.getLote() != null) {
                        try (PreparedStatement ps = con.prepareStatement(
                                "UPDATE lote SET cantidad = GREATEST(cantidad - ?, 0) WHERE id_lote = ?")) {
                            ps.setInt(1, d.getCantidad());
                            ps.setInt(2, d.getLote().getIdLote());
                            ps.executeUpdate();
                        }
                    }
                    try (PreparedStatement ps = con.prepareStatement(
                            "UPDATE inventario SET existencia = GREATEST(existencia - ?, 0), ultima_actualizacion = NOW() WHERE id_producto = ?")) {
                        ps.setInt(1, d.getCantidad());
                        ps.setInt(2, d.getProducto().getIdProducto());
                        ps.executeUpdate();
                    }
                }

                try (PreparedStatement ps = con.prepareStatement(
                        "INSERT INTO pago (id_factura, metodo, monto, fecha) VALUES (?,?,?,?)")) {
                    ps.setInt(1, f.getIdFactura());
                    ps.setString(2, metodoPago);
                    ps.setDouble(3, redondear(f.getTotal()));
                    ps.setTimestamp(4, Timestamp.valueOf(f.getFecha()));
                    ps.executeUpdate();
                }
                f.setComprobante(new ComprobanteFiscal(f.getComprobante().getIdComprobante(), tipo,
                        f.getComprobante().getDescripcion(), ncf));
                con.commit();
            } catch (SQLException | RuntimeException e) {
                con.rollback();
                throw e;
            }
        }
    }

    /** Siguiente NCF del tipo dado, por ejemplo B0200000001, calculado desde la base de datos. */
    private String siguienteNcf(Connection con, String tipo) throws SQLException {
        String sql = "SELECT COALESCE(MAX(CAST(SUBSTRING(ncf, ?) AS UNSIGNED)), 0) + 1 FROM factura WHERE ncf LIKE ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, tipo.length() + 1);
            ps.setString(2, tipo + "%");
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return String.format("%s%08d", tipo, rs.getInt(1));
            }
        }
    }

    private double redondear(double v) { return Math.round(v * 100.0) / 100.0; }
}
