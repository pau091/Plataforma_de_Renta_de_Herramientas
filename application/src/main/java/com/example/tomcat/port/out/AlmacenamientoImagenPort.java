package com.example.tomcat.port.out;

/**
 * Puerto de salida para guardar imágenes de herramientas. La implementación decide dónde
 * (disco local, nube...) y devuelve la URL pública con la que se sirve la imagen.
 */
public interface AlmacenamientoImagenPort {

    /** Guarda el contenido con un nombre generado por el sistema y devuelve su URL pública. */
    String guardar(byte[] contenido, String extension);

    /** Elimina la imagen si fue guardada por este almacenamiento; ignora URLs externas. */
    void eliminarSiEsPropia(String url);
}
