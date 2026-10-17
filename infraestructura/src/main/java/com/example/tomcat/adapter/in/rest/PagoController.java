package com.example.tomcat.adapter.in.rest;

import com.example.tomcat.model.Pago;
import com.example.tomcat.port.in.ListarPagosUseCase;
import com.example.tomcat.port.in.ProcesarPagoUseCase;
import com.example.tomcat.security.UsuarioAutenticado;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/pagos")
public class PagoController {

    private final ProcesarPagoUseCase procesarPagoUseCase;
    private final ListarPagosUseCase listarPagosUseCase;

    public PagoController(ProcesarPagoUseCase procesarPagoUseCase, ListarPagosUseCase listarPagosUseCase) {
        this.procesarPagoUseCase = procesarPagoUseCase;
        this.listarPagosUseCase = listarPagosUseCase;
    }

    @PostMapping
    public ResponseEntity<Pago> pagar(@AuthenticationPrincipal UsuarioAutenticado usuario, @RequestBody PagoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(procesarPagoUseCase.procesar(
                usuario.solicitante(), request.reservaId(), request.monto(), request.metodoPago()));
    }

    @GetMapping
    public ResponseEntity<List<Pago>> listar(@AuthenticationPrincipal UsuarioAutenticado usuario,
                                             @RequestParam(required = false) Long reservaId) {
        if (reservaId != null) {
            return ResponseEntity.ok(listarPagosUseCase.historialPorReserva(usuario.solicitante(), reservaId));
        }
        return ResponseEntity.ok(listarPagosUseCase.listar(usuario.solicitante()));
    }

    @GetMapping("/reserva/{reservaId}")
    public ResponseEntity<List<Pago>> historialPorReserva(@AuthenticationPrincipal UsuarioAutenticado usuario,
                                                          @PathVariable Long reservaId) {
        return ResponseEntity.ok(listarPagosUseCase.historialPorReserva(usuario.solicitante(), reservaId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Pago> obtener(@AuthenticationPrincipal UsuarioAutenticado usuario, @PathVariable Long id) {
        return ResponseEntity.ok(listarPagosUseCase.buscarPorId(usuario.solicitante(), id));
    }

    public record PagoRequest(Long reservaId, BigDecimal monto, String metodoPago) {}
}
