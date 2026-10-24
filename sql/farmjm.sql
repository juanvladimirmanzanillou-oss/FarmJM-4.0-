-- ============================================================
-- FarmJM 2.0 - Sistema Integral de Farmacia  |  MySQL 8
-- Script LOCAL: ejecutar completo en MySQL Workbench (rayo).
-- Para la base en la nube (MyASP.NET) usar farmjm_nube.sql
-- ============================================================
SET NAMES utf8mb4;
DROP DATABASE IF EXISTS farmjm;
CREATE DATABASE farmjm CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE farmjm;

-- Usuario de la aplicación (debe coincidir con config.properties)
CREATE USER IF NOT EXISTS 'farmjm_user'@'localhost' IDENTIFIED BY 'FarmJM#2026';
GRANT ALL PRIVILEGES ON farmjm.* TO 'farmjm_user'@'localhost';
FLUSH PRIVILEGES;

-- ---------------------- Seguridad ----------------------
CREATE TABLE rol (
  id_rol INT AUTO_INCREMENT PRIMARY KEY,
  nombre VARCHAR(50) NOT NULL UNIQUE,
  descripcion VARCHAR(150)
);
CREATE TABLE usuario (
  id_usuario INT AUTO_INCREMENT PRIMARY KEY,
  nombre VARCHAR(60) NOT NULL,
  apellido VARCHAR(60) NOT NULL,
  licencia VARCHAR(30),
  username VARCHAR(40) NOT NULL UNIQUE,
  clave_hash VARCHAR(100) NOT NULL,            -- hash BCrypt, nunca texto plano
  email VARCHAR(100),
  activo TINYINT(1) NOT NULL DEFAULT 1,
  id_rol INT NOT NULL,
  totp_secret VARCHAR(64) NULL,                -- clave secreta TOTP (2FA) por usuario
  totp_activo TINYINT(1) NOT NULL DEFAULT 0,   -- 1 = ya escaneó el QR y confirmó el código
  intentos_fallidos INT NOT NULL DEFAULT 0,
  bloqueado_hasta DATETIME NULL,               -- bloqueo temporal tras 3 intentos fallidos
  FOREIGN KEY (id_rol) REFERENCES rol(id_rol)
);
-- Bitácora de inicios de sesión (exitosos y fallidos)
CREATE TABLE bitacora_acceso (
  id_bitacora INT AUTO_INCREMENT PRIMARY KEY,
  id_usuario INT NULL,
  username VARCHAR(40) NOT NULL,
  fecha DATETIME NOT NULL,
  resultado VARCHAR(10) NOT NULL,              -- EXITOSO | FALLIDO
  detalle VARCHAR(150),
  INDEX idx_bitacora_fecha (fecha)
);

-- ---------------------- Catálogo ----------------------
CREATE TABLE cliente (
  id_cliente INT AUTO_INCREMENT PRIMARY KEY,
  nombre VARCHAR(100) NOT NULL,
  cedula_rnc VARCHAR(20),
  telefono VARCHAR(20),
  email VARCHAR(100),
  activo TINYINT(1) NOT NULL DEFAULT 1
);
CREATE TABLE proveedor (
  id_proveedor INT AUTO_INCREMENT PRIMARY KEY,
  nombre VARCHAR(100) NOT NULL,
  rnc VARCHAR(20),
  telefono VARCHAR(20),
  direccion VARCHAR(150)
);
CREATE TABLE categoria (
  id_categoria INT AUTO_INCREMENT PRIMARY KEY,
  nombre VARCHAR(60) NOT NULL UNIQUE
);
CREATE TABLE producto (
  id_producto INT AUTO_INCREMENT PRIMARY KEY,
  codigo VARCHAR(20) NOT NULL UNIQUE,
  nombre VARCHAR(100) NOT NULL,
  precio DECIMAL(10,2) NOT NULL,
  stock INT NOT NULL DEFAULT 0,
  id_categoria INT,
  id_proveedor INT,
  activo TINYINT(1) NOT NULL DEFAULT 1,
  FOREIGN KEY (id_categoria) REFERENCES categoria(id_categoria),
  FOREIGN KEY (id_proveedor) REFERENCES proveedor(id_proveedor)
);
-- Herencia Medicamento -> Producto (misma clave primaria)
CREATE TABLE medicamento (
  id_producto INT PRIMARY KEY,
  principio_activo VARCHAR(100) NOT NULL,
  presentacion VARCHAR(60),
  requiere_receta TINYINT(1) NOT NULL DEFAULT 0,
  controlado TINYINT(1) NOT NULL DEFAULT 0,
  FOREIGN KEY (id_producto) REFERENCES producto(id_producto)
);
CREATE TABLE lote (
  id_lote INT AUTO_INCREMENT PRIMARY KEY,
  codigo_lote VARCHAR(20) NOT NULL,
  fecha_vencimiento DATE NOT NULL,
  cantidad INT NOT NULL,
  id_producto INT NOT NULL,
  FOREIGN KEY (id_producto) REFERENCES producto(id_producto)
);

-- ---------------------- Inventario ----------------------
CREATE TABLE inventario (
  id_inventario INT AUTO_INCREMENT PRIMARY KEY,
  id_producto INT NOT NULL UNIQUE,
  existencia INT NOT NULL,
  stock_minimo INT NOT NULL DEFAULT 10,
  ultima_actualizacion DATETIME,
  FOREIGN KEY (id_producto) REFERENCES producto(id_producto)
);
CREATE TABLE ajuste_inventario (
  id_ajuste INT AUTO_INCREMENT PRIMARY KEY,
  id_producto INT NOT NULL,
  cantidad INT NOT NULL,
  motivo VARCHAR(150),
  fecha DATETIME NOT NULL,
  id_usuario INT NOT NULL,
  FOREIGN KEY (id_producto) REFERENCES producto(id_producto),
  FOREIGN KEY (id_usuario) REFERENCES usuario(id_usuario)
);
CREATE TABLE alerta_vencimiento (
  id_alerta INT AUTO_INCREMENT PRIMARY KEY,
  id_lote INT NOT NULL,
  dias_restantes INT NOT NULL,
  atendida TINYINT(1) NOT NULL DEFAULT 0,
  FOREIGN KEY (id_lote) REFERENCES lote(id_lote)
);

-- ---------------------- Caja y facturación ----------------------
CREATE TABLE caja (
  id_caja INT AUTO_INCREMENT PRIMARY KEY,
  id_usuario INT NOT NULL,
  monto_base DECIMAL(10,2) NOT NULL,
  apertura DATETIME NOT NULL,
  cierre DATETIME,
  abierta TINYINT(1) NOT NULL DEFAULT 1,
  FOREIGN KEY (id_usuario) REFERENCES usuario(id_usuario)
);
CREATE TABLE arqueo_caja (
  id_arqueo INT AUTO_INCREMENT PRIMARY KEY,
  id_caja INT NOT NULL,
  monto_esperado DECIMAL(10,2) NOT NULL,
  monto_contado DECIMAL(10,2) NOT NULL,
  diferencia DECIMAL(10,2) NOT NULL,
  fecha DATETIME NOT NULL,
  FOREIGN KEY (id_caja) REFERENCES caja(id_caja)
);
CREATE TABLE comprobante_fiscal (
  id_comprobante INT AUTO_INCREMENT PRIMARY KEY,
  tipo VARCHAR(5) NOT NULL,
  descripcion VARCHAR(60) NOT NULL,
  secuencia VARCHAR(15)
);
CREATE TABLE descuento (
  id_descuento INT AUTO_INCREMENT PRIMARY KEY,
  nombre VARCHAR(60) NOT NULL,
  porcentaje DECIMAL(5,2) NOT NULL,
  activo TINYINT(1) NOT NULL DEFAULT 1
);
CREATE TABLE factura (
  id_factura INT AUTO_INCREMENT PRIMARY KEY,
  numero VARCHAR(15) NOT NULL UNIQUE,
  ncf VARCHAR(15) NOT NULL UNIQUE,
  fecha DATETIME NOT NULL,
  subtotal DECIMAL(10,2) NOT NULL,
  descuento_aplicado DECIMAL(10,2) NOT NULL DEFAULT 0,
  itbis DECIMAL(10,2) NOT NULL,
  total DECIMAL(10,2) NOT NULL,
  id_cliente INT,
  id_usuario INT NOT NULL,
  id_caja INT NOT NULL,
  id_comprobante INT NOT NULL,
  id_descuento INT,
  FOREIGN KEY (id_cliente) REFERENCES cliente(id_cliente),
  FOREIGN KEY (id_usuario) REFERENCES usuario(id_usuario),
  FOREIGN KEY (id_caja) REFERENCES caja(id_caja),
  FOREIGN KEY (id_comprobante) REFERENCES comprobante_fiscal(id_comprobante),
  FOREIGN KEY (id_descuento) REFERENCES descuento(id_descuento)
);
CREATE TABLE detalle_factura (
  id_detalle INT AUTO_INCREMENT PRIMARY KEY,
  id_factura INT NOT NULL,
  id_producto INT NOT NULL,
  id_lote INT,
  cantidad INT NOT NULL,
  precio_unitario DECIMAL(10,2) NOT NULL,
  subtotal DECIMAL(10,2) NOT NULL,
  FOREIGN KEY (id_factura) REFERENCES factura(id_factura),
  FOREIGN KEY (id_producto) REFERENCES producto(id_producto),
  FOREIGN KEY (id_lote) REFERENCES lote(id_lote)
);
CREATE TABLE pago (
  id_pago INT AUTO_INCREMENT PRIMARY KEY,
  id_factura INT NOT NULL,
  metodo VARCHAR(20) NOT NULL,
  monto DECIMAL(10,2) NOT NULL,
  fecha DATETIME NOT NULL,
  FOREIGN KEY (id_factura) REFERENCES factura(id_factura)
);
CREATE TABLE devolucion (
  id_devolucion INT AUTO_INCREMENT PRIMARY KEY,
  id_factura INT NOT NULL,
  id_producto INT NOT NULL,
  cantidad INT NOT NULL,
  motivo VARCHAR(150),
  monto DECIMAL(10,2) NOT NULL,
  ncf_nota_credito VARCHAR(15),
  fecha DATETIME NOT NULL,
  FOREIGN KEY (id_factura) REFERENCES factura(id_factura),
  FOREIGN KEY (id_producto) REFERENCES producto(id_producto)
);

-- ---------------------- Recetas ----------------------
CREATE TABLE medico (
  id_medico INT AUTO_INCREMENT PRIMARY KEY,
  nombre VARCHAR(100) NOT NULL,
  exequatur VARCHAR(30),
  especialidad VARCHAR(60)
);
CREATE TABLE receta (
  id_receta INT AUTO_INCREMENT PRIMARY KEY,
  id_medico INT NOT NULL,
  id_cliente INT NOT NULL,
  fecha DATE NOT NULL,
  controlada TINYINT(1) NOT NULL DEFAULT 0,
  FOREIGN KEY (id_medico) REFERENCES medico(id_medico),
  FOREIGN KEY (id_cliente) REFERENCES cliente(id_cliente)
);
CREATE TABLE detalle_receta (
  id_detalle INT AUTO_INCREMENT PRIMARY KEY,
  id_receta INT NOT NULL,
  id_producto INT NOT NULL,
  cantidad INT NOT NULL,
  indicaciones VARCHAR(200),
  FOREIGN KEY (id_receta) REFERENCES receta(id_receta),
  FOREIGN KEY (id_producto) REFERENCES medicamento(id_producto)
);

-- ---------------------- Compras y auditoría ----------------------
CREATE TABLE orden_compra (
  id_orden INT AUTO_INCREMENT PRIMARY KEY,
  id_proveedor INT NOT NULL,
  fecha DATE NOT NULL,
  estado VARCHAR(20) NOT NULL DEFAULT 'Pendiente',
  total DECIMAL(10,2) NOT NULL DEFAULT 0,
  FOREIGN KEY (id_proveedor) REFERENCES proveedor(id_proveedor)
);
CREATE TABLE detalle_orden_compra (
  id_detalle INT AUTO_INCREMENT PRIMARY KEY,
  id_orden INT NOT NULL,
  id_producto INT NOT NULL,
  cantidad INT NOT NULL,
  costo_unitario DECIMAL(10,2) NOT NULL,
  FOREIGN KEY (id_orden) REFERENCES orden_compra(id_orden),
  FOREIGN KEY (id_producto) REFERENCES producto(id_producto)
);
CREATE TABLE log_actividad (
  id_log INT AUTO_INCREMENT PRIMARY KEY,
  id_usuario INT NOT NULL,
  accion VARCHAR(200) NOT NULL,
  modulo VARCHAR(40) NOT NULL,
  fecha DATETIME NOT NULL,
  FOREIGN KEY (id_usuario) REFERENCES usuario(id_usuario)
);

-- ====================== DATOS DE EJEMPLO ======================
INSERT INTO rol (nombre, descripcion) VALUES
 ('Administrador','Acceso total, gestión de usuarios y 2FA'),
 ('Supervisor','Supervisa ventas, inventario, caja y reportes'),
 ('Farmacéutico','Atiende ventas y consulta inventario');
-- Usuarios de prueba (un usuario por rol). Claves con BCrypt; el 2FA se activa en el primer ingreso.
INSERT INTO usuario (nombre, apellido, licencia, username, clave_hash, email, id_rol) VALUES
 ('Ana','Administradora','Lic. 0001','admin','$2a$10$HCb1yoYhG3VQ8Lylxl1kqu8hHNBijLIoxgCRiGoYzoQjkALJL0JWW','admin@farmjm.test',1),
 ('Sergio','Supervisor','Lic. 0002','supervisor','$2a$10$O7bVOPY3HRuTCGlLvcZjk.1BFhW0cX6/LD.XinH1.zB8XN6QXGL/i','supervisor@farmjm.test',2),
 ('Juan','Rodríguez','Lic. 4091','jrodriguez','$2a$10$TXpsP//h8bGLqRYJlPeUD.DNkk8LvnZOJ.Zy3eUnKByWg/IzcFSy.','jrodriguez@farmjm.test',3);
INSERT INTO categoria (nombre) VALUES ('Antibióticos'),('Analgésicos'),('Cardiovasculares'),('Controlados'),('Gastroprotectores');
INSERT INTO proveedor (nombre, rnc, telefono, direccion) VALUES ('Distribuidora Farma RD','1-30-55555-1','809-555-0100','Santo Domingo');

INSERT INTO producto (id_producto, codigo, nombre, precio, stock, id_categoria, id_proveedor) VALUES
 (1,'7460111','Amoxicilina 500mg',250.00,45,1,1),
 (2,'7460222','Paracetamol 500mg',35.00,120,2,1),
 (3,'7460333','Losartán Potásico 50mg',420.00,18,3,1),
 (4,'7460444','Alprazolam 0.5mg',680.00,12,4,1),
 (5,'7460555','Ibuprofeno 400mg',45.00,8,2,1),
 (6,'7460666','Omeprazol 20mg',180.00,60,5,1),
 (7,'7460777','Azitromicina 500mg',320.00,30,1,1);
INSERT INTO medicamento VALUES
 (1,'Amoxicilina','Cápsulas',1,0),(2,'Paracetamol','Tabletas',0,0),(3,'Losartán','Tabletas',0,0),
 (4,'Alprazolam','Tabletas',1,1),(5,'Ibuprofeno','Tabletas',0,0),(6,'Omeprazol','Cápsulas',0,0),(7,'Azitromicina','Tabletas',1,0);
INSERT INTO lote (codigo_lote, fecha_vencimiento, cantidad, id_producto) VALUES
 ('L-2026-04', DATE_ADD(CURDATE(), INTERVAL 60 DAY), 45, 1),
 ('L-2027-01', DATE_ADD(CURDATE(), INTERVAL 400 DAY), 120, 2),
 ('L-2026-02', DATE_ADD(CURDATE(), INTERVAL 20 DAY), 18, 3),
 ('L-CTRL-09', DATE_ADD(CURDATE(), INTERVAL 300 DAY), 12, 4),
 ('L-2026-01', DATE_ADD(CURDATE(), INTERVAL 10 DAY), 8, 5),
 ('L-2027-03', DATE_ADD(CURDATE(), INTERVAL 450 DAY), 60, 6),
 ('L-2027-02', DATE_ADD(CURDATE(), INTERVAL 80 DAY), 30, 7);
INSERT INTO inventario (id_producto, existencia, stock_minimo, ultima_actualizacion)
SELECT id_producto, stock, 10, NOW() FROM producto;
INSERT INTO comprobante_fiscal (tipo, descripcion) VALUES ('B01','Crédito Fiscal'),('B02','Consumidor Final'),('B14','Regímenes Especiales'),('B15','Gubernamental');
INSERT INTO descuento (nombre, porcentaje) VALUES ('Sin descuento',0),('Adulto mayor',10),('Seguro médico',15);
INSERT INTO caja (id_usuario, monto_base, apertura, abierta) VALUES (3, 5000.00, NOW(), 1);
INSERT INTO medico (nombre, exequatur, especialidad) VALUES ('Dra. Laura Méndez','Exequátur 11245','Medicina general');
INSERT INTO cliente (nombre, cedula_rnc, telefono, email) VALUES
 ('María Pérez','001-0000001-1','809-555-0111','maria@correo.test'),
 ('José Martínez','001-0000002-2','809-555-0222',NULL),
 ('Farmacia Vecina SRL','1-31-00001-1','809-555-0333','compras@vecina.test');
INSERT INTO log_actividad (id_usuario, accion, modulo, fecha) VALUES (3,'Apertura de caja #001 con base RD$ 5,000.00','Caja',NOW());


-- Facturas de ejemplo de los últimos 7 días (alimentan el dashboard y los reportes)
INSERT INTO factura (id_factura, numero, ncf, fecha, subtotal, descuento_aplicado, itbis, total, id_cliente, id_usuario, id_caja, id_comprobante, id_descuento) VALUES
 (1,'F-00001','B0200000001',DATE_SUB(CONCAT(CURDATE(),' 08:40:00'), INTERVAL 6 DAY),375.00,0.00,67.50,442.50,NULL,3,1,2,1),
 (2,'F-00002','B0200000002',DATE_SUB(CONCAT(CURDATE(),' 11:50:00'), INTERVAL 6 DAY),640.00,0.00,115.20,755.20,NULL,2,1,2,1),
 (3,'F-00003','B0200000003',DATE_SUB(CONCAT(CURDATE(),' 14:23:00'), INTERVAL 6 DAY),1355.00,203.25,207.31,1359.06,NULL,3,1,2,3),
 (4,'F-00004','B0200000004',DATE_SUB(CONCAT(CURDATE(),' 17:41:00'), INTERVAL 6 DAY),2130.00,0.00,383.40,2513.40,NULL,3,1,2,1),
 (5,'F-00005','B0200000005',DATE_SUB(CONCAT(CURDATE(),' 08:54:00'), INTERVAL 5 DAY),2020.00,303.00,309.06,2026.06,NULL,2,1,2,3),
 (6,'F-00006','B0200000006',DATE_SUB(CONCAT(CURDATE(),' 11:55:00'), INTERVAL 5 DAY),420.00,63.00,64.26,421.26,NULL,3,1,2,3),
 (7,'F-00007','B0200000007',DATE_SUB(CONCAT(CURDATE(),' 08:34:00'), INTERVAL 4 DAY),250.00,0.00,45.00,295.00,NULL,3,1,2,1),
 (8,'F-00008','B0200000008',DATE_SUB(CONCAT(CURDATE(),' 11:38:00'), INTERVAL 4 DAY),45.00,4.50,7.29,47.79,NULL,3,1,2,2),
 (9,'F-00009','B0200000009',DATE_SUB(CONCAT(CURDATE(),' 14:37:00'), INTERVAL 4 DAY),1055.00,158.25,161.41,1058.16,NULL,3,1,2,3),
 (10,'F-00010','B0200000010',DATE_SUB(CONCAT(CURDATE(),' 08:22:00'), INTERVAL 3 DAY),285.00,28.50,46.17,302.67,NULL,3,1,2,2),
 (11,'F-00011','B0200000011',DATE_SUB(CONCAT(CURDATE(),' 11:02:00'), INTERVAL 3 DAY),630.00,0.00,113.40,743.40,NULL,2,1,2,1),
 (12,'F-00012','B0200000012',DATE_SUB(CONCAT(CURDATE(),' 08:44:00'), INTERVAL 2 DAY),835.00,0.00,150.30,985.30,NULL,3,1,2,1),
 (13,'F-00013','B0200000013',DATE_SUB(CONCAT(CURDATE(),' 11:18:00'), INTERVAL 2 DAY),920.00,0.00,165.60,1085.60,NULL,3,1,2,1),
 (14,'F-00014','B0200000014',DATE_SUB(CONCAT(CURDATE(),' 08:54:00'), INTERVAL 1 DAY),180.00,0.00,32.40,212.40,NULL,3,1,2,1),
 (15,'F-00015','B0200000015',DATE_SUB(CONCAT(CURDATE(),' 11:01:00'), INTERVAL 1 DAY),1895.00,284.25,289.94,1900.69,NULL,2,1,2,3),
 (16,'F-00016','B0200000016',DATE_SUB(CONCAT(CURDATE(),' 08:14:00'), INTERVAL 0 DAY),45.00,4.50,7.29,47.79,NULL,3,1,2,2),
 (17,'F-00017','B0200000017',DATE_SUB(CONCAT(CURDATE(),' 11:32:00'), INTERVAL 0 DAY),1590.00,0.00,286.20,1876.20,NULL,3,1,2,1);
INSERT INTO detalle_factura (id_factura, id_producto, cantidad, precio_unitario, subtotal) VALUES
 (1,2,1,35.00,35.00),
 (1,5,2,45.00,90.00),
 (1,1,1,250.00,250.00),
 (2,7,2,320.00,640.00),
 (3,2,1,35.00,35.00),
 (3,7,3,320.00,960.00),
 (3,6,2,180.00,360.00),
 (4,1,3,250.00,750.00),
 (4,7,3,320.00,960.00),
 (4,3,1,420.00,420.00),
 (5,7,2,320.00,640.00),
 (5,6,3,180.00,540.00),
 (5,3,2,420.00,840.00),
 (6,3,1,420.00,420.00),
 (7,1,1,250.00,250.00),
 (8,5,1,45.00,45.00),
 (9,3,1,420.00,420.00),
 (9,1,2,250.00,500.00),
 (9,5,3,45.00,135.00),
 (10,1,1,250.00,250.00),
 (10,2,1,35.00,35.00),
 (11,6,3,180.00,540.00),
 (11,5,2,45.00,90.00),
 (12,1,1,250.00,250.00),
 (12,6,3,180.00,540.00),
 (12,5,1,45.00,45.00),
 (13,1,2,250.00,500.00),
 (13,3,1,420.00,420.00),
 (14,6,1,180.00,180.00),
 (15,1,2,250.00,500.00),
 (15,3,3,420.00,1260.00),
 (15,5,3,45.00,135.00),
 (16,5,1,45.00,45.00),
 (17,6,3,180.00,540.00),
 (17,5,2,45.00,90.00),
 (17,7,3,320.00,960.00);
INSERT INTO pago (id_factura, metodo, monto, fecha) VALUES
 (1,'Tarjeta',442.50,DATE_SUB(CONCAT(CURDATE(),' 08:40:00'), INTERVAL 6 DAY)),
 (2,'Efectivo',755.20,DATE_SUB(CONCAT(CURDATE(),' 11:50:00'), INTERVAL 6 DAY)),
 (3,'Mixto',1359.06,DATE_SUB(CONCAT(CURDATE(),' 14:23:00'), INTERVAL 6 DAY)),
 (4,'Tarjeta',2513.40,DATE_SUB(CONCAT(CURDATE(),' 17:41:00'), INTERVAL 6 DAY)),
 (5,'Efectivo',2026.06,DATE_SUB(CONCAT(CURDATE(),' 08:54:00'), INTERVAL 5 DAY)),
 (6,'Tarjeta',421.26,DATE_SUB(CONCAT(CURDATE(),' 11:55:00'), INTERVAL 5 DAY)),
 (7,'Efectivo',295.00,DATE_SUB(CONCAT(CURDATE(),' 08:34:00'), INTERVAL 4 DAY)),
 (8,'Mixto',47.79,DATE_SUB(CONCAT(CURDATE(),' 11:38:00'), INTERVAL 4 DAY)),
 (9,'Tarjeta',1058.16,DATE_SUB(CONCAT(CURDATE(),' 14:37:00'), INTERVAL 4 DAY)),
 (10,'Efectivo',302.67,DATE_SUB(CONCAT(CURDATE(),' 08:22:00'), INTERVAL 3 DAY)),
 (11,'Tarjeta',743.40,DATE_SUB(CONCAT(CURDATE(),' 11:02:00'), INTERVAL 3 DAY)),
 (12,'Efectivo',985.30,DATE_SUB(CONCAT(CURDATE(),' 08:44:00'), INTERVAL 2 DAY)),
 (13,'Mixto',1085.60,DATE_SUB(CONCAT(CURDATE(),' 11:18:00'), INTERVAL 2 DAY)),
 (14,'Tarjeta',212.40,DATE_SUB(CONCAT(CURDATE(),' 08:54:00'), INTERVAL 1 DAY)),
 (15,'Efectivo',1900.69,DATE_SUB(CONCAT(CURDATE(),' 11:01:00'), INTERVAL 1 DAY)),
 (16,'Tarjeta',47.79,DATE_SUB(CONCAT(CURDATE(),' 08:14:00'), INTERVAL 0 DAY)),
 (17,'Efectivo',1876.20,DATE_SUB(CONCAT(CURDATE(),' 11:32:00'), INTERVAL 0 DAY));
INSERT INTO bitacora_acceso (id_usuario, username, fecha, resultado, detalle) VALUES
 (1,'admin',NOW(),'EXITOSO','Dato de ejemplo'),
 (NULL,'desconocido',NOW(),'FALLIDO','Usuario inexistente');

-- Verificación
SHOW TABLES;
SELECT u.username, r.nombre AS rol, u.totp_activo FROM usuario u JOIN rol r ON r.id_rol = u.id_rol;
SELECT COUNT(*) AS facturas, ROUND(SUM(total),2) AS ventas FROM factura;
