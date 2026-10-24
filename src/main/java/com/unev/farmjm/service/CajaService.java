package com.unev.farmjm.service;

import com.unev.farmjm.dao.CajaDAO;
import com.unev.farmjm.model.ArqueoCaja;
import com.unev.farmjm.model.Caja;
import com.unev.farmjm.model.Usuario;
import com.unev.farmjm.security.Accion;
import com.unev.farmjm.security.Modulo;
import com.unev.farmjm.security.Permisos;
import com.unev.farmjm.util.Fmt;

import java.sql.SQLException;
import java.time.LocalDateTime;

/** Apertura y cierre de caja con arqueo. */
public class CajaService {

    public static final double MONTO_BASE = 5000.00;
    private final CajaDAO dao = new CajaDAO();

    /** Última caja del usuario; si nunca abrió una, devuelve una caja cerrada sin guardar (id 0). */
    public Caja cargar(Usuario u) {
        try {
            Caja c = dao.ultimaDe(u);
            return c != null ? c : new Caja(0, u, MONTO_BASE, LocalDateTime.now(), null, false);
        } catch (SQLException e) {
            throw Errores.bd("cargar caja", e);
        }
    }

    public double efectivo(Caja c) {
        if (c.getIdCaja() == 0) return 0;
        try {
            return dao.efectivoDeCaja(c.getIdCaja());
        } catch (SQLException e) {
            throw Errores.bd("calcular efectivo de caja", e);
        }
    }

    public Caja abrir(Usuario actor) {
        Permisos.exigir(actor, Modulo.CAJA, Accion.CREAR);
        Caja c;
        try {
            c = dao.abrir(actor, MONTO_BASE);
        } catch (SQLException e) {
            throw Errores.bd("abrir caja", e);
        }
        Auditoria.registrar(actor, "Caja", "Apertura de caja #" + String.format("%03d", c.getIdCaja())
                + " con base " + Fmt.dinero(MONTO_BASE));
        return c;
    }

    public ArqueoCaja cerrar(Usuario actor, Caja caja, double efectivoVentas, double contado) {
        Permisos.exigir(actor, Modulo.CAJA, Accion.EDITAR);
        if (!caja.isAbierta()) throw new ServicioException("La caja ya está cerrada.");
        if (contado < 0) throw new ServicioException("El monto contado no puede ser negativo.");
        double esperado = caja.getMontoBase() + efectivoVentas;
        ArqueoCaja a = new ArqueoCaja(0, caja, esperado, contado, contado - esperado, LocalDateTime.now());
        try {
            dao.cerrar(caja, a);
        } catch (SQLException e) {
            throw Errores.bd("cerrar caja", e);
        }
        Auditoria.registrar(actor, "Caja", "Cierre de caja. Esperado " + Fmt.dinero(esperado) + ", contado "
                + Fmt.dinero(contado) + ", diferencia " + Fmt.dinero(a.getDiferencia()));
        return a;
    }
}
