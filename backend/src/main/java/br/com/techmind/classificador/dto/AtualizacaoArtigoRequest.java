package br.com.techmind.classificador.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * @author Diego Pitoco
 */
public record AtualizacaoArtigoRequest(
        @NotBlank(message = "título é obrigatório") @Size(min = 3, max = 200) String titulo,
        @NotBlank(message = "texto é obrigatório") @Size(min = 10) String texto,
        String autores,
        String link,
        @Min(1900) @Max(2100) Integer ano) {
}
