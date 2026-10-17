# Contexto del proyecto: Plataforma de Renta de Herramientas

> Documento de traspaso para otro asistente de IA. Resume el proyecto, el estado del código al
> **17/10/2026**, las decisiones tomadas, las reglas de negocio vigentes, el entorno de trabajo y
> las preferencias del usuario. Léelo completo antes de proponer o hacer cambios.

---

## 1. Qué es el proyecto

Proyecto académico, evaluado por un *trainer*. El enunciado completo está en `explicacion.md`, en la raíz del
repositorio. Es la **fuente de verdad**: cualquier cambio debe contrastarse con él.

Es una plataforma donde **proveedores** publican herramientas y equipos de construcción y **clientes** los alquilan.
Incluye reservas por fechas, pagos en línea, facturación automática, devoluciones con reporte de daños,
mantenimientos y reportes para el **administrador**.

Requisitos clave del enunciado:
- **Roles:** ADMIN, PROVEEDOR y CLIENTE, con JWT y Spring Security.
- **CRUD completo:** clientes, proveedores, herramientas, reservas y pagos.
- **Disponibilidad:** control de disponibilidad, más la validación antes de confirmar una reserva.
- **Flujo de reserva:** confirmación automática, aunque el proveedor también acepta o rechaza solicitudes.
- **Pagos y facturas:** facturación automática, historial de pagos y vencimiento de reservas.
- **Reportes:** ingresos, herramientas más alquiladas, clientes frecuentes, disponibilidad y estado.
- **Base de datos:** MySQL normalizado, scripts SQL de creación y de datos (al menos 20 registros por entidad),
  diagrama relacional en imagen e índices.
- **Documentación:** Swagger con ejemplos JSON y explicación del JWT; README completo; colección de Postman.
- **Frontend:** HTML, CSS y JS puro (con Bootstrap), responsivo, con panel por rol.
- **Pruebas:** unitarias y de integración. Despliegue en **Apache Tomcat**.
- **Java:** el enunciado pide **Java 21** en el punto 2 (en otra parte dice "17 mínimo"). Se usa **Java 21**.

---

## 2. Usuario y forma de trabajo (IMPORTANTE)

- Escribe en **español**. Responde siempre en español.
- Trabaja **fase por fase**, siguiendo el plan de la sección 9. No adelantes fases sin que lo pida.
- **Commits:** desde la Fase 2, **el usuario hace los commits él mismo**. Al terminar una parte, avísale y
  sugiérele el mensaje de commit. **No ejecutes `git commit` ni `git push`.**
- **Push:** el usuario sube los cambios manualmente a
  `https://github.com/pau091/Plataforma_de_Renta_de_Herramientas.git` (remoto `origin`, rama `main`).
- **Base de datos limpia:** el usuario quiere conservar limpia su base de Docker. Las pruebas que modifican
  datos se hacen en un **MySQL temporal aparte** (ver la sección 8), nunca en `mysql_rentas`.
- Antes de borrar o sobrescribir algo fuera del repositorio, pide confirmación.
- Si una decisión es ambigua, toma un valor por defecto razonable, aplícalo y **explícalo al final** para que
  el usuario lo pueda cambiar.

---

## 3. Entorno de trabajo

| Elemento | Valor |
|---|---|
| SO | Windows 11 Pro. Shells: PowerShell 5.1 y Git Bash |
| Carpeta del proyecto (raíz del repo) | `C:\Users\developer\OneDrive\Desktop\tomcat\src\tomcat` |
| Carpeta padre `Desktop\tomcat` | Solo contiene `src\tomcat`. Había un proyecto "Hello world" del IDE que se envió a la Papelera con permiso del usuario |
| JDK | **Temurin 21** en `C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot`. También hay JDK 17 y 25 instalados |
| `JAVA_HOME` (sistema) | Apunta al JDK 21, que va primero en el PATH. Terminales abiertas antes de la instalación pueden seguir viendo el 17 |
| Build | Maven Wrapper `./mvnw` con Spring Boot 3.2.5 |
| Docker | Docker Desktop. Hay que abrirlo antes de usar `docker` |
| MySQL local de Windows | Servicio `MySQL80` en el puerto **3306**. **No se usa** y no tiene el usuario `mysql/mysql` |
| Otros contenedores del usuario | `postgres_db` (5433), `pgadmin_web` (**8081**) y `postgres-dev-workspace`, de otro proyecto (`pgsqlcontainer`). **No tocarlos** |

Detalles prácticos que ya causaron problemas:
- **Compilar:**
  `export JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-21.0.12.101-hotspot" && ./mvnw -q clean package -DskipTests`
- **Git Bash convierte rutas** como `/usr/local/...` en `docker exec`. Usa `MSYS_NO_PATHCONV=1 docker exec ...`.
- **Scripts Python grandes:** los heredocs con muchas comillas pueden fallar en bash. Es más seguro escribir el
  script en un archivo y ejecutarlo con `python -I archivo.py`.
- **OneDrive** puede bloquear la copia de carpetas `target/` en el build de Docker. Por eso existe `.dockerignore`.
- **Puertos ocupados:** 3306 (MySQL de Windows), 8081 (pgAdmin), 8080 (la app en Docker).
  Para pruebas locales se usa la app en el **8095** y MySQL temporal en el **3308**.
- La consola de Windows muestra mal las tildes al imprimir. La API las devuelve bien (UTF-8).

---

## 4. Cómo se ejecuta

```powershell
cd C:\Users\developer\OneDrive\Desktop\tomcat\src\tomcat
docker compose up -d --build        # MySQL 8.4 + app (WAR) en Tomcat 10.1 con JDK 21
docker compose logs -f tomcat       # esperar "Server startup in [...] milliseconds"
docker compose down                 # apagar (conserva datos)
docker compose down -v              # apagar y BORRAR datos (vuelve a los datos de prueba)
```

- App: http://localhost:8080
- Swagger: http://localhost:8080/swagger-ui/index.html
- MySQL de Docker desde el equipo: `localhost:3307`, usuario `mysql`, clave `mysql`, base `renta_herramientas_db`.
  Dentro de Docker la app usa `mysql:3306`.
- Contenedores: `mysql_rentas` y `tomcat_rentas`. Volúmenes: `tomcat_mysqldata` (base) y `tomcat_uploads` (imágenes).
- **Cuentas de prueba** (clave para todas: `Rentas2026*`):

| Rol | Usuarios |
|---|---|
| ADMIN | id 1: `admin@rentas.com` |
| PROVEEDOR | ids 2 a 11. Ejemplos: `carlos.proveedor@gmail.com` (2, herramientas 1 y 2), `maria.equipos@gmail.com` (3, herramientas 3 y 4), `luis.equipos@gmail.com` (4, herramientas 5 y 6), `sofia.maquinaria@gmail.com` (5, herramientas 7 y 8), `andres.rentals@gmail.com` (6, herramientas 9 y 10), `elena.maqpro@gmail.com` (11, herramientas 19 y 20) |
| CLIENTE | ids 12 a 21. Ejemplos: `juan.cliente@gmail.com` (12), `ana.rentas@gmail.com` (13) |

  Cada proveedor tiene 2 herramientas consecutivas: el proveedor *n* tiene las herramientas 2(*n*−2)+1 y 2(*n*−2)+2.

**Ejecutar la app sin Docker**, contra cualquier MySQL:
```bash
DB_URL='jdbc:mysql://localhost:3308/renta_herramientas_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC' \
APP_UPLOADS_DIR=/ruta/temporal \
"/c/Program Files/Eclipse Adoptium/jdk-21.0.12.101-hotspot/bin/java" -jar infraestructura/target/infraestructura-0.0.1-SNAPSHOT.war --server.port=8095
```

---

## 5. Arquitectura

Arquitectura hexagonal en un proyecto **Maven multimódulo**. El `pom.xml` padre usa `spring-boot-starter-parent` 3.2.5,
Java 21 y JJWT 0.12.5.

```
domain          -> modelos, value objects, excepciones y puertos de repositorio. SIN Spring.
application     -> puertos de entrada (casos de uso), DTOs, puertos de salida y servicios. SIN Spring (solo depende de domain).
infraestructura -> Spring Boot (WAR): controllers REST, JPA, seguridad JWT, configuración, scheduler y frontend estático.
```

Paquete base: `com.example.tomcat`.

**domain** (`domain/src/main/java/com/example/tomcat/`)
- `model/`: `Usuario`, `Rol` (enum ADMIN/PROVEEDOR/CLIENTE), `Herramienta`, `Categoria`, `Reserva`, `Pago`, `Factura` y `Mantenimiento`.
  Las reglas de transición de estado viven dentro de los modelos (por ejemplo, `Reserva.aceptar/confirmarPago/entregar/devolver/vencer`).
- `valueobject/`: `Dinero` (no nulo ni negativo) y `Email` (validado con regex).
- `exception/`: `DomainException` (→ 400) y sus subclases `RecursoNoEncontradoException` (404), `ConflictoException` (409),
  `AccesoDenegadoException` (403), `UsuarioInactivoException` (403) y `AuthenticationFailedException` (401).
- `repository/`: puertos `*RepositoryPort`.

**application** (`application/src/main/java/com/example/tomcat/`)
- `port/in/`: casos de uso:
  - autenticación y usuarios: `AutenticarUsuario`, `RegistrarUsuario`, `GestionarUsuario`;
  - catálogo: `GestionarHerramienta`, `GestionarCategoria`, `ConsultarDisponibilidad`;
  - reservas: `CrearReserva`, `GestionarReserva`, `VencerReservas`;
  - pagos y facturas: `ProcesarPago`, `ListarPagos`, `GestionarFactura`;
  - otros: `GestionarMantenimiento`, `ConsultarReportes`, `ConsultarPanel`.
- `port/in/dto/`:
  - `Solicitante(id, rol)`: el usuario autenticado. Lo arma la infraestructura desde el JWT.
  - `Panel`: interfaz sellada con `Panel.Cliente`, `Panel.Proveedor` y `Panel.Admin`.
  - `Reporte*`: los cuatro DTOs de reportes.
- `port/out/`: `PasswordHasherPort` (BCrypt) y `AlmacenamientoImagenPort` (disco local).
- `service/`: implementaciones, más tres clases de apoyo:
  - `ControlAcceso`: reglas de propiedad por rol.
  - `PoliticaVencimiento`: reloj y plazo máximo.
  - `ValidadorImagen`: valida el formato por bytes mágicos.

**infraestructura** (`infraestructura/src/main/java/com/example/tomcat/`)
- `adapter/in/rest/`: controllers, `GlobalExceptionHandler`, `FacturaHtmlRenderer` (factura descargable en HTML)
  y `dto/UsuarioResponse`. Los DTOs de entrada son *records* anidados en cada controller, con Bean Validation.
- `adapter/out/persistence/`: entidades JPA y repositorios Spring Data. `adapter/out/mapper/`: mappers entre entidad y dominio.
- `repository/`: `*RepositoryAdapter`, que implementan los puertos del dominio.
- `adapter/out/storage/AlmacenamientoImagenLocalAdapter`: guarda imágenes en `app.uploads.dir`.
- `security/`:
  - `SecurityConfig`: reglas por URL y rol.
  - `JwtAuthenticationFilter`: lee el token y **consulta la BD** para validar que el usuario exista y esté activo.
  - `JwtTokenProvider`: genera y valida el token.
  - `UsuarioAutenticado`: el principal; su método `solicitante()` arma el `Solicitante`.
  - `JsonSecurityErrorHandler`: respuestas 401/403 en JSON.
  - `BCryptPasswordHasherAdapter`: implementa `PasswordHasherPort`.
- `config/`:
  - `BeanConfig`: crea todos los casos de uso y los envuelve con `TransaccionalProxy`. También define los beans
    `Clock` y `PoliticaVencimiento`.
  - `TransaccionalProxy`: proxy dinámico que ejecuta cada método del caso de uso en un `TransactionTemplate`.
    Las consultas van en modo solo lectura.
  - `VencimientoReservasScheduler`: corre al arrancar y luego según el cron.
  - `WebConfig`: publica `/uploads/**`.
  - `CorsConfig`: solo `localhost` y `127.0.0.1`.
  - `OpenApiConfig`: esquema `bearerAuth`.
- `TomcatApplication`: extiende `SpringBootServletInitializer` (WAR para Tomcat). Un bloque estático fija la
  zona por defecto de la JVM en **UTC**.

**Convenciones:**
- Los servicios **no tienen anotaciones de Spring**. Se cablean en `BeanConfig`.
- Los controllers reciben `@AuthenticationPrincipal UsuarioAutenticado usuario` y pasan `usuario.solicitante()`
  al caso de uso. **Nunca se confía en ids de cliente o proveedor enviados en la petición.**
- Mensajes de error y comentarios en español.
- Las respuestas de error tienen el formato `{timestamp, status, error, message}`. En errores de validación se
  agrega `errores: {campo: mensaje}`.

---

## 6. Reglas de negocio vigentes

### 6.1 Permisos (ControlAcceso + SecurityConfig)
- **ADMIN:** ve y gestiona todo.
  - Al crear una herramienta debe indicar `proveedorId`; al crear una reserva, `clienteId`.
  - Gestiona usuarios (`/api/usuarios/**`) y categorías, ve los reportes y puede forzar el vencimiento.
- **PROVEEDOR:**
  - Solo edita o elimina **sus** herramientas.
  - Solo acepta, rechaza, entrega o recibe devoluciones de reservas **sobre sus herramientas**.
  - Solo ve pagos, facturas y mantenimientos relacionados con ellas.
  - Al crear una herramienta, siempre queda a su nombre.
- **CLIENTE:**
  - Solo ve y paga **sus** reservas, pagos y facturas.
  - Al reservar, la reserva siempre queda a su nombre.
- **Registro público** (`POST /api/auth/registro`): solo crea cuentas CLIENTE. Pedir rol PROVEEDOR devuelve 403.
  Los proveedores los crea el ADMIN con `POST /api/usuarios`.
- **Usuario inactivo:**
  - Su login responde 403. Primero se valida la contraseña, para no revelar el estado de la cuenta.
  - Su token vigente deja de servir (401), porque el filtro consulta la BD.
- **Catálogo** (`GET /api/herramientas`, `GET /api/categorias`): abierto a cualquier usuario autenticado.
- **Imágenes** (`/uploads/**`): públicas, sin token.

### 6.2 Ciclo de vida de una reserva
```
PENDIENTE --(proveedor acepta)--> ACEPTADA --(cliente paga = confirmación automática)--> CONFIRMADA
   |                                  |                                                    |
   +--(proveedor rechaza)--> RECHAZADA |                                         (proveedor entrega)
   +--(vence)--> VENCIDA <--(vence)----+                                                    v
                                                    FINALIZADA <--(devolución)-- ENTREGADA
```
- **Al crear:** se valida que la herramienta esté DISPONIBLE, que la fecha de inicio no sea pasada (según
  `app.zona-horaria`) y que no se cruce con otras fechas. Se cruzan las reservas en estado PENDIENTE,
  ACEPTADA, CONFIRMADA o ENTREGADA.
- **Plazo máximo:** `app.reservas.plazo-maximo`, por defecto `PT1H` (1 hora).
  - Una reserva PENDIENTE sin aceptar 1 hora después de `created_at` pasa a VENCIDA.
  - Una reserva ACEPTADA sin pagar 1 hora después de `fecha_aceptacion` pasa a VENCIDA.
  - Las dos también vencen si su fecha de inicio ya pasó.
  - Aceptar o pagar una reserva cuyo plazo venció responde 409, aunque el scheduler aún no haya corrido.
  - Una reserva con pago completado nunca vence (protección para datos antiguos).
  - Una reserva VENCIDA libera las fechas.
- **Scheduler de vencimiento:** corre al arrancar y luego según `app.vencimiento.cron` (por defecto
  `0 * * * * *`, cada minuto). El ADMIN también puede ejecutarlo con `POST /api/reservas/vencer`.
- **Pago:**
  - El monto debe ser exactamente `tarifaDiaria × días` (los días se cuentan con ambos extremos incluidos).
  - Métodos: TARJETA, TRANSFERENCIA o EFECTIVO.
  - Una reserva solo admite un pago COMPLETADO.
  - El pago, la factura (`FAC-%06d` con el id del pago) y el paso a CONFIRMADA ocurren en **una sola transacción**.
- **Entrega:** solo si la reserva está CONFIRMADA.
- **Devolución:** el estado del equipo puede ser BUENO, DANADO (también acepta "DAÑADO") o PERDIDO.
  - DANADO: la herramienta pasa a MANTENIMIENTO y se crea un mantenimiento ligado a la reserva.
  - PERDIDO: la herramienta pasa a FUERA_DE_SERVICIO.

### 6.3 Herramientas, mantenimientos y categorías
- **Estados de una herramienta:** DISPONIBLE, MANTENIMIENTO o FUERA_DE_SERVICIO. La tarifa debe ser mayor que 0.
- **PUT parcial:** lo que no se envía se conserva (incluida la descripción).
- **Eliminar una herramienta:** solo si no tiene reservas ni mantenimientos (si no, 409 y se sugiere FUERA_DE_SERVICIO).
- **Imagen:**
  - `POST /api/herramientas/{id}/imagen` (multipart, campo `archivo`): JPG, PNG, WEBP o GIF, máximo 5 MB.
  - El formato se valida por **bytes mágicos**, no por la extensión.
  - El nombre del archivo es un UUID generado por el servidor. Al reemplazar la imagen se borra la anterior.
  - Sigue existiendo `PATCH /{id}/imagen` para asignar una URL externa.
- **Mantenimiento:**
  - Solo puede haber uno abierto por herramienta, y al abrirlo la herramienta pasa a MANTENIMIENTO.
  - Al cerrarlo, la herramienta vuelve a DISPONIBLE **solo si seguía en MANTENIMIENTO**; no reactiva una FUERA_DE_SERVICIO.
  - El costo no puede ser negativo.
- **Categoría:**
  - El nombre es único (sin distinguir mayúsculas). Solo el ADMIN escribe.
  - No se elimina si tiene herramientas (409).
- **Eliminar un usuario:**
  - Un cliente con reservas o un proveedor con herramientas no se elimina (409 y se sugiere `activo=false`).
  - Solo se gestionan CLIENTE y PROVEEDOR.

### 6.4 Fechas y horas
- La JVM usa **UTC**: `created_at`, `fecha_aceptacion`, `fecha_pago` y demás instantes se guardan en UTC, igual que MySQL en Docker.
- "Hoy", las fechas de reserva y el cron usan `app.zona-horaria`, por defecto `America/Bogota`.
  Esa zona es una suposición: el equipo del usuario marca UTC−6. Pregúntale si debe cambiarse.

---

## 7. API (base `/api`)

| Recurso | Endpoints | Quién |
|---|---|---|
| Auth | `POST /auth/registro` (solo crea CLIENTE, 201), `POST /auth/login` → `{token, usuarioId, nombre, email, rol}` | Público |
| Usuarios | `POST /usuarios` (crea PROVEEDOR o CLIENTE), `GET /usuarios[?rol=]`, `GET /usuarios/clientes`, `GET /usuarios/proveedores`, `GET/PUT/DELETE /usuarios/{id}` | ADMIN |
| Categorías | `GET /categorias`, `GET /categorias/{id}` (cualquier rol). `POST`, `PUT /{id}`, `DELETE /{id}` (ADMIN) | Mixto |
| Herramientas | `GET /herramientas[?disponibles=true][&proveedorId=]`, `GET /herramientas/mias`, `GET /{id}` (cualquier rol). `POST`, `PUT /{id}`, `PATCH /{id}/estado`, `PATCH /{id}/imagen` (URL), `POST /{id}/imagen` (archivo), `DELETE /{id}` (ADMIN o el proveedor dueño) | Mixto |
| Reservas | `POST /reservas` (CLIENTE o ADMIN), `GET /reservas[?clienteId&proveedorId&herramientaId]` (filtradas según el rol), `GET /{id}`, `POST /{id}/aceptar`, `/rechazar`, `/entregar`, `/devolver` (proveedor dueño o ADMIN), `POST /reservas/vencer` (ADMIN), `GET /reservas/disponibilidad?fechaInicio&fechaFin`, `GET /reservas/disponibilidad/{herramientaId}?fechaInicio&fechaFin` | Mixto |
| Pagos | `POST /pagos` (CLIENTE dueño o ADMIN; 201), `GET /pagos[?reservaId]`, `GET /pagos/reserva/{id}`, `GET /pagos/{id}` (filtrados según el rol) | Mixto |
| Facturas | `GET /facturas[?clienteId]`, `GET /{id}`, `GET /pago/{pagoId}`, `GET /reserva/{reservaId}`, `GET /{id}/descargar` (HTML adjunto) | Según el rol |
| Mantenimientos | `GET /mantenimientos[?herramientaId|reservaId]`, `POST /mantenimientos` (201), `POST /{id}/cerrar` | ADMIN o PROVEEDOR (los suyos) |
| Reportes | `GET /reportes/ingresos`, `/herramientas-mas-alquiladas`, `/clientes-frecuentes`, `/disponibilidad` | ADMIN |
| Panel | `GET /panel`: `Panel.Cliente` (reservas por estado, próximas, total pagado, facturas), `Panel.Proveedor` (inventario, solicitudes pendientes, ingresos, mantenimientos abiertos) o `Panel.Admin` (totales globales) | Cualquier rol autenticado |

**Códigos HTTP:**
- 200/201/204 si la operación sale bien.
- 400: error de validación (con `errores`), JSON mal formado, parámetro inválido o faltante, o error de dominio.
- 401: sin token o con token inválido.
- 403: el rol no tiene permiso, el recurso no le pertenece o el usuario está inactivo.
- 404: el recurso o la ruta no existe.
- 405 y 415.
- 409: conflicto (correo repetido, fechas que se cruzan, pago duplicado, plazo vencido, historial o FK).
- 413: imagen de más de 5 MB.
- 500: error genérico; el detalle va al log.

---

## 8. Base de datos y migraciones (Flyway, `infraestructura/src/main/resources/db/migration/`)

`spring.jpa.hibernate.ddl-auto=validate`: el esquema lo define **solo Flyway**.
**Nunca modifiques una migración ya aplicada**; crea una nueva. **La siguiente versión libre es V9.**

| Versión | Contenido |
|---|---|
| V1 | Tablas `roles`, `usuarios`, `categorias`, `herramientas`, `reservas` (con `created_at`), `pagos`, `mantenimientos` |
| V2 | Datos de prueba: 3 roles, 21 usuarios, 20 categorías, 20 herramientas, 20 reservas y 20 pagos (todos COMPLETADO) |
| V3 | `usuario_roles` (N:M) y asignación de roles |
| V4 | `herramientas.imagen_url`, `reservas.fecha_entrega/fecha_devolucion/estado_devolucion` e índices |
| V5 | `reservas.observaciones_devolucion`, `mantenimientos.reserva_id` (SET NULL) y tabla `facturas` con índices |
| V6 | Datos de prueba: 20 mantenimientos y 20 facturas generadas desde los pagos |
| V7 | Las FKs pasan de `ON DELETE CASCADE` a `RESTRICT`, para conservar historial. `usuario_roles` mantiene CASCADE y `mantenimientos.reserva_id` mantiene SET NULL |
| V8 | `reservas.fecha_aceptacion`; las reservas ACEPTADA con pago pasan a CONFIRMADA; índice `idx_reservas_estado` |

**Problemas conocidos de los datos de prueba** (se corrigen en la Fase 4 con una migración V9):
- Hay pagos COMPLETADO para reservas PENDIENTE (2, 6, 10, 12, 16, 19) y RECHAZADA (8). Esos pagos falsos evitan
  hoy que las reservas PENDIENTE venzan.
- Algunos montos no cuadran con `tarifa × días` (por ejemplo, la reserva 2: 120000 × 8 = 960000, pero el pago es de 840000).
- Las reservas FINALIZADA no tienen `fecha_entrega` ni `fecha_devolucion`.
- **Ojo:** al quitar los pagos falsos, las reservas PENDIENTE de los datos de prueba vencerán 1 hora después de crear
  la base (por la regla del plazo). Pregunta al usuario cómo quiere los datos de demostración.

**Cómo se prueba sin ensuciar la base del usuario:**
```bash
docker run -d --name mysql_prueba_X -e MYSQL_DATABASE=renta_herramientas_db -e MYSQL_USER=mysql \
  -e MYSQL_PASSWORD=mysql -e MYSQL_ROOT_PASSWORD=mysql -p 3308:3306 mysql:8.4
# esperar "ready for connections ... port: 3306" en docker logs, luego levantar la app en 8095 (sección 4),
# ejecutar un script de pruebas HTTP y al final: detener la app y `docker rm -f mysql_prueba_X`.
```
Para probar los plazos sin esperar, se adelanta el tiempo en la base temporal, por ejemplo:
`UPDATE reservas SET created_at = UTC_TIMESTAMP() - INTERVAL 61 MINUTE WHERE id = ...`.

---

## 9. Plan por fases y estado

| Fase | Estado | Commit |
|---|---|---|
| 0. Preparación: git init, `.env` fuera de Git, comprobar el build | ✅ | `f4072ed` |
| 1. Seguridad y reglas de acceso por rol | ✅ (36 pruebas HTTP OK) | `07e8b3e` |
| Extra: migración a Java 21 y ajustes de Docker (MySQL en 3307, `.dockerignore`) | ✅ | `8bb51e8` |
| Extra: eliminar código y archivos sin uso | ✅ | `132b818` |
| 2. Robustez: transacciones, 404/409/500, Bean Validation, V7, no perder historial | ✅ (36 pruebas OK) | `7b2040d` (lo hizo el usuario) |
| 3. Funcionalidades: CRUD de categorías, subida de imágenes, vencimiento, panel; ajuste de confirmación automática y plazo de 1 h (V8) | ✅ (42 + 28 pruebas OK) | `3ba6c04`, `60eaaea` (los hizo el usuario) |
| **4. Base de datos** | ⏳ **Siguiente** | — |
| 5. Documentación de la API: Swagger (`@Operation`, `@Tag`, ejemplos JSON) y colección de Postman | Pendiente | — |
| 6. Frontend por rol | Pendiente | — |
| 7. Pruebas automatizadas | Pendiente | — |
| 8. Cierre: README y verificación final | Pendiente | — |

**Fase 4, qué falta:**
- V9 que corrija los datos de prueba (ver la sección 8): pagos coherentes con el estado, montos = tarifa × días,
  fechas de entrega y devolución en las reservas FINALIZADA, y algunas reservas en estados variados (CONFIRMADA, ENTREGADA, VENCIDA).
- Al menos 20 registros por entidad, que el enunciado exige.
- `sql/schema.sql`: hoy solo tiene `CREATE DATABASE`. Debe quedar como el esquema completo equivalente a V1–V8.
- `sql/data.sql` con los datos.
- Diagrama relacional en imagen (por ejemplo `docs/diagrama-er.png`).
- Revisar índices.

**Fase 6, qué falta:** hoy `infraestructura/src/main/resources/static/index.html` + `app.js` es mínimo:
- Hay login, catálogo y un formulario de alta de herramienta.
- `proveedorId: 2` está fijo en el código; el backend lo ignora cuando quien registra es un PROVEEDOR.
- El botón "Reservar" solo muestra un `alert`.

Debe convertirse en:
- registro y paneles por rol, usando `GET /api/panel`;
- para el proveedor, botones **Aceptar/Rechazar** las solicitudes pendientes, y marcar entrega y devolución;
- para el cliente: reservar con fechas, pagar, descargar la factura y ver su historial;
- para el admin: usuarios, categorías, pagos y reportes;
- subida de imágenes;
- diseño responsivo con Bootstrap 5.
- El frontend debe enviar `Authorization: Bearer <token>`. El token se guarda en `localStorage`.

**Fase 7:** no existe `src/test` en ningún módulo. Plan:
- unitarias de dominio y servicios con JUnit 5 + Mockito, inyectando un `Clock` fijo en `PoliticaVencimiento`;
- integración de controllers y seguridad con MockMvc + Testcontainers MySQL (el usuario tiene Docker) o H2.

**Fase 8:** el README actual está incompleto: se corta en la sección de clonar y menciona PostgreSQL.
Debe cubrir lo que pide el enunciado (sección 4 de `explicacion.md`).

---

## 10. Configuración relevante

`infraestructura/src/main/resources/application.yml` (todas las claves admiten una variable de entorno):

| Clave | Variable | Valor por defecto |
|---|---|---|
| `spring.datasource.url` | `DB_URL` | `jdbc:mysql://localhost:3307/renta_herramientas_db?...` |
| `spring.servlet.multipart` | — | máximo 5 MB por archivo y 6 MB por petición |
| `jwt.secret` | `JWT_SECRET` | Hay un valor de desarrollo en el yml. **No lo copies a otros archivos** |
| `jwt.expiration-ms` | — | 24 h |
| `app.zona-horaria` | `APP_ZONA_HORARIA` | `America/Bogota` |
| `app.uploads.dir` | `APP_UPLOADS_DIR` | `uploads` (en Docker: `/usr/local/tomcat/uploads`, en un volumen) |
| `app.reservas.plazo-maximo` | `APP_RESERVAS_PLAZO_MAXIMO` | `PT1H` |
| `app.vencimiento.cron` | `APP_VENCIMIENTO_CRON` | `0 * * * * *` |

**Archivos de entorno y Docker:**
- `.env`: local e ignorado por Git, con `DB_URL` al puerto 3307. `.env.example` es la plantilla versionada.
- `.gitignore` incluye `.env` y `uploads/`.
- `.dockerignore` excluye `**/target/`, `.git/`, `.idea/`, `.vscode/`, `.env`, `*.iml` y `uploads/`.
- `Dockerfile`: dos etapas, `maven:3.9.6-eclipse-temurin-21` → `tomcat:10.1-jdk21`. El WAR se despliega como `ROOT.war`.
- `compose.yaml`:
  - servicio `mysql` (`mysql:8.4`, puerto 3307:3306, healthcheck);
  - servicio `tomcat` (puerto 8080, variables `DB_*`, `APP_UPLOADS_DIR` y `APP_ZONA_HORARIA`, volumen `uploads`).

---

## 11. Decisiones tomadas y su motivo (no las revoques sin consultar)

1. **`application` no depende de Spring.** Las transacciones se aplican en la infraestructura con `TransaccionalProxy`,
   para mantener limpia la arquitectura hexagonal.
2. **Las reglas de propiedad van en la capa application** (`ControlAcceso`), no en los controllers. Así se pueden probar
   en la Fase 7.
3. **El filtro JWT consulta la BD en cada petición**, para revocar el acceso a usuarios inactivos o eliminados. El rol
   también se toma de la BD.
4. **La confirmación automática ocurre al pagar.** El proveedor sigue aceptando o rechazando. Esto lo pidió el usuario
   y es la forma de cumplir a la vez las líneas 63, 171, 366 y 368 de `explicacion.md`.
5. **El plazo de 1 hora** para aceptar o pagar es un requisito explícito del usuario.
6. **Borrado restringido en lugar de cascada**, para no perder historial de reservas, pagos y facturas.
7. **Las imágenes se guardan en disco, en un volumen de Docker**, no en la BD. Se validan por contenido.
8. **El MySQL de Docker se publica en el 3307**, por el choque con el servicio `MySQL80` de Windows.
9. **Java 21**, porque lo pide el enunciado.

---

## 12. Lista de comprobación para continuar

1. Lee `explicacion.md` y este archivo.
2. Ejecuta `git log --oneline` y `git status` para confirmar en qué estado está el repositorio.
3. Compila con el JDK 21 (sección 3).
4. Haz la siguiente fase pendiente (sección 9), una a la vez.
5. Verifica con pruebas HTTP contra un **MySQL temporal** (sección 8), no contra `mysql_rentas`.
6. Si cambiaste algo del backend, reconstruye el Docker del usuario con `docker compose up -d --build`, y solo haz
   comprobaciones de lectura en su base.
7. Informa al usuario en español qué cambió, qué verificaste y el **mensaje de commit sugerido**. El commit lo hace él.
