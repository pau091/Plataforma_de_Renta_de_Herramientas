-- =====================================================================================================
-- Plataforma de Renta de Herramientas - Script de creación de la base de datos (MySQL 8)
-- =====================================================================================================
-- Esquema final equivalente al que construye Flyway al arrancar la aplicación con las migraciones
-- V1 a V14 (infraestructura/src/main/resources/db/migration). Úselo para crear la base a mano y luego
-- poblarla con sql/data.sql:
--     mysql -u root -p < sql/schema.sql
--     mysql -u root -p < sql/data.sql
-- Si después arranca la aplicación sobre esta base, Flyway la registra como versión 12
-- (spring.flyway.baseline-version) y no vuelve a ejecutar las migraciones.
--
-- Modelo (3FN):
--   roles  N:M  usuarios          (tabla puente usuario_roles). Un usuario es ADMIN, PROVEEDOR o CLIENTE.
--   usuarios (proveedor) 1:N herramientas  N:1 categorias
--   usuarios (cliente)   1:N reservas      N:1 herramientas
--   reservas 1:N pagos, pagos 1:1 facturas, herramientas 1:N mantenimientos (opcionalmente ligados a la
--   reserva cuya devolución reportó el daño).
--   facturas guarda una copia de nombre, correo, herramienta y tarifa a propósito: un comprobante no debe
--   cambiar si después se edita el cliente o la herramienta.
--   Las llaves foráneas usan RESTRICT para conservar el historial (no se borran reservas, pagos ni facturas
--   en cascada); solo usuario_roles usa CASCADE y mantenimientos.reserva_id usa SET NULL.
-- =====================================================================================================

CREATE DATABASE IF NOT EXISTS renta_herramientas_db
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_0900_ai_ci;

USE renta_herramientas_db;

-- Catálogo de roles: ADMIN, PROVEEDOR y CLIENTE.
CREATE TABLE roles (
    id     BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(50) NOT NULL UNIQUE
);

-- Cuentas de acceso. La contraseña se guarda con BCrypt; activo = FALSE bloquea el inicio de sesión.
CREATE TABLE usuarios (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre     VARCHAR(100) NOT NULL,
    email      VARCHAR(150) NOT NULL UNIQUE,
    password   VARCHAR(255) NOT NULL,
    activo     BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    suspendido_hasta TIMESTAMP NULL,
    baneado          BOOLEAN NOT NULL DEFAULT FALSE,
    motivo_sancion   VARCHAR(255) NULL
);

CREATE TABLE usuario_roles (
    usuario_id BIGINT NOT NULL,
    rol_id     BIGINT NOT NULL,
    PRIMARY KEY (usuario_id, rol_id),
    CONSTRAINT fk_usuario_roles_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE,
    CONSTRAINT fk_usuario_roles_rol FOREIGN KEY (rol_id) REFERENCES roles(id) ON DELETE CASCADE
);
CREATE INDEX idx_usuario_roles_rol_id ON usuario_roles(rol_id);

CREATE TABLE categorias (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre      VARCHAR(100) NOT NULL,
    descripcion TEXT,
    CONSTRAINT uk_categorias_nombre UNIQUE (nombre)
);

-- Inventario publicado por cada proveedor.
CREATE TABLE herramientas (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    proveedor_id  BIGINT NOT NULL,
    categoria_id  BIGINT,
    nombre        VARCHAR(150) NOT NULL,
    descripcion   TEXT,
    tarifa_diaria DECIMAL(12,2) NOT NULL,
    estado        VARCHAR(30) NOT NULL,
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    imagen_url    VARCHAR(500) NULL,
    CONSTRAINT fk_herramientas_proveedor FOREIGN KEY (proveedor_id) REFERENCES usuarios(id) ON DELETE RESTRICT,
    CONSTRAINT fk_herramientas_categoria FOREIGN KEY (categoria_id) REFERENCES categorias(id),
    CONSTRAINT chk_herramientas_tarifa CHECK (tarifa_diaria > 0),
    CONSTRAINT chk_herramientas_estado CHECK (estado IN ('DISPONIBLE', 'MANTENIMIENTO', 'FUERA_DE_SERVICIO'))
);
CREATE INDEX idx_herramientas_proveedor_id ON herramientas(proveedor_id);
CREATE INDEX idx_herramientas_categoria_id ON herramientas(categoria_id);
CREATE INDEX idx_herramientas_estado ON herramientas(estado);

-- Alquileres. Flujo: PENDIENTE -> ACEPTADA -> (pago) CONFIRMADA -> ENTREGADA -> FINALIZADA,
-- o RECHAZADA / VENCIDA / CANCELADA. Los instantes (created_at, fecha_aceptacion...) se guardan en UTC.
CREATE TABLE reservas (
    id                       BIGINT AUTO_INCREMENT PRIMARY KEY,
    cliente_id               BIGINT NOT NULL,
    herramienta_id           BIGINT NOT NULL,
    fecha_inicio             DATE NOT NULL,
    fecha_fin                DATE NOT NULL,
    estado                   VARCHAR(30) NOT NULL,
    created_at               TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fecha_aceptacion         TIMESTAMP NULL,
    fecha_entrega            TIMESTAMP NULL,
    fecha_devolucion         TIMESTAMP NULL,
    estado_devolucion        VARCHAR(50) NULL,
    observaciones_devolucion TEXT NULL,
    version                  BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_reservas_cliente FOREIGN KEY (cliente_id) REFERENCES usuarios(id) ON DELETE RESTRICT,
    CONSTRAINT fk_reservas_herramienta FOREIGN KEY (herramienta_id) REFERENCES herramientas(id) ON DELETE RESTRICT,
    CONSTRAINT chk_reservas_fechas CHECK (fecha_fin >= fecha_inicio),
    CONSTRAINT chk_reservas_estado CHECK (estado IN ('PENDIENTE', 'ACEPTADA', 'CONFIRMADA', 'ENTREGADA',
                                                     'FINALIZADA', 'RECHAZADA', 'VENCIDA', 'CANCELADA')),
    CONSTRAINT chk_reservas_estado_devolucion CHECK (estado_devolucion IS NULL
                                                     OR estado_devolucion IN ('BUENO', 'DANADO', 'PERDIDO'))
);
CREATE INDEX idx_reservas_cliente_id ON reservas(cliente_id);
-- Validación de cruce de fechas: herramienta + rango (también sirve a la llave foránea).
CREATE INDEX idx_reservas_herramienta_fechas ON reservas(herramienta_id, fecha_inicio, fecha_fin);
-- Vencimiento automático (cada minuto) y paneles por estado.
CREATE INDEX idx_reservas_estado ON reservas(estado);
-- Reporte de incidencias (devoluciones con daño o pérdida).
CREATE INDEX idx_reservas_estado_devolucion ON reservas(estado_devolucion);

-- Pagos de reservas. Una reserva admite un solo pago COMPLETADO; REEMBOLSADO deja de contar como ingreso.
CREATE TABLE pagos (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    reserva_id  BIGINT NOT NULL,
    monto       DECIMAL(12,2) NOT NULL,
    metodo_pago VARCHAR(50) NOT NULL,
    estado_pago VARCHAR(30) NOT NULL,
    fecha_pago  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_pagos_reserva FOREIGN KEY (reserva_id) REFERENCES reservas(id) ON DELETE RESTRICT,
    CONSTRAINT chk_pagos_monto CHECK (monto > 0),
    CONSTRAINT chk_pagos_metodo CHECK (metodo_pago IN ('TARJETA', 'TRANSFERENCIA', 'EFECTIVO')),
    CONSTRAINT chk_pagos_estado CHECK (estado_pago IN ('PENDIENTE', 'COMPLETADO', 'RECHAZADO', 'REEMBOLSADO'))
);
CREATE INDEX idx_pagos_reserva_id ON pagos(reserva_id);

-- Reparaciones y mantenimientos. fecha_fin NULL = abierto (la herramienta queda en MANTENIMIENTO).
CREATE TABLE mantenimientos (
    id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
    herramienta_id       BIGINT NOT NULL,
    descripcion_problema TEXT NOT NULL,
    costo_reparacion     DECIMAL(12,2),
    fecha_inicio         TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fecha_fin            TIMESTAMP NULL,
    reserva_id           BIGINT NULL,
    CONSTRAINT fk_mantenimientos_herramienta FOREIGN KEY (herramienta_id) REFERENCES herramientas(id) ON DELETE RESTRICT,
    CONSTRAINT fk_mantenimientos_reserva FOREIGN KEY (reserva_id) REFERENCES reservas(id) ON DELETE SET NULL,
    CONSTRAINT chk_mantenimientos_costo CHECK (costo_reparacion IS NULL OR costo_reparacion >= 0)
);
CREATE INDEX idx_mantenimientos_reserva_id ON mantenimientos(reserva_id);

-- Comprobante generado automáticamente con cada pago (FAC-000001...). ANULADA si el pago se reembolsa.
CREATE TABLE facturas (
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
    numero             VARCHAR(30) NOT NULL UNIQUE,
    pago_id            BIGINT NOT NULL UNIQUE,
    reserva_id         BIGINT NOT NULL,
    cliente_id         BIGINT NOT NULL,
    cliente_nombre     VARCHAR(100) NOT NULL,
    cliente_email      VARCHAR(150) NOT NULL,
    herramienta_id     BIGINT NOT NULL,
    herramienta_nombre VARCHAR(150) NOT NULL,
    fecha_inicio       DATE NOT NULL,
    fecha_fin          DATE NOT NULL,
    dias               INT NOT NULL,
    tarifa_diaria      DECIMAL(12,2) NOT NULL,
    subtotal           DECIMAL(12,2) NOT NULL,
    total              DECIMAL(12,2) NOT NULL,
    metodo_pago        VARCHAR(50) NOT NULL,
    estado             VARCHAR(20) NOT NULL DEFAULT 'VIGENTE',
    fecha_emision      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_facturas_pago FOREIGN KEY (pago_id) REFERENCES pagos(id) ON DELETE RESTRICT,
    CONSTRAINT fk_facturas_reserva FOREIGN KEY (reserva_id) REFERENCES reservas(id) ON DELETE RESTRICT,
    CONSTRAINT fk_facturas_cliente FOREIGN KEY (cliente_id) REFERENCES usuarios(id) ON DELETE RESTRICT,
    CONSTRAINT fk_facturas_herramienta FOREIGN KEY (herramienta_id) REFERENCES herramientas(id) ON DELETE RESTRICT,
    CONSTRAINT chk_facturas_estado CHECK (estado IN ('VIGENTE', 'ANULADA'))
);
CREATE INDEX idx_facturas_cliente_id ON facturas(cliente_id);
CREATE INDEX idx_facturas_reserva_id ON facturas(reserva_id);

-- Tokens JWT emitidos y lista negra de tokens revocados (suspensión, baneo, desactivación o cierre de sesión).
CREATE TABLE tokens_emitidos (
    jti        VARCHAR(36) PRIMARY KEY,
    usuario_id BIGINT NOT NULL,
    emitido_en TIMESTAMP NOT NULL,
    expira_en  TIMESTAMP NOT NULL,
    CONSTRAINT fk_tokens_emitidos_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE
);
CREATE INDEX idx_tokens_emitidos_usuario ON tokens_emitidos(usuario_id, expira_en);

CREATE TABLE tokens_revocados (
    jti         VARCHAR(36) PRIMARY KEY,
    usuario_id  BIGINT NOT NULL,
    motivo      VARCHAR(300) NOT NULL,
    revocado_en TIMESTAMP NOT NULL,
    expira_en   TIMESTAMP NOT NULL,
    CONSTRAINT fk_tokens_revocados_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE
);
CREATE INDEX idx_tokens_revocados_expira ON tokens_revocados(expira_en);
