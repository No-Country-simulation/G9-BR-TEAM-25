package br.com.techmind.classificador.client;

import br.com.techmind.classificador.config.MlServiceProperties;
import br.com.techmind.classificador.exception.MlIntegrationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.ArrayList;

@Component
public class PredicaoClient implements PredicaoGateway {
    private static final Logger log = LoggerFactory.getLogger(PredicaoClient.class);

    private final RestClient restClient;

    private final boolean mockEnabled;

    public PredicaoClient(RestClient.Builder builder, MlServiceProperties properties) {
        if (!properties.mockEnabled() && (properties.url() == null || properties.url().isBlank())) {
            throw new IllegalStateException(
                    "Configuração inválida: ML_SERVICE_MOCK_ENABLED=false requer ML_SERVICE_URL configurado. "
                            + "Defina a variável de ambiente ML_SERVICE_URL com a URL do serviço de Ciência de Dados "
                            + "ou habilite o mock (ML_SERVICE_MOCK_ENABLED=true) para desenvolvimento local.");
        }
        this.restClient = builder.baseUrl(properties.url()).build();
        this.mockEnabled = properties.mockEnabled();
        log.info("Integração com serviço de ML configurada: modo={}, url={}, connectTimeoutMs={}, readTimeoutMs={}",
                mockEnabled ? "MOCK" : "SERVIÇO EXTERNO", properties.url(),
                properties.connectTimeoutMs(), properties.readTimeoutMs());
    }

    public PredicaoResponse predizer(String titulo, String texto) {
        if (mockEnabled) {
            return mockPrediction(titulo + " " + texto);
        }
        try {
            var response = restClient.post().uri("/api/v1/artigos/processar-completo")
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
                    relacionado("104", "Construindo Microsservicos com Java e Spring Cloud", "Backend", 0.91),
                    relacionado("312", "Boas Praticas de Autenticacao JWT em APIs REST", "Ciberseguranca", 0.85),
                    relacionado("58", "Arquitetura de Software em Aplicacoes Java", "Backend", 0.79)));
        }
        if (textoNormalizado.contains("react") || textoNormalizado.contains("frontend")) {
            adicionais.add("React");
            adicionais.add("JavaScript");
            return new PredicaoResponse("Frontend", 0.82, adicionais, List.of(
                    relacionado("201", "Componentes reutilizaveis com React", "Frontend", 0.88)));
        }
        if (textoNormalizado.contains("aws") || textoNormalizado.contains("cloud")) {
            adicionais.add("AWS");
            return new PredicaoResponse("Cloud Computing", 0.76, adicionais, List.of(
                    relacionado("407", "Fundamentos de computacao em nuvem", "Cloud Computing", 0.81)));
        }
        return new PredicaoResponse("Indefinido", 0.45, adicionais, List.of());
    }

    private br.com.techmind.classificador.dto.ClassificacaoResponse.ArtigoRelacionado relacionado(
            String id, String titulo, String categoria, double scoreSimilaridade) {
        return new br.com.techmind.classificador.dto.ClassificacaoResponse.ArtigoRelacionado(id, titulo, categoria, scoreSimilaridade);
    }

    public record PredicaoRequest(String titulo, String resumo) { }
    public record PredicaoResponse(String categoria, double confianca, String status, List<String> palavrasChave,
                                   List<br.com.techmind.classificador.dto.ClassificacaoResponse.ArtigoRelacionado> artigosRelacionados) {
        public PredicaoResponse(String categoria, double confianca, List<String> palavrasChave,
                                 List<br.com.techmind.classificador.dto.ClassificacaoResponse.ArtigoRelacionado> artigosRelacionados) {
            this(categoria, confianca, null, palavrasChave, artigosRelacionados);
        }

        public PredicaoResponse(String categoria, double confianca, List<String> palavrasChave) {
            this(categoria, confianca, null, palavrasChave, List.of());
        }
        public double probabilidade() { return confianca; }
        public List<String> informacoesAdicionais() { return palavrasChave; }
    }
}
