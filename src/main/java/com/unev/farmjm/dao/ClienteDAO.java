package com.unev.farmjm.dao;

import com.unev.farmjm.db.ConexionBD;
import com.unev.farmjm.model.Cliente;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/** CRUD de clientes. La eliminación es lógica (activo = 0) para no romper facturas y recetas anteriores. */
public class ClienteDAO {

    public List<Cliente> listar() throws SQLException {
        List<Cliente> l = new ArrayList<>();
        String sql = "SELECT id_cliente, nombre, cedula_rnc, telefono, email FROM cliente WHERE activo = 1 ORDER BY nombre";
        try (Connection con = ConexionBD.getConexion(); PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                l.add(new Cliente(rs.getInt(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5)));
            }
        }
        return l;
    }

    public void insertar(Cliente c) throws SQLException {
        String sql = "INSERT INTO cliente (nombre, cedula_rnc, telefono, email) VALUES (?,?,?,?)";
        try (Connection con = ConexionBD.getConexion();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, c.getNombre());
            ps.setString(2, c.getCedulaRnc());
            ps.setString(3, c.getTelefono());
            ps.setString(4, c.getEmail());
            ps.executeUpdate();
            try (ResultSet k = ps.getGeneratedKeys()) {
                if (k.next()) c.setIdCliente(k.getInt(1));
            }
        }
    }

    public void actualizar(Cliente c) throws SQLException {
        String sql = "UPDATE cliente SET nombre=?, cedula_rnc=?, telefono=?, email=? WHERE id_cliente=?";
        try (Connection con = ConexionBD.getConexion(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, c.getNombre());
            ps.setString(2, c.getCedulaRnc());
            ps.setString(3, c.getTelefono());
            ps.setString(4, c.getEmail());
            ps.setInt(5, c.getIdCliente());
            ps.executeUpdate();
        }
    }

    public void desactivar(int idCliente) throws SQLException {
        try (Connection con = ConexionBD.getConexion();
             PreparedStatement ps = con.prepareStatement("UPDATE cliente SET activo = 0 WHERE id_cliente = ?")) {
            ps.setInt(1, idCliente);
            ps.executeUpdate();
        }
    }
}
