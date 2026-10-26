# Pruebas RabbitMQ — categoría creada

Guía corta para reproducir el flujo y defenderlo. El código vive en `ms-catalogo` (publica) y `ms-solicitudes` (guarda una copia). El BFF solo reenvía `GET /v1/categorias-catalogo`.

## Qué hace el sistema

1. Un administrador crea una categoría (`POST /v1/catalogo/categorias`).
2. `ms-catalogo` guarda la fila en MySQL y publica un JSON en RabbitMQ.
3. `ms-solicitudes` lee ese mensaje y guarda una copia en su tabla `categorias_catalogo`.
4. Cualquier usuario autenticado puede ver esa copia (`GET /v1/categorias-catalogo`), también desde el menú **Copia catálogo** del React.

El JSON es exactamente:

```json
{"id": 1, "nombre": "Hardware"}
```

Exchange: `catalogo.exchange` (direct, durable).  
Routing key: `categoria.creada`.  
Cola: `solicitudes.catalogo.queue` (durable).

## Dónde se declara la topología

Se declara en **dos lugares a propósito**. Declarar dos veces con los mismos argumentos no rompe nada.

| Lugar | Qué declara | Por qué |
| --- | --- | --- |
| `infra/rabbitmq/definitions.template.json` | vhost, usuarios, permisos, exchange, cola y binding | El broker ya nace cableado, antes de que arranque Java. También deja a `guest` fuera del vhost. |
| `ms-catalogo` `RabbitConfig` | solo el exchange | El que publica es dueño del contrato (el nombre del exchange). |
| `ms-solicitudes` `RabbitConfig` | exchange, cola y binding | La cola es de quien consume. Si catálogo aún no arrancó, solicitudes igual puede crear el binding. |

## Usuarios y permisos mínimos

Vhost: `mesatech`. Contraseñas de ejemplo en `infra/.env.example`.

| Usuario | Para qué | configure | write | read |
| --- | --- | --- | --- | --- |
| `catalogo` | publicar | solo `catalogo.exchange` | solo `catalogo.exchange` | nada (`^$`) |
| `solicitudes` | consumir, declarar la cola y hacer el binding | exchange y cola | solo la cola | exchange y cola |
| `mesatech-ui` | consola `http://localhost:15672` | todo el vhost | todo el vhost | todo el vhost |
| `guest` | no se usa | sin fila en `mesatech` | sin fila | sin fila |

`guest` / `guest` existe a propósito, sin ninguna fila de permisos en `mesatech`. Además solo puede conectar desde dentro del contenedor (`loopback_users.guest`). Desde el host ni siquiera llega a autenticarse.

En RabbitMQ 4 el binding no se autoriza como un publish. El servidor, en `binding_action_with_checks`, pide **write sobre la cola** y **read sobre el exchange**. Publicar es otra cosa: **write sobre el exchange** (`basic.publish`). Por eso `solicitudes` puede enganchar la cola y no puede publicar. `catalogo` puede publicar y declarar el exchange, y no puede leer la cola: su lectura es `^$`, que no coincide con ningún nombre.

La consola usa `mesatech-ui` (tag `management`). Las aplicaciones no usan esa cuenta.

## Confirms, returns y el 503

`ms-catalogo` publica con **publisher confirms** y **mandatory** (returns). Espera el ack del broker.

- Si el broker confirma y hay cola enlazada: HTTP **200** y la categoría queda guardada.
- Si el broker no confirma, devuelve el mensaje (nadie lo recibió) o no está disponible: HTTP **503** con cuerpo JSON `error` + `message`. No es un 500.
- Esa publicación va **dentro de la transacción** de la base. Si hay 503, el INSERT se revierte: no queda una categoría sin evento.

Límite para decirlo en la defensa: si el broker alcanzó a aceptar el mensaje y recién después se corta el tiempo de espera, puede existir el mensaje y no la fila. Preferimos ese caso raro antes que guardar la categoría cuando RabbitMQ no respondió.

## Cómo reproducir en local

Hace falta Java 21, Maven, Docker y un token de Entra con el scope `access_as_user`. Crear categoría en el BFF además exige rol **Administrador**. Para ver el 503 también sirve llamar directo a `ms-catalogo` con el mismo Bearer.

### 1. Broker y bases

```bash
cd infra
cp .env.example .env
docker compose up -d
docker compose ps
```

MySQL crea `catalogo` y `solicitudes`. RabbitMQ importa el vhost `mesatech`. Consola: `http://localhost:15672` con usuario `mesatech-ui` y la clave `RABBITMQ_UI_PASSWORD` del `.env` (en el ejemplo, `mesatech-ui-dev`). Hay que elegir el vhost `mesatech` arriba a la derecha.

### 2. Tres terminales Java

En cada una, exporta `ENTRA_ISSUER_URI` y `ENTRA_AUDIENCE` (los da Ari). No hace falta exportar `RABBITMQ_USER` si usas las claves del ejemplo: cada `application.properties` trae su usuario.

```bash
# terminal A — consumidor
cd ms-solicitudes
mvn spring-boot:run

# terminal B — publicador
cd ms-catalogo
mvn spring-boot:run

# terminal C — BFF
cd bff
mvn spring-boot:run
```

En PowerShell también sirve `infra/scripts/run-local-stack.ps1` (pasa un usuario de Rabbit distinto a cada JAR).

### 3. Ver el mensaje en la cola

Para que el mensaje no desaparezca al instante, **detén ms-solicitudes** (Ctrl+C en la terminal A).

Crea una categoría (reemplaza el token):

```bash
curl -sS -D - -H "Authorization: Bearer <TOKEN>" -H "Content-Type: application/json" \
  -d '{"nombre":"Hardware"}' \
  http://localhost:8080/v1/catalogo/categorias
```

Respuesta esperada: **200** y un JSON con `id` y `nombre`.

En la consola: Queues → `solicitudes.catalogo.queue` → Get messages → Ack mode **Nack message requeue true**. El payload es el JSON de arriba. También, con la CLI que trae la imagen 4.0:

```bash
docker exec mesatech-rabbitmq rabbitmqadmin -u mesatech-ui -p mesatech-ui-dev -V mesatech \
  get messages --queue solicitudes.catalogo.queue --ack-mode reject_requeue_true --count 1
```

### 4. Ver la copia

Vuelve a arrancar `ms-solicitudes`. El mensaje sale de la cola y entra en MySQL.

```bash
curl -sS -H "Authorization: Bearer <TOKEN>" http://localhost:8080/v1/categorias-catalogo
```

Esperado: un arreglo con `catalogoId` (el id de catálogo) y `nombre`. En el React, menú **Copia catálogo**. Si acabas de crear la categoría, pulsa **Actualizar**: la copia llega por el broker, no en el mismo milisegundo del POST.

Comprobar la tabla:

```bash
docker exec mesatech-mysql mysql -umesatech -pmesatech -e "SELECT * FROM solicitudes.categorias_catalogo;"
```

Publicar otra vez el mismo id no duplica la fila: `catalogo_id` es único. Si llegan dos mensajes iguales, el segundo se confirma y se ignora.

### 5. Provocar el error (RabbitMQ abajo)

```bash
docker stop mesatech-rabbitmq
curl -sS -D - -H "Authorization: Bearer <TOKEN>" -H "Content-Type: application/json" \
  -d '{"nombre":"NoDebeQuedar"}' \
  http://localhost:8080/v1/catalogo/categorias
```

Esperado: **503** (no 500) y un JSON parecido a:

```json
{
  "error": "Servicio no disponible",
  "message": "RabbitMQ no está disponible. La categoría no se guardó. Intente de nuevo cuando el broker responda."
}
```

La categoría **no** queda en `catalogo.categorias`. El BFF reenvía el 503 tal cual. En el React, el aviso muestra `message`.

```bash
docker start mesatech-rabbitmq
```

### 6. Probar que guest no entra a mesatech

Desde el host, `guest` ni siquiera pasa de loopback. Desde dentro del contenedor (donde loopback sí lo dejaría conectar) el vhost igual lo rechaza:

```bash
docker exec mesatech-rabbitmq rabbitmqctl list_permissions -p mesatech
```

`list_permissions` no muestra a `guest`. Una conexión AMQP `guest` / `guest` al vhost `mesatech` la cierra el broker con `access to vhost 'mesatech' refused for user 'guest'`. El usuario `catalogo` puede entrar al vhost, pero si intenta leer la cola el broker responde `read access to queue 'solicitudes.catalogo.queue' refused`. `solicitudes` no tiene write sobre el exchange, así que no puede publicar.

## Pruebas automáticas

```bash
mvn -pl ms-catalogo,ms-solicitudes test
```

Levantan MySQL 8.4 y RabbitMQ reales (Testcontainers), con los mismos permisos:

- crear categoría deja el JSON en la cola y responde 200;
- sin binding, o con el broker detenido, responde 503 y no guarda la fila;
- `guest` no está en los permisos del vhost y no abre conexión;
- el usuario `catalogo` no puede consumir;
- dos mensajes con el mismo id dejan una sola fila en `categorias_catalogo`;
- `GET /v1/categorias-catalogo` sin token responde 401.

## Ruta nueva en API Gateway

Hay que crear en el HTTP API la misma ruta que el BFF, con el JWT Authorizer de siempre:

| Método | Path | Integración |
| --- | --- | --- |
| GET | `/v1/categorias-catalogo` | HTTP hacia el BFF `:8080`, mismo path |

Sin esa ruta, el React en la nube recibe 404 aunque el BFF esté bien. Detalle en [`equipo/rutas-api-gateway-bff.md`](equipo/rutas-api-gateway-bff.md). El rol no se filtra en Gateway: cualquier token con scope `access_as_user` puede leer la copia. Crear la categoría sigue siendo solo Administrador.
