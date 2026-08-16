package br.com.techmind.classificador.dto;

import java.util.Map;

/**
 * @author Diego Pitoco
 */
public record EstatisticasResponse(
        long total,
        long aprovados,
        long pendentesModeracao,
        long rejeitados,
        int quantidadeCategorias,
        double confiancaMedia,
        Map<String, Long> distribuicaoPorCategoria) {
}
