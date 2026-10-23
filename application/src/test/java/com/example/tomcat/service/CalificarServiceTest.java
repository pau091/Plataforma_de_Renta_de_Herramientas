package com.example.tomcat.service;

import com.example.tomcat.exception.AccesoDenegadoException;
import com.example.tomcat.exception.ConflictoException;
import com.example.tomcat.model.Calificacion;
import com.example.tomcat.model.Rol;
import com.example.tomcat.port.in.dto.ResumenCalificaciones;
import com.example.tomcat.repository.CalificacionRepositoryPort;
import com.example.tomcat.repository.HerramientaRepositoryPort;
import com.example.tomcat.repository.ReservaRepositoryPort;
import com.example.tomcat.repository.UsuarioRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Optional;

import static com.example.tomcat.service.Datos.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Calificaciones: solo quien alquiló, una vez, y promedios para el catálogo")
class CalificarServiceTest {

    @Mock
    CalificacionRepositoryPort calificaciones;
    @Mock
    ReservaRepositoryPort reservas;
    @Mock
    HerramientaRepositoryPort herramientas;
    @Mock
    UsuarioRepositoryPort usuarios;

    CalificarService servicio;

    @BeforeEach
    void preparar() {
        servicio = new CalificarService(calificaciones, reservas, herramientas, usuarios, POLITICA);
        when(reservas.buscarPorId(1L)).thenReturn(Optional.of(reserva(1L, "FINALIZADA")));
        when(herramientas.buscarPorId(HERRAMIENTA_ID)).thenReturn(Optional.of(herramienta("DISPONIBLE")));
        when(usuarios.buscarPorId(CLIENTE_ID)).thenReturn(Optional.of(usuario(CLIENTE_ID, "Juan Cliente", Rol.CLIENTE, true)));
        when(calificaciones.guardar(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    @DisplayName("El cliente que alquiló califica herramienta y proveedor; se guarda con su nombre")
    void califica() {
        Calificacion c = servicio.calificar(CLIENTE, 1L, 5, 4, "Muy buena");
        assertThat(c.getClienteNombre()).isEqualTo("Juan Cliente");
        assertThat(c.getProveedorId()).isEqualTo(PROVEEDOR_ID);
        assertThat(c.getFecha()).isEqualTo(AHORA_UTC);
    }

    @Test
    @DisplayName("Nadie más puede calificar ese alquiler (ni otro cliente, ni el proveedor, ni el admin)")
    void soloQuienAlquilo() {
        for (var otro : List.of(OTRO_CLIENTE, PROVEEDOR, ADMIN)) {
            assertThatThrownBy(() -> servicio.calificar(otro, 1L, 5, 5, null)).isInstanceOf(AccesoDenegadoException.class);
        }
        verify(calificaciones, never()).guardar(any());
    }

    @Test
    @DisplayName("Solo una vez por alquiler")
    void unaVez() {
        when(calificaciones.existePorReserva(1L)).thenReturn(true);
        assertThatThrownBy(() -> servicio.calificar(CLIENTE, 1L, 5, 5, null))
                .isInstanceOf(ConflictoException.class).hasMessageContaining("ya fue calificado");
    }

    @Test
    @DisplayName("Promedios con 1 decimal; las reseñas que no calificaron un aspecto no cuentan en ese promedio")
    void promedios() {
        when(calificaciones.listar()).thenReturn(List.of(
                new Calificacion(1L, 1L, CLIENTE_ID, "Juan", 1L, PROVEEDOR_ID, 5, 4, null, AHORA_UTC),
                new Calificacion(2L, 2L, OTRO_CLIENTE_ID, "Ana", 1L, PROVEEDOR_ID, 4, null, null, AHORA_UTC),
                new Calificacion(3L, 3L, CLIENTE_ID, "Juan", 1L, PROVEEDOR_ID, 4, 3, null, AHORA_UTC)));

        ResumenCalificaciones r = servicio.resumen();

        assertThat(r.herramientas()).singleElement().satisfies(p -> {
            assertThat(p.promedio()).isEqualByComparingTo("4.3");
            assertThat(p.total()).isEqualTo(3);
        });
        assertThat(r.proveedores()).singleElement().satisfies(p -> {
            assertThat(p.promedio()).isEqualByComparingTo("3.5");
            assertThat(p.total()).isEqualTo(2);
        });
    }

    @Test
    @DisplayName("Una reserva que no terminó no se califica")
    void soloTerminadas() {
        when(reservas.buscarPorId(2L)).thenReturn(Optional.of(reserva(2L, "ENTREGADA")));
        assertThatThrownBy(() -> servicio.calificar(CLIENTE, 2L, 5, 5, null))
                .hasMessageContaining("FINALIZADA");
    }
}
