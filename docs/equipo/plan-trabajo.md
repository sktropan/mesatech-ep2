# Plan de trabajo EV1 — MesaTech Cloud

Equipo: **Ari**, **Ninna**, **Nico**, **Skarlet** (Nicolas coordina despliegue/seguridad en bloque C+D).

Monorepo: `frontend/`, `bff/`, `ms-solicitudes/`, `ms-catalogo/`, `infra/`.  
Guías: ver índice en [`docs/README.md`](../README.md).

**Paso a paso por integrante (sin experiencia previa):** [`guias/README.md`](guias/README.md).  
**Capturas para la rúbrica:** [`guias/COMO-HACER-CAPTURAS.md`](guias/COMO-HACER-CAPTURAS.md) + sección “Capturas” al final de cada guía personal.

| Integrante | Guía |
| --- | --- |
| Ari | [`guias/ari-identidad-azure.md`](guias/ari-identidad-azure.md) |
| Ninna | [`guias/ninna-gateway-marca-versiones.md`](guias/ninna-gateway-marca-versiones.md) |
| Nico | [`guias/nico-ec2-seguridad-presentacion.md`](guias/nico-ec2-seguridad-presentacion.md) |
| Skarlet | [`guias/skarlet-negocio-pruebas-informe.md`](guias/skarlet-negocio-pruebas-informe.md) |

---

## Vista rápida

| Persona | Bloques | Foco |
| --- | --- | --- |
| **Ari** | A | Microsoft Entra ID (IDaaS) |
| **Ninna** | B + F + **H** | API Gateway, versionamiento, **identidad corporativa** |
| **Nico** | C + D + **P** | EC2, PostgreSQL Docker, red, 401 directo, **presentación oral** |
| **Skarlet** | E + G | Negocio, roles, UI, pruebas e informe final |

---

## Dependencias (orden lógico)

```text
Ari (Entra: tenant, apps, scope, roles)
  ├─→ Ninna (JWT Authorizer issuer/audience + pruebas Gateway)
  ├─→ Nico (ENTRA_* en EC2 + validación JWT en BFF/MS)
  └─→ Skarlet (roles en token → autorización 403)

Nico (BFF/MS/MySQL en EC2, SG, URLs internas)
  └─→ Ninna (integración HTTP API → BFF :8080 en EC2)

Ninna (Gateway público + REACT_APP_API_BASE_URL)
  └─→ Skarlet (E2E y pruebas sección 11 vía Gateway)

Skarlet (E listo + resto del sistema)
  └─→ G (informe, checklist 15, diagrama final)

Ninna (identidad corporativa: paleta, logo, tipografía)
  └─→ Skarlet (aplicar en React) + Nico (plantilla PPT)

Nico (presentación)
  ← capturas de todos + identidad visual de Ninna + borradores de informe (Skarlet)
```

**Reuniones de sync sugeridas:** cuando Ari cierre Entra; cuando Nico entregue IP/URL del BFF; cuando Ninna entregue URL del Gateway; **pre-presentación** (Ninna entrega kit visual; Nico arma slides).

---

## Ari — A. Identidad (Azure)

**Objetivo:** Entra ID listo para React, API Gateway Authorizer y Spring Resource Server.

**Estado (2026-09-17):** portal + roles **listos**. Falta prueba local, handoff confirmado y capturas. Detalle: [`guias/ari-identidad-azure.md`](guias/ari-identidad-azure.md) § Estado EV1.

### Tareas

1. Registrar **SPA React** (redirect URI `http://localhost:3000/` + URL de producción si aplica). **Hecho** (URI de producción: cuando exista el hosting).
2. Registrar **`api-cloud-native`** (API expuesta). **Hecho**
3. **Exponer API:** URI `api://<API_CLIENT_ID>`, ámbito **`access_as_user`**. **Hecho**
4. Consentimiento de administrador en SPA y API (permiso delegado al scope). **Hecho**
5. Manifiesto: **`requestedAccessTokenVersion`: 2**. **Hecho**
6. Definir **roles de negocio**. **Hecho** — App roles en `api-cloud-native` (`cliente`, `operador`, `administrador`) y 3 usuarios de prueba asignados.
7. Entregar al equipo (canal seguro, **no en git**) — **pendiente de confirmar**:
   - `ENTRA_TENANT_ID`
   - `ENTRA_SPA_CLIENT_ID`
   - `ENTRA_API_CLIENT_ID` (= audience)
   - `ENTRA_ISSUER_URI` = `https://login.microsoftonline.com/<TENANT>/v2.0`
   - Scope para front: `api://<API_CLIENT_ID>/access_as_user`
8. Evidencias para informe: capturas App Registrations, Exponer API, Permisos, Manifiesto v2, usuarios/roles — **pendiente** (ARI-01 … ARI-10).

### Entregables a otros

| Para | Qué |
| --- | --- |
| Ninna | Issuer URI + audience (Client ID API) para JWT Authorizer |
| Nico | Mismas variables para EC2 |
| Skarlet | Usuarios de prueba por rol + cómo leer el rol en el JWT |
| Todos | Plantilla `frontend/.env` y `env.example` actualizados (solo placeholders en repo) |

### Checklist Ari (propia)

- [x] SPA + API registradas en **vuestro** tenant (no IDs del profesor).
- [x] Scope `access_as_user` concedido.
- [x] Token v2 en manifiesto.
- [x] Al menos 3 usuarios o roles distinguibles para demo cliente/operador/admin.
- [ ] `frontend/.env` local + login + `GET /api/usuario` (claim `roles` en jwt.ms).
- [ ] Handoff por chat (issuer, audience, usuarios demo) a Ninna, Nico y Skarlet.
- [ ] Capturas ARI para informe (Skarlet) y 2–3 para PPT (Nico).

---

## Ninna — B. AWS API Gateway + F. Versionamiento

**Objetivo:** único entrypoint público; React apunta al Gateway; v1/v2 documentados.

**Prerrequisito:** Ari entregó issuer/audience; Nico entregó URL/IP del BFF en EC2 (`http://<privado-o-público-según-Nico>:8080`).

### B. API Gateway

1. Crear **HTTP API** en AWS.
2. Rutas hacia integración **HTTP** al BFF (proxy), al menos:
   - `/v1/solicitudes`, `/v1/solicitudes/mias`, `/v1/solicitudes/{id}/estado`
   - `/v1/catalogo`, `/v1/catalogo/...`
   - `/v2/solicitudes/mias` (y las que use el BFF)
   - `/api/usuario` (demo JWT/claims)
   - Opcional: `/public/hola` solo si lo piden en demo
3. **JWT Authorizer:** issuer y audience de Entra (Ari).
4. **CORS:** origen del React (localhost:3000 + hosting del front), headers `Authorization`, `Content-Type`, métodos `GET, POST, PATCH, PUT, DELETE, OPTIONS`.
5. Pruebas:
   - Sin `Authorization` → **401**
   - JWT inválido/expirado → **401**
   - JWT válido con scope → **2xx** (BFF responde)
6. Coordinar con Nico: React en producción usa **`REACT_APP_API_BASE_URL`** = URL del Gateway (PR o commit en `.env.example` + doc).
7. Capturas: API creada, rutas, integración, Authorizer, CORS, prueba 401/200.

### F. Versionamiento

1. Redactar en informe (sección Skarlet puede integrar):
   - **v1:** `GET /v1/solicitudes/mias` → lista de solicitudes.
   - **v2:** `GET /v2/solicitudes/mias` → envoltorio `{ version, usuario, solicitudes }`.
   - **Por qué se mantiene v1:** clientes antiguos / contrato estable; v2 extiende sin romper.
2. Evidencia: misma sesión, mismo token, llamadas a v1 y v2 (Postman o capturas desde React).

### Entregables a otros

| Para | Qué |
| --- | --- |
| Skarlet | URL base del Gateway + colección Postman exportada |
| Nico | Confirmación de que solo el Gateway debe llegar al BFF desde internet |
| Equipo | Variable `REACT_APP_API_BASE_URL` documentada en README |

### H. Identidad corporativa (MesaTech Cloud)

**Objetivo:** imagen coherente en React, informe y presentación (marca ficticia del caso de negocio).

1. Definir **identidad visual** mínima:
   - Nombre y tagline (ej. soporte tecnológico centralizado).
   - **Paleta de colores** (primario, secundario, fondo, texto, estados/error).
   - **Tipografía** (web-safe o Google Fonts acordada con el front).
   - **Logo o isotipo** (SVG/PNG en alta resolución; variantes claro/oscuro si aplica).
2. Entregar **kit al equipo** (carpeta compartida fuera de secretos, o `frontend/public/` + `docs/assets/marca/` en el repo **sin** datos sensibles):
   - Archivos de logo.
   - Guía corta de uso (márgenes, tamaño mínimo del logo, no deformar).
3. Coordinar con **Skarlet:** aplicar estilos en `frontend/` (CSS, favicon, título de la app).
4. Coordinar con **Nico:** assets para la presentación (portada, íconos, diagrama con colores de marca).

### Checklist Ninna

*(Contingencia: Nico — seguir [`estado/ninna-ev1.md`](estado/ninna-ev1.md) + carpeta `E:\DOWNLOADS\MesaTech-EV1-Ninna\`.)*

- [ ] HTTP API + Authorizer Entra operativo (capturas NIN-01–NIN-07).
- [ ] CORS probado desde navegador (NIN-05, NIN-09).
- [ ] Front configurado para Gateway — Invoke URL en `frontend/.env` (entrega final).
- [ ] Texto v1 vs v2 en Word / informe (NIN-10, NIN-11).
- [ ] Kit marca: [`assets/marca/guia-marca.md`](../assets/marca/guia-marca.md) + NIN-H1.
- [x] Doc rutas Gateway en repo ([`rutas-api-gateway-bff.md`](rutas-api-gateway-bff.md)).
- [x] URL BFF EC2 para integración: `http://54.90.110.67:8080` (Nico).

---

## Nico — C. Despliegue EC2 + D. Seguridad y arquitectura

**Objetivo:** BFF + 2 MS + MySQL 8.4 en EC2; acceso directo a MS (y BFF si aplica) → 401 o bloqueo de red. RabbitMQ 4.0 va en el mismo Compose y el código aún no lo usa.

**Prerrequisito:** Ari entregó `ENTRA_ISSUER_URI` y `ENTRA_AUDIENCE`.

### C. Despliegue EC2

1. Instancia(s) EC2 (una con puertos 8080/8081/8082 o tres instancias — documentar decisión).
2. Desplegar JARs o `java -jar` / systemd:
   - **BFF** `:8080`
   - **ms-solicitudes** `:8081`
   - **ms-catalogo** `:8082`
3. **MySQL 8.4** con `infra/docker-compose.yml` (en `infra/`: `cp .env.example .env`). En EC2, el override publica solo loopback. RabbitMQ 4.0 sube junto, sin clientes Java todavía.
4. Bases: `solicitudes`, `catalogo`.
5. Variables en EC2:
   - `ENTRA_ISSUER_URI`, `ENTRA_AUDIENCE`
   - `MS_SOLICITUDES_URL=http://127.0.0.1:8081` (o IP privada)
   - `MS_CATALOGO_URL=http://127.0.0.1:8082`
   - `DB_USER`, `DB_PASSWORD`
   - `DB_URL` solo si MySQL no está en localhost, y una URL por servicio (`jdbc:mysql://<host>:3306/solicitudes` y `.../catalogo`)
6. Evidencias: SSH, `docker ps`, servicios escuchando, logs de arranque, consulta que persiste datos.

### D. Seguridad y arquitectura

1. **Security groups:**
   - BFF: entrante **443/80 solo desde API Gateway** (o IP pública del Gateway si aplica) + **22** admin restringido a IPs del equipo.
   - MS **8081/8082:** solo desde SG del BFF (no 0.0.0.0/0).
2. Verificar **401** en acceso directo desde internet a MS (puerto cerrado o rechazo JWT).
3. BFF: mantener OAuth2 Resource Server; probar issuer/audience en EC2 (no solo local).
4. **CORS:** BFF permite origen del React; Gateway tiene CORS principal para el browser.
5. Actualizar **diagrama final** (con Skarlet): IPs/hostnames reales y puertos.

### Entregables a otros

| Para | Qué |
| --- | --- |
| Ninna | URL de integración del BFF (la que Gateway usará) |
| Skarlet | Entorno estable para pruebas E2E |
| Ari/Skarlet | Confirmación de que tokens emitidos funcionan contra BFF en EC2 |

### P. Presentación oral (EP1)

**Objetivo:** diapositivas para la exposición del grupo (guía EP1 § instrucciones generales: resumen Cloud + prints de React; **no** demo en vivo durante la presentación).

**Prerrequisitos:** borrador de contenido de Ari/Ninna/Skarlet; **identidad corporativa** de Ninna (H).

1. Armar **PowerPoint/Google Slides** (o equivalente) con estructura sugerida:
   - Portada MesaTech Cloud (marca Ninna).
   - Contexto del caso (1 slide).
   - Arquitectura resumida (diagrama del equipo, URLs reales).
   - Microsoft Entra ID — **resumen** (Ari aporta registro de `api-cloud-native`, URI de id. de aplicación, consentimiento y roles).
   - API Gateway — **resumen** (Ninna).
   - EC2 + PostgreSQL + seguridad 401 (Nico).
   - React: login, roles, flujo de negocio — **prints** (Skarlet).
   - Versionamiento v1/v2 (Ninna).
   - Cierre: lecciones aprendidas / división de trabajo.
2. Repartir **tiempo de palabra** entre el equipo (coordinar con el docente si hay límite).
3. Ensayo: cada integrante domina su bloque; Nico modera la presentación.
4. Entregar archivo de presentación junto al informe (Teams/Aula según indique el profesor).

### Nico — Catálogo CRUD completo (EP1 §APIs + UI admin)

**Responsable:** **Nicolás (Nico)** — código BFF/`ms-catalogo`/React + rutas Gateway + redeploy EC2.

1. **Backend:** `PUT`/`DELETE` en `/v1/catalogo/categorias/{id}` y `/v1/catalogo/prioridades/{id}`; BFF proxy + **403** si no es administrador (misma regla que POST).
2. **Frontend:** pestaña Catálogo — editar y eliminar categorías/prioridades (`PanelCatalogo`).
3. **Gateway:** cuatro rutas nuevas (tabla en [`rutas-api-gateway-bff.md`](rutas-api-gateway-bff.md)).
4. **Evidencia:** captura **`NIN-CRUD.png`** (UI catálogo + Network POST/PUT/DELETE **200/204**).
5. Tras merge: `build-jars.ps1`, subir JARs a EC2 MS+BFF, reiniciar procesos.

### Checklist Nico

- [ ] Catálogo CRUD (PUT/DELETE) en repo + Gateway + EC2 (**Nico**).
- [x] Tres servicios Java + Postgres arriba en EC2 en la entrega EV1 (3 instancias; ver [`estado/nico-ev1.md`](estado/nico-ev1.md)). EP2 cambia el motor a MySQL 8.4.
- [x] MS no accesibles desde Internet.
- [x] BFF valida JWT con issuer/audience del tenant del equipo (EC2 + local).
- [x] Documento topología EC2: [`infra/ec2/TOPOLOGIA.md`](../infra/ec2/TOPOLOGIA.md) + diagrama en repo.
- [x] `infra/`, CORS BFF, doc rutas Gateway; integración en rama **`dev`** (sin merge a `main` aún).
- [ ] Presentación lista, con identidad corporativa y capturas acordadas del equipo.

---

## Skarlet — E. Negocio y autorización + G. Pruebas y entrega

**Objetivo:** matriz cliente/operador/admin, UI MesaTech, informe y checklist EP1 completo.

**Prerrequisitos:** roles definidos por Ari; Gateway URL de Ninna; EC2 estable de Nico.

### E. Negocio y autorización

1. **Autorización** (BFF preferido; MS si hace falta defensa en profundidad):

   | Acción | Cliente | Operador | Admin |
   | --- | --- | --- | --- |
   | Crear solicitud | Sí | Opcional | Opcional |
   | Ver propias | Sí | Sí | Sí |
   | Ver todas | No | Sí | Sí |
   | Cambiar estado | No | Sí | Sí |
   | Catálogo CRUD | No | No | Sí |

2. Respuestas **403** cuando el rol no corresponda (evidencia en informe).
3. **Flujo de estados:** CREADA → ASIGNADA → EN_PROCESO → RESUELTA → CERRADA; CANCELADA con reglas; mantener regla RESUELTA solo desde EN_PROCESO.
4. **Frontend** (`frontend/`):
   - Crear solicitud
   - Listar mías / todas (según rol)
   - Cambiar estado (operador)
   - CRUD catálogo (admin)
   - Mostrar rol actual y claims relevantes
5. Demo clara de **tres perfiles** (tres usuarios Entra).

### G. Pruebas y entrega

1. **9 pruebas** sección 11 EP1 (capturas/Postman): sin auth, Gateway sin token, JWT inválido, JWT OK, acción prohibida, CORS, regla estado, v1+v2, persistencia.
2. **Flujo E2E** sección 10 (12 pasos) con prints.
3. **Informe** pormenorizado: Azure (Ari), AWS Gateway (Ninna), EC2/MySQL (Nico), React/negocio (Skarlet) — integrar capturas de todos. En la EV1 las capturas de Nico muestran PostgreSQL; en EP2 la persistencia es MySQL 8.4.
4. **Diagrama final:** componente → servicio → URL/ruta real (actualizar [`docs/arquitectura/diagrama.md`](../arquitectura/diagrama.md) o anexo PDF).
5. **Checklist 15 ítems** (EP1 §15) marcado con evidencia cruzada.
6. Revisión: **ningún secret** en repo (`.env`, claves, IDs del profesor).

### Checklist Skarlet

- [x] Matriz de permisos implementada en BFF + UI (demostración SKA-T05 pendiente con roles Ari).
- [x] UI mínima usable en `dev` (pantallas MesaTech, no JSON crudo).
- [ ] Informe único con índice por responsable.
- [ ] Checklist §15 completo con figuras (ver [`estado/skarlet-ev1.md`](estado/skarlet-ev1.md)).

---

## Reparto del informe (sección 1 EP1)

| Capítulo | Responsable principal | Contribuciones |
| --- | --- | --- |
| Microsoft Entra ID | Ari | — |
| API Gateway | Ninna | Nico (SG/CORS backend) |
| EC2 + PostgreSQL | Nico | — |
| React + MSAL + negocio | Skarlet | Ari (login), Ninna (URL Gateway) |
| Versionamiento v1/v2 | Ninna | Skarlet (evidencia funcional) |
| Pruebas y checklist | Skarlet | Todos aportan capturas |
| Identidad corporativa (informe + UI) | Ninna | Skarlet (React) |
| Presentación oral | Nico | Todos aportan slides/capturas |

---

## Checklist global EP1 (§15) — responsable sugerido

| Ítem | Owner |
| --- | --- |
| Login/logout Entra | Ari + Skarlet (UI) |
| Access Token API | Ari + Skarlet |
| Front solo vía Gateway | Ninna + Skarlet |
| CORS | Ninna + Nico |
| Gateway rechaza sin JWT | Ninna |
| BFF revalida JWT | Nico + Ari |
| BFF → MS, sin BD | Nico (hecho) + Skarlet (prueba) |
| Dos MS persisten | Nico + Skarlet |
| v1 y v2 coexisten | Ninna + Skarlet |
| Despliegue EC2 | Nico |
| Diferencias por tipo usuario | Skarlet + Ari (roles) |
| Sin secretos en repo | Skarlet (revisión final) |

---

## Riesgos y acuerdos de equipo

1. **No avanzar Gateway** sin issuer/audience de Ari (Ninna bloqueada).
2. **No cerrar informe** sin URL Gateway + EC2 + tres usuarios rol.
3. **Entrega final React:** `REACT_APP_API_BASE_URL` = Gateway, no `:8080` directo en demo de evaluación.
4. Credenciales solo por canal privado; en git solo `env.example`.

---

## Git y branching

Reglas Git/commits/PR (obligatorio): **[`reglas-repositorio.md`](reglas-repositorio.md)**.  
Estrategia ampliada (cronograma, plantilla PR): **[`estrategia-branching.md`](estrategia-branching.md)**.

Resumen:

| Integrante | Rama feature principal |
| --- | --- |
| Ari | `feature/ari-entra` |
| Ninna | `feature/ninna-gateway` (+ `feature/ninna-marca`) |
| Nico | `feature/nico-ec2` |
| Skarlet | `feature/skarlet-negocio` |

Merge a **`main`** solo por PR con al menos 1 revisión ajena.
