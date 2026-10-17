package com.example.tomcat.port.in;

import com.example.tomcat.model.Usuario;

public interface RegistrarUsuarioUseCase {

    /** Registro abierto desde la página de inicio: solo crea cuentas de CLIENTE. */
    Usuario registrarCliente(String nombre, String email, String password, String rolSolicitado);

    /** Alta hecha por el administrador: permite crear PROVEEDOR o CLIENTE (nunca ADMIN). */
    Usuario registrar(String nombre, String email, String password, String rol);
}
