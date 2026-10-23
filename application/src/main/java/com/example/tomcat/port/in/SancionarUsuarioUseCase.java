package com.example.tomcat.port.in;

import com.example.tomcat.model.Usuario;
import com.example.tomcat.port.in.dto.Sancion;

/** Suspender, banear o reactivar a un proveedor o cliente conflictivo (solo ADMIN). */
public interface SancionarUsuarioUseCase {
    Sancion suspender(Long usuarioId, int dias, String motivo);
    Sancion banear(Long usuarioId, String motivo);
    Usuario reactivar(Long usuarioId);
}
