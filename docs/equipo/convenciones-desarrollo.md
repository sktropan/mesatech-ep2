# Convenciones de desarrollo — MesaTech EV1

Acuerdos del equipo para código y documentación del monorepo.

> **Reglas oficiales Git + producto (leer primero):** [`reglas-repositorio.md`](reglas-repositorio.md) — válido para todo el equipo.

## Lectura antes de tocar el repo

1. [`../README.md`](../README.md) — índice de documentación
2. [`../contexto/anotaciones-profesor.md`](../contexto/anotaciones-profesor.md) — Entra, Gateway único, 401 directo
3. [`../dominio/README.md`](../dominio/README.md) — rutas, estados, roles
4. [`plan-trabajo.md`](plan-trabajo.md) — reparto Ari / Ninna / Nico / Skarlet
5. [`estrategia-branching.md`](estrategia-branching.md) — ramas, PRs, orden de merge y tag de entrega

## Git y commits

Ver [`reglas-repositorio.md`](reglas-repositorio.md) §1–3 y [`estrategia-branching.md`](estrategia-branching.md).

## Reglas del producto

- **Identidad:** Microsoft Entra ID; scope `access_as_user`; audience = API `api-cloud-native`.
- **Entrada pública:** AWS API Gateway; React **no** apunta a IP:8080 en producción.
- **BFF:** OAuth2 Resource Server; **sin** JPA ni acceso directo a BD.
- **Persistencia:** solo `ms-solicitudes` y `ms-catalogo` → PostgreSQL 16 (`infra/`).
- **Secretos:** no commitear `.env`, `.pem`, Client IDs del profesor ni contraseñas reales.
- Documentación nueva en `docs/`; en la raíz solo `README.md`.

## Dónde implementar

| Cambio | Carpeta |
| --- | --- |
| Login, UI, llamadas API | `frontend/` |
| JWT, proxy, autorización por rol | `bff/` |
| Solicitudes / estados | `ms-solicitudes/` |
| Catálogo | `ms-catalogo/` |
| MySQL 8.4 y RabbitMQ local/EC2 | `infra/` |

## Backend (Spring)

- JWT: `ENTRA_ISSUER_URI`, `ENTRA_AUDIENCE`; rutas protegidas con `SCOPE_access_as_user`.
- BFF: proxy HTTP a MS; no añadir JPA al BFF.
- MS: rutas de negocio autenticadas; CORS con origen del React e incluir `PATCH` donde aplique.
- Autorización por rol preferentemente en BFF; **403** si el rol no corresponde.

## Frontend (React)

- MSAL y API vía variables `REACT_APP_*` en `.env` local (plantilla en `.env.example`).
- `REACT_APP_API_BASE_URL`: local → BFF; AWS → URL del Gateway.
- Manejar 401/403 en las llamadas Axios.

## Documentación

- Caso oficial: `docs/caso/ep1-guia-oficial.md`.
- Imágenes en `docs/assets/`; español claro; sin tokens ni secretos en ejemplos.
