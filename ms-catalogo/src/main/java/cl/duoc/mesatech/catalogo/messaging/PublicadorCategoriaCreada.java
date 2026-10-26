package cl.duoc.mesatech.catalogo.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Component
public class PublicadorCategoriaCreada {

    public static final String EXCHANGE = "catalogo.exchange";
    public static final String ROUTING_KEY = "categoria.creada";

    static final String NO_DISPONIBLE =
            "RabbitMQ no está disponible. La categoría no se guardó. Intente de nuevo cuando el broker responda.";
    static final String SIN_CONFIRMACION =
            "RabbitMQ no confirmó a tiempo el evento categoria.creada. La categoría no se guardó.";
    static final String RECHAZADO =
            "RabbitMQ rechazó el evento categoria.creada. La categoría no se guardó.";
    static final String DEVUELTO =
            "RabbitMQ devolvió el evento categoria.creada porque ninguna cola lo recibió. La categoría no se guardó.";

    private static final Logger log = LoggerFactory.getLogger(PublicadorCategoriaCreada.class);
    private static final long ESPERA_SEGUNDOS = 8;

    private final RabbitTemplate rabbitTemplate;

    public PublicadorCategoriaCreada(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publicar(CategoriaCreadaEvento evento) {
        CorrelationData correlation = new CorrelationData(UUID.randomUUID().toString());
        try {
            rabbitTemplate.convertAndSend(EXCHANGE, ROUTING_KEY, evento, correlation);
            CorrelationData.Confirm confirm = correlation.getFuture().get(ESPERA_SEGUNDOS, TimeUnit.SECONDS);
            if (confirm == null || !confirm.isAck()) {
                throw new EventoNoConfirmadoException(RECHAZADO);
            }
            // basic.return llega antes que basic.ack: al completar el future, getReturned() ya está puesto.
            if (correlation.getReturned() != null) {
                throw new EventoNoConfirmadoException(DEVUELTO);
            }
        } catch (EventoNoConfirmadoException ex) {
            log.warn("Publicación no confirmada: {}", ex.getMessage());
            throw ex;
        } catch (TimeoutException ex) {
            log.warn("Timeout esperando confirmación de categoria.creada", ex);
            throw new EventoNoConfirmadoException(SIN_CONFIRMACION, ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new EventoNoConfirmadoException(SIN_CONFIRMACION, ex);
        } catch (ExecutionException ex) {
            log.warn("El broker no completó la confirmación", ex);
            throw new EventoNoConfirmadoException(NO_DISPONIBLE, ex);
        } catch (AmqpException | IllegalStateException ex) {
            log.warn("No se pudo publicar categoria.creada", ex);
            throw new EventoNoConfirmadoException(NO_DISPONIBLE, ex);
        }
    }
}
