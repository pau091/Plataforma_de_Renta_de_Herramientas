CREATE TABLE roles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE usuarios (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    activo BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE categorias (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    descripcion TEXT
);

CREATE TABLE herramientas (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    proveedor_id BIGINT NOT NULL,
    categoria_id BIGINT,
    nombre VARCHAR(150) NOT NULL,
    descripcion TEXT,
    tarifa_diaria DECIMAL(12,2) NOT NULL,
    estado VARCHAR(30) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_herramientas_proveedor FOREIGN KEY (proveedor_id) REFERENCES usuarios(id) ON DELETE CASCADE,
    CONSTRAINT fk_herramientas_categoria FOREIGN KEY (categoria_id) REFERENCES categorias(id)
);

CREATE TABLE reservas (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    cliente_id BIGINT NOT NULL,
    herramienta_id BIGINT NOT NULL,
    fecha_inicio DATE NOT NULL,
    fecha_fin DATE NOT NULL,
    estado VARCHAR(30) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_reservas_cliente FOREIGN KEY (cliente_id) REFERENCES usuarios(id) ON DELETE CASCADE,
    CONSTRAINT fk_reservas_herramienta FOREIGN KEY (herramienta_id) REFERENCES herramientas(id) ON DELETE CASCADE
);

CREATE TABLE pagos (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    reserva_id BIGINT NOT NULL,
    monto DECIMAL(12,2) NOT NULL,
    metodo_pago VARCHAR(50) NOT NULL,
    estado_pago VARCHAR(30) NOT NULL,
    fecha_pago TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_pagos_reserva FOREIGN KEY (reserva_id) REFERENCES reservas(id) ON DELETE CASCADE
);

CREATE TABLE mantenimientos (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    herramienta_id BIGINT NOT NULL,
    descripcion_problema TEXT NOT NULL,
    costo_reparacion DECIMAL(12,2),
    fecha_inicio TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fecha_fin TIMESTAMP NULL,
    CONSTRAINT fk_mantenimientos_herramienta FOREIGN KEY (herramienta_id) REFERENCES herramientas(id) ON DELETE CASCADE
);
