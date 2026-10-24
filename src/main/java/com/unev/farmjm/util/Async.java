package com.unev.farmjm.util;

import com.unev.farmjm.service.ServicioException;
import javafx.concurrent.Task;

import java.util.concurrent.Callable;
import java.util.function.Consumer;

/** Ejecuta trabajo pesado (base de datos, PDF) en un hilo de fondo con Task, sin congelar la interfaz. */
public final class Async {

    private Async() { }

    public static <T> void run(Callable<T> trabajo, Consumer<T> alTerminar, Consumer<Throwable> alFallar) {
        Task<T> tarea = new Task<>() {
            @Override
            protected T call() throws Exception {
                return trabajo.call();
            }
        };
        tarea.setOnSucceeded(e -> alTerminar.accept(tarea.getValue()));
        tarea.setOnFailed(e -> alFallar.accept(tarea.getException()));
        Thread hilo = new Thread(tarea, "farmjm-tarea");
        hilo.setDaemon(true);
        hilo.start();
    }

    /** Mensaje apto para el usuario; los errores inesperados se registran en el log. */
    public static String mensaje(Throwable t) {
        if (t instanceof ServicioException) return t.getMessage();
        Log.error("Error inesperado", t);
        return "Ocurrió un error inesperado. Detalle en logs/farmjm.log.";
    }
}
