package com.pezesha.cblms.configs;

import org.springframework.context.annotation.Configuration;
import io.r2dbc.spi.ConnectionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.r2dbc.connection.init.ConnectionFactoryInitializer;
import org.springframework.r2dbc.connection.init.ResourceDatabasePopulator;
/**
 * @author AOmar
 */
@Configuration
public class DatabaseConfig {
    @Bean
    public ConnectionFactoryInitializer databaseInitializer(ConnectionFactory connectionFactory) {
        ConnectionFactoryInitializer initializer = new ConnectionFactoryInitializer();
        initializer.setConnectionFactory(connectionFactory);

        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        populator.addScript(new ClassPathResource("db/db.sql"));
        populator.setContinueOnError(true);
        populator.setIgnoreFailedDrops(true);

        initializer.setDatabasePopulator(populator);

        return initializer;
    }
}
