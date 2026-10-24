package com.unev.farmjm.ui;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

/** Diseño adaptable: escucha el ancho de la ventana y publica el modo (escritorio, tableta o teléfono). */
public final class Responsive {

    public static final ObjectProperty<Modo> modo = new SimpleObjectProperty<>(Modo.ESCRITORIO);

    private Responsive() { }

    /** Conecta la escena: al cambiar su ancho se actualiza el modo y la clase CSS de la raíz. */
    public static void instalar(Scene scene) {
        Runnable aplicar = () -> {
            Modo m = Modo.de(scene.getWidth());
            modo.set(m);
            if (scene.getRoot() != null) clase(scene.getRoot(), m);
        };
        scene.widthProperty().addListener((o, a, b) -> aplicar.run());
        scene.rootProperty().addListener((o, a, b) -> aplicar.run());
        aplicar.run();
    }

    /** Deja en el nodo solo la clase CSS del modo actual (.modo-escritorio, .modo-tableta, .modo-telefono). */
    public static void clase(Node nodo, Modo m) {
        for (Modo x : Modo.values()) nodo.getStyleClass().remove(x.clase);
        nodo.getStyleClass().add(m.clase);
    }

    /** Ejecuta la acción ahora y cada vez que cambie el modo. */
    public static void alCambiar(Consumer<Modo> accion) {
        accion.accept(modo.get());
        modo.addListener((o, a, b) -> accion.accept(b));
    }

    /** Tabla de muchas columnas: tableta muestra {@code paraTableta}; teléfono, solo {@code paraTelefono}; escritorio, todas. */
    public static void columnasPrincipales(TableView<?> tabla, String[] paraTableta, String[] paraTelefono) {
        Set<String> tab = new HashSet<>(Arrays.asList(paraTableta));
        Set<String> tel = new HashSet<>(Arrays.asList(paraTelefono));
        alCambiar(m -> {
            for (TableColumn<?, ?> c : tabla.getColumns()) {
                c.setVisible(m == Modo.ESCRITORIO || (m == Modo.TABLETA ? tab : tel).contains(c.getText()));
            }
        });
    }

    /** En teléfono solo se muestran las columnas principales de la tabla. */
    public static void columnasPrincipales(TableView<?> tabla, String... principales) {
        Set<String> clave = new HashSet<>(Arrays.asList(principales));
        alCambiar(m -> {
            for (TableColumn<?, ?> c : tabla.getColumns()) {
                c.setVisible(m != Modo.TELEFONO || clave.contains(c.getText()));
            }
        });
    }
}
