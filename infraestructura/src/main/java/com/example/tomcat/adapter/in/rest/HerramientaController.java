package com.example.tomcat.adapter.in.rest;

import com.example.tomcat.model.Herramienta;
import com.example.tomcat.port.in.GestionarHerramientaUseCase;
import com.example.tomcat.security.UsuarioAutenticado;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

@Tag(name = "Herramientas")
@RestController
@RequestMapping("/api/herramientas")
public class HerramientaController {

    private static final String SOLO_DUENO = "Solo el proveedor dueño de la herramienta puede gestionarla.";

    private final GestionarHerramientaUseCase gestionarHerramientaUseCase;

    public HerramientaController(GestionarHerramientaUseCase gestionarHerramientaUseCase) {
        this.gestionarHerramientaUseCase = gestionarHerramientaUseCase;
    }

    /** Catálogo con filtros combinables: categoría, proveedor, solo disponibles y texto (q) en nombre o descripción. */
    @Operation(summary = "Explorar el catálogo",
            description = "**Roles:** cualquier usuario autenticado. Todos los filtros son opcionales y se combinan. "
                    + "Para saber si una herramienta está libre en unas fechas use `GET /api/reservas/disponibilidad`.")
    @ApiResponse(responseCode = "200", description = "Herramientas que cumplen los filtros")
    @ApiResponse(responseCode = "404", description = "La categoría no existe.")
    @GetMapping
    public ResponseEntity<List<Herramienta>> listar(
            @Parameter(description = "Solo herramientas de esta categoría", example = "1")
            @RequestParam(required = false) Long categoriaId,
            @Parameter(description = "true = solo herramientas en estado DISPONIBLE", example = "true")
            @RequestParam(required = false) Boolean disponibles,
            @Parameter(description = "Solo herramientas de este proveedor", example = "2")
            @RequestParam(required = false) Long proveedorId,
            @Parameter(description = "Texto a buscar en el nombre o la descripción (sin distinguir mayúsculas)", example = "taladro")
            @RequestParam(required = false) String q
    ) {
        return ResponseEntity.ok(gestionarHerramientaUseCase.buscar(
                categoriaId, proveedorId, Boolean.TRUE.equals(disponibles), q));
    }

    /** Inventario del proveedor autenticado. */
    @Operation(summary = "Mi inventario", description = "**Rol:** PROVEEDOR. Herramientas del proveedor autenticado con su estado.")
    @GetMapping("/mias")
    public ResponseEntity<List<Herramienta>> listarMias(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return ResponseEntity.ok(gestionarHerramientaUseCase.listarPorProveedor(usuario.id()));
    }

    @Operation(summary = "Consultar una herramienta", description = "**Roles:** cualquier usuario autenticado. Incluye precio y estado.")
    @ApiResponse(responseCode = "200", description = "Herramienta encontrada")
    @ApiResponse(responseCode = "404", description = "La herramienta no existe.")
    @GetMapping("/{id}")
    public ResponseEntity<Herramienta> obtener(@Parameter(example = "1") @PathVariable Long id) {
        return ResponseEntity.ok(gestionarHerramientaUseCase.buscarPorId(id));
    }

    @Operation(summary = "Registrar una herramienta",
            description = "**Roles:** PROVEEDOR (queda a su nombre; `proveedorId` se ignora) o ADMIN (debe indicar `proveedorId`). "
                    + "Nace en estado DISPONIBLE. La imagen se puede subir después con `POST /{id}/imagen`.")
    @ApiResponse(responseCode = "201", description = "Herramienta registrada")
    @ApiResponse(responseCode = "400", description = "La tarifa diaria debe ser mayor que cero.")
    @ApiResponse(responseCode = "403", description = "Su rol no tiene permiso para realizar esta operación.")
    @ApiResponse(responseCode = "404", description = "La categoría no existe.")
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public ResponseEntity<Herramienta> crear(@AuthenticationPrincipal UsuarioAutenticado usuario,
                                             @Valid @RequestBody CrearHerramientaRequest request) {
        // proveedorId solo se usa cuando quien registra es ADMIN; un PROVEEDOR siempre registra a su nombre.
        return ResponseEntity.status(HttpStatus.CREATED).body(gestionarHerramientaUseCase.crear(
                usuario.solicitante(),
                request.proveedorId(),
                request.categoriaId(),
                request.nombre(),
                request.descripcion(),
                request.tarifaDiaria(),
                request.deposito(),
                request.imagenUrl()
        ));
    }

    @Operation(summary = "Editar una herramienta",
            description = "**Roles:** el PROVEEDOR dueño o ADMIN. Edición parcial: lo que no se envía se conserva. "
                    + "Permite cambiar costo, disponibilidad (estado) y descripción.")
    @ApiResponse(responseCode = "200", description = "Herramienta actualizada")
    @ApiResponse(responseCode = "400", description = "Estado de herramienta no válido. Use DISPONIBLE, MANTENIMIENTO o FUERA_DE_SERVICIO.")
    @ApiResponse(responseCode = "403", description = SOLO_DUENO)
    @ApiResponse(responseCode = "404", description = "La herramienta no existe.")
    @PutMapping("/{id}")
    public ResponseEntity<Herramienta> actualizar(@AuthenticationPrincipal UsuarioAutenticado usuario,
                                                  @Parameter(example = "1") @PathVariable Long id,
                                                  @Valid @RequestBody ActualizarHerramientaRequest request) {
        return ResponseEntity.ok(gestionarHerramientaUseCase.actualizar(
                usuario.solicitante(),
                id,
                request.categoriaId(),
                request.nombre(),
                request.descripcion(),
                request.tarifaDiaria(),
                request.deposito(),
                request.estado(),
                request.imagenUrl()
        ));
    }

    @Operation(summary = "Cambiar la disponibilidad", description = "**Roles:** el PROVEEDOR dueño o ADMIN.")
    @ApiResponse(responseCode = "200", description = "Estado actualizado")
    @ApiResponse(responseCode = "400", description = "Estado de herramienta no válido. Use DISPONIBLE, MANTENIMIENTO o FUERA_DE_SERVICIO.")
    @ApiResponse(responseCode = "403", description = SOLO_DUENO)
    @ApiResponse(responseCode = "404", description = "La herramienta no existe.")
    @PatchMapping("/{id}/estado")
    public ResponseEntity<Herramienta> cambiarEstado(@AuthenticationPrincipal UsuarioAutenticado usuario,
                                                     @Parameter(example = "1") @PathVariable Long id,
                                                     @Valid @RequestBody EstadoRequest request) {
        return ResponseEntity.ok(gestionarHerramientaUseCase.cambiarEstado(usuario.solicitante(), id, request.estado()));
    }

    @Operation(summary = "Asignar una imagen por URL", description = "**Roles:** el PROVEEDOR dueño o ADMIN. Alternativa a subir el archivo.")
    @ApiResponse(responseCode = "200", description = "Imagen asignada")
    @ApiResponse(responseCode = "403", description = SOLO_DUENO)
    @ApiResponse(responseCode = "404", description = "La herramienta no existe.")
    @PatchMapping("/{id}/imagen")
    public ResponseEntity<Herramienta> actualizarImagen(@AuthenticationPrincipal UsuarioAutenticado usuario,
                                                        @Parameter(example = "1") @PathVariable Long id,
                                                        @Valid @RequestBody ImagenRequest request) {
        return ResponseEntity.ok(gestionarHerramientaUseCase.actualizarImagen(usuario.solicitante(), id, request.imagenUrl()));
    }

    /** Sube el archivo de imagen de la herramienta (campo "archivo"; JPG, PNG, WEBP o GIF, máx. 5 MB). */
    @Operation(summary = "Subir la imagen de una herramienta",
            description = "**Roles:** el PROVEEDOR dueño o ADMIN. `multipart/form-data` con el campo `archivo`. "
                    + "Formatos JPG, PNG, WEBP o GIF (se valida el contenido, no la extensión), máximo 5 MB. "
                    + "La imagen queda pública en la `imagenUrl` devuelta (`/uploads/...`) y reemplaza a la anterior.")
    @ApiResponse(responseCode = "200", description = "Imagen guardada")
    @ApiResponse(responseCode = "400", description = "Formato de imagen no permitido. Use JPG, PNG, WEBP o GIF.")
    @ApiResponse(responseCode = "403", description = SOLO_DUENO)
    @ApiResponse(responseCode = "404", description = "La herramienta no existe.")
    @ApiResponse(responseCode = "413", description = "La imagen supera el tamaño máximo de 5 MB.")
    @PostMapping(value = "/{id}/imagen", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Herramienta> subirImagen(@AuthenticationPrincipal UsuarioAutenticado usuario,
                                                   @Parameter(example = "1") @PathVariable Long id,
                                                   @Parameter(description = "Archivo de imagen (JPG, PNG, WEBP o GIF; máx. 5 MB)")
                                                   @RequestParam("archivo") MultipartFile archivo) throws IOException {
        return ResponseEntity.ok(gestionarHerramientaUseCase.subirImagen(usuario.solicitante(), id, archivo.getBytes()));
    }

    @Operation(summary = "Eliminar una herramienta",
            description = "**Roles:** el PROVEEDOR dueño o ADMIN. Si ya tiene reservas o mantenimientos no se elimina "
                    + "(se conserva el historial): márquela FUERA_DE_SERVICIO.")
    @ApiResponse(responseCode = "204", description = "Herramienta eliminada")
    @ApiResponse(responseCode = "403", description = SOLO_DUENO)
    @ApiResponse(responseCode = "404", description = "La herramienta no existe.")
    @ApiResponse(responseCode = "409", description = "La herramienta tiene reservas o mantenimientos registrados y no se puede eliminar. Cambie su estado a FUERA_DE_SERVICIO.")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@AuthenticationPrincipal UsuarioAutenticado usuario,
                                         @Parameter(example = "1") @PathVariable Long id) {
        gestionarHerramientaUseCase.eliminar(usuario.solicitante(), id);
        return ResponseEntity.noContent().build();
    }

    /** proveedorId solo lo usa un ADMIN; un PROVEEDOR siempre registra a su nombre. */
    public record CrearHerramientaRequest(
            @Schema(example = "2", description = "Solo para ADMIN: dueño de la herramienta")
            Long proveedorId,
            @Schema(example = "2")
            @NotNull(message = "La categoría es obligatoria.")
            Long categoriaId,
            @Schema(example = "Esmeriladora Angular 4 1/2 pulgadas")
            @NotBlank(message = "El nombre de la herramienta es obligatorio.")
            @Size(max = 150, message = "El nombre admite máximo 150 caracteres.")
            String nombre,
            @Schema(example = "Esmeriladora de 850W con guarda y llave, ideal para corte de metal y cerámica")
            String descripcion,
            @Schema(example = "25000", description = "Costo del alquiler por día (COP), mayor que 0")
            @NotNull(message = "La tarifa diaria es obligatoria.")
            @Positive(message = "La tarifa diaria debe ser mayor que cero.")
            @Digits(integer = 10, fraction = 2, message = "La tarifa admite hasta 10 enteros y 2 decimales.")
            BigDecimal tarifaDiaria,
            @Schema(example = "50000", description = "Depósito de garantía que se cobra con el alquiler y se devuelve al "
                    + "recibir la herramienta en buen estado. Opcional: 0 o vacío = sin depósito")
            @PositiveOrZero(message = "El depósito no puede ser negativo.")
            @Digits(integer = 10, fraction = 2, message = "El depósito admite hasta 10 enteros y 2 decimales.")
            BigDecimal deposito,
            @Schema(example = "https://images.example.com/esmeriladora.jpg", description = "Opcional: URL externa de la imagen")
            @Size(max = 500, message = "La URL de la imagen admite máximo 500 caracteres.")
            String imagenUrl
    ) {}

    /** Todos los campos son opcionales: solo se cambia lo que se envía. */
    public record ActualizarHerramientaRequest(
            @Schema(example = "2")
            Long categoriaId,
            @Schema(example = "Esmeriladora Angular 4 1/2 pulgadas")
            @Size(max = 150, message = "El nombre admite máximo 150 caracteres.")
            String nombre,
            @Schema(example = "Incluye disco de corte y gafas de seguridad")
            String descripcion,
            @Schema(example = "28000")
            @Positive(message = "La tarifa diaria debe ser mayor que cero.")
            @Digits(integer = 10, fraction = 2, message = "La tarifa admite hasta 10 enteros y 2 decimales.")
            BigDecimal tarifaDiaria,
            @Schema(example = "56000", description = "Nuevo depósito de garantía (aplica a los pagos futuros)")
            @PositiveOrZero(message = "El depósito no puede ser negativo.")
            @Digits(integer = 10, fraction = 2, message = "El depósito admite hasta 10 enteros y 2 decimales.")
            BigDecimal deposito,
            @Schema(example = "DISPONIBLE", allowableValues = {"DISPONIBLE", "MANTENIMIENTO", "FUERA_DE_SERVICIO"})
            String estado,
            @Schema(example = "https://images.example.com/esmeriladora-v2.jpg")
            @Size(max = 500, message = "La URL de la imagen admite máximo 500 caracteres.")
            String imagenUrl
    ) {}

    public record EstadoRequest(
            @Schema(example = "FUERA_DE_SERVICIO", allowableValues = {"DISPONIBLE", "MANTENIMIENTO", "FUERA_DE_SERVICIO"})
            @NotBlank(message = "El estado es obligatorio (DISPONIBLE, MANTENIMIENTO o FUERA_DE_SERVICIO).")
            String estado
    ) {}

    public record ImagenRequest(
            @Schema(example = "https://images.example.com/taladro.jpg")
            @NotBlank(message = "La URL de la imagen es obligatoria.")
            @Size(max = 500, message = "La URL de la imagen admite máximo 500 caracteres.")
            String imagenUrl
    ) {}
}
