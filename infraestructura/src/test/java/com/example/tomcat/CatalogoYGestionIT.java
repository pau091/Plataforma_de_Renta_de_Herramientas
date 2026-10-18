package com.example.tomcat;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;

@DisplayName("Integración: catálogo con filtros, imágenes, categorías y usuarios")
class CatalogoYGestionIT extends IntegracionBase {

    /** Firma PNG + relleno: el servidor valida el formato por los bytes iniciales. */
    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0x0D, 'I', 'H', 'D', 'R'};

    @Test
    @DisplayName("Búsqueda por categoría, texto y disponibilidad (filtros combinables)")
    void filtrosDelCatalogo() throws Exception {
        JsonNode porCategoria = esperar(GET("/api/herramientas?categoriaId=1", CLIENTE), 200);
        assertThat(porCategoria).isNotEmpty().allSatisfy(h -> assertThat(h.get("categoriaId").asLong()).isEqualTo(1L));

        JsonNode porTexto = esperar(GET("/api/herramientas?q=TALADRO", CLIENTE), 200);
        assertThat(porTexto).isNotEmpty().allSatisfy(h ->
                assertThat((h.get("nombre").asText() + " " + h.get("descripcion").asText()).toLowerCase()).contains("taladro"));

        JsonNode disponibles = esperar(GET("/api/herramientas?disponibles=true", CLIENTE), 200);
        assertThat(disponibles).allSatisfy(h -> assertThat(h.get("estado").asText()).isEqualTo("DISPONIBLE"));
        JsonNode todas = esperar(GET("/api/herramientas", CLIENTE), 200);
        assertThat(todas.size()).isGreaterThan(disponibles.size()); // los datos demo tienen una en mantenimiento y otra perdida

        JsonNode combinados = esperar(GET("/api/herramientas?proveedorId=2&categoriaId=1", CLIENTE), 200);
        assertThat(combinados).allSatisfy(h -> {
            assertThat(h.get("proveedorId").asLong()).isEqualTo(2L);
            assertThat(h.get("categoriaId").asLong()).isEqualTo(1L);
        });
        esperar(GET("/api/herramientas?categoriaId=999999", CLIENTE), 404);
    }

    @Test
    @DisplayName("El proveedor gestiona su inventario: crear, editar (parcial), cambiar estado; otro proveedor no puede")
    void inventarioDelProveedor() throws Exception {
        long id = nuevaHerramienta("Taladro IT", 35000);
        JsonNode creada = esperar(GET("/api/herramientas/" + id, CLIENTE), 200);
        assertThat(creada.get("proveedorId").asLong()).isEqualTo(2L); // queda a nombre del proveedor autenticado

        JsonNode editada = esperar(PUT("/api/herramientas/" + id, PROVEEDOR, Map.of("tarifaDiaria", 38000)), 200);
        assertThat(editada.get("tarifaDiaria").decimalValue()).isEqualByComparingTo("38000");
        assertThat(editada.get("descripcion").asText()).isEqualTo("Creada por una prueba de integración");

        assertThat(esperar(PATCH("/api/herramientas/" + id + "/estado", PROVEEDOR, Map.of("estado", "MANTENIMIENTO")), 200)
                .get("estado").asText()).isEqualTo("MANTENIMIENTO");
        esperar(PATCH("/api/herramientas/" + id + "/estado", PROVEEDOR, Map.of("estado", "ROTA")), 400);
        esperar(PUT("/api/herramientas/" + id, OTRO_PROVEEDOR, Map.of("tarifaDiaria", 1)), 403);

        JsonNode invalida = esperar(POST("/api/herramientas", PROVEEDOR, Map.of("categoriaId", 2, "nombre", "", "tarifaDiaria", -5)), 400);
        assertThat(invalida.get("errores").has("nombre")).isTrue();
        assertThat(invalida.get("errores").has("tarifaDiaria")).isTrue();

        esperar(DELETE("/api/herramientas/" + id, PROVEEDOR), 204); // sin historial sí se puede borrar
        esperar(GET("/api/herramientas/" + id, CLIENTE), 404);
    }

    @Test
    @DisplayName("Subida de imagen: se valida el contenido y queda publicada en /uploads")
    void subidaDeImagen() throws Exception {
        long id = nuevaHerramienta("Escalera IT", 12000);

        MvcResult subida = mvc.perform(conToken(multipart("/api/herramientas/" + id + "/imagen")
                .file(new MockMultipartFile("archivo", "foto.png", "image/png", PNG)), PROVEEDOR)).andReturn();
        assertThat(subida.getResponse().getStatus()).isEqualTo(200);
        String url = json.readTree(subida.getResponse().getContentAsString()).get("imagenUrl").asText();
        assertThat(url).startsWith("/uploads/").endsWith(".png");

        MvcResult imagen = mvc.perform(get(url)).andReturn(); // pública, sin token
        assertThat(imagen.getResponse().getStatus()).isEqualTo(200);
        assertThat(imagen.getResponse().getContentAsByteArray()).isEqualTo(PNG);

        MvcResult falsa = mvc.perform(conToken(multipart("/api/herramientas/" + id + "/imagen")
                .file(new MockMultipartFile("archivo", "virus.png", "image/png",
                        "<script>alert(1)</script>".getBytes(StandardCharsets.UTF_8))), PROVEEDOR)).andReturn();
        assertThat(falsa.getResponse().getStatus()).isEqualTo(400);

        MvcResult ajena = mvc.perform(conToken(multipart("/api/herramientas/" + id + "/imagen")
                .file(new MockMultipartFile("archivo", "foto.png", "image/png", PNG)), OTRO_PROVEEDOR)).andReturn();
        assertThat(ajena.getResponse().getStatus()).isEqualTo(403);
    }

    @Test
    @DisplayName("Categorías: CRUD del ADMIN, nombre único y no se borra si tiene herramientas")
    void categorias() throws Exception {
        String nombre = "Topografía IT " + System.nanoTime();
        long id = esperar(POST("/api/categorias", ADMIN, Map.of("nombre", nombre, "descripcion", "GPS")), 201).get("id").asLong();
        esperar(POST("/api/categorias", ADMIN, Map.of("nombre", nombre.toUpperCase())), 409);
        esperar(POST("/api/categorias", CLIENTE, Map.of("nombre", "No permitida")), 403);
        assertThat(esperar(PUT("/api/categorias/" + id, ADMIN, Map.of("descripcion", "GPS y niveles")), 200)
                .get("nombre").asText()).isEqualTo(nombre);
        esperar(DELETE("/api/categorias/1", ADMIN), 409);
        esperar(DELETE("/api/categorias/" + id, ADMIN), 204);
        esperar(GET("/api/categorias/" + id, CLIENTE), 404);
    }

    @Test
    @DisplayName("Usuarios: el ADMIN registra proveedores; correo único; no se borra a quien tiene historial")
    void usuarios() throws Exception {
        String correo = "proveedor.it." + System.nanoTime() + "@prueba.test";
        JsonNode creado = esperar(POST("/api/usuarios", ADMIN, Map.of("nombre", "Proveedor IT", "email", correo,
                "password", CLAVE, "rol", "PROVEEDOR")), 201);
        assertThat(creado.get("rol").asText()).isEqualTo("PROVEEDOR");
        assertThat(creado.has("password")).isFalse();

        esperar(POST("/api/usuarios", ADMIN, Map.of("nombre", "Repetido", "email", correo, "password", CLAVE, "rol", "CLIENTE")), 409);
        JsonNode proveedores = esperar(GET("/api/usuarios/proveedores", ADMIN), 200);
        assertThat(proveedores).anySatisfy(u -> assertThat(u.get("email").asText()).isEqualTo(correo));

        JsonNode historial = esperar(DELETE("/api/usuarios/12", ADMIN), 409);
        assertThat(historial.get("message").asText()).contains("activo=false");
        esperar(DELETE("/api/usuarios/" + creado.get("id").asLong(), ADMIN), 204);
    }
}
