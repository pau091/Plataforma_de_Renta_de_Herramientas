-- Calificaciones y reseñas: al terminar un alquiler (reserva FINALIZADA), quien lo hizo califica la herramienta y/o
-- al proveedor de 1 a 5 estrellas, una sola vez. El catálogo muestra el promedio.
CREATE TABLE calificaciones (
    id                    BIGINT AUTO_INCREMENT PRIMARY KEY,
    reserva_id            BIGINT NOT NULL,
    cliente_id            BIGINT NOT NULL,
    cliente_nombre        VARCHAR(100) NULL,
    herramienta_id        BIGINT NOT NULL,
    proveedor_id          BIGINT NOT NULL,
    estrellas_herramienta INT NULL,
    estrellas_proveedor   INT NULL,
    comentario            VARCHAR(500) NULL,
    fecha                 TIMESTAMP NOT NULL,
    -- Una sola reseña por alquiler, aunque lleguen dos peticiones a la vez.
    CONSTRAINT uk_calificaciones_reserva UNIQUE (reserva_id),
    CONSTRAINT fk_calificaciones_reserva FOREIGN KEY (reserva_id) REFERENCES reservas(id) ON DELETE RESTRICT,
    CONSTRAINT fk_calificaciones_cliente FOREIGN KEY (cliente_id) REFERENCES usuarios(id) ON DELETE RESTRICT,
    CONSTRAINT fk_calificaciones_herramienta FOREIGN KEY (herramienta_id) REFERENCES herramientas(id) ON DELETE RESTRICT,
    CONSTRAINT fk_calificaciones_proveedor FOREIGN KEY (proveedor_id) REFERENCES usuarios(id) ON DELETE RESTRICT,
    CONSTRAINT chk_calificaciones_estrellas CHECK ((estrellas_herramienta IS NULL OR estrellas_herramienta BETWEEN 1 AND 5)
        AND (estrellas_proveedor IS NULL OR estrellas_proveedor BETWEEN 1 AND 5)
        AND (estrellas_herramienta IS NOT NULL OR estrellas_proveedor IS NOT NULL))
);
CREATE INDEX idx_calificaciones_herramienta ON calificaciones(herramienta_id);
CREATE INDEX idx_calificaciones_proveedor ON calificaciones(proveedor_id);

-- ---------------------------------------------------------------------------------------- datos de demostración
-- Reseñas de los alquileres terminados. Quedan sin calificar las reservas con id múltiplo de 4 (por ejemplo, la 32
-- de juan.cliente@gmail.com) para poder calificarlas en la demostración.
INSERT INTO calificaciones (reserva_id, cliente_id, cliente_nombre, herramienta_id, proveedor_id,
                            estrellas_herramienta, estrellas_proveedor, comentario, fecha)
SELECT r.id, r.cliente_id, u.nombre, r.herramienta_id, h.proveedor_id,
       CASE r.estado_devolucion WHEN 'BUENO' THEN 4 + (r.id % 2) WHEN 'DANADO' THEN 3 ELSE 2 END,
       CASE WHEN r.id % 3 = 0 THEN 4 ELSE 5 END,
       CASE r.estado_devolucion
           WHEN 'BUENO' THEN 'Funcionó muy bien y el proveedor la entregó a tiempo.'
           WHEN 'DANADO' THEN 'Cumplió, aunque presentó una falla durante el uso.'
           ELSE 'Buena atención del proveedor a pesar del inconveniente.' END,
       COALESCE(r.fecha_devolucion, TIMESTAMP(CURDATE(), '12:00:00')) + INTERVAL 2 HOUR
  FROM reservas r
  JOIN herramientas h ON h.id = r.herramienta_id
  JOIN usuarios u ON u.id = r.cliente_id
 WHERE r.estado = 'FINALIZADA' AND r.id % 4 <> 0;
