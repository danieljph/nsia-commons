package com.nsia.commons.cucumber.config;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MockServerContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.ConfluentKafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ThreadLocalRandom;

/**
 * @author Daniel Joi Partogi Hutapea
 */
@Testcontainers
public abstract class ContainerBase
{
    private static final int RANDOM_REDIS_PORT = ThreadLocalRandom.current().nextInt(10000, 65535);

    @Container
    public static PostgreSQLContainer<?> POSTGRE_SQL_CONTAINER = new PostgreSQLContainer<>("postgres:16.8");

    @Container
    public static MockServerContainer MOCK_SERVER_CONTAINER = new MockServerContainer(DockerImageName.parse("mockserver/mockserver:5.15.0"));

    @Container
    public static ConfluentKafkaContainer KAFKA_CONTAINER = new ConfluentKafkaContainer("confluentinc/cp-kafka:7.5.0");

    @Container
    public static GenericContainer<?> REDIS_CONTAINER = new GenericContainer(DockerImageName.parse("redis:5.0.3-alpine"));

    static
    {
        POSTGRE_SQL_CONTAINER.withDatabaseName("");
        POSTGRE_SQL_CONTAINER.withUrlParam("serverTimezone", "Asia/Jakarta");
        POSTGRE_SQL_CONTAINER.withEnv("TZ","Asia/Jakarta");
        POSTGRE_SQL_CONTAINER.withReuse(true);
        POSTGRE_SQL_CONTAINER.start();

        MOCK_SERVER_CONTAINER.withReuse(false);
        MOCK_SERVER_CONTAINER.withStartupTimeout(Duration.of(5, ChronoUnit.MINUTES));
        MOCK_SERVER_CONTAINER.start();

        KAFKA_CONTAINER.withReuse(false);
        KAFKA_CONTAINER.start();

        REDIS_CONTAINER.withExposedPorts(6379);
        REDIS_CONTAINER.withReuse(true);
        REDIS_CONTAINER.start();
    }

    @DynamicPropertySource
    public static void overrideProps(DynamicPropertyRegistry registry)
    {
        registry.add("spring.datasource.url", POSTGRE_SQL_CONTAINER::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRE_SQL_CONTAINER::getUsername);
        registry.add("spring.datasource.password", POSTGRE_SQL_CONTAINER::getPassword);

        registry.add("mockserver.base-url", () -> MOCK_SERVER_CONTAINER.getEndpoint());
        registry.add("mockserver.host", MOCK_SERVER_CONTAINER::getHost);
        registry.add("mockserver.port", MOCK_SERVER_CONTAINER::getServerPort);

        registry.add("spring.kafka.bootstrap-servers", KAFKA_CONTAINER::getBootstrapServers);
        registry.add("spring.kafka.producer.bootstrap-servers", KAFKA_CONTAINER::getBootstrapServers);
        registry.add("spring.kafka.consumer.bootstrap-servers", KAFKA_CONTAINER::getBootstrapServers);

        registry.add("spring.data.redis.host", REDIS_CONTAINER::getHost);
        registry.add("spring.data.redis.port", REDIS_CONTAINER::getFirstMappedPort);
    }
}
