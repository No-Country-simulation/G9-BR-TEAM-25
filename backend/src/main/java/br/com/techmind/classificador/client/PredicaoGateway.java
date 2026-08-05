package br.com.techmind.classificador.client;

public interface PredicaoGateway {
    PredicaoClient.PredicaoResponse predizer(String texto);
}
