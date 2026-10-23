package com.example.tomcat.security;

import com.example.tomcat.adapter.out.persistence.entity.TokenEmitidoEntity;
import com.example.tomcat.adapter.out.persistence.entity.TokenRevocadoEntity;
import com.example.tomcat.adapter.out.persistence.repository.SpringDataTokenEmitidoRepository;
import com.example.tomcat.adapter.out.persistence.repository.SpringDataTokenRevocadoRepository;
import com.example.tomcat.port.out.ListaNegraTokensPort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

/**
 * Lista negra de tokens JWT guardada en MySQL (tablas tokens_emitidos y tokens_revocados).
 * - Al iniciar sesión se registra el jti de cada token emitido.
 * - Revocar = copiar el token a tokens_revocados. El filtro JWT rechaza cualquier token que esté ahí.
 * - Una tarea periódica borra las filas de tokens ya expirados: ese token lo rechaza igual la validación de vigencia.
 * En producción esto mismo se haría con Redis (clave = jti, con TTL hasta la expiración); bastaría otro adaptador.
 */
@Component
public class ListaNegraTokensAdapter implements ListaNegraTokensPort {

    private final SpringDataTokenEmitidoRepository emitidos;
    private final SpringDataTokenRevocadoRepository revocados;
    private final Clock reloj;

    public ListaNegraTokensAdapter(SpringDataTokenEmitidoRepository emitidos, SpringDataTokenRevocadoRepository revocados,
                                   Clock reloj) {
        this.emitidos = emitidos;
        this.revocados = revocados;
        this.reloj = reloj;
    }

    public void registrarEmision(JwtTokenProvider.TokenGenerado token, Long usuarioId) {
        emitidos.save(new TokenEmitidoEntity(token.jti(), usuarioId, utc(token.emitidoEn()), utc(token.expiraEn())));
    }

    @Override
    public int revocarTodos(Long usuarioId, String motivo) {
        LocalDateTime ahora = ahoraUtc();
        int nuevos = 0;
        for (TokenEmitidoEntity token : emitidos.findByUsuarioIdAndExpiraEnAfter(usuarioId, ahora)) {
            if (!revocados.existsById(token.getJti())) {
                revocados.save(new TokenRevocadoEntity(token.getJti(), usuarioId, recortar(motivo), ahora, token.getExpiraEn()));
                nuevos++;
            }
        }
        return nuevos;
    }

    /** Cierre de sesión: revoca solo el token con el que se hizo la petición. */
    public void revocar(String jti, Long usuarioId, Instant expiraEn, String motivo) {
        if (!revocados.existsById(jti)) {
            revocados.save(new TokenRevocadoEntity(jti, usuarioId, recortar(motivo), ahoraUtc(), utc(expiraEn)));
        }
    }

    /** Motivo de la revocación si el token está en la lista negra; vacío si sigue siendo válido. */
    public Optional<String> motivoRevocacion(String jti) {
        return revocados.findById(jti).map(TokenRevocadoEntity::getMotivo);
    }

    /** Borra de ambas tablas los tokens que ya expiraron. Devuelve cuántas filas eliminó. */
    @Transactional
    public int limpiarExpirados() {
        LocalDateTime ahora = ahoraUtc();
        return emitidos.borrarExpirados(ahora) + revocados.borrarExpirados(ahora);
    }

    private LocalDateTime ahoraUtc() {
        return utc(reloj.instant());
    }

    private static LocalDateTime utc(Instant instante) {
        return LocalDateTime.ofInstant(instante, ZoneOffset.UTC);
    }

    private static String recortar(String motivo) {
        return motivo.length() <= 300 ? motivo : motivo.substring(0, 300);
    }
}
