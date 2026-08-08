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

/**
 * @author Diego Pitoco
 */
class PredicaoClientTest {

    private static final String RESPOSTA_COMPLETA = """
            {
              "categoria": "Backend",
              "probabilidade": 0.87,
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
    void deveLerCategoriaProbabilidadeStatusEPalavrasChave() {
        server.expect(requestTo("http://ml-service.test/api/v1/artigos/processar-completo"))
                .andRespond(withSuccess(RESPOSTA_COMPLETA, MediaType.APPLICATION_JSON));

        var resposta = client.predizer("Spring Boot", "Artigo sobre APIs Java");

        assertEquals("Backend", resposta.categoria());
        assertEquals(0.87, resposta.probabilidade());
        assertEquals("APROVADO", resposta.status());
        assertEquals(java.util.List.of("Java", "Spring Boot"), resposta.palavrasChave());
    }

    @Test
    void devePreservarStatusPendenteModeracaoRecebidoDaApi() {
        var respostaComPendenteModeracao = """
                {
                  "categoria": "Indefinido",
                  "probabilidade": 0.95,
                  "status": "PENDENTE_MODERACAO",
                  "palavrasChave": [],
                  "artigosRelacionados": []
                }
                """;
        server.expect(requestTo("http://ml-service.test/api/v1/artigos/processar-completo"))
                .andRespond(withSuccess(respostaComPendenteModeracao, MediaType.APPLICATION_JSON));

        var resposta = client.predizer("Spring Boot", "Artigo sobre APIs Java");

        assertEquals("PENDENTE_MODERACAO", resposta.status());
        assertEquals(0.95, resposta.probabilidade());
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
    void deveLancarMensagemEspecificaDeTimeoutQuandoServicoDemorarDemais() {
        server.expect(requestTo("http://ml-service.test/api/v1/artigos/processar-completo"))
                .andRespond(request -> { throw new java.net.SocketTimeoutException("Read timed out"); });

        var exception = assertThrows(MlIntegrationException.class,
                () -> client.predizer("Spring Boot", "Artigo sobre APIs Java"));

        assertEquals("O serviço de Ciência de Dados demorou demais para responder. Tente novamente.",
                exception.getMessage());
    }

    @Test
    void deveFalharNaInicializacaoQuandoMockDesabilitadoEUrlAusente() {
        var builder = RestClient.builder();

        assertThrows(IllegalStateException.class,
                () -> new PredicaoClient(builder, new MlServiceProperties(" ", false, 3000, 5000)));
    }
}
