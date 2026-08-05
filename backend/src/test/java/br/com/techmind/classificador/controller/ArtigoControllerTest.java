package br.com.techmind.classificador.controller;

import br.com.techmind.classificador.dto.ClassificacaoResponse;
import br.com.techmind.classificador.exception.RegistroNotFoundException;
import br.com.techmind.classificador.service.ArtigoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ArtigoController.class)
@Import(br.com.techmind.classificador.exception.GlobalExceptionHandler.class)
class ArtigoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ArtigoService artigoService;

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
    void deveRetornarArtigoNaoEncontrado() throws Exception {
        when(artigoService.buscar(99L)).thenThrow(new RegistroNotFoundException(99L));

        mockMvc.perform(get("/api/artigos/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void deveRetornarHealthDaApi() throws Exception {
        mockMvc.perform(get("/api/artigos/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.servico").value("techmind-classificador"));
    }
}
