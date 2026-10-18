package com.example.tomcat.adapter.in.rest;

import com.example.tomcat.model.Factura;
import com.example.tomcat.port.in.GestionarFacturaUseCase;
import com.example.tomcat.security.UsuarioAutenticado;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Tag(name = "Facturas")
@RestController
@RequestMapping("/api/facturas")
public class FacturaController {

    private static final String SIN_PERMISO = "No tiene permiso para acceder a esta factura.";

    private final GestionarFacturaUseCase gestionarFacturaUseCase;
    private final FacturaHtmlRenderer facturaHtmlRenderer;

    public FacturaController(GestionarFacturaUseCase gestionarFacturaUseCase, FacturaHtmlRenderer facturaHtmlRenderer) {
        this.gestionarFacturaUseCase = gestionarFacturaUseCase;
        this.facturaHtmlRenderer = facturaHtmlRenderer;
    }

    @Operation(summary = "Listar facturas",
            description = "**Roles:** CLIENTE (las suyas), PROVEEDOR (las de sus herramientas) o ADMIN (todas, o las de un cliente).")
    @ApiResponse(responseCode = "200", description = "Facturas visibles para el usuario")
    @ApiResponse(responseCode = "403", description = "Solo puede consultar sus propias facturas.")
    @GetMapping
    public ResponseEntity<List<Factura>> listar(@AuthenticationPrincipal UsuarioAutenticado usuario,
                                                @Parameter(description = "Solo facturas de este cliente", example = "12")
                                                @RequestParam(required = false) Long clienteId) {
        return ResponseEntity.ok(gestionarFacturaUseCase.listar(usuario.solicitante(), clienteId));
    }

    @Operation(summary = "Consultar una factura", description = "**Roles:** el CLIENTE facturado, el PROVEEDOR de la herramienta o ADMIN.")
    @ApiResponse(responseCode = "200", description = "Factura encontrada")
    @ApiResponse(responseCode = "403", description = SIN_PERMISO)
    @ApiResponse(responseCode = "404", description = "La factura no existe.")
    @GetMapping("/{id}")
    public ResponseEntity<Factura> obtener(@AuthenticationPrincipal UsuarioAutenticado usuario,
                                           @Parameter(example = "1") @PathVariable Long id) {
        return ResponseEntity.ok(gestionarFacturaUseCase.buscarPorId(usuario.solicitante(), id));
    }

    @Operation(summary = "Factura de un pago", description = "**Roles:** el CLIENTE facturado, el PROVEEDOR de la herramienta o ADMIN.")
    @ApiResponse(responseCode = "200", description = "Factura del pago")
    @ApiResponse(responseCode = "403", description = SIN_PERMISO)
    @ApiResponse(responseCode = "404", description = "No hay factura asociada al pago.")
    @GetMapping("/pago/{pagoId}")
    public ResponseEntity<Factura> porPago(@AuthenticationPrincipal UsuarioAutenticado usuario,
                                           @Parameter(example = "1") @PathVariable Long pagoId) {
        return ResponseEntity.ok(gestionarFacturaUseCase.buscarPorPago(usuario.solicitante(), pagoId));
    }

    @Operation(summary = "Factura de una reserva", description = "**Roles:** el CLIENTE facturado, el PROVEEDOR de la herramienta o ADMIN.")
    @ApiResponse(responseCode = "200", description = "Factura de la reserva")
    @ApiResponse(responseCode = "403", description = SIN_PERMISO)
    @ApiResponse(responseCode = "404", description = "No hay factura asociada a la reserva.")
    @GetMapping("/reserva/{reservaId}")
    public ResponseEntity<Factura> porReserva(@AuthenticationPrincipal UsuarioAutenticado usuario,
                                              @Parameter(example = "1") @PathVariable Long reservaId) {
        return ResponseEntity.ok(gestionarFacturaUseCase.buscarPorReserva(usuario.solicitante(), reservaId));
    }

    @Operation(summary = "Descargar una factura",
            description = "**Roles:** el CLIENTE facturado, el PROVEEDOR de la herramienta o ADMIN. Devuelve el comprobante "
                    + "como archivo HTML adjunto (`FAC-000001.html`), listo para abrir o imprimir. Si el pago se reembolsó, "
                    + "muestra la marca FACTURA ANULADA.")
    @ApiResponse(responseCode = "200", description = "Archivo HTML de la factura",
            content = @Content(mediaType = MediaType.TEXT_HTML_VALUE, schema = @Schema(type = "string", format = "binary")))
    @ApiResponse(responseCode = "403", description = SIN_PERMISO)
    @ApiResponse(responseCode = "404", description = "La factura no existe.")
    @GetMapping(value = "/{id}/descargar", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<byte[]> descargar(@AuthenticationPrincipal UsuarioAutenticado usuario,
                                            @Parameter(example = "1") @PathVariable Long id) {
        Factura factura = gestionarFacturaUseCase.buscarPorId(usuario.solicitante(), id);
        byte[] contenido = facturaHtmlRenderer.render(factura).getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + factura.getNumero() + ".html")
                .contentType(MediaType.TEXT_HTML)
                .body(contenido);
    }
}
