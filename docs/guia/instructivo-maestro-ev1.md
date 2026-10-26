# Instructivo maestro EV1 — paso a paso por fases

Hoja de ruta única para cerrar la evaluación. Detalle técnico por capa en los enlaces; reparto humano en [`../equipo/plan-trabajo.md`](../equipo/plan-trabajo.md).

**Regla de oro:** en producción, React → **solo API Gateway**; directo a BFF/MS → **401**. Sin secretos en Git.

---

## Fase 0 — Entorno local (todos)

1. Clonar el monorepo; leer [`../onboarding/refs-y-entorno.md`](../onboarding/refs-y-entorno.md).
2. Opcional: clonar `_refs/` (no va a Git).
3. `cd infra && docker compose up -d` (PostgreSQL 16).
4. Copiar `env.example` y `frontend/.env.example` → `.env` locales (canal privado para IDs de Entra).
5. Arrancar `bff`, `ms-solicitudes`, `ms-catalogo` con `ENTRA_ISSUER_URI` y `ENTRA_AUDIENCE`.
6. `cd frontend && npm install && npm start` — login contra Entra del **equipo**.

**Listo cuando:** login/logout, `GET /api/usuario` y al menos una ruta `/v1/...` responden en local.

---

## Fase A — Identidad Azure (**Ari**)

Seguir [`entra-spring-react.md`](entra-spring-react.md) **Parte A** (pasos 1–11 del portal).  
**Estado (2026-09-17):** ítems 1–5 de este listado **hechos** (portal + roles). Pendiente: prueba local (Fase 0 con IDs del equipo), handoff confirmado y capturas. Ver [`../equipo/guias/ari-identidad-azure.md`](../equipo/guias/ari-identidad-azure.md).

1. ~~Registro SPA React (redirect URI localhost + producción).~~ **Hecho** (producción: cuando haya URL).
2. ~~Registro API `api-cloud-native`.~~ **Hecho**
3. ~~Exponer scope `access_as_user`; token **v2** en manifiesto.~~ **Hecho**
4. ~~Permisos de la SPA sobre la API + consentimiento admin.~~ **Hecho**
5. ~~Roles de negocio (`cliente`, `operador`, `administrador`) y usuarios de prueba.~~ **Hecho**
6. Entregar por canal seguro: tenant, client IDs, issuer, scope, usuarios demo — **confirmar**.

**Handoff:** Ninna (Authorizer), Nico (env EC2), Skarlet (403 por rol).

Las Partes B y C de `entra-spring-react.md` (Spring Initializr / React desde cero) **no** se rehacen: el código está en `bff/` y `frontend/`.

---

## Fase B — API Gateway (**Ninna**)

**Prerrequisitos:** A cerrada; Nico entrega URL del BFF en EC2 (o IP:8080 según SG).

1. Crear **HTTP API** en AWS.
2. Rutas proxy hacia BFF: `/v1/**`, `/v2/**`, `/api/usuario` (y las que use el front).
3. **JWT Authorizer:** issuer + audience (Entra del equipo).
4. **CORS:** origen del React, header `Authorization`, métodos usados.
5. Probar con Postman/curl:
   - Sin token → **401**
   - Token válido → respuesta del BFF
6. Configurar `REACT_APP_API_BASE_URL` al URL del Gateway (build de demo).
7. Capturas para informe.

---

## Fase C — Despliegue EC2 (**Nico**)

**Prerrequisito:** variables Entra de Ari.

1. EC2: desplegar JARs BFF `:8080`, MS `:8081` / `:8082`.
2. MySQL 8.4 y RabbitMQ 4.0 con [`../../infra/docker-compose.yml`](../../infra/docker-compose.yml) adaptado en la instancia. Antes: `cp .env.example .env` dentro de `infra/`. El flujo del evento está en [`../pruebas-rabbitmq.md`](../pruebas-rabbitmq.md).
3. Variables: `ENTRA_*`, `MS_SOLICITUDES_URL`, `MS_CATALOGO_URL`, `DB_USER`, `DB_PASSWORD`. `DB_URL` solo si la base no está en localhost, y una URL distinta por microservicio (`catalogo` y `solicitudes`).
4. Evidencias: servicios activos, datos persisten.

---

## Fase D — Seguridad y arquitectura (**Nico**)

1. Security groups: MS no públicos; BFF solo desde Gateway (+ SSH admin acotado).
2. Verificar **401** (o puerto cerrado) al llamar BFF/MS desde internet sin pasar por Gateway.
3. Confirmar segunda validación JWT en BFF (`iss`, `aud`).
4. CORS coherente Gateway + BFF.
5. Actualizar [`../arquitectura/diagrama.md`](../arquitectura/diagrama.md) con URLs reales.

---

## Fase E — Negocio y autorización (**Skarlet**)

**Prerrequisitos:** roles en token (Ari); API estable (local o Gateway).

1. Implementar matriz en BFF (y/o MS): 403 en acciones prohibidas.
2. Completar transiciones de estado + **CANCELADA**; regla RESUELTA ← EN_PROCESO.
3. Front: crear solicitud, listados por rol, cambio estado, CRUD catálogo admin.
4. Demo con tres usuarios Entra (capturas + Postman 403).

Contratos: [`../dominio/README.md`](../dominio/README.md).

---

## Fase F — Versionamiento (**Ninna** + evidencia **Skarlet**)

1. Documentar en informe diferencia v1 vs v2 y por qué conviven.
2. Misma operación en `/v1/solicitudes/mias` y `/v2/solicitudes/mias` (capturas).

---

## Fase H — Identidad corporativa (**Ninna**)

1. Paleta, logo/isotipo, tipografía y guía breve de uso (marca MesaTech Cloud).
2. Entregar assets a Skarlet (React) y Nico (presentación). Detalle: [`../equipo/plan-trabajo.md`](../equipo/plan-trabajo.md) § H.

---

## Fase G — Pruebas y entrega (**Skarlet** lidera)

Referencia: [`../caso/ep1-guia-oficial.md`](../caso/ep1-guia-oficial.md) §10, §11, §15.

1. **9 pruebas mínimas** (§11): documentar resultado esperado vs obtenido.
2. **Flujo E2E** 12 pasos (§10) con prints.
3. **Informe** único: Azure (Ari), Gateway (Ninna), EC2 (Nico), React/negocio (Skarlet).
4. **Diagrama final:** componente → servicio → URL/ruta real.
5. **Checklist §15** marcado.
6. Revisión: `git status` sin `.env`, `.pem`, tokens en commits.

---

## Fase P — Presentación oral (**Nico**)

1. Slides con resumen Cloud + prints React (sin demo en vivo en la exposición).
2. Usar identidad corporativa (Ninna); contenido aportado por Ari, Ninna y Skarlet. Detalle: [`../equipo/plan-trabajo.md`](../equipo/plan-trabajo.md) § P.

---

## Orden recomendado (cronograma)

```text
Semana 1: Fase 0 + A (Ari) + inicio E en local (Skarlet)
          → Portal Entra + roles: listos. Cerrar prueba local + handoff.
Semana 2: C + D (Nico) en paralelo con B (Ninna tras handoff)
Semana 3: E en Gateway + F + H (Ninna) + G + informe + P (Nico)
```

Sync obligatorios: post-Entra, post-EC2, post-Gateway.

---

## Documentos de apoyo

| Necesidad | Documento |
| --- | --- |
| Rúbrica completa | [`../caso/ep1-guia-oficial.md`](../caso/ep1-guia-oficial.md) |
| Entra 19 pasos + capturas | [`entra-spring-react.md`](entra-spring-react.md) |
| Qué dijo el profesor | [`../contexto/anotaciones-profesor.md`](../contexto/anotaciones-profesor.md) |
| Términos técnicos | [`../glosario/README.md`](../glosario/README.md) |
| Contratos negocio | [`../dominio/README.md`](../dominio/README.md) |
