// Inicio de sesión y registro público de clientes (JWT).
import { api, sesion } from '../api.js';
import { html, mensajeError } from '../ui.js';

const CUENTAS_DEMO = [
    { rol: 'Administrador', email: 'admin@rentas.com', icono: 'shield-lock' },
    { rol: 'Proveedor', email: 'carlos.proveedor@gmail.com', icono: 'shop' },
    { rol: 'Cliente', email: 'juan.cliente@gmail.com', icono: 'person' },
];
const CLAVE_DEMO = 'Rentas2026*';

function campo({ id, nombre, etiqueta, tipo = 'text', icono, autocompletar, extra = '', ayuda = '' }) {
    const esClave = tipo === 'password';
    return html`<div class="mb-3">
        <label class="form-label fw-medium" for="${id}">${etiqueta}</label>
        <div class="input-group campo-acceso">
            <span class="input-group-text"><i aria-hidden="true" class="bi bi-${icono}"></i></span>
            <input type="${tipo}" class="form-control" id="${id}" name="${nombre}" autocomplete="${autocompletar}" required ${extra}>
            ${esClave ? html`<button type="button" class="btn btn-ver-clave" data-ver-clave="${id}" aria-label="Mostrar contraseña">
                <i aria-hidden="true" class="bi bi-eye"></i></button>` : ''}
        </div>
        ${ayuda ? html`<div class="form-text">${ayuda}</div>` : ''}
    </div>`;
}

export function vistaAcceso(raiz, alIniciar, aviso) {
    raiz.innerHTML = html`<div class="acceso">
        <section class="acceso-hero d-none d-lg-flex" aria-label="Presentación">
            <div class="acceso-hero-contenido">
                <div class="marca"><span class="marca-logo"><i aria-hidden="true" class="bi bi-tools"></i></span>RentaHerramientas</div>
                <div class="mt-auto">
                    <h1 class="display-6 fw-bold mb-3">Herramientas y maquinaria para tu obra, cuando las necesitas.</h1>
                    <p class="lead opacity-75 mb-4">Reserva por fechas, paga en línea y recibe tu factura al instante.
                        Proveedores verificados y control total de cada alquiler.</p>
                    <div class="row g-3 acceso-beneficios">
                        <div class="col-4"><i aria-hidden="true" class="bi bi-calendar2-check"></i><span>Reservas por fechas con disponibilidad real</span></div>
                        <div class="col-4"><i aria-hidden="true" class="bi bi-shield-check"></i><span>Pagos seguros y confirmación automática</span></div>
                        <div class="col-4"><i aria-hidden="true" class="bi bi-receipt"></i><span>Facturas descargables al momento</span></div>
                    </div>
                </div>
                <div class="small opacity-50 mt-5">© 2026 RentaHerramientas · Plataforma de alquiler de equipos de construcción</div>
            </div>
        </section>

        <section class="acceso-panel">
            <div class="acceso-formulario">
                <div class="marca marca-oscura d-lg-none mb-4"><span class="marca-logo"><i aria-hidden="true" class="bi bi-tools"></i></span>RentaHerramientas</div>
                ${aviso ? html`<div class="alert alert-warning d-flex gap-2 small" role="alert">
                    <i aria-hidden="true" class="bi bi-exclamation-triangle-fill"></i><div>${aviso}</div></div>` : ''}

                <form data-login novalidate>
                    <h2 class="h3 fw-bold mb-1">Bienvenido de nuevo</h2>
                    <p class="text-secondary mb-4">Ingresa con tu correo y contraseña para continuar.</p>
                    ${campo({ id: 'l-email', nombre: 'email', etiqueta: 'Correo electrónico', tipo: 'email', icono: 'envelope',
                              autocompletar: 'username', extra: html`placeholder="nombre@correo.com"` })}
                    ${campo({ id: 'l-pass', nombre: 'password', etiqueta: 'Contraseña', tipo: 'password', icono: 'lock',
                              autocompletar: 'current-password', extra: html`placeholder="Tu contraseña"` })}
                    <div class="alert alert-danger d-none small py-2" data-error role="alert"></div>
                    <button type="submit" class="btn btn-dark btn-lg w-100 btn-acceso">
                        <span class="spinner-border spinner-border-sm d-none me-2" aria-hidden="true"></span>Iniciar sesión</button>
                    <p class="text-center text-secondary mt-4 mb-0">¿No tienes cuenta?
                        <a href="#" class="enlace-acceso" data-mostrar="registro">Regístrate como cliente</a></p>
                </form>

                <form class="d-none" data-registro novalidate>
                    <h2 class="h3 fw-bold mb-1">Crea tu cuenta</h2>
                    <p class="text-secondary mb-4">Regístrate como cliente para reservar herramientas.</p>
                    ${campo({ id: 'r-nombre', nombre: 'nombre', etiqueta: 'Nombre completo', icono: 'person', autocompletar: 'name',
                              extra: html`maxlength="100" placeholder="Tu nombre y apellido"` })}
                    ${campo({ id: 'r-email', nombre: 'email', etiqueta: 'Correo electrónico', tipo: 'email', icono: 'envelope',
                              autocompletar: 'email', extra: html`maxlength="150" placeholder="nombre@correo.com"` })}
                    ${campo({ id: 'r-pass', nombre: 'password', etiqueta: 'Contraseña', tipo: 'password', icono: 'lock',
                              autocompletar: 'new-password', extra: html`minlength="8" maxlength="72"`, ayuda: 'Mínimo 8 caracteres.' })}
                    <div class="alert alert-danger d-none small py-2" data-error role="alert"></div>
                    <button type="submit" class="btn btn-dark btn-lg w-100 btn-acceso">
                        <span class="spinner-border spinner-border-sm d-none me-2" aria-hidden="true"></span>Crear cuenta</button>
                    <p class="small text-secondary mt-3 mb-0">¿Quieres publicar herramientas como proveedor? El administrador de la
                        plataforma crea esas cuentas.</p>
                    <p class="text-center text-secondary mt-3 mb-0">¿Ya tienes cuenta?
                        <a href="#" class="enlace-acceso" data-mostrar="login">Inicia sesión</a></p>
                </form>

                <div class="acceso-demo mt-5">
                    <div class="separador"><span>Acceso de demostración</span></div>
                    <div class="d-flex gap-2 mt-3">${CUENTAS_DEMO.map(c => html`
                        <button type="button" class="btn btn-demo flex-fill" data-demo="${c.email}" title="${c.email}">
                            <i aria-hidden="true" class="bi bi-${c.icono}"></i><span>${c.rol}</span></button>`)}</div>
                    <div class="small text-secondary text-center mt-2">Contraseña de todas: <code>${CLAVE_DEMO}</code></div>
                </div>
            </div>
        </section>
    </div>`;

    const login = raiz.querySelector('[data-login]');
    const registro = raiz.querySelector('[data-registro]');

    const enviar = (form, accion) => form.addEventListener('submit', async evento => {
        evento.preventDefault();
        const error = form.querySelector('[data-error]');
        if (!form.checkValidity()) {
            form.classList.add('was-validated');
            return;
        }
        const boton = form.querySelector('[type=submit]');
        const giro = boton.querySelector('.spinner-border');
        boton.disabled = true;
        giro.classList.remove('d-none');
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
            giro.classList.add('d-none');
        }
    });
    enviar(login, datos => api.post('/auth/login', datos));
    enviar(registro, datos => api.post('/auth/registro', datos));

    raiz.querySelectorAll('[data-mostrar]').forEach(enlace => enlace.addEventListener('click', evento => {
        evento.preventDefault();
        const mostrarRegistro = enlace.dataset.mostrar === 'registro';
        login.classList.toggle('d-none', mostrarRegistro);
        registro.classList.toggle('d-none', !mostrarRegistro);
        (mostrarRegistro ? registro : login).querySelector('input').focus();
    }));

    raiz.querySelectorAll('[data-ver-clave]').forEach(boton => boton.addEventListener('click', () => {
        const campoClave = raiz.querySelector(`#${boton.dataset.verClave}`);
        const visible = campoClave.type === 'text';
        campoClave.type = visible ? 'password' : 'text';
        boton.setAttribute('aria-label', visible ? 'Mostrar contraseña' : 'Ocultar contraseña');
        boton.querySelector('i').className = `bi bi-${visible ? 'eye' : 'eye-slash'}`;
    }));

    raiz.querySelectorAll('[data-demo]').forEach(boton => boton.addEventListener('click', () => {
        login.classList.remove('d-none');
        registro.classList.add('d-none');
        login.email.value = boton.dataset.demo;
        login.password.value = CLAVE_DEMO;
        login.requestSubmit();
    }));
}
