import { useCallback, useEffect, useState } from "react";
import {
  AuthenticatedTemplate,
  UnauthenticatedTemplate,
  useMsal,
} from "@azure/msal-react";
import { loginRequest } from "./authConfig";
import { ApiError, apiDelete, apiGet, apiPatch, apiPost, apiPut } from "./api/http";
import { etiquetaRol, navDesdePermisos, permisosDesdeRoles } from "./roles";
import Layout from "./components/Layout";
import EspacioTrabajo from "./components/EspacioTrabajo";

function App() {
  const { instance, accounts } = useMsal();
  const [sesionApi, setSesionApi] = useState(null);
  const [catalogo, setCatalogo] = useState({ categorias: [], prioridades: [] });
  const [mias, setMias] = useState([]);
  const [todas, setTodas] = useState([]);
  const [copiaCatalogo, setCopiaCatalogo] = useState([]);
  const [vista, setVista] = useState("mias");
  const [mensaje, setMensaje] = useState(null);
  const [ocupado, setOcupado] = useState(false);

  const cuenta = accounts[0];
  const permisos = permisosDesdeRoles(sesionApi?.roles);

  const avisar = (texto, tipo = "danger") => {
    setMensaje({ texto, tipo });
    if (tipo === "success") {
      window.clearTimeout(window.__mtFeedbackTimer);
      window.__mtFeedbackTimer = window.setTimeout(() => setMensaje(null), 4500);
    }
  };

  const cargar = useCallback(async () => {
    if (!cuenta) {
      return;
    }
    try {
      const usuario = await apiGet(instance, cuenta, "/api/usuario");
      setSesionApi(usuario);

      const [miasData, catalogoData, copiaData] = await Promise.all([
        apiGet(instance, cuenta, "/v1/solicitudes/mias"),
        apiGet(instance, cuenta, "/v1/catalogo"),
        apiGet(instance, cuenta, "/v1/categorias-catalogo"),
      ]);
      setMias(Array.isArray(miasData) ? miasData : []);
      setCatalogo(catalogoData || { categorias: [], prioridades: [] });
      setCopiaCatalogo(Array.isArray(copiaData) ? copiaData : []);

      const puedeVerTodas = permisosDesdeRoles(usuario.roles).verTodas;
      if (puedeVerTodas) {
        try {
          const todasData = await apiGet(instance, cuenta, "/v1/solicitudes");
          setTodas(Array.isArray(todasData) ? todasData : []);
        } catch (err) {
          if (err instanceof ApiError && err.status === 403) {
            setTodas([]);
            avisar(err.message);
          } else {
            throw err;
          }
        }
      } else {
        setTodas([]);
      }
    } catch (err) {
      avisar(err.message || "No fue posible conectar con el servicio. Verifique la sesión y el backend.");
    }
  }, [cuenta, instance]);

  useEffect(() => {
    cargar();
  }, [cargar]);

  const iniciarSesion = () => {
    instance.loginRedirect(loginRequest).catch((err) => console.error(err));
  };

  const cerrarSesion = () => {
    instance.logoutRedirect();
  };

  const ejecutar = async (accion, ok) => {
    setOcupado(true);
    setMensaje(null);
    try {
      await accion();
      if (ok) {
        avisar(ok, "success");
      }
      await cargar();
    } catch (err) {
      avisar(err.message);
    } finally {
      setOcupado(false);
    }
  };

  return (
    <>
      <UnauthenticatedTemplate>
        <Layout>
          <div className="mt-card mt-login">
            <div className="mt-card-body">
              <img src={`${process.env.PUBLIC_URL}/logo.svg`} alt="" />
              <p className="mt-kicker">Acceso institucional</p>
              <h1>MesaTech Cloud</h1>
              <p className="text-muted">
                Identifíquese con la cuenta corporativa de Microsoft Entra ID
                para gestionar solicitudes de soporte.
              </p>
              <button className="btn btn-primary" onClick={iniciarSesion}>
                Iniciar sesión
              </button>
            </div>
          </div>
        </Layout>
      </UnauthenticatedTemplate>

      <AuthenticatedTemplate>
        <Layout
          usuario={{
            nombre: cuenta?.name || cuenta?.username,
            rol: etiquetaRol(sesionApi?.roles),
          }}
          onCerrarSesion={cerrarSesion}
          nav={navDesdePermisos(permisos)}
          vista={vista}
          onVista={setVista}
        >
          <EspacioTrabajo
            aviso={
              permisos.sinRolEnToken
                ? "El token no incluye app roles de Entra (Cliente, Operador o Administrador). Solicite la asignación al equipo de identidad."
                : null
            }
            mensaje={mensaje}
            vista={vista}
            permisos={permisos}
            catalogo={catalogo}
            mias={mias}
            todas={todas}
            ocupado={ocupado}
            onCrear={(body) =>
              ejecutar(
                () => apiPost(instance, cuenta, "/v1/solicitudes", body),
                "La solicitud fue registrada."
              )
            }
            onTransicionInvalida={(texto) => avisar(texto, "danger")}
            onCambiarEstado={(id, estado, mensajeOk) =>
              ejecutar(
                () => apiPatch(instance, cuenta, `/v1/solicitudes/${id}/estado`, { estado }),
                mensajeOk || `El estado se actualizó a ${estado.replaceAll("_", " ")}.`
              )
            }
            onCrearCategoria={(body) =>
              ejecutar(
                () => apiPost(instance, cuenta, "/v1/catalogo/categorias", body),
                "La categoría fue registrada."
              )
            }
            onCrearPrioridad={(body) =>
              ejecutar(
                () => apiPost(instance, cuenta, "/v1/catalogo/prioridades", body),
                "La prioridad fue registrada."
              )
            }
            onActualizarCategoria={(id, body) =>
              ejecutar(
                () => apiPut(instance, cuenta, `/v1/catalogo/categorias/${id}`, body),
                "La categoría fue actualizada."
              )
            }
            onEliminarCategoria={(id) =>
              ejecutar(
                () => apiDelete(instance, cuenta, `/v1/catalogo/categorias/${id}`),
                "La categoría fue eliminada."
              )
            }
            onActualizarPrioridad={(id, body) =>
              ejecutar(
                () => apiPut(instance, cuenta, `/v1/catalogo/prioridades/${id}`, body),
                "La prioridad fue actualizada."
              )
            }
            onEliminarPrioridad={(id) =>
              ejecutar(
                () => apiDelete(instance, cuenta, `/v1/catalogo/prioridades/${id}`),
                "La prioridad fue eliminada."
              )
            }
            copiaCatalogo={copiaCatalogo}
            onActualizarCopia={() => ejecutar(async () => {}, null)}
          />
        </Layout>
      </AuthenticatedTemplate>
    </>
  );
}

export default App;
