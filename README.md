# MesaTech Cloud — EV1 (DSY1107)

Monorepo MesaTech: React + **Microsoft Entra ID** + **AWS API Gateway** + BFF y microservicios Spring Boot (**Java 21**) en **EC2**, con **MySQL 8.4** y **RabbitMQ 4.0** en Docker. Al crear una categoría, `ms-catalogo` publica `categoria.creada` y `ms-solicitudes` guarda una copia.

Equipo: **Ari**, **Ninna**, **Nico**, **Skarlet**.

```text
React + MSAL  →  API Gateway  →  BFF :8080  →  ms-solicitudes :8081  →  MySQL (solicitudes)
                                         └→  ms-catalogo    :8082  →  MySQL (catalogo)
```

---

## Documentación (empezar aquí)

Toda la guía del proyecto está en **[`docs/README.md`](docs/README.md)**:

| Documento | Uso |
| --- | --- |
| [Onboarding y `_refs`](docs/onboarding/refs-y-entorno.md) | Clone, secretos, setup local |
| [Instructivo maestro EV1](docs/guia/instructivo-maestro-ev1.md) | Fases A→G en orden |
| [Plan del equipo](docs/equipo/plan-trabajo.md) | Ari / Ninna / Nico / Skarlet |
| [Guías paso a paso](docs/equipo/guias/README.md) | Instructivo detallado por integrante |
| [Guía oficial EP1](docs/caso/ep1-guia-oficial.md) | Rúbrica, pruebas §10–§15 |
| [Glosario](docs/glosario/README.md) · [Dominio](docs/dominio/README.md) | Conceptos cloud y negocio |
| [Arquitectura](docs/arquitectura/diagrama.md) | Flujos y diagrama |
| [Pruebas RabbitMQ](docs/pruebas-rabbitmq.md) | Crear categoría, ver el mensaje, ver la copia y el 503 |
| **[Reglas del repositorio](docs/equipo/reglas-repositorio.md)** | **Git, commits, PR y reglas EV1 (todo el equipo)** |
| [Convenciones de desarrollo](docs/equipo/convenciones-desarrollo.md) | Detalle por carpeta (`bff/`, `frontend/`, …) |
| [Estrategia Git / branching](docs/equipo/estrategia-branching.md) | Ramas, PRs y tag `ev1.0.0` |

---

## Estructura del código

| Carpeta | Puerto | Rol |
| --- | --- | --- |
| `frontend/` | 3000 | React, MSAL, Axios → `REACT_APP_API_BASE_URL` |
| `bff/` | 8080 | OAuth2 Resource Server, **sin JPA**, proxy a MS |
| `ms-solicitudes/` | 8081 | Solicitudes y reglas de estado |
| `ms-catalogo/` | 8082 | Categorías y prioridades |
| `infra/` | 3306, 5672, 15672 | `docker compose` — MySQL 8.4 y RabbitMQ 4.0 (vhost `mesatech`); EC2 en [`infra/ec2/README.md`](infra/ec2/README.md) |

Origen conceptual: repos de ejemplo del profesor (ver onboarding); código entregable solo en las carpetas anteriores.

---

## Levantar en local (resumen)

1. Entra ID del **equipo** — [`docs/guia/entra-spring-react.md`](docs/guia/entra-spring-react.md).
2. Copiar `env.example` y `frontend/.env.example` (IDs por canal privado, no los del profesor).
3. `cd infra && cp .env.example .env && docker compose up -d`
4. Exportar `ENTRA_ISSUER_URI` y `ENTRA_AUDIENCE`; en tres terminales: `mvn spring-boot:run` en `bff`, `ms-solicitudes`, `ms-catalogo`.
5. `cd frontend && npm install && npm start`

En **AWS**, `REACT_APP_API_BASE_URL` debe ser la URL del **HTTP API (Gateway)**. Acceso directo a `:8080`, `:8081` o `:8082` desde internet debe responder **401** (o puerto no expuesto).

Detalle paso a paso: [`docs/onboarding/refs-y-entorno.md`](docs/onboarding/refs-y-entorno.md) §6.
