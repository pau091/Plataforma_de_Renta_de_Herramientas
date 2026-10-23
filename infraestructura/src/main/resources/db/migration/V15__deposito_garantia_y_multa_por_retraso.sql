-- Depósito de garantía y multa por devolución tardía.
-- El proveedor fija el depósito de cada herramienta; se cobra junto con el alquiler y queda retenido. Al devolver
-- se liquida: se descuentan la multa por retraso (tarifa + recargo por cada día después de la fecha fin) y los
-- daños (costo de la reparación); si se pierde la herramienta, no se devuelve. Lo que no alcance queda como saldo.

ALTER TABLE herramientas
    ADD COLUMN deposito DECIMAL(12,2) NOT NULL DEFAULT 0,
    ADD CONSTRAINT chk_herramientas_deposito CHECK (deposito >= 0);

-- Estado y liquidación del depósito de cada reserva (el dato de negocio).
ALTER TABLE reservas
    ADD COLUMN deposito          DECIMAL(12,2) NOT NULL DEFAULT 0,
    ADD COLUMN estado_deposito   VARCHAR(30)   NULL,
    ADD COLUMN dias_retraso      INT           NOT NULL DEFAULT 0,
    ADD COLUMN multa_retraso     DECIMAL(12,2) NOT NULL DEFAULT 0,
    ADD COLUMN cargo_danos       DECIMAL(12,2) NOT NULL DEFAULT 0,
    ADD COLUMN deposito_devuelto DECIMAL(12,2) NOT NULL DEFAULT 0,
    ADD COLUMN saldo_pendiente   DECIMAL(12,2) NOT NULL DEFAULT 0,
    ADD CONSTRAINT chk_reservas_estado_deposito CHECK (estado_deposito IS NULL OR estado_deposito IN
        ('SIN_DEPOSITO', 'RETENIDO', 'PENDIENTE_REPARACION', 'DEVUELTO', 'DEVUELTO_PARCIAL', 'NO_DEVUELTO', 'LIQUIDADO', 'REEMBOLSADO')),
    ADD CONSTRAINT chk_reservas_garantia_montos CHECK (deposito >= 0 AND dias_retraso >= 0 AND multa_retraso >= 0
        AND cargo_danos >= 0 AND deposito_devuelto >= 0 AND saldo_pendiente >= 0);

-- La factura guarda una copia, como el resto de sus datos: el comprobante muestra la multa y la liquidación.
ALTER TABLE facturas
    ADD COLUMN deposito          DECIMAL(12,2) NOT NULL DEFAULT 0,
    ADD COLUMN estado_deposito   VARCHAR(30)   NULL,
    ADD COLUMN dias_retraso      INT           NOT NULL DEFAULT 0,
    ADD COLUMN multa_retraso     DECIMAL(12,2) NOT NULL DEFAULT 0,
    ADD COLUMN cargo_danos       DECIMAL(12,2) NOT NULL DEFAULT 0,
    ADD COLUMN deposito_devuelto DECIMAL(12,2) NOT NULL DEFAULT 0,
    ADD COLUMN saldo_pendiente   DECIMAL(12,2) NOT NULL DEFAULT 0,
    ADD CONSTRAINT chk_facturas_garantia_montos CHECK (deposito >= 0 AND dias_retraso >= 0 AND multa_retraso >= 0
        AND cargo_danos >= 0 AND deposito_devuelto >= 0 AND saldo_pendiente >= 0);

-- ---------------------------------------------------------------------------------------- datos de demostración
-- Las herramientas de demostración piden un depósito equivalente a 2 días de alquiler.
UPDATE herramientas SET deposito = tarifa_diaria * 2;

-- Alquileres en curso (pagados y aún no devueltos): el depósito se cobró con el pago y está retenido.
UPDATE reservas r JOIN herramientas h ON h.id = r.herramienta_id
   SET r.deposito = h.deposito, r.estado_deposito = 'RETENIDO'
 WHERE r.estado IN ('CONFIRMADA', 'ENTREGADA');
UPDATE pagos p JOIN reservas r ON r.id = p.reserva_id
   SET p.monto = p.monto + r.deposito
 WHERE r.estado IN ('CONFIRMADA', 'ENTREGADA') AND p.estado_pago = 'COMPLETADO';
UPDATE facturas f JOIN reservas r ON r.id = f.reserva_id
   SET f.deposito = r.deposito, f.estado_deposito = 'RETENIDO', f.total = f.subtotal + r.deposito
 WHERE r.estado IN ('CONFIRMADA', 'ENTREGADA');

-- Historial anterior al depósito: los alquileres terminados se liquidaron sin depósito y el reembolsado lo devolvió.
UPDATE reservas SET estado_deposito = 'LIQUIDADO' WHERE estado = 'FINALIZADA';
UPDATE reservas r JOIN pagos p ON p.reserva_id = r.id
   SET r.estado_deposito = 'REEMBOLSADO'
 WHERE r.estado = 'CANCELADA' AND p.estado_pago = 'REEMBOLSADO';
UPDATE facturas f JOIN reservas r ON r.id = f.reserva_id
   SET f.estado_deposito = r.estado_deposito
 WHERE r.estado IN ('FINALIZADA', 'CANCELADA');

-- Para demostrar la multa: la reserva 15 (Rotomartillo SDS Plus, ya entregada) debía devolverse hace 2 días.
-- Conserva sus 4 días de alquiler, así el monto pagado sigue siendo tarifa × días + depósito.
UPDATE reservas
   SET fecha_inicio = CURDATE() - INTERVAL 5 DAY, fecha_fin = CURDATE() - INTERVAL 2 DAY,
       created_at = TIMESTAMP(CURDATE() - INTERVAL 8 DAY, '09:00:00'),
       fecha_aceptacion = TIMESTAMP(CURDATE() - INTERVAL 8 DAY, '09:00:00') + INTERVAL 30 MINUTE,
       fecha_entrega = TIMESTAMP(CURDATE() - INTERVAL 5 DAY, '08:00:00')
 WHERE id = 15 AND estado = 'ENTREGADA';
UPDATE facturas f JOIN reservas r ON r.id = f.reserva_id
   SET f.fecha_inicio = r.fecha_inicio, f.fecha_fin = r.fecha_fin
 WHERE r.id = 15;
