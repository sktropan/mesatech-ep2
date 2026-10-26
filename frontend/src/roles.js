export const ESTADOS = [
  "CREADA",
  "ASIGNADA",
  "EN_PROCESO",
  "RESUELTA",
  "CERRADA",
  "CANCELADA",
];

function normalizar(rol) {
  const raw = String(rol || "").toLowerCase().trim();
  if (["administrador", "administrator", "admin"].includes(raw)) {
    return "admin";
  }
  if (["operador", "operator"].includes(raw)) {
    return "operador";
  }
  if (["cliente", "client"].includes(raw)) {
    return "cliente";
  }
  return raw;
}

export function permisosDesdeRoles(roles) {
  const lista = Array.isArray(roles) ? roles : roles ? [roles] : [];
  if (lista.length === 0) {
    return {
      verTodas: false,
      cambiarEstado: false,
      catalogo: false,
      sinRolEnToken: true,
    };
  }

  const set = new Set(lista.map(normalizar));
  const esAdmin = set.has("admin");
  const esOperador = esAdmin || set.has("operador");

  return {
    verTodas: esOperador,
    cambiarEstado: esOperador,
    catalogo: esAdmin,
    sinRolEnToken: false,
  };
}

export function etiquetaRol(roles) {
  const lista = Array.isArray(roles) ? roles : roles ? [roles] : [];
  if (lista.length === 0) {
    return "Perfil pendiente de asignación";
  }
  return lista.join(", ");
}

export function navDesdePermisos(permisos) {
  const items = [
    { id: "mias", label: "Mis solicitudes" },
    { id: "copia", label: "Copia catálogo" },
  ];
  if (permisos.verTodas) {
    items.push({ id: "todas", label: "Bandeja general" });
  }
  if (permisos.catalogo) {
    items.push({ id: "catalogo", label: "Catálogo" });
  }
  return items;
}
