package br.com.techmind.classificador.dto;

import java.time.LocalDateTime;

/**
 * @author Diego Pitoco
 */
public record FeedbackResponse(
        Long id,
        Long artigoId,
        String categoriaOriginal,
        String categoriaCorrigida,
        Double probabilidadeOriginal,
        String decisao,
        LocalDateTime decididoEm) {
}
