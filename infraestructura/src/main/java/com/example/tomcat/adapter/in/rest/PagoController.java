package com.example.tomcat.adapter.in.rest;

import com.example.tomcat.model.Pago;
import com.example.tomcat.port.in.ListarPagosUseCase;
import com.example.tomcat.port.in.ProcesarPagoUseCase;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<Pago> pagar(@RequestBody PagoRequest request) {
        return ResponseEntity.ok(procesarPagoUseCase.procesar(request.reservaId(), request.monto(), request.metodoPago()));
    }

    @GetMapping
    public ResponseEntity<List<Pago>> listar(@RequestParam(required = false) Long reservaId) {
        if (reservaId != null) {
            return ResponseEntity.ok(listarPagosUseCase.historialPorReserva(reservaId));
        }
        return ResponseEntity.ok(listarPagosUseCase.listar());
    }

    @GetMapping("/reserva/{reservaId}")
    public ResponseEntity<List<Pago>> historialPorReserva(@PathVariable Long reservaId) {
        return ResponseEntity.ok(listarPagosUseCase.historialPorReserva(reservaId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Pago> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(listarPagosUseCase.buscarPorId(id));
    }

    public record PagoRequest(Long reservaId, BigDecimal monto, String metodoPago) {}
}
