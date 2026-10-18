-- Datos de demostración coherentes con las reglas de negocio (Fase 4).
-- Corrige los datos de V2/V6 (pagos de reservas no pagadas, montos que no eran tarifa × días,
-- reservas finalizadas sin entrega ni devolución) y completa al menos 20 registros por entidad:
--   41 usuarios (1 admin, 20 proveedores, 20 clientes), 30 herramientas, 34 reservas, 24 pagos,
--   24 facturas, 21 mantenimientos y 20 categorías. Clave de todas las cuentas: Rentas2026*
--   Estados de reserva incluidos: FINALIZADA (con entrega, devolución BUENO/DANADO/PERDIDO), ENTREGADA,
--   CONFIRMADA (pagada), ACEPTADA (sin pagar), PENDIENTE, RECHAZADA, VENCIDA y CANCELADA (una con pago reembolsado).
--   Fechas relativas al día en que se crea la base (CURDATE() ± n días), para que la demostración sea coherente
--   sin importar cuándo se ejecute. Las reservas PENDIENTE y ACEPTADA vencen 1 hora después (plazo máximo):
--   para demostrar aceptar/rechazar/pagar, cree una reserva nueva desde la aplicación.

-- 1. Proveedores y clientes nuevos
INSERT INTO usuarios (nombre, email, password, activo) VALUES
('Hernán Ferretería', 'hernan.ferreteria@gmail.com', '$2a$10$gXHCpn8/Hx4Pl0AQVn5/ceP/HwOE0dcQH/CcdDuEYNgd2/xp5tknG', TRUE),
('Gloria Alquiequipos', 'gloria.alquiequipos@gmail.com', '$2a$10$gXHCpn8/Hx4Pl0AQVn5/ceP/HwOE0dcQH/CcdDuEYNgd2/xp5tknG', TRUE),
('Fabián Maquinarias', 'fabian.maquinarias@gmail.com', '$2a$10$gXHCpn8/Hx4Pl0AQVn5/ceP/HwOE0dcQH/CcdDuEYNgd2/xp5tknG', TRUE),
('Rosa Construequipos', 'rosa.construequipos@gmail.com', '$2a$10$gXHCpn8/Hx4Pl0AQVn5/ceP/HwOE0dcQH/CcdDuEYNgd2/xp5tknG', TRUE),
('Mauricio Andamios', 'mauricio.andamios@gmail.com', '$2a$10$gXHCpn8/Hx4Pl0AQVn5/ceP/HwOE0dcQH/CcdDuEYNgd2/xp5tknG', TRUE),
('Paola Herramientas Pro', 'paola.herrapro@gmail.com', '$2a$10$gXHCpn8/Hx4Pl0AQVn5/ceP/HwOE0dcQH/CcdDuEYNgd2/xp5tknG', TRUE),
('Iván Equipos Pesados', 'ivan.pesados@gmail.com', '$2a$10$gXHCpn8/Hx4Pl0AQVn5/ceP/HwOE0dcQH/CcdDuEYNgd2/xp5tknG', TRUE),
('Liliana Rent', 'liliana.rent@gmail.com', '$2a$10$gXHCpn8/Hx4Pl0AQVn5/ceP/HwOE0dcQH/CcdDuEYNgd2/xp5tknG', TRUE),
('Oscar Soluciones Obra', 'oscar.solucionesobra@gmail.com', '$2a$10$gXHCpn8/Hx4Pl0AQVn5/ceP/HwOE0dcQH/CcdDuEYNgd2/xp5tknG', TRUE),
('Daniela Equipamiento', 'daniela.equipamiento@gmail.com', '$2a$10$gXHCpn8/Hx4Pl0AQVn5/ceP/HwOE0dcQH/CcdDuEYNgd2/xp5tknG', TRUE),
('Felipe Remodelaciones', 'felipe.remodelaciones@gmail.com', '$2a$10$gXHCpn8/Hx4Pl0AQVn5/ceP/HwOE0dcQH/CcdDuEYNgd2/xp5tknG', TRUE),
('Juliana Arquitectura', 'juliana.arquitectura@gmail.com', '$2a$10$gXHCpn8/Hx4Pl0AQVn5/ceP/HwOE0dcQH/CcdDuEYNgd2/xp5tknG', TRUE),
('Tomás Maestro de Obra', 'tomas.maestro@gmail.com', '$2a$10$gXHCpn8/Hx4Pl0AQVn5/ceP/HwOE0dcQH/CcdDuEYNgd2/xp5tknG', TRUE),
('Mariana Interiores', 'mariana.interiores@gmail.com', '$2a$10$gXHCpn8/Hx4Pl0AQVn5/ceP/HwOE0dcQH/CcdDuEYNgd2/xp5tknG', TRUE),
('Andrés Electricista', 'andres.electricista@gmail.com', '$2a$10$gXHCpn8/Hx4Pl0AQVn5/ceP/HwOE0dcQH/CcdDuEYNgd2/xp5tknG', TRUE),
('Catalina Paisajismo', 'catalina.paisajismo@gmail.com', '$2a$10$gXHCpn8/Hx4Pl0AQVn5/ceP/HwOE0dcQH/CcdDuEYNgd2/xp5tknG', TRUE),
('Esteban Plomería', 'esteban.plomeria@gmail.com', '$2a$10$gXHCpn8/Hx4Pl0AQVn5/ceP/HwOE0dcQH/CcdDuEYNgd2/xp5tknG', TRUE),
('Daniel Acabados', 'daniel.acabados@gmail.com', '$2a$10$gXHCpn8/Hx4Pl0AQVn5/ceP/HwOE0dcQH/CcdDuEYNgd2/xp5tknG', TRUE),
('Sara Ingeniería', 'sara.ingenieria@gmail.com', '$2a$10$gXHCpn8/Hx4Pl0AQVn5/ceP/HwOE0dcQH/CcdDuEYNgd2/xp5tknG', TRUE),
('Gabriel Mantenimiento', 'gabriel.mantenimiento@gmail.com', '$2a$10$gXHCpn8/Hx4Pl0AQVn5/ceP/HwOE0dcQH/CcdDuEYNgd2/xp5tknG', FALSE);

INSERT INTO usuario_roles (usuario_id, rol_id)
SELECT u.id, r.id FROM usuarios u
JOIN roles r ON r.nombre = 'PROVEEDOR'
WHERE u.email IN (
    'hernan.ferreteria@gmail.com',
    'gloria.alquiequipos@gmail.com',
    'fabian.maquinarias@gmail.com',
    'rosa.construequipos@gmail.com',
    'mauricio.andamios@gmail.com',
    'paola.herrapro@gmail.com',
    'ivan.pesados@gmail.com',
    'liliana.rent@gmail.com',
    'oscar.solucionesobra@gmail.com',
    'daniela.equipamiento@gmail.com'
);

INSERT INTO usuario_roles (usuario_id, rol_id)
SELECT u.id, r.id FROM usuarios u
JOIN roles r ON r.nombre = 'CLIENTE'
WHERE u.email IN (
    'felipe.remodelaciones@gmail.com',
    'juliana.arquitectura@gmail.com',
    'tomas.maestro@gmail.com',
    'mariana.interiores@gmail.com',
    'andres.electricista@gmail.com',
    'catalina.paisajismo@gmail.com',
    'esteban.plomeria@gmail.com',
    'daniel.acabados@gmail.com',
    'sara.ingenieria@gmail.com',
    'gabriel.mantenimiento@gmail.com'
);

-- 2. Una herramienta por cada proveedor nuevo (una perdida y otra en mantenimiento)
INSERT INTO herramientas (proveedor_id, categoria_id, nombre, descripcion, tarifa_diaria, estado) VALUES
((SELECT id FROM usuarios WHERE email = 'hernan.ferreteria@gmail.com'), (SELECT id FROM categorias WHERE nombre = 'Herramientas Manuales'),
 'Pulidora Angular 7 pulgadas', 'Pulidora de 2200W para corte y desbaste de metal y piedra', 28000.00, 'DISPONIBLE'),
((SELECT id FROM usuarios WHERE email = 'gloria.alquiequipos@gmail.com'), (SELECT id FROM categorias WHERE nombre = 'Elevación y Andamios'),
 'Escalera Telescópica 6m', 'Escalera de aluminio extensible con seguro de bloqueo', 18000.00, 'FUERA_DE_SERVICIO'),
((SELECT id FROM usuarios WHERE email = 'fabian.maquinarias@gmail.com'), (SELECT id FROM categorias WHERE nombre = 'Construcción Pesada'),
 'Minicargador Compacto', 'Minicargador con balde de 0.5 m3 para movimiento de tierra', 450000.00, 'MANTENIMIENTO'),
((SELECT id FROM usuarios WHERE email = 'rosa.construequipos@gmail.com'), (SELECT id FROM categorias WHERE nombre = 'Compactación'),
 'Rodillo Compactador Doble', 'Rodillo vibratorio de 1 tonelada para asfalto y bases', 280000.00, 'DISPONIBLE'),
((SELECT id FROM usuarios WHERE email = 'mauricio.andamios@gmail.com'), (SELECT id FROM categorias WHERE nombre = 'Elevación y Andamios'),
 'Plataforma Elevadora de Tijera', 'Elevador eléctrico de tijera con alcance de 8 metros', 320000.00, 'DISPONIBLE'),
((SELECT id FROM usuarios WHERE email = 'paola.herrapro@gmail.com'), (SELECT id FROM categorias WHERE nombre = 'Corte y Demolición'),
 'Cortadora de Concreto', 'Cortadora de piso con disco diamantado de 14 pulgadas', 85000.00, 'DISPONIBLE'),
((SELECT id FROM usuarios WHERE email = 'ivan.pesados@gmail.com'), (SELECT id FROM categorias WHERE nombre = 'Construcción Pesada'),
 'Retroexcavadora Ligera', 'Retroexcavadora de 3 toneladas para zanjas y cimentaciones', 900000.00, 'DISPONIBLE'),
((SELECT id FROM usuarios WHERE email = 'liliana.rent@gmail.com'), (SELECT id FROM categorias WHERE nombre = 'Seguridad'),
 'Kit de Señalización Vial', 'Conos, vallas y cintas para delimitar zonas de obra', 15000.00, 'DISPONIBLE'),
((SELECT id FROM usuarios WHERE email = 'oscar.solucionesobra@gmail.com'), (SELECT id FROM categorias WHERE nombre = 'Plomería'),
 'Destapadora Eléctrica', 'Máquina de resortes para tubería de 2 a 6 pulgadas', 48000.00, 'DISPONIBLE'),
((SELECT id FROM usuarios WHERE email = 'daniela.equipamiento@gmail.com'), (SELECT id FROM categorias WHERE nombre = 'Anclaje'),
 'Taladro de Columna', 'Taladro de banco con 16 velocidades y mesa inclinable', 33000.00, 'DISPONIBLE');

-- 3. Reservas existentes (1 a 20): estados variados y fechas coherentes
UPDATE reservas SET estado = 'FINALIZADA', fecha_inicio = CURDATE() - INTERVAL 40 DAY, fecha_fin = CURDATE() - INTERVAL 36 DAY,
    created_at = TIMESTAMP(CURDATE() - INTERVAL 43 DAY, '09:00:00'), fecha_aceptacion = TIMESTAMP(CURDATE() - INTERVAL 43 DAY, '09:00:00') + INTERVAL 30 MINUTE,
    fecha_entrega = TIMESTAMP(CURDATE() - INTERVAL 40 DAY, '08:00:00'), fecha_devolucion = TIMESTAMP(CURDATE() - INTERVAL 36 DAY, '17:00:00'),
    estado_devolucion = 'BUENO', observaciones_devolucion = NULL
WHERE id = 1;
UPDATE reservas SET estado = 'FINALIZADA', fecha_inicio = CURDATE() - INTERVAL 30 DAY, fecha_fin = CURDATE() - INTERVAL 27 DAY,
    created_at = TIMESTAMP(CURDATE() - INTERVAL 33 DAY, '09:00:00'), fecha_aceptacion = TIMESTAMP(CURDATE() - INTERVAL 33 DAY, '09:00:00') + INTERVAL 30 MINUTE,
    fecha_entrega = TIMESTAMP(CURDATE() - INTERVAL 30 DAY, '08:00:00'), fecha_devolucion = TIMESTAMP(CURDATE() - INTERVAL 27 DAY, '17:00:00'),
    estado_devolucion = 'BUENO', observaciones_devolucion = NULL
WHERE id = 2;
UPDATE reservas SET estado = 'FINALIZADA', fecha_inicio = CURDATE() - INTERVAL 25 DAY, fecha_fin = CURDATE() - INTERVAL 23 DAY,
    created_at = TIMESTAMP(CURDATE() - INTERVAL 28 DAY, '09:00:00'), fecha_aceptacion = TIMESTAMP(CURDATE() - INTERVAL 28 DAY, '09:00:00') + INTERVAL 30 MINUTE,
    fecha_entrega = TIMESTAMP(CURDATE() - INTERVAL 25 DAY, '08:00:00'), fecha_devolucion = TIMESTAMP(CURDATE() - INTERVAL 23 DAY, '17:00:00'),
    estado_devolucion = 'BUENO', observaciones_devolucion = NULL
WHERE id = 3;
UPDATE reservas SET estado = 'FINALIZADA', fecha_inicio = CURDATE() - INTERVAL 20 DAY, fecha_fin = CURDATE() - INTERVAL 18 DAY,
    created_at = TIMESTAMP(CURDATE() - INTERVAL 23 DAY, '09:00:00'), fecha_aceptacion = TIMESTAMP(CURDATE() - INTERVAL 23 DAY, '09:00:00') + INTERVAL 30 MINUTE,
    fecha_entrega = TIMESTAMP(CURDATE() - INTERVAL 20 DAY, '08:00:00'), fecha_devolucion = TIMESTAMP(CURDATE() - INTERVAL 18 DAY, '17:00:00'),
    estado_devolucion = 'DANADO', observaciones_devolucion = 'La placa compactadora llegó con la base fisurada'
WHERE id = 4;
UPDATE reservas SET estado = 'CONFIRMADA', fecha_inicio = CURDATE() + INTERVAL 5 DAY, fecha_fin = CURDATE() + INTERVAL 8 DAY,
    created_at = TIMESTAMP(CURDATE() - INTERVAL 2 DAY, '09:00:00'), fecha_aceptacion = TIMESTAMP(CURDATE() - INTERVAL 2 DAY, '09:00:00') + INTERVAL 30 MINUTE,
    fecha_entrega = NULL, fecha_devolucion = NULL,
    estado_devolucion = NULL, observaciones_devolucion = NULL
WHERE id = 5;
UPDATE reservas SET estado = 'PENDIENTE', fecha_inicio = CURDATE() + INTERVAL 10 DAY, fecha_fin = CURDATE() + INTERVAL 12 DAY,
    created_at = UTC_TIMESTAMP(), fecha_aceptacion = NULL,
    fecha_entrega = NULL, fecha_devolucion = NULL,
    estado_devolucion = NULL, observaciones_devolucion = NULL
WHERE id = 6;
UPDATE reservas SET estado = 'ENTREGADA', fecha_inicio = CURDATE() - INTERVAL 2 DAY, fecha_fin = CURDATE() + INTERVAL 3 DAY,
    created_at = TIMESTAMP(CURDATE() - INTERVAL 5 DAY, '09:00:00'), fecha_aceptacion = TIMESTAMP(CURDATE() - INTERVAL 5 DAY, '09:00:00') + INTERVAL 30 MINUTE,
    fecha_entrega = TIMESTAMP(CURDATE() - INTERVAL 2 DAY, '08:00:00'), fecha_devolucion = NULL,
    estado_devolucion = NULL, observaciones_devolucion = NULL
WHERE id = 7;
UPDATE reservas SET estado = 'RECHAZADA', fecha_inicio = CURDATE() + INTERVAL 6 DAY, fecha_fin = CURDATE() + INTERVAL 8 DAY,
    created_at = TIMESTAMP(CURDATE() - INTERVAL 2 DAY, '09:00:00'), fecha_aceptacion = NULL,
    fecha_entrega = NULL, fecha_devolucion = NULL,
    estado_devolucion = NULL, observaciones_devolucion = NULL
WHERE id = 8;
UPDATE reservas SET estado = 'FINALIZADA', fecha_inicio = CURDATE() - INTERVAL 15 DAY, fecha_fin = CURDATE() - INTERVAL 13 DAY,
    created_at = TIMESTAMP(CURDATE() - INTERVAL 18 DAY, '09:00:00'), fecha_aceptacion = TIMESTAMP(CURDATE() - INTERVAL 18 DAY, '09:00:00') + INTERVAL 30 MINUTE,
    fecha_entrega = TIMESTAMP(CURDATE() - INTERVAL 15 DAY, '08:00:00'), fecha_devolucion = TIMESTAMP(CURDATE() - INTERVAL 13 DAY, '17:00:00'),
    estado_devolucion = 'BUENO', observaciones_devolucion = NULL
WHERE id = 9;
UPDATE reservas SET estado = 'PENDIENTE', fecha_inicio = CURDATE() + INTERVAL 7 DAY, fecha_fin = CURDATE() + INTERVAL 10 DAY,
    created_at = UTC_TIMESTAMP(), fecha_aceptacion = NULL,
    fecha_entrega = NULL, fecha_devolucion = NULL,
    estado_devolucion = NULL, observaciones_devolucion = NULL
WHERE id = 10;
UPDATE reservas SET estado = 'CONFIRMADA', fecha_inicio = CURDATE() + INTERVAL 3 DAY, fecha_fin = CURDATE() + INTERVAL 5 DAY,
    created_at = TIMESTAMP(CURDATE() - INTERVAL 2 DAY, '09:00:00'), fecha_aceptacion = TIMESTAMP(CURDATE() - INTERVAL 2 DAY, '09:00:00') + INTERVAL 30 MINUTE,
    fecha_entrega = NULL, fecha_devolucion = NULL,
    estado_devolucion = NULL, observaciones_devolucion = NULL
WHERE id = 11;
UPDATE reservas SET estado = 'VENCIDA', fecha_inicio = CURDATE() - INTERVAL 3 DAY, fecha_fin = CURDATE() - INTERVAL 1 DAY,
    created_at = TIMESTAMP(CURDATE() - INTERVAL 6 DAY, '09:00:00'), fecha_aceptacion = NULL,
    fecha_entrega = NULL, fecha_devolucion = NULL,
    estado_devolucion = NULL, observaciones_devolucion = NULL
WHERE id = 12;
UPDATE reservas SET estado = 'FINALIZADA', fecha_inicio = CURDATE() - INTERVAL 12 DAY, fecha_fin = CURDATE() - INTERVAL 10 DAY,
    created_at = TIMESTAMP(CURDATE() - INTERVAL 15 DAY, '09:00:00'), fecha_aceptacion = TIMESTAMP(CURDATE() - INTERVAL 15 DAY, '09:00:00') + INTERVAL 30 MINUTE,
    fecha_entrega = TIMESTAMP(CURDATE() - INTERVAL 12 DAY, '08:00:00'), fecha_devolucion = TIMESTAMP(CURDATE() - INTERVAL 10 DAY, '17:00:00'),
    estado_devolucion = 'BUENO', observaciones_devolucion = NULL
WHERE id = 13;
UPDATE reservas SET estado = 'FINALIZADA', fecha_inicio = CURDATE() - INTERVAL 9 DAY, fecha_fin = CURDATE() - INTERVAL 8 DAY,
    created_at = TIMESTAMP(CURDATE() - INTERVAL 12 DAY, '09:00:00'), fecha_aceptacion = TIMESTAMP(CURDATE() - INTERVAL 12 DAY, '09:00:00') + INTERVAL 30 MINUTE,
    fecha_entrega = TIMESTAMP(CURDATE() - INTERVAL 9 DAY, '08:00:00'), fecha_devolucion = TIMESTAMP(CURDATE() - INTERVAL 8 DAY, '17:00:00'),
    estado_devolucion = 'DANADO', observaciones_devolucion = 'Un panel LED de la torre no enciende'
WHERE id = 14;
UPDATE reservas SET estado = 'ENTREGADA', fecha_inicio = CURDATE() - INTERVAL 1 DAY, fecha_fin = CURDATE() + INTERVAL 2 DAY,
    created_at = TIMESTAMP(CURDATE() - INTERVAL 4 DAY, '09:00:00'), fecha_aceptacion = TIMESTAMP(CURDATE() - INTERVAL 4 DAY, '09:00:00') + INTERVAL 30 MINUTE,
    fecha_entrega = TIMESTAMP(CURDATE() - INTERVAL 1 DAY, '08:00:00'), fecha_devolucion = NULL,
    estado_devolucion = NULL, observaciones_devolucion = NULL
WHERE id = 15;
UPDATE reservas SET estado = 'CANCELADA', fecha_inicio = CURDATE() + INTERVAL 8 DAY, fecha_fin = CURDATE() + INTERVAL 9 DAY,
    created_at = TIMESTAMP(CURDATE() - INTERVAL 2 DAY, '09:00:00'), fecha_aceptacion = NULL,
    fecha_entrega = NULL, fecha_devolucion = NULL,
    estado_devolucion = NULL, observaciones_devolucion = NULL
WHERE id = 16;
UPDATE reservas SET estado = 'FINALIZADA', fecha_inicio = CURDATE() - INTERVAL 35 DAY, fecha_fin = CURDATE() - INTERVAL 31 DAY,
    created_at = TIMESTAMP(CURDATE() - INTERVAL 38 DAY, '09:00:00'), fecha_aceptacion = TIMESTAMP(CURDATE() - INTERVAL 38 DAY, '09:00:00') + INTERVAL 30 MINUTE,
    fecha_entrega = TIMESTAMP(CURDATE() - INTERVAL 35 DAY, '08:00:00'), fecha_devolucion = TIMESTAMP(CURDATE() - INTERVAL 31 DAY, '17:00:00'),
    estado_devolucion = 'BUENO', observaciones_devolucion = NULL
WHERE id = 17;
UPDATE reservas SET estado = 'CONFIRMADA', fecha_inicio = CURDATE() + INTERVAL 2 DAY, fecha_fin = CURDATE() + INTERVAL 4 DAY,
    created_at = TIMESTAMP(CURDATE() - INTERVAL 2 DAY, '09:00:00'), fecha_aceptacion = TIMESTAMP(CURDATE() - INTERVAL 2 DAY, '09:00:00') + INTERVAL 30 MINUTE,
    fecha_entrega = NULL, fecha_devolucion = NULL,
    estado_devolucion = NULL, observaciones_devolucion = NULL
WHERE id = 18;
UPDATE reservas SET estado = 'ACEPTADA', fecha_inicio = CURDATE() + INTERVAL 4 DAY, fecha_fin = CURDATE() + INTERVAL 6 DAY,
    created_at = UTC_TIMESTAMP() - INTERVAL 20 MINUTE, fecha_aceptacion = UTC_TIMESTAMP() - INTERVAL 5 MINUTE,
    fecha_entrega = NULL, fecha_devolucion = NULL,
    estado_devolucion = NULL, observaciones_devolucion = NULL
WHERE id = 19;
UPDATE reservas SET estado = 'CANCELADA', fecha_inicio = CURDATE() + INTERVAL 12 DAY, fecha_fin = CURDATE() + INTERVAL 14 DAY,
    created_at = TIMESTAMP(CURDATE() - INTERVAL 2 DAY, '09:00:00'), fecha_aceptacion = TIMESTAMP(CURDATE() - INTERVAL 2 DAY, '09:00:00') + INTERVAL 30 MINUTE,
    fecha_entrega = NULL, fecha_devolucion = NULL,
    estado_devolucion = NULL, observaciones_devolucion = NULL
WHERE id = 20;

-- 4. Reservas nuevas (se guardan sus ids en variables para ligar pagos y mantenimientos)
INSERT INTO reservas (cliente_id, herramienta_id, fecha_inicio, fecha_fin, estado, created_at,
    fecha_aceptacion, fecha_entrega, fecha_devolucion, estado_devolucion, observaciones_devolucion)
VALUES ((SELECT id FROM usuarios WHERE email = 'felipe.remodelaciones@gmail.com'), (SELECT id FROM herramientas WHERE nombre = 'Pulidora Angular 7 pulgadas'),
    CURDATE() - INTERVAL 28 DAY, CURDATE() - INTERVAL 26 DAY, 'FINALIZADA', TIMESTAMP(CURDATE() - INTERVAL 31 DAY, '09:00:00'),
    TIMESTAMP(CURDATE() - INTERVAL 31 DAY, '09:00:00') + INTERVAL 30 MINUTE, TIMESTAMP(CURDATE() - INTERVAL 28 DAY, '08:00:00'), TIMESTAMP(CURDATE() - INTERVAL 26 DAY, '17:00:00'), 'BUENO', NULL);
SET @reserva_21 = LAST_INSERT_ID();
INSERT INTO reservas (cliente_id, herramienta_id, fecha_inicio, fecha_fin, estado, created_at,
    fecha_aceptacion, fecha_entrega, fecha_devolucion, estado_devolucion, observaciones_devolucion)
VALUES ((SELECT id FROM usuarios WHERE email = 'juliana.arquitectura@gmail.com'), (SELECT id FROM herramientas WHERE nombre = 'Escalera Telescópica 6m'),
    CURDATE() - INTERVAL 22 DAY, CURDATE() - INTERVAL 21 DAY, 'FINALIZADA', TIMESTAMP(CURDATE() - INTERVAL 25 DAY, '09:00:00'),
    TIMESTAMP(CURDATE() - INTERVAL 25 DAY, '09:00:00') + INTERVAL 30 MINUTE, TIMESTAMP(CURDATE() - INTERVAL 22 DAY, '08:00:00'), TIMESTAMP(CURDATE() - INTERVAL 21 DAY, '17:00:00'), 'PERDIDO', 'El cliente reporta que la escalera se perdió en la obra');
SET @reserva_22 = LAST_INSERT_ID();
INSERT INTO reservas (cliente_id, herramienta_id, fecha_inicio, fecha_fin, estado, created_at,
    fecha_aceptacion, fecha_entrega, fecha_devolucion, estado_devolucion, observaciones_devolucion)
VALUES ((SELECT id FROM usuarios WHERE email = 'tomas.maestro@gmail.com'), (SELECT id FROM herramientas WHERE nombre = 'Minicargador Compacto'),
    CURDATE() - INTERVAL 6 DAY, CURDATE() - INTERVAL 4 DAY, 'FINALIZADA', TIMESTAMP(CURDATE() - INTERVAL 9 DAY, '09:00:00'),
    TIMESTAMP(CURDATE() - INTERVAL 9 DAY, '09:00:00') + INTERVAL 30 MINUTE, TIMESTAMP(CURDATE() - INTERVAL 6 DAY, '08:00:00'), TIMESTAMP(CURDATE() - INTERVAL 4 DAY, '17:00:00'), 'DANADO', 'Fuga hidráulica en el brazo del balde');
SET @reserva_23 = LAST_INSERT_ID();
INSERT INTO reservas (cliente_id, herramienta_id, fecha_inicio, fecha_fin, estado, created_at,
    fecha_aceptacion, fecha_entrega, fecha_devolucion, estado_devolucion, observaciones_devolucion)
VALUES ((SELECT id FROM usuarios WHERE email = 'mariana.interiores@gmail.com'), 1,
    CURDATE() - INTERVAL 10 DAY, CURDATE() - INTERVAL 7 DAY, 'FINALIZADA', TIMESTAMP(CURDATE() - INTERVAL 13 DAY, '09:00:00'),
    TIMESTAMP(CURDATE() - INTERVAL 13 DAY, '09:00:00') + INTERVAL 30 MINUTE, TIMESTAMP(CURDATE() - INTERVAL 10 DAY, '08:00:00'), TIMESTAMP(CURDATE() - INTERVAL 7 DAY, '17:00:00'), 'BUENO', NULL);
SET @reserva_24 = LAST_INSERT_ID();
INSERT INTO reservas (cliente_id, herramienta_id, fecha_inicio, fecha_fin, estado, created_at,
    fecha_aceptacion, fecha_entrega, fecha_devolucion, estado_devolucion, observaciones_devolucion)
VALUES ((SELECT id FROM usuarios WHERE email = 'andres.electricista@gmail.com'), (SELECT id FROM herramientas WHERE nombre = 'Rodillo Compactador Doble'),
    CURDATE() + INTERVAL 9 DAY, CURDATE() + INTERVAL 11 DAY, 'CONFIRMADA', TIMESTAMP(CURDATE() - INTERVAL 2 DAY, '09:00:00'),
    TIMESTAMP(CURDATE() - INTERVAL 2 DAY, '09:00:00') + INTERVAL 30 MINUTE, NULL, NULL, NULL, NULL);
SET @reserva_25 = LAST_INSERT_ID();
INSERT INTO reservas (cliente_id, herramienta_id, fecha_inicio, fecha_fin, estado, created_at,
    fecha_aceptacion, fecha_entrega, fecha_devolucion, estado_devolucion, observaciones_devolucion)
VALUES ((SELECT id FROM usuarios WHERE email = 'catalina.paisajismo@gmail.com'), (SELECT id FROM herramientas WHERE nombre = 'Plataforma Elevadora de Tijera'),
    CURDATE() - INTERVAL 3 DAY, CURDATE() + INTERVAL 1 DAY, 'ENTREGADA', TIMESTAMP(CURDATE() - INTERVAL 6 DAY, '09:00:00'),
    TIMESTAMP(CURDATE() - INTERVAL 6 DAY, '09:00:00') + INTERVAL 30 MINUTE, TIMESTAMP(CURDATE() - INTERVAL 3 DAY, '08:00:00'), NULL, NULL, NULL);
SET @reserva_26 = LAST_INSERT_ID();
INSERT INTO reservas (cliente_id, herramienta_id, fecha_inicio, fecha_fin, estado, created_at,
    fecha_aceptacion, fecha_entrega, fecha_devolucion, estado_devolucion, observaciones_devolucion)
VALUES ((SELECT id FROM usuarios WHERE email = 'esteban.plomeria@gmail.com'), (SELECT id FROM herramientas WHERE nombre = 'Cortadora de Concreto'),
    CURDATE() + INTERVAL 14 DAY, CURDATE() + INTERVAL 16 DAY, 'PENDIENTE', UTC_TIMESTAMP(),
    NULL, NULL, NULL, NULL, NULL);
SET @reserva_27 = LAST_INSERT_ID();
INSERT INTO reservas (cliente_id, herramienta_id, fecha_inicio, fecha_fin, estado, created_at,
    fecha_aceptacion, fecha_entrega, fecha_devolucion, estado_devolucion, observaciones_devolucion)
VALUES ((SELECT id FROM usuarios WHERE email = 'daniel.acabados@gmail.com'), (SELECT id FROM herramientas WHERE nombre = 'Retroexcavadora Ligera'),
    CURDATE() + INTERVAL 5 DAY, CURDATE() + INTERVAL 6 DAY, 'RECHAZADA', TIMESTAMP(CURDATE() - INTERVAL 2 DAY, '09:00:00'),
    NULL, NULL, NULL, NULL, NULL);
SET @reserva_28 = LAST_INSERT_ID();
INSERT INTO reservas (cliente_id, herramienta_id, fecha_inicio, fecha_fin, estado, created_at,
    fecha_aceptacion, fecha_entrega, fecha_devolucion, estado_devolucion, observaciones_devolucion)
VALUES ((SELECT id FROM usuarios WHERE email = 'sara.ingenieria@gmail.com'), (SELECT id FROM herramientas WHERE nombre = 'Kit de Señalización Vial'),
    CURDATE() - INTERVAL 8 DAY, CURDATE() - INTERVAL 6 DAY, 'VENCIDA', TIMESTAMP(CURDATE() - INTERVAL 11 DAY, '09:00:00'),
    NULL, NULL, NULL, NULL, NULL);
SET @reserva_29 = LAST_INSERT_ID();
INSERT INTO reservas (cliente_id, herramienta_id, fecha_inicio, fecha_fin, estado, created_at,
    fecha_aceptacion, fecha_entrega, fecha_devolucion, estado_devolucion, observaciones_devolucion)
VALUES ((SELECT id FROM usuarios WHERE email = 'ana.rentas@gmail.com'), (SELECT id FROM herramientas WHERE nombre = 'Destapadora Eléctrica'),
    CURDATE() + INTERVAL 6 DAY, CURDATE() + INTERVAL 7 DAY, 'ACEPTADA', UTC_TIMESTAMP() - INTERVAL 20 MINUTE,
    UTC_TIMESTAMP() - INTERVAL 5 MINUTE, NULL, NULL, NULL, NULL);
SET @reserva_30 = LAST_INSERT_ID();
INSERT INTO reservas (cliente_id, herramienta_id, fecha_inicio, fecha_fin, estado, created_at,
    fecha_aceptacion, fecha_entrega, fecha_devolucion, estado_devolucion, observaciones_devolucion)
VALUES ((SELECT id FROM usuarios WHERE email = 'juan.cliente@gmail.com'), 3,
    CURDATE() - INTERVAL 50 DAY, CURDATE() - INTERVAL 48 DAY, 'FINALIZADA', TIMESTAMP(CURDATE() - INTERVAL 53 DAY, '09:00:00'),
    TIMESTAMP(CURDATE() - INTERVAL 53 DAY, '09:00:00') + INTERVAL 30 MINUTE, TIMESTAMP(CURDATE() - INTERVAL 50 DAY, '08:00:00'), TIMESTAMP(CURDATE() - INTERVAL 48 DAY, '17:00:00'), 'BUENO', NULL);
SET @reserva_31 = LAST_INSERT_ID();
INSERT INTO reservas (cliente_id, herramienta_id, fecha_inicio, fecha_fin, estado, created_at,
    fecha_aceptacion, fecha_entrega, fecha_devolucion, estado_devolucion, observaciones_devolucion)
VALUES ((SELECT id FROM usuarios WHERE email = 'juan.cliente@gmail.com'), 9,
    CURDATE() - INTERVAL 45 DAY, CURDATE() - INTERVAL 44 DAY, 'FINALIZADA', TIMESTAMP(CURDATE() - INTERVAL 48 DAY, '09:00:00'),
    TIMESTAMP(CURDATE() - INTERVAL 48 DAY, '09:00:00') + INTERVAL 30 MINUTE, TIMESTAMP(CURDATE() - INTERVAL 45 DAY, '08:00:00'), TIMESTAMP(CURDATE() - INTERVAL 44 DAY, '17:00:00'), 'BUENO', NULL);
SET @reserva_32 = LAST_INSERT_ID();
INSERT INTO reservas (cliente_id, herramienta_id, fecha_inicio, fecha_fin, estado, created_at,
    fecha_aceptacion, fecha_entrega, fecha_devolucion, estado_devolucion, observaciones_devolucion)
VALUES ((SELECT id FROM usuarios WHERE email = 'ana.rentas@gmail.com'), 15,
    CURDATE() - INTERVAL 42 DAY, CURDATE() - INTERVAL 40 DAY, 'FINALIZADA', TIMESTAMP(CURDATE() - INTERVAL 45 DAY, '09:00:00'),
    TIMESTAMP(CURDATE() - INTERVAL 45 DAY, '09:00:00') + INTERVAL 30 MINUTE, TIMESTAMP(CURDATE() - INTERVAL 42 DAY, '08:00:00'), TIMESTAMP(CURDATE() - INTERVAL 40 DAY, '17:00:00'), 'BUENO', NULL);
SET @reserva_33 = LAST_INSERT_ID();
INSERT INTO reservas (cliente_id, herramienta_id, fecha_inicio, fecha_fin, estado, created_at,
    fecha_aceptacion, fecha_entrega, fecha_devolucion, estado_devolucion, observaciones_devolucion)
VALUES ((SELECT id FROM usuarios WHERE email = 'pedro.obrero@gmail.com'), 7,
    CURDATE() - INTERVAL 38 DAY, CURDATE() - INTERVAL 37 DAY, 'FINALIZADA', TIMESTAMP(CURDATE() - INTERVAL 41 DAY, '09:00:00'),
    TIMESTAMP(CURDATE() - INTERVAL 41 DAY, '09:00:00') + INTERVAL 30 MINUTE, TIMESTAMP(CURDATE() - INTERVAL 38 DAY, '08:00:00'), TIMESTAMP(CURDATE() - INTERVAL 37 DAY, '17:00:00'), 'BUENO', NULL);
SET @reserva_34 = LAST_INSERT_ID();

-- 5. Pagos 1 a 20 reasignados a las reservas pagadas: monto = tarifa diaria × días
UPDATE pagos SET reserva_id = 1, monto = 225000.00, metodo_pago = 'TARJETA',
    estado_pago = 'COMPLETADO', fecha_pago = TIMESTAMP(CURDATE() - INTERVAL 43 DAY, '09:00:00') + INTERVAL 50 MINUTE
WHERE id = 1;
UPDATE pagos SET reserva_id = 2, monto = 480000.00, metodo_pago = 'TRANSFERENCIA',
    estado_pago = 'COMPLETADO', fecha_pago = TIMESTAMP(CURDATE() - INTERVAL 33 DAY, '09:00:00') + INTERVAL 50 MINUTE
WHERE id = 2;
UPDATE pagos SET reserva_id = 3, monto = 75000.00, metodo_pago = 'TRANSFERENCIA',
    estado_pago = 'COMPLETADO', fecha_pago = TIMESTAMP(CURDATE() - INTERVAL 28 DAY, '09:00:00') + INTERVAL 50 MINUTE
WHERE id = 3;
UPDATE pagos SET reserva_id = 4, monto = 285000.00, metodo_pago = 'TARJETA',
    estado_pago = 'COMPLETADO', fecha_pago = TIMESTAMP(CURDATE() - INTERVAL 23 DAY, '09:00:00') + INTERVAL 50 MINUTE
WHERE id = 4;
UPDATE pagos SET reserva_id = 5, monto = 220000.00, metodo_pago = 'EFECTIVO',
    estado_pago = 'COMPLETADO', fecha_pago = TIMESTAMP(CURDATE() - INTERVAL 2 DAY, '09:00:00') + INTERVAL 50 MINUTE
WHERE id = 5;
UPDATE pagos SET reserva_id = 7, monto = 900000.00, metodo_pago = 'TARJETA',
    estado_pago = 'COMPLETADO', fecha_pago = TIMESTAMP(CURDATE() - INTERVAL 5 DAY, '09:00:00') + INTERVAL 50 MINUTE
WHERE id = 6;
UPDATE pagos SET reserva_id = 9, monto = 180000.00, metodo_pago = 'TARJETA',
    estado_pago = 'COMPLETADO', fecha_pago = TIMESTAMP(CURDATE() - INTERVAL 18 DAY, '09:00:00') + INTERVAL 50 MINUTE
WHERE id = 7;
UPDATE pagos SET reserva_id = 11, monto = 90000.00, metodo_pago = 'EFECTIVO',
    estado_pago = 'COMPLETADO', fecha_pago = TIMESTAMP(CURDATE() - INTERVAL 2 DAY, '09:00:00') + INTERVAL 50 MINUTE
WHERE id = 8;
UPDATE pagos SET reserva_id = 13, monto = 270000.00, metodo_pago = 'TRANSFERENCIA',
    estado_pago = 'COMPLETADO', fecha_pago = TIMESTAMP(CURDATE() - INTERVAL 15 DAY, '09:00:00') + INTERVAL 50 MINUTE
WHERE id = 9;
UPDATE pagos SET reserva_id = 14, monto = 220000.00, metodo_pago = 'TARJETA',
    estado_pago = 'COMPLETADO', fecha_pago = TIMESTAMP(CURDATE() - INTERVAL 12 DAY, '09:00:00') + INTERVAL 50 MINUTE
WHERE id = 10;
UPDATE pagos SET reserva_id = 15, monto = 168000.00, metodo_pago = 'TARJETA',
    estado_pago = 'COMPLETADO', fecha_pago = TIMESTAMP(CURDATE() - INTERVAL 4 DAY, '09:00:00') + INTERVAL 50 MINUTE
WHERE id = 11;
UPDATE pagos SET reserva_id = 17, monto = 250000.00, metodo_pago = 'TRANSFERENCIA',
    estado_pago = 'COMPLETADO', fecha_pago = TIMESTAMP(CURDATE() - INTERVAL 38 DAY, '09:00:00') + INTERVAL 50 MINUTE
WHERE id = 12;
UPDATE pagos SET reserva_id = 18, monto = 114000.00, metodo_pago = 'EFECTIVO',
    estado_pago = 'COMPLETADO', fecha_pago = TIMESTAMP(CURDATE() - INTERVAL 2 DAY, '09:00:00') + INTERVAL 50 MINUTE
WHERE id = 13;
UPDATE pagos SET reserva_id = 20, monto = 66000.00, metodo_pago = 'TARJETA',
    estado_pago = 'REEMBOLSADO', fecha_pago = TIMESTAMP(CURDATE() - INTERVAL 2 DAY, '09:00:00') + INTERVAL 50 MINUTE
WHERE id = 14;
UPDATE pagos SET reserva_id = @reserva_21, monto = 84000.00, metodo_pago = 'TRANSFERENCIA',
    estado_pago = 'COMPLETADO', fecha_pago = TIMESTAMP(CURDATE() - INTERVAL 31 DAY, '09:00:00') + INTERVAL 50 MINUTE
WHERE id = 15;
UPDATE pagos SET reserva_id = @reserva_22, monto = 36000.00, metodo_pago = 'EFECTIVO',
    estado_pago = 'COMPLETADO', fecha_pago = TIMESTAMP(CURDATE() - INTERVAL 25 DAY, '09:00:00') + INTERVAL 50 MINUTE
WHERE id = 16;
UPDATE pagos SET reserva_id = @reserva_23, monto = 1350000.00, metodo_pago = 'TARJETA',
    estado_pago = 'COMPLETADO', fecha_pago = TIMESTAMP(CURDATE() - INTERVAL 9 DAY, '09:00:00') + INTERVAL 50 MINUTE
WHERE id = 17;
UPDATE pagos SET reserva_id = @reserva_24, monto = 180000.00, metodo_pago = 'EFECTIVO',
    estado_pago = 'COMPLETADO', fecha_pago = TIMESTAMP(CURDATE() - INTERVAL 13 DAY, '09:00:00') + INTERVAL 50 MINUTE
WHERE id = 18;
UPDATE pagos SET reserva_id = @reserva_25, monto = 840000.00, metodo_pago = 'TRANSFERENCIA',
    estado_pago = 'COMPLETADO', fecha_pago = TIMESTAMP(CURDATE() - INTERVAL 2 DAY, '09:00:00') + INTERVAL 50 MINUTE
WHERE id = 19;
UPDATE pagos SET reserva_id = @reserva_26, monto = 1600000.00, metodo_pago = 'TARJETA',
    estado_pago = 'COMPLETADO', fecha_pago = TIMESTAMP(CURDATE() - INTERVAL 6 DAY, '09:00:00') + INTERVAL 50 MINUTE
WHERE id = 20;

-- 6. Facturas 1 a 20 regeneradas desde su pago (la del pago reembolsado queda ANULADA)
UPDATE facturas f
JOIN pagos p ON p.id = f.pago_id
JOIN reservas r ON r.id = p.reserva_id
JOIN usuarios u ON u.id = r.cliente_id
JOIN herramientas h ON h.id = r.herramienta_id
SET f.reserva_id = r.id, f.cliente_id = u.id, f.cliente_nombre = u.nombre, f.cliente_email = u.email,
    f.herramienta_id = h.id, f.herramienta_nombre = h.nombre, f.fecha_inicio = r.fecha_inicio,
    f.fecha_fin = r.fecha_fin, f.dias = DATEDIFF(r.fecha_fin, r.fecha_inicio) + 1,
    f.tarifa_diaria = h.tarifa_diaria, f.subtotal = p.monto, f.total = p.monto,
    f.metodo_pago = p.metodo_pago, f.fecha_emision = p.fecha_pago,
    f.estado = IF(p.estado_pago = 'REEMBOLSADO', 'ANULADA', 'VIGENTE')
WHERE f.pago_id BETWEEN 1 AND 20;

-- 7. Pagos y facturas de los alquileres anteriores de clientes frecuentes
INSERT INTO pagos (reserva_id, monto, metodo_pago, estado_pago, fecha_pago) VALUES
(@reserva_31, 360000.00, 'TARJETA', 'COMPLETADO', TIMESTAMP(CURDATE() - INTERVAL 53 DAY, '09:00:00') + INTERVAL 50 MINUTE),
(@reserva_32, 120000.00, 'EFECTIVO', 'COMPLETADO', TIMESTAMP(CURDATE() - INTERVAL 48 DAY, '09:00:00') + INTERVAL 50 MINUTE),
(@reserva_33, 126000.00, 'TRANSFERENCIA', 'COMPLETADO', TIMESTAMP(CURDATE() - INTERVAL 45 DAY, '09:00:00') + INTERVAL 50 MINUTE),
(@reserva_34, 300000.00, 'TARJETA', 'COMPLETADO', TIMESTAMP(CURDATE() - INTERVAL 41 DAY, '09:00:00') + INTERVAL 50 MINUTE);
-- El INSERT ... SELECT de V6 reservó ids por bloques: se reajusta el contador para que las facturas
-- nuevas sigan la numeración (MySQL lo fija en el id máximo + 1).
ALTER TABLE facturas AUTO_INCREMENT = 1;
INSERT INTO facturas (numero, pago_id, reserva_id, cliente_id, cliente_nombre, cliente_email,
    herramienta_id, herramienta_nombre, fecha_inicio, fecha_fin, dias, tarifa_diaria, subtotal, total,
    metodo_pago, fecha_emision, estado)
SELECT
    CONCAT('FAC-', LPAD(p.id, 6, '0')), p.id, r.id, u.id, u.nombre, u.email, h.id, h.nombre,
    r.fecha_inicio, r.fecha_fin, DATEDIFF(r.fecha_fin, r.fecha_inicio) + 1, h.tarifa_diaria,
    p.monto, p.monto, p.metodo_pago, p.fecha_pago,
    IF(p.estado_pago = 'REEMBOLSADO', 'ANULADA', 'VIGENTE')
FROM pagos p
JOIN reservas r ON r.id = p.reserva_id
JOIN usuarios u ON u.id = r.cliente_id
JOIN herramientas h ON h.id = r.herramienta_id
WHERE p.reserva_id IN (@reserva_31, @reserva_32, @reserva_33, @reserva_34)
ORDER BY p.id;


-- 8. Mantenimientos: los de daño quedan después de la devolución y los preventivos antes de los alquileres
UPDATE mantenimientos SET reserva_id = 4, herramienta_id = 4,
    descripcion_problema = 'Daño reportado en devolución de reserva 4: La placa compactadora llegó con la base fisurada', costo_reparacion = 80000.00,
    fecha_inicio = TIMESTAMP(CURDATE() - INTERVAL 18 DAY, '17:00:00') + INTERVAL 1 HOUR, fecha_fin = TIMESTAMP(CURDATE() - INTERVAL 18 DAY, '17:00:00') + INTERVAL 1 DAY
WHERE id = 1;
UPDATE mantenimientos SET reserva_id = 14, herramienta_id = 14,
    descripcion_problema = 'Daño reportado en devolución de reserva 14: Un panel LED de la torre no enciende', costo_reparacion = 45000.00,
    fecha_inicio = TIMESTAMP(CURDATE() - INTERVAL 8 DAY, '17:00:00') + INTERVAL 1 HOUR, fecha_fin = TIMESTAMP(CURDATE() - INTERVAL 8 DAY, '17:00:00') + INTERVAL 1 DAY
WHERE id = 2;
-- Daño aún en reparación: la herramienta sigue en MANTENIMIENTO.
INSERT INTO mantenimientos (herramienta_id, reserva_id, descripcion_problema, costo_reparacion, fecha_inicio, fecha_fin)
VALUES ((SELECT id FROM herramientas WHERE nombre = 'Minicargador Compacto'), @reserva_23,
    'Daño reportado en devolución de reserva 23: Fuga hidráulica en el brazo del balde', NULL, TIMESTAMP(CURDATE() - INTERVAL 4 DAY, '17:00:00') + INTERVAL 1 HOUR, NULL);
UPDATE mantenimientos SET fecha_inicio = TIMESTAMP(CURDATE() - INTERVAL 84 DAY, '09:00:00'), fecha_fin = TIMESTAMP(CURDATE() - INTERVAL 84 DAY, '09:00:00') + INTERVAL 6 HOUR WHERE id = 3;
UPDATE mantenimientos SET fecha_inicio = TIMESTAMP(CURDATE() - INTERVAL 82 DAY, '09:00:00'), fecha_fin = TIMESTAMP(CURDATE() - INTERVAL 82 DAY, '09:00:00') + INTERVAL 6 HOUR WHERE id = 4;
UPDATE mantenimientos SET fecha_inicio = TIMESTAMP(CURDATE() - INTERVAL 80 DAY, '09:00:00'), fecha_fin = TIMESTAMP(CURDATE() - INTERVAL 80 DAY, '09:00:00') + INTERVAL 6 HOUR WHERE id = 5;
UPDATE mantenimientos SET fecha_inicio = TIMESTAMP(CURDATE() - INTERVAL 78 DAY, '09:00:00'), fecha_fin = TIMESTAMP(CURDATE() - INTERVAL 78 DAY, '09:00:00') + INTERVAL 6 HOUR WHERE id = 6;
UPDATE mantenimientos SET fecha_inicio = TIMESTAMP(CURDATE() - INTERVAL 76 DAY, '09:00:00'), fecha_fin = TIMESTAMP(CURDATE() - INTERVAL 76 DAY, '09:00:00') + INTERVAL 6 HOUR WHERE id = 7;
UPDATE mantenimientos SET fecha_inicio = TIMESTAMP(CURDATE() - INTERVAL 74 DAY, '09:00:00'), fecha_fin = TIMESTAMP(CURDATE() - INTERVAL 74 DAY, '09:00:00') + INTERVAL 6 HOUR WHERE id = 8;
UPDATE mantenimientos SET fecha_inicio = TIMESTAMP(CURDATE() - INTERVAL 72 DAY, '09:00:00'), fecha_fin = TIMESTAMP(CURDATE() - INTERVAL 72 DAY, '09:00:00') + INTERVAL 6 HOUR WHERE id = 9;
UPDATE mantenimientos SET fecha_inicio = TIMESTAMP(CURDATE() - INTERVAL 70 DAY, '09:00:00'), fecha_fin = TIMESTAMP(CURDATE() - INTERVAL 70 DAY, '09:00:00') + INTERVAL 6 HOUR WHERE id = 10;
UPDATE mantenimientos SET fecha_inicio = TIMESTAMP(CURDATE() - INTERVAL 68 DAY, '09:00:00'), fecha_fin = TIMESTAMP(CURDATE() - INTERVAL 68 DAY, '09:00:00') + INTERVAL 6 HOUR WHERE id = 11;
UPDATE mantenimientos SET fecha_inicio = TIMESTAMP(CURDATE() - INTERVAL 66 DAY, '09:00:00'), fecha_fin = TIMESTAMP(CURDATE() - INTERVAL 66 DAY, '09:00:00') + INTERVAL 6 HOUR WHERE id = 12;
UPDATE mantenimientos SET fecha_inicio = TIMESTAMP(CURDATE() - INTERVAL 64 DAY, '09:00:00'), fecha_fin = TIMESTAMP(CURDATE() - INTERVAL 64 DAY, '09:00:00') + INTERVAL 6 HOUR WHERE id = 13;
UPDATE mantenimientos SET fecha_inicio = TIMESTAMP(CURDATE() - INTERVAL 62 DAY, '09:00:00'), fecha_fin = TIMESTAMP(CURDATE() - INTERVAL 62 DAY, '09:00:00') + INTERVAL 6 HOUR WHERE id = 14;
UPDATE mantenimientos SET fecha_inicio = TIMESTAMP(CURDATE() - INTERVAL 60 DAY, '09:00:00'), fecha_fin = TIMESTAMP(CURDATE() - INTERVAL 60 DAY, '09:00:00') + INTERVAL 6 HOUR WHERE id = 15;
UPDATE mantenimientos SET fecha_inicio = TIMESTAMP(CURDATE() - INTERVAL 58 DAY, '09:00:00'), fecha_fin = TIMESTAMP(CURDATE() - INTERVAL 58 DAY, '09:00:00') + INTERVAL 6 HOUR WHERE id = 16;
UPDATE mantenimientos SET fecha_inicio = TIMESTAMP(CURDATE() - INTERVAL 56 DAY, '09:00:00'), fecha_fin = TIMESTAMP(CURDATE() - INTERVAL 56 DAY, '09:00:00') + INTERVAL 6 HOUR WHERE id = 17;
UPDATE mantenimientos SET fecha_inicio = TIMESTAMP(CURDATE() - INTERVAL 54 DAY, '09:00:00'), fecha_fin = TIMESTAMP(CURDATE() - INTERVAL 54 DAY, '09:00:00') + INTERVAL 6 HOUR WHERE id = 18;
UPDATE mantenimientos SET fecha_inicio = TIMESTAMP(CURDATE() - INTERVAL 52 DAY, '09:00:00'), fecha_fin = TIMESTAMP(CURDATE() - INTERVAL 52 DAY, '09:00:00') + INTERVAL 6 HOUR WHERE id = 19;
UPDATE mantenimientos SET fecha_inicio = TIMESTAMP(CURDATE() - INTERVAL 50 DAY, '09:00:00'), fecha_fin = TIMESTAMP(CURDATE() - INTERVAL 50 DAY, '09:00:00') + INTERVAL 6 HOUR WHERE id = 20;
