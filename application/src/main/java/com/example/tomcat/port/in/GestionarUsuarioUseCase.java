package com.example.tomcat.port.in;

import com.example.tomcat.model.Rol;
import com.example.tomcat.model.Usuario;

import java.util.List;

public interface GestionarUsuarioUseCase {
    List<Usuario> listarClientes();
    List<Usuario> listarProveedores();
    Usuario buscarPorId(Long id);
    Usuario actualizar(Long id, String nombre, String email, Boolean activo, String password);
    void eliminar(Long id);
}
