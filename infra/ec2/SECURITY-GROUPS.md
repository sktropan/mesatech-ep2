# Security groups — MesaTech EV1 (1 EC2)

Documento para informe y configuración en consola AWS.

## Reglas inbound objetivo

| Puerto | Protocolo | Origen | Servicio | Notas |
| --- | --- | --- | --- | --- |
| 22 | TCP | IP del equipo / VPN | SSH | Admin; no `0.0.0.0/0` en entrega |
| 8080 | TCP | Solo tráfico Gateway* | BFF | *En EV1 simple: restringir lo máximo posible; idealmente IP de integración Gateway o prueba temporal acotada |
| 8081 | — | **No** Internet | ms-solicitudes | Solo localhost / SG interno |
| 8082 | — | **No** Internet | ms-catalogo | Solo localhost / SG interno |
| 3306 | — | **No** Internet | MySQL 8.4 | Solo 127.0.0.1 o SG de los MS (`docker-compose.ec2.yml`) |
| 5672 | — | **No** Internet | RabbitMQ AMQP | Igual. Los JAR se conectan por localhost al vhost `mesatech` |
| 15672 | — | **No** Internet | Consola RabbitMQ | Solo loopback |

## Regla EP1

- Acceso directo a MS desde Internet: **bloqueado** (evidencia NIC-08).
- BFF sin JWT: **401** (evidencia NIC-07).
- React en producción: **API Gateway**, no `:8081`/`:8082`.

## Outbound

Por defecto: permitir salida (Entra JWKS, apt, docker pull).

## Diagrama

```text
Internet → API Gateway → (8080) BFF → 127.0.0.1:8081 / :8082 → MS → 127.0.0.1:3306 MySQL
Internet -X→ 8081, 8082, 3306, 5672, 15672
```
