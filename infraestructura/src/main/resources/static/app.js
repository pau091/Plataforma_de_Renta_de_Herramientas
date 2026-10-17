const API_URL = '/api';
let token = localStorage.getItem('jwt_token') || '';
let rolActual = localStorage.getItem('user_rol') || '';

document.addEventListener('DOMContentLoaded', () => {
    if (token) {
        mostrarDashboard();
    }

    // Manejar Login
    document.getElementById('login-form').addEventListener('submit', async (e) => {
        e.preventDefault();
        const email = document.getElementById('email').value;
        const password = document.getElementById('password').value;

        try {
            const response = await fetch(`${API_URL}/auth/login`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ email, password })
            });

            if (response.ok) {
                const data = await response.json();
                token = data.token;
                rolActual = data.rol;
                localStorage.setItem('jwt_token', token);
                localStorage.setItem('user_rol', rolActual);
                mostrarDashboard();
            } else {
                alert('Credenciales incorrectas');
            }
        } catch (error) {
            console.error('Error en el login:', error);
            alert('No se pudo conectar con el servidor backend.');
        }
    });

    // Manejar Registro de Herramienta
    document.getElementById('herramienta-form').addEventListener('submit', async (e) => {
        e.preventDefault();
        const nombre = document.getElementById('h-nombre').value;
        const descripcion = document.getElementById('h-descripcion').value;
        const categoriaId = parseInt(document.getElementById('h-categoria').value, 10);
        const imagenUrl = document.getElementById('h-imagen').value;
        const tarifaDiaria = parseFloat(document.getElementById('h-tarifa').value);

        try {
            const response = await fetch(`${API_URL}/herramientas`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${token}`
                },
                body: JSON.stringify({ proveedorId: 2, categoriaId, nombre, descripcion, tarifaDiaria, imagenUrl })
            });

            if (response.ok) {
                alert('¡Herramienta registrada con éxito!');
                document.getElementById('herramienta-form').reset();
                cargarHerramientas();
            } else {
                alert('Error al registrar la herramienta');
            }
        } catch (error) {
            console.error('Error:', error);
        }
    });
});

function mostrarDashboard() {
    document.getElementById('login-section').classList.add('d-none');
    document.getElementById('dashboard-section').classList.remove('d-none');
    document.getElementById('auth-status').innerHTML = `
        <span class="me-3">Sesión ${rolActual || 'Activa'}</span>
        <button class="btn btn-outline-light btn-sm" onclick="cerrarSesion()">Cerrar Sesión</button>
    `;
    const tabProveedor = document.getElementById('nueva-herramienta-tab');
    if (tabProveedor) {
        const puedeGestionar = rolActual === 'ADMIN' || rolActual === 'PROVEEDOR';
        tabProveedor.parentElement.classList.toggle('d-none', !puedeGestionar);
    }
    cargarHerramientas();
}

function cerrarSesion() {
    localStorage.removeItem('jwt_token');
    localStorage.removeItem('user_rol');
    location.reload();
}

async function cargarHerramientas() {
    try {
        const response = await fetch(`${API_URL}/herramientas`, {
            headers: { 'Authorization': `Bearer ${token}` }
        });
        if (response.ok) {
            const herramientas = await response.json();
            const contenedor = document.getElementById('herramientas-list');
            contenedor.innerHTML = '';

            herramientas.forEach(h => {
                contenedor.innerHTML += `
                    <div class="col-md-4 mb-3">
                        <div class="card h-100 shadow-sm">
                            <div class="card-body">
                                <h5 class="card-title">${h.nombre}</h5>
                                <p class="card-text">${h.descripcion}</p>
                                <p class="text-success fw-bold">$${h.tarifaDiaria} / día</p>
                                <button class="btn btn-primary btn-sm" onclick="alquilar(${h.id})">Reservar</button>
                            </div>
                        </div>
                    </div>
                `;
            });
        }
    } catch (error) {
        console.error('Error cargando herramientas:', error);
    }
}

function alquilar(herramientaId) {
    alert(`Iniciando proceso de reserva para la herramienta ID: ${herramientaId}`);
}