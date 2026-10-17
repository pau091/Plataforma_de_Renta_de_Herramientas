-- Antes, borrar un usuario o una herramienta eliminaba en cascada sus reservas, pagos y facturas.
-- Ahora la base de datos rechaza ese borrado (RESTRICT) para conservar el historial; la aplicación
-- sugiere desactivar el usuario o marcar la herramienta como FUERA_DE_SERVICIO.
-- usuario_roles conserva CASCADE (es solo la tabla puente) y mantenimientos.reserva_id conserva SET NULL.

ALTER TABLE herramientas DROP FOREIGN KEY fk_herramientas_proveedor;
ALTER TABLE herramientas ADD CONSTRAINT fk_herramientas_proveedor
    FOREIGN KEY (proveedor_id) REFERENCES usuarios(id) ON DELETE RESTRICT;

ALTER TABLE reservas DROP FOREIGN KEY fk_reservas_cliente;
ALTER TABLE reservas ADD CONSTRAINT fk_reservas_cliente
    FOREIGN KEY (cliente_id) REFERENCES usuarios(id) ON DELETE RESTRICT;

ALTER TABLE reservas DROP FOREIGN KEY fk_reservas_herramienta;
ALTER TABLE reservas ADD CONSTRAINT fk_reservas_herramienta
    FOREIGN KEY (herramienta_id) REFERENCES herramientas(id) ON DELETE RESTRICT;

ALTER TABLE pagos DROP FOREIGN KEY fk_pagos_reserva;
ALTER TABLE pagos ADD CONSTRAINT fk_pagos_reserva
    FOREIGN KEY (reserva_id) REFERENCES reservas(id) ON DELETE RESTRICT;

ALTER TABLE mantenimientos DROP FOREIGN KEY fk_mantenimientos_herramienta;
ALTER TABLE mantenimientos ADD CONSTRAINT fk_mantenimientos_herramienta
    FOREIGN KEY (herramienta_id) REFERENCES herramientas(id) ON DELETE RESTRICT;

ALTER TABLE facturas DROP FOREIGN KEY fk_facturas_pago;
ALTER TABLE facturas ADD CONSTRAINT fk_facturas_pago
    FOREIGN KEY (pago_id) REFERENCES pagos(id) ON DELETE RESTRICT;

ALTER TABLE facturas DROP FOREIGN KEY fk_facturas_reserva;
ALTER TABLE facturas ADD CONSTRAINT fk_facturas_reserva
    FOREIGN KEY (reserva_id) REFERENCES reservas(id) ON DELETE RESTRICT;

ALTER TABLE facturas DROP FOREIGN KEY fk_facturas_cliente;
ALTER TABLE facturas ADD CONSTRAINT fk_facturas_cliente
    FOREIGN KEY (cliente_id) REFERENCES usuarios(id) ON DELETE RESTRICT;

ALTER TABLE facturas DROP FOREIGN KEY fk_facturas_herramienta;
ALTER TABLE facturas ADD CONSTRAINT fk_facturas_herramienta
    FOREIGN KEY (herramienta_id) REFERENCES herramientas(id) ON DELETE RESTRICT;
