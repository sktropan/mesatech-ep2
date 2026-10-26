package cl.duoc.mesatech.catalogo.messaging;

/**
 * Contrato JSON publicado en {@code catalogo.exchange} con routing key {@code categoria.creada}.
 * ms-solicitudes tiene su propia copia de este registro: el cuerpo es {@code {"id","nombre"}}.
 */
public record CategoriaCreadaEvento(Long id, String nombre) {
}
