package com.unev.farmjm.service;

import com.unev.farmjm.dao.DevolucionDAO;
import com.unev.farmjm.model.Factura;
import com.unev.farmjm.model.Medicamento;
import com.unev.farmjm.model.Usuario;
import com.unev.farmjm.security.Accion;
import com.unev.farmjm.security.Modulo;
import com.unev.farmjm.security.Permisos;
import com.unev.farmjm.util.Fmt;

import java.sql.SQLException;

/** Emite notas de crédito y regresa las unidades al inventario. */
public class DevolucionService {

    private final DevolucionDAO dao = new DevolucionDAO();

    /** @return NCF de la nota de crédito; el monto incluye el ITBIS. */
    public Resultado emitir(Usuario actor, String numeroFactura, Medicamento m, int cantidad, String motivo) {
        Permisos.exigir(actor, Modulo.DEVOLUCIONES, Accion.CREAR);
        if (numeroFactura == null || numeroFactura.isBlank() || m == null || motivo == null || motivo.isBlank()) {
            throw new ServicioException("Indique la factura, el medicamento y el motivo.");
        }
        if (cantidad <= 0) throw new ServicioException("La cantidad debe ser mayor que cero.");
        try {
            int idFactura = dao.idFactura(numeroFactura.trim());
            if (idFactura == 0) throw new ServicioException("La factura " + numeroFactura.trim() + " no existe.");
            double[] r = dao.resumen(idFactura, m.getIdProducto());
            if (r[0] == 0) throw new ServicioException(m.getNombre() + " no fue vendido en la factura " + numeroFactura.trim() + ".");
            double disponible = r[0] - r[1];
            if (cantidad > disponible) {
                throw new ServicioException("Solo se pueden devolver " + (int) disponible + " unidad(es) de "
                        + m.getNombre() + " en esa factura.");
            }
            double monto = r[2] * cantidad * (1 + Factura.TASA_ITBIS);
            String ncf = dao.registrar(idFactura, m.getIdProducto(), cantidad, motivo.trim(), monto);
            Auditoria.registrar(actor, "Devoluciones", "Nota de crédito " + ncf + " por " + Fmt.dinero(monto)
                    + " (" + m.getNombre() + " x" + cantidad + ")");
            return new Resultado(ncf, monto);
        } catch (SQLException e) {
            throw Errores.bd("emitir la devolución", e);
        }
    }

    public record Resultado(String ncf, double monto) { }
}
