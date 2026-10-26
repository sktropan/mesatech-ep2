package cl.duoc.mesatech.catalogo;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.Container;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.rabbitmq.RabbitMQContainer;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CatalogoRabbitTest {

    static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.4");
    static final RabbitMQContainer RABBIT = new RabbitMQContainer("rabbitmq:4.0-management");
    static final AtomicBoolean BROKER_LISTO = new AtomicBoolean(false);

    @Autowired
    MockMvc mvc;

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
        registry.add("spring.rabbitmq.username", () -> "catalogo");
        registry.add("spring.rabbitmq.password", () -> "catalogo-dev");
        registry.add("spring.rabbitmq.virtual-host", () -> "mesatech");
        registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri",
                () -> "https://login.microsoftonline.com/common/v2.0");
        registry.add("spring.security.oauth2.resourceserver.jwt.audiences", () -> "test-audience");
    }

    @Test
    @Order(1)
    void guestNoFiguraEnElVhostYNoSeConecta() throws Exception {
        Container.ExecResult permisos = RABBIT.execInContainer("rabbitmqctl", "list_permissions", "-p", "mesatech");
        assertThat(permisos.getExitCode()).isZero();
        assertThat(permisos.getStdout()).doesNotContain("guest");

        CachingConnectionFactory cf = fabrica("guest", "guest");
        try {
            RabbitTemplate template = new RabbitTemplate(cf);
            assertThatThrownBy(() -> template.execute(channel -> channel.isOpen()))
                    .isInstanceOf(AmqpException.class);
        } finally {
            cf.destroy();
        }
    }

    @Test
    @Order(2)
    void usuarioSinPermisosNoAbreElVhost() {
        CachingConnectionFactory cf = fabrica("sinpermiso", "sinpermiso-dev");
        try {
            RabbitTemplate template = new RabbitTemplate(cf);
            assertThatThrownBy(() -> template.execute(channel -> channel.isOpen()))
                    .isInstanceOf(AmqpException.class);
        } finally {
            cf.destroy();
        }
    }

    @Test
    @Order(3)
    void elPublicadorNoPuedeLeerLaCola() {
        CachingConnectionFactory cf = fabrica("catalogo", "catalogo-dev");
        try {
            RabbitTemplate template = new RabbitTemplate(cf);
            assertThatThrownBy(() -> template.receive("solicitudes.catalogo.queue", 500))
                    .isInstanceOf(AmqpException.class);
        } finally {
            cf.destroy();
        }
    }

    @Test
    @Order(4)
    void nombreEnBlancoResponde400() throws Exception {
        mvc.perform(post("/v1/catalogo/categorias")
                        .with(token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"   \"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(5)
    void crearCategoriaPublicaJsonYLoConfirma() throws Exception {
        MvcResult result = mvc.perform(post("/v1/catalogo/categorias")
                        .with(token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Hardware\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Hardware"))
                .andExpect(jsonPath("$.id").isNumber())
                .andReturn();
        Number id = JsonPath.read(result.getResponse().getContentAsString(), "$.id");

        CachingConnectionFactory cf = fabrica("solicitudes", "solicitudes-dev");
        try {
            Message message = new RabbitTemplate(cf).receive("solicitudes.catalogo.queue", 5000);
            assertThat(message).isNotNull();
            assertThat(message.getMessageProperties().getContentType()).contains("json");
            String json = new String(message.getBody(), StandardCharsets.UTF_8);
            assertThat(json).contains("\"id\":" + id);
            assertThat(json).contains("\"nombre\":\"Hardware\"");
        } finally {
            cf.destroy();
        }
    }

    @Test
    @Order(6)
    void sinColaDestinoResponde503YNoGuarda() throws Exception {
        CachingConnectionFactory cf = fabrica("solicitudes", "solicitudes-dev");
        RabbitAdmin admin = new RabbitAdmin(cf);
        DirectExchange exchange = new DirectExchange("catalogo.exchange", true, false);
        Queue queue = new Queue("solicitudes.catalogo.queue", true);
        Binding binding = BindingBuilder.bind(queue).to(exchange).with("categoria.creada");
        admin.removeBinding(binding);
        try {
            mvc.perform(post("/v1/catalogo/categorias")
                            .with(token())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"nombre\":\"Huerfana\"}"))
                    .andExpect(status().isServiceUnavailable())
                    .andExpect(jsonPath("$.error").value("Servicio no disponible"))
                    .andExpect(jsonPath("$.message", containsString("La categoría no se guardó")));

            String listado = mvc.perform(get("/v1/catalogo").with(token()))
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse()
                    .getContentAsString();
            assertThat(listado).doesNotContain("Huerfana");
        } finally {
            admin.declareBinding(binding);
            cf.destroy();
        }
    }

    @Test
    @Order(7)
    void brokerDetenidoResponde503YNoGuarda() throws Exception {
        RABBIT.stop();
        mvc.perform(post("/v1/catalogo/categorias")
                        .with(token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"SinBroker\"}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message", containsString("La categoría no se guardó")));

        String listado = mvc.perform(get("/v1/catalogo").with(token()))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertThat(listado).doesNotContain("SinBroker");
    }

    private static org.springframework.test.web.servlet.request.RequestPostProcessor token() {
        return jwt().authorities(new SimpleGrantedAuthority("SCOPE_access_as_user"));
    }

    private static CachingConnectionFactory fabrica(String usuario, String clave) {
        CachingConnectionFactory cf = new CachingConnectionFactory(RABBIT.getHost(), RABBIT.getAmqpPort());
        cf.setUsername(usuario);
        cf.setPassword(clave);
        cf.setVirtualHost("mesatech");
        cf.setConnectionTimeout(3000);
        return cf;
    }

    private static void prepararBroker() throws Exception {
        comando("rabbitmqctl", "add_vhost", "mesatech");
        comando("rabbitmqctl", "add_user", "catalogo", "catalogo-dev");
        comando("rabbitmqctl", "add_user", "solicitudes", "solicitudes-dev");
        comando("rabbitmqctl", "add_user", "sinpermiso", "sinpermiso-dev");
        comando("rabbitmqctl", "set_permissions", "-p", "mesatech", "catalogo",
                "^catalogo\\.exchange$", "^catalogo\\.exchange$", "^$");
        comando("rabbitmqctl", "set_permissions", "-p", "mesatech", "solicitudes",
                "^(catalogo\\.exchange|solicitudes\\.catalogo\\.queue)$",
                "^solicitudes\\.catalogo\\.queue$",
                "^(catalogo\\.exchange|solicitudes\\.catalogo\\.queue)$");
        declararTopologiaComoConsumidor();
    }

    private static void declararTopologiaComoConsumidor() {
        CachingConnectionFactory cf = fabrica("solicitudes", "solicitudes-dev");
        try {
            RabbitAdmin admin = new RabbitAdmin(cf);
            DirectExchange exchange = new DirectExchange("catalogo.exchange", true, false);
            Queue queue = new Queue("solicitudes.catalogo.queue", true);
            admin.declareExchange(exchange);
            admin.declareQueue(queue);
            admin.declareBinding(BindingBuilder.bind(queue).to(exchange).with("categoria.creada"));
        } finally {
            cf.destroy();
        }
    }

    private static void comando(String... args) throws Exception {
        Container.ExecResult result = RABBIT.execInContainer(args);
        if (result.getExitCode() != 0) {
            throw new IllegalStateException(String.join(" ", args)
                    + "\n" + result.getStdout() + "\n" + result.getStderr());
        }
    }
}
