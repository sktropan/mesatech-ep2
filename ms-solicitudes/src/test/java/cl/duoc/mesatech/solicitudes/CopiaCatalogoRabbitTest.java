package cl.duoc.mesatech.solicitudes;

import cl.duoc.mesatech.solicitudes.repo.CategoriaCatalogoRepository;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.DefaultJacksonJavaTypeMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.Container;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.rabbitmq.RabbitMQContainer;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.hamcrest.Matchers.contains;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CopiaCatalogoRabbitTest {

    static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.4");
    static final RabbitMQContainer RABBIT = new RabbitMQContainer("rabbitmq:4.0-management");
    static final AtomicBoolean BROKER_LISTO = new AtomicBoolean(false);

    @Autowired
    MockMvc mvc;

    @Autowired
    CategoriaCatalogoRepository copias;

    @DynamicPropertySource
    static void propiedades(DynamicPropertyRegistry registry) throws Exception {
        MYSQL.start();
        RABBIT.start();
        if (BROKER_LISTO.compareAndSet(false, true)) {
            prepararBroker();
        }
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.rabbitmq.host", RABBIT::getHost);
        registry.add("spring.rabbitmq.port", () -> Integer.toString(RABBIT.getAmqpPort()));
        registry.add("spring.rabbitmq.username", () -> "solicitudes");
        registry.add("spring.rabbitmq.password", () -> "solicitudes-dev");
        registry.add("spring.rabbitmq.virtual-host", () -> "mesatech");
        registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri",
                () -> "https://login.microsoftonline.com/common/v2.0");
        registry.add("spring.security.oauth2.resourceserver.jwt.audiences", () -> "test-audience");
    }

    @Test
    @Order(1)
    void dosEntregasDelMismoIdDejanUnaSolaCopia() throws Exception {
        publicar(42, "Redes");
        publicar(42, "Redes");

        await().atMost(Duration.ofSeconds(20)).until(() -> contar(42) == 1);
        await().during(Duration.ofSeconds(2)).atMost(Duration.ofSeconds(4)).until(() -> contar(42) == 1);

        mvc.perform(get("/v1/categorias-catalogo").with(jwt()
                        .authorities(new SimpleGrantedAuthority("SCOPE_access_as_user"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.catalogoId == 42)].nombre").value(contains("Redes")));
        assertThat(contar(42)).isEqualTo(1);
    }

    @Test
    @Order(2)
    void otroIdGeneraOtraFila() {
        publicar(43, "Software");
        await().atMost(Duration.ofSeconds(20)).until(() -> contar(43) == 1);
        assertThat(contar(42)).isEqualTo(1);
        assertThat(contar(43)).isEqualTo(1);
    }

    @Test
    void sinTokenElListadoResponde401() throws Exception {
        mvc.perform(get("/v1/categorias-catalogo"))
                .andExpect(status().isUnauthorized());
    }

    private long contar(long catalogoId) {
        return copias.findAll().stream().filter(copia -> catalogoId == copia.getCatalogoId()).count();
    }

    private static void publicar(long id, String nombre) {
        CachingConnectionFactory cf = new CachingConnectionFactory(RABBIT.getHost(), RABBIT.getAmqpPort());
        cf.setUsername("catalogo");
        cf.setPassword("catalogo-dev");
        cf.setVirtualHost("mesatech");
        try {
            MessageProperties properties = new MessageProperties();
            properties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
            properties.setHeader(
                    DefaultJacksonJavaTypeMapper.DEFAULT_CLASSID_FIELD_NAME,
                    "cl.duoc.mesatech.catalogo.messaging.CategoriaCreadaEvento"
            );
            Message message = MessageBuilder
                    .withBody(("{\"id\":" + id + ",\"nombre\":\"" + nombre + "\"}").getBytes(StandardCharsets.UTF_8))
                    .andProperties(properties)
                    .build();
            new RabbitTemplate(cf).send("catalogo.exchange", "categoria.creada", message);
        } finally {
            cf.destroy();
        }
    }

    private static void prepararBroker() throws Exception {
        comando("rabbitmqctl", "add_vhost", "mesatech");
        comando("rabbitmqctl", "add_user", "catalogo", "catalogo-dev");
        comando("rabbitmqctl", "add_user", "solicitudes", "solicitudes-dev");
        comando("rabbitmqctl", "set_permissions", "-p", "mesatech", "catalogo",
                "^catalogo\\.exchange$", "^catalogo\\.exchange$", "^$");
        comando("rabbitmqctl", "set_permissions", "-p", "mesatech", "solicitudes",
                "^(catalogo\\.exchange|solicitudes\\.catalogo\\.queue)$",
                "^solicitudes\\.catalogo\\.queue$",
                "^(catalogo\\.exchange|solicitudes\\.catalogo\\.queue)$");
    }

    private static void comando(String... args) throws Exception {
        Container.ExecResult result = RABBIT.execInContainer(args);
        if (result.getExitCode() != 0) {
            throw new IllegalStateException(String.join(" ", args)
                    + "\n" + result.getStdout() + "\n" + result.getStderr());
        }
    }
}
