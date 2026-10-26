# Infraestructura MesaTech

MySQL 8.4 y RabbitMQ 4.0 en Docker para **local** y **EC2**. Los microservicios se conectan a MySQL en `localhost:3306`. RabbitMQ escucha en `5672` (AMQP) y `15672` (consola), vhost `mesatech`. `ms-catalogo` publica con el usuario `catalogo`. `ms-solicitudes` consume con el usuario `solicitudes`. Cómo probarlo: [`docs/pruebas-rabbitmq.md`](../docs/pruebas-rabbitmq.md).

| Archivo | Uso |
| --- | --- |
| [`docker-compose.yml`](docker-compose.yml) | Desarrollo local (3306, 5672 y 15672 publicados en la máquina) |
| [`docker-compose.ec2.yml`](docker-compose.ec2.yml) | EC2: MySQL y RabbitMQ **solo** en loopback (no expuestos a Internet) |
| [`.env.example`](.env.example) | Credenciales de ejemplo. Copiar a `.env` (ese archivo no se versiona) |
| [`init-databases.sh`](init-databases.sh) | Crea las bases `catalogo` y `solicitudes` y da permiso al usuario de la app |
| [`rabbitmq/`](rabbitmq/) | Vhost `mesatech`, usuarios con permisos mínimos, exchange, cola y binding |
| [`ec2/README.md`](ec2/README.md) | Guía de despliegue Nico (C + D) |
| [`scripts/build-jars.sh`](scripts/build-jars.sh) | Compilar BFF + MS en Linux/macOS/Git Bash |
| [`scripts/build-jars.ps1`](scripts/build-jars.ps1) | Compilar en Windows |

Arranque local:

```bash
cd infra
cp .env.example .env
docker compose up -d
```

`DB_USER` y `DB_PASSWORD` de la raíz (`env.example`) deben coincidir con `MYSQL_USER` y `MYSQL_PASSWORD`. Cada microservicio usa su propia `DB_URL` (o el default de su `application.properties`): base `catalogo` en el puerto 8082 y base `solicitudes` en el 8081.

RabbitMQ no usa un usuario administrador de aplicación. El entrypoint escribe `definitions.json` con las claves de `infra/.env` y el servidor las importa al arrancar. Consola: `http://localhost:15672`, usuario `mesatech-ui`. El usuario `guest` queda creado y sin permisos en el vhost `mesatech`.

El exchange lo declara también `ms-catalogo` (es quien publica). La cola y el binding los declara `ms-solicitudes` (es quien consume). El JSON del broker repite los tres para que el mensaje tenga destino aunque un JAR arranque más tarde. Declarar dos veces, con los mismos argumentos, es válido.

Guía extendida: [`docs/equipo/guias/nico-ec2-seguridad-presentacion.md`](../docs/equipo/guias/nico-ec2-seguridad-presentacion.md).
