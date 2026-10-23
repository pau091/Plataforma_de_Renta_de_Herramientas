// Utilidades de interfaz: plantillas seguras, formatos, insignias de estado, avisos y ventanas modales.

/** Texto que ya es HTML confiable (generado por la propia aplicación) y no se debe escapar. */
class HtmlSeguro {
    constructor(valor) { this.valor = valor; }
    toString() { return this.valor; }
}

export const raw = valor => new HtmlSeguro(valor);

export function esc(valor) {
    if (valor === null || valor === undefined) return '';
    return String(valor)
        .replaceAll('&', '&amp;')
        .replaceAll('<', '&lt;')
        .replaceAll('>', '&gt;')
        .replaceAll('"', '&quot;')
        .replaceAll("'", '&#39;');
}

function convertir(valor) {
    if (valor instanceof HtmlSeguro) return valor.valor;
    if (Array.isArray(valor)) return valor.map(convertir).join('');
    if (valor === false || valor === null || valor === undefined) return '';
    return esc(valor);
}

/**
 * Plantilla HTML que escapa todo lo interpolado (evita XSS con datos de la API).
 * Las piezas generadas con html`` o raw() se insertan sin escapar.
 */
export function html(partes, ...valores) {
    let salida = partes[0];
    valores.forEach((valor, i) => { salida += convertir(valor) + partes[i + 1]; });
    return new HtmlSeguro(salida);
}

// ---------------------------------------------------------------- formatos

const moneda = new Intl.NumberFormat('es-CO', { style: 'currency', currency: 'COP', maximumFractionDigits: 0 });
const fechaCorta = new Intl.DateTimeFormat('es-CO', { day: 'numeric', month: 'short', year: 'numeric' });
const fechaHora = new Intl.DateTimeFormat('es-CO', { day: 'numeric', month: 'short', hour: '2-digit', minute: '2-digit' });
const hora = new Intl.DateTimeFormat('es-CO', { hour: '2-digit', minute: '2-digit' });

export const fmtMoneda = valor => (valor === null || valor === undefined ? '—' : moneda.format(valor));

/** Fechas de reserva (yyyy-MM-dd): se interpretan como día local, sin desfase por zona horaria. */
export function fmtFecha(texto) {
    if (!texto) return '—';
    const [a, m, d] = texto.split('-').map(Number);
    return fechaCorta.format(new Date(a, m - 1, d));
}

/** Instantes de la API (yyyy-MM-ddTHH:mm:ss en UTC) mostrados en la hora local del navegador. */
export const instante = texto => (texto ? new Date(texto.endsWith('Z') ? texto : texto + 'Z') : null);
export const fmtInstante = texto => (texto ? fechaHora.format(instante(texto)) : '—');
export const fmtHora = fecha => hora.format(fecha);

/** Zona horaria del negocio: debe coincidir con app.zona-horaria del backend, que decide qué día es "hoy". */
const ZONA_NEGOCIO = 'America/Bogota';

/** Fecha de hoy (yyyy-mm-dd) en la zona del negocio, no en la del equipo, para coincidir con el backend. */
export function hoyIso(sumarDias = 0) {
    const hoy = new Intl.DateTimeFormat('en-CA', { timeZone: ZONA_NEGOCIO }).format(new Date());
    const d = new Date(`${hoy}T00:00:00Z`);
    d.setUTCDate(d.getUTCDate() + sumarDias);
    return d.toISOString().slice(0, 10);
}

/** Días de alquiler con ambos extremos incluidos (igual que el backend). */
export function diasAlquiler(inicio, fin) {
    if (!inicio || !fin) return 0;
    const [a1, m1, d1] = inicio.split('-').map(Number);
    const [a2, m2, d2] = fin.split('-').map(Number);
    return Math.round((Date.UTC(a2, m2 - 1, d2) - Date.UTC(a1, m1 - 1, d1)) / 86400000) + 1;
}

// ---------------------------------------------------------------- estados

const ESTADOS = {
    // reservas
    PENDIENTE: ['warning', 'Pendiente'], ACEPTADA: ['info', 'Aceptada · por pagar'], CONFIRMADA: ['primary', 'Confirmada'],
    ENTREGADA: ['dark', 'Entregada'], FINALIZADA: ['success', 'Finalizada'], RECHAZADA: ['danger', 'Rechazada'],
    VENCIDA: ['secondary', 'Vencida'], CANCELADA: ['secondary', 'Cancelada'],
    // herramientas
    DISPONIBLE: ['success', 'Disponible'], MANTENIMIENTO: ['warning', 'En mantenimiento'],
    FUERA_DE_SERVICIO: ['danger', 'Fuera de servicio'],
    // pagos y facturas
    COMPLETADO: ['success', 'Completado'], REEMBOLSADO: ['secondary', 'Reembolsado'], RECHAZADO: ['danger', 'Rechazado'],
    VIGENTE: ['success', 'Vigente'], ANULADA: ['danger', 'Anulada'],
    // devoluciones
    BUENO: ['success', 'Buen estado'], DANADO: ['warning', 'Dañado'], PERDIDO: ['danger', 'Perdido'],
    // lista de espera
    ESPERANDO: ['warning', 'Esperando'], NOTIFICADA: ['success', '¡Ya está libre!'], ATENDIDA: ['primary', 'Reservada'],
};

export function badge(estado) {
    const [color, texto] = ESTADOS[estado] || ['light', estado || '—'];
    const textoOscuro = ['warning', 'info', 'light'].includes(color) ? ' text-dark' : '';
    return html`<span class="badge text-bg-${color}${textoOscuro}">${texto}</span>`;
}

/** Promedio de 1 a 5 como estrellas (con media estrella) y el número de reseñas. */
export function estrellas(promedio, total) {
    if (!total) return html`<span class="small text-muted"><i aria-hidden="true" class="bi bi-star"></i> Sin reseñas</span>`;
    const valor = Number(promedio);
    const llenas = Math.floor(valor);
    const media = valor - llenas >= 0.5;
    const iconos = [0, 1, 2, 3, 4].map(i => (i < llenas ? 'star-fill' : i === llenas && media ? 'star-half' : 'star'));
    return html`<span class="small text-warning text-nowrap" title="${valor.toFixed(1)} de 5">${iconos.map(i =>
        html`<i aria-hidden="true" class="bi bi-${i}"></i>`)}<span class="text-muted ms-1">${valor.toFixed(1)} (${total})</span></span>`;
}

export const nombreEstado = estado => (ESTADOS[estado] ? ESTADOS[estado][1] : estado);

// ---------------------------------------------------------------- componentes

export function cargando(contenedor, texto = 'Cargando...') {
    contenedor.innerHTML = html`<div class="text-center text-muted py-5">
        <div class="spinner-border text-warning mb-2" role="status"></div><div>${texto}</div></div>`;
}

export function vacio(texto, icono = 'inbox') {
    return html`<div class="text-center text-muted py-5"><i aria-hidden="true" class="bi bi-${icono} fs-1 d-block mb-2"></i>${texto}</div>`;
}

export function kpi(titulo, valor, icono, color = 'warning') {
    return html`<div class="col-6 col-lg-3">
        <div class="card kpi h-100 border-0 shadow-sm"><div class="card-body d-flex align-items-center gap-3">
            <div class="kpi-icono bg-${color}-subtle text-${color}-emphasis"><i aria-hidden="true" class="bi bi-${icono}"></i></div>
            <div><div class="text-muted small">${titulo}</div><div class="fs-4 fw-bold">${valor}</div></div>
        </div></div></div>`;
}

/** Barras horizontales simples para un mapa {estado: cantidad}. */
export function barrasPorEstado(mapa) {
    const total = Object.values(mapa || {}).reduce((s, n) => s + n, 0);
    if (!total) return vacio('Sin datos', 'bar-chart');
    return html`${Object.entries(mapa).sort((a, b) => b[1] - a[1]).map(([estado, n]) => html`
        <div class="mb-2"><div class="d-flex justify-content-between small"><span>${nombreEstado(estado)}</span><span class="fw-semibold">${n}</span></div>
        <div class="progress" role="progressbar" aria-label="${nombreEstado(estado)}" style="height: 8px">
            <div class="progress-bar bg-warning" style="width: ${Math.round(n * 100 / total)}%"></div></div></div>`)}`;
}

export function imagenHerramienta(h, clase = 'herramienta-img') {
    return h.imagenUrl
        ? html`<img src="${h.imagenUrl}" class="${clase}" alt="${h.nombre}" loading="lazy">`
        : html`<div class="${clase} sin-imagen"><i aria-hidden="true" class="bi bi-tools"></i></div>`;
}

// ---------------------------------------------------------------- avisos

export function toast(mensaje, tipo = 'success') {
    const iconos = { success: 'check-circle-fill', danger: 'exclamation-octagon-fill', warning: 'exclamation-triangle-fill', info: 'info-circle-fill' };
    const nodo = document.createElement('div');
    nodo.className = `toast align-items-center text-bg-${tipo} border-0`;
    nodo.setAttribute('role', 'alert');
    nodo.innerHTML = html`<div class="d-flex"><div class="toast-body"><i aria-hidden="true" class="bi bi-${iconos[tipo] || iconos.info} me-2"></i>${mensaje}</div>
        <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast" aria-label="Cerrar"></button></div>`;
    document.getElementById('toasts').appendChild(nodo);
    const t = new bootstrap.Toast(nodo, { delay: tipo === 'danger' ? 7000 : 4000 });
    nodo.addEventListener('hidden.bs.toast', () => nodo.remove());
    t.show();
}

/** Mensaje de error de la API, con el detalle por campo cuando es un error de validación. */
export function mensajeError(error) {
    const detalle = error.errores ? ' ' + Object.values(error.errores).join(' ') : '';
    return (error.message || 'Ocurrió un error inesperado.') + detalle;
}

export const toastError = error => toast(mensajeError(error), 'danger');

// ---------------------------------------------------------------- modales

/**
 * Abre una ventana modal con un formulario y devuelve su nodo. alConfirmar(form, datos) puede ser async; si lanza un error,
 * el mensaje se muestra dentro de la ventana y esta sigue abierta. Si devuelve false, la ventana no se cierra.
 */
export function abrirModal({ titulo, cuerpo, textoConfirmar = 'Guardar', colorConfirmar = 'warning', alConfirmar,
                             alAbrir, tamano = '', soloLectura = false }) {
    const nodo = document.createElement('div');
    nodo.className = 'modal fade';
    nodo.tabIndex = -1;
    nodo.innerHTML = html`<div class="modal-dialog modal-dialog-centered modal-dialog-scrollable ${tamano}">
        <form class="modal-content" novalidate>
            <div class="modal-header"><h5 class="modal-title">${titulo}</h5>
                <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Cerrar"></button></div>
            <div class="modal-body">${cuerpo}<div class="alert alert-danger d-none mt-3 mb-0" data-error></div></div>
            <div class="modal-footer">
                <button type="button" class="btn btn-outline-secondary" data-bs-dismiss="modal">${soloLectura ? 'Cerrar' : 'Cancelar'}</button>
                ${soloLectura ? '' : html`<button type="submit" class="btn btn-${colorConfirmar}">${textoConfirmar}</button>`}
            </div>
        </form></div>`;
    document.body.appendChild(nodo);
    const modal = new bootstrap.Modal(nodo);
    const form = nodo.querySelector('form');
    const alerta = nodo.querySelector('[data-error]');

    form.addEventListener('submit', async evento => {
        evento.preventDefault();
        if (soloLectura) return;
        if (!form.checkValidity()) {
            form.classList.add('was-validated');
            return;
        }
        const boton = form.querySelector('[type=submit]');
        boton.disabled = true;
        alerta.classList.add('d-none');
        try {
            const datos = Object.fromEntries(new FormData(form).entries());
            const resultado = await alConfirmar(form, datos);
            if (resultado !== false) modal.hide();
        } catch (error) {
            alerta.textContent = mensajeError(error);
            alerta.classList.remove('d-none');
        } finally {
            boton.disabled = false;
        }
    });
    nodo.addEventListener('hidden.bs.modal', () => { modal.dispose(); nodo.remove(); });
    nodo.addEventListener('shown.bs.modal', () => nodo.querySelector('input:not([type=hidden]), select, textarea')?.focus());
    if (alAbrir) alAbrir(form);
    modal.show();
    return nodo;
}

/** Confirmación sí/no. Devuelve una promesa que se resuelve con true si el usuario acepta. */
export function confirmar(mensaje, { titulo = 'Confirmar', textoConfirmar = 'Sí, continuar', color = 'danger' } = {}) {
    return new Promise(resolve => {
        let aceptado = false;
        const nodo = abrirModal({
            titulo, cuerpo: html`<p class="mb-0">${mensaje}</p>`, textoConfirmar, colorConfirmar: color,
            alConfirmar: () => { aceptado = true; },
        });
        nodo.addEventListener('hidden.bs.modal', () => resolve(aceptado));
    });
}

export function opciones(lista, valorSeleccionado, { valor = 'id', texto = 'nombre', vacia } = {}) {
    return html`${vacia ? html`<option value="">${vacia}</option>` : ''}${lista.map(item => html`
        <option value="${item[valor]}" ${String(item[valor]) === String(valorSeleccionado) ? raw('selected') : ''}>${item[texto]}</option>`)}`;
}
