# Documentación MesaTech Cloud — EV1

Índice central del monorepo. El código vive en `frontend/`, `bff/`, `ms-solicitudes/`, `ms-catalogo/` e `infra/`. **Empieza por** [`onboarding/refs-y-entorno.md`](onboarding/refs-y-entorno.md) si acabas de clonar.

Equipo: **Ari**, **Ninna**, **Nico**, **Skarlet**.

En la **raíz del repo** solo queda `README.md` como entrada; el resto de documentación vive aquí.

---

## Ubicación canónica (archivos que antes estaban en la raíz)

| Antes | Ahora |
| --- | --- |
| `PLAN_EQUIPO_EV1.md` | [`equipo/plan-trabajo.md`](equipo/plan-trabajo.md) |
| `GUIA_REFS_Y_ENTORNO.md` | [`onboarding/refs-y-entorno.md`](onboarding/refs-y-entorno.md) |
| `EP1_Caso_Negocio_MesaTech_Cloud.md` | [`caso/ep1-guia-oficial.md`](caso/ep1-guia-oficial.md) (+ PDF en `caso/`) |
| `DIAGRAMA_ARQUITECTURA.md` | [`arquitectura/diagrama.md`](arquitectura/diagrama.md) |
| `Configuracion_Cloud_Backend.md` | [`guia/entra-spring-react.md`](guia/entra-spring-react.md) |
| `ANOTACIONES_PROFESOR.md` | [`contexto/anotaciones-profesor.md`](contexto/anotaciones-profesor.md) |

---

## Mapa de lectura

| Orden | Documento | Para quién |
| --- | --- | --- |
| 1 | [Onboarding: refs y entorno](onboarding/refs-y-entorno.md) | Todos (clone, secretos, local) |
| 2 | [Caso EP1 — guía oficial](caso/ep1-guia-oficial.md) | Todos (rúbrica, pruebas §10–§15) |
| 3 | [Instructivo maestro EV1](guia/instructivo-maestro-ev1.md) | Todos (fases A→G en orden) |
| 4 | [Plan de trabajo del equipo](equipo/plan-trabajo.md) | Reparto y dependencias |
| 4a | [Guías paso a paso por persona](equipo/guias/README.md) | Ari, Ninna, Nico, Skarlet |
| 4b | [Estado EV1 — Nico (EC2/Git)](equipo/estado/nico-ev1.md) | Nico, coordinación informe |
| 4c | [Estado EV1 — Skarlet (UI/pruebas)](equipo/estado/skarlet-ev1.md) | Skarlet, informe §11–§15 |
| 4d | [Estado EV1 — Ninna / Gateway (contingencia Nico)](equipo/estado/ninna-ev1.md) | Gateway, CORS, v1/v2, marca |
| 5 | **[Reglas del repositorio](equipo/reglas-repositorio.md)** | **Git, commits `[ TIPO ]`, PR, producto — todo el equipo** |
| 5b | [Convenciones de desarrollo](equipo/convenciones-desarrollo.md) | Detalle código por carpeta |
| 5c | [Estrategia de branching](equipo/estrategia-branching.md) | Ramas, PRs, entrega `ev1.0.0` |
| 6 | [Arquitectura](arquitectura/diagrama.md) | Nico, Ninna, informe |
| 7 | [Entra + Spring + React (19 pasos)](guia/entra-spring-react.md) | Ari, quien toque JWT |
| 8 | [Anotaciones del profesor](contexto/anotaciones-profesor.md) | Decisiones vs guía escrita |
| 9 | [Glosario cloud/JWT](glosario/README.md) | Informe y presentación |
| 9b | [Pruebas RabbitMQ](pruebas-rabbitmq.md) | Crear categoría, ver mensaje, copia y error 503 |
| 10 | [Diccionario de dominio](dominio/README.md) | Skarlet, front, APIs |

---

## Estructura de carpetas

```text
docs/
├── README.md                 ← este índice
├── caso/                     Guía oficial EP1 (+ PDF)
├── contexto/                 Anotaciones del profesor
├── equipo/                   Plan + convenciones
│   └── guias/                Paso a paso por integrante
├── onboarding/               _refs, .env, setup local
├── arquitectura/             Diagrama y flujos
├── guia/                     Instructivo maestro + Entra paso a paso
├── glosario/                 Conceptos técnicos (IDaaS, JWT, Gateway…)
├── dominio/                  Negocio MesaTech (estados, roles, rutas)
└── assets/
    ├── diagramas/            PNG de arquitectura
    ├── pptx/                 Capturas extraídas de la PPT (opcional)
    └── fuentes/              PPT original del profesor
```

---

## Enlaces rápidos al código

| Componente | Puerto local | Notas |
| --- | --- | --- |
| `frontend/` | 3000 | `REACT_APP_API_BASE_URL` → Gateway en AWS |
| `bff/` | 8080 | Resource Server, sin JPA |
| `ms-solicitudes/` | 8081 | MySQL 8.4, base `solicitudes` |
| `ms-catalogo/` | 8082 | MySQL 8.4, base `catalogo` |
| `infra/` | 3306, 5672, 15672 | `docker compose up -d` (MySQL + RabbitMQ, vhost `mesatech`) |

Plantillas de variables: `env.example`, `frontend/.env.example`.
