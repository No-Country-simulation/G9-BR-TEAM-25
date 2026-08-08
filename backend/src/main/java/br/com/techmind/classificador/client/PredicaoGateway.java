package br.com.techmind.classificador.client;

/**
 * @author Diego Pitoco
 */
public interface PredicaoGateway {
    PredicaoClient.PredicaoResponse predizer(String titulo, String texto);
}
