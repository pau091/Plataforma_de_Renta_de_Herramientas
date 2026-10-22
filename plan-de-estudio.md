# Plan de estudio para la sustentación

**Proyecto:** Plataforma de Renta de Herramientas y Equipos de Construcción
**Duración sugerida:** unas 6 horas, en 8 fases. Haz las fases en orden: cada una se apoya en la anterior.

**Cómo estudiar cada fase:**
1. Lee la explicación.
2. Abre los archivos indicados en el IDE.
3. Responde en voz alta las preguntas del final **sin mirar**.

Si puedes explicar una fase en 2 minutos con tus palabras, pasa a la siguiente.

| Fase | Tema | Tiempo |
|---|---|---|
| 1 | Visión general y requisitos | 30 min |
| 2 | Arquitectura hexagonal, módulos e imports | 60 min |
| 3 | Capa `domain` (reglas de negocio) | 45 min |
| 4 | Capa `application` (casos de uso) | 45 min |
| 5 | Capa `infraestructura` (Spring, REST, JPA, seguridad) | 75 min |
| 6 | Base de datos, migraciones y datos | 30 min |
| 7 | Frontend | 30 min |
| 8 | Pruebas, despliegue y ensayo de la demo | 45 min |

---

## Fase 1 · Visión general y requisitos (30 min)

### Qué es el proyecto, en una frase
Una plataforma web donde los **proveedores** publican herramientas y maquinaria, los **clientes** las reservan por
fechas y pagan en línea con factura automática, y el **administrador** supervisa usuarios, pagos, daños y rentabilidad.

### Los 3 roles
| Rol | Qué hace |
|---|---|
| **ADMIN** | Gestiona proveedores, clientes, herramientas y categorías; ve todo el historial; reembolsa pagos; reportes; incidencias |
| **PROVEEDOR** | Publica y edita **sus** herramientas (con imagen); acepta o rechaza solicitudes; registra entregas y devoluciones con daños; mantenimientos |
| **CLIENTE** | Busca en el catálogo, reserva por fechas, paga, descarga facturas, ve su historial, cambia fechas o cancela |

### El flujo central (apréndelo de memoria: casi todas las preguntas pasan por aquí)
```
PENDIENTE --(proveedor acepta)--> ACEPTADA --(cliente paga = confirmación automática + factura)--> CONFIRMADA
    |                                |                                                               |
    |--(proveedor rechaza)--> RECHAZADA |--(cliente cancela)--> CANCELADA <--(admin reembolsa)----------|
    |--(cliente cancela)----> CANCELADA |--(no paga en 1 hora)--> VENCIDA                  (proveedor entrega)
    |--(no la aceptan en 1 hora)--> VENCIDA                                                          v
                                                 FINALIZADA <--(devolución: BUENO / DANADO / PERDIDO)-- ENTREGADA
```

### Tecnologías (y por qué cada una)
| Tecnología | Para qué | Por qué |
|---|---|---|
| **Java 21** | Lenguaje | Lo pide el enunciado; versión LTS con *records*, `switch` moderno y *pattern matching* |
| **Spring Boot 4.1.1** | Framework del backend | El enunciado pide la **última versión** |
| **Spring Security + JWT** | Autenticación y roles | Seguridad sin sesión (*stateless*), ideal para una API consumida por un frontend JS |
| **Spring Data JPA / Hibernate 7** | Acceso a datos | Repositorios sin escribir SQL repetitivo |
| **MySQL 8.4** | Base de datos | La pide el enunciado |
| **Flyway** | Migraciones | El esquema queda versionado (V1…V12) y se aplica solo al arrancar |
| **springdoc (Swagger)** | Documentación | La pide el enunciado; se genera a partir del código |
| **HTML, CSS, JS + Bootstrap 5** | Frontend | El enunciado pide JS puro; Bootstrap da diseño responsivo |
| **Docker + Tomcat 11** | Despliegue | El enunciado pide Apache Tomcat; Docker hace que funcione igual en cualquier computador |
| **JUnit 6, Mockito, Testcontainers** | Pruebas | Unitarias rápidas y de integración contra un MySQL real |

### Preguntas para practicar
- ¿Qué problema resuelve la plataforma y quiénes la usan?
- Dibuja de memoria el flujo de estados de una reserva.
- ¿Por qué Java 21 y Spring Boot 4.1.1?

---

## Fase 2 · Arquitectura hexagonal, módulos e imports (60 min) ⭐ la más importante

### Qué es la arquitectura hexagonal ("puertos y adaptadores")
Es separar el **núcleo del negocio** (las reglas: cuándo se puede pagar, cuándo vence una reserva…) de los **detalles
técnicos** (HTTP, base de datos, Spring). El núcleo define **puertos**, que son interfaces. Los detalles técnicos son
**adaptadores**, que implementan o usan esos puertos.

**Por qué se eligió:**
- **Las reglas no dependen del framework.** El dominio se puede probar sin levantar Spring ni la base de datos (las 76 pruebas de `domain` corren en menos de 1 segundo).
- **Se puede cambiar un detalle técnico sin tocar el negocio.** Por ejemplo, cambiar MySQL por otra base solo afecta a `infraestructura`.
- **Cada capa tiene una responsabilidad clara.** El enunciado pide "separación adecuada de capas".

### Los 3 módulos Maven
```
tomcat/                    ← proyecto padre (pom.xml): versión de Spring Boot, Java 21, versiones de librerías
├── domain/                ← EL NEGOCIO: modelos, reglas, excepciones, puertos de repositorio.   NO usa Spring.
├── application/           ← LOS CASOS DE USO: qué puede hacer cada rol.  Depende SOLO de domain. NO usa Spring.
└── infraestructura/       ← LO TÉCNICO: Spring Boot, REST, seguridad, JPA, Flyway, frontend.    Genera el WAR.
```

**La regla de dependencias** (las flechas siempre apuntan hacia el centro):
```
infraestructura  ──depende de──►  application  ──depende de──►  domain
```
- `domain/pom.xml` no tiene dependencias propias, solo las de pruebas.
- `application/pom.xml` solo declara `domain`.
- `infraestructura/pom.xml` declara `domain`, `application` y todo Spring.

Así, **Maven impide** que alguien importe Spring desde el dominio: no compilaría. Abre los tres `pom.xml` y míralo.

### Cómo se conectan los imports (ejemplo real)
Todo el código está en el paquete `com.example.tomcat`, repartido en los 3 módulos:

| Módulo | Paquetes | Importa de |
|---|---|---|
| `domain` | `model`, `valueobject`, `exception`, `repository` (puertos) | Solo Java estándar |
| `application` | `port.in` (casos de uso), `port.in.dto`, `port.out`, `service` | `com.example.tomcat.model`, `.exception`, `.repository` (domain) |
| `infraestructura` | `adapter.in.rest`, `adapter.out.*`, `repository` (adaptadores), `security`, `config` | Todo lo anterior + Spring |

Ejemplo con el pago:
- `PagoController` (infraestructura) importa `ProcesarPagoUseCase` (interfaz de **application**).
- `ProcesarPagoService` (application) implementa esa interfaz e importa `Pago`, `Reserva` y `Factura` (**domain**) y `PagoRepositoryPort` (puerto de **domain**).
- `PagoRepositoryAdapter` (infraestructura) **implementa** `PagoRepositoryPort` usando JPA.

El servicio **nunca** importa el adaptador: solo conoce la interfaz. Eso es **inversión de dependencias** (la D de SOLID).

### Recorrido de una petición de principio a fin: `POST /api/pagos`
```
Navegador (js/vistas/comun.js → api.post('/pagos'))  con  Authorization: Bearer <token>
  │
  ▼  infraestructura/security/JwtAuthenticationFilter      valida el token y consulta el usuario en la BD
  ▼  infraestructura/security/SecurityConfig               ¿el rol CLIENTE puede hacer POST /api/pagos? Sí
  ▼  infraestructura/adapter/in/rest/PagoController        valida el JSON (@Valid) y arma el Solicitante
  ▼  infraestructura/config/TransaccionalProxy             abre una TRANSACCIÓN
  ▼  application/service/ProcesarPagoService               reglas: dueño, no pagada, no vencida, monto = tarifa × días
  │     ├─ domain/model/Reserva.confirmarPago()            ACEPTADA → CONFIRMADA
  │     ├─ PagoRepositoryPort.guardar(pago)                 → PagoRepositoryAdapter → JPA → MySQL
  │     ├─ ReservaRepositoryPort.guardar(reserva)
  │     └─ FacturaRepositoryPort.guardar(factura)           FAC-000025
  ▼  TransaccionalProxy hace COMMIT (si algo falló antes: ROLLBACK de todo)
  ▼  PagoController responde 201 con el pago en JSON
```
Si hay un error (por ejemplo, un monto incorrecto), el servicio lanza una `DomainException`. `GlobalExceptionHandler` la
convierte en un **400** con el formato `{timestamp, status, error, message}`.

### Preguntas para practicar
- ¿Qué es la arquitectura hexagonal y por qué la usaste?
- ¿Qué hay en cada módulo y cuál depende de cuál? ¿Cómo garantiza Maven esa regla?
- ¿Qué es un puerto y qué es un adaptador? Da un ejemplo real del proyecto.
- Explica paso a paso qué pasa cuando un cliente paga.

---

## Fase 3 · Capa `domain`: el negocio puro (45 min)

**Ruta:** `domain/src/main/java/com/example/tomcat/`

| Carpeta | Qué contiene | Archivos clave |
|---|---|---|
| `model/` | Las entidades del negocio **con su comportamiento** | `Reserva`, `Pago`, `Factura`, `Herramienta`, `Mantenimiento`, `Usuario`, `Categoria`, `Rol` |
| `valueobject/` | Valores que se validan solos | `Dinero` (no nulo ni negativo), `Email` (formato válido) |
| `exception/` | Errores del negocio, cada uno con su código HTTP | `DomainException` (400), `RecursoNoEncontradoException` (404), `ConflictoException` (409), `AccesoDenegadoException` (403), `UsuarioInactivoException` (403), `AuthenticationFailedException` (401) |
| `repository/` | **Puertos** (interfaces) para guardar y buscar | `ReservaRepositoryPort`, `PagoRepositoryPort`, … |

### Lo más importante: `model/Reserva.java`
Las reglas de cambio de estado están **dentro del modelo**. Esto se llama *modelo rico*, lo contrario de una clase con
solo getters y setters. Ejemplos:
- `aceptar(ahora)`: solo si está `PENDIENTE`; guarda la hora de aceptación.
- `confirmarPago()`: solo si está `ACEPTADA`.
- `entregar()` y `devolver(estadoEquipo, observaciones)`: normaliza "DAÑADO" a `DANADO`.
- `cancelar()`: si ya está pagada, avisa que debe hacerse un reembolso.
- `debeVencer(hoy, limite)`: vence si pasó 1 hora sin respuesta o si ya llegó la fecha de inicio.
- `validarFechas(inicio, fin, hoy)`: la misma regla se usa al crear y al modificar.

**Por qué así:** si cualquier clase pudiera hacer `reserva.setEstado("CONFIRMADA")`, nada impediría estados imposibles.
Con métodos como `confirmarPago()` el objeto **protege sus propias reglas** (encapsulamiento).

### Otros detalles
- **`Pago.normalizarMetodo()`** acepta TARJETA, TRANSFERENCIA o EFECTIVO.
- **`Pago.reembolsar()`** solo procede si el pago está COMPLETADO.
- **`Factura`** guarda una **copia** del nombre del cliente y de la herramienta. Una factura no debe cambiar si mañana se edita el cliente. Por eso no está normalizada en esos campos, y es a propósito.
- **`Herramienta.cambiarEstado()`** solo admite DISPONIBLE, MANTENIMIENTO o FUERA_DE_SERVICIO.

### Preguntas para practicar
- ¿Por qué `domain` no tiene ninguna anotación de Spring ni de JPA?
- ¿Dónde está la regla "no se puede entregar una reserva sin pagar"? (en `Reserva.entregar()`)
- ¿Qué es un objeto de valor? Explica `Dinero` y `Email`.
- ¿Por qué la factura copia el nombre del cliente?

---

## Fase 4 · Capa `application`: los casos de uso (45 min)

**Ruta:** `application/src/main/java/com/example/tomcat/`

| Carpeta | Qué contiene |
|---|---|
| `port/in/` | **Interfaces de casos de uso**: lo que el sistema ofrece (`CrearReservaUseCase`, `ProcesarPagoUseCase`, `GestionarHerramientaUseCase`, `ConsultarReportesUseCase`…) |
| `port/in/dto/` | Datos que entran o salen de los casos de uso: `Solicitante` (quién pide), `Panel` (Cliente, Proveedor o Admin) y los `Reporte*` |
| `port/out/` | Puertos hacia afuera que no son repositorios: `PasswordHasherPort` (BCrypt) y `AlmacenamientoImagenPort` (disco) |
| `service/` | **Implementaciones** de los casos de uso, más tres clases de apoyo |

### Las clases de apoyo (te las pueden preguntar)
- **`ControlAcceso`**: reglas de **propiedad**. Un proveedor solo gestiona sus herramientas; un cliente solo ve y paga sus reservas. Están aquí y no en los controllers para que se puedan probar con pruebas unitarias.
- **`PoliticaVencimiento`**: recibe un `Clock` (reloj) y el plazo de 1 hora. Al recibir el reloj desde afuera, las pruebas usan un reloj **fijo** y no dependen de la hora real.
- **`ValidadorImagen`**: revisa los **bytes iniciales** del archivo (los "números mágicos") para saber si es JPG, PNG, WEBP o GIF. No se fía de la extensión, porque un `virus.png` podría ser texto.

### `Solicitante`: la decisión de seguridad clave
Los servicios reciben `Solicitante(id, rol)`, construido a partir del **JWT**, nunca de lo que llega en el JSON. Por
eso, aunque un cliente envíe `"clienteId": 99`, la reserva queda **a su nombre**. Nunca se confía en ids enviados por
el usuario.

### Ejemplo: `service/ProcesarPagoService.java`
1. Busca la reserva → si no existe, `RecursoNoEncontradoException` (404).
2. ¿El solicitante es el cliente dueño o un admin? Si no, `AccesoDenegadoException` (403).
3. ¿Ya tiene un pago completado? → `ConflictoException` (409).
4. ¿Venció el plazo de 1 hora? → 409.
5. `reserva.confirmarPago()` (regla del dominio).
6. ¿El monto es exactamente `tarifa × días`? Si no, `DomainException` (400).
7. Guarda el pago, la reserva y la factura `FAC-%06d`.

### ¿Por qué `application` no usa Spring (ni siquiera `@Service` o `@Transactional`)?
Para que la lógica sea Java puro. La infraestructura crea los servicios en `BeanConfig` y les agrega las transacciones
con `TransaccionalProxy` (fase 5).

### Preguntas para practicar
- ¿Qué diferencia hay entre `port/in` y `service`?
- ¿Por qué el `Solicitante` sale del token y no del cuerpo de la petición?
- ¿Cómo se prueba el vencimiento de 1 hora sin esperar una hora? (con un `Clock` fijo)
- ¿Cómo sabes que una imagen es de verdad una imagen?

---

## Fase 5 · Capa `infraestructura`: Spring, REST, JPA y seguridad (75 min)

**Ruta:** `infraestructura/src/main/java/com/example/tomcat/`

| Carpeta | Qué hace | Archivos clave |
|---|---|---|
| raíz | Punto de entrada | `TomcatApplication`: `@SpringBootApplication`; extiende `SpringBootServletInitializer` para desplegarse como **WAR en Tomcat**; fija la JVM en UTC |
| `adapter/in/rest/` | **Adaptadores de entrada**: los controllers REST | `ReservaController`, `PagoController`, `HerramientaController`… · `GlobalExceptionHandler` · `FacturaHtmlRenderer` |
| `adapter/in/rest/dto/` | DTO de respuesta | `UsuarioResponse` (nunca expone la contraseña) |
| `adapter/out/persistence/entity/` | **Entidades JPA** (`@Entity`) que mapean las tablas | `ReservaEntity`, `PagoEntity`… |
| `adapter/out/persistence/repository/` | Repositorios **Spring Data** (`JpaRepository`) | `SpringDataReservaRepository` (con la consulta `existsSolape`) |
| `adapter/out/mapper/` | Convierten entidad JPA ↔ modelo del dominio | `ReservaJpaMapper`… |
| `repository/` | **Adaptadores de salida**: implementan los puertos del dominio | `ReservaRepositoryAdapter implements ReservaRepositoryPort` |
| `adapter/out/storage/` | Guarda las imágenes en disco | `AlmacenamientoImagenLocalAdapter` |
| `security/` | JWT y Spring Security | `SecurityConfig`, `JwtAuthenticationFilter`, `JwtTokenProvider`, `UsuarioAutenticado`, `JsonSecurityErrorHandler`, `BCryptPasswordHasherAdapter` |
| `config/` | Configuración | `BeanConfig`, `TransaccionalProxy`, `CorsConfig`, `OpenApiConfig`, `WebConfig`, `VencimientoReservasScheduler` |

### ¿Por qué hay entidad JPA y además modelo del dominio? (pregunta muy probable)
El modelo `Reserva` (domain) no puede tener `@Entity`, porque eso es JPA, un detalle técnico. Por eso existe
`ReservaEntity` (infraestructura) y un **mapper** que copia los datos entre los dos. Es más código, pero el dominio
queda limpio y se puede probar solo.

### `config/BeanConfig.java`: dónde se "arma" todo
Como los servicios no tienen `@Service`, aquí se crean con `new` y se registran como beans de Spring. Cada uno se
envuelve con `TransaccionalProxy`:
```java
return TransaccionalProxy.envolver(ProcesarPagoUseCase.class,
        new ProcesarPagoService(pagoRepository, reservaRepository, ...), tx, ESCRITURA);
```
Spring inyecta los adaptadores (`PagoRepositoryAdapter`…) en los parámetros, porque implementan los puertos.

### `config/TransaccionalProxy.java`
Es un **proxy dinámico de Java** (`java.lang.reflect.Proxy`). Cada llamada a un método del caso de uso se ejecuta
dentro de un `TransactionTemplate`.
- **Si algo falla**, se deshace todo (*rollback*). Por ejemplo, si falla al guardar la factura, tampoco queda el pago.
- **Las consultas van en modo solo lectura.**
- **Hace lo mismo que `@Transactional`, pero sin poner Spring dentro de `application`.**

### Seguridad, paso a paso
1. **Login** (`AuthController` → `AutenticarUsuarioService`): verifica la contraseña con **BCrypt**. Un usuario inactivo recibe 403; antes se comprueba la clave, para no revelar si la cuenta existe.
2. **`JwtTokenProvider`** genera el token: firmado con **HS512**, válido **24 horas**, con `sub` (correo), `rol` y `usuarioId`.
3. **En cada petición, `JwtAuthenticationFilter`:**
   - lee `Authorization: Bearer <token>` y valida la firma;
   - **consulta la base de datos**: si el usuario fue desactivado o eliminado, el token deja de servir al instante;
   - toma el rol de la base de datos, no del token.
4. **`SecurityConfig`:**
   - sin sesión (`STATELESS`) y sin CSRF, porque se usa un token y no cookies;
   - reglas por URL y rol (`hasRole("ADMIN")`…);
   - **el orden importa**: las reglas específicas van antes que las generales.
5. **`JsonSecurityErrorHandler`** responde 401 o 403 en JSON.
6. **`UsuarioAutenticado`** es el usuario dentro de Spring. Los controllers lo reciben con `@AuthenticationPrincipal` y llaman a `usuario.solicitante()`.

### Otros detalles de esta capa
- **Validación:** los DTO de entrada son *records* con `@NotNull`, `@Positive`, etc. `@Valid` los revisa y `GlobalExceptionHandler` devuelve 400 con `errores: {campo: mensaje}`.
- **CORS** (`CorsConfig`): acepta `localhost` y `127.0.0.1` en cualquier puerto.
- **Swagger** (`OpenApiConfig`):
  - página principal con la explicación de JWT, los roles y los errores;
  - ejemplos JSON reales;
  - cada controller lleva `@Tag`, `@Operation` y `@ApiResponse`.
- **Vencimiento automático** (`VencimientoReservasScheduler`): `@Scheduled` cada minuto, más una ejecución al arrancar.
- **Imágenes:** `WebConfig` publica `/uploads/**` desde el disco; las fotos de demostración están en `static/images/`.

### Preguntas para practicar
- ¿Qué es un JWT, qué contiene y cómo se valida?
- ¿Por qué el filtro consulta la base de datos si el token ya trae el rol?
- ¿Por qué `STATELESS` y sin CSRF?
- ¿Para qué sirve el mapper? ¿Por qué no usar la entidad JPA en todas partes?
- ¿Cómo se manejan las transacciones si `application` no tiene `@Transactional`?
- ¿Dónde se convierten las excepciones en códigos HTTP?

---

## Fase 6 · Base de datos, migraciones y datos (30 min)

**Archivos:**
- `infraestructura/src/main/resources/db/migration/V1…V12`
- `sql/schema.sql` y `sql/data.sql`
- `docs/diagrama-er.png`

### Tablas (9) y relaciones
- `usuarios` **N:M** `roles`, a través de `usuario_roles`.
- Un **proveedor** (usuario) tiene muchas `herramientas`; cada herramienta pertenece a una `categoria`.
- Un **cliente** tiene muchas `reservas`; cada reserva es de una herramienta.
- Una reserva tiene `pagos`; cada pago genera **una** `factura`.
- Una herramienta tiene `mantenimientos`; si vienen de una devolución con daño, quedan ligados a la reserva.

### Decisiones que debes poder justificar
- **Flyway con `ddl-auto: validate`:** Hibernate **no** crea tablas; solo comprueba que coincidan. El esquema lo definen las migraciones, en orden y versionadas. **Nunca se edita una migración ya aplicada**: se crea una nueva.
- **`RESTRICT` en las llaves foráneas** (V7): no se borran reservas, pagos ni facturas en cascada. Para "borrar" un usuario con historial se **desactiva** (`activo=false`).
- **Restricciones `CHECK`** (V10): la base de datos también rechaza estados inválidos, montos negativos o fechas al revés. Es una doble protección.
- **Índices** (V4, V10): en llaves foráneas y estados, y uno compuesto `(herramienta_id, fecha_inicio, fecha_fin)` para la consulta de cruce de fechas.
- **20 o más registros por entidad** (V11): 20 proveedores, 20 clientes, 30 herramientas, 34 reservas, 24 pagos. Las fechas son **relativas** al día de creación (`CURDATE() ± n`), para que la demo se vea coherente cualquier día.
- **Fotos** (V12): cada herramienta de demostración apunta a su imagen en `/images/herramientas/`.
- **Doble camino:** `schema.sql` + `data.sql` crean lo mismo que Flyway, comprobado tabla por tabla.

### Preguntas para practicar
- ¿Por qué Flyway y no dejar que Hibernate cree las tablas?
- ¿Qué pasa si intento borrar un cliente que tiene reservas?
- ¿Cómo se evita que una herramienta se reserve dos veces en las mismas fechas? (la consulta `existsSolape`, más el índice compuesto)
- ¿La base está normalizada? ¿Qué excepción hiciste a propósito y por qué? (la copia de datos en `facturas`)

---

## Fase 7 · Frontend (30 min)

**Ruta:** `infraestructura/src/main/resources/static/`. Spring Boot lo sirve en `/`. Es HTML, CSS y JS **puro** con
módulos ES, sin frameworks; solo Bootstrap 5 para el diseño.

| Archivo | Qué hace |
|---|---|
| `index.html` | Página única: carga Bootstrap, los íconos, la fuente Inter y `js/app.js` como `type="module"` |
| `js/app.js` | **Enrutador**: según el rol arma el menú y muestra la vista de `#/ruta` (`#/catalogo`, `#/reservas`…) |
| `js/api.js` | Cliente de la API: agrega `Authorization: Bearer`, guarda la sesión en `localStorage` y, ante un 401, cierra la sesión |
| `js/ui.js` | Utilidades: plantilla `html` que **escapa todo** (anti-XSS), formato de moneda y fechas, avisos (toasts), ventanas modales |
| `js/vistas/auth.js` | Inicio de sesión, registro y botones de las cuentas de demostración |
| `js/vistas/comun.js` | Piezas compartidas: lista de reservas con acciones según el rol, ficha de la herramienta, pago, devolución, inventario, mantenimientos, facturas |
| `js/vistas/cliente.js`, `proveedor.js`, `admin.js` | Las pantallas de cada rol |
| `css/estilos.css` | Colores, login dividido, tarjetas y tablas en forma de tarjeta en el celular |

**Cómo se conectan los imports del frontend:**
- `app.js` importa las vistas;
- las vistas importan `api.js`, `ui.js` y `comun.js`;
- `index.html` solo carga `app.js`.

**Detalles que suman puntos:**
- **Protección contra XSS:** todo texto que viene de la API se escapa. Lo probé con una categoría llamada `<img src=x onerror=alert(1)>`, que se muestra como texto.
- **Responsivo:** en el celular el menú se abre como panel lateral y las reservas se muestran como tarjetas.
- **Seguridad en el cliente:** si el token vence o el usuario se desactiva, la app vuelve al login con un aviso.

### Preguntas para practicar
- ¿Cómo sabe el frontend qué menú mostrar? (por el `rol` que devuelve el login)
- ¿Dónde se guarda el token y cómo se envía?
- ¿Qué es XSS y cómo lo evitaste?

---

## Fase 8 · Pruebas, despliegue y ensayo de la demo (45 min)

### Pruebas (175 en total)
| Tipo | Dónde | Cuántas | Herramientas |
|---|---|---|---|
| Unitarias de dominio | `domain/src/test` | 76 | JUnit 6 + AssertJ |
| Unitarias de servicios | `application/src/test` | 51 | Mockito (repositorios simulados) + `Clock` fijo |
| Integración | `infraestructura/src/test` (`*IT`) | 48 | Spring Boot Test + MockMvc + **Testcontainers** (MySQL 8.4 real en Docker) |

- **Comandos:** `./mvnw test` corre solo las unitarias (sin Docker). `./mvnw verify` corre todas.
- **Por qué Testcontainers y no H2:** las migraciones usan sintaxis de MySQL (`CHECK`, `ALTER … AFTER`). Probar contra el motor real evita sorpresas.
- **Postman:** `postman/` tiene 97 peticiones con 195 pruebas automáticas (login, 401/403, CRUD, flujo completo, reembolso).

### Despliegue
- **`Dockerfile`, en dos etapas:**
  1. imagen con Maven y JDK 21 que compila el WAR;
  2. **Apache Tomcat 11**, que lo despliega como `ROOT.war`.
- **`compose.yaml`:** levanta `mysql` (8.4, puerto 3307) y `tomcat` (puerto 8080). Espera a que MySQL esté sano (*healthcheck*) y guarda las imágenes subidas en un volumen.
- **Por qué WAR y no JAR:** el enunciado pide Apache Tomcat. El WAR también se puede ejecutar con `java -jar`, porque trae un Tomcat embebido.

### Guion de la demo (unos 10 minutos)
1. `docker compose up -d --build` → abrir http://localhost:8080.
2. **Cliente:** catálogo con fotos → ficha ampliada → reservar con fechas.
3. **Proveedor:** aceptar la solicitud en su panel.
4. **Cliente:** pagar → la factura aparece sola → descargarla.
5. **Proveedor:** registrar la entrega → la devolución con daño → la herramienta queda en mantenimiento.
6. **Admin:** incidencias → cerrar la reparación → reportes de rentabilidad.
7. **Swagger:** Authorize con el token y probar un endpoint.
8. **Error a propósito:** intentar pagar con un monto incorrecto (400) o entrar con la cuenta inactiva (403).

> Recuerda el plazo de 1 hora: si preparas la demo con anticipación, crea la reserva justo antes de mostrarla.

---

## Preguntas difíciles que te pueden hacer (con respuesta corta)

| Pregunta | Respuesta |
|---|---|
| ¿Por qué tantas capas para un proyecto así? | Para separar el negocio de la tecnología: se prueba sin base de datos, se puede cambiar MySQL o Spring sin tocar las reglas, y cada clase tiene una sola responsabilidad |
| ¿Dónde está la "confirmación automática"? | En `ProcesarPagoService`: al pagar, `reserva.confirmarPago()` la pasa a CONFIRMADA y se genera la factura en la misma transacción |
| ¿Cómo validas la disponibilidad? | Al crear y al aceptar: la herramienta debe estar DISPONIBLE y la consulta `existsSolape` revisa que no haya otra reserva activa en esas fechas |
| ¿Qué pasa si dos clientes reservan a la vez las mismas fechas? | Las dos quedan PENDIENTE. Al **aceptar** se revuelve a revisar el cruce, así que solo una puede aceptarse |
| ¿Y si un usuario modifica el `clienteId` en la petición? | Se ignora: el cliente sale siempre del token (`Solicitante`) |
| ¿Cómo se maneja un error? | Excepciones propias del dominio → `GlobalExceptionHandler` → JSON con el código HTTP correcto |
| ¿Cómo funcionan los reportes? | `ConsultarReportesService`: ingresos (solo pagos COMPLETADO), rentabilidad (ingresos − costo de mantenimientos), más alquiladas, clientes frecuentes, disponibilidad, incidencias |
| ¿Por qué no se borran los datos? | Para conservar el historial (facturas y pagos son registros contables): se desactivan o se marcan FUERA_DE_SERVICIO |
| ¿Qué cambió al actualizar a Spring Boot 4? | Jackson 3, Spring Security 7, Hibernate 7, Tomcat 11 y starters nuevos (`webmvc`, `flyway`). Se verificó con las 175 pruebas |
| ¿Usaste inteligencia artificial? | Sí, el curso lo permite. Responde con honestidad: explica qué decidiste tú, qué revisaste y cómo lo verificaste |

---

## Lista final antes de dormir
- [ ] Puedo dibujar los 3 módulos y la dirección de las dependencias.
- [ ] Puedo explicar el recorrido de `POST /api/pagos` de principio a fin.
- [ ] Sé qué es un puerto, un adaptador, un mapper y un caso de uso, con un ejemplo de cada uno.
- [ ] Sé cómo funciona el JWT y por qué el filtro consulta la base de datos.
- [ ] Me sé de memoria el flujo de estados de la reserva y la regla de 1 hora.
- [ ] Sé por qué Flyway, por qué `RESTRICT` y para qué son los `CHECK`.
- [ ] Ensayé la demo completa una vez, con el cronómetro.
- [ ] Docker Desktop está abierto y `docker compose up -d --build` funciona en el computador de la presentación.
