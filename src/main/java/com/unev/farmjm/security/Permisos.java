package com.unev.farmjm.security;

import com.unev.farmjm.model.Usuario;
import com.unev.farmjm.service.ServicioException;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Matriz de roles y permisos. Se consulta desde la vista (para ocultar o deshabilitar opciones)
 * y también desde la capa de servicio (exigir), de modo que no basta con manipular la interfaz.
 */
public final class Permisos {

    public static final String ADMINISTRADOR = "Administrador";
    public static final String SUPERVISOR = "Supervisor";
    public static final String FARMACEUTICO = "Farmacéutico";

    private static final Map<String, Map<Modulo, Set<Accion>>> MATRIZ = new java.util.LinkedHashMap<>();

    static {
        // Administrador: todo
        Map<Modulo, Set<Accion>> admin = new EnumMap<>(Modulo.class);
        for (Modulo m : Modulo.values()) admin.put(m, EnumSet.allOf(Accion.class));
        MATRIZ.put(ADMINISTRADOR, admin);

        // Supervisor: opera y supervisa, pero no administra usuarios ni elimina
        Map<Modulo, Set<Accion>> sup = new EnumMap<>(Modulo.class);
        sup.put(Modulo.DASHBOARD, EnumSet.of(Accion.VER));
        sup.put(Modulo.POS, EnumSet.of(Accion.VER, Accion.CREAR));
        sup.put(Modulo.INVENTARIO, EnumSet.of(Accion.VER, Accion.CREAR, Accion.EDITAR, Accion.REPORTAR));
        sup.put(Modulo.CLIENTES, EnumSet.of(Accion.VER, Accion.CREAR, Accion.EDITAR));
        sup.put(Modulo.CAJA, EnumSet.of(Accion.VER, Accion.CREAR, Accion.EDITAR));
        sup.put(Modulo.RECETAS, EnumSet.of(Accion.VER, Accion.CREAR));
        sup.put(Modulo.DEVOLUCIONES, EnumSet.of(Accion.VER, Accion.CREAR));
        sup.put(Modulo.REPORTES, EnumSet.of(Accion.VER, Accion.REPORTAR));
        sup.put(Modulo.AUDITORIA, EnumSet.of(Accion.VER));
        MATRIZ.put(SUPERVISOR, sup);

        // Farmacéutico: vende, consulta inventario y atiende clientes
        Map<Modulo, Set<Accion>> far = new EnumMap<>(Modulo.class);
        far.put(Modulo.DASHBOARD, EnumSet.of(Accion.VER));
        far.put(Modulo.POS, EnumSet.of(Accion.VER, Accion.CREAR));
        far.put(Modulo.INVENTARIO, EnumSet.of(Accion.VER));
        far.put(Modulo.CLIENTES, EnumSet.of(Accion.VER, Accion.CREAR, Accion.EDITAR));
        far.put(Modulo.CAJA, EnumSet.of(Accion.VER, Accion.CREAR, Accion.EDITAR));
        far.put(Modulo.RECETAS, EnumSet.of(Accion.VER));
        far.put(Modulo.DEVOLUCIONES, EnumSet.of(Accion.VER, Accion.CREAR));
        far.put(Modulo.REPORTES, EnumSet.of(Accion.VER, Accion.REPORTAR));
        MATRIZ.put(FARMACEUTICO, far);
    }

    private Permisos() { }

    public static boolean puede(Usuario u, Modulo m, Accion a) {
        if (u == null || u.getRol() == null || !u.isActivo()) return false;
        return puede(u.getRol().getNombre(), m, a);
    }

    public static boolean puede(String rol, Modulo m, Accion a) {
        Map<Modulo, Set<Accion>> p = MATRIZ.get(rol);
        if (p == null) return false;
        Set<Accion> s = p.get(m);
        return s != null && s.contains(a);
    }

    /** Validación en la capa de servicio: lanza una excepción si el rol no tiene el permiso. */
    public static void exigir(Usuario u, Modulo m, Accion a) {
        if (!puede(u, m, a)) {
            throw new ServicioException("Su rol no tiene permiso para " + a.name().toLowerCase()
                    + " en " + m.titulo() + ".");
        }
    }

    public static boolean esAdministrador(Usuario u) {
        return u != null && u.getRol() != null && ADMINISTRADOR.equals(u.getRol().getNombre());
    }

    /** Matriz completa (para documentarla en el informe). */
    public static Map<String, Map<Modulo, Set<Accion>>> matriz() { return MATRIZ; }
}
