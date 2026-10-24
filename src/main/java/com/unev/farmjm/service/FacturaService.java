package com.unev.farmjm.service;

import com.unev.farmjm.dao.CatalogoDAO;
import com.unev.farmjm.dao.FacturaDAO;
import com.unev.farmjm.model.*;
import com.unev.farmjm.security.Accion;
import com.unev.farmjm.security.Modulo;
import com.unev.farmjm.security.Permisos;
import com.unev.farmjm.util.Fmt;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

/** Procesa las ventas del punto de venta. */
public class FacturaService {

    private final FacturaDAO dao = new FacturaDAO();
    private final CatalogoDAO catalogo = new CatalogoDAO();

    public List<ComprobanteFiscal> comprobantes() {
        try {
            return catalogo.comprobantes();
        } catch (SQLException e) {
            throw Errores.bd("cargar comprobantes", e);
        }
    }

    public List<Descuento> descuentos() {
        try {
            return catalogo.descuentos();
        } catch (SQLException e) {
            throw Errores.bd("cargar descuentos", e);
        }
    }

    /** Guarda la factura (con su NCF, pago y descuento de stock) y la devuelve con su número asignado. */
    public Factura procesar(Usuario actor, Factura f, ComprobanteFiscal tipo, String metodoPago, Caja caja) {
        Permisos.exigir(actor, Modulo.POS, Accion.CREAR);
        if (caja == null || !caja.isAbierta() || caja.getIdCaja() == 0) {
            throw new ServicioException("Debe abrir la caja antes de facturar.");
        }
        if (f.getDetalles().isEmpty()) throw new ServicioException("Seleccione al menos un medicamento.");
        f.setUsuario(actor);
        f.setCaja(caja);
        f.setFecha(LocalDateTime.now());
        f.setComprobante(tipo);
        f.calcularTotales();
        try {
            dao.guardar(f, metodoPago);
        } catch (SQLException e) {
            throw Errores.bd("procesar factura", e);
        }
        Auditoria.registrar(actor, "Facturación", "Factura " + f.getNumero() + " (NCF " + f.getComprobante().getSecuencia()
                + ") por " + Fmt.dinero(f.getTotal()));
        return f;
    }
}
