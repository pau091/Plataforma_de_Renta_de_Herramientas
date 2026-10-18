// Componentes compartidos por los paneles: reservas y sus acciones, inventario, mantenimientos y facturas.
import { api, conParametros, descargarFactura } from '../api.js';
import {
    abrirModal, badge, cargando, confirmar, diasAlquiler, fmtFecha, fmtHora, fmtInstante, fmtMoneda, hoyIso, html,
    imagenHerramienta, instante, opciones, raw, toast, toastError, vacio,
} from '../ui.js';

/** Plazo (minutos) para que el proveedor acepte o el cliente pague antes de que la reserva venza. */
export const PLAZO_MINUTOS = 60;

export const porId = lista => new Map(lista.map(item => [item.id, item]));

export const cargarCategorias = () => api.get('/categorias');

export function encabezado(titulo, subtitulo, acciones = '') {
    return html`<div class="d-flex flex-wrap justify-content-between align-items-end gap-2 mb-4">
        <div><h2 class="h3 mb-1">${titulo}</h2>${subtitulo ? html`<p class="text-muted mb-0">${subtitulo}</p>` : ''}</div>
        <div class="d-flex flex-wrap gap-2">${acciones}</div></div>`;
}

/** Ejecuta una acción, muestra el resultado y vuelve a cargar la vista. */
async function ejecutar(accion, mensaje, recargar) {
    try {
        await accion();
        if (mensaje) toast(mensaje);
        await recargar();
    } catch (error) {
        toastError(error);
    }
}

// ================================================================ reservas

function totalReserva(reserva, herramienta) {
    return herramienta ? herramienta.tarifaDiaria * diasAlquiler(reserva.fechaInicio, reserva.fechaFin) : null;
}

/** Aviso del plazo de 1 hora para las reservas que todavía pueden vencer. */
function aviso(reserva) {
    const referencia = reserva.estado === 'PENDIENTE' ? reserva.fechaCreacion
        : reserva.estado === 'ACEPTADA' ? reserva.fechaAceptacion : null;
    if (!referencia) return '';
    const limite = new Date(instante(referencia).getTime() + PLAZO_MINUTOS * 60000);
    const quien = reserva.estado === 'PENDIENTE' ? 'aceptarla' : 'pagarla';
    return limite > new Date()
        ? html`<div class="small text-danger"><i aria-hidden="true" class="bi bi-hourglass-split"></i> Plazo para ${quien}: ${fmtHora(limite)}</div>`
        : html`<div class="small text-muted"><i aria-hidden="true" class="bi bi-hourglass-bottom"></i> Plazo vencido</div>`;
}

function accionesReserva(reserva, rol) {
    const boton = (accion, texto, icono, color = 'outline-secondary') =>
        html`<button class="btn btn-sm btn-${color}" data-accion="${accion}" data-id="${reserva.id}" title="${texto}">
            <i aria-hidden="true" class="bi bi-${icono}"></i><span class="d-inline d-md-none d-xl-inline ms-1">${texto}</span></button>`;
    const factura = boton('factura', 'Factura', 'receipt');
    const botones = [];
    const e = reserva.estado;
    if (rol === 'CLIENTE' || rol === 'ADMIN') {
        if (e === 'PENDIENTE' && rol === 'CLIENTE') botones.push(boton('modificar', 'Cambiar fechas', 'calendar-event'));
        if (e === 'ACEPTADA' && rol === 'CLIENTE') botones.push(boton('pagar', 'Pagar', 'credit-card', 'success'));
    }
    if (rol === 'PROVEEDOR' || rol === 'ADMIN') {
        if (e === 'PENDIENTE') {
            botones.push(boton('aceptar', 'Aceptar', 'check-lg', 'success'), boton('rechazar', 'Rechazar', 'x-lg', 'outline-danger'));
        }
        if (e === 'CONFIRMADA') botones.push(boton('entregar', 'Registrar entrega', 'box-arrow-right', 'primary'));
        if (e === 'ENTREGADA') botones.push(boton('devolver', 'Registrar devolución', 'box-arrow-in-left', 'primary'));
    }
    if ((rol === 'CLIENTE' || rol === 'ADMIN') && (e === 'PENDIENTE' || e === 'ACEPTADA')) {
        botones.push(boton('cancelar', 'Cancelar', 'trash', 'outline-danger'));
    }
    if (['CONFIRMADA', 'ENTREGADA', 'FINALIZADA'].includes(e)) botones.push(factura);
    return html`<div class="d-flex flex-wrap gap-1 justify-content-end">${botones}</div>`;
}

/**
 * Lista de reservas con filtro por estado y las acciones que permite el rol.
 * nombresClientes: Map clienteId -> nombre (opcional).
 */
export async function vistaReservas(contenedor, { rol, titulo, subtitulo, estadoInicial = '', nombresClientes }) {
    contenedor.innerHTML = encabezado(titulo, subtitulo, html`
        <select class="form-select" data-filtro-estado aria-label="Filtrar por estado" style="min-width: 200px">
            ${opciones([
                { id: '', nombre: 'Todos los estados' }, { id: 'PENDIENTE', nombre: 'Pendientes' },
                { id: 'ACEPTADA', nombre: 'Aceptadas (por pagar)' }, { id: 'CONFIRMADA', nombre: 'Confirmadas (pagadas)' },
                { id: 'ENTREGADA', nombre: 'Entregadas' }, { id: 'FINALIZADA', nombre: 'Finalizadas' },
                { id: 'RECHAZADA', nombre: 'Rechazadas' }, { id: 'CANCELADA', nombre: 'Canceladas' },
                { id: 'VENCIDA', nombre: 'Vencidas' }], estadoInicial)}
        </select>
        ${rol === 'ADMIN' ? html`<button class="btn btn-outline-dark" data-accion="vencer" title="Ejecuta ya el vencimiento automático">
            <i aria-hidden="true" class="bi bi-hourglass-bottom me-1"></i>Vencer ahora</button>` : ''}`);
    const lista = document.createElement('div');
    contenedor.appendChild(lista);
    const filtro = contenedor.querySelector('[data-filtro-estado]');
    let herramientas = new Map();
    let reservas = [];

    async function recargar() {
        cargando(lista);
        try {
            const [listaHerramientas, listaReservas] = await Promise.all([
                api.get('/herramientas'), api.get(conParametros('/reservas', { estado: filtro.value })),
            ]);
            herramientas = porId(listaHerramientas);
            reservas = listaReservas.sort((a, b) => b.id - a.id);
            pintar();
        } catch (error) {
            lista.innerHTML = '';
            toastError(error);
        }
    }

    function pintar() {
        if (!reservas.length) {
            lista.innerHTML = vacio('No hay reservas con ese filtro.', 'calendar-x');
            return;
        }
        const mostrarCliente = rol !== 'CLIENTE';
        lista.innerHTML = html`<div class="card border-0 shadow-sm"><div class="table-responsive">
            <table class="table table-hover align-middle mb-0 tabla-apilada">
                <thead class="table-light"><tr><th>#</th><th>Herramienta</th>${mostrarCliente ? html`<th>Cliente</th>` : ''}
                    <th>Fechas</th><th class="text-end">Total</th><th>Estado</th><th class="text-end">Acciones</th></tr></thead>
                <tbody>${reservas.map(r => {
                    const h = herramientas.get(r.herramientaId);
                    // data-label: en pantallas pequeñas cada fila se muestra como tarjeta con la etiqueta de cada dato.
                    return html`<tr>
                        <td class="text-muted" data-label="Reserva"><div>#${r.id}</div></td>
                        <td data-label="Herramienta"><div><div class="fw-semibold">${h ? h.nombre : `Herramienta #${r.herramientaId}`}</div>
                            ${r.estadoDevolucion ? html`<div class="small">Devolución: ${badge(r.estadoDevolucion)}</div>` : ''}
                            ${r.observacionesDevolucion ? html`<div class="small text-muted">${r.observacionesDevolucion}</div>` : ''}</div></td>
                        ${mostrarCliente ? html`<td data-label="Cliente"><div>${nombresClientes?.get(r.clienteId) || `Cliente #${r.clienteId}`}</div></td>` : ''}
                        <td class="text-nowrap" data-label="Fechas"><div>${fmtFecha(r.fechaInicio)} → ${fmtFecha(r.fechaFin)}
                            <div class="small text-muted">${diasAlquiler(r.fechaInicio, r.fechaFin)} día(s)</div></div></td>
                        <td class="text-end text-nowrap" data-label="Total"><div>${fmtMoneda(totalReserva(r, h))}</div></td>
                        <td data-label="Estado"><div>${badge(r.estado)}${aviso(r)}</div></td>
                        <td data-label=""><div class="w-100">${accionesReserva(r, rol)}</div></td></tr>`;
                })}</tbody></table></div></div>`;
    }

    filtro.addEventListener('change', recargar);
    contenedor.addEventListener('click', evento => {
        const boton = evento.target.closest('[data-accion]');
        if (!boton) return;
        const reserva = reservas.find(r => r.id === Number(boton.dataset.id));
        manejarAccionReserva(boton.dataset.accion, reserva, herramientas, recargar);
    });
    await recargar();
}

export async function manejarAccionReserva(accion, reserva, herramientas, recargar) {
    const id = reserva?.id;
    switch (accion) {
        case 'vencer':
            return ejecutar(async () => {
                const vencidas = await api.post('/reservas/vencer');
                toast(`${vencidas.length} reserva(s) pasaron a VENCIDA.`, 'info');
            }, null, recargar);
        case 'aceptar':
            return ejecutar(() => api.post(`/reservas/${id}/aceptar`),
                `Reserva #${id} aceptada. El cliente tiene ${PLAZO_MINUTOS} minutos para pagarla.`, recargar);
        case 'rechazar':
            if (await confirmar(`¿Rechazar la solicitud #${id}?`, { textoConfirmar: 'Rechazar' })) {
                await ejecutar(() => api.post(`/reservas/${id}/rechazar`), `Reserva #${id} rechazada.`, recargar);
            }
            return;
        case 'entregar':
            if (await confirmar(`¿Registrar la entrega de la herramienta de la reserva #${id}?`,
                { titulo: 'Registrar entrega', textoConfirmar: 'Registrar entrega', color: 'primary' })) {
                await ejecutar(() => api.post(`/reservas/${id}/entregar`), `Entrega de la reserva #${id} registrada.`, recargar);
            }
            return;
        case 'cancelar':
            if (await confirmar(`¿Cancelar la reserva #${id}? Las fechas quedarán libres.`, { textoConfirmar: 'Cancelar reserva' })) {
                await ejecutar(() => api.del(`/reservas/${id}`), `Reserva #${id} cancelada.`, recargar);
            }
            return;
        case 'devolver':
            return modalDevolucion(reserva, recargar);
        case 'modificar':
            return modalFechas(reserva, herramientas.get(reserva.herramientaId), recargar);
        case 'pagar':
            return modalPago(reserva, herramientas.get(reserva.herramientaId), recargar);
        case 'factura':
            try {
                return modalFactura(await api.get(`/facturas/reserva/${id}`));
            } catch (error) {
                return toastError(error);
            }
        default:
            return undefined;
    }
}

function camposFechas(inicio = '', fin = '') {
    return html`<div class="row g-3">
        <div class="col-sm-6"><label class="form-label" for="f-inicio">Desde</label>
            <input type="date" class="form-control" id="f-inicio" name="fechaInicio" min="${hoyIso()}" value="${inicio}" required></div>
        <div class="col-sm-6"><label class="form-label" for="f-fin">Hasta (incluido)</label>
            <input type="date" class="form-control" id="f-fin" name="fechaFin" min="${hoyIso()}" value="${fin}" required></div>
    </div>
    <div class="alert alert-light border mt-3 mb-0" data-resumen></div>`;
}

/** Recalcula días y total mientras el usuario cambia las fechas. */
function enlazarResumen(form, tarifa) {
    const resumen = form.querySelector('[data-resumen]');
    const actualizar = () => {
        const dias = diasAlquiler(form.fechaInicio.value, form.fechaFin.value);
        form.fechaFin.min = form.fechaInicio.value || hoyIso();
        resumen.innerHTML = dias > 0
            ? html`<div class="d-flex justify-content-between"><span>${dias} día(s) × ${fmtMoneda(tarifa)}</span>
                <strong>${fmtMoneda(dias * tarifa)}</strong></div>`
            : html`<span class="text-muted">Seleccione un rango de fechas válido.</span>`;
    };
    form.addEventListener('input', actualizar);
    actualizar();
}

function validarRango(datos) {
    if (diasAlquiler(datos.fechaInicio, datos.fechaFin) < 1) {
        throw new Error('La fecha de inicio no puede ser posterior a la fecha fin.');
    }
}

/** Reservar una herramienta (CLIENTE). Verifica la disponibilidad antes de enviar la solicitud. */
export function modalReservar(herramienta, alTerminar, { inicio = '', fin = '' } = {}) {
    abrirModal({
        titulo: `Reservar: ${herramienta.nombre}`,
        textoConfirmar: 'Enviar solicitud',
        cuerpo: html`<div class="d-flex gap-3 mb-3">${imagenHerramienta(herramienta, 'herramienta-mini')}
            <div><div class="fw-semibold">${herramienta.nombre}</div><div class="text-muted small">${herramienta.descripcion}</div>
            <div class="text-success fw-bold">${fmtMoneda(herramienta.tarifaDiaria)} / día</div></div></div>
            ${camposFechas(inicio, fin)}
            <p class="small text-muted mt-3 mb-0"><i aria-hidden="true" class="bi bi-info-circle"></i> La solicitud queda pendiente hasta que el
            proveedor la acepte (tiene ${PLAZO_MINUTOS} minutos). Después tendrá ${PLAZO_MINUTOS} minutos para pagarla y
            confirmar el alquiler.</p>`,
        alAbrir: form => enlazarResumen(form, herramienta.tarifaDiaria),
        alConfirmar: async (form, datos) => {
            validarRango(datos);
            const consulta = await api.get(conParametros(`/reservas/disponibilidad/${herramienta.id}`, datos));
            if (!consulta.disponible) throw new Error('La herramienta ya está reservada en esas fechas. Elija otras.');
            const reserva = await api.post('/reservas', { herramientaId: herramienta.id, ...datos });
            toast(`Solicitud #${reserva.id} enviada. Le avisaremos en "Mis reservas" cuando el proveedor responda.`);
            alTerminar?.();
        },
    });
}

function modalFechas(reserva, herramienta, recargar) {
    abrirModal({
        titulo: `Cambiar fechas de la reserva #${reserva.id}`,
        cuerpo: camposFechas(reserva.fechaInicio, reserva.fechaFin),
        alAbrir: form => enlazarResumen(form, herramienta?.tarifaDiaria || 0),
        alConfirmar: async (form, datos) => {
            validarRango(datos);
            await api.put(`/reservas/${reserva.id}`, datos);
            toast(`Fechas de la reserva #${reserva.id} actualizadas.`);
            await recargar();
        },
    });
}

function modalPago(reserva, herramienta, recargar) {
    const dias = diasAlquiler(reserva.fechaInicio, reserva.fechaFin);
    const monto = herramienta.tarifaDiaria * dias;
    abrirModal({
        titulo: `Pagar la reserva #${reserva.id}`,
        textoConfirmar: `Pagar ${fmtMoneda(monto)}`,
        colorConfirmar: 'success',
        cuerpo: html`<ul class="list-group mb-3">
                <li class="list-group-item d-flex justify-content-between"><span>Herramienta</span><strong>${herramienta.nombre}</strong></li>
                <li class="list-group-item d-flex justify-content-between"><span>Periodo</span>
                    <span>${fmtFecha(reserva.fechaInicio)} → ${fmtFecha(reserva.fechaFin)} (${dias} día(s))</span></li>
                <li class="list-group-item d-flex justify-content-between"><span>Tarifa diaria</span><span>${fmtMoneda(herramienta.tarifaDiaria)}</span></li>
                <li class="list-group-item d-flex justify-content-between fs-5"><span>Total</span><strong>${fmtMoneda(monto)}</strong></li></ul>
            <label class="form-label" for="p-metodo">Método de pago</label>
            <select class="form-select" id="p-metodo" name="metodoPago" required>
                ${opciones([{ id: 'TARJETA', nombre: 'Tarjeta de crédito o débito' }, { id: 'TRANSFERENCIA', nombre: 'Transferencia (PSE)' },
                            { id: 'EFECTIVO', nombre: 'Efectivo en punto aliado' }], 'TARJETA')}
            </select>
            <p class="small text-muted mt-3 mb-0"><i aria-hidden="true" class="bi bi-shield-check"></i> Pago simulado: al confirmar, la reserva queda
            CONFIRMADA automáticamente y se genera su factura.</p>`,
        alConfirmar: async (form, datos) => {
            const pago = await api.post('/pagos', { reservaId: reserva.id, monto, metodoPago: datos.metodoPago });
            const factura = await api.get(`/facturas/pago/${pago.id}`);
            toast(`Pago aprobado. Reserva confirmada y factura ${factura.numero} generada.`);
            await recargar();
            modalFactura(factura);
        },
    });
}

function modalDevolucion(reserva, recargar) {
    const opcion = (valor, texto, ayuda, marcado = false) => html`<div class="form-check mb-2">
        <input class="form-check-input" type="radio" name="estadoEquipo" id="dev-${valor}" value="${valor}" ${marcado ? raw('checked') : ''} required>
        <label class="form-check-label" for="dev-${valor}"><strong>${texto}</strong><div class="small text-muted">${ayuda}</div></label></div>`;
    abrirModal({
        titulo: `Devolución de la reserva #${reserva.id}`,
        textoConfirmar: 'Registrar devolución',
        cuerpo: html`<p class="mb-2">¿En qué estado se recibió la herramienta?</p>
            ${opcion('BUENO', 'Buen estado', 'La herramienta queda disponible para nuevos alquileres.', true)}
            ${opcion('DANADO', 'Dañada', 'Pasa a mantenimiento y se abre un reporte de daño.')}
            ${opcion('PERDIDO', 'Perdida', 'Queda fuera de servicio.')}
            <label class="form-label mt-2" for="dev-obs">Observaciones</label>
            <textarea class="form-control" id="dev-obs" name="observaciones" rows="3" maxlength="1000"
                placeholder="Describa el daño o la situación"></textarea>`,
        alConfirmar: async (form, datos) => {
            await api.post(`/reservas/${reserva.id}/devolver`, datos);
            const extra = datos.estadoEquipo === 'DANADO' ? ' Se abrió un mantenimiento por el daño.'
                : datos.estadoEquipo === 'PERDIDO' ? ' La herramienta quedó fuera de servicio.' : '';
            toast(`Devolución registrada; la reserva quedó finalizada.${extra}`);
            await recargar();
        },
    });
}

export function modalFactura(f) {
    const nodo = abrirModal({
        titulo: `Factura ${f.numero}`,
        soloLectura: true,
        tamano: 'modal-lg',
        cuerpo: html`${f.estado === 'ANULADA' ? html`<div class="alert alert-danger">Factura anulada: el pago fue reembolsado.</div>` : ''}
            <div class="row g-3 mb-3">
                <div class="col-sm-6"><div class="text-muted small">Cliente</div><div class="fw-semibold">${f.clienteNombre}</div>
                    <div class="small">${f.clienteEmail}</div></div>
                <div class="col-sm-6 text-sm-end"><div class="text-muted small">Emitida</div><div>${fmtInstante(f.fechaEmision)}</div>
                    <div>${badge(f.estado)}</div></div></div>
            <div class="table-responsive"><table class="table">
                <thead class="table-light"><tr><th>Herramienta</th><th>Periodo</th><th class="text-end">Días</th>
                    <th class="text-end">Tarifa</th><th class="text-end">Subtotal</th></tr></thead>
                <tbody><tr><td>${f.herramientaNombre}</td><td>${fmtFecha(f.fechaInicio)} → ${fmtFecha(f.fechaFin)}</td>
                    <td class="text-end">${f.dias}</td><td class="text-end">${fmtMoneda(f.tarifaDiaria)}</td>
                    <td class="text-end">${fmtMoneda(f.subtotal)}</td></tr></tbody>
                <tfoot><tr><th colspan="4" class="text-end">Total (${f.metodoPago})</th><th class="text-end">${fmtMoneda(f.total)}</th></tr></tfoot>
            </table></div>
            <button type="button" class="btn btn-warning" data-descargar><i aria-hidden="true" class="bi bi-download me-1"></i>Descargar factura</button>`,
    });
    nodo.querySelector('[data-descargar]').addEventListener('click', () => descargarFactura(f.id, f.numero).catch(toastError));
}

// ================================================================ facturas

export async function vistaFacturas(contenedor, { titulo, subtitulo }) {
    contenedor.innerHTML = encabezado(titulo, subtitulo);
    const lista = document.createElement('div');
    contenedor.appendChild(lista);
    cargando(lista);
    let facturas;
    try {
        facturas = (await api.get('/facturas')).sort((a, b) => b.id - a.id);
    } catch (error) {
        lista.innerHTML = '';
        return toastError(error);
    }
    const vigentes = facturas.filter(f => f.estado !== 'ANULADA');
    lista.innerHTML = html`<div class="row g-3 mb-4">
            ${kpiSimple('Facturas', facturas.length, 'receipt')}
            ${kpiSimple('Total facturado', fmtMoneda(vigentes.reduce((s, f) => s + f.total, 0)), 'cash-coin')}
            ${kpiSimple('Anuladas', facturas.length - vigentes.length, 'x-circle')}</div>
        ${facturas.length ? html`<div class="card border-0 shadow-sm"><div class="table-responsive">
            <table class="table table-hover align-middle mb-0"><thead class="table-light"><tr><th>Número</th><th>Emitida</th>
                <th>Cliente</th><th>Herramienta</th><th>Periodo</th><th class="text-end">Total</th><th>Estado</th><th></th></tr></thead>
            <tbody>${facturas.map(f => html`<tr>
                <td class="fw-semibold">${f.numero}</td><td class="text-nowrap">${fmtInstante(f.fechaEmision)}</td>
                <td>${f.clienteNombre}</td><td>${f.herramientaNombre}</td>
                <td class="text-nowrap">${fmtFecha(f.fechaInicio)} → ${fmtFecha(f.fechaFin)}</td>
                <td class="text-end text-nowrap">${fmtMoneda(f.total)}<div class="small text-muted">${f.metodoPago}</div></td>
                <td>${badge(f.estado)}</td>
                <td class="text-end text-nowrap"><button class="btn btn-sm btn-outline-secondary" data-ver="${f.id}" title="Ver"><i aria-hidden="true" class="bi bi-eye"></i></button>
                    <button class="btn btn-sm btn-warning" data-descargar="${f.id}" title="Descargar"><i aria-hidden="true" class="bi bi-download"></i></button></td>
            </tr>`)}</tbody></table></div></div>` : vacio('Aún no hay facturas.', 'receipt')}`;
    const porIdFactura = porId(facturas);
    lista.addEventListener('click', evento => {
        const ver = evento.target.closest('[data-ver]');
        const descargar = evento.target.closest('[data-descargar]');
        if (ver) modalFactura(porIdFactura.get(Number(ver.dataset.ver)));
        if (descargar) {
            const f = porIdFactura.get(Number(descargar.dataset.descargar));
            descargarFactura(f.id, f.numero).catch(toastError);
        }
    });
    return undefined;
}

function kpiSimple(titulo, valor, icono) {
    return html`<div class="col-sm-4"><div class="card border-0 shadow-sm"><div class="card-body">
        <div class="text-muted small"><i aria-hidden="true" class="bi bi-${icono} me-1"></i>${titulo}</div><div class="fs-4 fw-bold">${valor}</div></div></div></div>`;
}

// ================================================================ herramientas (PROVEEDOR y ADMIN)

export async function vistaInventario(contenedor, { admin }) {
    contenedor.innerHTML = encabezado(admin ? 'Herramientas' : 'Mis herramientas',
        admin ? 'Inventario de todos los proveedores.' : 'Su inventario, con estado, disponibilidad y costos.',
        html`<input type="search" class="form-control" placeholder="Buscar..." data-buscar aria-label="Buscar herramienta" style="min-width: 180px">
            <button class="btn btn-warning" data-nueva><i aria-hidden="true" class="bi bi-plus-lg me-1"></i>Nueva herramienta</button>`);
    const lista = document.createElement('div');
    contenedor.appendChild(lista);
    const buscar = contenedor.querySelector('[data-buscar]');
    let herramientas = [];
    let categorias = new Map();
    let proveedores = [];

    async function recargar() {
        cargando(lista);
        try {
            const [h, c, p] = await Promise.all([
                api.get(admin ? '/herramientas' : '/herramientas/mias'), cargarCategorias(),
                admin ? api.get('/usuarios/proveedores') : Promise.resolve([]),
            ]);
            herramientas = h;
            categorias = porId(c);
            proveedores = p;
            pintar();
        } catch (error) {
            lista.innerHTML = '';
            toastError(error);
        }
    }

    function pintar() {
        const texto = buscar.value.trim().toLowerCase();
        const visibles = herramientas.filter(h => !texto || `${h.nombre} ${h.descripcion}`.toLowerCase().includes(texto));
        const nombresProveedores = porId(proveedores);
        if (!visibles.length) {
            lista.innerHTML = vacio(herramientas.length ? 'Ninguna herramienta coincide con la búsqueda.'
                : 'Todavía no hay herramientas. Registre la primera con "Nueva herramienta".', 'tools');
            return;
        }
        lista.innerHTML = html`<div class="row g-3">${visibles.map(h => html`
            <div class="col-sm-6 col-lg-4 col-xxl-3"><div class="card h-100 border-0 shadow-sm herramienta-card">
                ${imagenHerramienta(h)}
                <div class="card-body d-flex flex-column">
                    <div class="d-flex justify-content-between gap-2"><h3 class="h6 mb-1">${h.nombre}</h3>${badge(h.estado)}</div>
                    <div class="small text-muted mb-2">${categorias.get(h.categoriaId)?.nombre || 'Sin categoría'}
                        ${admin ? html` · ${nombresProveedores.get(h.proveedorId)?.nombre || `Proveedor #${h.proveedorId}`}` : ''}</div>
                    <p class="small flex-grow-1">${h.descripcion}</p>
                    <div class="fw-bold text-success mb-2">${fmtMoneda(h.tarifaDiaria)} / día</div>
                    <div class="d-flex gap-1 flex-wrap">
                        <button class="btn btn-sm btn-outline-secondary" data-accion="editar" data-id="${h.id}"><i aria-hidden="true" class="bi bi-pencil"></i> Editar</button>
                        <button class="btn btn-sm btn-outline-secondary" data-accion="imagen" data-id="${h.id}"><i aria-hidden="true" class="bi bi-image"></i> Imagen</button>
                        <div class="dropdown">
                            <button class="btn btn-sm btn-outline-secondary dropdown-toggle" data-bs-toggle="dropdown" aria-expanded="false">Estado</button>
                            <ul class="dropdown-menu">${['DISPONIBLE', 'MANTENIMIENTO', 'FUERA_DE_SERVICIO'].map(e => html`
                                <li><button class="dropdown-item" data-accion="estado" data-estado="${e}" data-id="${h.id}" ${e === h.estado ? raw('disabled') : ''}>${badge(e)}</button></li>`)}
                            </ul></div>
                        <button class="btn btn-sm btn-outline-danger ms-auto" data-accion="eliminar" data-id="${h.id}" title="Eliminar"><i aria-hidden="true" class="bi bi-trash"></i></button>
                    </div></div></div></div>`)}</div>`;
    }

    function formulario(h = {}) {
        return html`<div class="row g-3">
            ${admin && !h.id ? html`<div class="col-12"><label class="form-label" for="h-prov">Proveedor</label>
                <select class="form-select" id="h-prov" name="proveedorId" required>${opciones(proveedores.filter(p => p.activo), '', { vacia: 'Seleccione...' })}</select></div>` : ''}
            <div class="col-12"><label class="form-label" for="h-nombre">Nombre</label>
                <input class="form-control" id="h-nombre" name="nombre" maxlength="150" value="${h.nombre || ''}" required></div>
            <div class="col-12"><label class="form-label" for="h-desc">Descripción</label>
                <textarea class="form-control" id="h-desc" name="descripcion" rows="3">${h.descripcion || ''}</textarea></div>
            <div class="col-sm-6"><label class="form-label" for="h-cat">Categoría</label>
                <select class="form-select" id="h-cat" name="categoriaId" required>${opciones([...categorias.values()], h.categoriaId, { vacia: 'Seleccione...' })}</select></div>
            <div class="col-sm-6"><label class="form-label" for="h-tarifa">Tarifa diaria (COP)</label>
                <input type="number" class="form-control" id="h-tarifa" name="tarifaDiaria" min="1" step="0.01" value="${h.tarifaDiaria || ''}" required></div>
            ${h.id ? '' : html`<div class="col-12"><label class="form-label" for="h-img">Imagen (opcional)</label>
                <input type="file" class="form-control" id="h-img" name="archivo" accept="image/jpeg,image/png,image/webp,image/gif">
                <div class="form-text">JPG, PNG, WEBP o GIF de máximo 5 MB.</div></div>`}
        </div>`;
    }

    function datosHerramienta(datos) {
        return {
            ...(datos.proveedorId ? { proveedorId: Number(datos.proveedorId) } : {}),
            categoriaId: Number(datos.categoriaId), nombre: datos.nombre.trim(),
            descripcion: datos.descripcion.trim(), tarifaDiaria: Number(datos.tarifaDiaria),
        };
    }

    async function subirImagen(id, archivo) {
        const formulario = new FormData();
        formulario.append('archivo', archivo);
        return api.subir(`/herramientas/${id}/imagen`, formulario);
    }

    contenedor.addEventListener('input', evento => { if (evento.target === buscar) pintar(); });
    contenedor.addEventListener('click', async evento => {
        if (evento.target.closest('[data-nueva]')) {
            abrirModal({
                titulo: 'Nueva herramienta', textoConfirmar: 'Registrar', cuerpo: formulario(),
                alConfirmar: async (form, datos) => {
                    const creada = await api.post('/herramientas', datosHerramienta(datos));
                    const archivo = form.archivo.files[0];
                    if (archivo) {
                        try {
                            await subirImagen(creada.id, archivo);
                        } catch (error) {
                            toastError(error);
                        }
                    }
                    toast(`Herramienta "${creada.nombre}" registrada.`);
                    await recargar();
                },
            });
            return;
        }
        const boton = evento.target.closest('[data-accion]');
        if (!boton) return;
        const h = herramientas.find(x => x.id === Number(boton.dataset.id));
        switch (boton.dataset.accion) {
            case 'editar':
                abrirModal({
                    titulo: `Editar: ${h.nombre}`, cuerpo: formulario(h),
                    alConfirmar: async (form, datos) => {
                        await api.put(`/herramientas/${h.id}`, datosHerramienta(datos));
                        toast('Herramienta actualizada.');
                        await recargar();
                    },
                });
                break;
            case 'imagen':
                abrirModal({
                    titulo: `Imagen de ${h.nombre}`, textoConfirmar: 'Subir imagen',
                    cuerpo: html`<div class="text-center mb-3" data-vista-previa>${imagenHerramienta(h, 'herramienta-preview')}</div>
                        <input type="file" class="form-control" name="archivo" accept="image/jpeg,image/png,image/webp,image/gif" required>
                        <div class="form-text">JPG, PNG, WEBP o GIF de máximo 5 MB. Reemplaza la imagen actual.</div>`,
                    alAbrir: form => form.archivo.addEventListener('change', () => {
                        const archivo = form.archivo.files[0];
                        if (archivo) {
                            form.querySelector('[data-vista-previa]').innerHTML =
                                html`<img class="herramienta-preview" alt="Vista previa" src="${URL.createObjectURL(archivo)}">`;
                        }
                    }),
                    alConfirmar: async form => {
                        await subirImagen(h.id, form.archivo.files[0]);
                        toast('Imagen actualizada.');
                        await recargar();
                    },
                });
                break;
            case 'estado':
                await ejecutar(() => api.patch(`/herramientas/${h.id}/estado`, { estado: boton.dataset.estado }),
                    'Estado actualizado.', recargar);
                break;
            case 'eliminar':
                if (await confirmar(`¿Eliminar "${h.nombre}"? Si ya tiene reservas o mantenimientos no se podrá eliminar; en ese caso márquela fuera de servicio.`,
                    { textoConfirmar: 'Eliminar' })) {
                    await ejecutar(() => api.del(`/herramientas/${h.id}`), 'Herramienta eliminada.', recargar);
                }
                break;
            default:
        }
    });
    await recargar();
}

// ================================================================ mantenimientos (PROVEEDOR y ADMIN)

export async function vistaMantenimientos(contenedor, { admin }) {
    contenedor.innerHTML = encabezado('Mantenimientos',
        'Reparaciones en curso y cerradas. Mientras un mantenimiento está abierto, la herramienta no se puede reservar.',
        html`<select class="form-select" data-filtro aria-label="Filtrar mantenimientos" style="min-width: 160px">
                ${opciones([{ id: 'abiertos', nombre: 'Abiertos' }, { id: 'cerrados', nombre: 'Cerrados' }, { id: '', nombre: 'Todos' }], 'abiertos')}</select>
            <button class="btn btn-warning" data-nuevo><i aria-hidden="true" class="bi bi-wrench me-1"></i>Abrir mantenimiento</button>`);
    const lista = document.createElement('div');
    contenedor.appendChild(lista);
    const filtro = contenedor.querySelector('[data-filtro]');
    let mantenimientos = [];
    let herramientas = [];

    async function recargar() {
        cargando(lista);
        try {
            [mantenimientos, herramientas] = await Promise.all([
                api.get('/mantenimientos'), api.get(admin ? '/herramientas' : '/herramientas/mias'),
            ]);
            pintar();
        } catch (error) {
            lista.innerHTML = '';
            toastError(error);
        }
    }

    function pintar() {
        const nombres = porId(herramientas);
        const visibles = mantenimientos
            .filter(m => !filtro.value || (filtro.value === 'abiertos') === (m.fechaFin === null))
            .sort((a, b) => b.id - a.id);
        lista.innerHTML = visibles.length ? html`<div class="card border-0 shadow-sm"><div class="table-responsive">
            <table class="table table-hover align-middle mb-0"><thead class="table-light"><tr><th>#</th><th>Herramienta</th>
                <th>Problema</th><th>Inicio</th><th>Fin</th><th class="text-end">Costo</th><th></th></tr></thead>
            <tbody>${visibles.map(m => html`<tr>
                <td class="text-muted">${m.id}</td><td class="fw-semibold">${nombres.get(m.herramientaId)?.nombre || `#${m.herramientaId}`}</td>
                <td>${m.descripcionProblema}${m.reservaId ? html`<div class="small text-muted">Por devolución de la reserva #${m.reservaId}</div>` : ''}</td>
                <td class="text-nowrap">${fmtInstante(m.fechaInicio)}</td>
                <td class="text-nowrap">${m.fechaFin ? fmtInstante(m.fechaFin) : html`<span class="badge text-bg-warning">Abierto</span>`}</td>
                <td class="text-end">${fmtMoneda(m.costoReparacion)}</td>
                <td class="text-end">${m.fechaFin ? '' : html`<button class="btn btn-sm btn-success" data-cerrar="${m.id}">
                    <i aria-hidden="true" class="bi bi-check2-circle me-1"></i>Cerrar</button>`}</td></tr>`)}</tbody></table></div></div>`
            : vacio('No hay mantenimientos con ese filtro.', 'wrench');
    }

    filtro.addEventListener('change', pintar);
    contenedor.addEventListener('click', evento => {
        if (evento.target.closest('[data-nuevo]')) {
            const operativas = herramientas.filter(h => h.estado !== 'FUERA_DE_SERVICIO');
            abrirModal({
                titulo: 'Abrir mantenimiento', textoConfirmar: 'Abrir',
                cuerpo: html`<label class="form-label" for="m-herr">Herramienta</label>
                    <select class="form-select mb-3" id="m-herr" name="herramientaId" required>${opciones(operativas, '', { vacia: 'Seleccione...' })}</select>
                    <label class="form-label" for="m-desc">Descripción del problema</label>
                    <textarea class="form-control mb-3" id="m-desc" name="descripcionProblema" rows="3" required></textarea>
                    <label class="form-label" for="m-costo">Costo estimado (opcional)</label>
                    <input type="number" class="form-control" id="m-costo" name="costoReparacion" min="0" step="0.01">`,
                alConfirmar: async (form, datos) => {
                    await api.post('/mantenimientos', {
                        herramientaId: Number(datos.herramientaId), descripcionProblema: datos.descripcionProblema.trim(),
                        costoReparacion: datos.costoReparacion === '' ? null : Number(datos.costoReparacion),
                    });
                    toast('Mantenimiento abierto: la herramienta quedó en mantenimiento.');
                    filtro.value = 'abiertos';
                    await recargar();
                },
            });
            return;
        }
        const cerrar = evento.target.closest('[data-cerrar]');
        if (!cerrar) return;
        const m = mantenimientos.find(x => x.id === Number(cerrar.dataset.cerrar));
        abrirModal({
            titulo: `Cerrar mantenimiento #${m.id}`, textoConfirmar: 'Cerrar mantenimiento', colorConfirmar: 'success',
            cuerpo: html`<p>${m.descripcionProblema}</p><label class="form-label" for="m-final">Costo final de la reparación</label>
                <input type="number" class="form-control" id="m-final" name="costoReparacion" min="0" step="0.01" value="${m.costoReparacion ?? ''}" required>`,
            alConfirmar: async (form, datos) => {
                await api.post(`/mantenimientos/${m.id}/cerrar`, { costoReparacion: Number(datos.costoReparacion) });
                toast('Mantenimiento cerrado. Si la herramienta estaba en mantenimiento, ya está disponible.');
                await recargar();
            },
        });
    });
    await recargar();
}
