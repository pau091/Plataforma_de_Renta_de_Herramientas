package com.example.tomcat.exception;

/**
 * Se lanza cuando la operación choca con el estado actual de los datos, por ejemplo
 * al intentar eliminar un registro que tiene historial asociado. Se traduce a HTTP 409.
 */
public class ConflictoException extends DomainException {
    public ConflictoException(String message) {
        super(message);
    }
}
