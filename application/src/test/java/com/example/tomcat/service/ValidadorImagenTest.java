package com.example.tomcat.service;

import com.example.tomcat.exception.DomainException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ValidadorImagen: el formato se decide por el contenido, no por la extensión")
class ValidadorImagenTest {

    private static byte[] bytes(int... valores) {
        byte[] b = new byte[Math.max(valores.length, 16)];
        for (int i = 0; i < valores.length; i++) {
            b[i] = (byte) valores[i];
        }
        return b;
    }

    @Test
    void reconoceLosFormatosPermitidos() {
        assertThat(ValidadorImagen.extensionValida(bytes(0xFF, 0xD8, 0xFF, 0xE0))).isEqualTo("jpg");
        assertThat(ValidadorImagen.extensionValida(bytes(0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A))).isEqualTo("png");
        assertThat(ValidadorImagen.extensionValida("GIF89a....".getBytes(StandardCharsets.US_ASCII))).isEqualTo("gif");
        assertThat(ValidadorImagen.extensionValida(bytes('R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'E', 'B', 'P'))).isEqualTo("webp");
    }

    @Test
    @DisplayName("Un archivo de texto renombrado a .png se rechaza")
    void rechazaContenidoQueNoEsImagen() {
        assertThatThrownBy(() -> ValidadorImagen.extensionValida("<script>alert(1)</script>".getBytes(StandardCharsets.UTF_8)))
                .isInstanceOf(DomainException.class).hasMessageContaining("Formato de imagen no permitido");
    }

    @Test
    void rechazaVacioYDemasiadoGrande() {
        assertThatThrownBy(() -> ValidadorImagen.extensionValida(new byte[0])).isInstanceOf(DomainException.class);
        byte[] grande = new byte[(int) ValidadorImagen.TAMANO_MAXIMO_BYTES + 1];
        grande[0] = (byte) 0xFF;
        grande[1] = (byte) 0xD8;
        grande[2] = (byte) 0xFF;
        assertThatThrownBy(() -> ValidadorImagen.extensionValida(grande))
                .isInstanceOf(DomainException.class).hasMessageContaining("5 MB");
    }
}
