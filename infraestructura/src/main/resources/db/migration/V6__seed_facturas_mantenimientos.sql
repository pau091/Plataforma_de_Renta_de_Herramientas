INSERT INTO mantenimientos (herramienta_id, reserva_id, descripcion_problema, costo_reparacion, fecha_inicio, fecha_fin) VALUES
(4, 4, 'Daño en placa compactadora reportado en devolución', 80000.00, '2026-11-05 09:00:00', '2026-11-06 16:00:00'),
(14, 14, 'Falla en torre de iluminación LED', 45000.00, '2026-11-15 08:00:00', '2026-11-16 12:00:00'),
(1, NULL, 'Cambio de brocas y revisión de motor', 20000.00, '2026-10-20 10:00:00', '2026-10-20 18:00:00'),
(2, NULL, 'Soldadura de módulo de andamio', 35000.00, '2026-10-21 09:00:00', '2026-10-22 11:00:00'),
(3, NULL, 'Cambio de correa de mezcladora', 28000.00, '2026-10-22 08:30:00', '2026-10-22 17:00:00'),
(5, NULL, 'Afilado de disco de sierra', 15000.00, '2026-10-23 09:00:00', '2026-10-23 13:00:00'),
(6, NULL, 'Revisión de cincel demoledor', 22000.00, '2026-10-24 10:00:00', '2026-10-24 15:00:00'),
(7, NULL, 'Cambio de filtro de planta eléctrica', 18000.00, '2026-10-25 08:00:00', '2026-10-25 12:00:00'),
(8, NULL, 'Fuga de aceite en compresor', 40000.00, '2026-10-26 09:00:00', '2026-10-27 10:00:00'),
(9, NULL, 'Calibración de soldadora', 12000.00, '2026-10-27 11:00:00', '2026-10-27 16:00:00'),
(10, NULL, 'Cambio de batería de nivel láser', 9000.00, '2026-10-28 09:30:00', '2026-10-28 11:00:00'),
(11, NULL, 'Limpieza de pistola HVLP', 8000.00, '2026-10-29 08:00:00', '2026-10-29 10:00:00'),
(12, NULL, 'Cambio de manguera de hidrolavadora', 16000.00, '2026-10-30 09:00:00', '2026-10-30 14:00:00'),
(13, NULL, 'Revisión hidráulica de montacargas', 50000.00, '2026-10-31 08:00:00', '2026-11-01 17:00:00'),
(15, NULL, 'Cambio de broca SDS', 7000.00, '2026-11-01 09:00:00', '2026-11-01 11:00:00'),
(16, NULL, 'Cambio de lija de pulidora', 6000.00, '2026-11-02 10:00:00', '2026-11-02 12:00:00'),
(17, NULL, 'Cambio de flexible de vibrador', 19000.00, '2026-11-03 08:30:00', '2026-11-03 15:00:00'),
(18, NULL, 'Afilado de cuchillas de cortacésped', 11000.00, '2026-11-04 09:00:00', '2026-11-04 13:00:00'),
(19, NULL, 'Calibración de roscadora', 14000.00, '2026-11-05 10:00:00', '2026-11-05 16:00:00'),
(20, NULL, 'Cambio de puntas del kit eléctrico', 5000.00, '2026-11-06 08:00:00', '2026-11-06 09:30:00');

INSERT INTO facturas (
    numero, pago_id, reserva_id, cliente_id, cliente_nombre, cliente_email,
    herramienta_id, herramienta_nombre, fecha_inicio, fecha_fin, dias,
    tarifa_diaria, subtotal, total, metodo_pago, fecha_emision
)
SELECT
    CONCAT('FAC-', LPAD(p.id, 6, '0')),
    p.id,
    r.id,
    u.id,
    u.nombre,
    u.email,
    h.id,
    h.nombre,
    r.fecha_inicio,
    r.fecha_fin,
    DATEDIFF(r.fecha_fin, r.fecha_inicio) + 1,
    h.tarifa_diaria,
    p.monto,
    p.monto,
    p.metodo_pago,
    p.fecha_pago
FROM pagos p
JOIN reservas r ON r.id = p.reserva_id
JOIN usuarios u ON u.id = r.cliente_id
JOIN herramientas h ON h.id = r.herramienta_id
WHERE p.estado_pago = 'COMPLETADO';
