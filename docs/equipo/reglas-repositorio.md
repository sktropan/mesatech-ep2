# Reglas del repositorio — MesaTech EV1

**Documento oficial del equipo.** Aplica a Ari, Ninna, Nico y Skarlet.

La misma normativa está en [`.cursor/rules/`](../../.cursor/rules/) para quien use un IDE que lea esas reglas. Si hay duda, **prevalece este archivo** en `docs/`.

| Tema ampliado | Documento |
| --- | --- |
| Branching, PRs, tag entrega | [`estrategia-branching.md`](estrategia-branching.md) |
| Código y carpetas | [`convenciones-desarrollo.md`](convenciones-desarrollo.md) |
| Tareas por persona | [`plan-trabajo.md`](plan-trabajo.md) + [`guias/README.md`](guias/README.md) |

---

## 1. Git — ramas

| Rama | Uso |
| --- | --- |
| **`main`** | Código integrado; solo entra por **Pull Request** |
| **`feature/<integrante>-<tema>`** | Trabajo diario |
| **`release/ev1`** | Opcional: cierre pre-entrega |
| **`hotfix/<tema>`** | Arreglo urgente desde `main` |

Ramas feature principales:

| Integrante | Ejemplo |
| --- | --- |
| Ari | `feature/ari-entra` |
| Ninna | `feature/ninna-gateway`, `feature/ninna-marca` |
| Nico | `feature/nico-ec2` |
| Skarlet | `feature/skarlet-negocio` |

**Orden de merge recomendado:** Ari → Skarlet/Nico (paralelo) → Ninna → integración E2E → tag `ev1.0.0` en `main`.

**Prohibido commitear:** `.env`, `.pem`, `target/`, `node_modules/`, tokens JWT, Client IDs del profesor.

---

## 2. Git — mensajes de commit (obligatorio)

Formato:

```text
[ TIPO ]: Detalle del commit
```

| Regla | Detalle |
| --- | --- |
| Idioma | Español |
| `TIPO` | Mayúsculas entre corchetes |
| Cuerpo | Una idea por commit; claro y concreto |

| TIPO | Uso |
| --- | --- |
| `FEAT` | Funcionalidad nueva (React, BFF, MS) |
| `FIX` | Corrección de bug |
| `DOCS` | Documentación |
| `CHORE` | Mantenimiento del repo |
| `REFACTOR` | Refactor sin cambio funcional |
| `TEST` | Pruebas |

Ejemplos:

```text
[ FEAT ]: Rechazar GET /v1/solicitudes para rol cliente en BFF
[ DOCS ]: Guía de capturas para informe EP1
[ FIX ]: CORS PATCH en SecurityConfig del BFF
```

---

## 3. Git — Pull Requests

- Destino: **`main`**.
- Al menos **1 aprobación** de un integrante distinto al autor.
- Tamaño preferido: **&lt; 400 líneas** por PR.
- Marcar bloque EV1 (A, B, C, … P) en la descripción.
- Checklist PR: sin secretos; probar local si tocó código.

| Cambios en | Revisor sugerido |
| --- | --- |
| `bff/`, JWT, roles | Ari o Skarlet |
| `infra/`, EC2 | Nico |
| Gateway, CORS (docs/config) | Ninna |
| `frontend/` | Skarlet |

Plantilla y flujo detallado: [`estrategia-branching.md`](estrategia-branching.md) §6.

---

## 4. Reglas del producto (EV1)

- **Identidad:** Microsoft Entra ID; scope `access_as_user`; audience = API `api-cloud-native`.
- **Entrada pública:** AWS API Gateway; en entrega React **no** usa IP:8080 directa.
- **401:** acceso directo a BFF/MS desde internet debe fallar o estar bloqueado (SG).
- **BFF:** OAuth2 Resource Server; **sin** JPA ni BD.
- **Persistencia:** solo `ms-solicitudes` y `ms-catalogo` → PostgreSQL 16 (`infra/`).
- **Autorización:** roles cliente / operador / admin → **403** cuando no corresponda.
- **Documentación:** nueva contenido en `docs/`; en raíz solo `README.md`.

---

## 5. Dónde implementar

| Cambio | Carpeta |
| --- | --- |
| Login, UI, API desde React | `frontend/` |
| JWT, proxy, roles | `bff/` |
| Solicitudes y estados | `ms-solicitudes/` |
| Catálogo | `ms-catalogo/` |
| MySQL 8.4 y RabbitMQ (Docker) | `infra/` |

---

## 6. Reglas en `.cursor/rules/` (opcional)

Archivos `.mdc` en el repo (misma normativa que §1–5):

| Archivo | Contenido |
| --- | --- |
| [`.cursor/rules/mesatech-git-workflow.mdc`](../../.cursor/rules/mesatech-git-workflow.mdc) | Git, commits, PR |
| [`.cursor/rules/mesatech-proyecto-ev1.mdc`](../../.cursor/rules/mesatech-proyecto-ev1.mdc) | Producto EV1 y carpetas |

Esos archivos **no sustituyen** este documento; deben mantenerse alineados cuando el equipo cambie un acuerdo.
