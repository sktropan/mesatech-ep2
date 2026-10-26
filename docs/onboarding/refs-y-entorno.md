# Guía de referencias (`_refs`) y lo que no va a Git

Documento para el equipo (Ari, Ninna, Nico, Skarlet). Explica de dónde salió el código, cómo clonar las referencias del profesor **en local**, y qué **nunca** debe subirse al repositorio.

`_refs/` **no está en Git**. Quien clone `duoc-mesatech` no verá esa carpeta hasta que la cree en su máquina.

---

## 1. Qué es `_refs/`

Es una carpeta **solo local** con clones de los repos de ejemplo del profesor. Sirven para analizar el patrón de clases (MSAL + JWT Resource Server) y compararlo con MesaTech.

| Carpeta local | Repo origen | Qué demuestra |
| --- | --- | --- |
| `_refs/dfs-s3-login-1/` | [pauloberrios44/dfs-s3-login-1](https://github.com/pauloberrios44/dfs-s3-login-1.git) | React (CRA) + MSAL + Axios: login/logout Entra ID y llamada `GET /api/usuario` |
| `_refs/backend-springboot-jwt/` | [pauloberrios44/backend-springboot-jwt](https://github.com/pauloberrios44/backend-springboot-jwt.git) | Spring Boot OAuth2 Resource Server: valida JWT (`iss`, `aud`), CORS, `/public/hola` y `/api/usuario` |

**No son el producto a entregar.** El entregable es el monorepo MesaTech (`frontend/`, `bff/`, `ms-solicitudes/`, `ms-catalogo/`, `infra/`). Las referencias se usaron como base conceptual y se adaptaron al caso de negocio.

Cada clone trae su propio `.git`. Por eso `_refs/` está en `.gitignore`: un repo dentro de otro ensucia el historial y, además, los ejemplos del profesor contienen **IDs de su tenant**.

---

## 2. Cómo obtener `_refs/` después de clonar este repo

Desde la raíz de `duoc-mesatech`:

```bash
mkdir _refs
cd _refs
git clone https://github.com/pauloberrios44/dfs-s3-login-1.git
git clone https://github.com/pauloberrios44/backend-springboot-jwt.git
```

Opcional: no hace falta `npm install` ni `mvn spring-boot:run` ahí. El código que corre el equipo es el de `frontend/`, `bff/`, etc.

Si ya tienes `_refs/` y quieres actualizar las referencias:

```bash
cd _refs/dfs-s3-login-1 && git pull
cd ../backend-springboot-jwt && git pull
```

---

## 3. De la referencia al código MesaTech

### Frontend: `_refs/dfs-s3-login-1` → `frontend/`

| Idea del profesor | Dónde quedó en MesaTech | Qué cambió |
| --- | --- | --- |
| `MsalProvider` + `PublicClientApplication` | `frontend/src/index.js` | Igual en espíritu; se quitó `reportWebVitals` |
| `authConfig.js` con IDs hardcodeados | `frontend/src/authConfig.js` | Lee `REACT_APP_*` desde `.env` (no hay Client ID en el código) |
| `loginRedirect` / `logoutRedirect` | `frontend/src/App.js` | Misma idea, textos de MesaTech |
| `AuthenticatedTemplate` / `UnauthenticatedTemplate` | `frontend/src/App.js` | Igual |
| `acquireTokenSilent` + Axios a `localhost:8080` | `frontend/src/api/http.js` | URL base = `REACT_APP_API_BASE_URL` (BFF en local, **API Gateway en AWS**) |
| Muestra nombre, username, `oid` | `frontend/src/App.js` | Se mantiene; además llama `/v1/solicitudes/mias` |

### Backend: `_refs/backend-springboot-jwt` → `bff/` (+ microservicios)

| Idea del profesor | Dónde quedó | Qué cambió |
| --- | --- | --- |
| Resource Server (`issuer-uri` + `audiences`) | `bff/`, `ms-solicitudes/`, `ms-catalogo/` | Valores por variables `ENTRA_ISSUER_URI` y `ENTRA_AUDIENCE`, no hardcodeados |
| `SecurityConfig`: CORS, CSRF off, JWT, `SCOPE_access_as_user` | `bff/.../config/SecurityConfig.java` | Protege también `/v1/**` y `/v2/**`; CORS incluye `PATCH` |
| `GET /public/hola` | `bff/.../controller/PublicController.java` | Se conserva para demo |
| `GET /api/usuario` (claims del JWT) | `bff/.../controller/UsuarioController.java` | Se conserva; el BFF **no tiene JPA** |
| MySQL + JPA en el mismo artefacto | **No en el BFF** | Persistencia solo en `ms-solicitudes` y `ms-catalogo` con **PostgreSQL 16** |
| Un solo API `:8080` | BFF `:8080` + MS `:8081` / `:8082` | El BFF reenvía con `MicroservicioClient` |

Los microservicios copian el mismo patrón JWT, pero **todas** sus rutas exigen `SCOPE_access_as_user` (no hay `/public/**`).

---

## 4. Qué NO copiar de `_refs` (IDs y secretos del profesor)

Los clones traen datos del tenant de clases. **No van al código MesaTech ni a un commit.**

En `_refs/dfs-s3-login-1/src/authConfig.js` aparecen:

- Tenant / authority del profesor
- Client ID de su SPA
- Scope `api://<API_DEL_PROFESOR>/access_as_user`

En `_refs/dfs-s3-login-1/src/env` hay **otros** Client ID / Tenant ID (resto de un ejercicio). Tampoco usarlos.

En `_refs/backend-springboot-jwt/src/main/resources/application.properties`:

- El mismo issuer/audience del profesor
- MySQL `localhost:3306/cloudnative1` con usuario `root` y password vacío

En MesaTech:

- Identidad = **tenant y App Registrations del equipo** (Ari). Ver [`docs/guia/entra-spring-react.md`](../guia/entra-spring-react.md).
- Persistencia = PostgreSQL Docker (`infra/`), no MySQL.
- Front en producción = URL de **API Gateway**, no `http://localhost:8080` ni la IP de EC2.

Si en un PR ves un UUID de Entra hardcodeado, password de BD o una URL de EC2: **no mergear**.

Los UUID de la PPT del profesor también están anotados como “solo referencia” en [`docs/guia/entra-spring-react.md`](../guia/entra-spring-react.md). No son los del equipo.

---

## 5. Qué no se sube a este repositorio

Está cubierto por `.gitignore` o por acuerdo del equipo. Después de `git clone` hay que **recrearlo en local**.

### 5.1 Carpeta `_refs/`

- Clones completos de los repos del profesor (código + `.git` + IDs).
- Cómo obtenerla: sección 2.

### 5.2 Archivos de entorno con valores reales

| Archivo que SÍ está en Git (plantilla) | Archivo local que NO se sube | Quién lo llena |
| --- | --- | --- |
| `env.example` | `.env` en la raíz (si lo usan para exportar variables) | Ari entrega IDs; cada uno copia en su máquina |
| `frontend/.env.example` | `frontend/.env` y `frontend/.env.local` | Quien levante el front |
| `application.properties` con `${ENTRA_*}` | `**/application-local.properties` | Opcional, overrides locales |

Variables que **nunca** van en un commit con valores reales:

```text
ENTRA_TENANT_ID
ENTRA_SPA_CLIENT_ID
ENTRA_API_CLIENT_ID
ENTRA_ISSUER_URI
ENTRA_AUDIENCE
REACT_APP_ENTRA_CLIENT_ID
REACT_APP_ENTRA_TENANT_ID
REACT_APP_ENTRA_API_SCOPE
REACT_APP_API_BASE_URL   ← en AWS es la URL del Gateway; no commitear IPs
DB_USER / DB_PASSWORD           ← si se cambian respecto del example
DB_URL                          ← solo si no usas el default local de cada MS
MYSQL_* / RABBITMQ_DEFAULT_*    ← infra/.env (Compose); no versionar
MS_SOLICITUDES_URL / MS_CATALOGO_URL
```

Cómo compartir IDs: canal privado del equipo (Teams, WhatsApp, etc.), **no un commit ni un issue público**.

### 5.3 Dependencias y compilados

| Ruta | Por qué |
| --- | --- |
| `frontend/node_modules/` | Se regenera con `npm install` |
| `frontend/build/` | Salida de `npm run build` |
| `**/target/` | Salida de Maven (`mvn spring-boot:run` / `mvn package`) |
| `**/.mvn/wrapper/maven-wrapper.jar` | Binario del wrapper |

### 5.4 IDE, SO y scripts auxiliares

| Ruta | Por qué |
| --- | --- |
| `.idea/`, `*.iml`, `.vscode/` | Config personal del IDE |
| `.DS_Store` | macOS |
| `__pycache__/` | Python |
| `_draw_arquitectura.py` | Script local para generar el PNG del diagrama |

### 5.5 Cloud y máquina (no versionar aunque no estén en `.gitignore` todavía)

Crear estos archivos **fuera del repo** o confirmar que Git los ignore antes de `git add .`:

| Qué | Dónde suele aparecer | Uso |
| --- | --- | --- |
| Clave SSH de EC2 | `*.pem`, `*.ppk` | Nico: `ssh -i ...` |
| Credenciales AWS | `~/.aws/credentials` (nunca copiar al repo) | Consola / CLI de Ninna y Nico |
| Client secret de Entra | Portal Azure | Esta SPA es pública (MSAL); **no hace falta** secret en el front. Si alguien crea un secret de la API, no pegarlo en código |
| Password de MySQL en EC2 | `DB_PASSWORD` y `infra/.env` / systemd | Distinta de `mesatech` de local si se endurece |
| Capturas del informe con tokens visibles | Carpeta del Word/PDF, no Git | Redactar Bearer tokens en prints |

`.gitignore` del repo ya ignora `*.pem` y `*.ppk` para no subir la llave de EC2 por accidente.

### 5.6 Material de clases (opcional en tu disco)

No es código del producto. Si lo tienes, déjalo fuera de Git:

- Material duplicado fuera de `docs/` (la fuente PPT está en `docs/assets/fuentes/`).
- Docs de Google del profesor: enlaces en [`docs/guia/entra-spring-react.md`](../guia/entra-spring-react.md).

---

## 6. Setup local (sin `_refs`, con el código de este repo)

Requisitos: Java 21, Maven, Node.js (LTS), Docker.

1. Pedir a Ari (canal privado) tenant, Client ID SPA, Client ID API y scope.
2. Copiar plantillas:

   ```bash
   copy env.example .env
   copy frontend\.env.example frontend\.env
   ```

   En `frontend/.env` rellenar:

   ```properties
   REACT_APP_ENTRA_CLIENT_ID=<SPA_CLIENT_ID>
   REACT_APP_ENTRA_TENANT_ID=<TENANT_ID>
   REACT_APP_ENTRA_API_SCOPE=api://<API_CLIENT_ID>/access_as_user
   REACT_APP_API_BASE_URL=http://localhost:8080
   ```

3. Levantar MySQL 8.4 y RabbitMQ 4.0:

   ```bash
   cd infra
   cp .env.example .env
   docker compose up -d
   ```

   Crea las bases `catalogo` y `solicitudes`. RabbitMQ queda en marcha; los JAR todavía no se conectan.

4. Exportar variables (PowerShell) y arrancar los tres Spring Boot **en terminales distintas**:

   ```powershell
   $env:ENTRA_ISSUER_URI="https://login.microsoftonline.com/<TENANT_ID>/v2.0"
   $env:ENTRA_AUDIENCE="<API_CLIENT_ID>"

   cd bff; mvn spring-boot:run
   cd ms-solicitudes; mvn spring-boot:run
   cd ms-catalogo; mvn spring-boot:run
   ```

5. Front:

   ```bash
   cd frontend
   npm install
   npm start
   ```

   Abre `http://localhost:3000/`. El redirect URI de la SPA en Entra debe incluir exactamente esa URL.

En **AWS**, Skarlet/Ninna cambian `REACT_APP_API_BASE_URL` a la URL del HTTP API (Gateway). Llamar directo a `:8080`, `:8081` o `:8082` debe dar **401**.

---

## 7. Contratos que hay que conocer (aunque no estén en `_refs`)

Las referencias del profesor solo cubren login + `/api/usuario`. El dominio MesaTech está en este repo:

| Método | Ruta | Quién la implementa |
| --- | --- | --- |
| `GET` | `/public/hola` | BFF (sin JWT) |
| `GET` | `/api/usuario` | BFF (claims) |
| `POST` | `/v1/solicitudes` | BFF → `ms-solicitudes` |
| `GET` | `/v1/solicitudes/mias` | BFF → `ms-solicitudes` |
| `GET` | `/v1/solicitudes` | BFF → `ms-solicitudes` |
| `PATCH` | `/v1/solicitudes/{id}/estado` | BFF → `ms-solicitudes` |
| `GET` | `/v2/solicitudes/mias` | Igual datos que v1, envoltorio `{ version, usuario, solicitudes }` |
| `GET` | `/v1/catalogo` | BFF → `ms-catalogo` |
| `POST` | `/v1/catalogo/categorias` | BFF → `ms-catalogo` |
| `POST` | `/v1/catalogo/prioridades` | BFF → `ms-catalogo` |

Regla de negocio: no se puede pasar a **RESUELTA** si el estado no es **EN_PROCESO**.

Autorización por rol (cliente / operador / admin) es trabajo de Skarlet + Ari; el esqueleto actual valida JWT y scope, no la matriz completa.

---

## 8. Documentación del repo

Toda la documentación vive bajo **`docs/`**. Índice: [`docs/README.md`](../README.md).

| Ruta | Para qué |
| --- | --- |
| `README.md` (raíz) | Entrada al monorepo y enlaces a `docs/` |
| `docs/onboarding/refs-y-entorno.md` | Este archivo |
| `docs/caso/ep1-guia-oficial.md` | Guía oficial EP1 |
| `docs/contexto/anotaciones-profesor.md` | Instrucciones verbales y decisiones del equipo |
| `docs/guia/entra-spring-react.md` | Entra ID + Resource Server + MSAL (19 pasos) |
| `docs/guia/instructivo-maestro-ev1.md` | Hoja de ruta EV1 por fases A–G |
| `docs/arquitectura/diagrama.md` | Flujo React → Gateway → BFF → MS → MySQL |
| `docs/equipo/plan-trabajo.md` | Reparto Ari / Ninna / Nico / Skarlet |
| `docs/glosario/` y `docs/dominio/` | Términos cloud y negocio MesaTech |
| `env.example` y `frontend/.env.example` | Plantillas **sin** secretos |

---

## 9. Checklist al clonar (nuevo integrante)

- [ ] Cloné `duoc-mesatech` (sin `_refs`).
- [ ] Leí `README.md`, [`docs/README.md`](../README.md) y este archivo.
- [ ] Opcional: cloné `_refs/` para estudiar el ejemplo del profesor.
- [ ] Pedí IDs de Entra por canal privado (no los del profesor).
- [ ] Creé `frontend/.env` desde `.env.example`.
- [ ] `docker compose up -d` en `infra/`.
- [ ] Exporté `ENTRA_ISSUER_URI` y `ENTRA_AUDIENCE` antes de `mvn spring-boot:run`.
- [ ] Confirmé que `git status` no muestra `.env`, `_refs/`, `node_modules/`, `target/` ni `*.pem`.
- [ ] No hardcodeé IDs de Entra ni `localhost:8080` en el front de producción. Las claves de MySQL y RabbitMQ están solo en `.env` / `infra/.env`.
