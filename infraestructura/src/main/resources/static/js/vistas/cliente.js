// Vistas del CLIENTE: panel, catálogo con búsqueda, historial de reservas y facturas.
import { api, conParametros } from '../api.js';
import { badge, barrasPorEstado, cargando, fmtFecha, fmtMoneda, hoyIso, html, imagenHerramienta, kpi, opciones, toastError, vacio } from '../ui.js';
import { cargarCategorias, encabezado, modalReservar, porId, vistaFacturas, vistaReservas } from './comun.js';

export async function inicio(contenedor, usuario) {
    cargando(contenedor);
    try {
        const [panel, herramientas] = await Promise.all([api.get('/panel'), api.get('/herramientas')]);
        const nombres = porId(herramientas);
        const total = Object.values(panel.reservasPorEstado).reduce((s, n) => s + n, 0);
        contenedor.innerHTML = html`${encabezado(`Hola, ${usuario.nombre}`, 'Resumen de sus alquileres.',
                html`<a class="btn btn-warning" href="#/catalogo"><i aria-hidden="true" class="bi bi-search me-1"></i>Buscar herramientas</a>`)}
            <div class="row g-3 mb-4">
                ${kpi('Reservas', total, 'calendar-check')}
                ${kpi('Por pagar', panel.reservasPorEstado.ACEPTADA || 0, 'credit-card', 'info')}
                ${kpi('Total pagado', fmtMoneda(panel.totalPagado), 'cash-coin', 'success')}
                ${kpi('Facturas', panel.facturas, 'receipt', 'secondary')}</div>
            ${panel.reservasPorEstado.ACEPTADA ? html`<div class="alert alert-info d-flex align-items-center gap-2">
                <i aria-hidden="true" class="bi bi-bell-fill"></i><div>Tiene reservas aceptadas por el proveedor esperando su pago.
                <a href="#/reservas" class="alert-link">Pagar ahora</a> (el plazo es de 1 hora).</div></div>` : ''}
            <div class="row g-3">
                <div class="col-lg-8"><div class="card border-0 shadow-sm h-100"><div class="card-body">
                    <h3 class="h6">Próximos alquileres</h3>
                    ${panel.proximasReservas.length ? html`<ul class="list-group list-group-flush">${panel.proximasReservas.map(r => html`
                        <li class="list-group-item d-flex justify-content-between align-items-center px-0">
                            <div><div class="fw-semibold">${nombres.get(r.herramientaId)?.nombre || `Herramienta #${r.herramientaId}`}</div>
                            <div class="small text-muted">${fmtFecha(r.fechaInicio)} → ${fmtFecha(r.fechaFin)}</div></div>${badge(r.estado)}</li>`)}</ul>`
                        : vacio('No tiene alquileres próximos.', 'calendar')}</div></div></div>
                <div class="col-lg-4"><div class="card border-0 shadow-sm h-100"><div class="card-body">
                    <h3 class="h6 mb-3">Sus reservas por estado</h3>${barrasPorEstado(panel.reservasPorEstado)}</div></div></div>
            </div>`;
    } catch (error) {
        contenedor.innerHTML = '';
        toastError(error);
    }
}

/** Catálogo: búsqueda por texto, categoría y disponibilidad (estado y, opcionalmente, rango de fechas). */
export async function catalogo(contenedor) {
    cargando(contenedor);
    let categorias;
    try {
        categorias = await cargarCategorias();
    } catch (error) {
        contenedor.innerHTML = '';
        return toastError(error);
    }
    const nombresCategorias = porId(categorias);
    contenedor.innerHTML = html`${encabezado('Catálogo de herramientas', 'Busque por categoría y disponibilidad, consulte precios y reserve.')}
        <form class="card border-0 shadow-sm mb-4" data-filtros><div class="card-body row g-2 align-items-end">
            <div class="col-md-3"><label class="form-label small" for="c-q">Buscar</label>
                <input type="search" class="form-control" id="c-q" name="q" placeholder="Taladro, andamio..."></div>
            <div class="col-md-3"><label class="form-label small" for="c-cat">Categoría</label>
                <select class="form-select" id="c-cat" name="categoriaId">${opciones(categorias, '', { vacia: 'Todas' })}</select></div>
            <div class="col-6 col-md-2"><label class="form-label small" for="c-ini">Desde</label>
                <input type="date" class="form-control" id="c-ini" name="fechaInicio" min="${hoyIso()}"></div>
            <div class="col-6 col-md-2"><label class="form-label small" for="c-fin">Hasta</label>
                <input type="date" class="form-control" id="c-fin" name="fechaFin" min="${hoyIso()}"></div>
            <div class="col-md-2"><div class="form-check mb-2"><input class="form-check-input" type="checkbox" id="c-disp" name="disponibles" checked>
                <label class="form-check-label small" for="c-disp">Solo disponibles</label></div></div>
        </div></form>
        <div data-resultados></div>`;
    const form = contenedor.querySelector('[data-filtros]');
    const resultados = contenedor.querySelector('[data-resultados]');
    let herramientas = [];

    async function buscar() {
        cargando(resultados);
        const f = Object.fromEntries(new FormData(form).entries());
        const conFechas = Boolean(f.fechaInicio && f.fechaFin);
        try {
            // Con fechas solo tiene sentido mostrar herramientas operativas.
            herramientas = await api.get(conParametros('/herramientas', {
                categoriaId: f.categoriaId, q: f.q.trim(), disponibles: Boolean(f.disponibles) || conFechas,
            }));
            if (conFechas) {
                // Solo las que no tienen reservas activas en ese rango.
                const libres = new Set((await api.get(conParametros('/reservas/disponibilidad', {
                    fechaInicio: f.fechaInicio, fechaFin: f.fechaFin,
                }))).map(h => h.id));
                herramientas = herramientas.filter(h => libres.has(h.id));
            }
            pintar(conFechas ? f : {});
        } catch (error) {
            resultados.innerHTML = '';
            toastError(error);
        }
    }

    function pintar(fechas) {
        resultados.innerHTML = herramientas.length ? html`
            <p class="text-muted small">${herramientas.length} herramienta(s)${fechas.fechaInicio
                ? ` libres del ${fmtFecha(fechas.fechaInicio)} al ${fmtFecha(fechas.fechaFin)}` : ''}.</p>
            <div class="row g-3">${herramientas.map(h => html`
            <div class="col-sm-6 col-lg-4 col-xxl-3"><div class="card h-100 border-0 shadow-sm herramienta-card">
                <a href="#" class="herramienta-enlace" data-detalle="${h.id}" aria-label="Ver detalle de ${h.nombre}">
                    ${imagenHerramienta(h)}<span class="herramienta-ver"><i aria-hidden="true" class="bi bi-arrows-angle-expand me-1"></i>Ver detalle</span></a>
                <div class="card-body d-flex flex-column">
                    <div class="d-flex justify-content-between gap-2">
                        <h3 class="h6 mb-1"><a href="#" class="text-reset text-decoration-none" data-detalle="${h.id}">${h.nombre}</a></h3>${badge(h.estado)}</div>
                    <div class="small text-muted mb-2"><i aria-hidden="true" class="bi bi-tag"></i> ${nombresCategorias.get(h.categoriaId)?.nombre || 'Sin categoría'}</div>
                    <p class="small flex-grow-1">${h.descripcion}</p>
                    <div class="d-flex justify-content-between align-items-center">
                        <div><span class="fs-5 fw-bold text-success">${fmtMoneda(h.tarifaDiaria)}</span><span class="small text-muted"> / día</span></div>
                        <button class="btn btn-warning btn-sm" data-reservar="${h.id}" ${h.estado === 'DISPONIBLE' ? '' : html`disabled`}>
                            <i aria-hidden="true" class="bi bi-calendar-plus me-1"></i>Reservar</button></div>
                </div></div></div>`)}</div>`
            : vacio('No hay herramientas que coincidan con la búsqueda.', 'search');
    }

    let espera;
    form.addEventListener('input', () => { clearTimeout(espera); espera = setTimeout(buscar, 300); });
    form.addEventListener('submit', evento => evento.preventDefault());
    resultados.addEventListener('click', evento => {
        // La foto, el nombre y el botón "Reservar" abren la ficha ampliada con la reserva por fechas.
        const boton = evento.target.closest('[data-reservar], [data-detalle]');
        if (!boton) return;
        evento.preventDefault();
        const herramienta = herramientas.find(h => h.id === Number(boton.dataset.reservar || boton.dataset.detalle));
        modalReservar(herramienta, () => { location.hash = '#/reservas'; }, {
            inicio: form.fechaInicio.value, fin: form.fechaFin.value,
            categoria: nombresCategorias.get(herramienta.categoriaId)?.nombre,
        });
    });
    await buscar();
    return undefined;
}

export const reservas = contenedor => vistaReservas(contenedor, {
    rol: 'CLIENTE', titulo: 'Mis reservas',
    subtitulo: 'Historial de alquileres y estado de las herramientas. Pague las reservas aceptadas antes de que venza el plazo.',
});

export const facturas = contenedor => vistaFacturas(contenedor, {
    titulo: 'Pagos y facturas', subtitulo: 'Comprobantes de sus pagos. Puede descargarlos en cualquier momento.',
});
