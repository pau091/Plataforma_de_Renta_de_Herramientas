package com.example.tomcat.port.in;

import com.example.tomcat.model.Usuario;
import com.example.tomcat.port.in.dto.Solicitante;

import java.util.List;

public interface GestionarUsuarioUseCase {
    List<Usuario> listarClientes();
    List<Usuario> listarProveedores();
    List<Usuario> listarAdministradores();
    Usuario buscarPorId(Long id);
    Usuario actualizar(Long id, String nombre, String email, Boolean activo, String password);
    void eliminar(Long id);
    /** Ascender o bajar de rango a un usuario. Solo lo puede hacer el administrador principal. */
    Usuario cambiarRol(Solicitante solicitante, Long id, String rol);
}
