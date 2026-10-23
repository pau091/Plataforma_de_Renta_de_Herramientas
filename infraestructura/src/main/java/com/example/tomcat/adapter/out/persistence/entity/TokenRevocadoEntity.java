package com.example.tomcat.adapter.out.persistence.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** Entrada de la lista negra: un token que se revocó antes de expirar. */
@Entity
@Table(name = "tokens_revocados")
public class TokenRevocadoEntity {

    @Id
    @Column(length = 36)
    private String jti;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(nullable = false, length = 300)
    private String motivo;

    /** Instantes en UTC. */
    @Column(name = "revocado_en", nullable = false)
    private LocalDateTime revocadoEn;

    /** Se conserva hasta que el token habría expirado; después la limpieza periódica lo borra. */
    @Column(name = "expira_en", nullable = false)
    private LocalDateTime expiraEn;

    protected TokenRevocadoEntity() {
    }

    public TokenRevocadoEntity(String jti, Long usuarioId, String motivo, LocalDateTime revocadoEn, LocalDateTime expiraEn) {
        this.jti = jti;
        this.usuarioId = usuarioId;
        this.motivo = motivo;
        this.revocadoEn = revocadoEn;
        this.expiraEn = expiraEn;
    }

    public String getJti() { return jti; }
    public Long getUsuarioId() { return usuarioId; }
    public String getMotivo() { return motivo; }
    public LocalDateTime getRevocadoEn() { return revocadoEn; }
    public LocalDateTime getExpiraEn() { return expiraEn; }
}
