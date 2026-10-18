package com.example.tomcat.adapter.in.rest;

import com.example.tomcat.port.in.ConsultarPanelUseCase;
import com.example.tomcat.port.in.dto.Panel;
import com.example.tomcat.security.UsuarioAutenticado;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Panel de control del usuario autenticado. El contenido cambia según el rol:
 * CLIENTE (historial y próximas reservas), PROVEEDOR (inventario y solicitudes pendientes)
 * o ADMIN (resumen global de la plataforma).
 */
@Tag(name = "Panel")
@RestController
@RequestMapping("/api/panel")
public class PanelController {

    private final ConsultarPanelUseCase consultarPanelUseCase;

    public PanelController(ConsultarPanelUseCase consultarPanelUseCase) {
        this.consultarPanelUseCase = consultarPanelUseCase;
    }

    @Operation(summary = "Panel de control según el rol",
            description = "**Roles:** cualquier usuario autenticado. El campo `rol` indica la forma de la respuesta: "
                    + "CLIENTE (reservas por estado, próximas reservas, total pagado y facturas), PROVEEDOR (inventario, "
                    + "solicitudes pendientes de responder, ingresos y mantenimientos abiertos) o ADMIN (totales de la plataforma).")
    @ApiResponse(responseCode = "200", description = "Resumen del usuario autenticado",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(oneOf = {Panel.Cliente.class, Panel.Proveedor.class, Panel.Admin.class})))
    @GetMapping
    public ResponseEntity<Panel> obtener(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return ResponseEntity.ok(consultarPanelUseCase.obtener(usuario.solicitante()));
    }
}
