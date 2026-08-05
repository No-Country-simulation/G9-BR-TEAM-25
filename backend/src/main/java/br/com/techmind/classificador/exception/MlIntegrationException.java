package br.com.techmind.classificador.exception;

public class MlIntegrationException extends RuntimeException {
    public MlIntegrationException(String message) { super(message); }
    public MlIntegrationException(String message, Throwable cause) { super(message, cause); }
}
