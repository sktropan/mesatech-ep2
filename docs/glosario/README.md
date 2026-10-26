# Glosario — conceptos cloud y seguridad (EV1)

Términos que aparecen en la guía EP1, el informe y la arquitectura MesaTech. Para **negocio** (estados, roles, rutas) ver [`../dominio/README.md`](../dominio/README.md).

---

## Identidad y tokens

| Término | Definición breve | Dónde se usa en MesaTech |
| --- | --- | --- |
| **IDaaS** | Identity as a Service: proveedor que autentica usuarios y emite tokens. | **Microsoft Entra ID** (Azure). |
| **Microsoft Entra ID** | Servicio de identidad de Microsoft (antes Azure AD). | Login del React; emisor del JWT. |
| **App Registration** | Aplicación registrada en Entra (SPA o API). | SPA React + API `api-cloud-native`. |
| **SPA** | Single Page Application: front que corre en el navegador. | `frontend/` con MSAL. |
| **MSAL** | Microsoft Authentication Library. | `@azure/msal-react` en React. |
| **OAuth 2.0** | Marco de autorización (flujos, scopes, tokens). | MSAL obtiene tokens; API usa Bearer. |
| **OpenID Connect (OIDC)** | Capa de identidad sobre OAuth (id_token, claims estándar). | Claims como `oid`, `preferred_username`. |
| **JWT** | JSON Web Token firmado: header + payload + firma. | Access Token hacia Gateway y BFF. |
| **Access Token** | Token para invocar APIs (no confundir con id_token de perfil). | Header `Authorization: Bearer …`. |
| **Issuer (`iss`)** | Quién emitió el token. | `https://login.microsoftonline.com/<tenant>/v2.0` |
| **Audience (`aud`)** | Para qué API está destinado el token. | Client ID de `api-cloud-native`. |
| **Scope** | Permiso delegado que el cliente pide al login. | `api://<API_ID>/access_as_user` |
| **Claim** | Par clave-valor dentro del JWT. | `roles`, `scp`, `oid`, nombre. |
| **Resource Server** | API que valida JWT (issuer, audience, firma, exp). | BFF y microservicios Spring. |
| **Consentimiento de administrador** | Aprobación del tenant para que la SPA use el scope de la API. | Portal Azure, permisos API. |
| **Token v2** | Formato de access token de Entra (manifiesto `requestedAccessTokenVersion: 2`). | Obligatorio en la PPT del curso. |

---

## AWS y red

| Término | Definición breve | MesaTech |
| --- | --- | --- |
| **API Gateway (HTTP API)** | Entrada HTTP administrada en AWS; rutas, CORS, authorizers. | **Único** entrypoint del front en producción. |
| **JWT Authorizer** | Valida JWT en el Gateway antes de reenviar al backend. | Issuer + audience de Entra (Ari → Ninna). |
| **Integración HTTP** | Gateway reenvía la petición a una URL backend (BFF EC2). | `:8080` en instancia privada/pública acotada. |
| **CORS** | Reglas del navegador: qué orígenes pueden llamar la API. | Gateway + BFF; origen del React. |
| **EC2** | Máquina virtual en AWS. | BFF, MS, Docker MySQL. |
| **Security Group (SG)** | Firewall de instancia (puertos/origen). | BFF solo desde Gateway; MS solo desde BFF. |
| **401 Unauthorized** | Sin autenticación válida. | Gateway sin token; acceso directo a BFF/MS en entrega. |
| **403 Forbidden** | Autenticado pero sin permiso (rol/autorización). | Matriz cliente/operador/admin (Skarlet). |

---

## Backend y datos

| Término | Definición breve | MesaTech |
| --- | --- | --- |
| **BFF** | Backend for Frontend: capa que adapta APIs al front. | `bff/` — valida JWT, no tiene BD. |
| **Microservicio** | Servicio de dominio independiente con su persistencia. | `ms-solicitudes`, `ms-catalogo`. |
| **Versionamiento de API** | Convivencia de `/v1` y `/v2` con contratos distintos. | v2 envoltorio en `GET …/mias`. |
| **MySQL 8.4** | Motor relacional. Hibernate detecta el dialecto por la URL JDBC; no hay dialecto fijo en `application.properties`. | Docker `infra/`; bases `catalogo` y `solicitudes`. |
| **RabbitMQ 4.0** | Broker de mensajes. | Contenedor `rabbitmq:4.0-management` en `infra/`. El código Java todavía no publica ni consume. |
| **Docker Compose** | Orquestación local de contenedores. | MySQL y RabbitMQ en local y en EC2 (Nico). |

---

## Frontend

| Término | Definición breve | MesaTech |
| --- | --- | --- |
| **CRA** | Create React App. | Base del `frontend/`. |
| **Redirect URI** | URL a la que Entra devuelve al usuario tras login. | `http://localhost:3000/` en dev. |
| **acquireTokenSilent** | MSAL renueva el access token sin popup si hay sesión. | Antes de cada llamada Axios. |
| **REACT_APP_API_BASE_URL** | Base URL de la API en el front. | Local: BFF; AWS: URL del Gateway. |

---

## Evaluación

| Término | Definición breve |
| --- | --- |
| **E2E** | Flujo completo usuario → Entra → Gateway → BFF → MS → UI (§10 EP1). |
| **Evidencia** | Capturas/Postman sin secretos; tokens redactados en informe. |
| **Checklist §15** | 15 ítems obligatorios antes de entregar (ver guía oficial). |

---

## Relaciones (diagrama mental)

```text
Usuario → MSAL → Entra ID → Access Token (aud = API, scp = access_as_user)
Usuario → React → API Gateway [JWT Authorizer] → BFF [Resource Server] → MS → PostgreSQL
```

Más detalle visual: [`../arquitectura/diagrama.md`](../arquitectura/diagrama.md).
