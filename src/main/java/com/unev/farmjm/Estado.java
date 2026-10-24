package com.unev.farmjm;

import com.unev.farmjm.dao.DemoData;
import com.unev.farmjm.dao.LogDAO;
import com.unev.farmjm.model.*;
import com.unev.farmjm.security.Accion;
import com.unev.farmjm.security.Modulo;
import com.unev.farmjm.security.Permisos;
import com.unev.farmjm.service.Auditoria;
import com.unev.farmjm.service.CajaService;
import com.unev.farmjm.service.FacturaService;
import com.unev.farmjm.service.MedicamentoService;
import com.unev.farmjm.util.Async;
import com.unev.farmjm.util.Log;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Estado compartido de la sesión (usuario, caja y listas observables). Todos los datos vienen de MySQL. */
public class Estado {
    public static Usuario usuario;
    public static Caja caja;
    public static final BooleanProperty cajaAbierta = new SimpleBooleanProperty(false);
    public static boolean conectadoBD = false;
    public static double ventasEfectivo = 0;

    public static final ObservableList<Medicamento> medicamentos = FXCollections.observableArrayList();
    public static final ObservableList<LogActividad> logs = FXCollections.observableArrayList();
    public static final ObservableList<Receta> recetas = FXCollections.observableArrayList();
    public static final ObservableList<Devolucion> devoluciones = FXCollections.observableArrayList();
    public static List<ComprobanteFiscal> comprobantes = new ArrayList<>();
    public static List<Descuento> descuentos = new ArrayList<>();

    /** Datos leídos de la base de datos en segundo plano tras iniciar sesión. */
    public record Carga(Usuario usuario, Caja caja, double efectivo, List<Medicamento> medicamentos,
                        List<ComprobanteFiscal> comprobantes, List<Descuento> descuentos, List<LogActividad> logs) { }

    /** Se ejecuta en un hilo de fondo (Task). */
    public static Carga cargar(Usuario u) {
        CajaService cajas = new CajaService();
        Caja c = cajas.cargar(u);
        List<Medicamento> meds = Permisos.puede(u, Modulo.INVENTARIO, Accion.VER)
                ? new MedicamentoService().listar(u) : new ArrayList<>();
        FacturaService fs = new FacturaService();
        List<LogActividad> l = new ArrayList<>();
        try {
            l = new LogDAO().recientes(200);
        } catch (SQLException e) {
            Log.error("No se pudo cargar el log de actividad", e);
        }
        return new Carga(u, c, cajas.efectivo(c), meds, fs.comprobantes(), fs.descuentos(), l);
    }

    /** Se ejecuta en el hilo de JavaFX. */
    public static void aplicar(Carga c) {
        usuario = c.usuario();
        caja = c.caja();
        ventasEfectivo = c.efectivo();
        cajaAbierta.set(caja.isAbierta());
        comprobantes = c.comprobantes();
        descuentos = c.descuentos();
        medicamentos.setAll(c.medicamentos());
        logs.setAll(c.logs());
        recetas.setAll(DemoData.recetas(medicamentos));
        devoluciones.clear();
        conectadoBD = true;
        Auditoria.alRegistrar = l -> Platform.runLater(() -> logs.add(0, l));
    }

    public static void cerrarSesion() {
        if (usuario != null) Auditoria.registrar(usuario, "Sesión", "Cierre de sesión de " + usuario.getNombreCompleto());
        Auditoria.alRegistrar = l -> { };
        usuario = null;
        caja = null;
        cajaAbierta.set(false);
        medicamentos.clear();
        logs.clear();
        recetas.clear();
        devoluciones.clear();
        conectadoBD = false;
    }

    /** Registra una acción en la auditoría sin bloquear la interfaz. */
    public static void log(String modulo, String accion) {
        Usuario u = usuario;
        if (u == null) return;
        Async.run(() -> { Auditoria.registrar(u, modulo, accion); return Boolean.TRUE; }, ok -> { }, ex -> { });
    }

    /** Lotes vencidos o que vencen en 90 días o menos. */
    public static List<AlertaVencimiento> alertas() {
        List<AlertaVencimiento> l = new ArrayList<>();
        for (Medicamento m : medicamentos) {
            Lote lote = m.getLoteProximo();
            if (lote != null && lote.getDiasParaVencer() <= 90) {
                l.add(new AlertaVencimiento(l.size() + 1, lote, (int) lote.getDiasParaVencer(), false));
            }
        }
        return l;
    }
}
