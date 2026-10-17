ALTER TABLE reservas ADD COLUMN observaciones_devolucion TEXT NULL;

ALTER TABLE mantenimientos ADD COLUMN reserva_id BIGINT NULL;
ALTER TABLE mantenimientos ADD CONSTRAINT fk_mantenimientos_reserva
    FOREIGN KEY (reserva_id) REFERENCES reservas(id) ON DELETE SET NULL;

CREATE TABLE facturas (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    numero VARCHAR(30) NOT NULL UNIQUE,
    pago_id BIGINT NOT NULL UNIQUE,
    reserva_id BIGINT NOT NULL,
    cliente_id BIGINT NOT NULL,
    cliente_nombre VARCHAR(100) NOT NULL,
    cliente_email VARCHAR(150) NOT NULL,
    herramienta_id BIGINT NOT NULL,
    herramienta_nombre VARCHAR(150) NOT NULL,
    fecha_inicio DATE NOT NULL,
    fecha_fin DATE NOT NULL,
    dias INT NOT NULL,
    tarifa_diaria DECIMAL(12,2) NOT NULL,
    subtotal DECIMAL(12,2) NOT NULL,
    total DECIMAL(12,2) NOT NULL,
    metodo_pago VARCHAR(50) NOT NULL,
    fecha_emision TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_facturas_pago FOREIGN KEY (pago_id) REFERENCES pagos(id) ON DELETE CASCADE,
    CONSTRAINT fk_facturas_reserva FOREIGN KEY (reserva_id) REFERENCES reservas(id) ON DELETE CASCADE,
    CONSTRAINT fk_facturas_cliente FOREIGN KEY (cliente_id) REFERENCES usuarios(id) ON DELETE CASCADE,
    CONSTRAINT fk_facturas_herramienta FOREIGN KEY (herramienta_id) REFERENCES herramientas(id) ON DELETE CASCADE
);

CREATE INDEX idx_facturas_cliente_id ON facturas(cliente_id);
CREATE INDEX idx_facturas_reserva_id ON facturas(reserva_id);
CREATE INDEX idx_mantenimientos_reserva_id ON mantenimientos(reserva_id);
