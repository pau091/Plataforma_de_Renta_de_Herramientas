package com.example.tomcat;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.AbstractMockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.file.Files;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/**
 * Base de las pruebas de integración: la aplicación completa (seguridad JWT, controllers, JPA y Flyway V1–V12)
 * contra un MySQL 8.4 real en Docker. Un solo contenedor y un solo contexto de Spring se comparten entre todas
 * las clases *IT; cada prueba crea sus propios datos para no depender de las demás.
 * Si Docker no está disponible, las pruebas se omiten (no fallan).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
public abstract class IntegracionBase {

    protected static final String CLAVE = "Rentas2026*";
    protected static final String ADMIN = "admin@rentas.com";
    protected static final String PROVEEDOR = "carlos.proveedor@gmail.com";   // id 2: herramientas 1 y 2
    protected static final String OTRO_PROVEEDOR = "maria.equipos@gmail.com"; // id 3
    protected static final String CLIENTE = "juan.cliente@gmail.com";         // id 12
    protected static final String OTRO_CLIENTE = "ana.rentas@gmail.com";      // id 13
    protected static final String INACTIVO = "gabriel.mantenimiento@gmail.com";

    private static MySQLContainer<?> mysql;
    private static String uploads;
    private static final Map<String, String> TOKENS = new ConcurrentHashMap<>();

    @Autowired
    protected MockMvc mvc;
    @Autowired
    protected ObjectMapper json;

    @DynamicPropertySource
    static void baseDeDatos(DynamicPropertyRegistry registro) throws Exception {
        if (mysql == null) {
            // Se arranca una sola vez para todas las clases y Ryuk lo elimina al terminar.
            mysql = new MySQLContainer<>("mysql:8.4").withDatabaseName("renta_herramientas_db");
            mysql.start();
            uploads = Files.createTempDirectory("uploads-test").toString();
        }
        registro.add("spring.datasource.url", mysql::getJdbcUrl);
        registro.add("spring.datasource.username", mysql::getUsername);
        registro.add("spring.datasource.password", mysql::getPassword);
        registro.add("spring.jpa.show-sql", () -> "false");
        registro.add("app.uploads.dir", () -> uploads);
        registro.add("app.vencimiento.cron", () -> "-"); // sin vencimiento periódico durante las pruebas
    }

    // ------------------------------------------------------------------ peticiones

    /** Token JWT de una cuenta (se inicia sesión una vez y se reutiliza). */
    protected String token(String email) {
        return TOKENS.computeIfAbsent(email, correo -> login(correo, CLAVE).get("token").asText());
    }

    protected JsonNode login(String email, String clave) {
        try {
            MvcResult r = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                    .content(json.writeValueAsString(Map.of("email", email, "password", clave)))).andReturn();
            if (r.getResponse().getStatus() != 200) {
                throw new IllegalStateException("No se pudo iniciar sesión con " + email + ": " + r.getResponse().getContentAsString());
            }
            return json.readTree(r.getResponse().getContentAsString());
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /** Agrega el token; sirve para peticiones normales y multipart (en Spring 7 no comparten la misma clase). */
    protected <T extends AbstractMockHttpServletRequestBuilder<T>> T conToken(T peticion, String email) {
        return email == null ? peticion : peticion.header(HttpHeaders.AUTHORIZATION, "Bearer " + token(email));
    }

    protected MockHttpServletRequestBuilder conCuerpo(MockHttpServletRequestBuilder peticion, Object cuerpo) throws Exception {
        return peticion.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(cuerpo));
    }

    protected MockHttpServletRequestBuilder GET(String url, String email) {
        return conToken(get(url), email);
    }

    protected MockHttpServletRequestBuilder POST(String url, String email, Object cuerpo) throws Exception {
        return conToken(cuerpo == null ? post(url) : conCuerpo(post(url), cuerpo), email);
    }

    protected MockHttpServletRequestBuilder PUT(String url, String email, Object cuerpo) throws Exception {
        return conToken(conCuerpo(put(url), cuerpo), email);
    }

    protected MockHttpServletRequestBuilder PATCH(String url, String email, Object cuerpo) throws Exception {
        return conToken(conCuerpo(patch(url), cuerpo), email);
    }

    protected MockHttpServletRequestBuilder DELETE(String url, String email) {
        return conToken(delete(url), email);
    }

    /** Ejecuta la petición, exige el código HTTP indicado y devuelve el cuerpo JSON (o null si no tiene). */
    protected JsonNode esperar(AbstractMockHttpServletRequestBuilder<?> peticion, int status) throws Exception {
        MvcResult r = mvc.perform(peticion).andReturn();
        String cuerpo = r.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        if (r.getResponse().getStatus() != status) {
            throw new AssertionError("Se esperaba HTTP " + status + " pero fue " + r.getResponse().getStatus() + ": " + cuerpo);
        }
        return cuerpo.isBlank() ? null : json.readTree(cuerpo);
    }

    // ------------------------------------------------------------------ datos

    /** Día de negocio (America/Bogota) desplazado n días. */
    protected static String dia(int n) {
        return LocalDate.now(ZoneId.of("America/Bogota")).plusDays(n).toString();
    }

    /** El PROVEEDOR publica una herramienta nueva (sin reservas) para que la prueba no dependa de otras. */
    protected long nuevaHerramienta(String nombre, int tarifa) throws Exception {
        return esperar(POST("/api/herramientas", PROVEEDOR, Map.of("categoriaId", 2, "nombre", nombre + " " + System.nanoTime(),
                "descripcion", "Creada por una prueba de integración", "tarifaDiaria", tarifa)), 201).get("id").asLong();
    }

    protected long reservar(String cliente, long herramientaId, String inicio, String fin) throws Exception {
        return esperar(POST("/api/reservas", cliente, Map.of("herramientaId", herramientaId, "fechaInicio", inicio,
                "fechaFin", fin)), 201).get("id").asLong();
    }
}
