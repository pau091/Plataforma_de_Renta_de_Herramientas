package com.example.tomcat.port.in;

import com.example.tomcat.port.in.dto.ReporteClientesFrecuentes;
import com.example.tomcat.port.in.dto.ReporteDisponibilidad;
import com.example.tomcat.port.in.dto.ReporteHerramientasMasAlquiladas;
import com.example.tomcat.port.in.dto.ReporteIncidencias;
import com.example.tomcat.port.in.dto.ReporteIngresos;
import com.example.tomcat.port.in.dto.ReporteRentabilidad;

public interface ConsultarReportesUseCase {
    ReporteIngresos ingresos();
    ReporteHerramientasMasAlquiladas herramientasMasAlquiladas();
    ReporteClientesFrecuentes clientesFrecuentes();
    ReporteDisponibilidad disponibilidad();
    ReporteRentabilidad rentabilidad();
    ReporteIncidencias incidencias();
}
