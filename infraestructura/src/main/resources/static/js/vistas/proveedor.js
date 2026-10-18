// Vistas del PROVEEDOR: panel con solicitudes, inventario, reservas, mantenimientos y facturación.
import { api } from '../api.js';
import { barrasPorEstado, cargando, diasAlquiler, fmtFecha, fmtMoneda, html, kpi, toastError, vacio } from '../ui.js';
import {
    encabezado, manejarAccionReserva, porId, vistaFacturas, vistaInventario, vistaMantenimientos, vistaReservas,
} from './comun.js';

/** El proveedor no consulta usuarios: los nombres de sus clientes salen de las facturas de sus herramientas. */
async function nombresDeClientes() {
    const facturas = await api.get('/facturas');
    return new Map(facturas.map(f => [f.clienteId, f.clienteNombre]));
}

export async function inicio(contenedor, usuario) {
    cargando(contenedor);
    try {
        const [panel, herramientas, clientes] = await Promise.all([
            api.get('/panel'), api.get('/herramientas/mias'), nombresDeClientes(),
        ]);
        const nombres = porId(herramientas);
        contenedor.innerHTML = html`${encabezado(`Hola, ${usuario.nombre}`, 'Resumen de su inventario y sus alquileres.',
                html`<a class="btn btn-warning" href="#/herramientas"><i aria-hidden="true" class="bi bi-plus-lg me-1"></i>Publicar herramienta</a>`)}
            <div class="row g-3 mb-4">
                ${kpi('Herramientas', panel.herramientas, 'tools')}
                ${kpi('Solicitudes pendientes', panel.solicitudesPendientes.length, 'bell', 'danger')}
                ${kpi('Ingresos', fmtMoneda(panel.ingresos), 'cash-coin', 'success')}
                ${kpi('Mantenimientos abiertos', panel.mantenimientosAbiertos, 'wrench', 'secondary')}</div>
            <div class="row g-3">
                <div class="col-lg-8"><div class="card border-0 shadow-sm h-100"><div class="card-body">
                    <h3 class="h6">Solicitudes por responder <span class="text-muted fw-normal small">(tiene 1 hora para aceptarlas)</span></h3>
                    ${panel.solicitudesPendientes.length ? html`<ul class="list-group list-group-flush">${panel.solicitudesPendientes.map(r => {
                        const h = nombres.get(r.herramientaId);
                        return html`<li class="list-group-item px-0 d-flex flex-wrap justify-content-between align-items-center gap-2">
                            <div><div class="fw-semibold">${h?.nombre || `Herramienta #${r.herramientaId}`}</div>
                                <div class="small text-muted">${clientes.get(r.clienteId) || `Cliente #${r.clienteId}`} ·
                                ${fmtFecha(r.fechaInicio)} → ${fmtFecha(r.fechaFin)} ·
                                ${fmtMoneda(h ? h.tarifaDiaria * diasAlquiler(r.fechaInicio, r.fechaFin) : null)}</div></div>
                            <div class="d-flex gap-1">
                                <button class="btn btn-sm btn-success" data-accion="aceptar" data-id="${r.id}"><i aria-hidden="true" class="bi bi-check-lg"></i> Aceptar</button>
                                <button class="btn btn-sm btn-outline-danger" data-accion="rechazar" data-id="${r.id}"><i aria-hidden="true" class="bi bi-x-lg"></i> Rechazar</button></div></li>`;
                    })}</ul>` : vacio('No hay solicitudes pendientes.', 'bell-slash')}</div></div></div>
                <div class="col-lg-4 d-flex flex-column gap-3">
                    <div class="card border-0 shadow-sm"><div class="card-body"><h3 class="h6 mb-3">Inventario por estado</h3>
                        ${barrasPorEstado(panel.herramientasPorEstado)}</div></div>
                    <div class="card border-0 shadow-sm"><div class="card-body"><h3 class="h6 mb-3">Reservas por estado</h3>
                        ${barrasPorEstado(panel.reservasPorEstado)}</div></div></div>
            </div>`;
        contenedor.querySelectorAll('[data-accion]').forEach(boton => boton.addEventListener('click', () => {
            const reserva = panel.solicitudesPendientes.find(r => r.id === Number(boton.dataset.id));
            manejarAccionReserva(boton.dataset.accion, reserva, nombres, () => inicio(contenedor, usuario));
        }));
    } catch (error) {
        contenedor.innerHTML = '';
        toastError(error);
    }
}

export const herramientas = contenedor => vistaInventario(contenedor, { admin: false });

export async function reservas(contenedor) {
    let clientes = new Map();
    try {
        clientes = await nombresDeClientes();
    } catch (error) {
        toastError(error);
    }
    return vistaReservas(contenedor, {
        rol: 'PROVEEDOR', titulo: 'Reservas de mis herramientas', estadoInicial: 'PENDIENTE', nombresClientes: clientes,
        subtitulo: 'Acepte o rechace solicitudes, registre entregas y confirme devoluciones reportando daños.',
    });
}

export const mantenimientos = contenedor => vistaMantenimientos(contenedor, { admin: false });

export const facturacion = contenedor => vistaFacturas(contenedor, {
    titulo: 'Facturación', subtitulo: 'Facturas y comprobantes generados automáticamente por los alquileres de sus herramientas.',
});
