package br.com.techmind.classificador.controller;

import br.com.techmind.classificador.dto.ArtigoResponse;
import br.com.techmind.classificador.dto.ClassificacaoResponse;
import br.com.techmind.classificador.dto.EstatisticasResponse;
import br.com.techmind.classificador.dto.FeedbackResponse;
import br.com.techmind.classificador.dto.PaginaArtigosResponse;
import br.com.techmind.classificador.exception.ParametroInvalidoException;
import br.com.techmind.classificador.exception.RegistroNotFoundException;
import br.com.techmind.classificador.service.ArtigoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Diego Pitoco
 */
@WebMvcTest(ArtigoController.class)
@Import(br.com.techmind.classificador.exception.GlobalExceptionHandler.class)
class ArtigoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ArtigoService artigoService;

    private ArtigoResponse artigoDeExemplo(String categoria, String categoriaOriginal, String status) {
        return new ArtigoResponse(1L, "Introdução ao Spring Boot", "Conteúdo completo do artigo.", categoria,
                categoriaOriginal, 0.89, status, List.of("Java", "Spring Boot"), "Equipe TechMind",
                "https://exemplo.com", 2026, LocalDateTime.of(2026, 8, 6, 10, 0));
    }

    @Test
    void deveClassificarArtigo() throws Exception {
        when(artigoService.classificar(any()))
                .thenReturn(new ClassificacaoResponse("Backend", 0.89, "APROVADO", List.of("Java")));

        mockMvc.perform(post("/api/artigos/classificar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\":\"Spring Boot\",\"texto\":\"Artigo sobre APIs\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categoria").value("Backend"))
                .andExpect(jsonPath("$.probabilidade").value(0.89))
                .andExpect(jsonPath("$.status").value("APROVADO"));
    }

    @Test
    void deveRejeitarClassificacaoSemTitulo() throws Exception {
        mockMvc.perform(post("/api/artigos/classificar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\":\"\",\"texto\":\"Texto válido\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.path").value("/api/artigos/classificar"));
    }

    @Test
    void deveListarArtigosPaginados() throws Exception {
        var artigo = artigoDeExemplo("Backend", "Backend", "APROVADO");
        var pagina = new PaginaArtigosResponse(List.of(artigo), 0, 10, 1, 1, true, true);
        when(artigoService.listar(0, 10, "criadoEm,desc", null, null, null, null)).thenReturn(pagina);

        mockMvc.perform(get("/api/artigos?page=0&size=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conteudo[0].titulo").value("Introdução ao Spring Boot"))
                .andExpect(jsonPath("$.conteudo[0].palavrasChave[0]").value("Java"))
                .andExpect(jsonPath("$.pagina").value(0))
                .andExpect(jsonPath("$.tamanho").value(10))
                .andExpect(jsonPath("$.totalElementos").value(1))
                .andExpect(jsonPath("$.totalPaginas").value(1))
                .andExpect(jsonPath("$.primeira").value(true))
                .andExpect(jsonPath("$.ultima").value(true));
    }

    @Test
    void deveRetornarBadRequestQuandoStatusForInvalido() throws Exception {
        when(artigoService.listar(anyInt(), anyInt(), any(), any(), any(), any(), any()))
                .thenThrow(new ParametroInvalidoException("Status inválido: 'INVALIDO'. Valores aceitos: [APROVADO, PENDENTE_MODERACAO, REJEITADO]"));

        mockMvc.perform(get("/api/artigos?status=INVALIDO"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.erro").value("Parâmetro inválido"));
    }

    @Test
    void deveRetornarBadRequestQuandoSizeNaoForNumerico() throws Exception {
        mockMvc.perform(get("/api/artigos?size=abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.erro").value("Parâmetro inválido"));
    }

    @Test
    void deveBuscarArtigoExistente() throws Exception {
        when(artigoService.buscar(1L)).thenReturn(artigoDeExemplo("Backend", "Backend", "APROVADO"));

        mockMvc.perform(get("/api/artigos/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.titulo").value("Introdução ao Spring Boot"))
                .andExpect(jsonPath("$.categoria").value("Backend"))
                .andExpect(jsonPath("$.palavrasChave[0]").value("Java"))
                .andExpect(jsonPath("$.artigosRelacionados").doesNotExist());
    }

    @Test
    void deveRetornarArtigoNaoEncontrado() throws Exception {
        when(artigoService.buscar(99L)).thenThrow(new RegistroNotFoundException(99L));

        mockMvc.perform(get("/api/artigos/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void deveAtualizarConteudoDoArtigo() throws Exception {
        when(artigoService.atualizar(eq(1L), any())).thenReturn(artigoDeExemplo("Backend", "Backend", "APROVADO"));

        mockMvc.perform(put("/api/artigos/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\":\"Título editado\",\"texto\":\"Texto editado com conteúdo suficiente\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titulo").value("Introdução ao Spring Boot"));
    }

    @Test
    void deveRetornarNotFoundAoAtualizarArtigoInexistente() throws Exception {
        when(artigoService.atualizar(eq(99L), any())).thenThrow(new RegistroNotFoundException(99L));

        mockMvc.perform(put("/api/artigos/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\":\"Título válido\",\"texto\":\"Texto com conteúdo suficiente\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void deveExcluirArtigo() throws Exception {
        mockMvc.perform(delete("/api/artigos/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void deveRetornarNotFoundAoExcluirArtigoInexistente() throws Exception {
        org.mockito.Mockito.doThrow(new RegistroNotFoundException(99L)).when(artigoService).excluir(99L);

        mockMvc.perform(delete("/api/artigos/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void deveModerarArtigoAprovando() throws Exception {
        when(artigoService.moderar(eq(1L), any())).thenReturn(artigoDeExemplo("Backend", "Backend", "APROVADO"));

        mockMvc.perform(patch("/api/artigos/1/moderacao")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decisao\":\"APROVADO\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APROVADO"));
    }

    @Test
    void deveRetornarBadRequestParaTransicaoDeModeracaoInvalida() throws Exception {
        when(artigoService.moderar(eq(1L), any()))
                .thenThrow(new ParametroInvalidoException("Transição inválida"));

        mockMvc.perform(patch("/api/artigos/1/moderacao")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decisao\":\"APROVADO\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deveListarFeedbackDoArtigo() throws Exception {
        when(artigoService.buscarFeedback(1L)).thenReturn(List.of(
                new FeedbackResponse(1L, 1L, "Mobile Development", "Backend Development", 0.24, "APROVADO",
                        LocalDateTime.of(2026, 8, 7, 12, 0))));

        mockMvc.perform(get("/api/artigos/1/feedback"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].decisao").value("APROVADO"))
                .andExpect(jsonPath("$[0].categoriaCorrigida").value("Backend Development"));
    }

    @Test
    void deveRetornarEstatisticasGlobais() throws Exception {
        when(artigoService.estatisticas()).thenReturn(new EstatisticasResponse(
                6, 3, 2, 1, 2, 0.735, Map.of("Backend", 4L, "Frontend", 2L)));

        mockMvc.perform(get("/api/artigos/estatisticas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(6))
                .andExpect(jsonPath("$.aprovados").value(3))
                .andExpect(jsonPath("$.pendentesModeracao").value(2))
                .andExpect(jsonPath("$.rejeitados").value(1))
                .andExpect(jsonPath("$.quantidadeCategorias").value(2))
                .andExpect(jsonPath("$.confiancaMedia").value(0.735));
    }

    @Test
    void deveRetornarHealthDaApi() throws Exception {
        mockMvc.perform(get("/api/artigos/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.servico").value("techmind-classificador"));
    }
}
