package com.example.tomcat.adapter.in.rest;

import com.example.tomcat.port.in.ConsultarReportesUseCase;
import com.example.tomcat.port.in.dto.ReporteClientesFrecuentes;
import com.example.tomcat.port.in.dto.ReporteDisponibilidad;
import com.example.tomcat.port.in.dto.ReporteHerramientasMasAlquiladas;
import com.example.tomcat.port.in.dto.ReporteIncidencias;
import com.example.tomcat.port.in.dto.ReporteIngresos;
import com.example.tomcat.port.in.dto.ReporteMultas;
import com.example.tomcat.port.in.dto.ReporteRentabilidad;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Solo ADMIN (ver SecurityConfig). */
@Tag(name = "Reportes")
@ApiResponse(responseCode = "403", description = "Su rol no tiene permiso para realizar esta operación.")
@RestController
@RequestMapping("/api/reportes")
public class ReporteController {

    private final ConsultarReportesUseCase consultarReportesUseCase;

    public ReporteController(ConsultarReportesUseCase consultarReportesUseCase) {
        this.consultarReportesUseCase = consultarReportesUseCase;
    }

    @Operation(summary = "Ingresos por alquileres, multas y daños",
            description = "**Rol:** ADMIN. Pagos COMPLETADO (los reembolsados no cuentan). El depósito de garantía no es "
                    + "ingreso: se informa aparte lo que está en custodia y lo devuelto. totalIngresos = alquileres + multas "
                    + "por retraso + daños cobrados del depósito; saldoPendientePorCobrar = lo que el depósito no alcanzó a cubrir.")
    @ApiResponse(responseCode = "200", description = "Reporte de ingresos")
    @GetMapping("/ingresos")
    public ResponseEntity<ReporteIngresos> ingresos() {
        return ResponseEntity.ok(consultarReportesUseCase.ingresos());
    }

    @Operation(summary = "Herramientas más alquiladas",
            description = "**Rol:** ADMIN. Todas las herramientas ordenadas por número de alquileres (sin contar reservas "
                    + "rechazadas, vencidas ni canceladas) con los ingresos de cada una.")
    @ApiResponse(responseCode = "200", description = "Ranking de herramientas")
    @GetMapping("/herramientas-mas-alquiladas")
    public ResponseEntity<ReporteHerramientasMasAlquiladas> herramientasMasAlquiladas() {
        return ResponseEntity.ok(consultarReportesUseCase.herramientasMasAlquiladas());
    }

    @Operation(summary = "Clientes frecuentes",
            description = "**Rol:** ADMIN. Clientes ordenados por número de reservas, con el total que han pagado.")
    @ApiResponse(responseCode = "200", description = "Ranking de clientes")
    @GetMapping("/clientes-frecuentes")
    public ResponseEntity<ReporteClientesFrecuentes> clientesFrecuentes() {
        return ResponseEntity.ok(consultarReportesUseCase.clientesFrecuentes());
    }

    @Operation(summary = "Disponibilidad y estado de las herramientas",
            description = "**Rol:** ADMIN. Totales por estado (disponibles, en mantenimiento, fuera de servicio) y el detalle de cada herramienta.")
    @ApiResponse(responseCode = "200", description = "Estado del inventario")
    @GetMapping("/disponibilidad")
    public ResponseEntity<ReporteDisponibilidad> disponibilidad() {
        return ResponseEntity.ok(consultarReportesUseCase.disponibilidad());
    }

    /** Ingresos menos costo de mantenimiento, por herramienta y global. */
    @Operation(summary = "Rentabilidad",
            description = "**Rol:** ADMIN. Por herramienta y global: ingresos de alquileres pagados − costo de los "
                    + "mantenimientos = utilidad, y el margen (utilidad / ingresos × 100; null si no hubo ingresos). "
                    + "Ordenado de mayor a menor utilidad.")
    @ApiResponse(responseCode = "200", description = "Reporte de rentabilidad")
    @GetMapping("/rentabilidad")
    public ResponseEntity<ReporteRentabilidad> rentabilidad() {
        return ResponseEntity.ok(consultarReportesUseCase.rentabilidad());
    }

    /** Devoluciones con equipo dañado o perdido y el mantenimiento asociado. */
    @Operation(summary = "Incidencias con equipos",
            description = "**Rol:** ADMIN. Devoluciones con equipo DANADO o PERDIDO, el estado actual de la herramienta y "
                    + "el mantenimiento que generaron (abierto o cerrado, con su costo). Más recientes primero.")
    @ApiResponse(responseCode = "200", description = "Incidencias registradas")
    @GetMapping("/incidencias")
    public ResponseEntity<ReporteIncidencias> incidencias() {
        return ResponseEntity.ok(consultarReportesUseCase.incidencias());
    }

    @Operation(summary = "Multas por devolución tardía",
            description = "**Rol:** ADMIN. Devoluciones registradas después de la fecha fin: días de retraso, multa "
                    + "(tarifa + recargo por día), cuánto se descontó del depósito y el saldo pendiente. Más recientes primero.")
    @ApiResponse(responseCode = "200", description = "Devoluciones tardías y sus multas")
    @GetMapping("/multas")
    public ResponseEntity<ReporteMultas> multas() {
        return ResponseEntity.ok(consultarReportesUseCase.multas());
    }
}
