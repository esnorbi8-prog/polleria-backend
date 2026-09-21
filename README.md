# Servicio de Pedidos — Sistema Pollería (Caso 2)

Backend en Spring Boot para el sistema integrado (Spring Boot + Angular) de la pollería.

**Primer avance** — cubre los 4 puntos pedidos:

1. **Guardar los pedidos (entidades) en BD usando JPA.**
2. **Controlar los estados** del pedido con una máquina de estados validada.
3. **Avisar cada cambio de estado**, aunque sea por pantalla (historial + notificaciones consultables por endpoint, listas para que Angular las muestre).
4. **Endpoints** REST documentados con Swagger.

**Segundo avance** — prueba de concepto de pasarela de pago con **Mercado Pago (Checkout Pro)**: genera un link de pago real por un pedido, y al aprobarse el pago (manual o por webhook) el pedido pasa automáticamente a `PAGADO`, reusando toda la máquina de estados/historial/notificaciones ya construida. Ver la sección [Pago con Mercado Pago](#pago-con-mercado-pago-checkout-pro).

**Tercer avance** — usuarios y roles (RF-A01, RF-C01, RF-C02): login con JWT para los 5 actores del sistema (Cliente, Cocinero, Repartidor, Cajera, Administrador), registro público de clientes con verificación de email, alta de personal por el administrador, y protección por rol de todos los endpoints existentes. Ver la sección [Usuarios y roles](#usuarios-y-roles-jwt).

## Cómo ejecutar

Requisitos: Java 17+ y Maven (o usar el wrapper si lo agregas).

```bash
cd polleria-backend
mvn spring-boot:run
```

La app levanta en `http://localhost:8080`, con una base de datos H2 persistida
en archivo (`./data/polleria.mv.db`), así que los pedidos no se pierden al
reiniciar. Se cargan automáticamente algunos productos, un cliente de
ejemplo y una cuenta de prueba por cada rol (ver
[Usuarios y roles](#usuarios-y-roles-jwt) y `src/main/resources/data.sql`).

- Swagger UI (probar los endpoints desde el navegador): `http://localhost:8080/swagger-ui.html`
- Consola H2 (ver las tablas): `http://localhost:8080/h2-console` (JDBC URL: `jdbc:h2:file:./data/polleria`, user `sa`, sin password)

### Desplegar en la nube (PostgreSQL)

Cuando haya que subirlo a la nube, se activa el perfil `postgres` y se
configuran las variables de entorno `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`
(y opcionalmente `CORS_ALLOWED_ORIGINS` con la URL del Angular desplegado):

```bash
java -jar target/pedidos-0.1.0.jar --spring.profiles.active=postgres
```

No se cambia nada del código: JPA/Hibernate genera las tablas igual en
PostgreSQL (`ddl-auto=update`).

## Modelo de datos (entidades JPA)

- **Cliente**: datos básicos del cliente (sin login todavía).
- **Producto**: catálogo, con `stock` para modelar el caso de "producto agotado".
- **Pedido**: cliente, tipo de entrega (local/delivery), estado, total, fechas.
- **DetallePedido**: líneas del pedido (producto, cantidad, precio congelado).
- **HistorialEstadoPedido**: registro de cada transición de estado (para seguimiento y auditoría).
- **Notificacion**: aviso generado en cada cambio de estado (canal `PANTALLA` por ahora).
- **Usuario**: cuenta de acceso (email, contraseña con hash BCrypt, rol, verificación de email, activo/inactivo). Implementa `UserDetails` de Spring Security directamente.

## Máquina de estados del pedido

```
RECIBIDO ──► PAGADO ──► EN_PREPARACION ──► LISTO ──┬──► EN_CAMINO ──► ENTREGADO
   │             │              │                  └──► ENTREGADO (retiro en local)
   └── CANCELADO ┴── CANCELADO ─┴── CANCELADO ──────────── CANCELADO
```

Reglas:
- Solo se permiten los saltos definidos arriba (p. ej. no se puede pasar de `RECIBIDO` a `EN_CAMINO` directamente). Un intento inválido responde `409 Conflict`.
- `ENTREGADO` y `CANCELADO` son estados finales (no admiten más cambios).
- **Cancelación a medio preparar**: se puede cancelar desde `RECIBIDO`, `PAGADO`, `EN_PREPARACION` o `LISTO`/`EN_CAMINO`; al cancelar se repone automáticamente el stock reservado de los productos del pedido.
- **Producto agotado**: si el stock no alcanza, `POST /api/pedidos` responde `409 Conflict` y no se crea el pedido.
- **Pago abandonado**: mientras el pedido esté en `RECIBIDO` (pendiente de pago), se puede pasar directamente a `CANCELADO` (simula que el cliente abandonó el pago a mitad de camino; a futuro esto lo dispararía un timeout o un webhook de Mercado Pago).

## Separación pedidos / notificaciones

`PedidoService` solo publica un evento de dominio (`CambioEstadoPedidoEvent`)
cuando cambia el estado. Dos listeners independientes reaccionan a ese
evento sin que `PedidoService` los conozca:

- `HistorialListener` → guarda la transición en `historial_estado_pedido`.
- `NotificacionListener` → arma el mensaje ("tu pedido está en camino...") y lo guarda en `notificaciones` (además de loguearlo).

Esto deja el camino listo para que, más adelante, un servicio de WhatsApp o
de boleta/correo se agregue como **otro listener más** del mismo evento, sin
tocar la lógica de pedidos — tal como pide la casuística.

## Endpoints

Desde el tercer avance, casi todos los endpoints requieren estar logueado
(header `Authorization: Bearer <token>`); la columna **Rol** indica quién
puede llamarlos. Ver [Usuarios y roles](#usuarios-y-roles-jwt) para obtener el token.

### Pedidos
| Método | Ruta | Rol | Descripción |
| --- | --- | --- | --- |
| `POST` | `/api/pedidos` | CLIENTE, CAJERA, ADMINISTRADOR | Crea un pedido (queda en `RECIBIDO`) |
| `GET` | `/api/pedidos` | cualquiera logueado | Lista pedidos (filtro opcional `?estado=`) |
| `GET` | `/api/pedidos/{id}` | cualquiera logueado | Detalle de un pedido |
| `PATCH` | `/api/pedidos/{id}/estado` | COCINERO, REPARTIDOR, CAJERA, ADMINISTRADOR | Cambia el estado (`{ "nuevoEstado": "...", "observacion": "..." }`) |
| `GET` | `/api/pedidos/{id}/historial` | cualquiera logueado | Historial de cambios de estado del pedido |

### Notificaciones (avisos en pantalla)
| Método | Ruta | Rol | Descripción |
| --- | --- | --- | --- |
| `GET` | `/api/notificaciones` | cualquiera logueado | Feed global de avisos (más reciente primero) |
| `GET` | `/api/notificaciones/pedido/{pedidoId}` | cualquiera logueado | Avisos de un pedido puntual |
| `PATCH` | `/api/notificaciones/{id}/leida` | cualquiera logueado | Marca un aviso como leído |

### Productos y clientes (soporte)
| Método | Ruta | Rol | Descripción |
| --- | --- | --- | --- |
| `GET` | `/api/productos` | público | Listar productos (catálogo) |
| `POST/PUT` | `/api/productos` | CAJERA, ADMINISTRADOR | Crear / actualizar producto (incl. stock) |
| `GET/POST` | `/api/clientes` | CAJERA, ADMINISTRADOR | Listar / crear clientes (alta manual, p. ej. pedido telefónico) |

## Usuarios y roles (JWT)

Autenticación sin sesión de servidor: al loguearte recibes un JWT y lo mandas
en cada request siguiente. Cubre RF-A01 (roles), RF-C01 (login) y RF-C02
(verificación de email).

### Los 5 roles del sistema

`CLIENTE`, `COCINERO`, `REPARTIDOR`, `CAJERA`, `ADMINISTRADOR`. Un cliente se
registra solo; el resto de roles los crea un administrador (nadie se puede
auto-asignar "soy cocinero").

### Cuentas de prueba ya cargadas (ver `data.sql`)

| Rol | Email | Contraseña |
| --- | --- | --- |
| ADMINISTRADOR | admin@polleria.com | Admin123! |
| COCINERO | cocina@polleria.com | Cocina123! |
| REPARTIDOR | reparto@polleria.com | Reparto123! |
| CAJERA | caja@polleria.com | Caja123! |
| CLIENTE | cliente@polleria.com | Cliente123! |

### Endpoints

| Método | Ruta | Rol | Descripción |
| --- | --- | --- | --- |
| `POST` | `/api/auth/registro` | público | Registra un CLIENTE nuevo (email/telefono/password) |
| `POST` | `/api/auth/verificar-email?token=...` | público | Confirma el correo con el token del registro |
| `POST` | `/api/auth/login` | público | Devuelve el JWT (`{ "email": "...", "password": "..." }`) |
| `GET` | `/api/auth/me` | cualquiera logueado | Datos de la cuenta del token actual |
| `POST` | `/api/usuarios` | ADMINISTRADOR | Da de alta personal (cocinero/repartidor/cajera/admin) |
| `GET` | `/api/usuarios` | ADMINISTRADOR | Lista todas las cuentas |
| `PATCH` | `/api/usuarios/{id}/estado?activo=false` | ADMINISTRADOR | Activa/desactiva una cuenta |

### Probar en Swagger

1. `POST /api/auth/login` con una de las cuentas de la tabla de arriba →
   copia el campo `token` de la respuesta.
2. Arriba a la derecha de Swagger UI hay un botón **Authorize** (candado) →
   pega el token ahí (sin escribir "Bearer ", Swagger lo agrega solo) → **Authorize** → **Close**.
3. Ya podés probar cualquier endpoint protegido: Swagger manda el header
   `Authorization` automáticamente en cada "Try it out".

### Probar por curl

```bash
# 1. Login (usa una cuenta de la tabla de arriba)
curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"cliente@polleria.com","password":"Cliente123!"}'
# copia el "token" de la respuesta

# 2. Usarlo en cualquier endpoint protegido
curl -s http://localhost:8080/api/auth/me \
  -H "Authorization: Bearer PEGA_AQUI_EL_TOKEN"

# 3. Registrar un cliente nuevo desde cero
curl -s -X POST http://localhost:8080/api/auth/registro \
  -H "Content-Type: application/json" \
  -d '{"nombreCompleto":"María Torres","email":"maria@example.com","password":"Maria123!","telefono":"988777666","direccion":"Calle Falsa 123"}'
# el token de verificación sale en la consola del backend (log "[EMAIL SIMULADO]"),
# porque todavía no existe un servicio real de correo en el proyecto

# 4. Verificar el email con ese token
curl -s -X POST "http://localhost:8080/api/auth/verificar-email?token=PEGA_AQUI_EL_TOKEN_DEL_LOG"
```

> **Nota:** el envío real de correos (verificación, boleta) queda pendiente
> para cuando se construya el servicio de comprobantes de la casuística; por
> ahora el token de verificación se loguea en la consola en vez de mandarse
> por email, para poder probar el flujo completo sin depender de eso.

## Pago con Mercado Pago (Checkout Pro)

Prueba de concepto de cobro real usando el [SDK oficial de Mercado Pago para Java](https://github.com/mercadopago/sdk-java) y **Checkout Pro** (checkout redirigido: el cliente sale de nuestra web, paga en la página de Mercado Pago, y vuelve).

### 1. Credenciales (variable de entorno, nunca en el código)

```bash
# Windows PowerShell
$env:MERCADOPAGO_ACCESS_TOKEN = "TEST-tu-access-token-de-prueba"

# Linux / Mac
export MERCADOPAGO_ACCESS_TOKEN="TEST-tu-access-token-de-prueba"
```

Cómo obtener el Access Token de prueba: ver la guía paso a paso que se entregó junto con este avance (crear cuenta en Mercado Pago Developers, aplicación de prueba, credenciales de test).

### 2. Endpoints nuevos

| Método | Ruta | Descripción |
| --- | --- | --- |
| `POST` | `/api/pedidos/{id}/pago` | Genera la preferencia de pago; devuelve `initPoint`/`sandboxInitPoint` (el link al que se redirige al cliente para pagar) |
| `POST` | `/api/pedidos/{id}/pago/confirmar?paymentId=...` | Confirma manualmente un pago ya realizado (útil en local, sin webhook real) y pasa el pedido a `PAGADO` |
| `POST` | `/api/pagos/webhook` | URL que Mercado Pago llama sola cuando cambia el estado de un pago |

El pedido solo puede iniciar pago si está en `RECIBIDO` (409 si no).

> **Nota (bug real encontrado en pruebas):** al principio la preferencia se armaba con `auto_return` ("volver solo al sitio" tras pagar), pero Mercado Pago la rechaza con `invalid_auto_return` mientras `back_urls.success` apunte a `localhost` — exige un dominio público. Como el frontend Angular todavía no está desplegado, se quitó `auto_return` por ahora: el cliente vuelve manualmente con el botón "Volver al sitio" que muestra Mercado Pago al terminar de pagar. Se puede reactivar el día que exista una URL pública real.

### 3. Flujo de prueba (sandbox, sin desplegar nada)

Desde el tercer avance estos endpoints requieren token (ver
[Usuarios y roles](#usuarios-y-roles-jwt)); logueate primero con
`cliente@polleria.com` / `Cliente123!` y usa ese token en `$TOKEN`.

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"cliente@polleria.com","password":"Cliente123!"}' | python3 -c "import sys,json;print(json.load(sys.stdin)['token'])")

# 1. Crear un pedido (igual que antes, ahora con el token)
curl -s -X POST http://localhost:8080/api/pedidos \
  -H "Content-Type: application/json" -H "Authorization: Bearer $TOKEN" \
  -d '{"clienteId":1,"tipoEntrega":"DELIVERY","direccionEntrega":"Av. Siempre Viva 742","items":[{"productoId":2,"cantidad":2}]}'

# 2. Generar la preferencia de pago del pedido 1
curl -s -X POST http://localhost:8080/api/pedidos/1/pago -H "Authorization: Bearer $TOKEN"

# 3. Abrir en el navegador el "sandboxInitPoint" que devolvió el paso 2, y
#    pagar con una tarjeta de prueba (ver "Tarjetas de prueba" en tu panel de
#    Mercado Pago Developers). Mercado Pago te muestra el id del pago (payment id).

# 4a. Confirmar manualmente ese pago (reemplaza 123456789 por el payment id real)
curl -s -X POST "http://localhost:8080/api/pedidos/1/pago/confirmar?paymentId=123456789" -H "Authorization: Bearer $TOKEN"

# 4b. (alternativa) Si expones tu servidor con ngrok y configuras esa URL como
#     notification_url, Mercado Pago llama solo a /api/pagos/webhook y el
#     pedido pasa a PAGADO sin el paso 4a (el webhook queda público, sin token).

# 5. El pedido ya está en PAGADO: se generó su notificación e historial como siempre
curl -s http://localhost:8080/api/pedidos/1 -H "Authorization: Bearer $TOKEN"
curl -s http://localhost:8080/api/notificaciones/pedido/1 -H "Authorization: Bearer $TOKEN"
```

### 4. Por qué no hizo falta tocar la lógica de pedidos

`PagoService` no reimplementa nada del control de estados: valida el pago
contra la API de Mercado Pago y, si está aprobado, llama al mismo
`PedidoService.cambiarEstado(id, PAGADO, "...")` que ya se usaba para el
cambio manual. Por eso el historial y las notificaciones en pantalla
"simplemente funcionan" también para el pago real, sin duplicar código.

## Ejemplo de flujo (curl)

Dos tokens: `TOKEN_CLIENTE` crea el pedido, `TOKEN_COCINA` (o cualquier
cuenta de cocina/reparto/caja/admin) mueve los estados — un cliente no puede
cambiar el estado de su propio pedido, eso lo hace el personal (RF-K02/RF-D03).

```bash
TOKEN_CLIENTE=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" -d '{"email":"cliente@polleria.com","password":"Cliente123!"}' \
  | python3 -c "import sys,json;print(json.load(sys.stdin)['token'])")
TOKEN_COCINA=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" -d '{"email":"cocina@polleria.com","password":"Cocina123!"}' \
  | python3 -c "import sys,json;print(json.load(sys.stdin)['token'])")

# 1. Crear un pedido (cliente 1, "1/4 de pollo" x2 = producto 2)
curl -s -X POST http://localhost:8080/api/pedidos \
  -H "Content-Type: application/json" -H "Authorization: Bearer $TOKEN_CLIENTE" \
  -d '{"clienteId":1,"tipoEntrega":"DELIVERY","direccionEntrega":"Av. Siempre Viva 742","items":[{"productoId":2,"cantidad":2}]}'

# 2. Confirmar el pago (simula webhook de Mercado Pago)
curl -s -X PATCH http://localhost:8080/api/pedidos/1/estado \
  -H "Content-Type: application/json" -H "Authorization: Bearer $TOKEN_COCINA" \
  -d '{"nuevoEstado":"PAGADO"}'

# 3. Pasar a preparación, listo, en camino, entregado...
curl -s -X PATCH http://localhost:8080/api/pedidos/1/estado -H "Content-Type: application/json" -H "Authorization: Bearer $TOKEN_COCINA" -d '{"nuevoEstado":"EN_PREPARACION"}'
curl -s -X PATCH http://localhost:8080/api/pedidos/1/estado -H "Content-Type: application/json" -H "Authorization: Bearer $TOKEN_COCINA" -d '{"nuevoEstado":"LISTO"}'
curl -s -X PATCH http://localhost:8080/api/pedidos/1/estado -H "Content-Type: application/json" -H "Authorization: Bearer $TOKEN_COCINA" -d '{"nuevoEstado":"EN_CAMINO"}'
curl -s -X PATCH http://localhost:8080/api/pedidos/1/estado -H "Content-Type: application/json" -H "Authorization: Bearer $TOKEN_COCINA" -d '{"nuevoEstado":"ENTREGADO"}'

# 4. Ver los avisos generados
curl -s http://localhost:8080/api/notificaciones/pedido/1 -H "Authorization: Bearer $TOKEN_CLIENTE"

# 5. Ver el historial de estados
curl -s http://localhost:8080/api/pedidos/1/historial -H "Authorization: Bearer $TOKEN_CLIENTE"
```

## Próximos pasos (fuera de este avance)

Roadmap completo en marcha para cubrir el resto de la casuística (~26 RF):

- **Avance 4** — Módulo Cliente: catálogo con ofertas/recomendación del día, favoritos, carrito, seguimiento de pedido y detalle de transacción ligados al usuario logueado, historial de pedidos por cliente, canal de WhatsApp, atención al cliente (RF-C03–C13).
- **Avance 5** — Cocina y Reparto: recepción de pedidos en tiempo real, marcar "Listo", detalle de entrega, geolocalización GPS, confirmación de entrega (RF-K01–K02, RF-D01–D03).
- **Avance 6** — Administración avanzada: inventario CRUD, supervisión de flujo de pedidos, resumen operativo, notificaciones directas, punto de venta (POS), dashboard de ventas, reportes Excel, administración del chatbot de WhatsApp (RF-A03–A10), y 2FA/MFA (RF-A02).
- Exponer el webhook de Mercado Pago públicamente (desplegar en la nube, o ngrok en local) para probar la confirmación automática de pago de punta a punta.
- Servicio de comprobantes: generar boleta en PDF y enviarla por correo al llegar a `PAGADO` (y de paso reemplazar el "email simulado" de verificación de cuenta por un envío real).
- Frontend Angular que consuma estos endpoints (login, listado de pedidos, cambio de estado desde cocina/reparto, feed de notificaciones en pantalla).
