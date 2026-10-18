package com.example.tomcat.adapter.in.rest;

import com.example.tomcat.model.Factura;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;

@Component
public class FacturaHtmlRenderer {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public String render(Factura factura) {
        return """
                <!DOCTYPE html>
                <html lang="es">
                <head>
                    <meta charset="UTF-8">
                    <title>%s</title>
                    <style>
                        body { font-family: Arial, sans-serif; margin: 32px; color: #222; }
                        h1 { margin-bottom: 4px; }
                        .muted { color: #666; }
                        table { width: 100%%; border-collapse: collapse; margin-top: 24px; }
                        th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }
                        th { background: #f4f4f4; }
                        .total { font-size: 1.2rem; font-weight: bold; }
                        .anulada { color: #b00020; border: 2px solid #b00020; padding: 8px; display: inline-block; }
                    </style>
                </head>
                <body>
                    <h1>Comprobante de pago</h1>
                    <p class="muted">Plataforma de Renta de Herramientas</p>
                    %s
                    <p><strong>Factura:</strong> %s<br>
                    <strong>Fecha de emisión:</strong> %s</p>
                    <h3>Cliente</h3>
                    <p>%s<br>%s</p>
                    <table>
                        <thead>
                            <tr><th>Herramienta</th><th>Periodo</th><th>Días</th><th>Tarifa diaria</th><th>Subtotal</th></tr>
                        </thead>
                        <tbody>
                            <tr>
                                <td>%s</td>
                                <td>%s a %s</td>
                                <td>%d</td>
                                <td>$%s</td>
                                <td>$%s</td>
                            </tr>
                        </tbody>
                    </table>
                    <p>Método de pago: %s</p>
                    <p class="total">Total pagado: $%s</p>
                </body>
                </html>
                """.formatted(
                factura.getNumero(),
                factura.estaAnulada() ? "<p class=\"anulada\"><strong>FACTURA ANULADA:</strong> el pago fue reembolsado.</p>" : "",
                factura.getNumero(),
                factura.getFechaEmision() == null ? "" : factura.getFechaEmision().format(FECHA_HORA),
                factura.getClienteNombre(),
                factura.getClienteEmail(),
                factura.getHerramientaNombre(),
                factura.getFechaInicio().format(FECHA),
                factura.getFechaFin().format(FECHA),
                factura.getDias(),
                factura.getTarifaDiaria(),
                factura.getSubtotal(),
                factura.getMetodoPago(),
                factura.getTotal()
        );
    }
}
