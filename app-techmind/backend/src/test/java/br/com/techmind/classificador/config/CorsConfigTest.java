package br.com.techmind.classificador.config;

import br.com.techmind.classificador.controller.ArtigoController;
import br.com.techmind.classificador.exception.GlobalExceptionHandler;
import br.com.techmind.classificador.service.ArtigoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ArtigoController.class)
@Import({GlobalExceptionHandler.class, CorsConfig.class})
@TestPropertySource(properties = "cors.allowed-origins=http://localhost:5173")
class CorsConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ArtigoService artigoService;

    @Test
    void devePermitirOrigemConfigurada() throws Exception {
        mockMvc.perform(options("/api/artigos")
                        .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5173"));
    }

    @Test
    void deveBloquearOrigemNaoConfigurada() throws Exception {
        mockMvc.perform(options("/api/artigos")
                        .header(HttpHeaders.ORIGIN, "http://origem-nao-autorizada.example.com")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isForbidden());
    }
}
