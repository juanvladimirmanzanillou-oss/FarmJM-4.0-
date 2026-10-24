package com.unev.farmjm.security;

import org.mindrot.jbcrypt.BCrypt;

/** Contraseñas con hash BCrypt (jBCrypt). Nunca se guardan en texto plano. */
public final class PasswordUtil {

    private PasswordUtil() { }

    public static String hash(String clave) {
        return BCrypt.hashpw(clave, BCrypt.gensalt(10));
    }

    public static boolean verificar(String clave, String hash) {
        if (clave == null || hash == null) return false;
        try {
            return BCrypt.checkpw(clave, hash);
        } catch (IllegalArgumentException e) {
            return false;   // hash con formato inválido
        }
    }

    /** Política mínima: 8 caracteres, al menos una letra y un número. Devuelve el error o null si es válida. */
    public static String validarPolitica(String clave) {
        if (clave == null || clave.length() < 8) return "La contraseña debe tener al menos 8 caracteres.";
        boolean letra = clave.chars().anyMatch(Character::isLetter);
        boolean numero = clave.chars().anyMatch(Character::isDigit);
        if (!letra || !numero) return "La contraseña debe incluir letras y números.";
        return null;
    }
}
