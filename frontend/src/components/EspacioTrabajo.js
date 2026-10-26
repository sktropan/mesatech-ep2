import FormCrearSolicitud from "./FormCrearSolicitud";
import TablaSolicitudes from "./TablaSolicitudes";
import PanelCatalogo from "./PanelCatalogo";
import PanelCopiaCatalogo from "./PanelCopiaCatalogo";

export default function EspacioTrabajo({
  aviso,
  mensaje,
  vista,
  permisos,
  catalogo,
  mias,
  todas,
  ocupado,
  onCrear,
  onCambiarEstado,
  onTransicionInvalida,
  onCrearCategoria,
  onActualizarCategoria,
  onEliminarCategoria,
  onCrearPrioridad,
  onActualizarPrioridad,
  onEliminarPrioridad,
  copiaCatalogo,
  onActualizarCopia,
}) {
  return (
    <>
      {aviso && <div className="alert alert-info">{aviso}</div>}
      {mensaje && (
        <div
          className={`mt-feedback alert alert-${mensaje.tipo} fade show`}
          role="status"
          aria-live="polite"
        >
          {mensaje.texto}
        </div>
      )}

      {vista === "mias" && (
        <>
          <FormCrearSolicitud catalogo={catalogo} onCrear={onCrear} ocupado={ocupado} />
          <TablaSolicitudes
            titulo="Mis solicitudes"
            solicitudes={mias}
            vacio="No hay solicitudes asociadas a su cuenta."
            puedeCambiarEstado={permisos.cambiarEstado}
            onCambiarEstado={onCambiarEstado}
            onTransicionInvalida={onTransicionInvalida}
          />
        </>
      )}

      {vista === "todas" && permisos.verTodas && (
        <TablaSolicitudes
          titulo="Bandeja general de atención"
          solicitudes={todas}
          vacio="No existen solicitudes registradas en la bandeja."
          puedeCambiarEstado={permisos.cambiarEstado}
          onCambiarEstado={onCambiarEstado}
          onTransicionInvalida={onTransicionInvalida}
        />
      )}

      {vista === "copia" && (
        <PanelCopiaCatalogo
          categorias={copiaCatalogo}
          onActualizar={onActualizarCopia}
          ocupado={ocupado}
        />
      )}

      {vista === "catalogo" && permisos.catalogo && (
        <PanelCatalogo
          catalogo={catalogo}
          onCrearCategoria={onCrearCategoria}
          onActualizarCategoria={onActualizarCategoria}
          onEliminarCategoria={onEliminarCategoria}
          onCrearPrioridad={onCrearPrioridad}
          onActualizarPrioridad={onActualizarPrioridad}
          onEliminarPrioridad={onEliminarPrioridad}
          ocupado={ocupado}
        />
      )}
    </>
  );
}
