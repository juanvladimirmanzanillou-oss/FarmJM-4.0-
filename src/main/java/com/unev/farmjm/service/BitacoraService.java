package com.unev.farmjm.service;

import com.unev.farmjm.dao.BitacoraDAO;
import com.unev.farmjm.dao.LogDAO;
import com.unev.farmjm.model.LogActividad;
import com.unev.farmjm.model.Usuario;
import com.unev.farmjm.security.Accion;
import com.unev.farmjm.security.Modulo;
import com.unev.farmjm.security.Permisos;

import java.sql.SQLException;
import java.util.List;

/** Consulta de la bitácora de accesos (Administrador y Supervisor). */
public class BitacoraService {

    private final BitacoraDAO dao = new BitacoraDAO();

    public List<BitacoraDAO.Acceso> recientes(Usuario actor, int limite) {
        Permisos.exigir(actor, Modulo.AUDITORIA, Accion.VER);
        try {
            return dao.recientes(limite);
        } catch (SQLException e) {
            throw Errores.bd("consultar la bitácora de accesos", e);
        }
    }

    /** Últimas acciones registradas en la auditoría del sistema. */
    public List<LogActividad> actividad(Usuario actor, int limite) {
        Permisos.exigir(actor, Modulo.AUDITORIA, Accion.VER);
        try {
            return new LogDAO().recientes(limite);
        } catch (SQLException e) {
            throw Errores.bd("consultar la actividad", e);
        }
    }
}
