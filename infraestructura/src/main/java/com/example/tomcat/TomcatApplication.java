package com.example.tomcat;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

import java.util.TimeZone;

@SpringBootApplication
public class TomcatApplication extends SpringBootServletInitializer {

    static {
        // Las fechas y horas se guardan en UTC (igual que MySQL en Docker), sin importar la zona del equipo.
        // Las fechas de negocio ("hoy") usan app.zona-horaria a través del Clock definido en BeanConfig.
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
        return builder.sources(TomcatApplication.class);
    }

    public static void main(String[] args) {
        SpringApplication.run(TomcatApplication.class, args);
    }
}