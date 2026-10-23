package com.example.tomcat.config;

import com.example.tomcat.security.ListaNegraTokensAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Mantiene pequeña la lista negra: borra los tokens que ya expiraron (la validación de vigencia los rechaza igual,
 * así que no hace falta recordarlos). Es lo que en Redis haría solo el TTL de cada clave.
 * Se ejecuta según app.tokens.limpieza-cron (por defecto, cada hora).
 */
@Component
public class LimpiezaTokensScheduler {

    private static final Logger log = LoggerFactory.getLogger(LimpiezaTokensScheduler.class);

    private final ListaNegraTokensAdapter listaNegraTokens;

    public LimpiezaTokensScheduler(ListaNegraTokensAdapter listaNegraTokens) {
        this.listaNegraTokens = listaNegraTokens;
    }

    @Scheduled(cron = "${app.tokens.limpieza-cron}", zone = "${app.zona-horaria}")
    public void ejecutar() {
        int borrados = listaNegraTokens.limpiarExpirados();
        if (borrados > 0) {
            log.info("Tokens expirados eliminados de la lista negra y del registro: {}", borrados);
        }
    }
}
