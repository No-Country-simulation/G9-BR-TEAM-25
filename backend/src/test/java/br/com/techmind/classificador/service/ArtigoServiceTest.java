package br.com.techmind.classificador.service;

import br.com.techmind.classificador.client.PredicaoGateway;
import br.com.techmind.classificador.client.PredicaoClient;
import br.com.techmind.classificador.dto.ClassificacaoRequest;
import br.com.techmind.classificador.entity.ArtigoClassificado;
import br.com.techmind.classificador.exception.ParametroInvalidoException;
import br.com.techmind.classificador.exception.RegistroNotFoundException;
import br.com.techmind.classificador.repository.ArtigoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ArtigoServiceTest {

    @Mock
    private PredicaoGateway predicaoClient;

    @Mock
    private ArtigoRepository artigoRepository;

    private ArtigoService artigoService;

    @BeforeEach
    void setUp() {
        artigoService = new ArtigoService(predicaoClient, artigoRepository, 100);
    }

    @Test
    void deveClassificarComoAprovadoEGuardarTextoNormalizado() {
        var predicao = new PredicaoClient.PredicaoResponse(
                "Backend", 0.89, List.of("Java", "Spring Boot"));
        when(predicaoClient.predizer("Spring Boot\nArtigo sobre Java"))
                .thenReturn(predicao);
        when(artigoRepository.save(any(ArtigoClassificado.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var resposta = artigoService.classificar(new ClassificacaoRequest(
                "  Spring Boot  ", "  Artigo sobre Java  "));

        assertEquals("Backend", resposta.categoria());
        assertEquals(0.89, resposta.probabilidade());
        assertEquals("APROVADO", resposta.status());
        assertIterableEquals(List.of("Java", "Spring Boot"), resposta.informacoesAdicionais());

        var captor = ArgumentCaptor.forClass(ArtigoClassificado.class);
        verify(artigoRepository).save(captor.capture());
        assertEquals("Spring Boot", captor.getValue().getTitulo());
        assertEquals("Artigo sobre Java", captor.getValue().getTexto());
    }

    @Test
    void deveClassificarComoPendenteQuandoProbabilidadeForMenorQueLimiar() {
        when(predicaoClient.predizer(any()))
                .thenReturn(new PredicaoClient.PredicaoResponse("Indefinido", 0.69, null));
        when(artigoRepository.save(any(ArtigoClassificado.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var resposta = artigoService.classificar(new ClassificacaoRequest("Título", "Texto"));

        assertEquals("PENDENTE", resposta.status());
        assertEquals(List.of(), resposta.informacoesAdicionais());
    }

    @Test
    void deveListarArtigosConvertidosParaResposta() {
        var artigo = new ArtigoClassificado("Título", "Texto", "Backend", 0.89,
                "APROVADO", List.of("Java"));
        var pageable = PageRequest.of(0, 10);
        when(artigoRepository.findAll(any(Specification.class), any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(artigo), pageable, 1));

        var pagina = artigoService.listar(0, 10, null, null, null, null, null);

        assertEquals(1, pagina.conteudo().size());
        assertEquals("Título", pagina.conteudo().get(0).titulo());
        assertEquals("Backend", pagina.conteudo().get(0).categoria());
        assertIterableEquals(List.of("Java"), pagina.conteudo().get(0).informacoesAdicionais());
        assertEquals(0, pagina.pagina());
        assertEquals(1, pagina.totalElementos());
        assertEquals(1, pagina.totalPaginas());
        assertEquals(true, pagina.primeira());
        assertEquals(true, pagina.ultima());
    }

    @Test
    void deveLancarExcecaoQuandoPageForNegativo() {
        assertThrows(ParametroInvalidoException.class,
                () -> artigoService.listar(-1, 10, null, null, null, null, null));
    }

    @Test
    void deveLancarExcecaoQuandoSizeForZeroOuNegativo() {
        assertThrows(ParametroInvalidoException.class,
                () -> artigoService.listar(0, 0, null, null, null, null, null));
    }

    @Test
    void deveLancarExcecaoQuandoStatusForInvalido() {
        assertThrows(ParametroInvalidoException.class,
                () -> artigoService.listar(0, 10, null, null, null, "INVALIDO", null));
    }

    @Test
    void deveLancarExcecaoQuandoCampoDeOrdenacaoForInvalido() {
        assertThrows(ParametroInvalidoException.class,
                () -> artigoService.listar(0, 10, "campoInexistente,desc", null, null, null, null));
    }

    @Test
    void deveLancarExcecaoQuandoDirecaoDeOrdenacaoForInvalida() {
        assertThrows(ParametroInvalidoException.class,
                () -> artigoService.listar(0, 10, "criadoEm,invalida", null, null, null, null));
    }

    @Test
    void deveLimitarTamanhoDaPaginaAoMaximoConfigurado() {
        var pageable = PageRequest.of(0, 100);
        when(artigoRepository.findAll(any(Specification.class), any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        artigoService.listar(0, 500, null, null, null, null, null);

        var captor = ArgumentCaptor.forClass(org.springframework.data.domain.Pageable.class);
        verify(artigoRepository).findAll(any(Specification.class), captor.capture());
        assertEquals(100, captor.getValue().getPageSize());
    }

    @Test
    void deveBuscarArtigoPorId() {
        var artigo = new ArtigoClassificado("Título", "Texto", "Cloud", 0.76,
                "Aprovado", List.of("AWS"));
        when(artigoRepository.findById(7L)).thenReturn(Optional.of(artigo));

        var resposta = artigoService.buscar(7L);

        assertEquals("Título", resposta.titulo());
        assertEquals("Cloud", resposta.categoria());
    }

    @Test
    void deveLancarExcecaoQuandoArtigoNaoForEncontrado() {
        when(artigoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RegistroNotFoundException.class, () -> artigoService.buscar(99L));
    }
}
