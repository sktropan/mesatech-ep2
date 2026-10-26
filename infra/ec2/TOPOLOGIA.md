# Topología EC2 — decisión del equipo (EV1)

**EP2:** esta rama usa **MySQL 8.4** (puerto **3306**, bases `catalogo` y `solicitudes`) y deja **RabbitMQ 4.0** en el mismo Compose. El código Java todavía no se conecta a RabbitMQ. La tabla de abajo es el registro de la entrega EV1, que usó PostgreSQL 16 en el puerto 5432.

**Opción elegida:** **tres instancias EC2** en **us-east-1** (Learner Lab), una por capa:

| Capa | Name tag | Puertos | Persistencia / procesos |
| --- | --- | --- | --- |
| Base de datos | `mesatech-ev1-db` | 5432 (solo SG del MS) | Docker **PostgreSQL 16** |
| Microservicios | `mesatech-ev1-ms` | 8081, 8082 (solo SG del BFF) | `ms-solicitudes.jar`, `ms-catalogo.jar` |
| BFF | `mesatech-ev1-bff` | 8080 (integración API Gateway + pruebas) | `bff.jar` (Resource Server, sin JPA) |

**Justificación EV1:** separación clara para la rúbrica (MS y DB no expuestos a Internet, BFF como único backend HTTP hacia el Gateway), SG demostrables (NIC-08, NIC-09) y trazabilidad por capa en informe §12.

**Alternativa descartada para esta entrega:** una sola EC2 con 8080+8081+8082+Docker (válida para EV1 pero el equipo optó por 3 instancias para evidencia de red).

IPs y diagrama visual: [`docs/arquitectura/diagrama.md`](../../docs/arquitectura/diagrama.md) y PNG `docs/assets/diagramas/arquitectura_mesatech_ev1_despliegue.png`.

Reglas de firewall: [`SECURITY-GROUPS.md`](SECURITY-GROUPS.md).
