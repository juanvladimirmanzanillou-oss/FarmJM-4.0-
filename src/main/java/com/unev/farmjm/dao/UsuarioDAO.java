package com.unev.farmjm.dao;

import com.unev.farmjm.db.ConexionBD;
import com.unev.farmjm.model.Rol;
import com.unev.farmjm.model.Usuario;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Acceso a datos de usuarios (tabla usuario + rol). */
public class UsuarioDAO {

    private static final String BASE =
        "SELECT u.id_usuario, u.nombre, u.apellido, u.licencia, u.username, u.clave_hash, u.email, u.activo, " +
        "       u.totp_secret, u.totp_activo, u.intentos_fallidos, u.bloqueado_hasta, r.id_rol, r.nombre AS rol, r.descripcion " +
        "FROM usuario u JOIN rol r ON r.id_rol = u.id_rol ";

    public Usuario buscarPorUsername(String username) throws SQLException {
        try (Connection con = ConexionBD.getConexion();
             PreparedStatement ps = con.prepareStatement(BASE + "WHERE u.username = ?")) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        }
    }

    public List<Usuario> listar() throws SQLException {
        List<Usuario> l = new ArrayList<>();
        try (Connection con = ConexionBD.getConexion();
             PreparedStatement ps = con.prepareStatement(BASE + "ORDER BY u.nombre, u.apellido");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) l.add(mapear(rs));
        }
        return l;
    }

    public void insertar(Usuario u, String hash) throws SQLException {
        String sql = "INSERT INTO usuario (nombre, apellido, licencia, username, clave_hash, email, activo, id_rol) " +
                     "VALUES (?,?,?,?,?,?,?,?)";
        try (Connection con = ConexionBD.getConexion();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, u.getNombre());
            ps.setString(2, u.getApellido());
            ps.setString(3, u.getLicencia());
            ps.setString(4, u.getUsername());
            ps.setString(5, hash);
            ps.setString(6, u.getEmail());
            ps.setBoolean(7, u.isActivo());
            ps.setInt(8, u.getRol().getIdRol());
            ps.executeUpdate();
            try (ResultSet k = ps.getGeneratedKeys()) {
                if (k.next()) u.setIdUsuario(k.getInt(1));
            }
        }
    }

    public void actualizar(Usuario u) throws SQLException {
        String sql = "UPDATE usuario SET nombre=?, apellido=?, licencia=?, email=?, activo=?, id_rol=? WHERE id_usuario=?";
        try (Connection con = ConexionBD.getConexion(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, u.getNombre());
            ps.setString(2, u.getApellido());
            ps.setString(3, u.getLicencia());
            ps.setString(4, u.getEmail());
            ps.setBoolean(5, u.isActivo());
            ps.setInt(6, u.getRol().getIdRol());
            ps.setInt(7, u.getIdUsuario());
            ps.executeUpdate();
        }
    }

    public void cambiarClave(int idUsuario, String hash) throws SQLException {
        ejecutar("UPDATE usuario SET clave_hash=?, intentos_fallidos=0, bloqueado_hasta=NULL WHERE id_usuario=?", hash, idUsuario);
    }

    /** Guarda el contador de intentos fallidos y, si corresponde, el bloqueo temporal. */
    public void registrarFallo(int idUsuario, int intentos, LocalDateTime bloqueadoHasta) throws SQLException {
        String sql = "UPDATE usuario SET intentos_fallidos=?, bloqueado_hasta=? WHERE id_usuario=?";
        try (Connection con = ConexionBD.getConexion(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, intentos);
            ps.setTimestamp(2, bloqueadoHasta == null ? null : Timestamp.valueOf(bloqueadoHasta));
            ps.setInt(3, idUsuario);
            ps.executeUpdate();
        }
    }

    public void limpiarIntentos(int idUsuario) throws SQLException {
        ejecutar("UPDATE usuario SET intentos_fallidos=0, bloqueado_hasta=NULL WHERE id_usuario=?", idUsuario);
    }

    public void guardarTotp(int idUsuario, String secreto) throws SQLException {
        ejecutar("UPDATE usuario SET totp_secret=?, totp_activo=1 WHERE id_usuario=?", secreto, idUsuario);
    }

    /** Restablece el 2FA: el usuario deberá escanear un QR nuevo en su próximo ingreso. */
    public void resetTotp(int idUsuario) throws SQLException {
        ejecutar("UPDATE usuario SET totp_secret=NULL, totp_activo=0, intentos_fallidos=0, bloqueado_hasta=NULL WHERE id_usuario=?", idUsuario);
    }

    private void ejecutar(String sql, Object... params) throws SQLException {
        try (Connection con = ConexionBD.getConexion(); PreparedStatement ps = con.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) ps.setObject(i + 1, params[i]);
            ps.executeUpdate();
        }
    }

    private Usuario mapear(ResultSet rs) throws SQLException {
        Usuario u = new Usuario();
        u.setIdUsuario(rs.getInt("id_usuario"));
        u.setNombre(rs.getString("nombre"));
        u.setApellido(rs.getString("apellido"));
        u.setLicencia(rs.getString("licencia"));
        u.setUsername(rs.getString("username"));
        u.setClave(rs.getString("clave_hash"));
        u.setEmail(rs.getString("email"));
        u.setActivo(rs.getBoolean("activo"));
        u.setTotpSecret(rs.getString("totp_secret"));
        u.setTotpActivo(rs.getBoolean("totp_activo"));
        u.setIntentosFallidos(rs.getInt("intentos_fallidos"));
        Timestamp t = rs.getTimestamp("bloqueado_hasta");
        u.setBloqueadoHasta(t == null ? null : t.toLocalDateTime());
        u.setRol(new Rol(rs.getInt("id_rol"), rs.getString("rol"), rs.getString("descripcion")));
        return u;
    }
}
