package br.com.techmind.classificador.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;

@Configuration
@ConditionalOnProperty(name = "oci.database.enabled", havingValue = "true")
public class OciDatabaseConfig {
    @Bean
    DataSource dataSource(OciProperties properties) {
        System.setProperty("oracle.net.tns_admin", properties.walletPath());
        var dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("oracle.jdbc.OracleDriver");
        dataSource.setUrl(properties.url());
        dataSource.setUsername(properties.username());
        dataSource.setPassword(properties.password());
        return dataSource;
    }
}
