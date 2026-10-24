package com.unev.farmjm.service;

import com.unev.farmjm.dao.RolDAO;
import com.unev.farmjm.dao.UsuarioDAO;
import com.unev.farmjm.model.Rol;
import com.unev.farmjm.model.Usuario;
import com.unev.farmjm.security.Accion;
import com.unev.farmjm.security.Modulo;
import com.unev.farmjm.security.PasswordUtil;
import com.unev.farmjm.security.Permisos;

import java.sql.SQLException;
import java.util.List;

/** Administración de usuarios (solo Administrador). Los permisos se validan aquí, no solo en la vista. */
public class UsuarioService {

    private final UsuarioDAO dao = new UsuarioDAO();
    private final RolDAO roles = new RolDAO();

    public List<Usuario> listar(Usuario actor) {
        Permisos.exigir(actor, Modulo.USUARIOS, Accion.VER);
        try {
            return dao.listar();
        } catch (SQLException e) {
            throw Errores.bd("listar usuarios", e);
        }
    }

    public List<Rol> roles(Usuario actor) {
        Permisos.exigir(actor, Modulo.USUARIOS, Accion.VER);
        try {
            return roles.listar();
        } catch (SQLException e) {
            throw Errores.bd("listar roles", e);
        }
    }

    public void crear(Usuario actor, Usuario nuevo, String claveClara) {
        Permisos.exigir(actor, Modulo.USUARIOS, Accion.CREAR);
        validarDatos(nuevo);
        String err = PasswordUtil.validarPolitica(claveClara);
        if (err != null) throw new ServicioException(err);
        try {
            if (dao.buscarPorUsername(nuevo.getUsername()) != null) {
                throw new ServicioException("El nombre de usuario \"" + nuevo.getUsername() + "\" ya existe.");
            }
            dao.insertar(nuevo, PasswordUtil.hash(claveClara));
        } catch (SQLException e) {
            throw Errores.bd("crear usuario", e);
        }
        Auditoria.registrar(actor, "Usuarios", "Creó el usuario " + nuevo.getUsername() + " con rol " + nuevo.getRol().getNombre());
    }

    /** Edita datos, rol y estado; si {@code nuevaClave} no está vacía también cambia la contraseña. */
    public void actualizar(Usuario actor, Usuario u, String nuevaClave) {
        Permisos.exigir(actor, Modulo.USUARIOS, Accion.EDITAR);
        validarDatos(u);
        if (actor.getIdUsuario() == u.getIdUsuario()) {
            if (!u.isActivo()) throw new ServicioException("No puede desactivar su propia cuenta.");
            if (!Permisos.ADMINISTRADOR.equals(u.getRol().getNombre())) {
                throw new ServicioException("No puede quitarse a sí mismo el rol de Administrador.");
            }
        }
        try {
            dao.actualizar(u);
            if (nuevaClave != null && !nuevaClave.isEmpty()) {
                String err = PasswordUtil.validarPolitica(nuevaClave);
                if (err != null) throw new ServicioException(err);
                dao.cambiarClave(u.getIdUsuario(), PasswordUtil.hash(nuevaClave));
            }
        } catch (SQLException e) {
            throw Errores.bd("actualizar usuario", e);
        }
        Auditoria.registrar(actor, "Usuarios", "Actualizó a " + u.getUsername() + " (rol " + u.getRol().getNombre()
                + ", " + (u.isActivo() ? "activo" : "desactivado") + ")");
    }

    public void restablecer2fa(Usuario actor, Usuario u) {
        Permisos.exigir(actor, Modulo.USUARIOS, Accion.EDITAR);
        try {
            dao.resetTotp(u.getIdUsuario());
        } catch (SQLException e) {
            throw Errores.bd("restablecer 2FA", e);
        }
        Auditoria.registrar(actor, "Usuarios", "Restableció el 2FA de " + u.getUsername());
    }

    private void validarDatos(Usuario u) {
        if (u.getNombre() == null || u.getNombre().isBlank()) throw new ServicioException("El nombre es obligatorio.");
        if (u.getApellido() == null || u.getApellido().isBlank()) throw new ServicioException("El apellido es obligatorio.");
        if (u.getUsername() == null || !u.getUsername().matches("[A-Za-z0-9._-]{3,40}")) {
            throw new ServicioException("El usuario debe tener 3 a 40 caracteres (letras, números, punto, guion).");
        }
        if (u.getEmail() != null && !u.getEmail().isBlank() && !u.getEmail().matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+")) {
            throw new ServicioException("El correo electrónico no es válido.");
        }
        if (u.getRol() == null) throw new ServicioException("Seleccione un rol.");
    }
}
