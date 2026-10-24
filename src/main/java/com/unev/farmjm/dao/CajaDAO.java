package com.unev.farmjm.dao;

import com.unev.farmjm.db.ConexionBD;
import com.unev.farmjm.model.ArqueoCaja;
import com.unev.farmjm.model.Caja;
import com.unev.farmjm.model.Usuario;

import java.sql.*;
import java.time.LocalDateTime;

/** Apertura, cierre y arqueo de caja. */
public class CajaDAO {

    /** Última caja del usuario (abierta o cerrada), o null si nunca abrió una. */
    public Caja ultimaDe(Usuario u) throws SQLException {
        String sql = "SELECT id_caja, monto_base, apertura, cierre, abierta FROM caja WHERE id_usuario = ? ORDER BY id_caja DESC LIMIT 1";
        try (Connection con = ConexionBD.getConexion(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, u.getIdUsuario());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                Timestamp cierre = rs.getTimestamp("cierre");
                return new Caja(rs.getInt("id_caja"), u, rs.getDouble("monto_base"),
                        rs.getTimestamp("apertura").toLocalDateTime(),
                        cierre == null ? null : cierre.toLocalDateTime(), rs.getBoolean("abierta"));
            }
        }
    }

    public Caja abrir(Usuario u, double base) throws SQLException {
        String sql = "INSERT INTO caja (id_usuario, monto_base, apertura, abierta) VALUES (?,?,?,1)";
        LocalDateTime ahora = LocalDateTime.now();
        try (Connection con = ConexionBD.getConexion();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, u.getIdUsuario());
            ps.setDouble(2, base);
            ps.setTimestamp(3, Timestamp.valueOf(ahora));
            ps.executeUpdate();
            try (ResultSet k = ps.getGeneratedKeys()) {
                k.next();
                return new Caja(k.getInt(1), u, base, ahora, null, true);
            }
        }
    }

    public void cerrar(Caja c, ArqueoCaja a) throws SQLException {
        try (Connection con = ConexionBD.getConexion()) {
            con.setAutoCommit(false);
            try {
                try (PreparedStatement ps = con.prepareStatement("UPDATE caja SET cierre = ?, abierta = 0 WHERE id_caja = ?")) {
                    ps.setTimestamp(1, Timestamp.valueOf(a.getFecha()));
                    ps.setInt(2, c.getIdCaja());
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = con.prepareStatement(
                        "INSERT INTO arqueo_caja (id_caja, monto_esperado, monto_contado, diferencia, fecha) VALUES (?,?,?,?,?)")) {
                    ps.setInt(1, c.getIdCaja());
                    ps.setDouble(2, a.getMontoEsperado());
                    ps.setDouble(3, a.getMontoContado());
                    ps.setDouble(4, a.getDiferencia());
                    ps.setTimestamp(5, Timestamp.valueOf(a.getFecha()));
                    ps.executeUpdate();
                }
                con.commit();
            } catch (SQLException | RuntimeException e) {
                con.rollback();
                throw e;
            }
        }
    }

    public double efectivoDeCaja(int idCaja) throws SQLException {
        String sql = "SELECT COALESCE(SUM(p.monto), 0) FROM pago p JOIN factura f ON f.id_factura = p.id_factura " +
                     "WHERE f.id_caja = ? AND p.metodo = 'Efectivo'";
        try (Connection con = ConexionBD.getConexion(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idCaja);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getDouble(1);
            }
        }
    }
}
