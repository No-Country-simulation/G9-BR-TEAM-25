package br.com.techmind.classificador.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "oci.database")
public record OciProperties(String url, String username, String password, String walletPath) { }
