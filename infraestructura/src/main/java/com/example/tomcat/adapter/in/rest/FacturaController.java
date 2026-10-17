package com.example.tomcat.adapter.in.rest;

import com.example.tomcat.model.Factura;
import com.example.tomcat.port.in.GestionarFacturaUseCase;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<List<Factura>> listar(@RequestParam(required = false) Long clienteId) {
        if (clienteId != null) {
            return ResponseEntity.ok(gestionarFacturaUseCase.listarPorCliente(clienteId));
        }
        return ResponseEntity.ok(gestionarFacturaUseCase.listar());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Factura> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(gestionarFacturaUseCase.buscarPorId(id));
    }

    @GetMapping("/pago/{pagoId}")
    public ResponseEntity<Factura> porPago(@PathVariable Long pagoId) {
        return ResponseEntity.ok(gestionarFacturaUseCase.buscarPorPago(pagoId));
    }

    @GetMapping("/reserva/{reservaId}")
    public ResponseEntity<Factura> porReserva(@PathVariable Long reservaId) {
        return ResponseEntity.ok(gestionarFacturaUseCase.buscarPorReserva(reservaId));
    }

    @GetMapping(value = "/{id}/descargar", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<byte[]> descargar(@PathVariable Long id) {
        Factura factura = gestionarFacturaUseCase.buscarPorId(id);
        byte[] contenido = facturaHtmlRenderer.render(factura).getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + factura.getNumero() + ".html")
                .contentType(MediaType.TEXT_HTML)
                .body(contenido);
    }
}
