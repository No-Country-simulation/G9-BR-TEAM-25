package br.com.techmind.classificador.dto;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonProperty;

public record ClassificacaoResponse(String categoria, double confianca, String status,
                                    List<String> palavrasChave,
                                    List<ArtigoRelacionado> artigosRelacionados) {
    public ClassificacaoResponse(String categoria, double confianca, String status, List<String> palavrasChave) {
        this(categoria, confianca, status, palavrasChave, List.of());
    }
    @JsonProperty("probabilidade")
    public double probabilidade() { return confianca; }
    @JsonProperty("informacoesAdicionais")
    public List<String> informacoesAdicionais() { return palavrasChave; }
    public record ArtigoRelacionado(Long id, String titulo, String categoria) { }
}
