package br.com.techmind.classificador.client;

import br.com.techmind.classificador.config.MlServiceProperties;
import br.com.techmind.classificador.exception.MlIntegrationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.net.ConnectException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class PredicaoClientTest {

    private static final String RESPOSTA_COMPLETA = """
            {
              "categoria": "Backend",
              "confianca": 0.87,
              "status": "APROVADO",
              "palavrasChave": ["Java", "Spring Boot"],
              "artigosRelacionados": [
                {
                  "id": "123",
                  "titulo": "Artigo relacionado",
                  "categoria": "Backend",
                  "scoreSimilaridade": 0.84
                }
              ]
            }
            """;

    private MockRestServiceServer server;
    private PredicaoClient client;

    @BeforeEach
    void setUp() {
        var builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new PredicaoClient(builder, new MlServiceProperties("http://ml-service.test", false, 3000, 5000));
    }

    @Test
    void devePostarNoEndpointOficialComTituloEResumo() {
        server.expect(requestTo("http://ml-service.test/api/v1/artigos/processar-completo"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.titulo").value("Spring Boot"))
                .andExpect(jsonPath("$.resumo").value("Artigo sobre APIs Java"))
                .andRespond(withSuccess(RESPOSTA_COMPLETA, MediaType.APPLICATION_JSON));

        client.predizer("Spring Boot", "Artigo sobre APIs Java");

        server.verify();
    }

    @Test
    void deveLerCategoriaConfiancaStatusEPalavrasChave() {
        server.expect(requestTo("http://ml-service.test/api/v1/artigos/processar-completo"))
                .andRespond(withSuccess(RESPOSTA_COMPLETA, MediaType.APPLICATION_JSON));

        var resposta = client.predizer("Spring Boot", "Artigo sobre APIs Java");

        assertEquals("Backend", resposta.categoria());
        assertEquals(0.87, resposta.confianca());
        assertEquals("APROVADO", resposta.status());
        assertEquals(java.util.List.of("Java", "Spring Boot"), resposta.palavrasChave());
    }

    @Test
    void devePreservarArtigosRelacionadosComIdStringEScoreSimilaridade() {
        server.expect(requestTo("http://ml-service.test/api/v1/artigos/processar-completo"))
                .andRespond(withSuccess(RESPOSTA_COMPLETA, MediaType.APPLICATION_JSON));

        var resposta = client.predizer("Spring Boot", "Artigo sobre APIs Java");
        var relacionado = resposta.artigosRelacionados().get(0);

        assertEquals("123", relacionado.id());
        assertEquals("Artigo relacionado", relacionado.titulo());
        assertEquals("Backend", relacionado.categoria());
        assertEquals(0.84, relacionado.scoreSimilaridade());
    }

    @Test
    void deveLancarMlIntegrationExceptionQuandoServicoRetornarErro4xx() {
        server.expect(requestTo("http://ml-service.test/api/v1/artigos/processar-completo"))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST));

        assertThrows(MlIntegrationException.class,
                () -> client.predizer("Spring Boot", "Artigo sobre APIs Java"));
    }

    @Test
    void deveLancarMlIntegrationExceptionQuandoServicoRetornarErro5xx() {
        server.expect(requestTo("http://ml-service.test/api/v1/artigos/processar-completo"))
                .andRespond(withServerError());

        assertThrows(MlIntegrationException.class,
                () -> client.predizer("Spring Boot", "Artigo sobre APIs Java"));
    }

    @Test
    void deveLancarMlIntegrationExceptionQuandoServicoEstiverIndisponivel() {
        server.expect(requestTo("http://ml-service.test/api/v1/artigos/processar-completo"))
                .andRespond(request -> { throw new ConnectException("Conexão recusada"); });

        assertThrows(MlIntegrationException.class,
                () -> client.predizer("Spring Boot", "Artigo sobre APIs Java"));
    }

    @Test
    void deveFalharNaInicializacaoQuandoMockDesabilitadoEUrlAusente() {
        var builder = RestClient.builder();

        assertThrows(IllegalStateException.class,
                () -> new PredicaoClient(builder, new MlServiceProperties(" ", false, 3000, 5000)));
    }
}
