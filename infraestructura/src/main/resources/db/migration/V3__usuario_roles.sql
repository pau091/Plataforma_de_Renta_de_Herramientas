CREATE TABLE usuario_roles (
    usuario_id BIGINT NOT NULL,
    rol_id BIGINT NOT NULL,
    PRIMARY KEY (usuario_id, rol_id),
    CONSTRAINT fk_usuario_roles_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE,
    CONSTRAINT fk_usuario_roles_rol FOREIGN KEY (rol_id) REFERENCES roles(id) ON DELETE CASCADE
);

CREATE INDEX idx_usuario_roles_rol_id ON usuario_roles(rol_id);

INSERT IGNORE INTO usuario_roles (usuario_id, rol_id)
SELECT u.id, r.id FROM usuarios u
JOIN roles r ON r.nombre = 'ADMIN'
WHERE u.email = 'admin@rentas.com';

INSERT IGNORE INTO usuario_roles (usuario_id, rol_id)
SELECT u.id, r.id FROM usuarios u
JOIN roles r ON r.nombre = 'PROVEEDOR'
WHERE u.email IN (
    'carlos.proveedor@gmail.com',
    'maria.equipos@gmail.com',
    'luis.equipos@gmail.com',
    'sofia.maquinaria@gmail.com',
    'andres.rentals@gmail.com',
    'patricia.obras@gmail.com',
    'diego.constructora@gmail.com',
    'camila.herramientas@gmail.com',
    'jorge.alquileres@gmail.com',
    'elena.maqpro@gmail.com'
);

INSERT IGNORE INTO usuario_roles (usuario_id, rol_id)
SELECT u.id, r.id FROM usuarios u
JOIN roles r ON r.nombre = 'CLIENTE'
WHERE u.email IN (
    'juan.cliente@gmail.com',
    'ana.rentas@gmail.com',
    'pedro.obrero@gmail.com',
    'laura.constructor@gmail.com',
    'miguel.contratista@gmail.com',
    'valentina.obras@gmail.com',
    'ricardo.independiente@gmail.com',
    'natalia.reforma@gmail.com',
    'sebastian.pinturas@gmail.com',
    'carolina.drywall@gmail.com'
);
