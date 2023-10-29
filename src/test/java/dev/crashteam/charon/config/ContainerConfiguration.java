package dev.crashteam.charon.config;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

public class ContainerConfiguration {

    private static PostgreSQLContainer<?> postgresql;

    @BeforeAll
    public static void start() {
       postgresSql().start();
    }

    @AfterAll
    public static void stop() {
        postgresSql().stop();
    }

    public static PostgreSQLContainer<?> postgresSql() {
        if (postgresql == null) {
            postgresql = new PostgreSQLContainer<>("postgres:14-alpine");
            postgresql.withDatabaseName("postgresql")
                    .withUsername("postgres")
                    .withPassword("password");
        }
        return postgresql;
    }

    @DynamicPropertySource
    public static void properties(DynamicPropertyRegistry registry) {
        registry.add("data-source.pool-source.url", postgresql::getJdbcUrl);
        registry.add("data-source.pool-source.password", postgresql::getPassword);
        registry.add("data-source.pool-source.username", postgresql::getUsername);
        registry.add("spring.datasource.url", postgresql::getJdbcUrl);
        registry.add("spring.datasource.password", postgresql::getPassword);
        registry.add("spring.datasource.username", postgresql::getUsername);
    }

}
