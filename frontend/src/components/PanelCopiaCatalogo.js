export default function PanelCopiaCatalogo({ categorias, onActualizar, ocupado }) {
  const lista = Array.isArray(categorias) ? categorias : [];

  return (
    <div className="mt-card">
      <div className="mt-card-body">
        <p className="mt-kicker">Réplica</p>
        <h2>Copia del catálogo</h2>
        <p className="text-muted">
          Esta lista vive en ms-solicitudes. Llega cuando catálogo publica el evento categoria.creada.
        </p>
        <button
          className="btn btn-outline-primary btn-sm mb-3"
          type="button"
          onClick={onActualizar}
          disabled={ocupado}
        >
          Actualizar
        </button>
        {lista.length === 0 ? (
          <div className="mt-empty">
            Aún no hay categorías copiadas. Si acaba de crear una, pulse Actualizar.
          </div>
        ) : (
          <ul className="list-group list-group-flush">
            {lista.map((item) => (
              <li className="list-group-item px-0" key={item.id ?? item.catalogoId}>
                <strong>{item.nombre}</strong>
                <span className="text-muted"> · id catálogo {item.catalogoId}</span>
              </li>
            ))}
          </ul>
        )}
      </div>
    </div>
  );
}
