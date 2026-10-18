-- Revisión de índices y reglas de integridad en la base de datos (Fase 4).

-- Validación de cruce de fechas (existsSolape): filtra por herramienta y rango de fechas.
-- El índice compuesto empieza por herramienta_id, así que también sirve a su llave foránea
-- y reemplaza al índice simple.
CREATE INDEX idx_reservas_herramienta_fechas ON reservas(herramienta_id, fecha_inicio, fecha_fin);
DROP INDEX idx_reservas_herramienta_id ON reservas;

-- Catálogo filtrado por disponibilidad (GET /api/herramientas?disponibles=true).
CREATE INDEX idx_herramientas_estado ON herramientas(estado);

-- El nombre de la categoría es único (la intercalación por defecto ya ignora mayúsculas y tildes).
ALTER TABLE categorias ADD CONSTRAINT uk_categorias_nombre UNIQUE (nombre);

-- Reglas de negocio básicas también protegidas en la base de datos.
ALTER TABLE herramientas
    ADD CONSTRAINT chk_herramientas_tarifa CHECK (tarifa_diaria > 0),
    ADD CONSTRAINT chk_herramientas_estado CHECK (estado IN ('DISPONIBLE', 'MANTENIMIENTO', 'FUERA_DE_SERVICIO'));

ALTER TABLE reservas
    ADD CONSTRAINT chk_reservas_fechas CHECK (fecha_fin >= fecha_inicio),
    ADD CONSTRAINT chk_reservas_estado CHECK (estado IN ('PENDIENTE', 'ACEPTADA', 'CONFIRMADA', 'ENTREGADA',
                                                         'FINALIZADA', 'RECHAZADA', 'VENCIDA', 'CANCELADA')),
    ADD CONSTRAINT chk_reservas_estado_devolucion CHECK (estado_devolucion IS NULL
                                                         OR estado_devolucion IN ('BUENO', 'DANADO', 'PERDIDO'));

ALTER TABLE pagos
    ADD CONSTRAINT chk_pagos_monto CHECK (monto > 0),
    ADD CONSTRAINT chk_pagos_metodo CHECK (metodo_pago IN ('TARJETA', 'TRANSFERENCIA', 'EFECTIVO')),
    ADD CONSTRAINT chk_pagos_estado CHECK (estado_pago IN ('PENDIENTE', 'COMPLETADO', 'RECHAZADO', 'REEMBOLSADO'));

ALTER TABLE facturas
    ADD CONSTRAINT chk_facturas_estado CHECK (estado IN ('VIGENTE', 'ANULADA'));

ALTER TABLE mantenimientos
    ADD CONSTRAINT chk_mantenimientos_costo CHECK (costo_reparacion IS NULL OR costo_reparacion >= 0);
