package com.unev.farmjm.service;

import com.unev.farmjm.dao.LogDAO;
import com.unev.farmjm.model.LogActividad;
import com.unev.farmjm.model.Usuario;
import com.unev.farmjm.util.Log;

import java.time.LocalDateTime;
import java.util.function.Consumer;

/** Registro de auditoría: guarda cada acción importante en log_actividad y avisa a la interfaz. */
public final class Auditoria {

    private static final LogDAO DAO = new LogDAO();
    /** La interfaz se suscribe aquí para mostrar el movimiento en la pantalla de Auditoría. */
    public static volatile Consumer<LogActividad> alRegistrar = l -> { };

    private Auditoria() { }

    public static void registrar(Usuario actor, String modulo, String accion) {
        LogActividad l = new LogActividad(0, actor, accion, modulo, LocalDateTime.now());
        try {
            DAO.insertar(l);
        } catch (Exception e) {
            Log.error("No se pudo guardar el log de actividad", e);
        }
        alRegistrar.accept(l);
    }
}
