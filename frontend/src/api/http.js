import Axios from "axios";
import { apiBaseUrl, apiRequest } from "../authConfig";

export class ApiError extends Error {
  constructor(status, message) {
    super(message);
    this.status = status;
  }
}

function mensajeDesdeError(err) {
  const status = err.response?.status;
  const data = err.response?.data;

  if (status === 403) {
    return "No tienes permiso para esta acción.";
  }
  if (status === 401) {
    return "Sesión no válida. Inicie sesión de nuevo.";
  }
  if (status === 409 && data?.message) {
    return data.message;
  }
  if (status === 503) {
    return data?.message || "El servicio de mensajería no está disponible. Intente de nuevo.";
  }

  if (typeof data === "string" && data.trim()) {
    return data;
  }
  if (data?.message) {
    return data.message;
  }
  if (data?.detail) {
    return data.detail;
  }
  if (data?.error) {
    return data.error;
  }
  return err.message || "No se pudo consumir la API.";
}

async function conToken(instance, account) {
  const tokenResponse = await instance.acquireTokenSilent({
    ...apiRequest,
    account,
  });
  return {
    Authorization: `Bearer ${tokenResponse.accessToken}`,
  };
}

async function ejecutar(peticion) {
  try {
    const response = await peticion;
    return response.data;
  } catch (err) {
    throw new ApiError(err.response?.status, mensajeDesdeError(err));
  }
}

export async function apiGet(instance, account, path) {
  const headers = await conToken(instance, account);
  return ejecutar(Axios.get(`${apiBaseUrl}${path}`, { headers }));
}

export async function apiPost(instance, account, path, body) {
  const headers = await conToken(instance, account);
  return ejecutar(Axios.post(`${apiBaseUrl}${path}`, body, { headers }));
}

export async function apiPatch(instance, account, path, body) {
  const headers = await conToken(instance, account);
  return ejecutar(Axios.patch(`${apiBaseUrl}${path}`, body, { headers }));
}

export async function apiPut(instance, account, path, body) {
  const headers = await conToken(instance, account);
  return ejecutar(Axios.put(`${apiBaseUrl}${path}`, body, { headers }));
}

export async function apiDelete(instance, account, path) {
  const headers = await conToken(instance, account);
  return ejecutar(Axios.delete(`${apiBaseUrl}${path}`, { headers }));
}
