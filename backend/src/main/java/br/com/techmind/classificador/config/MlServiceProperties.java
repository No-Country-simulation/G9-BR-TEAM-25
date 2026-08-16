package br.com.techmind.classificador.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ml-service")
public record MlServiceProperties(String url, boolean mockEnabled, int connectTimeoutMs, int readTimeoutMs) { }
