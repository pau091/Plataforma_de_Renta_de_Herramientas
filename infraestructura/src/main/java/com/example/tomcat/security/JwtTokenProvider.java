package com.example.tomcat.security;

import com.example.tomcat.model.Rol;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtTokenProvider {

    private final SecretKey signingKey;
    private final long jwtExpirationMs;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String jwtSecret,
            @Value("${jwt.expiration-ms:86400000}") long jwtExpirationMs
    ) {
        this.signingKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        this.jwtExpirationMs = jwtExpirationMs;
    }

    public String generarToken(String email, Rol rol, Long usuarioId) {
        Date ahora = new Date();
        Date expiracion = new Date(ahora.getTime() + jwtExpirationMs);

        return Jwts.builder()
                .subject(email)
                .claim("rol", rol.name())
                .claim("usuarioId", usuarioId)
                .issuedAt(ahora)
                .expiration(expiracion)
                .signWith(signingKey)
                .compact();
    }

    public String obtenerEmailDelToken(String token) {
        return parseClaims(token).getSubject();
    }

    public Rol obtenerRolDelToken(String token) {
        Object rol = parseClaims(token).get("rol");
        return Rol.desde(rol != null ? rol.toString() : null);
    }

    public Long obtenerUsuarioIdDelToken(String token) {
        Object usuarioId = parseClaims(token).get("usuarioId");
        if (usuarioId instanceof Number number) {
            return number.longValue();
        }
        return usuarioId != null ? Long.parseLong(usuarioId.toString()) : null;
    }

    public boolean validarToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
