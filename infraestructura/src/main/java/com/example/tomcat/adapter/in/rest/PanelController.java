package com.example.tomcat.adapter.in.rest;

import com.example.tomcat.port.in.ConsultarPanelUseCase;
import com.example.tomcat.port.in.dto.Panel;
import com.example.tomcat.security.UsuarioAutenticado;
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
@RestController
@RequestMapping("/api/panel")
public class PanelController {

    private final ConsultarPanelUseCase consultarPanelUseCase;

    public PanelController(ConsultarPanelUseCase consultarPanelUseCase) {
        this.consultarPanelUseCase = consultarPanelUseCase;
    }

    @GetMapping
    public ResponseEntity<Panel> obtener(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return ResponseEntity.ok(consultarPanelUseCase.obtener(usuario.solicitante()));
    }
}
