# Rutas HTTP — React → API Gateway → BFF

Referencia para **Ninna** (HTTP API en AWS) y **Skarlet** (Axios). El front **no** arma URLs a microservicios: solo `${REACT_APP_API_BASE_URL}` + path (ver `frontend/src/authConfig.js` y `frontend/src/api/http.js`).

En **local**, `REACT_APP_API_BASE_URL=http://localhost:8080` (BFF directo).  
En **cloud**, la misma variable = **Invoke URL** del Gateway (sin barra final), con rutas proxy hacia el BFF EC2.

## Rutas que usa el React (MVP)

| Método | Path | Rol en BFF (app role Entra) | Origen en código |
| --- | --- | --- | --- |
| GET | `/api/usuario` | Token válido + scope | `App.js` |
| GET | `/v1/solicitudes/mias` | Cualquier autenticado | `App.js` |
| POST | `/v1/solicitudes` | Cualquier autenticado | `FormCrearSolicitud` → `App.js` |
| GET | `/v1/solicitudes` | **Operador** o **Administrador** → si no, **403** | `App.js` (bandeja general) |
| PATCH | `/v1/solicitudes/{id}/estado` | **Operador** o **Administrador** → si no, **403** | `TablaSolicitudes` |
| GET | `/v1/catalogo` | Cualquier autenticado (formulario) | `App.js` |
| GET | `/v1/categorias-catalogo` | Cualquier autenticado (copia que guardó ms-solicitudes) | `App.js` → menú Copia catálogo |
| POST | `/v1/catalogo/categorias` | **Administrador** → si no, **403** | `PanelCatalogo` |
| PUT | `/v1/catalogo/categorias/{id}` | **Administrador** → si no, **403** | `PanelCatalogo` |
| DELETE | `/v1/catalogo/categorias/{id}` | **Administrador** → si no, **403** | `PanelCatalogo` |
| POST | `/v1/catalogo/prioridades` | **Administrador** → si no, **403** | `PanelCatalogo` |
| PUT | `/v1/catalogo/prioridades/{id}` | **Administrador** → si no, **403** | `PanelCatalogo` |
| DELETE | `/v1/catalogo/prioridades/{id}` | **Administrador** → si no, **403** | `PanelCatalogo` |
| GET | `/public/hola` | Público (pruebas) | No usado en React |

Versionamiento: el BFF expone también `GET /v2/solicitudes/mias` (EP1); el front MVP usa **v1**.

## Gateway (Ninna)

Por cada fila anterior (excepto `/public/**` si se prueba directo al BFF):

1. Ruta HTTP API con el **mismo path** (ej. `GET /v1/solicitudes/mias`).
2. Integración **HTTP** hacia el BFF (`http://<bff-privado-o-público-según-SG>:8080`).
3. **JWT Authorizer** con issuer/audience de Entra (Ari documenta IDs fuera de Git).
4. **CORS**: origen del React (`http://localhost:3000` + hosting del front), headers `Authorization`, `Content-Type`, métodos `GET, POST, PUT, PATCH, DELETE, OPTIONS`.

### Rutas catálogo CRUD (Nico — crear en HTTP API si faltan)

Integración HTTP al BFF `:8080`, mismo JWT Authorizer que el resto de negocio:

| Método | Path |
| --- | --- |
| PUT | `/v1/catalogo/categorias/{id}` |
| DELETE | `/v1/catalogo/categorias/{id}` |
| PUT | `/v1/catalogo/prioridades/{id}` |
| DELETE | `/v1/catalogo/prioridades/{id}` |

### Ruta nueva EP2 — copia de categorías

| Método | Path | Integración | Quién |
| --- | --- | --- | --- |
| GET | `/v1/categorias-catalogo` | HTTP al BFF `:8080`, mismo path, JWT Authorizer | Cualquier autenticado. El BFF llama a `ms-solicitudes`. |

Sin esta ruta, el menú **Copia catálogo** del React devuelve **404** en la nube aunque el BFF responda bien en local. No hace falta rol extra en Gateway.

Path parameter `{id}` numérico (ID JPA). Sin las rutas de la tabla anterior, editar/eliminar desde React devuelve **404** en Gateway aunque BFF/MS estén actualizados.

CloudShell — script bash: **no** uses la variable `PATH` para el path de la ruta (pisa `$PATH` del sistema y `aws` deja de existir). Usa `ROUTE_PATH` (ver comentario en [`guias/ninna-gateway-marca-versiones.md`](guias/ninna-gateway-marca-versiones.md) si se documenta el snippet).

No hace falta duplicar reglas de rol en Gateway: el **BFF** devuelve **403** según claim `roles`.

## Entra ID (Ari)

App roles en la API (claim `roles` en el access token):

| Rol sugerido en Azure | Valor normalizado en BFF |
| --- | --- |
| Cliente | `cliente` |
| Operador | `operador` |
| Administrador | `admin` |

Asignar usuarios de prueba antes de demo §11 (403 cliente vs bandeja general). Guía: [`guias/ari-identidad-azure.md`](guias/ari-identidad-azure.md).
