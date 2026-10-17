package com.example.tomcat.port.in;

import com.example.tomcat.port.in.dto.Panel;
import com.example.tomcat.port.in.dto.Solicitante;

public interface ConsultarPanelUseCase {
    /** Devuelve el panel del usuario autenticado: Panel.Cliente, Panel.Proveedor o Panel.Admin. */
    Panel obtener(Solicitante solicitante);
}
