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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Fluxo completo: Controller -&gt; ArtigoService -&gt; PredicaoClient -&gt; serviço externo
 * simulado (MockRestServiceServer) -&gt; persistência H2 -&gt; resposta HTTP.
 *
 * @author Diego Pitoco
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
    void deveModerarEditarEExcluirArtigoPersistidoNoBanco() throws Exception {
        var respostaPendente = """
                {
                  "categoria": "Mobile Development",
                  "probabilidade": 0.24,
                  "status": "PENDENTE_MODERACAO",
                  "palavrasChave": ["mobile"],
                  "artigosRelacionados": []
                }
                """;
        ServicoExternoSimuladoConfig.server.expect(requestTo("http://ml-service.test/api/v1/artigos/processar-completo"))
                .andRespond(withSuccess(respostaPendente, MediaType.APPLICATION_JSON));

        mockMvc.perform(post("/api/artigos/classificar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REQUISICAO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDENTE_MODERACAO"));

        var id = artigoRepository.findAll().get(0).getId();

        mockMvc.perform(patch("/api/artigos/" + id + "/moderacao")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decisao\":\"APROVADO\",\"categoriaCorrigida\":\"Backend Development\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APROVADO"))
                .andExpect(jsonPath("$.categoria").value("Backend Development"))
                .andExpect(jsonPath("$.categoriaOriginal").value("Mobile Development"));

        mockMvc.perform(get("/api/artigos/" + id + "/feedback"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].decisao").value("APROVADO"))
                .andExpect(jsonPath("$[0].categoriaOriginal").value("Mobile Development"))
                .andExpect(jsonPath("$[0].categoriaCorrigida").value("Backend Development"));

        mockMvc.perform(put("/api/artigos/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\":\"Spring Boot na pratica - editado\",\"texto\":\"Texto editado com conteúdo suficiente\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titulo").value("Spring Boot na pratica - editado"))
                .andExpect(jsonPath("$.status").value("APROVADO"));

        mockMvc.perform(get("/api/artigos/estatisticas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.aprovados").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)));

        mockMvc.perform(delete("/api/artigos/" + id))
                .andExpect(status().isNoContent());

        assertEquals(0, artigoRepository.count());
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
