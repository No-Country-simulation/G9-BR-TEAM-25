package br.com.techmind.classificador.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @author Diego Pitoco
 */
public record ArtigoResponse(Long id, String titulo, String texto, String categoria, String categoriaOriginal,
                             double confianca, String status,
                             List<String> palavrasChave, String autores, String link, Integer ano,
                             LocalDateTime criadoEm) {
    public double probabilidade() { return confianca; }
    public List<String> informacoesAdicionais() { return palavrasChave; }
}
