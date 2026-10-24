package com.unev.farmjm.dao;

import com.unev.farmjm.db.ConexionBD;
import com.unev.farmjm.model.LogActividad;
import com.unev.farmjm.model.Usuario;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Auditoría: acciones importantes guardadas en log_actividad. */
public class LogDAO {

    public void insertar(LogActividad l) throws SQLException {
        String sql = "INSERT INTO log_actividad (id_usuario, accion, modulo, fecha) VALUES (?,?,?,?)";
        try (Connection con = ConexionBD.getConexion(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, l.getUsuario().getIdUsuario());
            ps.setString(2, l.getAccion().length() > 200 ? l.getAccion().substring(0, 200) : l.getAccion());
            ps.setString(3, l.getModulo());
            ps.setTimestamp(4, Timestamp.valueOf(l.getFecha()));
            ps.executeUpdate();
        }
    }

    public List<LogActividad> recientes(int limite) throws SQLException {
        String sql = "SELECT l.id_log, l.accion, l.modulo, l.fecha, u.id_usuario, u.nombre, u.apellido, u.username " +
                     "FROM log_actividad l JOIN usuario u ON u.id_usuario = l.id_usuario " +
                     "ORDER BY l.fecha DESC, l.id_log DESC LIMIT ?";
        List<LogActividad> lista = new ArrayList<>();
        try (Connection con = ConexionBD.getConexion(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, limite);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Usuario u = new Usuario();
                    u.setIdUsuario(rs.getInt("id_usuario"));
                    u.setNombre(rs.getString("nombre"));
                    u.setApellido(rs.getString("apellido"));
                    u.setUsername(rs.getString("username"));
                    LocalDateTime f = rs.getTimestamp("fecha").toLocalDateTime();
                    lista.add(new LogActividad(rs.getInt("id_log"), u, rs.getString("accion"), rs.getString("modulo"), f));
                }
            }
        }
        return lista;
    }
}
