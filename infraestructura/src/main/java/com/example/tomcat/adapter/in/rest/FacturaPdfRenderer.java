package com.example.tomcat.adapter.in.rest;

import com.example.tomcat.model.Factura;
import com.example.tomcat.model.Garantia;
import org.openpdf.text.Document;
import org.openpdf.text.Element;
import org.openpdf.text.Font;
import org.openpdf.text.PageSize;
import org.openpdf.text.Paragraph;
import org.openpdf.text.Phrase;
import org.openpdf.text.Rectangle;
import org.openpdf.text.pdf.BaseFont;
import org.openpdf.text.pdf.PdfContentByte;
import org.openpdf.text.pdf.PdfGState;
import org.openpdf.text.pdf.PdfPCell;
import org.openpdf.text.pdf.PdfPTable;
import org.openpdf.text.pdf.PdfWriter;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Genera la factura en PDF con OpenPDF. Es un detalle de presentación del adaptador REST: el dominio y los casos de
 * uso no saben nada del formato, por eso cambiar de HTML a PDF no tocó ninguna otra capa.
 */
@Component
public class FacturaPdfRenderer {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy hh:mm a", Locale.forLanguageTag("es-CO"));
    private static final Color AMARILLO = new Color(0xFF, 0xC1, 0x07);
    private static final Color OSCURO = new Color(0x21, 0x25, 0x29);
    private static final Color GRIS = new Color(0x6C, 0x75, 0x7D);
    private static final Color FONDO = new Color(0xF8, 0xF9, 0xFA);
    private static final Color ROJO = new Color(0xB0, 0x00, 0x20);

    private static final Font TITULO = new Font(Font.HELVETICA, 20, Font.BOLD, OSCURO);
    private static final Font MARCA = new Font(Font.HELVETICA, 14, Font.BOLD, OSCURO);
    private static final Font SUBTITULO = new Font(Font.HELVETICA, 11, Font.BOLD, OSCURO);
    private static final Font NORMAL = new Font(Font.HELVETICA, 10, Font.NORMAL, OSCURO);
    private static final Font NEGRITA = new Font(Font.HELVETICA, 10, Font.BOLD, OSCURO);
    private static final Font PEQUENA = new Font(Font.HELVETICA, 8, Font.NORMAL, GRIS);
    private static final Font ENCABEZADO_TABLA = new Font(Font.HELVETICA, 10, Font.BOLD, OSCURO);
    private static final Font TOTAL = new Font(Font.HELVETICA, 13, Font.BOLD, OSCURO);
    private static final Font ANULADA = new Font(Font.HELVETICA, 11, Font.BOLD, ROJO);

    private final Clock reloj;

    public FacturaPdfRenderer(Clock reloj) {
        this.reloj = reloj;
    }

    public byte[] render(Factura factura) {
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        Document documento = new Document(PageSize.LETTER, 50, 50, 50, 50);
        PdfWriter writer = PdfWriter.getInstance(documento, salida);
        documento.addTitle("Factura " + factura.getNumero());
        documento.addAuthor("Plataforma de Renta de Herramientas");
        documento.open();

        documento.add(encabezado(factura));
        if (factura.estaAnulada()) {
            Paragraph aviso = new Paragraph("FACTURA ANULADA: el pago fue reembolsado.", ANULADA);
            aviso.setSpacingBefore(12);
            documento.add(aviso);
        }
        documento.add(cliente(factura));
        documento.add(detalle(factura));
        documento.add(totales(factura));
        Garantia garantia = factura.getGarantia();
        if (garantia.liquidada() || garantia.pendienteReparacion()) {
            documento.add(liquidacion(garantia));
        }

        Paragraph pie = new Paragraph("Comprobante generado automáticamente al confirmar el pago de la reserva #"
                + factura.getReservaId() + ". Las fechas del alquiler incluyen el día de inicio y el de fin.", PEQUENA);
        pie.setSpacingBefore(30);
        documento.add(pie);

        if (factura.estaAnulada()) {
            marcaDeAgua(writer.getDirectContentUnder(), documento.getPageSize());
        }
        documento.close();
        return salida.toByteArray();
    }

    private PdfPTable encabezado(Factura factura) {
        PdfPTable tabla = new PdfPTable(new float[]{3, 2});
        tabla.setWidthPercentage(100);

        PdfPCell marca = contenedor();
        marca.addElement(new Paragraph("Plataforma de Renta de Herramientas", MARCA));
        marca.addElement(new Paragraph("Herramientas y equipos de construcción", PEQUENA));
        tabla.addCell(marca);

        PdfPCell datos = contenedor();
        Paragraph titulo = new Paragraph("FACTURA", TITULO);
        titulo.setAlignment(Element.ALIGN_RIGHT);
        datos.addElement(titulo);
        datos.addElement(derecha(new Phrase("No. " + factura.getNumero(), NEGRITA)));
        datos.addElement(derecha(new Phrase("Emisión: " + fechaEmision(factura), NORMAL)));
        datos.addElement(derecha(new Phrase("Estado: " + factura.getEstado(), factura.estaAnulada() ? ANULADA : NORMAL)));
        tabla.addCell(datos);

        // Franja amarilla bajo el encabezado, el color de la marca en la página web.
        PdfPCell franja = new PdfPCell();
        franja.setColspan(2);
        franja.setFixedHeight(4);
        franja.setBackgroundColor(AMARILLO);
        franja.setBorder(Rectangle.NO_BORDER);
        tabla.addCell(franja);
        return tabla;
    }

    private Paragraph cliente(Factura factura) {
        Paragraph bloque = new Paragraph();
        bloque.setSpacingBefore(18);
        bloque.add(new Paragraph("Facturado a", SUBTITULO));
        bloque.add(new Paragraph(factura.getClienteNombre(), NORMAL));
        bloque.add(new Paragraph(factura.getClienteEmail(), NORMAL));
        return bloque;
    }

    private PdfPTable detalle(Factura factura) {
        PdfPTable tabla = new PdfPTable(new float[]{4, 3, 1.2f, 2, 2});
        tabla.setWidthPercentage(100);
        tabla.setSpacingBefore(18);
        tabla.setHeaderRows(1);
        for (String titulo : new String[]{"Herramienta", "Periodo", "Días", "Tarifa diaria", "Subtotal"}) {
            PdfPCell celda = celda(new Phrase(titulo, ENCABEZADO_TABLA));
            celda.setBackgroundColor(FONDO);
            tabla.addCell(celda);
        }
        tabla.addCell(celda(new Phrase(factura.getHerramientaNombre(), NORMAL)));
        tabla.addCell(celda(new Phrase(factura.getFechaInicio().format(FECHA) + " a " + factura.getFechaFin().format(FECHA), NORMAL)));
        tabla.addCell(numero(new Phrase(String.valueOf(factura.getDias()), NORMAL)));
        tabla.addCell(numero(new Phrase(pesos(factura.getTarifaDiaria()), NORMAL)));
        tabla.addCell(numero(new Phrase(pesos(factura.getSubtotal()), NORMAL)));
        return tabla;
    }

    private PdfPTable totales(Factura factura) {
        PdfPTable tabla = new PdfPTable(new float[]{1.3f, 1.7f});
        tabla.setWidthPercentage(55);
        tabla.setHorizontalAlignment(Element.ALIGN_RIGHT);
        tabla.setSpacingBefore(12);
        tabla.addCell(sinBorde(new Phrase("Alquiler", NORMAL)));
        tabla.addCell(derechaSinBorde(new Phrase(pesos(factura.getSubtotal()), NORMAL)));
        if (factura.getGarantia().deposito().signum() > 0) {
            tabla.addCell(sinBorde(new Phrase("Depósito de garantía", NORMAL)));
            tabla.addCell(derechaSinBorde(new Phrase(pesos(factura.getGarantia().deposito()), NORMAL)));
        }
        tabla.addCell(sinBorde(new Phrase("Método de pago", NORMAL)));
        tabla.addCell(derechaSinBorde(new Phrase(factura.getMetodoPago(), NORMAL)));

        PdfPCell etiqueta = sinBorde(new Phrase(factura.estaAnulada() ? "Total (reembolsado)" : "Total pagado", TOTAL));
        PdfPCell valor = derechaSinBorde(new Phrase(pesos(factura.getTotal()), TOTAL));
        for (PdfPCell celda : new PdfPCell[]{etiqueta, valor}) {
            celda.setBorder(Rectangle.TOP);
            celda.setBorderColor(AMARILLO);
            celda.setBorderWidth(2);
            celda.setPaddingTop(6);
            tabla.addCell(celda);
        }
        return tabla;
    }

    /** Lo que pasó con el depósito al devolver: multa por retraso, daños, lo devuelto y lo que falta pagar. */
    private PdfPTable liquidacion(Garantia g) {
        PdfPTable tabla = new PdfPTable(new float[]{1.3f, 1.7f});
        tabla.setWidthPercentage(55);
        tabla.setHorizontalAlignment(Element.ALIGN_RIGHT);
        tabla.setSpacingBefore(18);
        PdfPCell titulo = sinBorde(new Phrase("Liquidación del depósito", SUBTITULO));
        titulo.setColspan(2);
        tabla.addCell(titulo);
        fila(tabla, "Estado", ESTADOS_DEPOSITO.getOrDefault(g.estado(), g.estado()), NORMAL);
        if (g.diasRetraso() > 0) {
            fila(tabla, "Devolución tardía", g.diasRetraso() + (g.diasRetraso() == 1 ? " día" : " días") + " de retraso", ANULADA);
            fila(tabla, "Multa por retraso", pesos(g.multaRetraso()), ANULADA);
        }
        if (g.cargoDanos().signum() > 0) {
            fila(tabla, "Daños / pérdida", pesos(g.cargoDanos()), NORMAL);
        }
        if (g.pendienteReparacion()) {
            fila(tabla, "Daños", "pendiente del costo de reparación", NORMAL);
        } else {
            fila(tabla, "Depósito devuelto", pesos(g.montoDevuelto()), NEGRITA);
        }
        if (g.saldoPendiente().signum() > 0) {
            fila(tabla, "Saldo pendiente a cargo del cliente", pesos(g.saldoPendiente()), ANULADA);
        }
        return tabla;
    }

    private static final java.util.Map<String, String> ESTADOS_DEPOSITO = java.util.Map.of(
            "DEVUELTO", "Devuelto completo", "DEVUELTO_PARCIAL", "Devuelto en parte", "NO_DEVUELTO", "No devuelto",
            "PENDIENTE_REPARACION", "Pendiente de reparación", "LIQUIDADO", "Sin depósito", "REEMBOLSADO", "Reembolsado con el pago");

    private static void fila(PdfPTable tabla, String etiqueta, String valor, Font fuente) {
        tabla.addCell(sinBorde(new Phrase(etiqueta, NORMAL)));
        tabla.addCell(derechaSinBorde(new Phrase(valor, fuente)));
    }

    private void marcaDeAgua(PdfContentByte lienzo, Rectangle pagina) {
        try {
            BaseFont fuente = BaseFont.createFont(BaseFont.HELVETICA_BOLD, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED);
            PdfGState transparencia = new PdfGState();
            transparencia.setFillOpacity(0.12f);
            lienzo.saveState();
            lienzo.setGState(transparencia);
            lienzo.beginText();
            lienzo.setColorFill(ROJO);
            lienzo.setFontAndSize(fuente, 90);
            lienzo.showTextAligned(Element.ALIGN_CENTER, "ANULADA", pagina.getWidth() / 2, pagina.getHeight() / 2, 45);
            lienzo.endText();
            lienzo.restoreState();
        } catch (java.io.IOException e) {
            throw new IllegalStateException("No se pudo cargar la fuente de la marca de agua.", e);
        }
    }

    /** La fecha se guarda en UTC; se muestra en la zona horaria del negocio (app.zona-horaria). */
    private String fechaEmision(Factura factura) {
        LocalDateTime utc = factura.getFechaEmision();
        return utc == null ? "" : utc.atOffset(ZoneOffset.UTC).atZoneSameInstant(reloj.getZone()).format(FECHA_HORA);
    }

    private static String pesos(BigDecimal valor) {
        DecimalFormatSymbols simbolos = new DecimalFormatSymbols(Locale.forLanguageTag("es-CO"));
        simbolos.setGroupingSeparator('.');
        simbolos.setDecimalSeparator(',');
        DecimalFormat formato = new DecimalFormat("$ #,##0", simbolos);
        return formato.format(valor) + " COP";
    }

    private static PdfPCell celda(Phrase contenido) {
        PdfPCell celda = new PdfPCell(contenido);
        celda.setPadding(7);
        celda.setBorderColor(new Color(0xDE, 0xE2, 0xE6));
        return celda;
    }

    private static PdfPCell numero(Phrase contenido) {
        PdfPCell celda = celda(contenido);
        celda.setHorizontalAlignment(Element.ALIGN_RIGHT);
        return celda;
    }

    /** Celda sin borde para apilar varios párrafos (modo compuesto de OpenPDF). */
    private static PdfPCell contenedor() {
        PdfPCell celda = new PdfPCell();
        celda.setBorder(Rectangle.NO_BORDER);
        celda.setPaddingBottom(8);
        return celda;
    }

    private static PdfPCell sinBorde(Phrase contenido) {
        PdfPCell celda = new PdfPCell(contenido);
        celda.setBorder(Rectangle.NO_BORDER);
        celda.setPadding(3);
        return celda;
    }

    private static PdfPCell derechaSinBorde(Phrase contenido) {
        PdfPCell celda = sinBorde(contenido);
        celda.setHorizontalAlignment(Element.ALIGN_RIGHT);
        return celda;
    }

    private static Paragraph derecha(Phrase contenido) {
        Paragraph parrafo = new Paragraph(contenido);
        parrafo.setAlignment(Element.ALIGN_RIGHT);
        return parrafo;
    }
}
