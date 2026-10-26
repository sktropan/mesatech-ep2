package cl.duoc.mesatech.catalogo.web;

import cl.duoc.mesatech.catalogo.messaging.EventoNoConfirmadoException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Cuerpo propio: Spring Boot no incluye {@code message} en el JSON de error por defecto.
 * El BFF reenvía este status y este cuerpo, y el React lee {@code data.message}.
 */
@RestControllerAdvice
public class ErrorBrokerHandler {

    @ExceptionHandler(EventoNoConfirmadoException.class)
    public ResponseEntity<Map<String, String>> brokerNoConfirmo(EventoNoConfirmadoException ex) {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("error", "Servicio no disponible");
        body.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(body);
    }
}
