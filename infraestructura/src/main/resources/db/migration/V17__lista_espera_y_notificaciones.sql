-- Lista de espera: clientes que esperan fechas ocupadas de una herramienta. Si la reserva que las ocupa se cancela,
-- se rechaza, vence o se reembolsa, se notifica al primero de la fila (orden de llegada = creada_en).
CREATE TABLE lista_espera (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    herramienta_id BIGINT NOT NULL,
    cliente_id     BIGINT NOT NULL,
    fecha_inicio   DATE NOT NULL,
    fecha_fin      DATE NOT NULL,
    estado         VARCHAR(20) NOT NULL DEFAULT 'ESPERANDO',
    creada_en      TIMESTAMP NOT NULL,
    notificada_en  TIMESTAMP NULL,
    -- No es historial contable: si se elimina la herramienta o el usuario, sus esperas se eliminan con ellos.
    CONSTRAINT fk_lista_espera_herramienta FOREIGN KEY (herramienta_id) REFERENCES herramientas(id) ON DELETE CASCADE,
    CONSTRAINT fk_lista_espera_cliente FOREIGN KEY (cliente_id) REFERENCES usuarios(id) ON DELETE CASCADE,
    CONSTRAINT chk_lista_espera_fechas CHECK (fecha_fin >= fecha_inicio),
    CONSTRAINT chk_lista_espera_estado CHECK (estado IN ('ESPERANDO', 'NOTIFICADA', 'ATENDIDA', 'CANCELADA'))
);
CREATE INDEX idx_lista_espera_fila ON lista_espera(herramienta_id, estado, creada_en);

-- Avisos que el usuario ve en la campana de la página.
CREATE TABLE notificaciones (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    usuario_id     BIGINT NOT NULL,
    tipo           VARCHAR(30) NOT NULL,
    titulo         VARCHAR(150) NOT NULL,
    mensaje        VARCHAR(500) NOT NULL,
    herramienta_id BIGINT NULL,
    leida          BOOLEAN NOT NULL DEFAULT FALSE,
    creada_en      TIMESTAMP NOT NULL,
    CONSTRAINT fk_notificaciones_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE,
    CONSTRAINT fk_notificaciones_herramienta FOREIGN KEY (herramienta_id) REFERENCES herramientas(id) ON DELETE SET NULL
);
CREATE INDEX idx_notificaciones_usuario ON notificaciones(usuario_id, leida, creada_en);

-- ---------------------------------------------------------------------------------------- datos de demostración
-- ana.rentas@gmail.com espera las fechas de la reserva 5 (Sierra Circular de Banco, ya pagada). Si el administrador
-- reembolsa ese pago, la reserva se cancela y Ana recibe el aviso en la campana.
INSERT INTO lista_espera (herramienta_id, cliente_id, fecha_inicio, fecha_fin, estado, creada_en)
SELECT r.herramienta_id, u.id, r.fecha_inicio, r.fecha_fin, 'ESPERANDO', UTC_TIMESTAMP() - INTERVAL 1 DAY
  FROM reservas r JOIN usuarios u ON u.email = 'ana.rentas@gmail.com'
 WHERE r.id = 5 AND r.estado = 'CONFIRMADA';
