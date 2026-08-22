# Servicio de Pedidos — Sistema Pollería (Caso 2)

Primer avance del backend en Spring Boot para el sistema integrado (Spring Boot + Angular)
de la pollería. Cubre los 4 puntos pedidos:

1. **Guardar los pedidos (entidades) en BD usando JPA.**
2. **Controlar los estados** del pedido con una máquina de estados validada.
3. **Avisar cada cambio de estado**, aunque sea por pantalla (historial + notificaciones consultables por endpoint, listas para que Angular las muestre).
4. **Endpoints** REST documentados con Swagger.

## Cómo ejecutar

Requisitos: Java 17+ y Maven (o usar el wrapper si lo agregas).

```bash
cd polleria-backend
mvn spring-boot:run
```

La app levanta en `http://localhost:8080`, con una base de datos H2 persistida
en archivo (`./data/polleria.mv.db`), así que los pedidos no se pierden al
reiniciar. Se cargan automáticamente algunos productos y un cliente de
ejemplo (ver `src/main/resources/data.sql`).

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

### Pedidos
| Método | Ruta | Descripción |
| --- | --- | --- |
| `POST` | `/api/pedidos` | Crea un pedido (queda en `RECIBIDO`) |
| `GET` | `/api/pedidos` | Lista pedidos (filtro opcional `?estado=`) |
| `GET` | `/api/pedidos/{id}` | Detalle de un pedido |
| `PATCH` | `/api/pedidos/{id}/estado` | Cambia el estado (`{ "nuevoEstado": "...", "observacion": "..." }`) |
| `GET` | `/api/pedidos/{id}/historial` | Historial de cambios de estado del pedido |

### Notificaciones (avisos en pantalla)
| Método | Ruta | Descripción |
| --- | --- | --- |
| `GET` | `/api/notificaciones` | Feed global de avisos (más reciente primero) |
| `GET` | `/api/notificaciones/pedido/{pedidoId}` | Avisos de un pedido puntual |
| `PATCH` | `/api/notificaciones/{id}/leida` | Marca un aviso como leído |

### Productos y clientes (soporte)
| Método | Ruta | Descripción |
| --- | --- | --- |
| `GET/POST` | `/api/productos` | Listar / crear productos |
| `PUT` | `/api/productos/{id}` | Actualizar producto (incl. stock) |
| `GET/POST` | `/api/clientes` | Listar / crear clientes |

## Ejemplo de flujo (curl)

```bash
# 1. Crear un pedido (cliente 1, "1/4 de pollo" x2 = producto 2)
curl -s -X POST http://localhost:8080/api/pedidos \
  -H "Content-Type: application/json" \
  -d '{"clienteId":1,"tipoEntrega":"DELIVERY","direccionEntrega":"Av. Siempre Viva 742","items":[{"productoId":2,"cantidad":2}]}'

# 2. Confirmar el pago (simula webhook de Mercado Pago)
curl -s -X PATCH http://localhost:8080/api/pedidos/1/estado \
  -H "Content-Type: application/json" \
  -d '{"nuevoEstado":"PAGADO"}'

# 3. Pasar a preparación, listo, en camino, entregado...
curl -s -X PATCH http://localhost:8080/api/pedidos/1/estado -H "Content-Type: application/json" -d '{"nuevoEstado":"EN_PREPARACION"}'
curl -s -X PATCH http://localhost:8080/api/pedidos/1/estado -H "Content-Type: application/json" -d '{"nuevoEstado":"LISTO"}'
curl -s -X PATCH http://localhost:8080/api/pedidos/1/estado -H "Content-Type: application/json" -d '{"nuevoEstado":"EN_CAMINO"}'
curl -s -X PATCH http://localhost:8080/api/pedidos/1/estado -H "Content-Type: application/json" -d '{"nuevoEstado":"ENTREGADO"}'

# 4. Ver los avisos generados
curl -s http://localhost:8080/api/notificaciones/pedido/1

# 5. Ver el historial de estados
curl -s http://localhost:8080/api/pedidos/1/historial
```

## Próximos pasos (fuera de este avance)

- Autenticación por rol (RF-A01) para Cliente/Cocinero/Repartidor/Cajera/Administrador.
- Integración real con Mercado Pago (webhook que dispare `PATCH /estado` a `PAGADO`).
- Canal de notificación por WhatsApp como nuevo listener del mismo evento.
- Servicio de comprobantes: generar boleta en PDF y enviarla por correo al llegar a `PAGADO`.
- Frontend Angular que consuma estos endpoints (listado de pedidos, cambio de estado desde cocina/reparto, feed de notificaciones en pantalla).
