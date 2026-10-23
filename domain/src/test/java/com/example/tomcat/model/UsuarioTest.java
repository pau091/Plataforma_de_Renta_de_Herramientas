package com.example.tomcat.model;

import com.example.tomcat.exception.DomainException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Usuario: suspensión, baneo y estado de la cuenta")
class UsuarioTest {

    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 10, 17, 20, 0);   // UTC
    private static final ZoneId BOGOTA = ZoneId.of("America/Bogota");

    private static Usuario juan() {
        return new Usuario(12L, "Juan", "juan@correo.com", "hash", true, null, Rol.CLIENTE);
    }

    @Test
    @DisplayName("La suspensión bloquea hasta la fecha indicada y luego se libera sola")
    void suspensionTemporal() {
        Usuario u = juan();
        u.suspender(AHORA.plusDays(2), "  Cancelaciones reiteradas ", AHORA);

        assertThat(u.estaSancionado(AHORA)).isTrue();
        assertThat(u.estadoCuenta(AHORA)).isEqualTo("SUSPENDIDA");
        assertThat(u.getMotivoSancion()).isEqualTo("Cancelaciones reiteradas");
        assertThat(u.mensajeSancion(AHORA, BOGOTA)).contains("hasta el 2026-10-19 15:00");   // UTC-5
        assertThat(u.estaSancionado(AHORA.plusDays(2))).isFalse();
        assertThat(u.estadoCuenta(AHORA.plusDays(3))).isEqualTo("ACTIVA");
    }

    @Test
    @DisplayName("La suspensión exige una fecha futura y un motivo")
    void suspensionInvalida() {
        assertThatThrownBy(() -> juan().suspender(AHORA, "Motivo", AHORA)).isInstanceOf(DomainException.class);
        assertThatThrownBy(() -> juan().suspender(AHORA.plusDays(1), null, AHORA)).isInstanceOf(DomainException.class);
        assertThatThrownBy(() -> juan().suspender(AHORA.plusDays(1), "x".repeat(256), AHORA))
                .isInstanceOf(DomainException.class);
    }

    @Test
    @DisplayName("El baneo es indefinido; levantarlo deja la cuenta activa")
    void baneo() {
        Usuario u = juan();
        u.banear("Fraude en pagos");
        assertThat(u.estadoCuenta(AHORA.plusYears(10))).isEqualTo("BANEADA");
        assertThat(u.mensajeSancion(AHORA, BOGOTA)).contains("baneada").contains("Fraude en pagos");

        u.levantarSancion(AHORA);
        assertThat(u.estaSancionado(AHORA)).isFalse();
        assertThat(u.getMotivoSancion()).isNull();
        assertThatThrownBy(() -> u.levantarSancion(AHORA)).isInstanceOf(DomainException.class);
    }

    @Test
    @DisplayName("Una cuenta desactivada se reporta como INACTIVA")
    void inactiva() {
        Usuario u = juan();
        u.desactivar();
        assertThat(u.estadoCuenta(AHORA)).isEqualTo("INACTIVA");
        assertThat(u.mensajeSancion(AHORA, BOGOTA)).isNull();
    }
}
