package com.unev.farmjm.dao;

import com.unev.farmjm.db.ConexionBD;
import com.unev.farmjm.model.Rol;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class RolDAO {

    public List<Rol> listar() throws SQLException {
        List<Rol> l = new ArrayList<>();
        try (Connection con = ConexionBD.getConexion();
             PreparedStatement ps = con.prepareStatement("SELECT id_rol, nombre, descripcion FROM rol ORDER BY id_rol");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) l.add(new Rol(rs.getInt(1), rs.getString(2), rs.getString(3)));
        }
        return l;
    }
}
