package com.example.tomcat.adapter.out.mapper;

import com.example.tomcat.adapter.out.persistence.entity.PagoEntity;
import com.example.tomcat.model.Pago;
import org.springframework.stereotype.Component;

@Component
public class PagoJpaMapper {

    public Pago toDomain(PagoEntity entity) {
        return new Pago(
                entity.getId(),
                entity.getReservaId(),
                entity.getMonto(),
                entity.getMetodoPago(),
                entity.getEstadoPago(),
                entity.getFechaPago()
        );
    }

    public void copyToEntity(Pago pago, PagoEntity entity) {
        entity.setReservaId(pago.getReservaId());
        entity.setMonto(pago.getMonto());
        entity.setMetodoPago(pago.getMetodoPago());
        entity.setEstadoPago(pago.getEstadoPago());
        if (pago.getFechaPago() != null) {
            entity.setFechaPago(pago.getFechaPago());
        }
    }
}
