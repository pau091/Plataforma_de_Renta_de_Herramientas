-- Bloqueo optimista (@Version en ReservaEntity): cada actualización de una reserva incrementa este número y
-- exige que no haya cambiado desde que se leyó. Así dos operaciones simultáneas sobre la misma reserva
-- (por ejemplo, dos pagos a la vez, o un pago mientras se vence) no se pisan: la segunda falla con 409.
ALTER TABLE reservas ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
