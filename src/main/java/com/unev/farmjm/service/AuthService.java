package com.unev.farmjm.service;

import com.unev.farmjm.dao.BitacoraDAO;
import com.unev.farmjm.dao.UsuarioDAO;
import com.unev.farmjm.model.Usuario;
import com.unev.farmjm.security.PasswordUtil;
import com.unev.farmjm.security.Totp;
import com.unev.farmjm.util.Log;

import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Autenticación en dos pasos:
 *  1) usuario y contraseña (hash BCrypt guardado en MySQL);
 *  2) código TOTP de 6 dígitos (Google/Microsoft Authenticator).
 * Máximo 3 intentos fallidos (entre ambos pasos); al superarlos la cuenta se bloquea 5 minutos.
 * Cada inicio de sesión exitoso o fallido queda en la bitácora.
 */
public class AuthService {

    public static final int MAX_INTENTOS = 3;
    public static final int MINUTOS_BLOQUEO = 5;

    private final UsuarioDAO usuarios = new UsuarioDAO();
    private final BitacoraDAO bitacora = new BitacoraDAO();

    // ------------------------------------------------------------------ primer factor
    public Usuario primerFactor(String username, String clave) {
        if (username == null || username.isBlank() || clave == null || clave.isEmpty()) {
            throw new AuthException("Escriba su usuario y contraseña.", false);
        }
        try {
            Usuario u = usuarios.buscarPorUsername(username.trim());
            if (u == null) {
                registrar(null, username.trim(), "FALLIDO", "Usuario inexistente");
                throw new AuthException("Usuario o contraseña incorrectos.", false);
            }
            validarCuenta(u);
            if (!PasswordUtil.verificar(clave, u.getClave())) {
                throw fallo(u, "Contraseña incorrecta");
            }
            return u;
        } catch (SQLException e) {
            throw Errores.bd("inicio de sesión", e);
        }
    }

    // ------------------------------------------------------------------ segundo factor
    /** Verifica el código TOTP de un usuario que ya tiene el 2FA activo. */
    public Usuario segundoFactor(Usuario sesion, String codigo) {
        try {
            Usuario u = usuarios.buscarPorUsername(sesion.getUsername());
            if (u == null) throw new AuthException("La cuenta ya no existe.", true);
            validarCuenta(u);
            if (!u.isTotpActivo() || u.getTotpSecret() == null) {
                throw new AuthException("El 2FA de esta cuenta fue restablecido. Inicie sesión de nuevo.", true);
            }
            if (!Totp.verificar(u.getTotpSecret(), codigo == null ? "" : codigo.trim())) {
                throw fallo(u, "Código 2FA incorrecto");
            }
            exito(u, "Inicio de sesión con 2FA");
            return u;
        } catch (SQLException e) {
            throw Errores.bd("verificación 2FA", e);
        }
    }

    /** Genera una clave secreta nueva (aún sin guardar) para mostrarla como QR. */
    public String nuevoSecreto() { return Totp.nuevoSecreto(); }

    /** Activa el 2FA: si el código coincide con el secreto escaneado, se guarda en la base de datos. */
    public Usuario activarTotp(Usuario sesion, String secreto, String codigo) {
        try {
            Usuario u = usuarios.buscarPorUsername(sesion.getUsername());
            if (u == null) throw new AuthException("La cuenta ya no existe.", true);
            validarCuenta(u);
            if (!Totp.verificar(secreto, codigo == null ? "" : codigo.trim())) {
                throw fallo(u, "Código 2FA incorrecto al activar");
            }
            usuarios.guardarTotp(u.getIdUsuario(), secreto);
            u.setTotpSecret(secreto);
            u.setTotpActivo(true);
            exito(u, "Inicio de sesión (2FA activado)");
            return u;
        } catch (SQLException e) {
            throw Errores.bd("activación 2FA", e);
        }
    }

    // ------------------------------------------------------------------ internos
    private void validarCuenta(Usuario u) throws SQLException {
        if (u.getBloqueadoHasta() != null && !u.estaBloqueado()) {   // el bloqueo ya venció
            usuarios.limpiarIntentos(u.getIdUsuario());
            u.setIntentosFallidos(0);
            u.setBloqueadoHasta(null);
        }
        if (u.estaBloqueado()) {
            long min = Math.max(1, Duration.between(LocalDateTime.now(), u.getBloqueadoHasta()).toMinutes() + 1);
            registrar(u.getIdUsuario(), u.getUsername(), "FALLIDO", "Intento con cuenta bloqueada");
            throw new AuthException("Cuenta bloqueada temporalmente. Intente de nuevo en " + min + " minuto(s).", true);
        }
        if (!u.isActivo()) {
            registrar(u.getIdUsuario(), u.getUsername(), "FALLIDO", "Cuenta desactivada");
            throw new AuthException("Su cuenta está desactivada. Contacte al administrador.", true);
        }
    }

    private AuthException fallo(Usuario u, String motivo) throws SQLException {
        int n = u.getIntentosFallidos() + 1;
        if (n >= MAX_INTENTOS) {
            usuarios.registrarFallo(u.getIdUsuario(), n, LocalDateTime.now().plusMinutes(MINUTOS_BLOQUEO));
            registrar(u.getIdUsuario(), u.getUsername(), "FALLIDO", motivo + " (intento " + n + "): cuenta bloqueada");
            return new AuthException("Superó los " + MAX_INTENTOS + " intentos. Su cuenta quedó bloqueada por "
                    + MINUTOS_BLOQUEO + " minutos.", true);
        }
        usuarios.registrarFallo(u.getIdUsuario(), n, null);
        registrar(u.getIdUsuario(), u.getUsername(), "FALLIDO", motivo + " (intento " + n + " de " + MAX_INTENTOS + ")");
        boolean codigo = motivo.contains("2FA");
        return new AuthException((codigo ? "Código incorrecto." : "Usuario o contraseña incorrectos.")
                + " Intento " + n + " de " + MAX_INTENTOS + ".", false);
    }

    private void exito(Usuario u, String detalle) throws SQLException {
        usuarios.limpiarIntentos(u.getIdUsuario());
        registrar(u.getIdUsuario(), u.getUsername(), "EXITOSO", detalle);
        Auditoria.registrar(u, "Sesión", "Inicio de sesión de " + u.getNombreCompleto());
    }

    private void registrar(Integer id, String username, String resultado, String detalle) {
        try {
            bitacora.registrar(id, username, resultado, detalle);
        } catch (SQLException e) {
            Log.error("No se pudo registrar en la bitácora de accesos", e);
        }
    }
}
