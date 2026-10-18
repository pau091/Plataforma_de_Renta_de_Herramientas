-- Cancelación de reservas y reembolso de pagos (CRUD completo de reservas y pagos).
--   reservas.estado admite CANCELADA: el cliente cancela antes de pagar o el admin reembolsa antes de la entrega.
--   pagos.estado_pago admite REEMBOLSADO: el pago deja de contar como ingreso.
--   facturas.estado: VIGENTE o ANULADA (la factura de un pago reembolsado se conserva como historial).
ALTER TABLE facturas ADD COLUMN estado VARCHAR(20) NOT NULL DEFAULT 'VIGENTE' AFTER metodo_pago;

-- El reporte de incidencias busca las devoluciones con daño o pérdida.
CREATE INDEX idx_reservas_estado_devolucion ON reservas(estado_devolucion);
