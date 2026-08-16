package br.com.techmind.classificador.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({OciProperties.class, MlServiceProperties.class})
public class PropertiesConfig { }
