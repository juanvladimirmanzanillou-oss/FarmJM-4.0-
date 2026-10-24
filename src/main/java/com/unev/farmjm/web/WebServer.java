package com.unev.farmjm.web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import com.unev.farmjm.dao.BitacoraDAO;
import com.unev.farmjm.dao.DashboardDAO.Movimiento;
import com.unev.farmjm.db.ConexionBD;
import com.unev.farmjm.model.*;
import com.unev.farmjm.security.Accion;
import com.unev.farmjm.security.Modulo;
import com.unev.farmjm.security.Permisos;
import com.unev.farmjm.security.QrPng;
import com.unev.farmjm.security.Totp;
import com.unev.farmjm.service.*;
import com.unev.farmjm.util.Fmt;
import com.unev.farmjm.util.Log;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;

/**
 * FarmJM Web: servidor HTTP (sin frameworks) que expone la misma capa de servicios de la aplicación de escritorio
 * como una API JSON y sirve la interfaz web (carpeta resources/web). Se conecta a MySQL con la misma configuración
 * (config.properties o variables de entorno FARMJM_DB_URL, FARMJM_DB_USER y FARMJM_DB_PASSWORD).
 */
public class WebServer {

    private record Pendiente(Usuario usuario, String secreto, long expira) { }
    private record Sesion(Usuario usuario, long expira) { }
    private static class NoAutorizado extends RuntimeException { }

    private static final long MIN = 60_000L;
    private static final SecureRandom RANDOM = new SecureRandom();
    private final Map<String, Pendiente> pendientes = new ConcurrentHashMap<>();
    private final Map<String, Sesion> sesiones = new ConcurrentHashMap<>();

    private final AuthService auth = new AuthService();
    private final UsuarioService usuarios = new UsuarioService();
    private final MedicamentoService medicamentos = new MedicamentoService();
    private final ClienteService clientes = new ClienteService();
    private final FacturaService facturas = new FacturaService();
    private final CajaService cajas = new CajaService();
    private final DashboardService dashboard = new DashboardService();
    private final ReporteService reportes = new ReporteService();
    private final BitacoraService bitacora = new BitacoraService();

    public static void main(String[] args) throws IOException {
        int puerto = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
        new WebServer().iniciar(puerto);
    }

    public void iniciar(int puerto) throws IOException {
        HttpServer s = HttpServer.create(new InetSocketAddress(puerto), 0);
        s.createContext("/api/", this::api);
        s.createContext("/health", ex -> enviarTexto(ex, 200, "text/plain", "ok"));
        s.createContext("/", this::estatico);
        s.setExecutor(Executors.newFixedThreadPool(8));
        s.start();
        Log.info("FarmJM Web escuchando en el puerto " + puerto);
        System.out.println("FarmJM Web en http://localhost:" + puerto);
    }

    // ============================================================ API
    private void api(HttpExchange ex) throws IOException {
        try {
            enrutar(ex, ex.getRequestURI().getPath().substring(4), ex.getRequestMethod());
        } catch (NoAutorizado e) {
            enviar(ex, 401, mapa("error", "Su sesión expiró. Inicie sesión de nuevo.", "sesion", true));
        } catch (AuthException e) {
            enviar(ex, 401, mapa("error", e.getMessage(), "volver", e.volverAlLogin()));
        } catch (ServicioException e) {
            enviar(ex, e.getMessage().contains("no tiene permiso") ? 403 : 400, mapa("error", e.getMessage()));
        } catch (IllegalArgumentException | IndexOutOfBoundsException e) {
            enviar(ex, 400, mapa("error", "Los datos enviados no son válidos."));
        } catch (Exception e) {
            Log.error("Error en la API " + ex.getRequestURI(), e);
            enviar(ex, 500, mapa("error", "Error inesperado del servidor. Detalle en el log."));
        }
    }

    private void enrutar(HttpExchange ex, String ruta, String metodo) throws Exception {
        // ---------- sin sesión
        if (ruta.equals("/login") && metodo.equals("POST")) { login(ex); return; }
        if (ruta.equals("/2fa") && metodo.equals("POST")) { segundoFactor(ex); return; }

        // ---------- con sesión
        Usuario u = actor(ex);
        if (ruta.equals("/salir") && metodo.equals("POST")) {
            sesiones.remove(token(ex));
            Auditoria.registrar(u, "Sesión", "Cierre de sesión de " + u.getNombreCompleto() + " (web)");
            enviar(ex, 200, mapa("ok", true));
        } else if (ruta.equals("/yo")) {
            enviar(ex, 200, infoUsuario(u));
        } else if (ruta.equals("/dashboard")) {
            enviar(ex, 200, dashboardJson(dashboard.cargar(u)));
        } else if (ruta.equals("/categorias")) {
            List<Object> l = new ArrayList<>();
            for (Categoria c : medicamentos.categorias()) l.add(mapa("id", c.getIdCategoria(), "nombre", c.getNombre()));
            enviar(ex, 200, l);
        } else if (ruta.equals("/medicamentos") && metodo.equals("GET")) {
            List<Object> l = new ArrayList<>();
            for (Medicamento m : medicamentos.listar(u)) l.add(medJson(m));
            enviar(ex, 200, l);
        } else if (ruta.equals("/medicamentos") && metodo.equals("POST")) {
            guardarMedicamento(ex, u);
        } else if (ruta.startsWith("/medicamentos/") && metodo.equals("DELETE")) {
            Medicamento m = new Medicamento();
            m.setIdProducto(idDe(ruta));
            medicamentos.eliminar(u, m);
            enviar(ex, 200, mapa("ok", true));
        } else if (ruta.equals("/clientes") && metodo.equals("GET")) {
            List<Object> l = new ArrayList<>();
            for (Cliente c : clientes.listar(u)) {
                l.add(mapa("id", c.getIdCliente(), "nombre", c.getNombre(), "doc", nz(c.getCedulaRnc()), "telefono", nz(c.getTelefono()), "email", nz(c.getEmail())));
            }
            enviar(ex, 200, l);
        } else if (ruta.equals("/clientes") && metodo.equals("POST")) {
            Map<String, Object> b = cuerpo(ex);
            clientes.guardar(u, new Cliente(Json.entero(b, "id"), Json.str(b, "nombre"), Json.str(b, "doc"), Json.str(b, "telefono"), Json.str(b, "email")));
            enviar(ex, 200, mapa("ok", true));
        } else if (ruta.startsWith("/clientes/") && metodo.equals("DELETE")) {
            clientes.eliminar(u, new Cliente(idDe(ruta), "", "", "", ""));
            enviar(ex, 200, mapa("ok", true));
        } else if (ruta.equals("/roles")) {
            List<Object> l = new ArrayList<>();
            for (Rol r : usuarios.roles(u)) l.add(mapa("id", r.getIdRol(), "nombre", r.getNombre()));
            enviar(ex, 200, l);
        } else if (ruta.equals("/usuarios") && metodo.equals("GET")) {
            List<Object> l = new ArrayList<>();
            for (Usuario x : usuarios.listar(u)) {
                l.add(mapa("id", x.getIdUsuario(), "username", x.getUsername(), "nombre", x.getNombre(), "apellido", x.getApellido(),
                        "email", nz(x.getEmail()), "licencia", nz(x.getLicencia()), "rolId", x.getRol().getIdRol(), "rol", x.getRol().getNombre(),
                        "activo", x.isActivo(), "totp", x.isTotpActivo(), "bloqueado", x.estaBloqueado()));
            }
            enviar(ex, 200, l);
        } else if (ruta.equals("/usuarios") && metodo.equals("POST")) {
            guardarUsuario(ex, u);
        } else if (ruta.matches("/usuarios/\\d+/reset2fa") && metodo.equals("POST")) {
            Usuario x = new Usuario();
            x.setIdUsuario(Integer.parseInt(ruta.split("/")[2]));
            x.setUsername("#" + x.getIdUsuario());
            usuarios.restablecer2fa(u, x);
            enviar(ex, 200, mapa("ok", true));
        } else if (ruta.equals("/auditoria")) {
            enviar(ex, 200, auditoria(u));
        } else if (ruta.equals("/reporte")) {
            reporte(ex, u);
        } else if (ruta.equals("/pos") && metodo.equals("GET")) {
            enviar(ex, 200, posConfig(u));
        } else if (ruta.equals("/ventas") && metodo.equals("POST")) {
            vender(ex, u);
        } else if (ruta.equals("/caja") && metodo.equals("GET")) {
            enviar(ex, 200, cajaJson(u, cajas.cargar(u)));
        } else if (ruta.equals("/caja/abrir") && metodo.equals("POST")) {
            enviar(ex, 200, cajaJson(u, cajas.abrir(u)));
        } else if (ruta.equals("/caja/cerrar") && metodo.equals("POST")) {
            Caja c = cajas.cargar(u);
            double contado = Json.num(cuerpo(ex), "contado");
            ArqueoCaja a = cajas.cerrar(u, c, cajas.efectivo(c), contado);
            enviar(ex, 200, mapa("esperado", a.getMontoEsperado(), "contado", contado, "diferencia", a.getDiferencia()));
        } else {
            enviar(ex, 404, mapa("error", "Ruta no encontrada"));
        }
    }

    // ------------------------------------------------------------ autenticación
    private void login(HttpExchange ex) throws IOException {
        Map<String, Object> b = cuerpo(ex);
        Usuario u = auth.primerFactor(Json.str(b, "usuario"), String.valueOf(b.getOrDefault("clave", "")));
        limpiar();
        String t = nuevoToken();
        Map<String, Object> r = mapa("pendiente", t);
        if (!u.isTotpActivo()) {
            String secreto = auth.nuevoSecreto();
            pendientes.put(t, new Pendiente(u, secreto, ahora() + 5 * MIN));
            r.put("activar", true);
            r.put("qr", QrPng.dataUri(Totp.uri("FarmJM", u.getUsername(), secreto), 220));
            r.put("secreto", secreto);
        } else {
            pendientes.put(t, new Pendiente(u, null, ahora() + 5 * MIN));
            r.put("activar", false);
        }
        enviar(ex, 200, r);
    }

    private void segundoFactor(HttpExchange ex) throws IOException {
        Map<String, Object> b = cuerpo(ex);
        Pendiente p = pendientes.get(Json.str(b, "pendiente"));
        if (p == null || p.expira() < ahora()) {
            throw new AuthException("El inicio de sesión expiró. Vuelva a escribir su usuario y contraseña.", true);
        }
        String codigo = Json.str(b, "codigo");
        Usuario u;
        try {
            u = p.secreto() != null ? auth.activarTotp(p.usuario(), p.secreto(), codigo) : auth.segundoFactor(p.usuario(), codigo);
        } catch (AuthException e) {
            if (e.volverAlLogin()) pendientes.remove(Json.str(b, "pendiente"));
            throw e;
        }
        pendientes.remove(Json.str(b, "pendiente"));
        String t = nuevoToken();
        sesiones.put(t, new Sesion(u, ahora() + 8 * 60 * MIN));
        Map<String, Object> r = infoUsuario(u);
        r.put("token", t);
        enviar(ex, 200, r);
    }

    private Usuario actor(HttpExchange ex) {
        String t = token(ex);
        Sesion s = t == null ? null : sesiones.get(t);
        if (s == null || s.expira() < ahora()) {
            if (t != null) sesiones.remove(t);
            throw new NoAutorizado();
        }
        return s.usuario();
    }

    private String token(HttpExchange ex) {
        String h = ex.getRequestHeaders().getFirst("Authorization");
        return h != null && h.startsWith("Bearer ") ? h.substring(7).trim() : null;
    }

    private void limpiar() {
        long n = ahora();
        pendientes.values().removeIf(p -> p.expira() < n);
        sesiones.values().removeIf(s -> s.expira() < n);
    }

    private static String nuevoToken() {
        byte[] b = new byte[24];
        RANDOM.nextBytes(b);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(b);
    }

    private static long ahora() { return System.currentTimeMillis(); }

    private Map<String, Object> infoUsuario(Usuario u) {
        Map<String, Object> permisos = new LinkedHashMap<>();
        for (Modulo m : Modulo.values()) {
            List<String> acciones = new ArrayList<>();
            for (Accion a : Accion.values()) if (Permisos.puede(u, m, a)) acciones.add(a.name());
            if (!acciones.isEmpty()) permisos.put(m.name(), acciones);
        }
        return mapa("usuario", u.getUsername(), "nombre", u.getNombreCompleto(), "rol", u.getRol().getNombre(), "permisos", permisos);
    }

    // ------------------------------------------------------------ módulos
    private Map<String, Object> medJson(Medicamento m) {
        Lote l = m.getLoteProximo();
        return mapa("id", m.getIdProducto(), "codigo", m.getCodigo(), "nombre", m.getNombre(), "principioActivo", nz(m.getPrincipioActivo()),
                "presentacion", nz(m.getPresentacion()), "categoriaId", m.getCategoria().getIdCategoria(), "categoria", nz(m.getCategoria().getNombre()),
                "precio", m.getPrecio(), "stock", m.getStock(), "requiereReceta", m.isRequiereReceta(), "controlado", m.isControlado(),
                "idLote", l == null ? 0 : l.getIdLote(), "lote", l == null ? "" : l.getCodigoLote(),
                "vence", l == null ? "" : l.getFechaVencimiento().toString(), "dias", l == null ? 9999 : l.getDiasParaVencer());
    }

    private void guardarMedicamento(HttpExchange ex, Usuario u) throws IOException {
        Map<String, Object> b = cuerpo(ex);
        Medicamento m = new Medicamento();
        m.setIdProducto(Json.entero(b, "id"));
        m.setCodigo(Json.str(b, "codigo"));
        m.setNombre(Json.str(b, "nombre"));
        m.setPrincipioActivo(Json.str(b, "principioActivo"));
        m.setPresentacion(Json.str(b, "presentacion"));
        m.setCategoria(Json.entero(b, "categoriaId") == 0 ? null : new Categoria(Json.entero(b, "categoriaId"), ""));
        m.setPrecio(Json.num(b, "precio"));
        m.setStock(Json.entero(b, "stock"));
        m.setRequiereReceta(Json.bool(b, "requiereReceta"));
        m.setControlado(Json.bool(b, "controlado"));
        LocalDate vence;
        try {
            vence = Json.str(b, "vence").isEmpty() ? null : LocalDate.parse(Json.str(b, "vence"));
        } catch (Exception e) {
            throw new ServicioException("La fecha de vencimiento no es válida.");
        }
        Lote lote = new Lote(Json.entero(b, "idLote"), Json.str(b, "lote"), m, vence, m.getStock());
        medicamentos.guardar(u, m, lote);
        enviar(ex, 200, mapa("ok", true));
    }

    private void guardarUsuario(HttpExchange ex, Usuario actor) throws IOException {
        Map<String, Object> b = cuerpo(ex);
        Rol rol = null;
        for (Rol r : usuarios.roles(actor)) if (r.getIdRol() == Json.entero(b, "rolId")) rol = r;
        Usuario x = new Usuario();
        x.setIdUsuario(Json.entero(b, "id"));
        x.setNombre(Json.str(b, "nombre"));
        x.setApellido(Json.str(b, "apellido"));
        x.setUsername(Json.str(b, "username"));
        x.setEmail(Json.str(b, "email"));
        x.setLicencia(Json.str(b, "licencia"));
        x.setActivo(Json.bool(b, "activo"));
        x.setRol(rol);
        String clave = String.valueOf(b.getOrDefault("clave", ""));
        if (x.getIdUsuario() == 0) usuarios.crear(actor, x, clave); else usuarios.actualizar(actor, x, clave);
        enviar(ex, 200, mapa("ok", true));
    }

    private Object dashboardJson(DashboardData d) {
        List<Object> k = new ArrayList<>();
        for (DashboardData.Kpi x : d.kpis()) k.add(mapa("etiqueta", x.etiqueta(), "valor", x.valor(), "nivel", x.nivel()));
        List<Object> barras = new ArrayList<>();
        d.barras().forEach((a, v) -> barras.add(mapa("etiqueta", a, "valor", v)));
        List<Object> pastel = new ArrayList<>();
        d.pastel().forEach((a, v) -> pastel.add(mapa("etiqueta", a, "valor", v)));
        List<Object> mov = new ArrayList<>();
        for (Movimiento m : d.movimientos()) mov.add(mapa("fecha", Fmt.fechaHora(m.fecha()), "usuario", m.usuario(), "modulo", m.modulo(), "accion", m.accion()));
        return mapa("subtitulo", d.subtitulo(), "kpis", k, "tituloBarras", d.tituloBarras(), "barras", barras,
                "tituloPastel", d.tituloPastel(), "pastel", pastel, "movimientos", mov);
    }

    private Object auditoria(Usuario u) {
        List<Object> act = new ArrayList<>();
        for (LogActividad l : bitacora.actividad(u, 100)) {
            act.add(mapa("fecha", Fmt.fechaHora(l.getFecha()), "usuario", l.getUsuario().getNombreCompleto(), "modulo", l.getModulo(), "accion", l.getAccion()));
        }
        List<Object> acc = new ArrayList<>();
        for (BitacoraDAO.Acceso a : bitacora.recientes(u, 100)) {
            acc.add(mapa("fecha", Fmt.fechaHora(a.fecha()), "usuario", a.usuario(), "resultado", a.resultado(), "detalle", nz(a.detalle())));
        }
        return mapa("actividad", act, "accesos", acc);
    }

    private void reporte(HttpExchange ex, Usuario u) throws IOException {
        Map<String, String> q = consulta(ex);
        File tmp = Files.createTempFile("farmjm-reporte", ".pdf").toFile();
        try {
            LocalDate desde = LocalDate.parse(q.getOrDefault("desde", ""));
            LocalDate hasta = LocalDate.parse(q.getOrDefault("hasta", ""));
            reportes.generarPdf(u, desde, hasta, tmp);
            byte[] pdf = Files.readAllBytes(tmp.toPath());
            ex.getResponseHeaders().add("Content-Disposition", "attachment; filename=\"Reporte_FarmJM_" + LocalDate.now() + ".pdf\"");
            enviarBytes(ex, 200, "application/pdf", pdf);
        } catch (java.time.format.DateTimeParseException e) {
            throw new ServicioException("Seleccione la fecha inicial y la final.");
        } finally {
            tmp.delete();
        }
    }

    // ------------------------------------------------------------ POS y caja
    private Object posConfig(Usuario u) {
        List<Object> comp = new ArrayList<>();
        for (ComprobanteFiscal c : facturas.comprobantes()) comp.add(mapa("id", c.getIdComprobante(), "nombre", c.getTipo() + " - " + c.getDescripcion()));
        List<Object> desc = new ArrayList<>();
        for (Descuento d : facturas.descuentos()) desc.add(mapa("id", d.getIdDescuento(), "nombre", d.getNombre(), "porcentaje", d.getPorcentaje()));
        return mapa("comprobantes", comp, "descuentos", desc, "caja", cajaJson(u, cajas.cargar(u)));
    }

    private Map<String, Object> cajaJson(Usuario u, Caja c) {
        return mapa("id", c.getIdCaja(), "abierta", c.isAbierta(), "base", c.getMontoBase(), "efectivo", cajas.efectivo(c));
    }

    @SuppressWarnings("unchecked")
    private void vender(HttpExchange ex, Usuario u) throws IOException {
        Map<String, Object> b = cuerpo(ex);
        Map<Integer, Medicamento> catalogo = new HashMap<>();
        for (Medicamento m : medicamentos.listar(u)) catalogo.put(m.getIdProducto(), m);
        Factura f = new Factura();
        int linea = 1;
        for (Object o : (List<Object>) b.getOrDefault("items", List.of())) {
            Map<String, Object> it = (Map<String, Object>) o;
            Medicamento m = catalogo.get(Json.entero(it, "id"));
            int cant = Json.entero(it, "cantidad");
            if (m == null || cant <= 0) throw new ServicioException("Hay un producto o una cantidad no válidos en la orden.");
            if (cant > m.getStock()) throw new ServicioException("Stock insuficiente de " + m.getNombre() + " (disponible: " + m.getStock() + ").");
            f.agregarDetalle(new DetalleFactura(linea++, m, m.getLoteProximo(), cant, m.getPrecio(), m.getPrecio() * cant));
        }
        ComprobanteFiscal tipo = facturas.comprobantes().stream().filter(c -> c.getIdComprobante() == Json.entero(b, "comprobanteId")).findFirst()
                .orElseThrow(() -> new ServicioException("Seleccione el tipo de comprobante."));
        f.setDescuento(facturas.descuentos().stream().filter(d -> d.getIdDescuento() == Json.entero(b, "descuentoId")).findFirst().orElse(null));
        String metodo = Json.str(b, "metodo");
        if (!List.of("Efectivo", "Tarjeta", "Mixto").contains(metodo)) throw new ServicioException("Seleccione el método de pago.");
        Factura r = facturas.procesar(u, f, tipo, metodo, cajas.cargar(u));
        enviar(ex, 200, mapa("numero", r.getNumero(), "ncf", r.getComprobante().getSecuencia(), "total", r.getTotal()));
    }

    // ============================================================ archivos estáticos
    private void estatico(HttpExchange ex) throws IOException {
        String p = ex.getRequestURI().getPath();
        if (p.equals("/")) p = "/index.html";
        if (p.contains("..") || p.contains("\\")) { enviarTexto(ex, 400, "text/plain", "Solicitud no válida"); return; }
        try (InputStream in = WebServer.class.getResourceAsStream("/web" + p)) {
            if (in == null) { enviarTexto(ex, 404, "text/plain", "No encontrado"); return; }
            enviarBytes(ex, 200, tipo(p), in.readAllBytes());
        }
    }

    private static String tipo(String p) {
        if (p.endsWith(".html")) return "text/html; charset=utf-8";
        if (p.endsWith(".css")) return "text/css; charset=utf-8";
        if (p.endsWith(".js")) return "application/javascript; charset=utf-8";
        if (p.endsWith(".svg")) return "image/svg+xml";
        if (p.endsWith(".png")) return "image/png";
        return "application/octet-stream";
    }

    // ============================================================ utilidades
    private Map<String, Object> cuerpo(HttpExchange ex) throws IOException {
        return Json.objeto(new String(ex.getRequestBody().readNBytes(1_000_000), StandardCharsets.UTF_8));
    }

    private Map<String, String> consulta(HttpExchange ex) {
        Map<String, String> m = new HashMap<>();
        String q = ex.getRequestURI().getRawQuery();
        if (q == null) return m;
        for (String par : q.split("&")) {
            String[] kv = par.split("=", 2);
            m.put(java.net.URLDecoder.decode(kv[0], StandardCharsets.UTF_8), kv.length > 1 ? java.net.URLDecoder.decode(kv[1], StandardCharsets.UTF_8) : "");
        }
        return m;
    }

    private static int idDe(String ruta) {
        return Integer.parseInt(ruta.substring(ruta.lastIndexOf('/') + 1));
    }

    private static String nz(String s) { return s == null ? "" : s; }

    static Map<String, Object> mapa(Object... kv) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) m.put((String) kv[i], kv[i + 1]);
        return m;
    }

    private void enviar(HttpExchange ex, int codigo, Object json) throws IOException {
        enviarBytes(ex, codigo, "application/json; charset=utf-8", Json.write(json).getBytes(StandardCharsets.UTF_8));
    }

    private void enviarTexto(HttpExchange ex, int codigo, String tipo, String texto) throws IOException {
        enviarBytes(ex, codigo, tipo, texto.getBytes(StandardCharsets.UTF_8));
    }

    private void enviarBytes(HttpExchange ex, int codigo, String tipo, byte[] datos) throws IOException {
        var h = ex.getResponseHeaders();
        h.set("Content-Type", tipo);
        h.set("X-Content-Type-Options", "nosniff");
        h.set("X-Frame-Options", "DENY");
        h.set("Referrer-Policy", "no-referrer");
        h.set("Content-Security-Policy", "default-src 'self'; img-src 'self' data:; style-src 'self' 'unsafe-inline'; script-src 'self'; frame-ancestors 'none'");
        if (ex.getRequestURI().getPath().startsWith("/api/")) h.set("Cache-Control", "no-store");
        ex.sendResponseHeaders(codigo, datos.length == 0 ? -1 : datos.length);
        if (datos.length > 0) {
            try (OutputStream out = ex.getResponseBody()) {
                out.write(datos);
            }
        } else {
            ex.close();
        }
    }
}
