package br.com.techmind.classificador.client;

import br.com.techmind.classificador.exception.MlIntegrationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.ArrayList;

@Component
public class PredicaoClient implements PredicaoGateway {
    private final RestClient restClient;

    private final boolean mockEnabled;

    public PredicaoClient(RestClient.Builder builder, @Value("${ml-service.url}") String url,
                          @Value("${ml-service.mock-enabled:false}") boolean mockEnabled) {
        this.restClient = builder.baseUrl(url).build();
        this.mockEnabled = mockEnabled;
    }

    public PredicaoResponse predizer(String titulo, String texto) {
        if (mockEnabled) {
            return mockPrediction(titulo + " " + texto);
        }
        try {
            var response = restClient.post().uri("/predict")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new PredicaoRequest(titulo, texto)).retrieve().body(PredicaoResponse.class);
            if (response == null || response.categoria() == null) {
                throw new MlIntegrationException("Resposta inválida do serviço de ML");
            }
            return response;
        } catch (RestClientException exception) {
            throw new MlIntegrationException("Não foi possível comunicar com o serviço de ML", exception);
        }
    }

    public PredicaoResponse predizer(String texto) {
        var partes = texto.split("\\n", 2);
        return predizer(partes[0], partes.length > 1 ? partes[1] : partes[0]);
    }

    private PredicaoResponse mockPrediction(String texto) {
        var adicionais = new ArrayList<String>();
        var textoNormalizado = texto.toLowerCase();
        if (textoNormalizado.contains("spring") || textoNormalizado.contains("java")) {
            adicionais.add("Java");
            adicionais.add("Spring Boot");
            adicionais.add("API REST");
            return new PredicaoResponse("Backend", 0.89, adicionais, List.of(
                    relacionado(104L, "Construindo Microsservicos com Java e Spring Cloud", "Backend"),
                    relacionado(312L, "Boas Praticas de Autenticacao JWT em APIs REST", "Ciberseguranca"),
                    relacionado(58L, "Arquitetura de Software em Aplicacoes Java", "Backend")));
        }
        if (textoNormalizado.contains("react") || textoNormalizado.contains("frontend")) {
            adicionais.add("React");
            adicionais.add("JavaScript");
            return new PredicaoResponse("Frontend", 0.82, adicionais, List.of(
                    relacionado(201L, "Componentes reutilizaveis com React", "Frontend")));
        }
        if (textoNormalizado.contains("aws") || textoNormalizado.contains("cloud")) {
            adicionais.add("AWS");
            return new PredicaoResponse("Cloud Computing", 0.76, adicionais, List.of(
                    relacionado(407L, "Fundamentos de computacao em nuvem", "Cloud Computing")));
        }
        return new PredicaoResponse("Indefinido", 0.45, adicionais, List.of());
    }

    private br.com.techmind.classificador.dto.ClassificacaoResponse.ArtigoRelacionado relacionado(Long id, String titulo, String categoria) {
        return new br.com.techmind.classificador.dto.ClassificacaoResponse.ArtigoRelacionado(id, titulo, categoria);
    }

    public record PredicaoRequest(String titulo, String texto) { }
    public record PredicaoResponse(String categoria, double confianca, List<String> palavrasChave,
                                   List<br.com.techmind.classificador.dto.ClassificacaoResponse.ArtigoRelacionado> artigosRelacionados) {
        public PredicaoResponse(String categoria, double confianca, List<String> palavrasChave) {
            this(categoria, confianca, palavrasChave, List.of());
        }
        public double probabilidade() { return confianca; }
        public List<String> informacoesAdicionais() { return palavrasChave; }
    }
}
