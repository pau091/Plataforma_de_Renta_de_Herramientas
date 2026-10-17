package com.example.tomcat.adapter.in.rest;

import com.example.tomcat.port.in.ConsultarReportesUseCase;
import com.example.tomcat.port.in.dto.ReporteClientesFrecuentes;
import com.example.tomcat.port.in.dto.ReporteDisponibilidad;
import com.example.tomcat.port.in.dto.ReporteHerramientasMasAlquiladas;
import com.example.tomcat.port.in.dto.ReporteIngresos;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reportes")
public class ReporteController {

    private final ConsultarReportesUseCase consultarReportesUseCase;

    public ReporteController(ConsultarReportesUseCase consultarReportesUseCase) {
        this.consultarReportesUseCase = consultarReportesUseCase;
    }

    @GetMapping("/ingresos")
    public ResponseEntity<ReporteIngresos> ingresos() {
        return ResponseEntity.ok(consultarReportesUseCase.ingresos());
    }

    @GetMapping("/herramientas-mas-alquiladas")
    public ResponseEntity<ReporteHerramientasMasAlquiladas> herramientasMasAlquiladas() {
        return ResponseEntity.ok(consultarReportesUseCase.herramientasMasAlquiladas());
    }

    @GetMapping("/clientes-frecuentes")
    public ResponseEntity<ReporteClientesFrecuentes> clientesFrecuentes() {
        return ResponseEntity.ok(consultarReportesUseCase.clientesFrecuentes());
    }

    @GetMapping("/disponibilidad")
    public ResponseEntity<ReporteDisponibilidad> disponibilidad() {
        return ResponseEntity.ok(consultarReportesUseCase.disponibilidad());
    }
}
