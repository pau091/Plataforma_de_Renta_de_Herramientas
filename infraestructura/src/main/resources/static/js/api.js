// Cliente de la API REST: guarda la sesión (token JWT) y envía "Authorization: Bearer <token>" en cada petición.

const BASE = '/api';
const CLAVE_SESION = 'renta_sesion';

export class ErrorApi extends Error {
    constructor(status, cuerpo) {
        super(cuerpo?.message || `Error ${status}`);
        this.status = status;
        this.errores = cuerpo?.errores;
    }
}

export const sesion = {
    obtener() {
        try {
            return JSON.parse(localStorage.getItem(CLAVE_SESION));
        } catch {
            return null;
        }
    },
    guardar(datos) {
        localStorage.setItem(CLAVE_SESION, JSON.stringify(datos));
    },
    cerrar() {
        localStorage.removeItem(CLAVE_SESION);
    },
};

/** Se dispara cuando el servidor rechaza el token (expiró, es inválido o el usuario fue desactivado). */
export const EVENTO_SESION_EXPIRADA = 'sesion-expirada';

async function peticion(metodo, ruta, { cuerpo, formulario, comoBlob = false } = {}) {
    const cabeceras = {};
    const token = sesion.obtener()?.token;
    if (token) cabeceras.Authorization = `Bearer ${token}`;
    let body;
    if (formulario) {
        body = formulario; // multipart: el navegador pone el Content-Type con el boundary
    } else if (cuerpo !== undefined) {
        cabeceras['Content-Type'] = 'application/json';
        body = JSON.stringify(cuerpo);
    }

    let respuesta;
    try {
        respuesta = await fetch(BASE + ruta, { method: metodo, headers: cabeceras, body });
    } catch {
        throw new ErrorApi(0, { message: 'No se pudo conectar con el servidor. Verifique que la aplicación esté en ejecución.' });
    }

    if (!respuesta.ok) {
        let error = null;
        try {
            error = await respuesta.json();
        } catch {
            // respuesta sin cuerpo JSON
        }
        if (respuesta.status === 401 && token) {
            window.dispatchEvent(new CustomEvent(EVENTO_SESION_EXPIRADA, { detail: error?.message }));
        }
        throw new ErrorApi(respuesta.status, error);
    }
    if (respuesta.status === 204) return null;
    if (comoBlob) return { blob: await respuesta.blob(), cabeceras: respuesta.headers };
    return respuesta.json();
}

export const api = {
    get: ruta => peticion('GET', ruta),
    post: (ruta, cuerpo) => peticion('POST', ruta, { cuerpo }),
    put: (ruta, cuerpo) => peticion('PUT', ruta, { cuerpo }),
    patch: (ruta, cuerpo) => peticion('PATCH', ruta, { cuerpo }),
    del: ruta => peticion('DELETE', ruta),
    subir: (ruta, formulario) => peticion('POST', ruta, { formulario }),
    blob: ruta => peticion('GET', ruta, { comoBlob: true }),
};

/** Construye una ruta con parámetros de consulta, omitiendo los vacíos. */
export function conParametros(ruta, parametros) {
    const qs = new URLSearchParams();
    Object.entries(parametros).forEach(([clave, valor]) => {
        if (valor !== undefined && valor !== null && valor !== '' && valor !== false) qs.append(clave, valor);
    });
    const texto = qs.toString();
    return texto ? `${ruta}?${texto}` : ruta;
}

/** Descarga la factura en HTML (la petición necesita el token, así que no basta con un enlace). */
export async function descargarFactura(id, numero) {
    const { blob } = await api.blob(`/facturas/${id}/descargar`);
    const url = URL.createObjectURL(blob);
    const enlace = document.createElement('a');
    enlace.href = url;
    enlace.download = `${numero || 'factura'}.html`;
    document.body.appendChild(enlace);
    enlace.click();
    enlace.remove();
    setTimeout(() => URL.revokeObjectURL(url), 1000);
}
