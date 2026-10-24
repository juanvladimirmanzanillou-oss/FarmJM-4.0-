package com.unev.farmjm.dao;

import com.unev.farmjm.db.ConexionBD;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Bitácora de inicios de sesión (exitosos y fallidos). */
public class BitacoraDAO {

    public record Acceso(LocalDateTime fecha, String usuario, String resultado, String detalle) { }

    public void registrar(Integer idUsuario, String username, String resultado, String detalle) throws SQLException {
        String sql = "INSERT INTO bitacora_acceso (id_usuario, username, fecha, resultado, detalle) VALUES (?,?,?,?,?)";
        try (Connection con = ConexionBD.getConexion(); PreparedStatement ps = con.prepareStatement(sql)) {
            if (idUsuario == null) ps.setNull(1, Types.INTEGER); else ps.setInt(1, idUsuario);
            ps.setString(2, username);
            ps.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
            ps.setString(4, resultado);
            ps.setString(5, detalle);
            ps.executeUpdate();
        }
    }

    public List<Acceso> recientes(int limite) throws SQLException {
        String sql = "SELECT fecha, username, resultado, detalle FROM bitacora_acceso ORDER BY fecha DESC, id_bitacora DESC LIMIT ?";
        return consultar(sql, ps -> ps.setInt(1, limite));
    }

    public List<Acceso> entre(LocalDate desde, LocalDate hasta) throws SQLException {
        String sql = "SELECT fecha, username, resultado, detalle FROM bitacora_acceso " +
                     "WHERE DATE(fecha) BETWEEN ? AND ? ORDER BY fecha DESC";
        return consultar(sql, ps -> {
            ps.setDate(1, Date.valueOf(desde));
            ps.setDate(2, Date.valueOf(hasta));
        });
    }

    private interface Param { void set(PreparedStatement ps) throws SQLException; }

    private List<Acceso> consultar(String sql, Param p) throws SQLException {
        List<Acceso> l = new ArrayList<>();
        try (Connection con = ConexionBD.getConexion(); PreparedStatement ps = con.prepareStatement(sql)) {
            p.set(ps);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    l.add(new Acceso(rs.getTimestamp(1).toLocalDateTime(), rs.getString(2), rs.getString(3), rs.getString(4)));
                }
            }
        }
        return l;
    }
}
