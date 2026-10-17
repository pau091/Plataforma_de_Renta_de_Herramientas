package com.example.tomcat.service;

import com.example.tomcat.exception.DomainException;
import com.example.tomcat.model.Herramienta;
import com.example.tomcat.model.Rol;
import com.example.tomcat.model.Usuario;
import com.example.tomcat.port.in.GestionarHerramientaUseCase;
import com.example.tomcat.repository.CategoriaRepositoryPort;
import com.example.tomcat.repository.HerramientaRepositoryPort;
import com.example.tomcat.repository.UsuarioRepositoryPort;
import com.example.tomcat.valueobject.Dinero;

import java.math.BigDecimal;
import java.util.List;

public class GestionarHerramientaService implements GestionarHerramientaUseCase {

    private final HerramientaRepositoryPort herramientaRepositoryPort;
    private final UsuarioRepositoryPort usuarioRepositoryPort;
    private final CategoriaRepositoryPort categoriaRepositoryPort;

    public GestionarHerramientaService(
            HerramientaRepositoryPort herramientaRepositoryPort,
            UsuarioRepositoryPort usuarioRepositoryPort,
            CategoriaRepositoryPort categoriaRepositoryPort
    ) {
        this.herramientaRepositoryPort = herramientaRepositoryPort;
        this.usuarioRepositoryPort = usuarioRepositoryPort;
        this.categoriaRepositoryPort = categoriaRepositoryPort;
    }

    @Override
    public Herramienta crear(Long proveedorId, Long categoriaId, String nombre, String descripcion, BigDecimal tarifaDiaria, String imagenUrl) {
        validarProveedor(proveedorId);
        validarCategoria(categoriaId);
        validarNombre(nombre);
        Dinero tarifa = new Dinero(tarifaDiaria);

        Herramienta herramienta = new Herramienta(
                null,
                proveedorId,
                categoriaId,
                nombre.trim(),
                descripcion,
                tarifa.getMonto(),
                "DISPONIBLE",
                imagenUrl
        );
        return herramientaRepositoryPort.guardar(herramienta);
    }

    @Override
    public Herramienta actualizar(Long id, Long categoriaId, String nombre, String descripcion, BigDecimal tarifaDiaria, String estado, String imagenUrl) {
        Herramienta herramienta = buscarPorId(id);
        Long categoria = categoriaId != null ? categoriaId : herramienta.getCategoriaId();
        validarCategoria(categoria);
        String nombreActualizado = (nombre == null || nombre.isBlank()) ? herramienta.getNombre() : nombre.trim();
        BigDecimal tarifa = tarifaDiaria != null ? new Dinero(tarifaDiaria).getMonto() : herramienta.getTarifaDiaria();

        herramienta.actualizar(categoria, nombreActualizado, descripcion, tarifa, estado, imagenUrl);
        return herramientaRepositoryPort.guardar(herramienta);
    }

    @Override
    public void eliminar(Long id) {
        buscarPorId(id);
        herramientaRepositoryPort.eliminar(id);
    }

    @Override
    public Herramienta cambiarEstado(Long id, String estado) {
        Herramienta herramienta = buscarPorId(id);
        herramienta.cambiarEstado(estado);
        return herramientaRepositoryPort.guardar(herramienta);
    }

    @Override
    public Herramienta actualizarImagen(Long id, String imagenUrl) {
        Herramienta herramienta = buscarPorId(id);
        if (imagenUrl == null || imagenUrl.isBlank()) {
            throw new DomainException("La URL de la imagen es obligatoria.");
        }
        herramienta.actualizarImagen(imagenUrl.trim());
        return herramientaRepositoryPort.guardar(herramienta);
    }

    @Override
    public List<Herramienta> listar() {
        return herramientaRepositoryPort.listar();
    }

    @Override
    public List<Herramienta> listarDisponibles() {
        return herramientaRepositoryPort.listarDisponibles();
    }

    @Override
    public List<Herramienta> listarPorProveedor(Long proveedorId) {
        return herramientaRepositoryPort.listarPorProveedor(proveedorId);
    }

    @Override
    public Herramienta buscarPorId(Long id) {
        return herramientaRepositoryPort.buscarPorId(id)
                .orElseThrow(() -> new DomainException("La herramienta no existe."));
    }

    private void validarProveedor(Long proveedorId) {
        Usuario proveedor = usuarioRepositoryPort.buscarPorId(proveedorId)
                .orElseThrow(() -> new DomainException("El proveedor no existe."));
        if (proveedor.getRol() != Rol.PROVEEDOR && proveedor.getRol() != Rol.ADMIN) {
            throw new DomainException("El usuario no puede registrar herramientas.");
        }
    }

    private void validarCategoria(Long categoriaId) {
        if (categoriaId == null) {
            throw new DomainException("La categoría es obligatoria.");
        }
        categoriaRepositoryPort.buscarPorId(categoriaId)
                .orElseThrow(() -> new DomainException("La categoría no existe."));
    }

    private void validarNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            throw new DomainException("El nombre de la herramienta es obligatorio.");
        }
    }
}
