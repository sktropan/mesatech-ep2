package cl.duoc.mesatech.catalogo.messaging;

/**
 * El broker no confirmó el evento, lo devolvió o no estaba disponible.
 * Es unchecked para que la transacción de la categoría haga rollback.
 */
public class EventoNoConfirmadoException extends RuntimeException {

    public EventoNoConfirmadoException(String message) {
        super(message);
    }

    public EventoNoConfirmadoException(String message, Throwable cause) {
        super(message, cause);
    }
}
