package com.unev.farmjm.service;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.unev.farmjm.dao.BitacoraDAO;
import com.unev.farmjm.dao.ReporteDAO;
import com.unev.farmjm.dao.ReporteDAO.InventarioFila;
import com.unev.farmjm.dao.ReporteDAO.VentaFila;
import com.unev.farmjm.model.Usuario;
import com.unev.farmjm.security.Accion;
import com.unev.farmjm.security.Modulo;
import com.unev.farmjm.security.Permisos;
import com.unev.farmjm.util.Fmt;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Reporte PDF cuyo contenido depende del rol:
 *  Farmacéutico: solo sus ventas · Supervisor: ventas de todos + inventario crítico ·
 *  Administrador: lo anterior + bitácora de accesos. Incluye encabezado y filtro por rango de fechas.
 */
public class ReporteService {

    private static final java.awt.Color PRIMARIO = new java.awt.Color(15, 118, 110);   // igual a -color-primario
    private static final java.awt.Color SUAVE = new java.awt.Color(241, 245, 249);
    private static final Font TITULO = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, PRIMARIO);
    private static final Font SECCION = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, PRIMARIO);
    private static final Font NORMAL = FontFactory.getFont(FontFactory.HELVETICA, 9);
    private static final Font NEGRITA = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9);
    private static final Font CABECERA = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, java.awt.Color.WHITE);

    private final ReporteDAO dao = new ReporteDAO();
    private final BitacoraDAO bitacora = new BitacoraDAO();

    /** Nombre sugerido del archivo según el rol. */
    public String descripcionContenido(Usuario u) {
        String rol = u.getRol().getNombre();
        if (Permisos.ADMINISTRADOR.equals(rol)) return "Ventas de todos los usuarios, inventario crítico y bitácora de accesos";
        if (Permisos.SUPERVISOR.equals(rol)) return "Ventas de todos los usuarios e inventario crítico";
        return "Sus propias ventas";
    }

    public File generarPdf(Usuario actor, LocalDate desde, LocalDate hasta, File destino) {
        Permisos.exigir(actor, Modulo.REPORTES, Accion.REPORTAR);
        if (desde == null || hasta == null) throw new ServicioException("Seleccione la fecha inicial y la final.");
        if (desde.isAfter(hasta)) throw new ServicioException("La fecha inicial no puede ser posterior a la final.");

        String rol = actor.getRol().getNombre();
        boolean admin = Permisos.ADMINISTRADOR.equals(rol);
        boolean supervisor = Permisos.SUPERVISOR.equals(rol);
        Integer filtro = (admin || supervisor) ? null : actor.getIdUsuario();

        try {
            List<VentaFila> ventas = dao.ventas(desde, hasta, filtro);
            List<InventarioFila> critico = (admin || supervisor) ? dao.inventarioCritico() : List.of();
            List<BitacoraDAO.Acceso> accesos = admin ? bitacora.entre(desde, hasta) : List.of();

            Document doc = new Document(PageSize.A4.rotate(), 36, 36, 36, 36);
            PdfWriter.getInstance(doc, new FileOutputStream(destino));
            doc.open();
            encabezado(doc, actor, desde, hasta);
            seccionVentas(doc, ventas, filtro != null);
            if (admin || supervisor) {
                seccionPorUsuario(doc, ventas);
                seccionInventario(doc, critico);
            }
            if (admin) seccionAccesos(doc, accesos);
            doc.close();
        } catch (SQLException e) {
            throw Errores.bd("generar el reporte", e);
        } catch (DocumentException | IOException e) {
            com.unev.farmjm.util.Log.error("No se pudo escribir el PDF", e);
            throw new ServicioException("No se pudo crear el archivo PDF. Verifique que no esté abierto en otro programa.", e);
        }
        Auditoria.registrar(actor, "Reportes", "Generó reporte PDF del " + Fmt.fecha(desde) + " al " + Fmt.fecha(hasta));
        return destino;
    }

    // ------------------------------------------------------------------ secciones
    private void encabezado(Document doc, Usuario u, LocalDate desde, LocalDate hasta) throws DocumentException {
        doc.add(new Paragraph("FarmJM - Sistema Integral de Farmacia", TITULO));
        doc.add(new Paragraph("Reporte de gestión", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11)));
        PdfPTable t = new PdfPTable(new float[]{1.2f, 4f});
        t.setWidthPercentage(60);
        t.setHorizontalAlignment(Element.ALIGN_LEFT);
        t.setSpacingBefore(6);
        t.setSpacingAfter(10);
        fila(t, "Fecha de generación", Fmt.fechaHora(LocalDateTime.now()));
        fila(t, "Generado por", u.getNombreCompleto() + " (" + u.getUsername() + ")");
        fila(t, "Rol", u.getRol().getNombre());
        fila(t, "Rango de fechas", Fmt.fecha(desde) + "  al  " + Fmt.fecha(hasta));
        fila(t, "Contenido", descripcionContenido(u));
        doc.add(t);
    }

    private void seccionVentas(Document doc, List<VentaFila> ventas, boolean propias) throws DocumentException {
        doc.add(new Paragraph(propias ? "Mis ventas" : "Ventas del período", SECCION));
        PdfPTable t = tabla(new String[]{"Factura", "NCF", "Fecha", "Usuario", "Pago", "Subtotal", "Descuento", "ITBIS", "Total"},
                new float[]{1.1f, 1.5f, 1.7f, 1.3f, 1.2f, 1.3f, 1.2f, 1.2f, 1.4f});
        double sub = 0, desc = 0, itbis = 0, total = 0;
        for (VentaFila v : ventas) {
            celda(t, v.numero(), false); celda(t, v.ncf(), false); celda(t, Fmt.fechaHora(v.fecha()), false);
            celda(t, v.usuario(), false); celda(t, v.metodo(), false);
            celda(t, Fmt.dinero(v.subtotal()), true); celda(t, Fmt.dinero(v.descuento()), true);
            celda(t, Fmt.dinero(v.itbis()), true); celda(t, Fmt.dinero(v.total()), true);
            sub += v.subtotal(); desc += v.descuento(); itbis += v.itbis(); total += v.total();
        }
        if (ventas.isEmpty()) vacio(t, 9, "No hay ventas en el rango seleccionado.");
        else {
            PdfPCell c = new PdfPCell(new Phrase("TOTAL (" + ventas.size() + " facturas)", NEGRITA));
            c.setColspan(5); c.setBackgroundColor(SUAVE); t.addCell(c);
            for (double d : new double[]{sub, desc, itbis, total}) {
                PdfPCell x = new PdfPCell(new Phrase(Fmt.dinero(d), NEGRITA));
                x.setHorizontalAlignment(Element.ALIGN_RIGHT); x.setBackgroundColor(SUAVE); t.addCell(x);
            }
        }
        doc.add(t);
    }

    private void seccionPorUsuario(Document doc, List<VentaFila> ventas) throws DocumentException {
        doc.add(new Paragraph("Resumen de ventas por usuario", SECCION));
        Map<String, double[]> m = new TreeMap<>();
        for (VentaFila v : ventas) {
            double[] a = m.computeIfAbsent(v.usuario(), k -> new double[2]);
            a[0] += 1;
            a[1] += v.total();
        }
        PdfPTable t = tabla(new String[]{"Usuario", "Facturas", "Total vendido"}, new float[]{3f, 1.5f, 2.5f});
        for (Map.Entry<String, double[]> e : m.entrySet()) {
            celda(t, e.getKey(), false);
            celda(t, String.valueOf((int) e.getValue()[0]), true);
            celda(t, Fmt.dinero(e.getValue()[1]), true);
        }
        if (m.isEmpty()) vacio(t, 3, "Sin datos.");
        t.setWidthPercentage(60);
        t.setHorizontalAlignment(Element.ALIGN_LEFT);
        doc.add(t);
    }

    private void seccionInventario(Document doc, List<InventarioFila> filas) throws DocumentException {
        doc.add(new Paragraph("Inventario crítico (stock bajo o lotes por vencer)", SECCION));
        PdfPTable t = tabla(new String[]{"Código", "Medicamento", "Stock", "Lote", "Vence", "Estado"},
                new float[]{1.2f, 3f, 1f, 1.4f, 1.4f, 1.8f});
        for (InventarioFila f : filas) {
            long dias = f.vence() == null ? Long.MAX_VALUE : java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), f.vence());
            String estado = dias < 0 ? "Vencido" : dias <= 30 ? "Vence en " + dias + " días" : dias <= 90 ? "Por vencer" : "Stock bajo";
            celda(t, f.codigo(), false); celda(t, f.nombre(), false); celda(t, String.valueOf(f.stock()), true);
            celda(t, f.lote() == null ? "-" : f.lote(), false); celda(t, Fmt.fecha(f.vence()), false); celda(t, estado, false);
        }
        if (filas.isEmpty()) vacio(t, 6, "No hay productos en estado crítico.");
        doc.add(t);
    }

    private void seccionAccesos(Document doc, List<BitacoraDAO.Acceso> accesos) throws DocumentException {
        doc.add(new Paragraph("Bitácora de accesos del período", SECCION));
        PdfPTable t = tabla(new String[]{"Fecha y hora", "Usuario", "Resultado", "Detalle"}, new float[]{2f, 1.5f, 1.2f, 4f});
        for (BitacoraDAO.Acceso a : accesos) {
            celda(t, Fmt.fechaHora(a.fecha()), false); celda(t, a.usuario(), false);
            celda(t, a.resultado(), false); celda(t, a.detalle() == null ? "" : a.detalle(), false);
        }
        if (accesos.isEmpty()) vacio(t, 4, "Sin registros.");
        doc.add(t);
    }

    // ------------------------------------------------------------------ utilidades
    private PdfPTable tabla(String[] cabeceras, float[] anchos) {
        PdfPTable t = new PdfPTable(anchos);
        t.setWidthPercentage(100);
        t.setSpacingBefore(4);
        t.setSpacingAfter(12);
        t.setHeaderRows(1);
        for (String h : cabeceras) {
            PdfPCell c = new PdfPCell(new Phrase(h, CABECERA));
            c.setBackgroundColor(PRIMARIO);
            c.setPadding(4);
            t.addCell(c);
        }
        return t;
    }

    private void celda(PdfPTable t, String texto, boolean derecha) {
        PdfPCell c = new PdfPCell(new Phrase(texto == null ? "" : texto, NORMAL));
        c.setPadding(3);
        if (derecha) c.setHorizontalAlignment(Element.ALIGN_RIGHT);
        t.addCell(c);
    }

    private void vacio(PdfPTable t, int columnas, String mensaje) {
        PdfPCell c = new PdfPCell(new Phrase(mensaje, NORMAL));
        c.setColspan(columnas);
        c.setPadding(8);
        t.addCell(c);
    }

    private void fila(PdfPTable t, String etiqueta, String valor) {
        PdfPCell a = new PdfPCell(new Phrase(etiqueta, NEGRITA));
        a.setBackgroundColor(SUAVE);
        a.setPadding(3);
        PdfPCell b = new PdfPCell(new Phrase(valor, NORMAL));
        b.setPadding(3);
        t.addCell(a);
        t.addCell(b);
    }
}
