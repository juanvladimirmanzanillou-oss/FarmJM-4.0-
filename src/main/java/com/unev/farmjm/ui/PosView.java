package com.unev.farmjm.ui;

import com.unev.farmjm.Estado;
import com.unev.farmjm.model.*;
import com.unev.farmjm.service.FacturaService;
import com.unev.farmjm.util.Async;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;


/** Punto de Venta (POS): catálogo de medicamentos + orden de venta. */
public class PosView extends StackPane implements Refrescable {

    private final FacturaService servicio = new FacturaService();
    private final VBox catalogo;
    private final VBox orden;
    private ScrollPane scrollCarrito;
    private Button btnProcesar;
    private int columnas = 3;

    private final GridPane grid = new GridPane();
    private final TextField txtBuscar = new TextField();
    private final ComboBox<String> cmbCategoria = new ComboBox<>();
    private final VBox listaCarrito = new VBox();
    private final ComboBox<ComprobanteFiscal> cmbNcf = new ComboBox<>();
    private final ComboBox<Descuento> cmbDescuento = new ComboBox<>();
    private final Label lblSubtotal = Ui.label("RD$ 0.00", "tot-value");
    private final Label lblDescuento = Ui.label("- RD$ 0.00", "tot-discount");
    private final Label lblItbis = Ui.label("RD$ 0.00", "tot-value");
    private final Label lblTotal = Ui.label("RD$ 0.00", "tot-big-value");
    private final Button btnEfectivo = new Button("Efectivo");
    private final Button btnTarjeta = new Button("Tarjeta");
    private final Button btnMixto = new Button("Mixto");
    private String metodoPago = "Efectivo";
    private Factura factura = new Factura();   // la orden de venta actual es un objeto Factura

    public PosView() {
        catalogo = construirCatalogo();
        orden = construirOrden();
        Responsive.alCambiar(this::aplicarModo);   // 3 columnas en escritorio, 2 en tableta/teléfono; carrito debajo en teléfono
        refrescar();
    }

    // ------------------------------------------------------------ catálogo
    private VBox construirCatalogo() {
        txtBuscar.setPromptText("Buscar medicamento por nombre, código o principio activo...");
        txtBuscar.getStyleClass().add("search");
        HBox.setHgrow(txtBuscar, Priority.ALWAYS);
        txtBuscar.textProperty().addListener((o, a, b) -> construirGrid());

        cmbCategoria.getStyleClass().add("combo-clean");
        cmbCategoria.setPrefWidth(210);
        cmbCategoria.setOnAction(e -> construirGrid());

        grid.setHgap(14);
        grid.setVgap(14);
        grid.setPadding(new Insets(2, 8, 8, 2));

        ScrollPane sp = new ScrollPane(grid);
        sp.setFitToWidth(true);
        VBox.setVgrow(sp, Priority.ALWAYS);

        VBox panel = new VBox(14, new HBox(12, txtBuscar, cmbCategoria), sp);
        panel.getStyleClass().add("panel");
        panel.setPadding(new Insets(16));
        HBox.setHgrow(panel, Priority.ALWAYS);
        return panel;
    }

    private void construirGrid() {
        grid.getChildren().clear();
        String q = txtBuscar.getText() == null ? "" : txtBuscar.getText().toLowerCase().trim();
        String cat = cmbCategoria.getValue();
        int i = 0;
        for (Medicamento m : Estado.medicamentos) {
            boolean texto = q.isEmpty() || m.getNombre().toLowerCase().contains(q)
                    || m.getCodigo().toLowerCase().contains(q)
                    || m.getPrincipioActivo().toLowerCase().contains(q);
            boolean categoria = cat == null || cat.startsWith("Todas") || m.getCategoria().getNombre().equals(cat);
            if (texto && categoria) {
                grid.add(tarjeta(m), i % columnas, i / columnas);
                i++;
            }
        }
    }

    private VBox tarjeta(Medicamento m) {
        Label receta = Ui.label("RECETA", "badge", "badge-receta");
        receta.setVisible(m.isRequiereReceta());
        receta.setManaged(m.isRequiereReceta());
        HBox arriba = new HBox(Ui.label(m.getCodigo(), "card-code"), Ui.espacio(), receta);
        arriba.setAlignment(Pos.CENTER_LEFT);

        Separator sep = new Separator();
        sep.setPadding(new Insets(4, 0, 2, 0));
        HBox pie = new HBox(Ui.label(Ui.dinero(m.getPrecio()), "card-price"), Ui.espacio(), Ui.badgeLote(m.getLoteProximo()));
        pie.setAlignment(Pos.CENTER_LEFT);

        VBox card = new VBox(3, arriba, Ui.label(m.getNombre(), "card-name"), Ui.label(m.getPrincipioActivo(), "card-sub"),
                sep, pie, Ui.label("Stock: " + m.getStock(), "card-stock"));
        card.getStyleClass().add("card");
        card.setMaxWidth(Double.MAX_VALUE);
        card.setOnMouseClicked(e -> agregar(m));
        return card;
    }

    // ------------------------------------------------------------ orden de venta
    private VBox construirOrden() {
        Button limpiar = new Button("Limpiar");
        limpiar.getStyleClass().add("cart-clear");
        limpiar.setOnAction(e -> nuevaFactura());
        HBox titulo = new HBox(8, Ui.label("🛍", "cart-title"), Ui.label("Orden de Venta", "cart-title"), Ui.espacio(), limpiar);
        titulo.setAlignment(Pos.CENTER_LEFT);

        cmbNcf.getItems().setAll(Estado.comprobantes);
        cmbNcf.getSelectionModel().select(1);   // B02 - Consumidor Final
        cmbNcf.getStyleClass().add("ncf-combo");
        cmbNcf.setMaxWidth(Double.MAX_VALUE);

        VBox header = new VBox(10, titulo, Ui.label("TIPO COMPROBANTE FISCAL (NCF)", "cart-label"), cmbNcf);
        header.getStyleClass().add("cart-header");
        header.setPadding(new Insets(16));

        listaCarrito.setPadding(new Insets(4, 12, 4, 12));
        scrollCarrito = new ScrollPane(listaCarrito);
        scrollCarrito.setFitToWidth(true);
        VBox.setVgrow(scrollCarrito, Priority.ALWAYS);

        cmbDescuento.getItems().setAll(Estado.descuentos);
        cmbDescuento.getSelectionModel().selectFirst();
        cmbDescuento.setOnAction(e -> refrescarCarrito());
        cmbDescuento.setMaxWidth(150);

        HBox fSub = new HBox(Ui.label("Subtotal:", "tot-label"), Ui.espacio(), lblSubtotal);
        HBox fDesc = new HBox(8, Ui.label("Descuento:", "tot-label"), cmbDescuento, Ui.espacio(), lblDescuento);
        fDesc.setAlignment(Pos.CENTER_LEFT);
        HBox fItbis = new HBox(Ui.label("ITBIS (18%):", "tot-label"), Ui.espacio(), lblItbis);
        HBox fTotal = new HBox(Ui.label("TOTAL A PAGAR:", "tot-big"), Ui.espacio(), lblTotal);
        fTotal.setAlignment(Pos.CENTER_LEFT);
        VBox totales = new VBox(8, fSub, fDesc, fItbis, new Separator(), fTotal);
        totales.getStyleClass().add("totals");
        totales.setPadding(new Insets(14, 16, 10, 16));

        for (Button b : new Button[]{btnEfectivo, btnTarjeta, btnMixto}) {
            b.getStyleClass().add("pay-btn");
            b.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(b, Priority.ALWAYS);
            b.setOnAction(e -> { metodoPago = b.getText(); marcarPago(); });
        }
        marcarPago();
        HBox pagos = new HBox(8, btnEfectivo, btnTarjeta, btnMixto);

        btnProcesar = new Button("🖨  PROCESAR FACTURA [F2]");
        btnProcesar.getStyleClass().add("process-btn");
        btnProcesar.setMaxWidth(Double.MAX_VALUE);
        btnProcesar.setOnAction(e -> procesar());

        VBox pie = new VBox(10, pagos, btnProcesar);
        pie.setPadding(new Insets(0, 16, 16, 16));
        pie.getStyleClass().add("totals-pie");

        VBox panel = new VBox(header, scrollCarrito, totales, pie);
        panel.getStyleClass().add("panel");
        return panel;
    }

    private void marcarPago() {
        for (Button b : new Button[]{btnEfectivo, btnTarjeta, btnMixto}) {
            b.getStyleClass().remove("pay-active");
            if (b.getText().equals(metodoPago)) b.getStyleClass().add("pay-active");
        }
    }

    private int cantidadEnCarrito(Medicamento m) {
        for (DetalleFactura d : factura.getDetalles()) if (d.getProducto() == m) return d.getCantidad();
        return 0;
    }

    private void agregar(Medicamento m) {
        if (!Estado.cajaAbierta.get()) {
            Ui.info("Caja cerrada", "Debe abrir la caja antes de facturar.");
            return;
        }
        int enCarrito = cantidadEnCarrito(m);
        if (enCarrito >= m.getStock()) {
            Ui.info("Stock insuficiente", "No hay más unidades disponibles de " + m.getNombre() + ".");
            return;
        }
        if (m.isRequiereReceta() && enCarrito == 0) {
            String tipo = m.isControlado() ? "medicamento CONTROLADO" : "medicamento con receta";
            if (!Ui.confirmar("Receta requerida", m.getNombre() + " es un " + tipo + ".\n¿Se verificó la receta del paciente?")) return;
            Estado.log("Recetas", "Receta verificada para " + m.getNombre());
        }
        for (DetalleFactura d : factura.getDetalles()) {
            if (d.getProducto() == m) {
                d.cambiarCantidad(d.getCantidad() + 1);
                refrescarCarrito();
                return;
            }
        }
        factura.agregarDetalle(new DetalleFactura(factura.getDetalles().size() + 1, m, m.getLoteProximo(), 1, m.getPrecio(), m.getPrecio()));
        refrescarCarrito();
    }

    private void refrescarCarrito() {
        listaCarrito.getChildren().clear();
        if (factura.getDetalles().isEmpty()) {
            VBox vacio = new VBox(8, Ui.label("🛒", "cart-empty-icon"), Ui.label("Carrito vacío. Selecciona productos", "cart-empty"));
            vacio.setAlignment(Pos.CENTER);
            vacio.setPadding(new Insets(40, 0, 40, 0));
            listaCarrito.getChildren().add(vacio);
        }
        for (DetalleFactura d : new java.util.ArrayList<>(factura.getDetalles())) {
            Label nombre = Ui.label(d.getProducto().getNombre(), "cart-item-name");
            nombre.setMinWidth(0);
            VBox info = new VBox(2, nombre, Ui.label(Ui.dinero(d.getPrecioUnitario()) + " c/u", "cart-item-sub"));
            HBox.setHgrow(info, Priority.ALWAYS);

            Button menos = new Button("−");
            Button mas = new Button("+");
            menos.getStyleClass().add("qty-btn");
            mas.getStyleClass().add("qty-btn");
            menos.setOnAction(e -> {
                if (d.getCantidad() > 1) d.cambiarCantidad(d.getCantidad() - 1); else factura.getDetalles().remove(d);
                refrescarCarrito();
            });
            mas.setOnAction(e -> {
                if (d.getCantidad() < d.getProducto().getStock()) d.cambiarCantidad(d.getCantidad() + 1);
                refrescarCarrito();
            });
            Label cant = new Label(String.valueOf(d.getCantidad()));
            cant.setMinWidth(22);
            cant.setAlignment(Pos.CENTER);
            Label sub = Ui.label(Ui.dinero(d.getSubtotal()), "cart-item-name");
            sub.setMinWidth(78);
            sub.setAlignment(Pos.CENTER_RIGHT);
            Button quitar = new Button("✕");
            quitar.getStyleClass().add("remove-btn");
            quitar.setOnAction(e -> { factura.getDetalles().remove(d); refrescarCarrito(); });

            HBox fila = new HBox(6, info, menos, cant, mas, sub, quitar);
            fila.setAlignment(Pos.CENTER_LEFT);
            fila.getStyleClass().add("cart-row");
            listaCarrito.getChildren().add(fila);
        }
        factura.setDescuento(cmbDescuento.getValue());
        factura.calcularTotales();
        lblSubtotal.setText(Ui.dinero(factura.getSubtotal()));
        lblDescuento.setText("- " + Ui.dinero(factura.getDescuentoAplicado()));
        lblItbis.setText(Ui.dinero(factura.getItbis()));
        lblTotal.setText(Ui.dinero(factura.getTotal()));
    }

    private void nuevaFactura() {
        factura = new Factura();
        cmbDescuento.getSelectionModel().selectFirst();
        refrescarCarrito();
        construirGrid();
    }

    /** Procesa la factura: asigna NCF, descuenta stock, registra el pago y deja constancia en el log (todo en MySQL). */
    public void procesar() {
        if (!Estado.cajaAbierta.get()) {
            Ui.info("Caja cerrada", "Debe abrir la caja antes de facturar.");
            return;
        }
        if (factura.getDetalles().isEmpty()) {
            Ui.info("Carrito vacío", "Seleccione al menos un medicamento.");
            return;
        }
        final Factura aProcesar = factura;
        final ComprobanteFiscal tipo = cmbNcf.getValue();
        final String metodo = metodoPago;
        aProcesar.setDescuento(cmbDescuento.getValue());
        aProcesar.calcularTotales();
        btnProcesar.setDisable(true);
        Async.run(() -> servicio.procesar(Estado.usuario, aProcesar, tipo, metodo, Estado.caja),
                f -> {
                    for (DetalleFactura d : f.getDetalles()) {
                        Producto p = d.getProducto();
                        p.setStock(p.getStock() - d.getCantidad());
                        if (d.getLote() != null) d.getLote().setCantidad(Math.max(0, d.getLote().getCantidad() - d.getCantidad()));
                    }
                    if ("Efectivo".equals(metodo)) Estado.ventasEfectivo += f.getTotal();
                    btnProcesar.setDisable(false);
                    Ui.info("Factura procesada",
                            "Factura: " + f.getNumero() + "\nNCF: " + f.getComprobante().getSecuencia()
                                    + "\nMétodo de pago: " + metodo + "\nTotal: " + Ui.dinero(f.getTotal()));
                    nuevaFactura();
                },
                ex -> {
                    btnProcesar.setDisable(false);
                    Ui.error(Async.mensaje(ex));
                });
    }

    /** Escritorio: 3 columnas + orden lateral. Tableta: 2 columnas y orden más angosta. Teléfono: 1 columna y la orden debajo, con scroll. */
    private void aplicarModo(Modo m) {
        columnas = m == Modo.ESCRITORIO ? 3 : m == Modo.TABLETA ? 2 : 1;
        grid.getColumnConstraints().clear();
        for (int i = 0; i < columnas; i++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPercentWidth(100.0 / columnas);
            cc.setHgrow(Priority.ALWAYS);
            grid.getColumnConstraints().add(cc);
        }
        if (m == Modo.TELEFONO) {
            orden.setMinWidth(0);
            orden.setPrefWidth(Region.USE_COMPUTED_SIZE);
            orden.setMaxWidth(Double.MAX_VALUE);
            catalogo.setMinHeight(430);
            catalogo.setPrefHeight(430);
            scrollCarrito.setMinHeight(170);
            scrollCarrito.setPrefHeight(240);
            VBox.setVgrow(scrollCarrito, Priority.NEVER);
            ScrollPane sp = new ScrollPane(new VBox(14, catalogo, orden));
            sp.setFitToWidth(true);
            sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
            getChildren().setAll(sp);
        } else {
            double w = m == Modo.ESCRITORIO ? 400 : 330;
            orden.setPrefWidth(w);
            orden.setMinWidth(w);
            orden.setMaxWidth(w);
            catalogo.setMinHeight(Region.USE_COMPUTED_SIZE);
            catalogo.setPrefHeight(Region.USE_COMPUTED_SIZE);
            scrollCarrito.setMinHeight(Region.USE_COMPUTED_SIZE);
            scrollCarrito.setPrefHeight(Region.USE_COMPUTED_SIZE);
            VBox.setVgrow(scrollCarrito, Priority.ALWAYS);
            getChildren().setAll(new HBox(16, catalogo, orden));
        }
        construirGrid();
    }

    @Override
    public void refrescar() {
        String actual = cmbCategoria.getValue();
        cmbCategoria.getItems().setAll("Todas las Categorías");
        Estado.medicamentos.stream().map(m -> m.getCategoria().getNombre()).distinct().sorted()
                .forEach(cmbCategoria.getItems()::add);
        cmbCategoria.setValue(actual != null && cmbCategoria.getItems().contains(actual) ? actual : "Todas las Categorías");
        construirGrid();
        refrescarCarrito();
    }
}
