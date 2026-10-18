package com.example.tomcat.valueobject;

import com.example.tomcat.exception.DomainException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Objetos de valor: Dinero y Email")
class ValueObjectsTest {

    @Test
    @DisplayName("Dinero acepta cero y positivos; compara por valor")
    void dineroValido() {
        assertThat(new Dinero(BigDecimal.ZERO).getMonto()).isEqualByComparingTo("0");
        assertThat(new Dinero(new BigDecimal("1500.00"))).isEqualTo(new Dinero(new BigDecimal("1500")));
    }

    @Test
    @DisplayName("Dinero rechaza nulos y negativos")
    void dineroInvalido() {
        assertThatThrownBy(() -> new Dinero(null)).isInstanceOf(DomainException.class).hasMessageContaining("nulo");
        assertThatThrownBy(() -> new Dinero(new BigDecimal("-1"))).isInstanceOf(DomainException.class).hasMessageContaining("negativo");
    }

    @Test
    @DisplayName("Email quita espacios y conserva el valor")
    void emailValido() {
        assertThat(new Email("  juan.cliente@gmail.com ").getValor()).isEqualTo("juan.cliente@gmail.com");
        assertThat(new Email("a@b.co")).isEqualTo(new Email("a@b.co"));
    }

    @ParameterizedTest(name = "correo inválido: \"{0}\"")
    @NullAndEmptySource
    @ValueSource(strings = {"sin-arroba", "juan@", "@gmail.com", "juan@gmail", "juan @gmail.com"})
    void emailInvalido(String valor) {
        assertThatThrownBy(() -> new Email(valor)).isInstanceOf(DomainException.class);
    }
}
