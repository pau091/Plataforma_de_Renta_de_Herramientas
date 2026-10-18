package com.example.tomcat.model;

import com.example.tomcat.exception.DomainException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Herramienta y Mantenimiento")
class HerramientaYMantenimientoTest {

    private static Herramienta herramienta() {
        return new Herramienta(1L, 2L, 1L, "Taladro", "Taladro 800W", new BigDecimal("45000"), null, null);
    }

    @Test
    @DisplayName("Sin estado nace DISPONIBLE")
    void naceDisponible() {
        assertThat(herramienta().estaDisponible()).isTrue();
    }

    @ParameterizedTest(name = "acepta el estado {0}")
    @ValueSource(strings = {"DISPONIBLE", "MANTENIMIENTO", "FUERA_DE_SERVICIO"})
    void cambiaAEstadosValidos(String estado) {
        Herramienta h = herramienta();
        h.cambiarEstado(estado);
        assertThat(h.getEstado()).isEqualTo(estado);
        assertThat(h.estaDisponible()).isEqualTo("DISPONIBLE".equals(estado));
    }

    @ParameterizedTest(name = "rechaza el estado \"{0}\"")
    @ValueSource(strings = {"ROTA", "disponible", ""})
    void rechazaEstadosInvalidos(String estado) {
        assertThatThrownBy(() -> herramienta().cambiarEstado(estado)).isInstanceOf(DomainException.class);
    }

    @Test
    @DisplayName("Actualizar conserva la descripción, el estado y la imagen si no se envían")
    void actualizacionParcial() {
        Herramienta h = herramienta();
        h.actualizarImagen("/uploads/herramientas/a.png");
        h.actualizar(4L, "Taladro Pro", null, new BigDecimal("50000"), null, null);
        assertThat(h.getNombre()).isEqualTo("Taladro Pro");
        assertThat(h.getCategoriaId()).isEqualTo(4L);
        assertThat(h.getTarifaDiaria()).isEqualByComparingTo("50000");
        assertThat(h.getDescripcion()).isEqualTo("Taladro 800W");
        assertThat(h.getEstado()).isEqualTo("DISPONIBLE");
        assertThat(h.getImagenUrl()).isEqualTo("/uploads/herramientas/a.png");
    }

    @Test
    @DisplayName("Un mantenimiento abierto se cierra una sola vez con su costo")
    void cierraMantenimiento() {
        Mantenimiento m = new Mantenimiento(1L, 1L, null, "Cambio de carbones", null, null, null);
        assertThat(m.estaAbierto()).isTrue();
        assertThat(m.getFechaInicio()).isNotNull();

        m.cerrar(new BigDecimal("30000"));
        assertThat(m.estaAbierto()).isFalse();
        assertThat(m.getCostoReparacion()).isEqualByComparingTo("30000");
        assertThatThrownBy(() -> m.cerrar(BigDecimal.ONE))
                .isInstanceOf(DomainException.class).hasMessageContaining("ya fue cerrado");
    }
}
