package com.example.tomcat.service;

import com.example.tomcat.exception.AccesoDenegadoException;
import com.example.tomcat.exception.DomainException;
import com.example.tomcat.model.Herramienta;
import com.example.tomcat.model.Rol;
import com.example.tomcat.model.Usuario;
import com.example.tomcat.port.in.GestionarHerramientaUseCase;
import com.example.tomcat.port.in.dto.Solicitante;
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
    private final ControlAcceso controlAcceso;

    public GestionarHerramientaService(
            HerramientaRepositoryPort herramientaRepositoryPort,
            UsuarioRepositoryPort usuarioRepositoryPort,
            CategoriaRepositoryPort categoriaRepositoryPort,
            ControlAcceso controlAcceso
    ) {
        this.herramientaRepositoryPort = herramientaRepositoryPort;
        this.usuarioRepositoryPort = usuarioRepositoryPort;
        this.categoriaRepositoryPort = categoriaRepositoryPort;
        this.controlAcceso = controlAcceso;
    }

    @Override
    public Herramienta crear(Solicitante solicitante, Long proveedorId, Long categoriaId, String nombre, String descripcion,
                             BigDecimal tarifaDiaria, String imagenUrl) {
        Long dueno = resolverProveedor(solicitante, proveedorId);
        validarCategoria(categoriaId);
        validarNombre(nombre);
        Dinero tarifa = new Dinero(tarifaDiaria);

        Herramienta herramienta = new Herramienta(
                null,
                dueno,
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
    public Herramienta actualizar(Solicitante solicitante, Long id, Long categoriaId, String nombre, String descripcion,
                                  BigDecimal tarifaDiaria, String estado, String imagenUrl) {
        Herramienta herramienta = buscarPropia(solicitante, id);
        Long categoria = categoriaId != null ? categoriaId : herramienta.getCategoriaId();
        validarCategoria(categoria);
        String nombreActualizado = (nombre == null || nombre.isBlank()) ? herramienta.getNombre() : nombre.trim();
        BigDecimal tarifa = tarifaDiaria != null ? new Dinero(tarifaDiaria).getMonto() : herramienta.getTarifaDiaria();

        herramienta.actualizar(categoria, nombreActualizado, descripcion, tarifa, estado, imagenUrl);
        return herramientaRepositoryPort.guardar(herramienta);
    }

    @Override
    public void eliminar(Solicitante solicitante, Long id) {
        buscarPropia(solicitante, id);
        herramientaRepositoryPort.eliminar(id);
    }

    @Override
    public Herramienta cambiarEstado(Solicitante solicitante, Long id, String estado) {
        Herramienta herramienta = buscarPropia(solicitante, id);
        herramienta.cambiarEstado(estado);
        return herramientaRepositoryPort.guardar(herramienta);
    }

    @Override
    public Herramienta actualizarImagen(Solicitante solicitante, Long id, String imagenUrl) {
        Herramienta herramienta = buscarPropia(solicitante, id);
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

    private Herramienta buscarPropia(Solicitante solicitante, Long id) {
        Herramienta herramienta = buscarPorId(id);
        controlAcceso.verificarPropietarioHerramienta(solicitante, herramienta);
        return herramienta;
    }

    private Long resolverProveedor(Solicitante solicitante, Long proveedorId) {
        if (solicitante.esProveedor()) {
            // El proveedor siempre registra a su propio nombre, sin importar lo que envíe en la petición.
            return solicitante.id();
        }
        if (!solicitante.esAdmin()) {
            throw new AccesoDenegadoException("Solo proveedores o administradores pueden registrar herramientas.");
        }
        if (proveedorId == null) {
            throw new DomainException("Debe indicar el proveedorId del dueño de la herramienta.");
        }
        Usuario proveedor = usuarioRepositoryPort.buscarPorId(proveedorId)
                .orElseThrow(() -> new DomainException("El proveedor no existe."));
        if (proveedor.getRol() != Rol.PROVEEDOR) {
            throw new DomainException("El usuario indicado no es un proveedor.");
        }
        return proveedorId;
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
