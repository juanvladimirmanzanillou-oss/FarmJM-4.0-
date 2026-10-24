package com.unev.farmjm.service;

/** Error de negocio o de acceso a datos con un mensaje apto para mostrar al usuario. */
public class ServicioException extends RuntimeException {

    public ServicioException(String mensaje) { super(mensaje); }

    public ServicioException(String mensaje, Throwable causa) { super(mensaje, causa); }
}
