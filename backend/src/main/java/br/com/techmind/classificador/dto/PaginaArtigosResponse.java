package br.com.techmind.classificador.dto;

import java.util.List;

public record PaginaArtigosResponse(
        List<ArtigoResponse> conteudo,
        int pagina,
        int tamanho,
        long totalElementos,
        int totalPaginas,
        boolean primeira,
        boolean ultima) {
}
