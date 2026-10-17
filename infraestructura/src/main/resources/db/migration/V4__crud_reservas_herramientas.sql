ALTER TABLE herramientas ADD COLUMN imagen_url VARCHAR(500) NULL;

ALTER TABLE reservas ADD COLUMN fecha_entrega TIMESTAMP NULL;
ALTER TABLE reservas ADD COLUMN fecha_devolucion TIMESTAMP NULL;
ALTER TABLE reservas ADD COLUMN estado_devolucion VARCHAR(50) NULL;

CREATE INDEX idx_herramientas_proveedor_id ON herramientas(proveedor_id);
CREATE INDEX idx_herramientas_categoria_id ON herramientas(categoria_id);
CREATE INDEX idx_reservas_cliente_id ON reservas(cliente_id);
CREATE INDEX idx_reservas_herramienta_id ON reservas(herramienta_id);
CREATE INDEX idx_pagos_reserva_id ON pagos(reserva_id);
