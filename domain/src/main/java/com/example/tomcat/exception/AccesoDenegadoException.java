package com.example.tomcat.exception;

/**
 * Se lanza cuando el usuario autenticado intenta operar sobre un recurso que no le pertenece
 * o para el cual su rol no tiene permiso. Se traduce a HTTP 403.
 */
public class AccesoDenegadoException extends DomainException {
    public AccesoDenegadoException(String message) {
        super(message);
    }
}
