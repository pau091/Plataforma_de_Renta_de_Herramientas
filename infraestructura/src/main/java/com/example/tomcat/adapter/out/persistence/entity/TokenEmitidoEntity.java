package com.example.tomcat.adapter.out.persistence.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** Token JWT emitido al iniciar sesión o registrarse. Sirve para saber qué tokens vigentes revocar de un usuario. */
@Entity
@Table(name = "tokens_emitidos")
public class TokenEmitidoEntity {

    @Id
    @Column(length = 36)
    private String jti;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    /** Instantes en UTC. */
    @Column(name = "emitido_en", nullable = false)
    private LocalDateTime emitidoEn;

    @Column(name = "expira_en", nullable = false)
    private LocalDateTime expiraEn;

    protected TokenEmitidoEntity() {
    }

    public TokenEmitidoEntity(String jti, Long usuarioId, LocalDateTime emitidoEn, LocalDateTime expiraEn) {
        this.jti = jti;
        this.usuarioId = usuarioId;
        this.emitidoEn = emitidoEn;
        this.expiraEn = expiraEn;
    }

    public String getJti() { return jti; }
    public Long getUsuarioId() { return usuarioId; }
    public LocalDateTime getEmitidoEn() { return emitidoEn; }
    public LocalDateTime getExpiraEn() { return expiraEn; }
}
