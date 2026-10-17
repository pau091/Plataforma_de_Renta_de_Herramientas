package com.example.tomcat.config;

import com.example.tomcat.adapter.out.storage.AlmacenamientoImagenLocalAdapter;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Publica las imágenes subidas en /uploads/** directamente desde la carpeta de almacenamiento. */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final AlmacenamientoImagenLocalAdapter almacenamiento;

    public WebConfig(AlmacenamientoImagenLocalAdapter almacenamiento) {
        this.almacenamiento = almacenamiento;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String ubicacion = almacenamiento.getDirectorio().toUri().toString();
        registry.addResourceHandler(AlmacenamientoImagenLocalAdapter.PREFIJO_URL + "**")
                .addResourceLocations(ubicacion.endsWith("/") ? ubicacion : ubicacion + "/");
    }
}
