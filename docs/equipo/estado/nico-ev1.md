# Estado EV1 — Nico (EC2, seguridad, integración Git)

Este archivo registra lo que quedó **hecho en la EV1** (PostgreSQL 16). En EP2 el motor es MySQL 8.4 y RabbitMQ 4.0 está en Docker sin uso desde Java. No reescribas las evidencias de abajo: describen esa entrega.

Última actualización del registro EV1: integración en rama **`dev`** / **`feature/nico-ec2`** (mismo commit). **`main`** aún no recibe merge (revisión E2E pendiente).

---

## 1. ¿Qué ya está en Git? (no hace falta re-commitear código)

| Commit | Tipo | Contenido relevante para Nico |
| --- | --- | --- |
| `e9a4d81` | Integración | UI Skarlet + MS transiciones (merge PR #2 → `dev`) |
| `f63c1e9` | FEAT | **403 BFF** por app roles + [`rutas-api-gateway-bff.md`](../rutas-api-gateway-bff.md) |
| `bd3d944` | FEAT | **`infra/`**, CORS BFF, diagrama PNG, [`diagrama.md`](../../arquitectura/diagrama.md) |

Rama de trabajo: **`feature/nico-ec2`** (tracking `origin/feature/nico-ec2`).  
Integración del equipo: **`dev`**.

Solo hace falta **nuevos commits** si cambias código o **esta documentación / checklists**.

---

## 2. Bloques del plan (C + D + extras)

| Bloque | Objetivo | Estado | Notas |
| --- | --- | --- | --- |
| **C — Despliegue EC2** | BFF + 2 MS + Postgres | **Hecho en AWS** (3 EC2) | Ver §3; servicios con `nohup`, JARs en `~/apps/` |
| **C — Repo `infra/`** | Scripts, compose, systemd, docs SG | **Hecho en Git** | `infra/scripts/build-jars.ps1`, `run-local-stack.ps1`, etc. |
| **D — Seguridad red** | SG: MS/DB no 0.0.0.0/0; BFF acotado | **Hecho** | NIC-08 timeout; NIC-09 capturas |
| **D — JWT BFF** | 401 sin token; issuer/audience en EC2 | **Hecho** | NIC-07 (`54.90.110.67:8080`) |
| **D — CORS BFF** | `CORS_ALLOWED_ORIGINS` | **Hecho en código** | Coordinar origen Gateway + React con Ninna |
| **D — Roles 403** | BFF antes de proxy | **Hecho en Git** (`EntraRoles`) | Depende app roles en Entra (**Ari**) |
| **Diagrama / arquitectura** | NIC-10, informe §12 | **Casi listo** | PNG + `diagrama.md`; falta **Invoke URL** Ninna en figura |
| **Gateway rutas** | Contrato HTTP para Ninna | **Doc en repo** | [`rutas-api-gateway-bff.md`](../rutas-api-gateway-bff.md) |
| **Catálogo CRUD** | PUT/DELETE catálogo + UI admin | **En repo (Nico)** | Falta: rutas Gateway + JARs EC2; ver plan § Catálogo CRUD |
| **P — Presentación** | PPT + ensayo | **Pendiente** | Capturas NIC + kit marca Ninna |

---

## 3. Topología real en AWS (us-east-1)

Decisión documentada: **3 EC2** (no monolito). Detalle: [`infra/ec2/TOPOLOGIA.md`](../../../infra/ec2/TOPOLOGIA.md), [`diagrama.md`](../../arquitectura/diagrama.md).

| Instancia | IP pública | IP privada | Rol |
| --- | --- | --- | --- |
| `mesatech-ev1-db` | 54.90.194.247 | 172.31.25.66 | Postgres Docker :5432 |
| `mesatech-ev1-ms` | 54.227.0.33 | 172.31.24.74 | MS :8081 / :8082 |
| `mesatech-ev1-bff` | **54.90.110.67** | 172.31.20.75 | BFF :8080 → Ninna integra Gateway aquí |

**Entregado a Ninna:** `http://54.90.110.67:8080` (puede requerir abrir 8080 en BFF-SG al tráfico del integrador Gateway).

---

## 4. Capturas rúbrica (NIC)

| ID | Estado | Evidencia / pendiente |
| --- | --- | --- |
| NIC-01 | Listo | Consola EC2 running (3 instancias) |
| NIC-02 | Listo | Detalle AMI/tipo/región |
| NIC-03 | Listo | `docker ps` Postgres en DB EC2 |
| NIC-04 | Listo | Compose / volúmenes |
| NIC-05 | Listo | 8080 / 8081 / 8082 escuchando |
| NIC-06 | **Pendiente** | Persistencia con JWT vía Gateway o flujo acordado (Skarlet/Ninna) |
| NIC-07 | Listo | BFF 401 sin JWT; `/public/hola` 200 |
| NIC-08 | Listo | MS :8081 timeout desde Internet |
| NIC-09 | Listo | SG db / ms / bff |
| NIC-10 | **Casi** | PNG en repo; completar Invoke URL cuando Ninna entregue |
| NIC-11 | **Pendiente** | Captura `bff/pom.xml` sin JPA + log proxy MS (rápido) |

Carpeta informe (fuera de Git): `capturas/nico/` según [`COMO-HACER-CAPTURAS.md`](../guias/COMO-HACER-CAPTURAS.md).

---

## 5. Coordinación con el equipo

| Integrante | Qué falta para cerrar EV1 cloud |
| --- | --- |
| **Ari** | App roles en Entra + usuarios de prueba (403 demostrable) |
| **Ninna / Nico (contingencia)** | HTTP API, rutas § [`rutas-api-gateway-bff.md`](../rutas-api-gateway-bff.md), JWT Authorizer, CORS, Invoke URL → front — runbook [`ninna-ev1.md`](ninna-ev1.md) |
| **Skarlet** | E2E React → Gateway; NIC-06; informe §11 |
| **Nico** | NIC-06/11 capturas; PPT bloques EC2/seguridad; PR `dev` → `main` cuando el equipo apruebe |

---

## 6. Comandos Git (recordatorio)

```bash
git checkout feature/nico-ec2
git pull origin feature/nico-ec2
# commits de doc o infra...
git push origin feature/nico-ec2
# integrar en dev cuando corresponda:
git checkout dev && git merge feature/nico-ec2 && git push origin dev
```

No mergear a **`main`** hasta checklist §15 y E2E en `dev`.

---

## Documentos relacionados

- Estado Skarlet (UI/pruebas): [`skarlet-ev1.md`](skarlet-ev1.md)
- Guía operativa: [`guias/nico-ec2-seguridad-presentacion.md`](../guias/nico-ec2-seguridad-presentacion.md)
- Plan equipo: [`plan-trabajo.md`](../plan-trabajo.md) (sección Nico)
- Branching: [`estrategia-branching.md`](../estrategia-branching.md)
