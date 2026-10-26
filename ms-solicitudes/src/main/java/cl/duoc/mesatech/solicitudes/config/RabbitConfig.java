package cl.duoc.mesatech.solicitudes.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.support.converter.DefaultJacksonJavaTypeMapper;
import org.springframework.amqp.support.converter.JacksonJavaTypeMapper;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * El consumidor declara la cola, el binding y otra vez el exchange.
 * Así puede arrancar aunque ms-catalogo todavía no exista: declarar dos veces
 * con los mismos argumentos es válido en RabbitMQ.
 * El exchange sigue siendo el contrato del publicador; la cola es de este servicio.
 * definitions.json repite la misma topología para que el broker ya nazca cableado.
 */
@Configuration
public class RabbitConfig {

    public static final String EXCHANGE = "catalogo.exchange";
    public static final String QUEUE = "solicitudes.catalogo.queue";
    public static final String ROUTING_KEY = "categoria.creada";

    @Bean
    DirectExchange catalogoExchange() {
        return new DirectExchange(EXCHANGE, true, false);
    }

    @Bean
    Queue solicitudesCatalogoQueue() {
        return QueueBuilder.durable(QUEUE).build();
    }

    @Bean
    Binding categoriaCreadaBinding(Queue solicitudesCatalogoQueue, DirectExchange catalogoExchange) {
        return BindingBuilder.bind(solicitudesCatalogoQueue).to(catalogoExchange).with(ROUTING_KEY);
    }

    @Bean
    MessageConverter jsonMessageConverter() {
        JacksonJsonMessageConverter converter = new JacksonJsonMessageConverter();
        DefaultJacksonJavaTypeMapper mapper = new DefaultJacksonJavaTypeMapper();
        // El header __TypeId__ apunta a la clase de ms-catalogo, que aquí no existe.
        // Manda el tipo del método del listener.
        mapper.setTypePrecedence(JacksonJavaTypeMapper.TypePrecedence.INFERRED);
        mapper.addTrustedPackages("*");
        converter.setJavaTypeMapper(mapper);
        return converter;
    }
}
