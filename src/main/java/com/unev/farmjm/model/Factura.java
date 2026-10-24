package com.unev.farmjm.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Clase del modelo: Factura. */
public class Factura {

    // Atributos privados (encapsulamiento)
    private int idFactura;
    private String numero;
    private LocalDateTime fecha;
    private Cliente cliente;
    private Usuario usuario;
    private Caja caja;
    private ComprobanteFiscal comprobante;
    private Descuento descuento;
    private double subtotal;
    private double descuentoAplicado;
    private double itbis;
    private double total;
    private List<DetalleFactura> detalles = new ArrayList<>();

    // Constructor vacío
    public Factura() { }

    // Constructor con parámetros
    public Factura(int idFactura, String numero, LocalDateTime fecha, Cliente cliente, Usuario usuario, Caja caja, ComprobanteFiscal comprobante, Descuento descuento, double subtotal, double descuentoAplicado, double itbis, double total) {
        this.idFactura = idFactura;
        this.numero = numero;
        this.fecha = fecha;
        this.cliente = cliente;
        this.usuario = usuario;
        this.caja = caja;
        this.comprobante = comprobante;
        this.descuento = descuento;
        this.subtotal = subtotal;
        this.descuentoAplicado = descuentoAplicado;
        this.itbis = itbis;
        this.total = total;
    }

    // Getters y setters
    public int getIdFactura() { return idFactura; }
    public void setIdFactura(int idFactura) { this.idFactura = idFactura; }
    public String getNumero() { return numero; }
    public void setNumero(String numero) { this.numero = numero; }
    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }
    public Cliente getCliente() { return cliente; }
    public void setCliente(Cliente cliente) { this.cliente = cliente; }
    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }
    public Caja getCaja() { return caja; }
    public void setCaja(Caja caja) { this.caja = caja; }
    public ComprobanteFiscal getComprobante() { return comprobante; }
    public void setComprobante(ComprobanteFiscal comprobante) { this.comprobante = comprobante; }
    public Descuento getDescuento() { return descuento; }
    public void setDescuento(Descuento descuento) { this.descuento = descuento; }
    public double getSubtotal() { return subtotal; }
    public void setSubtotal(double subtotal) { this.subtotal = subtotal; }
    public double getDescuentoAplicado() { return descuentoAplicado; }
    public void setDescuentoAplicado(double descuentoAplicado) { this.descuentoAplicado = descuentoAplicado; }
    public double getItbis() { return itbis; }
    public void setItbis(double itbis) { this.itbis = itbis; }
    public double getTotal() { return total; }
    public void setTotal(double total) { this.total = total; }
    public List<DetalleFactura> getDetalles() { return detalles; }
    public void setDetalles(List<DetalleFactura> detalles) { this.detalles = detalles; }

    // Métodos auxiliares
    public static final double TASA_ITBIS = 0.18;

    public void agregarDetalle(DetalleFactura d) { detalles.add(d); }

    /** Recalcula subtotal, descuento, ITBIS (18%) y total a partir de los detalles. */
    public void calcularTotales() {
        subtotal = 0;
        for (DetalleFactura d : detalles) subtotal += d.getSubtotal();
        descuentoAplicado = (descuento != null) ? subtotal * descuento.getPorcentaje() / 100.0 : 0;
        itbis = (subtotal - descuentoAplicado) * TASA_ITBIS;
        total = subtotal - descuentoAplicado + itbis;
    }

    @Override
    public String toString() {
        return numero;
    }
}
