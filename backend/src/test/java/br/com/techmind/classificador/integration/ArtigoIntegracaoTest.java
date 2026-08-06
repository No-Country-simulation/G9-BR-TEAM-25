package br.com.techmind.classificador.integration;

import br.com.techmind.classificador.repository.ArtigoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.net.ConnectException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Fluxo completo: Controller -&gt; ArtigoService -&gt; PredicaoClient -&gt; serviço externo
 * simulado (MockRestServiceServer) -&gt; persistência H2 -&gt; resposta HTTP.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
@TestPropertySource(properties = {
        "ml-service.mock-enabled=false",
        "ml-service.url=http://ml-service.test"
})
@Transactional
class ArtigoIntegracaoTest {

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

    private static final String REQUISICAO = """
            {"titulo":"Spring Boot na pratica","texto":"Artigo sobre Java, Spring e Docker"}
            """;

    @TestConfiguration
    static class ServicoExternoSimuladoConfig {
        static MockRestServiceServer server;

        @Bean
        @Primary
        RestClient.Builder mockRestClientBuilder() {
            var builder = RestClient.builder();
            server = MockRestServiceServer.bindTo(builder).build();
            return builder;
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ArtigoRepository artigoRepository;

    @BeforeEach
    void resetServidorSimulado() {
        ServicoExternoSimuladoConfig.server.reset();
    }

    @Test
    void deveClassificarViaServicoExternoSimuladoEPersistirArtigoPrincipal() throws Exception {
        ServicoExternoSimuladoConfig.server.expect(requestTo("http://ml-service.test/api/v1/artigos/processar-completo"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath("$.titulo").value("Spring Boot na pratica"))
                .andExpect(org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath("$.resumo").value("Artigo sobre Java, Spring e Docker"))
                .andRespond(withSuccess(RESPOSTA_COMPLETA, MediaType.APPLICATION_JSON));

        mockMvc.perform(post("/api/artigos/classificar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REQUISICAO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categoria").value("Backend"))
                .andExpect(jsonPath("$.confianca").value(0.87))
                .andExpect(jsonPath("$.status").value("APROVADO"))
                .andExpect(jsonPath("$.palavrasChave[0]").value("Java"))
                .andExpect(jsonPath("$.artigosRelacionados[0].id").value("123"))
                .andExpect(jsonPath("$.artigosRelacionados[0].titulo").value("Artigo relacionado"))
                .andExpect(jsonPath("$.artigosRelacionados[0].scoreSimilaridade").value(0.84));

        ServicoExternoSimuladoConfig.server.verify();

        var artigos = artigoRepository.findAll();
        assertEquals(1, artigos.size());
        assertEquals("Backend", artigos.get(0).getCategoria());
        assertEquals("APROVADO", artigos.get(0).getStatus());
    }

    @Test
    void deveRetornarErroPadronizadoQuandoServicoExternoIndisponivelSemFallbackParaMock() throws Exception {
        ServicoExternoSimuladoConfig.server.expect(requestTo("http://ml-service.test/api/v1/artigos/processar-completo"))
                .andRespond(request -> { throw new ConnectException("Conexão recusada"); });

        mockMvc.perform(post("/api/artigos/classificar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REQUISICAO))
                .andExpect(status().is(502))
                .andExpect(jsonPath("$.status").value(502))
                .andExpect(jsonPath("$.erro").value("Erro de integração ML"))
                .andExpect(jsonPath("$.path").value("/api/artigos/classificar"));

        assertEquals(0, artigoRepository.count());
    }
}
