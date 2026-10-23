// Vistas del ADMIN: panel global, usuarios, inventario, categorías, alquileres, pagos, incidencias y reportes.
import { api, conParametros } from '../api.js';
import {
    abrirModal, badge, barrasPorEstado, cargando, confirmar, fmtInstante, fmtMoneda, html, kpi, opciones,
    raw, toast, toastError, vacio,
} from '../ui.js';
import {
    cargarCategorias, encabezado, modalFactura, vistaFacturas, vistaInventario, vistaMantenimientos, vistaReservas,
} from './comun.js';

/** Carga los datos y pinta la vista; si algo falla, muestra el error. */
async function conDatos(contenedor, cargar, pintar) {
    cargando(contenedor);
    try {
        pintar(await cargar());
    } catch (error) {
        contenedor.innerHTML = '';
        toastError(error);
    }
}

const tabla = (cabeceras, filas) => html`<div class="card border-0 shadow-sm"><div class="table-responsive">
    <table class="table table-hover align-middle mb-0"><thead class="table-light"><tr>${cabeceras.map(c =>
        typeof c === 'string' ? html`<th>${c}</th>` : html`<th class="${c.clase}">${c.texto}</th>`)}</tr></thead>
    <tbody>${filas}</tbody></table></div></div>`;

// ================================================================ panel

export function inicio(contenedor, usuario) {
    return conDatos(contenedor, () => Promise.all([api.get('/panel'), api.get('/reportes/incidencias')]), ([panel, incidencias]) => {
        const abiertas = incidencias.items.filter(i => i.mantenimientoAbierto || i.estadoDevolucion === 'PERDIDO');
        contenedor.innerHTML = html`${encabezado(`Hola, ${usuario.nombre}`, 'Visión global de la plataforma.')}
            <div class="row g-3 mb-4">
                ${kpi('Clientes', panel.clientes, 'people')}
                ${kpi('Proveedores', panel.proveedores, 'shop', 'primary')}
                ${kpi('Ingresos', fmtMoneda(panel.ingresos), 'cash-coin', 'success')}
                ${kpi('Pagos completados', panel.pagosCompletados, 'credit-card', 'info')}</div>
            <div class="row g-3">
                <div class="col-lg-4"><div class="card border-0 shadow-sm h-100"><div class="card-body">
                    <h3 class="h6 mb-3">Alquileres por estado</h3>${barrasPorEstado(panel.reservasPorEstado)}
                    <a href="#/reservas" class="small">Ver historial de alquileres</a></div></div></div>
                <div class="col-lg-4"><div class="card border-0 shadow-sm h-100"><div class="card-body">
                    <h3 class="h6 mb-3">Estado de los equipos</h3>${barrasPorEstado(panel.herramientasPorEstado)}
                    <div class="small text-muted">Mantenimientos abiertos: <strong>${panel.mantenimientosAbiertos}</strong></div>
                    <a href="#/reportes" class="small">Ver reportes</a></div></div></div>
                <div class="col-lg-4"><div class="card border-0 shadow-sm h-100"><div class="card-body">
                    <h3 class="h6 mb-3">Incidencias por atender</h3>
                    ${abiertas.length ? html`<ul class="list-group list-group-flush">${abiertas.slice(0, 5).map(i => html`
                        <li class="list-group-item px-0"><div class="d-flex justify-content-between"><span class="fw-semibold">${i.herramientaNombre}</span>
                            ${badge(i.estadoDevolucion)}</div><div class="small text-muted">${i.observaciones || 'Sin observaciones'}</div></li>`)}</ul>`
                        : vacio('Sin incidencias abiertas.', 'shield-check')}
                    <a href="#/incidencias" class="small">Gestionar incidencias</a></div></div></div>
            </div>`;
    });
}

// ================================================================ usuarios

/** Estado real de la cuenta: activa, inactiva, suspendida (hasta cuándo) o baneada, con el motivo de la sanción. */
function estadoCuenta(u) {
    const motivo = u.motivoSancion ? html`<div class="small text-muted text-wrap" style="max-width: 260px">${u.motivoSancion}</div>` : '';
    switch (u.estadoCuenta) {
        case 'SUSPENDIDA':
            return html`<span class="badge text-bg-warning">Suspendida</span>
                <div class="small">hasta ${fmtInstante(u.suspendidoHasta)}</div>${motivo}`;
        case 'BANEADA':
            return html`<span class="badge text-bg-danger">Baneada</span>${motivo}`;
        case 'INACTIVA':
            return html`<span class="badge text-bg-secondary">Inactiva</span>`;
        default:
            return html`<span class="badge text-bg-success">Activa</span>`;
    }
}

const sancionado = u => u.estadoCuenta === 'SUSPENDIDA' || u.estadoCuenta === 'BANEADA';

const campoMotivo = ejemplo => html`<div class="col-12"><label class="form-label" for="s-motivo">Motivo</label>
    <textarea class="form-control" id="s-motivo" name="motivo" rows="2" maxlength="255" placeholder="${ejemplo}" required></textarea>
    <div class="form-text">El usuario lo verá al ser expulsado y al intentar iniciar sesión.</div></div>`;

const sesionesCerradas = n => (n === 1 ? 'Se cerró 1 sesión abierta al instante.'
    : n > 1 ? `Se cerraron ${n} sesiones abiertas al instante.` : 'No tenía sesiones abiertas.');

export async function usuarios(contenedor) {
    contenedor.innerHTML = encabezado('Usuarios', 'Registre y administre proveedores y clientes: edite, suspenda, banee o elimine.', html`
        <select class="form-select" data-rol aria-label="Tipo de usuario" style="min-width: 160px">
            ${opciones([{ id: 'PROVEEDOR', nombre: 'Proveedores' }, { id: 'CLIENTE', nombre: 'Clientes' }], 'PROVEEDOR')}</select>
        <button class="btn btn-warning" data-nuevo><i aria-hidden="true" class="bi bi-person-plus me-1"></i>Nuevo usuario</button>`);
    const lista = document.createElement('div');
    contenedor.appendChild(lista);
    const rol = contenedor.querySelector('[data-rol]');
    let lista_usuarios = [];

    const recargar = () => conDatos(lista, () => api.get(conParametros('/usuarios', { rol: rol.value })), datos => {
        lista_usuarios = datos;
        lista.innerHTML = tabla(['Nombre', 'Correo', 'Registro', 'Estado', { texto: 'Acciones', clase: 'text-end' }],
            datos.map(u => html`<tr>
                <td class="fw-semibold">${u.nombre}</td><td>${u.email}</td><td class="text-nowrap">${fmtInstante(u.createdAt)}</td>
                <td>${estadoCuenta(u)}</td>
                <td class="text-end text-nowrap">
                    <button class="btn btn-sm btn-outline-secondary" data-accion="editar" data-id="${u.id}" title="Editar"><i aria-hidden="true" class="bi bi-pencil"></i></button>
                    ${sancionado(u)
                        ? html`<button class="btn btn-sm btn-outline-success" data-accion="reactivar" data-id="${u.id}" title="Levantar la sanción">
                            <i aria-hidden="true" class="bi bi-unlock"></i></button>`
                        : html`<button class="btn btn-sm btn-outline-warning" data-accion="suspender" data-id="${u.id}" title="Suspender">
                            <i aria-hidden="true" class="bi bi-pause-circle"></i></button>
                          <button class="btn btn-sm btn-outline-danger" data-accion="banear" data-id="${u.id}" title="Banear">
                            <i aria-hidden="true" class="bi bi-ban"></i></button>`}
                    <button class="btn btn-sm btn-outline-${u.activo ? 'warning' : 'success'}" data-accion="activo" data-id="${u.id}"
                        title="${u.activo ? 'Desactivar' : 'Activar'}"><i aria-hidden="true" class="bi bi-${u.activo ? 'person-slash' : 'person-check'}"></i></button>
                    <button class="btn btn-sm btn-outline-danger" data-accion="eliminar" data-id="${u.id}" title="Eliminar"><i aria-hidden="true" class="bi bi-trash"></i></button></td></tr>`));
    });

    const formulario = (u = {}) => html`<div class="row g-3">
        <div class="col-12"><label class="form-label" for="u-nombre">Nombre</label>
            <input class="form-control" id="u-nombre" name="nombre" maxlength="100" value="${u.nombre || ''}" required></div>
        <div class="col-12"><label class="form-label" for="u-email">Correo</label>
            <input type="email" class="form-control" id="u-email" name="email" maxlength="150" value="${u.email || ''}" required></div>
        <div class="col-12"><label class="form-label" for="u-pass">${u.id ? 'Nueva contraseña (opcional)' : 'Contraseña'}</label>
            <input type="password" class="form-control" id="u-pass" name="password" minlength="8" maxlength="72" autocomplete="new-password" ${u.id ? '' : raw('required')}>
            <div class="form-text">Mínimo 8 caracteres.</div></div>
        ${u.id ? '' : html`<div class="col-12"><label class="form-label" for="u-rol">Rol</label>
            <select class="form-select" id="u-rol" name="rol">${opciones([{ id: 'PROVEEDOR', nombre: 'Proveedor' }, { id: 'CLIENTE', nombre: 'Cliente' }], rol.value)}</select></div>`}</div>`;

    rol.addEventListener('change', recargar);
    contenedor.addEventListener('click', async evento => {
        if (evento.target.closest('[data-nuevo]')) {
            abrirModal({
                titulo: 'Nuevo usuario', textoConfirmar: 'Registrar', cuerpo: formulario(),
                alConfirmar: async (form, datos) => {
                    const creado = await api.post('/usuarios', datos);
                    toast(`${creado.rol === 'PROVEEDOR' ? 'Proveedor' : 'Cliente'} "${creado.nombre}" registrado.`);
                    rol.value = creado.rol;
                    await recargar();
                },
            });
            return;
        }
        const boton = evento.target.closest('[data-accion]');
        if (!boton) return;
        const u = lista_usuarios.find(x => x.id === Number(boton.dataset.id));
        try {
            if (boton.dataset.accion === 'editar') {
                abrirModal({
                    titulo: `Editar a ${u.nombre}`, cuerpo: formulario(u),
                    alConfirmar: async (form, datos) => {
                        await api.put(`/usuarios/${u.id}`, { nombre: datos.nombre, email: datos.email, password: datos.password || undefined });
                        toast('Usuario actualizado.');
                        await recargar();
                    },
                });
            } else if (boton.dataset.accion === 'activo') {
                const texto = u.activo ? 'Al desactivarlo no podrá iniciar sesión y su sesión actual se cerrará.' : 'Podrá volver a iniciar sesión.';
                if (await confirmar(`¿${u.activo ? 'Desactivar' : 'Activar'} a ${u.nombre}? ${texto}`,
                    { textoConfirmar: u.activo ? 'Desactivar' : 'Activar', color: u.activo ? 'danger' : 'success' })) {
                    await api.put(`/usuarios/${u.id}`, { activo: !u.activo });
                    toast(`Usuario ${u.activo ? 'desactivado' : 'activado'}.`);
                    await recargar();
                }
            } else if (boton.dataset.accion === 'suspender') {
                abrirModal({
                    titulo: `Suspender a ${u.nombre}`, textoConfirmar: 'Suspender', colorConfirmar: 'warning',
                    cuerpo: html`<p class="small text-muted">No podrá iniciar sesión durante el plazo, y si está usando la plataforma
                        será expulsado de inmediato. Al terminar el plazo podrá volver a entrar.</p>
                        <div class="row g-3"><div class="col-sm-5"><label class="form-label" for="s-dias">Días</label>
                            <input type="number" class="form-control" id="s-dias" name="dias" min="1" max="365" value="7" required></div>
                        ${campoMotivo('Cancelaciones reiteradas sin aviso')}</div>`,
                    alConfirmar: async (form, datos) => {
                        const r = await api.post(`/usuarios/${u.id}/suspension`, { dias: Number(datos.dias), motivo: datos.motivo });
                        toast(`${u.nombre} suspendido por ${datos.dias} día(s). ${sesionesCerradas(r.tokensRevocados)}`, 'warning');
                        await recargar();
                    },
                });
            } else if (boton.dataset.accion === 'banear') {
                abrirModal({
                    titulo: `Banear a ${u.nombre}`, textoConfirmar: 'Banear', colorConfirmar: 'danger',
                    cuerpo: html`<p class="small text-muted">Bloqueo <strong>indefinido</strong>: no podrá entrar hasta que un administrador
                        lo reactive, y si está usando la plataforma será expulsado de inmediato.</p>
                        <div class="row g-3">${campoMotivo('Fraude en pagos')}</div>`,
                    alConfirmar: async (form, datos) => {
                        const r = await api.post(`/usuarios/${u.id}/baneo`, { motivo: datos.motivo });
                        toast(`${u.nombre} baneado. ${sesionesCerradas(r.tokensRevocados)}`, 'danger');
                        await recargar();
                    },
                });
            } else if (boton.dataset.accion === 'reactivar') {
                if (await confirmar(`¿Levantar la ${u.estadoCuenta === 'BANEADA' ? 'prohibición' : 'suspensión'} de ${u.nombre}? `
                    + 'Podrá volver a iniciar sesión.', { titulo: 'Reactivar usuario', textoConfirmar: 'Reactivar', color: 'success' })) {
                    await api.post(`/usuarios/${u.id}/reactivacion`);
                    toast(`${u.nombre} puede volver a iniciar sesión.`);
                    await recargar();
                }
            } else if (boton.dataset.accion === 'eliminar') {
                if (await confirmar(`¿Eliminar a ${u.nombre}? Si tiene historial no se podrá eliminar; desactívelo en su lugar.`,
                    { textoConfirmar: 'Eliminar' })) {
                    await api.del(`/usuarios/${u.id}`);
                    toast('Usuario eliminado.');
                    await recargar();
                }
            }
        } catch (error) {
            toastError(error);
        }
    });
    await recargar();
}

// ================================================================ inventario

export const herramientas = contenedor => vistaInventario(contenedor, { admin: true });
export const mantenimientos = contenedor => vistaMantenimientos(contenedor, { admin: true });

export async function categorias(contenedor) {
    contenedor.innerHTML = encabezado('Categorías', 'Clasificación del catálogo de herramientas.',
        html`<button class="btn btn-warning" data-nueva><i aria-hidden="true" class="bi bi-plus-lg me-1"></i>Nueva categoría</button>`);
    const lista = document.createElement('div');
    contenedor.appendChild(lista);
    let categorias = [];

    const recargar = () => conDatos(lista, () => Promise.all([cargarCategorias(), api.get('/herramientas')]), ([c, h]) => {
        categorias = c;
        const conteo = h.reduce((m, x) => m.set(x.categoriaId, (m.get(x.categoriaId) || 0) + 1), new Map());
        lista.innerHTML = tabla(['Nombre', 'Descripción', { texto: 'Herramientas', clase: 'text-end' }, { texto: 'Acciones', clase: 'text-end' }],
            c.map(cat => html`<tr><td class="fw-semibold">${cat.nombre}</td><td class="text-muted">${cat.descripcion}</td>
                <td class="text-end">${conteo.get(cat.id) || 0}</td>
                <td class="text-end text-nowrap"><button class="btn btn-sm btn-outline-secondary" data-editar="${cat.id}" title="Editar"><i aria-hidden="true" class="bi bi-pencil"></i></button>
                    <button class="btn btn-sm btn-outline-danger" data-eliminar="${cat.id}" title="Eliminar"><i aria-hidden="true" class="bi bi-trash"></i></button></td></tr>`));
    });

    const formulario = (c = {}) => html`<label class="form-label" for="c-nombre">Nombre</label>
        <input class="form-control mb-3" id="c-nombre" name="nombre" maxlength="100" value="${c.nombre || ''}" required>
        <label class="form-label" for="c-desc">Descripción</label>
        <textarea class="form-control" id="c-desc" name="descripcion" rows="3">${c.descripcion || ''}</textarea>`;

    contenedor.addEventListener('click', async evento => {
        const editar = evento.target.closest('[data-editar]');
        const eliminar = evento.target.closest('[data-eliminar]');
        if (evento.target.closest('[data-nueva]') || editar) {
            const c = editar ? categorias.find(x => x.id === Number(editar.dataset.editar)) : {};
            abrirModal({
                titulo: c.id ? `Editar categoría ${c.nombre}` : 'Nueva categoría', cuerpo: formulario(c),
                alConfirmar: async (form, datos) => {
                    await (c.id ? api.put(`/categorias/${c.id}`, datos) : api.post('/categorias', datos));
                    toast(c.id ? 'Categoría actualizada.' : 'Categoría creada.');
                    await recargar();
                },
            });
        } else if (eliminar) {
            const c = categorias.find(x => x.id === Number(eliminar.dataset.eliminar));
            if (await confirmar(`¿Eliminar la categoría "${c.nombre}"? Solo es posible si no tiene herramientas.`, { textoConfirmar: 'Eliminar' })) {
                try {
                    await api.del(`/categorias/${c.id}`);
                    toast('Categoría eliminada.');
                    await recargar();
                } catch (error) {
                    toastError(error);
                }
            }
        }
    });
    await recargar();
}

// ================================================================ alquileres

export async function reservas(contenedor) {
    let clientes = new Map();
    try {
        clientes = new Map((await api.get('/usuarios/clientes')).map(u => [u.id, u.nombre]));
    } catch (error) {
        toastError(error);
    }
    return vistaReservas(contenedor, {
        rol: 'ADMIN', titulo: 'Historial de alquileres', nombresClientes: clientes,
        subtitulo: 'Todas las reservas de la plataforma. Puede gestionarlas como el proveedor o cancelarlas como el cliente.',
    });
}

export function incidencias(contenedor) {
    return conDatos(contenedor, () => api.get('/reportes/incidencias'), reporte => {
        contenedor.innerHTML = html`${encabezado('Incidencias con equipos', 'Devoluciones con equipo dañado o perdido y su reparación.')}
            <div class="row g-3 mb-4">${kpi('Incidencias', reporte.total, 'exclamation-triangle', 'danger')}
                ${kpi('Equipos dañados', reporte.danadas, 'hammer', 'warning')}${kpi('Equipos perdidos', reporte.perdidas, 'question-octagon', 'secondary')}</div>
            ${reporte.items.length ? tabla(['Reserva', 'Herramienta', 'Cliente', 'Devolución', 'Observaciones', 'Equipo hoy', 'Reparación', ''],
                reporte.items.map(i => html`<tr>
                    <td class="text-muted">#${i.reservaId}<div class="small">${fmtInstante(i.fechaDevolucion)}</div></td>
                    <td class="fw-semibold">${i.herramientaNombre}</td><td>${i.clienteNombre}</td><td>${badge(i.estadoDevolucion)}</td>
                    <td class="small">${i.observaciones || '—'}</td><td>${badge(i.estadoHerramienta)}</td>
                    <td class="text-nowrap">${i.mantenimientoId ? (i.mantenimientoAbierto ? html`<span class="badge text-bg-warning">En reparación</span>`
                        : html`Cerrada · ${fmtMoneda(i.costoReparacion)}`) : '—'}</td>
                    <td class="text-end">${i.mantenimientoAbierto ? html`<button class="btn btn-sm btn-success" data-cerrar="${i.mantenimientoId}">
                        <i aria-hidden="true" class="bi bi-check2-circle me-1"></i>Cerrar reparación</button>` : ''}</td></tr>`))
                : vacio('No hay incidencias registradas.', 'shield-check')}`;
        contenedor.querySelectorAll('[data-cerrar]').forEach(boton => boton.addEventListener('click', () => abrirModal({
            titulo: 'Cerrar reparación', textoConfirmar: 'Cerrar', colorConfirmar: 'success',
            cuerpo: html`<label class="form-label" for="i-costo">Costo final de la reparación</label>
                <input type="number" class="form-control" id="i-costo" name="costoReparacion" min="0" step="0.01" required>`,
            alConfirmar: async (form, datos) => {
                await api.post(`/mantenimientos/${boton.dataset.cerrar}/cerrar`, { costoReparacion: Number(datos.costoReparacion) });
                toast('Reparación cerrada: la herramienta vuelve a estar disponible.');
                await incidencias(contenedor);
            },
        })));
    });
}

// ================================================================ pagos y facturación

export function pagos(contenedor) {
    return conDatos(contenedor, () => Promise.all([api.get('/pagos'), api.get('/facturas')]), ([listaPagos, facturas]) => {
        const facturaDePago = new Map(facturas.map(f => [f.pagoId, f]));
        const completados = listaPagos.filter(p => p.estadoPago === 'COMPLETADO');
        const reembolsados = listaPagos.filter(p => p.estadoPago === 'REEMBOLSADO');
        // Contenedor nuevo en cada repintado: los listeners del anterior se descartan con él.
        const raiz = document.createElement('div');
        contenedor.replaceChildren(raiz);
        raiz.innerHTML = html`${encabezado('Pagos', 'Monitoreo de pagos: corrija el método registrado o reembolse un pago antes de la entrega.')}
            <div class="row g-3 mb-4">${kpi('Recaudado', fmtMoneda(completados.reduce((s, p) => s + p.monto, 0)), 'cash-coin', 'success')}
                ${kpi('Pagos completados', completados.length, 'check-circle', 'info')}
                ${kpi('Reembolsados', reembolsados.length, 'arrow-counterclockwise', 'secondary')}
                ${kpi('Reembolsado', fmtMoneda(reembolsados.reduce((s, p) => s + p.monto, 0)), 'cash', 'danger')}</div>
            ${tabla(['#', 'Fecha', 'Cliente', 'Herramienta', 'Factura', { texto: 'Monto', clase: 'text-end' }, 'Método', 'Estado', { texto: 'Acciones', clase: 'text-end' }],
                listaPagos.sort((a, b) => b.id - a.id).map(p => {
                    const f = facturaDePago.get(p.id);
                    return html`<tr><td class="text-muted">${p.id}</td><td class="text-nowrap">${fmtInstante(p.fechaPago)}</td>
                        <td>${f?.clienteNombre || '—'}</td><td>${f?.herramientaNombre || `Reserva #${p.reservaId}`}</td>
                        <td>${f ? html`<a href="#" data-factura="${p.id}">${f.numero}</a>` : '—'}</td>
                        <td class="text-end text-nowrap">${fmtMoneda(p.monto)}</td><td>${p.metodoPago}</td><td>${badge(p.estadoPago)}</td>
                        <td class="text-end text-nowrap">${p.estadoPago === 'COMPLETADO' ? html`
                            <button class="btn btn-sm btn-outline-secondary" data-metodo="${p.id}" title="Corregir método"><i aria-hidden="true" class="bi bi-pencil"></i></button>
                            <button class="btn btn-sm btn-outline-danger" data-reembolsar="${p.id}" title="Reembolsar"><i aria-hidden="true" class="bi bi-arrow-counterclockwise"></i></button>` : ''}</td></tr>`;
                }))}`;
        const pago = id => listaPagos.find(p => p.id === Number(id));
        raiz.addEventListener('click', async evento => {
            const factura = evento.target.closest('[data-factura]');
            const metodo = evento.target.closest('[data-metodo]');
            const reembolsar = evento.target.closest('[data-reembolsar]');
            if (factura) {
                evento.preventDefault();
                modalFactura(facturaDePago.get(Number(factura.dataset.factura)));
            } else if (metodo) {
                const p = pago(metodo.dataset.metodo);
                abrirModal({
                    titulo: `Corregir el método del pago #${p.id}`,
                    cuerpo: html`<label class="form-label" for="pm-metodo">Método de pago</label>
                        <select class="form-select" id="pm-metodo" name="metodoPago">${opciones([{ id: 'TARJETA', nombre: 'Tarjeta' },
                            { id: 'TRANSFERENCIA', nombre: 'Transferencia' }, { id: 'EFECTIVO', nombre: 'Efectivo' }], p.metodoPago)}</select>
                        <div class="form-text">El monto no cambia: lo fija la tarifa. La factura se corrige también.</div>`,
                    alConfirmar: async (form, datos) => {
                        await api.put(`/pagos/${p.id}`, datos);
                        toast('Método de pago corregido.');
                        await pagos(contenedor);
                    },
                });
            } else if (reembolsar) {
                const p = pago(reembolsar.dataset.reembolsar);
                if (await confirmar(`¿Reembolsar ${fmtMoneda(p.monto)} del pago #${p.id}? La reserva quedará cancelada y la factura anulada. `
                    + 'Solo es posible si la herramienta aún no se ha entregado.', { textoConfirmar: 'Reembolsar' })) {
                    try {
                        await api.del(`/pagos/${p.id}`);
                        toast('Pago reembolsado: la reserva quedó cancelada y su factura anulada.');
                        await pagos(contenedor);
                    } catch (error) {
                        toastError(error);
                    }
                }
            }
        });
    });
}

export const facturas = contenedor => vistaFacturas(contenedor, {
    titulo: 'Facturación global', subtitulo: 'Todas las facturas emitidas por la plataforma.',
});

// ================================================================ reportes

export function reportes(contenedor) {
    return conDatos(contenedor, () => Promise.all([
        api.get('/reportes/ingresos'), api.get('/reportes/rentabilidad'), api.get('/reportes/herramientas-mas-alquiladas'),
        api.get('/reportes/clientes-frecuentes'), api.get('/reportes/disponibilidad'),
    ]), ([ingresos, rentabilidad, masAlquiladas, frecuentes, disponibilidad]) => {
        const margen = valor => (valor === null || valor === undefined ? '—' : `${valor.toFixed(1)} %`);
        contenedor.innerHTML = html`${encabezado('Reportes', 'Rentabilidad y uso de las herramientas.')}
            <div class="row g-3 mb-4">${kpi('Ingresos', fmtMoneda(rentabilidad.ingresosTotales), 'cash-coin', 'success')}
                ${kpi('Costo de mantenimiento', fmtMoneda(rentabilidad.costoMantenimientoTotal), 'wrench', 'danger')}
                ${kpi('Utilidad', fmtMoneda(rentabilidad.utilidadTotal), 'graph-up-arrow', 'primary')}
                ${kpi('Margen', margen(rentabilidad.margenPorcentaje), 'percent', 'warning')}</div>
            <div class="row g-3 mb-4">
                <div class="col-lg-6"><h3 class="h6">Ingresos por método de pago</h3>
                    ${tabla(['Método', { texto: 'Pagos', clase: 'text-end' }, { texto: 'Total', clase: 'text-end' }],
                        ingresos.porMetodo.map(m => html`<tr><td>${m.metodoPago}</td><td class="text-end">${m.cantidad}</td>
                            <td class="text-end">${fmtMoneda(m.total)}</td></tr>`))}</div>
                <div class="col-lg-6"><h3 class="h6">Disponibilidad y estado de los equipos</h3>
                    ${tabla(['Estado', { texto: 'Herramientas', clase: 'text-end' }], html`
                        <tr><td>${badge('DISPONIBLE')}</td><td class="text-end">${disponibilidad.disponibles}</td></tr>
                        <tr><td>${badge('MANTENIMIENTO')}</td><td class="text-end">${disponibilidad.enMantenimiento}</td></tr>
                        <tr><td>${badge('FUERA_DE_SERVICIO')}</td><td class="text-end">${disponibilidad.fueraDeServicio}</td></tr>
                        <tr class="fw-semibold"><td>Total</td><td class="text-end">${disponibilidad.total}</td></tr>`)}</div></div>
            <h3 class="h6">Rentabilidad por herramienta</h3>
            <div class="mb-4">${tabla(['Herramienta', 'Proveedor', { texto: 'Alquileres', clase: 'text-end' }, { texto: 'Ingresos', clase: 'text-end' },
                { texto: 'Mantenimiento', clase: 'text-end' }, { texto: 'Utilidad', clase: 'text-end' }, { texto: 'Margen', clase: 'text-end' }],
                rentabilidad.items.map(i => html`<tr><td class="fw-semibold">${i.nombre}</td><td>${i.proveedorNombre}</td>
                    <td class="text-end">${i.alquileresPagados}</td><td class="text-end">${fmtMoneda(i.ingresos)}</td>
                    <td class="text-end">${fmtMoneda(i.costoMantenimiento)}</td>
                    <td class="text-end ${i.utilidad < 0 ? 'text-danger' : ''}">${fmtMoneda(i.utilidad)}</td>
                    <td class="text-end">${margen(i.margenPorcentaje)}</td></tr>`))}</div>
            <div class="row g-3">
                <div class="col-lg-6"><h3 class="h6">Herramientas más alquiladas</h3>
                    ${tabla(['Herramienta', 'Estado', { texto: 'Alquileres', clase: 'text-end' }, { texto: 'Ingresos', clase: 'text-end' }],
                        masAlquiladas.items.slice(0, 10).map(i => html`<tr><td>${i.nombre}</td><td>${badge(i.estado)}</td>
                            <td class="text-end">${i.alquileres}</td><td class="text-end">${fmtMoneda(i.ingresos)}</td></tr>`))}</div>
                <div class="col-lg-6"><h3 class="h6">Clientes frecuentes</h3>
                    ${tabla(['Cliente', { texto: 'Reservas', clase: 'text-end' }, { texto: 'Total pagado', clase: 'text-end' }],
                        frecuentes.items.slice(0, 10).map(i => html`<tr><td>${i.nombre}<div class="small text-muted">${i.email}</div></td>
                            <td class="text-end">${i.reservas}</td><td class="text-end">${fmtMoneda(i.totalPagado)}</td></tr>`))}</div></div>`;
    });
}
