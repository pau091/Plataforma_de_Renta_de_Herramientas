// Punto de entrada: sesión, menú según el rol y navegación por "#/ruta".
import { EVENTO_SESION_EXPIRADA, sesion } from './api.js';
import { html, toast } from './ui.js';
import { vistaAcceso } from './vistas/auth.js';
import * as admin from './vistas/admin.js';
import * as cliente from './vistas/cliente.js';
import * as proveedor from './vistas/proveedor.js';

/** Menú y vistas de cada rol. El grupo agrupa las opciones en el menú lateral. */
const RUTAS = {
    CLIENTE: [
        { ruta: 'inicio', texto: 'Inicio', icono: 'house', vista: cliente.inicio },
        { ruta: 'catalogo', texto: 'Catálogo', icono: 'search', vista: cliente.catalogo },
        { ruta: 'reservas', texto: 'Mis reservas', icono: 'calendar-check', vista: cliente.reservas },
        { ruta: 'facturas', texto: 'Pagos y facturas', icono: 'receipt', vista: cliente.facturas },
    ],
    PROVEEDOR: [
        { ruta: 'inicio', texto: 'Inicio', icono: 'house', vista: proveedor.inicio },
        { ruta: 'herramientas', texto: 'Mis herramientas', icono: 'tools', vista: proveedor.herramientas, grupo: 'Inventario' },
        { ruta: 'mantenimientos', texto: 'Mantenimientos', icono: 'wrench', vista: proveedor.mantenimientos, grupo: 'Inventario' },
        { ruta: 'reservas', texto: 'Reservas', icono: 'calendar-check', vista: proveedor.reservas, grupo: 'Alquileres' },
        { ruta: 'facturacion', texto: 'Facturación', icono: 'receipt', vista: proveedor.facturacion, grupo: 'Alquileres' },
        { ruta: 'catalogo', texto: 'Catálogo', icono: 'search', vista: proveedor.catalogo, grupo: 'Como cliente' },
        { ruta: 'mis-alquileres', texto: 'Mis alquileres', icono: 'bag-check', vista: proveedor.misAlquileres, grupo: 'Como cliente' },
        { ruta: 'mis-facturas', texto: 'Mis pagos y facturas', icono: 'wallet2', vista: proveedor.misFacturas, grupo: 'Como cliente' },
    ],
    ADMIN: [
        { ruta: 'inicio', texto: 'Inicio', icono: 'house', vista: admin.inicio },
        { ruta: 'usuarios', texto: 'Usuarios', icono: 'people', vista: admin.usuarios, grupo: 'Gestión' },
        { ruta: 'herramientas', texto: 'Herramientas', icono: 'tools', vista: admin.herramientas, grupo: 'Gestión' },
        { ruta: 'categorias', texto: 'Categorías', icono: 'tags', vista: admin.categorias, grupo: 'Gestión' },
        { ruta: 'reservas', texto: 'Alquileres', icono: 'calendar-check', vista: admin.reservas, grupo: 'Supervisión' },
        { ruta: 'incidencias', texto: 'Incidencias', icono: 'exclamation-triangle', vista: admin.incidencias, grupo: 'Supervisión' },
        { ruta: 'mantenimientos', texto: 'Mantenimientos', icono: 'wrench', vista: admin.mantenimientos, grupo: 'Supervisión' },
        { ruta: 'pagos', texto: 'Pagos', icono: 'credit-card', vista: admin.pagos, grupo: 'Finanzas' },
        { ruta: 'facturas', texto: 'Facturas', icono: 'receipt', vista: admin.facturas, grupo: 'Finanzas' },
        { ruta: 'reportes', texto: 'Reportes', icono: 'bar-chart-line', vista: admin.reportes, grupo: 'Finanzas' },
    ],
};

const NOMBRE_ROL = { ADMIN: 'Administrador', PROVEEDOR: 'Proveedor', CLIENTE: 'Cliente' };
const raiz = document.getElementById('app');

function menu(rutas) {
    let grupoActual = null;
    return html`${rutas.map(r => {
        const titulo = r.grupo && r.grupo !== grupoActual ? html`<div class="menu-grupo">${r.grupo}</div>` : '';
        grupoActual = r.grupo || grupoActual;
        return html`${titulo}<a class="nav-link" href="#/${r.ruta}" data-ruta="${r.ruta}"><i aria-hidden="true" class="bi bi-${r.icono} me-2"></i>${r.texto}</a>`;
    })}`;
}

function pintarAplicacion(usuario) {
    const rutas = RUTAS[usuario.rol] || [];
    raiz.innerHTML = html`
        <nav class="navbar navbar-dark bg-dark sticky-top shadow-sm">
            <div class="container-fluid">
                <button class="btn btn-outline-light d-lg-none me-2" type="button" data-bs-toggle="offcanvas" data-bs-target="#menu"
                    aria-controls="menu" aria-label="Abrir menú"><i aria-hidden="true" class="bi bi-list"></i></button>
                <a class="navbar-brand fw-bold me-auto" href="#/inicio"><i aria-hidden="true" class="bi bi-tools text-warning"></i> RentaHerramientas</a>
                <div class="dropdown">
                    <button class="btn btn-dark dropdown-toggle d-flex align-items-center gap-2" data-bs-toggle="dropdown" aria-expanded="false">
                        <i aria-hidden="true" class="bi bi-person-circle fs-5"></i><span class="d-none d-sm-inline text-start lh-sm">${usuario.nombre}
                        <small class="d-block text-warning">${NOMBRE_ROL[usuario.rol]}</small></span></button>
                    <ul class="dropdown-menu dropdown-menu-end">
                        <li><span class="dropdown-item-text small text-muted">${usuario.email}</span></li>
                        <li><hr class="dropdown-divider"></li>
                        <li><button class="dropdown-item" data-salir><i aria-hidden="true" class="bi bi-box-arrow-right me-2"></i>Cerrar sesión</button></li>
                    </ul></div>
            </div></nav>
        <div class="d-flex">
            <aside class="offcanvas-lg offcanvas-start menu-lateral bg-white border-end" tabindex="-1" id="menu" aria-labelledby="menu-titulo">
                <div class="offcanvas-header"><h5 class="offcanvas-title" id="menu-titulo">Menú</h5>
                    <button type="button" class="btn-close" data-bs-dismiss="offcanvas" data-bs-target="#menu" aria-label="Cerrar"></button></div>
                <div class="offcanvas-body"><nav class="nav nav-pills flex-column w-100">${menu(rutas)}</nav></div>
            </aside>
            <main class="flex-grow-1 p-3 p-md-4 contenido" id="vista"></main>
        </div>`;
    raiz.querySelector('[data-salir]').addEventListener('click', () => cerrarSesion());
    // En móvil, al elegir una opción se cierra el menú.
    raiz.querySelectorAll('#menu .nav-link').forEach(enlace => enlace.addEventListener('click', () => {
        bootstrap.Offcanvas.getInstance(document.getElementById('menu'))?.hide();
    }));
}

async function navegar() {
    const usuario = sesion.obtener();
    if (!usuario) return;
    const rutas = RUTAS[usuario.rol] || [];
    const nombre = location.hash.replace(/^#\/?/, '') || 'inicio';
    const ruta = rutas.find(r => r.ruta === nombre) || rutas[0];
    if (ruta.ruta !== nombre) {
        history.replaceState(null, '', `#/${ruta.ruta}`);
    }
    document.querySelectorAll('#menu .nav-link').forEach(enlace =>
        enlace.classList.toggle('active', enlace.dataset.ruta === ruta.ruta));
    document.title = `${ruta.texto} · RentaHerramientas`;
    // Cada vista recibe un contenedor nuevo para que sus listeners no queden activos en otras vistas.
    const contenedor = document.createElement('div');
    document.getElementById('vista').replaceChildren(contenedor);
    window.scrollTo(0, 0);
    await ruta.vista(contenedor, usuario);
}

function iniciar(aviso) {
    const usuario = sesion.obtener();
    if (!usuario?.token || !RUTAS[usuario.rol]) {
        sesion.cerrar();
        vistaAcceso(raiz, datos => {
            toast(`Bienvenido, ${datos.nombre}.`);
            history.replaceState(null, '', '#/inicio'); // sin disparar hashchange: iniciar() ya pinta la vista
            iniciar();
        }, aviso);
        return;
    }
    pintarAplicacion(usuario);
    navegar();
}

function cerrarSesion(aviso) {
    sesion.cerrar();
    document.querySelectorAll('.modal.show').forEach(m => bootstrap.Modal.getInstance(m)?.hide());
    history.replaceState(null, '', location.pathname);
    iniciar(aviso);
}

window.addEventListener('hashchange', navegar);
window.addEventListener(EVENTO_SESION_EXPIRADA, evento => {
    if (sesion.obtener()) cerrarSesion(evento.detail || 'Su sesión expiró. Inicie sesión nuevamente.');
});

iniciar();
