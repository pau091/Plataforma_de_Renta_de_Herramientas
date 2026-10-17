package com.example.tomcat.exception;

/**
 * Se lanza cuando un usuario desactivado intenta iniciar sesión. Se traduce a HTTP 403.
 */
public class UsuarioInactivoException extends DomainException {
    public UsuarioInactivoException() {
        super("El usuario está inactivo. Contacte al administrador de la plataforma.");
    }
}
