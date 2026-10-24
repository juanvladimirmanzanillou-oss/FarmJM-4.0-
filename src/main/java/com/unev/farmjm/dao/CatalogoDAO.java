package com.unev.farmjm.dao;

import com.unev.farmjm.db.ConexionBD;
import com.unev.farmjm.model.Categoria;
import com.unev.farmjm.model.ComprobanteFiscal;
import com.unev.farmjm.model.Descuento;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/** Catálogos pequeños: categorías, tipos de comprobante fiscal y descuentos. */
public class CatalogoDAO {

    public List<Categoria> categorias() throws SQLException {
        List<Categoria> l = new ArrayList<>();
        try (Connection con = ConexionBD.getConexion();
             PreparedStatement ps = con.prepareStatement("SELECT id_categoria, nombre FROM categoria ORDER BY nombre");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) l.add(new Categoria(rs.getInt(1), rs.getString(2)));
        }
        return l;
    }

    public List<ComprobanteFiscal> comprobantes() throws SQLException {
        List<ComprobanteFiscal> l = new ArrayList<>();
        try (Connection con = ConexionBD.getConexion();
             PreparedStatement ps = con.prepareStatement("SELECT id_comprobante, tipo, descripcion FROM comprobante_fiscal ORDER BY id_comprobante");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) l.add(new ComprobanteFiscal(rs.getInt(1), rs.getString(2), rs.getString(3), ""));
        }
        return l;
    }

    public List<Descuento> descuentos() throws SQLException {
        List<Descuento> l = new ArrayList<>();
        try (Connection con = ConexionBD.getConexion();
             PreparedStatement ps = con.prepareStatement("SELECT id_descuento, nombre, porcentaje, activo FROM descuento WHERE activo = 1 ORDER BY porcentaje");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) l.add(new Descuento(rs.getInt(1), rs.getString(2), rs.getDouble(3), rs.getBoolean(4)));
        }
        return l;
    }
}
