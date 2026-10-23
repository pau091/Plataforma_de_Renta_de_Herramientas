package com.example.tomcat.port.in.dto;

import com.example.tomcat.model.Usuario;

/** Resultado de suspender o banear: el usuario actualizado y cuántas sesiones (tokens) se cerraron al instante. */
public record Sancion(Usuario usuario, int tokensRevocados) {
}
