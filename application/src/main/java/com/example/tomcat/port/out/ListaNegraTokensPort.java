package com.example.tomcat.port.out;

/**
 * Lista negra de tokens JWT: un token revocado deja de servir aunque su firma y su vigencia sigan siendo válidas.
 * La implementa la infraestructura (hoy con MySQL; podría ser Redis sin cambiar los casos de uso).
 */
public interface ListaNegraTokensPort {

    /** Revoca todos los tokens vigentes del usuario y devuelve cuántos revocó. */
    int revocarTodos(Long usuarioId, String motivo);
}
