package br.com.techmind.classificador.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * @author Diego Pitoco
 */
public record ModeracaoRequest(
        @NotBlank(message = "decisão é obrigatória") String decisao,
        String categoriaCorrigida) {
}
