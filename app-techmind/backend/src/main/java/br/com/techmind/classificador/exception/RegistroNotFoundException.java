package br.com.techmind.classificador.exception;

public class RegistroNotFoundException extends RuntimeException {
    public RegistroNotFoundException(Long id) {
        super("Artigo classificado não encontrado: " + id);
    }
}
