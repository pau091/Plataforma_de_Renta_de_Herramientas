package com.example.tomcat.service;

import com.example.tomcat.exception.DomainException;

/**
 * Valida una imagen subida revisando sus bytes iniciales ("números mágicos") en lugar de confiar
 * en la extensión o en el Content-Type que envía el navegador, que se pueden falsificar.
 */
final class ValidadorImagen {

    static final long TAMANO_MAXIMO_BYTES = 5L * 1024 * 1024;

    private ValidadorImagen() {
    }

    /** Devuelve la extensión real del archivo (jpg, png, webp o gif) o lanza un error si no es válido. */
    static String extensionValida(byte[] contenido) {
        if (contenido == null || contenido.length == 0) {
            throw new DomainException("Debe adjuntar un archivo de imagen.");
        }
        if (contenido.length > TAMANO_MAXIMO_BYTES) {
            throw new DomainException("La imagen supera el tamaño máximo de 5 MB.");
        }
        if (empiezaCon(contenido, 0xFF, 0xD8, 0xFF)) {
            return "jpg";
        }
        if (empiezaCon(contenido, 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)) {
            return "png";
        }
        if (empiezaCon(contenido, 'G', 'I', 'F', '8')) {
            return "gif";
        }
        if (contenido.length >= 12 && empiezaCon(contenido, 'R', 'I', 'F', 'F')
                && contenido[8] == 'W' && contenido[9] == 'E' && contenido[10] == 'B' && contenido[11] == 'P') {
            return "webp";
        }
        throw new DomainException("Formato de imagen no permitido. Use JPG, PNG, WEBP o GIF.");
    }

    private static boolean empiezaCon(byte[] contenido, int... firma) {
        if (contenido.length < firma.length) {
            return false;
        }
        for (int i = 0; i < firma.length; i++) {
            if ((contenido[i] & 0xFF) != firma[i]) {
                return false;
            }
        }
        return true;
    }
}
