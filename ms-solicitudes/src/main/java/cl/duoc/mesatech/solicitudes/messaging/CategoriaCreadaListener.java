package cl.duoc.mesatech.solicitudes.messaging;

import cl.duoc.mesatech.solicitudes.config.RabbitConfig;
import cl.duoc.mesatech.solicitudes.service.CopiaCategoriaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

@Component
public class CategoriaCreadaListener {

    private static final Logger log = LoggerFactory.getLogger(CategoriaCreadaListener.class);

    private final CopiaCategoriaService copias;

    public CategoriaCreadaListener(CopiaCategoriaService copias) {
        this.copias = copias;
    }

    @RabbitListener(queues = RabbitConfig.QUEUE)
    public void recibir(CategoriaCreadaEvento evento) {
        if (evento == null || evento.id() == null) {
            log.warn("Se ignoró un evento categoria.creada sin id");
            return;
        }
        try {
            copias.guardarSiNoExiste(evento);
        } catch (DataIntegrityViolationException ex) {
            // Dos entregas a la vez: la restricción única de catalogo_id ya guardó la copia.
            log.info("Copia ya existente para catalogoId {}. Se confirma el duplicado.", evento.id());
        }
    }
}
