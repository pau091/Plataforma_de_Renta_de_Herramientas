-- Nuevo flujo de reservas:
--   PENDIENTE -> (proveedor acepta) ACEPTADA -> (cliente paga) CONFIRMADA -> ENTREGADA -> FINALIZADA
-- Una reserva PENDIENTE o ACEPTADA sin pagar vence si no avanza dentro del plazo máximo (1 hora).

-- Instante (UTC) en que el proveedor aceptó la reserva: desde aquí corre el plazo para pagarla.
ALTER TABLE reservas ADD COLUMN fecha_aceptacion TIMESTAMP NULL AFTER created_at;

-- Las reservas ya aceptadas y pagadas quedan CONFIRMADAS (confirmación automática por pago).
UPDATE reservas r
SET r.estado = 'CONFIRMADA'
WHERE r.estado = 'ACEPTADA'
  AND EXISTS (SELECT 1 FROM pagos p WHERE p.reserva_id = r.id AND p.estado_pago = 'COMPLETADO');

-- Para las reservas que ya pasaron por la aceptación se toma su creación como referencia.
UPDATE reservas
SET fecha_aceptacion = created_at
WHERE estado IN ('ACEPTADA', 'CONFIRMADA', 'ENTREGADA', 'FINALIZADA');

-- El vencimiento consulta las reservas por estado cada minuto.
CREATE INDEX idx_reservas_estado ON reservas(estado);
