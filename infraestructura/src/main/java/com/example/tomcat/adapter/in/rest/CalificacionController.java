package com.example.tomcat.adapter.in.rest;

import com.example.tomcat.model.Calificacion;
import com.example.tomcat.port.in.CalificarUseCase;
import com.example.tomcat.port.in.dto.ResumenCalificaciones;
import com.example.tomcat.security.UsuarioAutenticado;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Calificaciones", description = "Reseñas de 1 a 5 estrellas de la herramienta y del proveedor al terminar un alquiler.")
@RestController
public class CalificacionController {

    private final CalificarUseCase calificarUseCase;

    public CalificacionController(CalificarUseCase calificarUseCase) {
        this.calificarUseCase = calificarUseCase;
    }

    @Operation(summary = "Calificar un alquiler terminado",
            description = "**Roles:** quien alquiló la herramienta (cliente o proveedor que alquila). La reserva debe estar "
                    + "**FINALIZADA** y solo se califica una vez. Califique la herramienta, al proveedor o ambos (1 a 5).")
    @ApiResponse(responseCode = "201", description = "Reseña registrada")
    @ApiResponse(responseCode = "400", description = "Solo se puede calificar un alquiler terminado (reserva FINALIZADA).")
    @ApiResponse(responseCode = "403", description = "Solo quien alquiló la herramienta puede calificarla.")
    @ApiResponse(responseCode = "404", description = "La reserva no existe.")
    @ApiResponse(responseCode = "409", description = "Este alquiler ya fue calificado.")
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/api/reservas/{id}/calificacion")
    public ResponseEntity<Calificacion> calificar(@AuthenticationPrincipal UsuarioAutenticado usuario,
                                                  @Parameter(example = "1") @PathVariable Long id,
                                                  @Valid @RequestBody CalificacionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(calificarUseCase.calificar(usuario.solicitante(), id,
                request.estrellasHerramienta(), request.estrellasProveedor(), request.comentario()));
    }

    @Operation(summary = "Reseñas de una herramienta", description = "**Roles:** cualquier usuario autenticado. Más recientes primero.")
    @ApiResponse(responseCode = "200", description = "Reseñas de la herramienta")
    @ApiResponse(responseCode = "404", description = "La herramienta no existe.")
    @GetMapping("/api/herramientas/{id}/calificaciones")
    public ResponseEntity<List<Calificacion>> porHerramienta(@Parameter(example = "1") @PathVariable Long id) {
        return ResponseEntity.ok(calificarUseCase.listarPorHerramienta(id));
    }

    @Operation(summary = "Promedios de calificación",
            description = "**Roles:** cualquier usuario autenticado. Promedio de estrellas y número de reseñas de cada herramienta "
                    + "y de cada proveedor; el catálogo los muestra en cada tarjeta.")
    @ApiResponse(responseCode = "200", description = "Promedios por herramienta y por proveedor")
    @GetMapping("/api/calificaciones/resumen")
    public ResponseEntity<ResumenCalificaciones> resumen() {
        return ResponseEntity.ok(calificarUseCase.resumen());
    }

    @Operation(summary = "Mis reseñas", description = "**Roles:** cualquier usuario autenticado. Las reseñas que dejó quien consulta.")
    @ApiResponse(responseCode = "200", description = "Reseñas propias")
    @GetMapping("/api/calificaciones/mias")
    public ResponseEntity<List<Calificacion>> mias(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return ResponseEntity.ok(calificarUseCase.mias(usuario.solicitante()));
    }

    public record CalificacionRequest(
            @Schema(example = "5", description = "Estrellas para la herramienta (1 a 5); opcional si califica al proveedor")
            @Min(value = 1, message = "Las estrellas van de 1 a 5.") @Max(value = 5, message = "Las estrellas van de 1 a 5.")
            Integer estrellasHerramienta,
            @Schema(example = "4", description = "Estrellas para el proveedor (1 a 5); opcional si califica la herramienta")
            @Min(value = 1, message = "Las estrellas van de 1 a 5.") @Max(value = 5, message = "Las estrellas van de 1 a 5.")
            Integer estrellasProveedor,
            @Schema(example = "Llegó a tiempo y funcionó perfecto.")
            @Size(max = 500, message = "El comentario admite máximo 500 caracteres.")
            String comentario
    ) {}
}
