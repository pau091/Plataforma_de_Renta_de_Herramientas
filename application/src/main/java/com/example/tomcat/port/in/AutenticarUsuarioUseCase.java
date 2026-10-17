package com.example.tomcat.port.in;

import com.example.tomcat.model.Usuario;

public interface AutenticarUsuarioUseCase {
    Usuario autenticar(String email, String password);
}
