// Inicio de sesión y registro público de clientes (JWT).
import { api, sesion } from '../api.js';
import { html, mensajeError } from '../ui.js';

const CUENTAS_DEMO = [
    { rol: 'Administrador', email: 'admin@rentas.com', icono: 'shield-lock' },
    { rol: 'Proveedor', email: 'carlos.proveedor@gmail.com', icono: 'shop' },
    { rol: 'Cliente', email: 'juan.cliente@gmail.com', icono: 'person' },
];
const CLAVE_DEMO = 'Rentas2026*';

export function vistaAcceso(raiz, alIniciar, aviso) {
    raiz.innerHTML = html`<div class="acceso min-vh-100 d-flex align-items-center py-5">
        <div class="container"><div class="row justify-content-center align-items-center g-5">
            <div class="col-lg-6 text-white d-none d-lg-block">
                <div class="display-5 fw-bold mb-3"><i aria-hidden="true" class="bi bi-tools text-warning"></i> RentaHerramientas</div>
                <p class="lead">Alquile herramientas y equipos de construcción sin comprarlos. Reserve por fechas, pague en línea y
                    descargue su factura. Los proveedores publican su inventario y gestionan sus alquileres.</p>
                <ul class="list-unstyled mt-4">
                    <li class="mb-2"><i aria-hidden="true" class="bi bi-check-circle-fill text-warning me-2"></i>Catálogo por categoría y disponibilidad</li>
                    <li class="mb-2"><i aria-hidden="true" class="bi bi-check-circle-fill text-warning me-2"></i>Confirmación automática al pagar y factura inmediata</li>
                    <li class="mb-2"><i aria-hidden="true" class="bi bi-check-circle-fill text-warning me-2"></i>Control de entregas, devoluciones y daños</li></ul>
            </div>
            <div class="col-md-8 col-lg-5"><div class="card border-0 shadow-lg"><div class="card-body p-4">
                <div class="text-center d-lg-none mb-3 fs-3 fw-bold"><i aria-hidden="true" class="bi bi-tools text-warning"></i> RentaHerramientas</div>
                ${aviso ? html`<div class="alert alert-warning small">${aviso}</div>` : ''}
                <ul class="nav nav-pills nav-fill mb-4" role="tablist">
                    <li class="nav-item" role="presentation"><button class="nav-link active" data-bs-toggle="pill" data-bs-target="#tab-login" type="button" role="tab">Iniciar sesión</button></li>
                    <li class="nav-item" role="presentation"><button class="nav-link" data-bs-toggle="pill" data-bs-target="#tab-registro" type="button" role="tab">Crear cuenta</button></li>
                </ul>
                <div class="tab-content">
                    <form class="tab-pane fade show active" id="tab-login" role="tabpanel" data-login novalidate>
                        <div class="mb-3"><label class="form-label" for="l-email">Correo electrónico</label>
                            <input type="email" class="form-control" id="l-email" name="email" autocomplete="username" required></div>
                        <div class="mb-3"><label class="form-label" for="l-pass">Contraseña</label>
                            <input type="password" class="form-control" id="l-pass" name="password" autocomplete="current-password" required></div>
                        <div class="alert alert-danger d-none small" data-error></div>
                        <button type="submit" class="btn btn-warning w-100 fw-semibold">Entrar</button>
                        <div class="mt-4"><div class="small text-muted mb-2">Cuentas de demostración (clave ${CLAVE_DEMO}):</div>
                            <div class="d-flex flex-wrap gap-2">${CUENTAS_DEMO.map(c => html`
                                <button type="button" class="btn btn-sm btn-outline-secondary" data-demo="${c.email}">
                                    <i aria-hidden="true" class="bi bi-${c.icono} me-1"></i>${c.rol}</button>`)}</div></div>
                    </form>
                    <form class="tab-pane fade" id="tab-registro" role="tabpanel" data-registro novalidate>
                        <div class="mb-3"><label class="form-label" for="r-nombre">Nombre completo</label>
                            <input class="form-control" id="r-nombre" name="nombre" maxlength="100" autocomplete="name" required></div>
                        <div class="mb-3"><label class="form-label" for="r-email">Correo electrónico</label>
                            <input type="email" class="form-control" id="r-email" name="email" maxlength="150" autocomplete="email" required></div>
                        <div class="mb-3"><label class="form-label" for="r-pass">Contraseña</label>
                            <input type="password" class="form-control" id="r-pass" name="password" minlength="8" maxlength="72" autocomplete="new-password" required>
                            <div class="form-text">Mínimo 8 caracteres.</div></div>
                        <div class="alert alert-danger d-none small" data-error></div>
                        <button type="submit" class="btn btn-warning w-100 fw-semibold">Crear cuenta de cliente</button>
                        <p class="small text-muted mt-3 mb-0">¿Quiere publicar herramientas como proveedor? El administrador de la plataforma crea esas cuentas.</p>
                    </form>
                </div>
            </div></div></div>
        </div></div></div>`;

    const enviar = (form, accion) => form.addEventListener('submit', async evento => {
        evento.preventDefault();
        const error = form.querySelector('[data-error]');
        if (!form.checkValidity()) {
            form.classList.add('was-validated');
            return;
        }
        const boton = form.querySelector('[type=submit]');
        boton.disabled = true;
        error.classList.add('d-none');
        try {
            const respuesta = await accion(Object.fromEntries(new FormData(form).entries()));
            sesion.guardar(respuesta);
            alIniciar(respuesta);
        } catch (e) {
            error.textContent = mensajeError(e);
            error.classList.remove('d-none');
        } finally {
            boton.disabled = false;
        }
    });

    const login = raiz.querySelector('[data-login]');
    enviar(login, datos => api.post('/auth/login', datos));
    enviar(raiz.querySelector('[data-registro]'), datos => api.post('/auth/registro', datos));
    raiz.querySelectorAll('[data-demo]').forEach(boton => boton.addEventListener('click', () => {
        login.email.value = boton.dataset.demo;
        login.password.value = CLAVE_DEMO;
        login.requestSubmit();
    }));
}
