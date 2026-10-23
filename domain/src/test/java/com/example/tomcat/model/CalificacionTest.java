package com.example.tomcat.model;

import com.example.tomcat.exception.DomainException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Calificación: solo alquileres terminados, de 1 a 5 estrellas")
class CalificacionTest {

    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 10, 17, 20, 0);
    private static final Herramienta TALADRO = new Herramienta(3L, 2L, 1L, "Taladro", "800W", new BigDecimal("45000"), "DISPONIBLE", null);

    private static Reserva reserva(String estado) {
        return new Reserva(7L, 12L, 3L, LocalDate.of(2026, 10, 10), LocalDate.of(2026, 10, 12), estado,
                null, null, null, null, AHORA.minusDays(9), null);
    }

    @Test
    @DisplayName("Toma de la reserva y la herramienta a quién y qué se califica")
    void creaLaResena() {
        Calificacion c = Calificacion.de(reserva("FINALIZADA"), TALADRO, "Juan", 5, 4, "  Excelente  ", AHORA);
        assertThat(c.getReservaId()).isEqualTo(7L);
        assertThat(c.getClienteId()).isEqualTo(12L);
        assertThat(c.getProveedorId()).isEqualTo(2L);
        assertThat(c.getComentario()).isEqualTo("Excelente");
    }

    @Test
    @DisplayName("Basta con calificar la herramienta o al proveedor")
    void unaSolaCalificacion() {
        assertThat(Calificacion.de(reserva("FINALIZADA"), TALADRO, "Juan", null, 3, null, AHORA).getEstrellasHerramienta()).isNull();
        assertThatThrownBy(() -> Calificacion.de(reserva("FINALIZADA"), TALADRO, "Juan", null, null, "Hola", AHORA))
                .isInstanceOf(DomainException.class).hasMessageContaining("Califique");
    }

    @ParameterizedTest(name = "no se califica una reserva {0}")
    @ValueSource(strings = {"PENDIENTE", "CONFIRMADA", "ENTREGADA", "CANCELADA"})
    void soloTerminadas(String estado) {
        assertThatThrownBy(() -> Calificacion.de(reserva(estado), TALADRO, "Juan", 5, 5, null, AHORA))
                .isInstanceOf(DomainException.class).hasMessageContaining("FINALIZADA");
    }

    @ParameterizedTest(name = "{0} estrellas no es válido")
    @ValueSource(ints = {0, 6, -1})
    void estrellasFueraDeRango(int estrellas) {
        assertThatThrownBy(() -> Calificacion.de(reserva("FINALIZADA"), TALADRO, "Juan", estrellas, null, null, AHORA))
                .isInstanceOf(DomainException.class);
    }
}
