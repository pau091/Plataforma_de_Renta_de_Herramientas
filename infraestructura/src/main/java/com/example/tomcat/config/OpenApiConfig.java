package com.example.tomcat.config;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.MapSchema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.tags.Tag;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Documentación Swagger / OpenAPI (http://localhost:8080/swagger-ui/index.html).
 * <p>
 * Los modelos del dominio no dependen de Swagger (arquitectura hexagonal), así que sus ejemplos JSON se
 * agregan aquí con un {@link OpenApiCustomizer}. Las respuestas de error que los controllers declaran solo
 * con su descripción se completan con el esquema {@code Error} y un ejemplo cuyo mensaje es esa descripción.
 */
@Configuration
public class OpenApiConfig {

    public static final String BEARER = "bearerAuth";
    public static final String ERROR = "#/components/schemas/Error";

    private static final String DESCRIPCION = """
            API REST de la **Plataforma de Renta de Herramientas y Equipos de Construcción**: los proveedores publican \
            su inventario y los clientes reservan, pagan y descargan su factura. El administrador supervisa usuarios, \
            pagos, incidencias y reportes.

            ## Autenticación con JWT
            1. Inicie sesión con `POST /api/auth/login` (o registre un cliente con `POST /api/auth/registro`).
            2. Copie el campo `token` de la respuesta.
            3. Pulse **Authorize** (arriba a la derecha), pegue el token **sin** la palabra `Bearer` y confirme.
            4. Swagger enviará en cada petición el encabezado `Authorization: Bearer <token>`.

            El token se firma con HS512 y dura **24 horas**. Contiene el correo (`sub`), el `rol` y el `usuarioId`. \
            En cada petición el servidor verifica la firma y además consulta la base de datos: si el usuario fue \
            desactivado o eliminado, el token deja de servir aunque no haya expirado. El rol efectivo es el de la base de datos.

            ## Roles
            | Rol | Puede |
            |---|---|
            | **ADMIN** | Todo: gestionar proveedores, clientes, categorías y herramientas, ver todas las reservas y pagos, reembolsar pagos, reportes e incidencias. El **administrador principal** (admin@rentas.com) es el único que cambia roles (`PATCH /api/usuarios/{id}/rol`). |
            | **PROVEEDOR** | Gestionar **sus** herramientas (con imágenes), aceptar o rechazar solicitudes, registrar entregas y devoluciones con daños, ver pagos y facturas de sus herramientas y gestionar sus mantenimientos. También puede **alquilar herramientas de otros proveedores** como un cliente. |
            | **CLIENTE** | Explorar el catálogo, reservar, modificar o cancelar sus reservas pendientes, pagar, descargar facturas y ver su historial. |

            Las reglas de propiedad se aplican siempre: nunca se confía en ids de cliente o proveedor enviados en la \
            petición cuando quien llama es un CLIENTE o un PROVEEDOR.

            ## Flujo de una reserva
            `PENDIENTE` → (proveedor acepta) `ACEPTADA` → (cliente paga: **confirmación automática** + factura) `CONFIRMADA` \
            → (proveedor entrega) `ENTREGADA` → (devolución) `FINALIZADA`. También: `RECHAZADA`, `CANCELADA` y `VENCIDA` \
            (si el proveedor no acepta o el cliente no paga dentro de **1 hora**, o si llega la fecha de inicio).

            ## Errores
            Todas las respuestas de error tienen el formato del esquema `Error`. Los errores de validación agregan \
            `errores` con el mensaje de cada campo.
            `400` datos inválidos o regla de negocio · `401` sin token, token inválido o vencido · `403` el rol o el \
            usuario no tiene permiso · `404` no existe · `409` conflicto (fechas cruzadas, duplicados, historial) · \
            `413` imagen de más de 5 MB.

            ## Cuentas de prueba (clave `Rentas2026*`)
            `admin@rentas.com` (ADMIN) · `carlos.proveedor@gmail.com` (PROVEEDOR, herramientas 1 y 2) · \
            `juan.cliente@gmail.com` (CLIENTE) · `gabriel.mantenimiento@gmail.com` (CLIENTE **inactivo**: login 403).

            Fechas en formato ISO-8601: días `yyyy-MM-dd` (zona del negocio America/Bogota) e instantes `yyyy-MM-ddTHH:mm:ss` en UTC.
            """;

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Plataforma de Renta de Herramientas - API")
                        .version("1.0.0")
                        .description(DESCRIPCION))
                .tags(List.of(
                        new Tag().name("Autenticación").description("Registro de clientes e inicio de sesión (públicos). Devuelven el token JWT."),
                        new Tag().name("Usuarios").description("Alta, edición, desactivación y baja de proveedores y clientes. Solo ADMIN."),
                        new Tag().name("Categorías").description("Clasificación de las herramientas. Consulta: cualquier rol. Escritura: ADMIN."),
                        new Tag().name("Herramientas").description("Catálogo con filtros, inventario del proveedor, estado e imágenes."),
                        new Tag().name("Reservas").description("Reservas por fechas, disponibilidad, aceptación, entrega, devolución, cancelación y vencimiento."),
                        new Tag().name("Pagos").description("Pago en línea con confirmación automática de la reserva, historial y reembolsos."),
                        new Tag().name("Facturas").description("Facturas generadas automáticamente con cada pago y su descarga."),
                        new Tag().name("Mantenimientos").description("Reparaciones de herramientas, incluidas las abiertas por daños en devoluciones."),
                        new Tag().name("Reportes").description("Ingresos, rentabilidad, herramientas más alquiladas, clientes frecuentes, disponibilidad e incidencias. Solo ADMIN."),
                        new Tag().name("Panel").description("Resumen personalizado según el rol del usuario autenticado.")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER))
                .components(new Components()
                        .addSecuritySchemes(BEARER, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Token devuelto por `POST /api/auth/login`. Se envía como `Authorization: Bearer <token>`.")));
    }

    @Bean
    public OpenApiCustomizer ejemplosYErrores(ObjectMapper objectMapper) {
        return openApi -> {
            openApi.getComponents().addSchemas("Error", esquemaError());
            Map<String, Schema> schemas = openApi.getComponents().getSchemas();
            EJEMPLOS.forEach((nombre, json) -> {
                Schema<?> schema = schemas.get(nombre);
                if (schema != null) {
                    schema.setExample(leer(objectMapper, json));
                }
            });

            openApi.getPaths().forEach((ruta, item) -> item.readOperations().forEach(operacion -> {
                if (ruta.startsWith("/api/auth/")) {
                    operacion.setSecurity(List.of()); // públicos: sin candado
                } else {
                    operacion.getResponses().putIfAbsent("401", new ApiResponse().description(
                            "Debe iniciar sesión y enviar el token en el encabezado 'Authorization: Bearer <token>'."));
                }
                completarErrores(operacion);
            }));

            // GET /api/panel: un ejemplo por rol.
            var panel = openApi.getPaths().get("/api/panel");
            ApiResponse panelOk = panel == null || panel.getGet() == null ? null : panel.getGet().getResponses().get("200");
            if (panelOk != null && panelOk.getContent() != null) {
                panelOk.getContent().values().forEach(media -> media
                        .addExamples("CLIENTE", new Example().summary("Panel de un cliente").value(leer(objectMapper, PANEL_CLIENTE)))
                        .addExamples("PROVEEDOR", new Example().summary("Panel de un proveedor").value(leer(objectMapper, PANEL_PROVEEDOR)))
                        .addExamples("ADMIN", new Example().summary("Panel del administrador").value(leer(objectMapper, PANEL_ADMIN))));
            }
        };
    }

    /**
     * Las respuestas de error se declaran en los controllers solo con su descripción (springdoc les pone el
     * esquema de la respuesta exitosa): se cambian por el esquema Error con un ejemplo cuyo mensaje es esa
     * descripción. Además se ordenan por código.
     */
    private static void completarErrores(Operation operacion) {
        ApiResponses ordenadas = new ApiResponses();
        operacion.getResponses().entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(e -> ordenadas.addApiResponse(e.getKey(), e.getValue()));
        operacion.setResponses(ordenadas);

        ordenadas.forEach((codigo, respuesta) -> {
            int status = Integer.parseInt(codigo);
            if (status < 400 || respuesta.get$ref() != null) {
                return;
            }
            Map<String, Object> ejemplo = new LinkedHashMap<>();
            ejemplo.put("timestamp", "2026-10-17T15:30:00Z");
            ejemplo.put("status", status);
            ejemplo.put("error", HttpStatus.valueOf(status).getReasonPhrase());
            ejemplo.put("message", respuesta.getDescription());
            respuesta.setContent(new Content().addMediaType("application/json",
                    new MediaType().schema(new Schema<>().$ref(ERROR)).example(ejemplo)));
        });
    }

    private static Schema<?> esquemaError() {
        return new ObjectSchema()
                .description("Formato común de todas las respuestas de error.")
                .addProperty("timestamp", new StringSchema().example("2026-10-17T15:30:00Z").description("Instante del error (UTC)"))
                .addProperty("status", new IntegerSchema().example(400).description("Código HTTP"))
                .addProperty("error", new StringSchema().example("Bad Request").description("Nombre del código HTTP"))
                .addProperty("message", new StringSchema().example("La petición tiene datos inválidos.").description("Explicación en español"))
                .addProperty("errores", new MapSchema().additionalProperties(new StringSchema())
                        .description("Solo en errores de validación: mensaje por cada campo inválido")
                        .example(Map.of("fechaFin", "La fecha fin es obligatoria.")));
    }

    private static Object leer(ObjectMapper objectMapper, String json) {
        try {
            return objectMapper.readValue(json, Object.class);
        } catch (JacksonException e) {
            throw new IllegalStateException("Ejemplo JSON inválido en la documentación: " + json, e);
        }
    }

    // ------------------------------------------------------------------ ejemplos (tomados de respuestas reales)

    private static final Map<String, String> EJEMPLOS = Map.ofEntries(
            Map.entry("Categoria", """
                    {"id": 1, "nombre": "Construcción Pesada", "descripcion": "Maquinaria de gran escala para obras civiles"}"""),
            Map.entry("Herramienta", """
                    {"id": 1, "proveedorId": 2, "categoriaId": 1, "nombre": "Taladro Percutor Industrial",
                     "descripcion": "Taladro de alta potencia 800W con maletín", "tarifaDiaria": 45000.0,
                     "estado": "DISPONIBLE", "imagenUrl": "/uploads/herramientas/3f2b9c1e-7a4d-4c8e-9b0a-1d2e3f4a5b6c.jpg"}"""),
            Map.entry("Reserva", """
                    {"id": 7, "clienteId": 18, "herramientaId": 7, "fechaInicio": "2026-10-16", "fechaFin": "2026-10-21",
                     "estado": "ENTREGADA", "fechaEntrega": "2026-10-16T08:00:00", "fechaDevolucion": null,
                     "estadoDevolucion": null, "observacionesDevolucion": null,
                     "fechaCreacion": "2026-10-13T09:00:00", "fechaAceptacion": "2026-10-13T09:30:00"}"""),
            Map.entry("Pago", """
                    {"id": 1, "reservaId": 1, "monto": 225000.0, "metodoPago": "TARJETA", "estadoPago": "COMPLETADO",
                     "fechaPago": "2026-09-05T09:50:00"}"""),
            Map.entry("Factura", """
                    {"id": 1, "numero": "FAC-000001", "pagoId": 1, "reservaId": 1, "clienteId": 12,
                     "clienteNombre": "Juan Cliente", "clienteEmail": "juan.cliente@gmail.com", "herramientaId": 1,
                     "herramientaNombre": "Taladro Percutor Industrial", "fechaInicio": "2026-09-08", "fechaFin": "2026-09-12",
                     "dias": 5, "tarifaDiaria": 45000.0, "subtotal": 225000.0, "total": 225000.0, "metodoPago": "TARJETA",
                     "fechaEmision": "2026-09-05T09:50:00", "estado": "VIGENTE"}"""),
            Map.entry("Mantenimiento", """
                    {"id": 1, "herramientaId": 4, "reservaId": 4,
                     "descripcionProblema": "Daño reportado en devolución de reserva 4: La placa compactadora llegó con la base fisurada",
                     "costoReparacion": 80000.0, "fechaInicio": "2026-09-30T18:00:00", "fechaFin": "2026-10-01T17:00:00"}"""),
            Map.entry("ReporteIngresos", """
                    {"totalIngresos": 8473000.0, "pagosCompletados": 23, "porMetodo": [
                      {"metodoPago": "TARJETA", "cantidad": 10, "total": 5588000.0},
                      {"metodoPago": "TRANSFERENCIA", "cantidad": 7, "total": 2125000.0},
                      {"metodoPago": "EFECTIVO", "cantidad": 6, "total": 760000.0}]}"""),
            Map.entry("ReporteHerramientasMasAlquiladas", """
                    {"items": [{"herramientaId": 1, "nombre": "Taladro Percutor Industrial", "estado": "DISPONIBLE",
                                "alquileres": 2, "ingresos": 405000.0}]}"""),
            Map.entry("ReporteClientesFrecuentes", """
                    {"items": [{"clienteId": 12, "nombre": "Juan Cliente", "email": "juan.cliente@gmail.com",
                                "reservas": 4, "totalPagado": 795000.0}]}"""),
            Map.entry("ReporteDisponibilidad", """
                    {"total": 30, "disponibles": 28, "enMantenimiento": 1, "fueraDeServicio": 1, "items": [
                      {"id": 1, "nombre": "Taladro Percutor Industrial", "estado": "DISPONIBLE", "categoriaId": 1,
                       "proveedorId": 2, "operativa": true}]}"""),
            Map.entry("ReporteRentabilidad", """
                    {"ingresosTotales": 8473000.0, "costoMantenimientoTotal": 460000.0, "utilidadTotal": 8013000.0,
                     "margenPorcentaje": 94.57, "items": [
                      {"herramientaId": 4, "nombre": "Compactador tipo Rana", "proveedorId": 3, "proveedorNombre": "Maria Proveadora",
                       "alquileresPagados": 1, "ingresos": 285000.0, "costoMantenimiento": 80000.0, "utilidad": 205000.0,
                       "margenPorcentaje": 71.93}]}"""),
            Map.entry("ReporteIncidencias", """
                    {"total": 4, "danadas": 3, "perdidas": 1, "items": [
                      {"reservaId": 23, "herramientaId": 23, "herramientaNombre": "Minicargador Compacto",
                       "estadoHerramienta": "MANTENIMIENTO", "proveedorId": 24, "clienteId": 34,
                       "clienteNombre": "Tomás Maestro de Obra", "estadoDevolucion": "DANADO",
                       "observaciones": "Fuga hidráulica en el brazo del balde", "fechaDevolucion": "2026-10-14T17:00:00",
                       "mantenimientoId": 21, "mantenimientoAbierto": true, "costoReparacion": null}]}""")
    );

    private static final String PANEL_CLIENTE = """
            {"rol": "CLIENTE", "reservasPorEstado": {"CONFIRMADA": 1, "FINALIZADA": 3},
             "proximasReservas": [{"id": 11, "clienteId": 12, "herramientaId": 11, "fechaInicio": "2026-10-21",
               "fechaFin": "2026-10-23", "estado": "CONFIRMADA", "fechaEntrega": null, "fechaDevolucion": null,
               "estadoDevolucion": null, "observacionesDevolucion": null, "fechaCreacion": "2026-10-16T09:00:00",
               "fechaAceptacion": "2026-10-16T09:30:00"}],
             "totalPagado": 795000.0, "facturas": 4}""";

    private static final String PANEL_PROVEEDOR = """
            {"rol": "PROVEEDOR", "herramientas": 2, "herramientasPorEstado": {"DISPONIBLE": 2},
             "reservasPorEstado": {"PENDIENTE": 1, "FINALIZADA": 3},
             "solicitudesPendientes": [{"id": 35, "clienteId": 13, "herramientaId": 1, "fechaInicio": "2026-11-02",
               "fechaFin": "2026-11-04", "estado": "PENDIENTE", "fechaEntrega": null, "fechaDevolucion": null,
               "estadoDevolucion": null, "observacionesDevolucion": null, "fechaCreacion": "2026-10-17T15:10:00",
               "fechaAceptacion": null}],
             "ingresos": 480000.0, "mantenimientosAbiertos": 0}""";

    private static final String PANEL_ADMIN = """
            {"rol": "ADMIN", "clientes": 20, "proveedores": 20,
             "herramientasPorEstado": {"DISPONIBLE": 28, "FUERA_DE_SERVICIO": 1, "MANTENIMIENTO": 1},
             "reservasPorEstado": {"ACEPTADA": 2, "CANCELADA": 2, "CONFIRMADA": 4, "ENTREGADA": 3, "FINALIZADA": 16,
                                   "PENDIENTE": 3, "RECHAZADA": 2, "VENCIDA": 2},
             "ingresos": 8473000.0, "pagosCompletados": 23, "mantenimientosAbiertos": 1}""";
}
