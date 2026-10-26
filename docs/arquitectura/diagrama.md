# Diagrama de arquitectura — MesaTech Cloud EP1

Arquitectura de la solución que vamos a entregar, alineada a la guía, a las anotaciones del profesor y al **monorepo ya creado**.

**IDaaS:** Microsoft Entra ID (Azure) · **Front:** React + MSAL + Axios · **Entrada:** AWS API Gateway · **Orquestación:** BFF Spring Boot (Java 21) · **Dominio:** `ms-solicitudes` + `ms-catalogo` · **Datos:** MySQL 8.4 en Docker (bases `solicitudes` y `catalogo`). Al crear una categoría, `ms-catalogo` publica `categoria.creada` en RabbitMQ y `ms-solicitudes` guarda una copia.

```text
React + MSAL  →  Entra ID (login / JWT)
React + MSAL  →  API Gateway  →  BFF :8080  →  ms-solicitudes :8081  →  MySQL (solicitudes)
                                         └→  ms-catalogo    :8082  →  MySQL (catalogo)
ms-catalogo  -- categoria.creada -->  catalogo.exchange  -->  solicitudes.catalogo.queue  -->  ms-solicitudes
```

**Regla de seguridad:** React solo habla con API Gateway (en AWS) o, en local, con el BFF. Un acceso directo a `:8080`, `:8081` o `:8082` sin JWT válido responde **401**.

![Arquitectura propuesta MesaTech Cloud](../assets/diagramas/arquitectura_mesatech_propuesta.png)

---

## Vista de componentes

```mermaid
flowchart LR
    subgraph Actores
        C[Cliente]
        O[Operador]
        A[Administrador]
    end

    subgraph Frontend["frontend/ · React + MSAL"]
        R[SPA<br/>loginRedirect / logoutRedirect<br/>acquireTokenSilent + Axios]
    end

    subgraph Identidad["Microsoft Entra ID"]
        ENTRA[App registrations<br/>SPA React + api-cloud-native<br/>scope access_as_user · token v2]
    end

    subgraph AWS["AWS Cloud"]
        GW[API Gateway HTTP API<br/>JWT Authorizer · CORS<br/>/v1 y /v2]
        BFF[bff/ · EC2 :8080<br/>Resource Server<br/>Sin JPA / sin BD]
        MS1[ms-solicitudes/ · :8081]
        MS2[ms-catalogo/ · :8082]
    end

    subgraph Datos["infra/ · Docker en EC2"]
        DB1[(MySQL 8.4<br/>solicitudes)]
        DB2[(MySQL 8.4<br/>catalogo)]
    end

    C --> R
    O --> R
    A --> R
    R -- "1. Login" --> ENTRA
    ENTRA -- "2. ID + Access Token" --> R
    R -- "3. Bearer" --> GW
    R -. "PROHIBIDO · directo EC2" .-> BFF
    GW -- "4. Si JWT ok" --> BFF
    BFF -- "5." --> MS1
    BFF -- "5." --> MS2
    MS1 -- "6-7." --> DB1
    MS2 -- "6-7." --> DB2
    BFF -- "8-10. Respuesta" --> GW
    GW --> R
```

---

## Mapeo al repositorio

| Caja del diagrama | Carpeta | Puerto | Persistencia |
| --- | --- | --- | --- |
| React + MSAL | `frontend/` | 3000 | No |
| Entra ID | Portal Azure (no es código) | — | Usuarios / apps del tenant |
| API Gateway | Consola AWS (HTTP API) | 443 | No |
| BFF | `bff/` | **8080** | **No** (solo reenvía) |
| Microservicio solicitudes | `ms-solicitudes/` | **8081** | `solicitudes` |
| Microservicio catálogo | `ms-catalogo/` | **8082** | `catalogo` |
| MySQL 8.4 Docker | `infra/docker-compose.yml` | 3306 | Las dos bases |
| RabbitMQ 4.0 Docker | `infra/docker-compose.yml` | 5672 / 15672 | Vhost `mesatech`. Exchange `catalogo.exchange`, cola `solicitudes.catalogo.queue` |

En **local**, `REACT_APP_API_BASE_URL=http://localhost:8080` (el BFF).  
En **AWS**, esa variable es la URL de **API Gateway**. Nunca la IP de un microservicio.

---

## Flujo extremo a extremo

```mermaid
sequenceDiagram
    actor Usuario
    participant React as frontend/ React+MSAL
    participant Entra as Microsoft Entra ID
    participant APIGW as API Gateway
    participant BFF as bff :8080
    participant MS as ms-solicitudes :8081 / ms-catalogo :8082
    participant DB as PostgreSQL Docker

    Usuario->>React: Abre la SPA
    React->>Usuario: UnauthenticatedTemplate
    Usuario->>React: Iniciar sesión
    React->>Entra: loginRedirect (openid profile email)
    Entra->>Usuario: Autentica en el tenant
    Entra->>React: ID Token + Access Token (scp=access_as_user)
    React->>React: acquireTokenSilent
    React->>APIGW: Axios + Authorization Bearer
    APIGW->>APIGW: JWT Authorizer + CORS
    alt JWT inválido o ausente
        APIGW-->>React: 401
    else JWT válido
        APIGW->>BFF: Reenvía a :8080
        BFF->>BFF: Resource Server (iss, aud, firma, exp, SCOPE_access_as_user)
        BFF->>MS: Reenvía Authorization
        MS->>DB: JPA
        DB-->>MS: Datos
        MS-->>BFF: JSON
        BFF-->>APIGW: JSON
        APIGW-->>React: JSON
        React-->>Usuario: Sesión, claims y solicitudes
    end
```

Acceso directo a BFF o microservicio (sin Gateway, sin JWT) → **401**.

---

## APIs que expone el BFF (y el Gateway)

| Método | Ruta | Destino interno |
| --- | --- | --- |
| `GET` | `/public/hola` | BFF (sin JWT) |
| `GET` | `/api/usuario` | BFF (lee claims del JWT) |
| `POST` | `/v1/solicitudes` | `ms-solicitudes` |
| `GET` | `/v1/solicitudes/mias` | `ms-solicitudes` |
| `GET` | `/v1/solicitudes` | `ms-solicitudes` |
| `PATCH` | `/v1/solicitudes/{id}/estado` | `ms-solicitudes` |
| `GET` | `/v2/solicitudes/mias` | `ms-solicitudes` (contrato v2) |
| `GET` | `/v1/catalogo` | `ms-catalogo` |
| `POST` | `/v1/catalogo/categorias` | `ms-catalogo` |
| `PUT` | `/v1/catalogo/categorias/{id}` | `ms-catalogo` |
| `DELETE` | `/v1/catalogo/categorias/{id}` | `ms-catalogo` |
| `POST` | `/v1/catalogo/prioridades` | `ms-catalogo` |
| `PUT` | `/v1/catalogo/prioridades/{id}` | `ms-catalogo` |
| `DELETE` | `/v1/catalogo/prioridades/{id}` | `ms-catalogo` |

Regla de negocio: no se puede pasar a **RESUELTA** si el estado no es **EN_PROCESO**.

---

## Acceso permitido vs bloqueado

```mermaid
flowchart TD
    R[React]
    GW[API Gateway]
    BFF[BFF :8080]
    MS1[ms-solicitudes :8081]
    MS2[ms-catalogo :8082]

    R -->|JWT válido| GW
    GW -->|Authorizer OK| BFF
    BFF --> MS1
    BFF --> MS2

    R -->|sin token / JWT malo| GW
    GW -->|401| R

    X[Llamada directa a EC2] -->|sin JWT| BFF
    BFF -->|401| X
    X -->|sin JWT| MS1
    MS1 -->|401| X
```

| Intento | Resultado |
| --- | --- |
| React → Gateway con JWT y `access_as_user` | 2xx |
| React → Gateway sin token o JWT vencido | **401** en el Gateway |
| Browser o curl a `:8080` / `:8081` / `:8082` sin Bearer | **401** |
| Front apuntando a un microservicio | No permitido |

---

## Qué hay en cada caja

| Zona | Componente | Qué hace |
| --- | --- | --- |
| Navegador | `frontend/` React + MSAL | Login/logout Entra ID, muestra nombre/correo/`oid`, Axios solo a `REACT_APP_API_BASE_URL`. |
| Azure | Microsoft Entra ID | Dos App Registrations (SPA + `api-cloud-native`), scope `access_as_user`, access token v2. |
| AWS | API Gateway HTTP API | Único entrypoint público. CORS, JWT Authorizer, rutas `/v1` y `/v2`. |
| EC2 | `bff/` :8080 | Segunda validación JWT. Orquesta. **Sin base de datos.** |
| EC2 | `ms-solicitudes/` :8081 | CRUD solicitudes + flujo de estados. |
| EC2 | `ms-catalogo/` :8082 | Categorías y prioridades. |
| EC2 | `infra/` PostgreSQL 16 Docker | Dos bases. Solo los microservicios escriben. Alternativa: Aurora. |

---

## Identidad (Entra ID)

| Registro | Uso en el código |
| --- | --- |
| SPA React | `REACT_APP_ENTRA_CLIENT_ID` / MSAL `clientId` |
| `api-cloud-native` | `ENTRA_AUDIENCE` en BFF y microservicios; scope `api://<API_CLIENT_ID>/access_as_user` |
| Tenant | `ENTRA_ISSUER_URI` = `https://login.microsoftonline.com/<TENANT>/v2.0` |

Spring valida **iss** (tenant) y **aud** (API). El claim `scp=access_as_user` se vuelve `SCOPE_access_as_user`.

---

## Adaptaciones respecto del diagrama oficial

| Diagrama de la guía | Esta solución |
| --- | --- |
| Angular + MSAL | **React + MSAL + Axios** (`frontend/`) |
| Microsoft Entra ID | **Microsoft Entra ID** (Cognito queda descartado) |
| Oracle Autonomous Database | **MySQL 8.4 Docker** (`infra/`). La entrega EV1 usó PostgreSQL 16 |
| Pedidos / productos | **solicitudes** y **catálogo de soporte** |
| Un backend genérico | BFF `:8080` + dos MS `:8081` / `:8082` |

La topología de la guía se mantiene: identidad aparte, Gateway al frente, BFF en el medio, microservicios y persistencia detrás.

---

## Despliegue EV1 — 3 EC2 (equipo, us-east-1)

Registro de la entrega EV1 (el motor de esa entrega fue PostgreSQL 16). En EP2 el motor es MySQL 8.4 en el puerto 3306; ver el mapeo de arriba.

Topología elegida: **una EC2 por capa** (DB, MS, BFF). Región **us-east-1** (Learner Lab).

| Recurso | Name | IP pública | IP privada | Puertos |
| --- | --- | --- | --- | --- |
| PostgreSQL Docker | `mesatech-ev1-db` | `54.90.194.247` | `172.31.25.66` | 5432 (solo MS-SG) |
| Microservicios | `mesatech-ev1-ms` | `54.227.0.33` | `172.31.24.74` | 8081, 8082 (solo BFF-SG) |
| BFF | `mesatech-ev1-bff` | **`54.90.110.67`** | `172.31.20.75` | **8080** (integración Gateway) |

**Ninna (API Gateway):** integración HTTP hacia `http://54.90.110.67:8080`.  
**Invoke URL (front):** `REACT_APP_API_BASE_URL` = `https://7gqw4633sg.execute-api.us-east-1.amazonaws.com`.

```mermaid
flowchart LR
    subgraph Azure
        ENTRA[Entra ID]
    end
    subgraph Cliente
        R[React + MSAL]
    end
    subgraph AWS["AWS us-east-1"]
        GW[API Gateway<br/>7gqw4633sg.execute-api]
        BFF[EC2 BFF<br/>54.90.110.67:8080]
        MS[EC2 MS<br/>172.31.24.74<br/>8081 / 8082]
        DB[EC2 DB<br/>172.31.25.66<br/>Postgres Docker]
    end
    R --> ENTRA
    R -->|Bearer| GW
    GW --> BFF
    BFF --> MS
    MS --> DB
```

![Despliegue EV1 — 3 EC2](../assets/diagramas/arquitectura_mesatech_ev1_despliegue.png)

Regenerar PNG (Windows, con Pillow):

```bash
python infra/scripts/draw-despliegue-ev1.py
```
