package com.demo.service.a.config;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.micrometer.tracing.Tracer;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.jdbc.datasource.JdbcTelemetry;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

@Configuration
@RequiredArgsConstructor
public class DataSourceConfig {

    private final OpenTelemetry openTelemetry;
    @Value( "${spring.datasource.url}")
    private String url;
    @Value( "${spring.datasource.username}")
    private String username;
    @Value( "${spring.datasource.password}")
    private String password;
    @Value( "${spring.datasource.driver-class-name}")
    private String driver;
    /**
     * HikariDataSource với OpenTelemetry instrumentation
     */
    @Bean
    public HikariDataSource hikariDataSource() {
        // Tạo HikariDataSource
        HikariConfig config = new HikariConfig();
        // Basic configuration
        config.setJdbcUrl(url);
        config.setUsername(username);
        config.setPassword(password);
        config.setDriverClassName(driver);
        // Pool configuration
        config.setPoolName("TracedHikariPool");
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(30000); // 30 seconds
        config.setIdleTimeout(600000); // 10 minutes
        config.setMaxLifetime(1800000); // 30 minutes
        config.setLeakDetectionThreshold(60000); // 60 seconds

        // Connection test
        config.setConnectionTestQuery("SELECT 1");

        // Metrics and monitoring
        config.setRegisterMbeans(false);
        config.setAllowPoolSuspension(true);

        return new HikariDataSource(config);
    }
    @Bean
    @Primary
    public DataSource dataSource() {
        // Wrap HikariDataSource với Proxy để trace queries
        return JdbcTelemetry.create(openTelemetry).wrap(hikariDataSource());

    }
}