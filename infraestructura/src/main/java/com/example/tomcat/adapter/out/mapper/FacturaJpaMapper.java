package com.example.tomcat.adapter.out.mapper;

import com.example.tomcat.adapter.out.persistence.entity.FacturaEntity;
import com.example.tomcat.model.Factura;
import org.springframework.stereotype.Component;

@Component
public class FacturaJpaMapper {

    public Factura toDomain(FacturaEntity entity) {
        return new Factura(
                entity.getId(),
                entity.getNumero(),
                entity.getPagoId(),
                entity.getReservaId(),
                entity.getClienteId(),
                entity.getClienteNombre(),
                entity.getClienteEmail(),
                entity.getHerramientaId(),
                entity.getHerramientaNombre(),
                entity.getFechaInicio(),
                entity.getFechaFin(),
                entity.getDias() == null ? 0 : entity.getDias(),
                entity.getTarifaDiaria(),
                entity.getSubtotal(),
                entity.getTotal(),
                entity.getMetodoPago(),
                entity.getFechaEmision()
        );
    }

    public void copyToEntity(Factura factura, FacturaEntity entity) {
        entity.setNumero(factura.getNumero());
        entity.setPagoId(factura.getPagoId());
        entity.setReservaId(factura.getReservaId());
        entity.setClienteId(factura.getClienteId());
        entity.setClienteNombre(factura.getClienteNombre());
        entity.setClienteEmail(factura.getClienteEmail());
        entity.setHerramientaId(factura.getHerramientaId());
        entity.setHerramientaNombre(factura.getHerramientaNombre());
        entity.setFechaInicio(factura.getFechaInicio());
        entity.setFechaFin(factura.getFechaFin());
        entity.setDias((int) factura.getDias());
        entity.setTarifaDiaria(factura.getTarifaDiaria());
        entity.setSubtotal(factura.getSubtotal());
        entity.setTotal(factura.getTotal());
        entity.setMetodoPago(factura.getMetodoPago());
        if (factura.getFechaEmision() != null) {
            entity.setFechaEmision(factura.getFechaEmision());
        }
    }
}
