package com.unev.farmjm.dao;

import com.unev.farmjm.db.ConexionBD;
import com.unev.farmjm.model.*;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/** CRUD de medicamentos (producto + medicamento + lote + inventario). Eliminación lógica con activo = 0. */
public class MedicamentoDAO {

    private static final String SQL_LISTAR =
        "SELECT p.id_producto, p.codigo, p.nombre, p.precio, p.stock, " +
        "       c.id_categoria, c.nombre AS categoria, " +
        "       m.principio_activo, m.presentacion, m.requiere_receta, m.controlado, " +
        "       l.id_lote, l.codigo_lote, l.fecha_vencimiento, l.cantidad AS lote_cantidad " +
        "FROM producto p " +
        "JOIN medicamento m ON m.id_producto = p.id_producto " +
        "LEFT JOIN categoria c ON c.id_categoria = p.id_categoria " +
        "LEFT JOIN lote l ON l.id_producto = p.id_producto " +
        "  AND l.id_lote = (SELECT l2.id_lote FROM lote l2 WHERE l2.id_producto = p.id_producto " +
        "                   ORDER BY l2.fecha_vencimiento, l2.id_lote LIMIT 1) " +
        "WHERE p.activo = 1 ORDER BY p.nombre";

    public static List<Medicamento> listar() throws SQLException {
        List<Medicamento> lista = new ArrayList<>();
        try (Connection con = ConexionBD.getConexion();
             PreparedStatement ps = con.prepareStatement(SQL_LISTAR);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Medicamento m = new Medicamento();
                m.setIdProducto(rs.getInt("id_producto"));
                m.setCodigo(rs.getString("codigo"));
                m.setNombre(rs.getString("nombre"));
                m.setPrecio(rs.getDouble("precio"));
                m.setStock(rs.getInt("stock"));
                m.setCategoria(new Categoria(rs.getInt("id_categoria"), rs.getString("categoria")));
                m.setPrincipioActivo(rs.getString("principio_activo"));
                m.setPresentacion(rs.getString("presentacion"));
                m.setRequiereReceta(rs.getBoolean("requiere_receta"));
                m.setControlado(rs.getBoolean("controlado"));
                if (rs.getObject("id_lote") != null) {
                    m.agregarLote(new Lote(rs.getInt("id_lote"), rs.getString("codigo_lote"), m,
                            rs.getDate("fecha_vencimiento").toLocalDate(), rs.getInt("lote_cantidad")));
                }
                lista.add(m);
            }
        }
        return lista;
    }

    public boolean existeCodigo(String codigo, int excluirId) throws SQLException {
        try (Connection con = ConexionBD.getConexion();
             PreparedStatement ps = con.prepareStatement("SELECT COUNT(*) FROM producto WHERE codigo = ? AND id_producto <> ?")) {
            ps.setString(1, codigo);
            ps.setInt(2, excluirId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }
        }
    }

    /** Inserta el medicamento con su lote inicial y su fila de inventario en una sola transacción. */
    public void insertar(Medicamento m, Lote lote) throws SQLException {
        try (Connection con = ConexionBD.getConexion()) {
            con.setAutoCommit(false);
            try {
                try (PreparedStatement ps = con.prepareStatement(
                        "INSERT INTO producto (codigo, nombre, precio, stock, id_categoria) VALUES (?,?,?,?,?)",
                        Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, m.getCodigo());
                    ps.setString(2, m.getNombre());
                    ps.setDouble(3, m.getPrecio());
                    ps.setInt(4, m.getStock());
                    ps.setInt(5, m.getCategoria().getIdCategoria());
                    ps.executeUpdate();
                    try (ResultSet k = ps.getGeneratedKeys()) {
                        k.next();
                        m.setIdProducto(k.getInt(1));
                    }
                }
                try (PreparedStatement ps = con.prepareStatement(
                        "INSERT INTO medicamento (id_producto, principio_activo, presentacion, requiere_receta, controlado) VALUES (?,?,?,?,?)")) {
                    ps.setInt(1, m.getIdProducto());
                    ps.setString(2, m.getPrincipioActivo());
                    ps.setString(3, m.getPresentacion());
                    ps.setBoolean(4, m.isRequiereReceta());
                    ps.setBoolean(5, m.isControlado());
                    ps.executeUpdate();
                }
                if (lote != null) insertarLote(con, m, lote);
                try (PreparedStatement ps = con.prepareStatement(
                        "INSERT INTO inventario (id_producto, existencia, stock_minimo, ultima_actualizacion) VALUES (?,?,10,NOW())")) {
                    ps.setInt(1, m.getIdProducto());
                    ps.setInt(2, m.getStock());
                    ps.executeUpdate();
                }
                con.commit();
            } catch (SQLException | RuntimeException e) {
                con.rollback();
                throw e;
            }
        }
    }

    public void actualizar(Medicamento m, Lote lote) throws SQLException {
        try (Connection con = ConexionBD.getConexion()) {
            con.setAutoCommit(false);
            try {
                try (PreparedStatement ps = con.prepareStatement(
                        "UPDATE producto SET codigo=?, nombre=?, precio=?, stock=?, id_categoria=? WHERE id_producto=?")) {
                    ps.setString(1, m.getCodigo());
                    ps.setString(2, m.getNombre());
                    ps.setDouble(3, m.getPrecio());
                    ps.setInt(4, m.getStock());
                    ps.setInt(5, m.getCategoria().getIdCategoria());
                    ps.setInt(6, m.getIdProducto());
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = con.prepareStatement(
                        "UPDATE medicamento SET principio_activo=?, presentacion=?, requiere_receta=?, controlado=? WHERE id_producto=?")) {
                    ps.setString(1, m.getPrincipioActivo());
                    ps.setString(2, m.getPresentacion());
                    ps.setBoolean(3, m.isRequiereReceta());
                    ps.setBoolean(4, m.isControlado());
                    ps.setInt(5, m.getIdProducto());
                    ps.executeUpdate();
                }
                if (lote != null) {
                    if (lote.getIdLote() > 0) {
                        try (PreparedStatement ps = con.prepareStatement(
                                "UPDATE lote SET codigo_lote=?, fecha_vencimiento=?, cantidad=? WHERE id_lote=?")) {
                            ps.setString(1, lote.getCodigoLote());
                            ps.setDate(2, Date.valueOf(lote.getFechaVencimiento()));
                            ps.setInt(3, lote.getCantidad());
                            ps.setInt(4, lote.getIdLote());
                            ps.executeUpdate();
                        }
                    } else {
                        insertarLote(con, m, lote);
                    }
                }
                try (PreparedStatement ps = con.prepareStatement(
                        "UPDATE inventario SET existencia=?, ultima_actualizacion=NOW() WHERE id_producto=?")) {
                    ps.setInt(1, m.getStock());
                    ps.setInt(2, m.getIdProducto());
                    ps.executeUpdate();
                }
                con.commit();
            } catch (SQLException | RuntimeException e) {
                con.rollback();
                throw e;
            }
        }
    }

    public void desactivar(int idProducto) throws SQLException {
        try (Connection con = ConexionBD.getConexion();
             PreparedStatement ps = con.prepareStatement("UPDATE producto SET activo = 0 WHERE id_producto = ?")) {
            ps.setInt(1, idProducto);
            ps.executeUpdate();
        }
    }

    private void insertarLote(Connection con, Medicamento m, Lote lote) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "INSERT INTO lote (codigo_lote, fecha_vencimiento, cantidad, id_producto) VALUES (?,?,?,?)",
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, lote.getCodigoLote());
            ps.setDate(2, Date.valueOf(lote.getFechaVencimiento()));
            ps.setInt(3, lote.getCantidad());
            ps.setInt(4, m.getIdProducto());
            ps.executeUpdate();
            try (ResultSet k = ps.getGeneratedKeys()) {
                if (k.next()) lote.setIdLote(k.getInt(1));
            }
        }
    }
}
