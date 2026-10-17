package com.example.tomcat.adapter.out.storage;

import com.example.tomcat.port.out.AlmacenamientoImagenPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/**
 * Guarda las imágenes en una carpeta del servidor (app.uploads.dir) y las publica en /uploads/{archivo}.
 * En Docker esa carpeta es un volumen, así que las imágenes sobreviven a reconstrucciones del contenedor.
 */
@Component
public class AlmacenamientoImagenLocalAdapter implements AlmacenamientoImagenPort {

    public static final String PREFIJO_URL = "/uploads/";

    private static final Logger log = LoggerFactory.getLogger(AlmacenamientoImagenLocalAdapter.class);

    private final Path directorio;

    public AlmacenamientoImagenLocalAdapter(@Value("${app.uploads.dir}") String directorio) {
        this.directorio = Path.of(directorio).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.directorio);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo crear la carpeta de imágenes " + this.directorio, e);
        }
    }

    public Path getDirectorio() {
        return directorio;
    }

    @Override
    public String guardar(byte[] contenido, String extension) {
        // El nombre lo genera el sistema: nunca se usa el nombre que envía el usuario.
        String nombre = UUID.randomUUID() + "." + extension;
        try {
            Files.createDirectories(directorio);
            Files.write(directorio.resolve(nombre), contenido);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo guardar la imagen.", e);
        }
        return PREFIJO_URL + nombre;
    }

    @Override
    public void eliminarSiEsPropia(String url) {
        if (url == null || !url.startsWith(PREFIJO_URL)) {
            return;
        }
        Path archivo = directorio.resolve(url.substring(PREFIJO_URL.length())).normalize();
        if (!archivo.startsWith(directorio)) {
            return; // Protección ante rutas manipuladas como /uploads/../algo
        }
        try {
            Files.deleteIfExists(archivo);
        } catch (IOException e) {
            log.warn("No se pudo eliminar la imagen anterior {}", archivo, e);
        }
    }
}
