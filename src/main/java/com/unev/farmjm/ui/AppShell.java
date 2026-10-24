package com.unev.farmjm.ui;

import com.unev.farmjm.Estado;
import com.unev.farmjm.security.Accion;
import com.unev.farmjm.security.Modulo;
import com.unev.farmjm.security.Permisos;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Ventana principal: menú + barra superior + contenido.
 * El menú solo muestra los módulos que el rol puede ver y se adapta al tamaño:
 * escritorio (menú lateral completo), tableta (menú lateral compacto) y teléfono (barra inferior).
 */
public class AppShell extends BorderPane {

    public static final String DASHBOARD = "Dashboard";
    public static final String POS = "Punto de Venta (POS)";
    public static final String INVENTARIO = "Inventario & Lotes";
    public static final String CLIENTES = "Clientes";
    public static final String CAJA = "Arqueo & Cierre Caja";
    public static final String RECETAS = "Recetas / Controlados";
    public static final String DEVOLUCIONES = "Devoluciones (NC)";
    public static final String REPORTES = "Reportes";
    public static final String USUARIOS = "Usuarios y Roles";
    public static final String AUDITORIA = "Auditoría & Logs";

    private final Map<String, Node> vistas = new LinkedHashMap<>();
    private final Map<String, Button> botones = new LinkedHashMap<>();
    private final List<Node> soloEscritorio = new ArrayList<>();
    private final StackPane contenido = new StackPane();
    private final Label titulo = Ui.label("", "top-title");
    private final Label fecha = Ui.label("", "top-date");
    private final Label badgeInventario = Ui.label("", "nav-badge");
    private final VBox menu = new VBox(6);
    private final HBox barraNav = new HBox(4);
    private final ScrollPane barraInferior = new ScrollPane(barraNav);
    private final HBox atajos = new HBox(10);
    private final Button btnMenu = new Button("☰");
    private final Label estadoBd;
    private Button btnSalir;
    private PosView pos;
    private VBox sidebar;
    private String actual = DASHBOARD;

    public AppShell(Runnable alCerrarSesion) {
        agregar(DASHBOARD, "🏠", Modulo.DASHBOARD, DashboardView::new);
        agregar(POS, "🛒", Modulo.POS, () -> pos = new PosView());
        agregar(INVENTARIO, "📦", Modulo.INVENTARIO, InventarioView::new);
        agregar(CLIENTES, "👥", Modulo.CLIENTES, ClientesView::new);
        agregar(CAJA, "🏛", Modulo.CAJA, CajaView::new);
        agregar(RECETAS, "📄", Modulo.RECETAS, RecetasView::new);
        agregar(DEVOLUCIONES, "↩", Modulo.DEVOLUCIONES, DevolucionesView::new);
        agregar(REPORTES, "📊", Modulo.REPORTES, ReportesView::new);
        agregar(USUARIOS, "🔑", Modulo.USUARIOS, UsuariosView::new);
        agregar(AUDITORIA, "🛡", Modulo.AUDITORIA, AuditoriaView::new);

        estadoBd = Ui.label(Estado.conectadoBD ? "● MySQL conectado" : "● Sin conexión", Estado.conectadoBD ? "status-ok" : "status-demo");
        sidebar = construirSidebar(alCerrarSesion);
        setTop(construirTopbar());
        contenido.setPadding(new Insets(18));
        setCenter(contenido);

        barraInferior.setFitToHeight(true);
        barraInferior.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        barraInferior.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        barraInferior.getStyleClass().add("bottom-scroll");
        barraNav.getStyleClass().add("bottom-nav");
        barraNav.setAlignment(Pos.CENTER_LEFT);

        Responsive.alCambiar(this::aplicarModo);
        mostrar(DASHBOARD);
    }

    private void agregar(String nombre, String icono, Modulo modulo, Supplier<Node> fabrica) {
        if (!Permisos.puede(Estado.usuario, modulo, Accion.VER)) return;   // el menú solo muestra lo permitido
        vistas.put(nombre, fabrica.get());
        botones.put(nombre, navBtn(nombre, icono));
    }

    public void mostrar(String nombre) {
        Node v = vistas.get(nombre);
        if (v == null) return;   // el rol no tiene acceso a ese módulo
        actual = nombre;
        if (v instanceof Refrescable r) r.refrescar();
        contenido.getChildren().setAll(v);
        titulo.setText(nombre);
        botones.forEach((k, b) -> {
            b.getStyleClass().remove("nav-active");
            if (k.equals(nombre)) b.getStyleClass().add("nav-active");
        });
        int alertas = Estado.alertas().size();
        badgeInventario.setText(String.valueOf(alertas));
        badgeInventario.setVisible(alertas > 0);
    }

    /** Atajo F2: en el POS procesa la factura; en otra pantalla vuelve al POS. */
    public void atajoF2() {
        if (pos == null) return;
        if (POS.equals(actual)) pos.procesar(); else mostrar(POS);
    }

    // ------------------------------------------------------------ adaptación al tamaño
    private void aplicarModo(Modo m) {
        boolean compacto = m != Modo.ESCRITORIO;
        boolean telefono = m == Modo.TELEFONO;
        sidebar.getStyleClass().remove("sidebar-compact");
        if (compacto) sidebar.getStyleClass().add("sidebar-compact");
        for (Node n : soloEscritorio) {
            n.setVisible(!compacto);
            n.setManaged(!compacto);
        }
        if (telefono) {
            barraNav.getChildren().setAll(botones.values());
            setLeft(null);
            setBottom(barraInferior);
        } else {
            menu.getChildren().setAll(botones.values());
            setBottom(null);
            setLeft(sidebar);
        }
        atajos.setVisible(!compacto);
        atajos.setManaged(!compacto);
        fecha.setVisible(!telefono);
        fecha.setManaged(!telefono);
        estadoBd.setVisible(!telefono);
        estadoBd.setManaged(!telefono);
        btnMenu.setVisible(!compacto);
        btnMenu.setManaged(!compacto);
        sidebar.setVisible(true);
        sidebar.setManaged(true);
        if (btnSalir != null) btnSalir.setText(compacto ? "⎋" : "⎋ Cerrar sesión");
        contenido.setPadding(new Insets(m == Modo.ESCRITORIO ? 18 : m == Modo.TABLETA ? 14 : 8));
    }

    // ------------------------------------------------------------ barra lateral
    private VBox construirSidebar(Runnable alCerrarSesion) {
        Label logo = Ui.label("✚", "brand-icon");
        VBox titulos = new VBox(1, Ui.label("FARMACIA JM", "brand-title"), Ui.label("Sistema Integral RD", "brand-sub"));
        soloEscritorio.add(titulos);
        HBox marca = new HBox(12, logo, titulos);
        marca.setAlignment(Pos.CENTER_LEFT);
        marca.setPadding(new Insets(4, 6, 8, 6));

        // Tarjeta de caja
        Circle punto = new Circle(4.5);
        Label cajaTitulo = Ui.label("", "caja-title");
        Label cajaSub = Ui.label("", "caja-sub");
        Label cajaBadge = Ui.label("", "caja-badge");
        HBox cajaCard = new HBox(8, punto, new VBox(2, cajaTitulo, cajaSub), Ui.espacio(), cajaBadge);
        cajaCard.setAlignment(Pos.CENTER_LEFT);
        cajaCard.getStyleClass().add("caja-card");
        cajaCard.setPadding(new Insets(11, 10, 11, 12));
        soloEscritorio.add(cajaCard);
        Runnable actualizarCaja = () -> {
            boolean ab = Estado.cajaAbierta.get();
            punto.getStyleClass().removeAll("dot-ok", "dot-bad");
            punto.getStyleClass().add(ab ? "dot-ok" : "dot-bad");
            cajaTitulo.setText(ab ? "Caja Abierta" : "Caja Cerrada");
            cajaSub.setText(String.format(Locale.US, "Monto base: RD$ %,.0f", Estado.caja.getMontoBase()));
            cajaBadge.setText(String.format("#%03d", Estado.caja.getIdCaja()));
        };
        Estado.cajaAbierta.addListener((o, a, b) -> actualizarCaja.run());
        actualizarCaja.run();

        menu.setPadding(new Insets(8, 0, 0, 0));
        menu.getChildren().addAll(botones.values());

        // Tarjeta de usuario
        String iniciales = "" + Estado.usuario.getNombre().charAt(0) + Estado.usuario.getApellido().charAt(0);
        VBox info = new VBox(1, Ui.label(Estado.usuario.getNombreCompleto(), "user-name"),
                Ui.label(Estado.usuario.getRol().getNombre() + (Estado.usuario.getLicencia() == null ? "" : " / " + Estado.usuario.getLicencia()), "user-role"));
        soloEscritorio.add(info);
        HBox usuario = new HBox(10, Ui.label(iniciales, "avatar"), info);
        usuario.setAlignment(Pos.CENTER_LEFT);
        usuario.getStyleClass().add("user-card");
        usuario.setPadding(new Insets(12));
        Button salir = Ui.boton("⎋ Cerrar sesión", "logout-btn", alCerrarSesion);
        btnSalir = salir;
        salir.setMaxWidth(Double.MAX_VALUE);
        salir.setTooltip(new Tooltip("Cerrar sesión"));
        Label version = Ui.label("Farmacia JM v2.0 • RNC: 1-30-88941-2", "version");
        soloEscritorio.add(version);
        VBox pie = new VBox(8, usuario, salir, version);
        pie.setAlignment(Pos.CENTER);

        Region relleno = new Region();
        VBox.setVgrow(relleno, Priority.ALWAYS);
        VBox sb = new VBox(12, marca, cajaCard, menu, relleno, pie);
        sb.getStyleClass().add("sidebar");
        sb.setPadding(new Insets(18, 14, 14, 14));
        return sb;
    }

    private Button navBtn(String nombre, String icono) {
        Label texto = new Label(nombre);
        soloEscritorio.add(texto);
        HBox g = new HBox(10, Ui.label(icono, "nav-icon"), texto);
        if (INVENTARIO.equals(nombre)) {
            g.getChildren().addAll(Ui.espacio(), badgeInventario);
            soloEscritorio.add(badgeInventario);
        }
        g.setAlignment(Pos.CENTER_LEFT);
        Button b = new Button();
        b.setGraphic(g);
        b.getStyleClass().add("nav-btn");
        b.setMaxWidth(Double.MAX_VALUE);
        b.setTooltip(new Tooltip(nombre));
        b.setOnAction(e -> mostrar(nombre));
        return b;
    }

    // ------------------------------------------------------------ barra superior
    private HBox construirTopbar() {
        btnMenu.getStyleClass().add("menu-btn");
        btnMenu.setOnAction(e -> {
            boolean v = !sidebar.isVisible();
            sidebar.setVisible(v);
            sidebar.setManaged(v);
        });

        atajos.getChildren().addAll(kbd("F2", "POS"), kbd("F4", "Caja"), kbd("F8", "Inventario"));
        atajos.getStyleClass().add("shortcuts");
        atajos.setAlignment(Pos.CENTER);
        atajos.setPadding(new Insets(6, 14, 6, 14));

        Label rol = Ui.label(Estado.usuario.getRol().getNombre(), "role-chip");

        HBox top = new HBox(14, btnMenu, titulo, Ui.label("•", "top-date"), fecha, Ui.espacio(), estadoBd, rol, atajos);
        top.setAlignment(Pos.CENTER_LEFT);
        top.getStyleClass().add("topbar");
        top.setPadding(new Insets(10, 22, 10, 18));
        top.setMinHeight(58);

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("d/M/yy, h:mm a", new Locale("es", "DO"));
        Runnable tick = () -> fecha.setText(LocalDateTime.now().format(fmt));
        tick.run();
        Timeline reloj = new Timeline(new KeyFrame(Duration.seconds(20), e -> tick.run()));
        reloj.setCycleCount(Timeline.INDEFINITE);
        reloj.play();
        return top;
    }

    private HBox kbd(String tecla, String texto) {
        HBox h = new HBox(5, Ui.label(tecla, "kbd"), Ui.label(texto, "kbd-label"));
        h.setAlignment(Pos.CENTER);
        return h;
    }
}
