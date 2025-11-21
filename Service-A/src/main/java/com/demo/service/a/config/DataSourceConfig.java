package com.demo.service.a.config;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.jdbc.datasource.JdbcTelemetry;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

/**
 * Application DataSource configuration.
 *
 * <p>This configuration creates two Spring beans:
 * <ul>
 *   <li>{@code HikariDataSource} named {@code hikariDataSource} — the actual connection pool configured
 *       with application properties and sensible defaults for pooling, timeouts and leak detection.</li>
 *   <li>{@code DataSource} (primary) — a wrapper around the Hikari pool created by
 *       {@link #hikariDataSource()} which instruments JDBC calls with OpenTelemetry via
 *       {@link JdbcTelemetry}. Use this {@code DataSource} for application JDBC access so that
 *       SQL statements are traced and exported.</li>
 * </ul>
 *
 * Configuration values are read from the standard Spring properties:
 * <ul>
 *   <li>{@code spring.datasource.url}</li>
 *   <li>{@code spring.datasource.username}</li>
 *   <li>{@code spring.datasource.password}</li>
 *   <li>{@code spring.datasource.driver-class-name}</li>
 * </ul>
 */
@Configuration
@RequiredArgsConstructor
public class DataSourceConfig {

    // OpenTelemetry SDK instance injected by Spring (configured elsewhere in the app)
    private final OpenTelemetry openTelemetry;

    // Standard Spring Boot datasource properties (injected from application.yml/properties)
    @Value( "${spring.datasource.url}")
    private String url;
    @Value( "${spring.datasource.username}")
    private String username;
    @Value( "${spring.datasource.password}")
    private String password;
    @Value( "${spring.datasource.driver-class-name}")
    private String driver;

    /**
     * Create a HikariCP DataSource with application-specific and safe defaults.
     *
     * <p>Notes about the configuration choices below:
     * <ul>
     *   <li>Pool sizing (maximum/minimum): tuned conservatively for small demo apps; adjust for
     *       production load.</li>
     *   <li>Connection timeouts and lifetimes: choose values that match your DB SLA and
     *       connection stability.</li>
     *   <li>Leak detection threshold: useful in development to detect connections that are not
     *       returned to the pool; set to 0 to disable in production if it creates noise.</li>
     *   <li>Connection test query: used to validate a connection before use when the driver does
     *       not support JDBC4isValid.</li>
     * </ul>
     *
     * @return a configured {@link HikariDataSource}
     */
    @Bean
    public HikariDataSource hikariDataSource() {
        // Create and configure the Hikari connection pool
        HikariConfig config = new HikariConfig();
        // Basic connection properties
        config.setJdbcUrl(url);
        config.setUsername(username);
        config.setPassword(password);
        config.setDriverClassName(driver);

        // Pool identification
        config.setPoolName("TracedHikariPool");

        // Pool sizing: adjust these numbers according to expected concurrency and DB limits
        config.setMaximumPoolSize(10); // maximum number of connections
        config.setMinimumIdle(2);      // minimum number of idle connections to maintain

        // Timeouts and lifetimes (milliseconds)
        config.setConnectionTimeout(30000); // max wait for a connection (30 seconds)
        config.setIdleTimeout(600000);      // how long a connection can sit idle (10 minutes)
        config.setMaxLifetime(1800000);     // max lifetime of a connection (30 minutes)

        // Leak detection: logs a WARN when connections are held longer than this threshold
        config.setLeakDetectionThreshold(60000); // 60 seconds

        // Use a simple validation query for drivers that require it
        config.setConnectionTestQuery("SELECT 1");

        // JMX and pool suspension settings — change as needed
        config.setRegisterMbeans(false);
        config.setAllowPoolSuspension(true);

        return new HikariDataSource(config);
    }

    /**
     * Primary {@link DataSource} bean for the application.
     *
     * <p>This wraps the Hikari pool with OpenTelemetry JDBC instrumentation so that each
     * executed statement produces tracing spans. Registering this bean as {@code @Primary}
     * ensures it is injected by default where a {@code DataSource} is required.
     *
     * @return an instrumented {@link DataSource} which delegates to the Hikari pool
     */
    @Bean
    @Primary
    public DataSource dataSource() {
        // Wrap the HikariDataSource with JDBC telemetry so SQL executions are traced.
        // Using JdbcTelemetry.create(openTelemetry).wrap(...) is a lightweight way to
        // instrument calls without changing existing DAO/repository code.
        return JdbcTelemetry.create(openTelemetry).wrap(hikariDataSource());

    }
}