package com.example.tomcat.security;

import com.example.tomcat.model.Rol;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

@Component
public class JwtTokenProvider {

    /** Token recién firmado y los datos con que se registra para poder revocarlo después. */
    public record TokenGenerado(String token, String jti, Instant emitidoEn, Instant expiraEn) {
    }

    /** Datos de un token con firma y vigencia válidas. */
    public record DatosToken(Long usuarioId, String jti, Instant expiraEn) {
    }

    private final SecretKey signingKey;
    private final long jwtExpirationMs;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String jwtSecret,
            @Value("${jwt.expiration-ms:86400000}") long jwtExpirationMs
    ) {
        this.signingKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        this.jwtExpirationMs = jwtExpirationMs;
    }

    public TokenGenerado generarToken(String email, Rol rol, Long usuarioId) {
        Instant ahora = Instant.now();
        Instant expiracion = ahora.plusMillis(jwtExpirationMs);
        // jti: identificador único del token. Es la llave de la lista negra (tokens_revocados).
        String jti = UUID.randomUUID().toString();

        String token = Jwts.builder()
                .id(jti)
                .subject(email)
                .claim("rol", rol.name())
                .claim("usuarioId", usuarioId)
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(expiracion))
                .signWith(signingKey)
                .compact();
        return new TokenGenerado(token, jti, ahora, expiracion);
    }

    /** Verifica firma y vigencia; vacío si el token es inválido, está alterado o expiró. */
    public Optional<DatosToken> leer(String token) {
        try {
            Claims claims = Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token).getPayload();
            return Optional.of(new DatosToken(usuarioId(claims), claims.getId(), claims.getExpiration().toInstant()));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private static Long usuarioId(Claims claims) {
        Object usuarioId = claims.get("usuarioId");
        if (usuarioId instanceof Number number) {
            return number.longValue();
        }
        return usuarioId != null ? Long.parseLong(usuarioId.toString()) : null;
    }
}
