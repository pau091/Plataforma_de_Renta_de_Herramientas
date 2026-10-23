package com.example.tomcat.model;

import com.example.tomcat.exception.DomainException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Lista de espera: ESPERANDO → NOTIFICADA → ATENDIDA, o CANCELADA")
class EntradaListaEsperaTest {

    private static final LocalDate HOY = LocalDate.of(2026, 10, 17);
    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 10, 17, 20, 0);

    private static EntradaListaEspera entrada() {
        return EntradaListaEspera.nueva(3L, 12L, HOY.plusDays(10), HOY.plusDays(15), HOY, AHORA);
    }

    @Test
    @DisplayName("Nace ESPERANDO y valida las fechas igual que una reserva")
    void nace() {
        assertThat(entrada().getEstado()).isEqualTo("ESPERANDO");
        assertThat(entrada().activa()).isTrue();
        assertThatThrownBy(() -> EntradaListaEspera.nueva(3L, 12L, HOY.minusDays(1), HOY, HOY, AHORA))
                .isInstanceOf(DomainException.class);
    }

    @Test
    @DisplayName("Detecta si sus fechas se cruzan con las de una reserva (extremos incluidos)")
    void cruce() {
        EntradaListaEspera e = entrada();   // del día +10 al +15
        assertThat(e.seCruzaCon(HOY.plusDays(15), HOY.plusDays(20))).isTrue();
        assertThat(e.seCruzaCon(HOY.plusDays(5), HOY.plusDays(10))).isTrue();
        assertThat(e.seCruzaCon(HOY.plusDays(16), HOY.plusDays(20))).isFalse();
    }

    @Test
    @DisplayName("Se notifica una sola vez; luego puede atenderse o cancelarse")
    void ciclo() {
        EntradaListaEspera e = entrada();
        e.notificar(AHORA);
        assertThat(e.getEstado()).isEqualTo("NOTIFICADA");
        assertThat(e.getNotificadaEn()).isEqualTo(AHORA);
        assertThat(e.activa()).isTrue();
        assertThatThrownBy(() -> e.notificar(AHORA)).isInstanceOf(DomainException.class);

        e.atender();
        assertThat(e.getEstado()).isEqualTo("ATENDIDA");
        assertThatThrownBy(e::cancelar).isInstanceOf(DomainException.class);
    }
}
