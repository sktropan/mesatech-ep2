package cl.duoc.mesatech.catalogo.config;

import cl.duoc.mesatech.catalogo.messaging.PublicadorCategoriaCreada;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.support.converter.DefaultJacksonJavaTypeMapper;
import org.springframework.amqp.support.converter.JacksonJavaTypeMapper;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * El publicador es dueño del exchange: lo declara al arrancar (idempotente si ya existe).
 * La cola y el binding los declara quien consume, y también el definitions.json del broker,
 * para que el mensaje no se pierda si este servicio arranca primero.
 */
@Configuration
public class RabbitConfig {

    @Bean
    DirectExchange catalogoExchange() {
        return new DirectExchange(PublicadorCategoriaCreada.EXCHANGE, true, false);
    }

    @Bean
    MessageConverter jsonMessageConverter() {
        JacksonJsonMessageConverter converter = new JacksonJsonMessageConverter();
        DefaultJacksonJavaTypeMapper mapper = new DefaultJacksonJavaTypeMapper();
        mapper.setTypePrecedence(JacksonJavaTypeMapper.TypePrecedence.INFERRED);
        mapper.addTrustedPackages("*");
        converter.setJavaTypeMapper(mapper);
        return converter;
    }
}
