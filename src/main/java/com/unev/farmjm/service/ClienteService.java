package com.unev.farmjm.service;

import com.unev.farmjm.dao.ClienteDAO;
import com.unev.farmjm.model.Cliente;
import com.unev.farmjm.model.Usuario;
import com.unev.farmjm.security.Accion;
import com.unev.farmjm.security.Modulo;
import com.unev.farmjm.security.Permisos;

import java.sql.SQLException;
import java.util.List;

/** Reglas de negocio de clientes. */
public class ClienteService {

    private final ClienteDAO dao = new ClienteDAO();

    public List<Cliente> listar(Usuario actor) {
        Permisos.exigir(actor, Modulo.CLIENTES, Accion.VER);
        try {
            return dao.listar();
        } catch (SQLException e) {
            throw Errores.bd("listar clientes", e);
        }
    }

    public void guardar(Usuario actor, Cliente c) {
        boolean nuevo = c.getIdCliente() == 0;
        Permisos.exigir(actor, Modulo.CLIENTES, nuevo ? Accion.CREAR : Accion.EDITAR);
        if (c.getNombre() == null || c.getNombre().isBlank()) throw new ServicioException("El nombre es obligatorio.");
        String doc = c.getCedulaRnc() == null ? "" : c.getCedulaRnc().trim();
        if (!doc.isEmpty() && !doc.matches("[0-9-]{9,15}")) {
            throw new ServicioException("La cédula o RNC solo admite números y guiones (9 a 15 caracteres).");
        }
        String tel = c.getTelefono() == null ? "" : c.getTelefono().trim();
        if (!tel.isEmpty() && !tel.matches("[0-9+() -]{7,20}")) throw new ServicioException("El teléfono no es válido.");
        String mail = c.getEmail() == null ? "" : c.getEmail().trim();
        if (!mail.isEmpty() && !mail.matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+")) throw new ServicioException("El correo electrónico no es válido.");
        try {
            if (nuevo) dao.insertar(c); else dao.actualizar(c);
        } catch (SQLException e) {
            throw Errores.bd(nuevo ? "crear cliente" : "editar cliente", e);
        }
        Auditoria.registrar(actor, "Clientes", (nuevo ? "Creó" : "Editó") + " el cliente " + c.getNombre());
    }

    public void eliminar(Usuario actor, Cliente c) {
        Permisos.exigir(actor, Modulo.CLIENTES, Accion.ELIMINAR);
        try {
            dao.desactivar(c.getIdCliente());
        } catch (SQLException e) {
            throw Errores.bd("eliminar cliente", e);
        }
        Auditoria.registrar(actor, "Clientes", "Eliminó el cliente " + c.getNombre());
    }
}
