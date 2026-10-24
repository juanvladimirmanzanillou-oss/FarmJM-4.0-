# FarmJM 2.0 — Sistema Integral de Farmacia (Programación II, UNEV)

Aplicación de escritorio **JavaFX 21 + MySQL 8** para una farmacia dominicana: punto de venta con NCF, inventario por lotes (FEFO),
caja, clientes, devoluciones, reportes PDF, roles de usuario y **login con 2FA (TOTP)**. La base de datos se publica en la nube (**MyASP.NET**).

> Release 2.0 — Tarea No. 4 · Ing. Carlos Artemio Escalante Lorenzo · Período R3 2026

## Tecnologías
Java 17+ · JavaFX 21 · CSS de JavaFX (paleta con variables) · JDBC + HikariCP · MySQL 8 · jBCrypt · ZXing (QR) · OpenPDF · Maven

## Requisitos
JDK 17 o superior · Maven 3.9+ · MySQL 8 (local con MySQL Workbench, o la base en la nube) · Google/Microsoft Authenticator en el teléfono.

## Puesta en marcha (base local)
1. En MySQL Workbench abra `sql/farmjm.sql` y ejecútelo completo (crea la BD `farmjm`, 26 tablas, el usuario `farmjm_user`, roles, usuarios de prueba y datos de ejemplo).
2. Copie `config.example.properties` como `config.properties` y verifique `db.url`, `db.user` y `db.password`.
3. Prueba de conexión: ejecute la clase `com.unev.farmjm.db.ConexionBD` → *"Conexión exitosa a la base de datos: farmjm"*.
4. Ejecute la aplicación: `mvn javafx:run` (o la clase `Launcher` desde VS Code).

## Base de datos en la nube (MyASP.NET)
Una aplicación JavaFX es de escritorio: **lo que se publica en la nube es la base de datos**, y la aplicación se conecta de forma remota.
1. Regístrese en <https://myasp.net> y active la prueba gratuita de hosting Windows (incluye una base MySQL de 1 GB).
2. En el panel, sección *MySQL*, cree la base y su usuario. Anote **servidor, puerto, base, usuario y contraseña**.
3. Compruebe el acceso remoto desde MySQL Workbench con esos datos (capture la pantalla).
4. Con la base remota seleccionada, ejecute **`sql/farmjm_nube.sql`** (no crea base ni usuario; borra y vuelve a crear las tablas, por lo que se puede repetir).
5. Edite `config.properties`:
   ```properties
   db.url=jdbc:mysql://SERVIDOR:3306/BASE?useSSL=true&serverTimezone=America/Santo_Domingo
   db.user=USUARIO_DEL_PANEL
   db.password=CLAVE_DEL_PANEL
   ```
6. Ejecute la aplicación y pruebe login con 2FA, dashboard y CRUD contra la nube.
   Si falla, revise en el panel las opciones de acceso remoto y el host exacto; el error queda en `logs/farmjm.log`.

## Ejecutable distribuible
```bash
mvn clean package                 # genera target/farmjm-2.0.jar (incluye todas las dependencias)
java -jar target/farmjm-2.0.jar   # coloque config.properties en la misma carpeta desde donde ejecuta
```
Instalador con `jpackage` (Windows):
```bash
jpackage --type exe --name FarmJM --app-version 2.0 --input target --main-jar farmjm-2.0.jar ^
         --main-class com.unev.farmjm.Launcher --win-shortcut --win-menu --icon icono.ico
```
> `config.properties` debe quedar junto al ejecutable (o en `%USERPROFILE%\.farmjm\config.properties`).

## Usuarios de prueba (un usuario por rol)
| Rol | Usuario | Contraseña |
|---|---|---|
| Administrador | `admin` | `Admin#2026` |
| Supervisor | `supervisor` | `Super#2026` |
| Farmacéutico | `jrodriguez` | `farmjm123` |

El **2FA se activa en el primer ingreso** de cada usuario: después de la contraseña aparece un código QR; se escanea con Google Authenticator o
Microsoft Authenticator y se escribe el primer código de 6 dígitos. El administrador puede **restablecer el 2FA** de cualquier usuario en *Usuarios y Roles*.
Las contraseñas están guardadas con hash BCrypt.

## Matriz de roles y permisos
V = ver · C = crear · E = editar · D = eliminar · R = reportar. Se valida en la vista (menú y botones) **y en la capa de servicio**.

| Módulo | Administrador | Supervisor | Farmacéutico |
|---|---|---|---|
| Dashboard | V C E D R | V | V |
| Punto de Venta (POS) | V C E D R | V C | V C |
| Inventario & Lotes | V C E D R | V C E R | V |
| Clientes | V C E D R | V C E | V C E |
| Arqueo & Cierre Caja | V C E D R | V C E | V C E |
| Recetas / Controlados | V C E D R | V C | V |
| Devoluciones (NC) | V C E D R | V C | V C |
| Reportes | V C E D R | V R | V R |
| Usuarios y Roles | V C E D R | — | — |
| Auditoría & Logs | V C E D R | V | — |

**Reporte PDF por rol** (con encabezado: sistema, fecha, usuario y rol; filtro por rango de fechas):
Farmacéutico = solo sus ventas · Supervisor = ventas de todos + inventario crítico · Administrador = lo anterior + bitácora de accesos.

## Seguridad del acceso
- Primer factor: usuario y contraseña (hash BCrypt en MySQL). Segundo factor: código TOTP (RFC 6238) de 6 dígitos; la clave secreta se guarda **en la base de datos**.
- Máximo **3 intentos** (entre contraseña y código); al superarlos la cuenta se bloquea **5 minutos**.
- **Bitácora** de inicios de sesión exitosos y fallidos (tabla `bitacora_acceso`, visible en *Auditoría & Logs*).
- Consultas con `PreparedStatement` y `try-with-resources`; credenciales fuera del código (`config.properties`).

## Estructura
```
src/main/java/com/unev/farmjm/
  config/     AppConfig (lee config.properties)
  db/         ConexionBD (HikariCP)
  dao/        UsuarioDAO, MedicamentoDAO, FacturaDAO, CajaDAO, DashboardDAO, ReporteDAO ... (todo el SQL)
  service/    AuthService, UsuarioService, MedicamentoService, FacturaService, ReporteService ... (reglas y permisos)
  security/   Permisos (matriz), PasswordUtil (BCrypt), Totp (RFC 6238), QrUtil (ZXing)
  model/      25 clases del diagrama UML
  ui/         LoginView, DosFactoresView, DashboardView, AppShell, vistas y modales (FormModal)
  util/       Async (Task), Log (archivo), Fmt
src/main/resources/css/farmjm.css    paleta en variables .root
sql/farmjm.sql · sql/farmjm_nube.sql
```
Patrón: **ConexionBD → DAO → Servicio → Vista**. Las vistas no escriben SQL.

## Diseño adaptable
Se escucha `scene.widthProperty()`: **escritorio** (≥ 1100 px: menú lateral completo, 3-4 columnas), **tableta** (700–1099 px: menú compacto, 2 columnas) y
**teléfono** (< 700 px: menú inferior, 1 columna, tablas con columnas principales). Atajos para las capturas de evidencia:
`Ctrl+1` 1366×768 · `Ctrl+2` 1024×768 · `Ctrl+3` 768×1024 · `Ctrl+4` 412×915. Otros atajos: `F2` POS/facturar · `F4` caja · `F8` inventario.

## Versión web (FarmJM Web)
Además de la aplicación de escritorio, el proyecto incluye una **versión web** que reutiliza la misma capa de servicios, los mismos permisos por rol, el 2FA y la misma base de datos MySQL.
- Servidor: `com.unev.farmjm.web.WebServer` (servidor HTTP de Java sin frameworks, API JSON en `/api/...`).
- Interfaz: `src/main/resources/web/` (HTML, CSS y JavaScript sin librerías; adaptable a escritorio, tableta y teléfono).
- Módulos web: Dashboard, Punto de Venta, Inventario, Clientes, Caja, Reportes PDF, Usuarios y Roles, Auditoría.

Ejecutar en local (con `config.properties` listo):
```bash
mvn -DskipTests package
java -cp target/farmjm-2.0.jar com.unev.farmjm.web.WebServer     # abre http://localhost:8080
```
Despliegue gratuito en **Render** con la base en **Aiven**: ver `Dockerfile` y `render.yaml`. Variables de entorno en Render:
`FARMJM_DB_URL`, `FARMJM_DB_USER`, `FARMJM_DB_PASSWORD` (nunca se suben a GitHub).

## Declaración de uso de inteligencia artificial
> **Complete esta sección antes de entregar** (lo exige la tarea): herramienta usada, para qué fase y qué se modificó del resultado.

| Herramienta | Fase | Qué se modificó / revisó |
|---|---|---|
| Claude (Anthropic) | Fases 1–9 (código base, SQL, documentación) | *Escriba aquí qué cambió, probó y entendió usted* |
