package com.example.tomcat.adapter.in.rest;

import com.example.tomcat.model.EntradaListaEspera;
import com.example.tomcat.port.in.ListaEsperaUseCase;
import com.example.tomcat.security.UsuarioAutenticado;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "Lista de espera", description = "Anotarse a la espera de fechas ocupadas: si la reserva que las ocupa se "
        + "cancela, se rechaza, vence o se reembolsa, se notifica al primero de la fila.")
@RestController
@RequestMapping("/api/lista-espera")
public class ListaEsperaController {

    private final ListaEsperaUseCase listaEsperaUseCase;

    public ListaEsperaController(ListaEsperaUseCase listaEsperaUseCase) {
        this.listaEsperaUseCase = listaEsperaUseCase;
    }

    @Operation(summary = "Anotarse en la lista de espera",
            description = "**Roles:** CLIENTE o PROVEEDOR (para herramientas de otros). Solo si esas fechas están ocupadas; "
                    + "si están libres responde 400 (reserve directamente). Se atiende por orden de llegada.")
    @ApiResponse(responseCode = "201", description = "Anotado: queda ESPERANDO")
    @ApiResponse(responseCode = "400", description = "La herramienta está libre en esas fechas: puede reservarla directamente.")
    @ApiResponse(responseCode = "403", description = "Solo clientes o proveedores (alquilando equipos de otros) pueden anotarse.")
    @ApiResponse(responseCode = "409", description = "Ya está en la lista de espera de esta herramienta para esas fechas.")
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public ResponseEntity<EntradaListaEspera> anotarse(@AuthenticationPrincipal UsuarioAutenticado usuario,
                                                       @Valid @RequestBody ListaEsperaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(listaEsperaUseCase.anotarse(usuario.solicitante(),
                request.herramientaId(), request.fechaInicio(), request.fechaFin()));
    }

    @Operation(summary = "Mis entradas en la lista de espera",
            description = "**Roles:** cualquier usuario autenticado. Estados: ESPERANDO, NOTIFICADA (ya puede reservar), "
                    + "ATENDIDA (reservó) o CANCELADA. Más recientes primero.")
    @ApiResponse(responseCode = "200", description = "Entradas del usuario")
    @GetMapping("/mias")
    public ResponseEntity<List<EntradaListaEspera>> mias(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return ResponseEntity.ok(listaEsperaUseCase.mias(usuario.solicitante()));
    }

    @Operation(summary = "Salir de la lista de espera", description = "**Roles:** quien se anotó (o ADMIN). Queda CANCELADA.")
    @ApiResponse(responseCode = "200", description = "Entrada cancelada")
    @ApiResponse(responseCode = "403", description = "Solo quien se anotó puede salir de la lista de espera.")
    @ApiResponse(responseCode = "404", description = "La entrada de la lista de espera no existe.")
    @DeleteMapping("/{id}")
    public ResponseEntity<EntradaListaEspera> cancelar(@AuthenticationPrincipal UsuarioAutenticado usuario,
                                                       @Parameter(example = "1") @PathVariable Long id) {
        return ResponseEntity.ok(listaEsperaUseCase.cancelar(usuario.solicitante(), id));
    }

    public record ListaEsperaRequest(
            @Schema(example = "3") @NotNull(message = "La herramienta es obligatoria.") Long herramientaId,
            @Schema(type = "string", example = "2026-12-01") @NotNull(message = "La fecha de inicio es obligatoria.") LocalDate fechaInicio,
            @Schema(type = "string", example = "2026-12-03") @NotNull(message = "La fecha fin es obligatoria.") LocalDate fechaFin
    ) {}
}
