package com.example.tomcat.valueobject;

import com.example.tomcat.exception.DomainException;
import java.math.BigDecimal;

public class Dinero {
    private final BigDecimal monto;

    public Dinero(BigDecimal monto) {
        if (monto == null) {
            throw new DomainException("El monto de dinero no puede ser nulo.");
        }
        if (monto.compareTo(BigDecimal.ZERO) < 0) {
            throw new DomainException("El monto de dinero no puede ser negativo.");
        }
        this.monto = monto;
    }

    public BigDecimal getMonto() {
        return monto;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Dinero dinero = (Dinero) o;
        return monto.compareTo(dinero.monto) == 0;
    }

    @Override
    public int hashCode() {
        return monto.hashCode();
    }

    @Override
    public String toString() {
        return monto.toString();
    }
}