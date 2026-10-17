package com.example.tomcat.exception;

/**
 * Se lanza cuando el recurso solicitado (usuario, herramienta, reserva, pago, factura...) no existe.
 * Se traduce a HTTP 404.
 */
public class RecursoNoEncontradoException extends DomainException {
    public RecursoNoEncontradoException(String message) {
        super(message);
    }
}
