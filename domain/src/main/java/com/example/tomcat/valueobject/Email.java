package com.example.tomcat.valueobject;

import com.example.tomcat.exception.DomainException;
import java.util.regex.Pattern;

public class Email {
    private final String valor;
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$");

    public Email(String valor) {
        if (valor == null || valor.isBlank()) {
            throw new DomainException("El correo electrónico no puede estar vacío.");
        }
        String trimmedValue = valor.trim();
        if (!EMAIL_PATTERN.matcher(trimmedValue).matches()) {
            throw new DomainException("El formato del correo electrónico no es válido.");
        }
        this.valor = trimmedValue;
    }

    public String getValor() {
        return valor;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Email email = (Email) o;
        return valor.equals(email.valor);
    }

    @Override
    public int hashCode() {
        return valor.hashCode();
    }

    @Override
    public String toString() {
        return valor;
    }
}