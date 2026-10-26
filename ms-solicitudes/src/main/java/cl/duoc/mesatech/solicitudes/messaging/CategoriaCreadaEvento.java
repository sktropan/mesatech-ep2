package cl.duoc.mesatech.solicitudes.messaging;

/**
 * Misma forma JSON que publica ms-catalogo: {@code {"id","nombre"}}.
 * No compartimos el jar: cada servicio tiene su copia del contrato.
 */
public record CategoriaCreadaEvento(Long id, String nombre) {
}
