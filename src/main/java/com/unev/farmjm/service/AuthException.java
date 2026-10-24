package com.unev.farmjm.service;

/** Error de autenticación. {@code volverAlLogin} indica que la cuenta se bloqueó y debe reiniciarse el proceso. */
public class AuthException extends ServicioException {

    private final boolean volverAlLogin;

    public AuthException(String mensaje, boolean volverAlLogin) {
        super(mensaje);
        this.volverAlLogin = volverAlLogin;
    }

    public boolean volverAlLogin() { return volverAlLogin; }
}
