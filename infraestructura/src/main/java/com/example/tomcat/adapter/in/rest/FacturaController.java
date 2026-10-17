package com.example.tomcat.adapter.in.rest;

import com.example.tomcat.model.Factura;
import com.example.tomcat.port.in.GestionarFacturaUseCase;
import com.example.tomcat.security.UsuarioAutenticado;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/facturas")
public class FacturaController {

    private final GestionarFacturaUseCase gestionarFacturaUseCase;
    private final FacturaHtmlRenderer facturaHtmlRenderer;

    public FacturaController(GestionarFacturaUseCase gestionarFacturaUseCase, FacturaHtmlRenderer facturaHtmlRenderer) {
        this.gestionarFacturaUseCase = gestionarFacturaUseCase;
        this.facturaHtmlRenderer = facturaHtmlRenderer;
    }

    @GetMapping
    public ResponseEntity<List<Factura>> listar(@AuthenticationPrincipal UsuarioAutenticado usuario,
                                                @RequestParam(required = false) Long clienteId) {
        return ResponseEntity.ok(gestionarFacturaUseCase.listar(usuario.solicitante(), clienteId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Factura> obtener(@AuthenticationPrincipal UsuarioAutenticado usuario, @PathVariable Long id) {
        return ResponseEntity.ok(gestionarFacturaUseCase.buscarPorId(usuario.solicitante(), id));
    }

    @GetMapping("/pago/{pagoId}")
    public ResponseEntity<Factura> porPago(@AuthenticationPrincipal UsuarioAutenticado usuario, @PathVariable Long pagoId) {
        return ResponseEntity.ok(gestionarFacturaUseCase.buscarPorPago(usuario.solicitante(), pagoId));
    }

    @GetMapping("/reserva/{reservaId}")
    public ResponseEntity<Factura> porReserva(@AuthenticationPrincipal UsuarioAutenticado usuario,
                                              @PathVariable Long reservaId) {
        return ResponseEntity.ok(gestionarFacturaUseCase.buscarPorReserva(usuario.solicitante(), reservaId));
    }

    @GetMapping(value = "/{id}/descargar", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<byte[]> descargar(@AuthenticationPrincipal UsuarioAutenticado usuario, @PathVariable Long id) {
        Factura factura = gestionarFacturaUseCase.buscarPorId(usuario.solicitante(), id);
        byte[] contenido = facturaHtmlRenderer.render(factura).getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + factura.getNumero() + ".html")
                .contentType(MediaType.TEXT_HTML)
                .body(contenido);
    }
}
