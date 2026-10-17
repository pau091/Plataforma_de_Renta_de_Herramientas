package com.example.tomcat.port.in;

import com.example.tomcat.model.Usuario;

public interface RegistrarUsuarioUseCase {
    Usuario registrar(String nombre, String email, String password, String rol);
}
