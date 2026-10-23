-- Suspensión (temporal) y baneo (indefinido) de proveedores y clientes.
ALTER TABLE usuarios
    ADD COLUMN suspendido_hasta TIMESTAMP NULL,
    ADD COLUMN baneado          BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN motivo_sancion   VARCHAR(255) NULL;

-- Registro de los tokens JWT emitidos (jti = identificador único de cada token). Permite saber qué tokens
-- vigentes tiene un usuario para revocarlos todos de una vez al suspenderlo o banearlo.
CREATE TABLE tokens_emitidos (
    jti        VARCHAR(36) PRIMARY KEY,
    usuario_id BIGINT NOT NULL,
    emitido_en TIMESTAMP NOT NULL,
    expira_en  TIMESTAMP NOT NULL,
    -- Datos técnicos, no historial: si se elimina el usuario, se eliminan con él.
    CONSTRAINT fk_tokens_emitidos_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE
);
CREATE INDEX idx_tokens_emitidos_usuario ON tokens_emitidos(usuario_id, expira_en);

-- Lista negra: tokens revocados antes de expirar (suspensión, baneo, desactivación o cierre de sesión).
-- El filtro JWT la consulta en cada petición por su llave primaria (jti).
CREATE TABLE tokens_revocados (
    jti         VARCHAR(36) PRIMARY KEY,
    usuario_id  BIGINT NOT NULL,
    motivo      VARCHAR(300) NOT NULL,
    revocado_en TIMESTAMP NOT NULL,
    expira_en   TIMESTAMP NOT NULL,
    CONSTRAINT fk_tokens_revocados_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE
);
CREATE INDEX idx_tokens_revocados_expira ON tokens_revocados(expira_en);
