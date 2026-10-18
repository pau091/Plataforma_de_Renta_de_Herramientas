package com.example.tomcat.service;

import com.example.tomcat.exception.AccesoDenegadoException;
import com.example.tomcat.exception.ConflictoException;
import com.example.tomcat.exception.DomainException;
import com.example.tomcat.exception.RecursoNoEncontradoException;
import com.example.tomcat.model.Herramienta;
import com.example.tomcat.model.Rol;
import com.example.tomcat.model.Usuario;
import com.example.tomcat.port.in.GestionarHerramientaUseCase;
import com.example.tomcat.port.in.dto.Solicitante;
import com.example.tomcat.port.out.AlmacenamientoImagenPort;
import com.example.tomcat.repository.CategoriaRepositoryPort;
import com.example.tomcat.repository.HerramientaRepositoryPort;
import com.example.tomcat.repository.MantenimientoRepositoryPort;
import com.example.tomcat.repository.ReservaRepositoryPort;
import com.example.tomcat.repository.UsuarioRepositoryPort;
import com.example.tomcat.valueobject.Dinero;

import java.math.BigDecimal;
import java.util.List;

public class GestionarHerramientaService implements GestionarHerramientaUseCase {

    private final HerramientaRepositoryPort herramientaRepositoryPort;
    private final UsuarioRepositoryPort usuarioRepositoryPort;
    private final CategoriaRepositoryPort categoriaRepositoryPort;
    private final ReservaRepositoryPort reservaRepositoryPort;
    private final MantenimientoRepositoryPort mantenimientoRepositoryPort;
    private final AlmacenamientoImagenPort almacenamientoImagenPort;
    private final ControlAcceso controlAcceso;

    public GestionarHerramientaService(
            HerramientaRepositoryPort herramientaRepositoryPort,
            UsuarioRepositoryPort usuarioRepositoryPort,
            CategoriaRepositoryPort categoriaRepositoryPort,
            ReservaRepositoryPort reservaRepositoryPort,
            MantenimientoRepositoryPort mantenimientoRepositoryPort,
            AlmacenamientoImagenPort almacenamientoImagenPort,
            ControlAcceso controlAcceso
    ) {
        this.herramientaRepositoryPort = herramientaRepositoryPort;
        this.usuarioRepositoryPort = usuarioRepositoryPort;
        this.categoriaRepositoryPort = categoriaRepositoryPort;
        this.reservaRepositoryPort = reservaRepositoryPort;
        this.mantenimientoRepositoryPort = mantenimientoRepositoryPort;
        this.almacenamientoImagenPort = almacenamientoImagenPort;
        this.controlAcceso = controlAcceso;
    }

    @Override
    public Herramienta crear(Solicitante solicitante, Long proveedorId, Long categoriaId, String nombre, String descripcion,
                             BigDecimal tarifaDiaria, String imagenUrl) {
        Long dueno = resolverProveedor(solicitante, proveedorId);
        validarCategoria(categoriaId);
        validarNombre(nombre);

        Herramienta herramienta = new Herramienta(
                null,
                dueno,
                categoriaId,
                nombre.trim(),
                descripcion,
                validarTarifa(tarifaDiaria),
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
        BigDecimal tarifa = tarifaDiaria != null ? validarTarifa(tarifaDiaria) : herramienta.getTarifaDiaria();

        herramienta.actualizar(categoria, nombreActualizado, descripcion, tarifa, estado, imagenUrl);
        return herramientaRepositoryPort.guardar(herramienta);
    }

    /**
     * Solo se eliminan herramientas sin historial. Si ya tiene reservas o mantenimientos se debe
     * marcar como FUERA_DE_SERVICIO, para no perder reservas, pagos ni facturas asociadas.
     */
    @Override
    public void eliminar(Solicitante solicitante, Long id) {
        buscarPropia(solicitante, id);
        if (!reservaRepositoryPort.listarPorHerramienta(id).isEmpty()
                || !mantenimientoRepositoryPort.listarPorHerramienta(id).isEmpty()) {
            throw new ConflictoException("La herramienta tiene reservas o mantenimientos registrados y no se puede eliminar. "
                    + "Cambie su estado a FUERA_DE_SERVICIO.");
        }
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
        String anterior = herramienta.getImagenUrl();
        herramienta.actualizarImagen(imagenUrl.trim());
        Herramienta guardada = herramientaRepositoryPort.guardar(herramienta);
        almacenamientoImagenPort.eliminarSiEsPropia(anterior);
        return guardada;
    }

    @Override
    public Herramienta subirImagen(Solicitante solicitante, Long id, byte[] contenido) {
        Herramienta herramienta = buscarPropia(solicitante, id);
        String extension = ValidadorImagen.extensionValida(contenido);

        String anterior = herramienta.getImagenUrl();
        String url = almacenamientoImagenPort.guardar(contenido, extension);
        herramienta.actualizarImagen(url);
        Herramienta guardada;
        try {
            guardada = herramientaRepositoryPort.guardar(herramienta);
        } catch (RuntimeException e) {
            // Si la base de datos falla, no se deja un archivo huérfano en disco.
            almacenamientoImagenPort.eliminarSiEsPropia(url);
            throw e;
        }
        // La imagen anterior se borra solo después de guardar la nueva, para no dejar la herramienta sin imagen.
        almacenamientoImagenPort.eliminarSiEsPropia(anterior);
        return guardada;
    }

    @Override
    public List<Herramienta> listarPorProveedor(Long proveedorId) {
        return herramientaRepositoryPort.listarPorProveedor(proveedorId);
    }

    @Override
    public List<Herramienta> buscar(Long categoriaId, Long proveedorId, boolean soloDisponibles, String texto) {
        if (categoriaId != null) {
            categoriaRepositoryPort.buscarPorId(categoriaId)
                    .orElseThrow(() -> new RecursoNoEncontradoException("La categoría no existe."));
        }
        return herramientaRepositoryPort.buscar(categoriaId, proveedorId, soloDisponibles ? "DISPONIBLE" : null, texto);
    }

    @Override
    public Herramienta buscarPorId(Long id) {
        return herramientaRepositoryPort.buscarPorId(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("La herramienta no existe."));
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
                .orElseThrow(() -> new RecursoNoEncontradoException("El proveedor no existe."));
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
                .orElseThrow(() -> new RecursoNoEncontradoException("La categoría no existe."));
    }

    private void validarNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            throw new DomainException("El nombre de la herramienta es obligatorio.");
        }
    }

    private BigDecimal validarTarifa(BigDecimal tarifaDiaria) {
        BigDecimal tarifa = new Dinero(tarifaDiaria).getMonto();
        if (tarifa.signum() <= 0) {
            throw new DomainException("La tarifa diaria debe ser mayor que cero.");
        }
        return tarifa;
    }
}
