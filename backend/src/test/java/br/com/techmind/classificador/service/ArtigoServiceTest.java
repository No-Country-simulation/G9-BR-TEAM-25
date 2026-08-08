package br.com.techmind.classificador.service;

import br.com.techmind.classificador.client.PredicaoGateway;
import br.com.techmind.classificador.client.PredicaoClient;
import br.com.techmind.classificador.dto.AtualizacaoArtigoRequest;
import br.com.techmind.classificador.dto.ClassificacaoRequest;
import br.com.techmind.classificador.dto.ModeracaoRequest;
import br.com.techmind.classificador.entity.ArtigoClassificado;
import br.com.techmind.classificador.entity.ArtigoFeedback;
import br.com.techmind.classificador.exception.ParametroInvalidoException;
import br.com.techmind.classificador.exception.RegistroNotFoundException;
import br.com.techmind.classificador.repository.ArtigoFeedbackRepository;
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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Diego Pitoco
 */
@ExtendWith(MockitoExtension.class)
class ArtigoServiceTest {

    @Mock
    private PredicaoGateway predicaoClient;

    @Mock
    private ArtigoRepository artigoRepository;

    @Mock
    private ArtigoFeedbackRepository artigoFeedbackRepository;

    private ArtigoService artigoService;

    @BeforeEach
    void setUp() {
        artigoService = new ArtigoService(predicaoClient, artigoRepository, artigoFeedbackRepository, 100);
    }

    @Test
    void deveClassificarComoAprovadoEGuardarTextoOriginal() {
        var predicao = new PredicaoClient.PredicaoResponse(
                "Backend", 0.89, "APROVADO", List.of("Java", "Spring Boot"), List.of());
        when(predicaoClient.predizer("Spring Boot", "Artigo sobre Java"))
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
    void deveNormalizarTituloETextoAntesDeEnviarAoServicoDeMl() {
        when(predicaoClient.predizer("Spring Boot em producao", "Artigo sobre \"Java\" e Docker"))
                .thenReturn(new PredicaoClient.PredicaoResponse("Backend", 0.89, "APROVADO", List.of(), List.of()));
        when(artigoRepository.save(any(ArtigoClassificado.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        artigoService.classificar(new ClassificacaoRequest(
                "Spring Boot  em\nproducao", "Artigo sobre  \"Java\"\r\ne   Docker"));

        verify(predicaoClient).predizer("Spring Boot em producao", "Artigo sobre \"Java\" e Docker");
    }

    @Test
    void devePreservarStatusAprovadoRecebidoDaApiMesmoComProbabilidadeBaixa() {
        when(predicaoClient.predizer(any(), any()))
                .thenReturn(new PredicaoClient.PredicaoResponse("Indefinido", 0.10, "APROVADO", List.of(), List.of()));
        when(artigoRepository.save(any(ArtigoClassificado.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var resposta = artigoService.classificar(new ClassificacaoRequest("Título", "Texto"));

        assertEquals("APROVADO", resposta.status());
        assertEquals(0.10, resposta.probabilidade());
    }

    @Test
    void devePreservarStatusPendenteModeracaoRecebidoDaApiMesmoComProbabilidadeAlta() {
        when(predicaoClient.predizer(any(), any()))
                .thenReturn(new PredicaoClient.PredicaoResponse("Indefinido", 0.95, "PENDENTE_MODERACAO", List.of(), List.of()));
        when(artigoRepository.save(any(ArtigoClassificado.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var resposta = artigoService.classificar(new ClassificacaoRequest("Título", "Texto"));

        assertEquals("PENDENTE_MODERACAO", resposta.status());
        assertEquals(0.95, resposta.probabilidade());
    }

    @Test
    void deveAceitarStatusPendenteModeracaoNoFiltroDeListagem() {
        var pageable = PageRequest.of(0, 10);
        when(artigoRepository.findAll(any(Specification.class), any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        artigoService.listar(0, 10, null, null, null, "PENDENTE_MODERACAO", null);
    }

    @Test
    void deveAceitarStatusRejeitadoNoFiltroDeListagem() {
        var pageable = PageRequest.of(0, 10);
        when(artigoRepository.findAll(any(Specification.class), any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        artigoService.listar(0, 10, null, null, null, "REJEITADO", null);
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
        assertEquals("Cloud", resposta.categoriaOriginal());
    }

    @Test
    void deveLancarExcecaoQuandoArtigoNaoForEncontrado() {
        when(artigoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RegistroNotFoundException.class, () -> artigoService.buscar(99L));
    }

    // ---- Moderação ----

    @Test
    void deveAprovarArtigoPendenteDeModeracaoESalvarFeedback() {
        var artigo = new ArtigoClassificado("Título", "Texto", "Mobile Development", 0.24,
                "PENDENTE_MODERACAO", List.of());
        when(artigoRepository.findById(5L)).thenReturn(Optional.of(artigo));

        var resposta = artigoService.moderar(5L, new ModeracaoRequest("APROVADO", null));

        assertEquals("APROVADO", resposta.status());
        assertEquals("Mobile Development", resposta.categoria());
        verify(artigoRepository).save(artigo);

        var captor = ArgumentCaptor.forClass(ArtigoFeedback.class);
        verify(artigoFeedbackRepository).save(captor.capture());
        assertEquals("APROVADO", captor.getValue().getDecisao());
        assertEquals("Mobile Development", captor.getValue().getCategoriaOriginal());
        assertNull(captor.getValue().getCategoriaCorrigida());
    }

    @Test
    void deveRejeitarArtigoPendenteDeModeracao() {
        var artigo = new ArtigoClassificado("Título", "Texto", "Indefinido", 0.30,
                "PENDENTE_MODERACAO", List.of());
        when(artigoRepository.findById(9L)).thenReturn(Optional.of(artigo));

        var resposta = artigoService.moderar(9L, new ModeracaoRequest("REJEITADO", null));

        assertEquals("REJEITADO", resposta.status());
    }

    @Test
    void deveCorrigirCategoriaAoAprovarPreservandoCategoriaOriginal() {
        var artigo = new ArtigoClassificado("Título", "Texto", "Mobile Development", 0.24,
                "PENDENTE_MODERACAO", List.of());
        when(artigoRepository.findById(5L)).thenReturn(Optional.of(artigo));

        var resposta = artigoService.moderar(5L, new ModeracaoRequest("APROVADO", "Backend Development"));

        assertEquals("Backend Development", resposta.categoria());
        assertEquals("Mobile Development", resposta.categoriaOriginal());

        var captor = ArgumentCaptor.forClass(ArtigoFeedback.class);
        verify(artigoFeedbackRepository).save(captor.capture());
        assertEquals("Mobile Development", captor.getValue().getCategoriaOriginal());
        assertEquals("Backend Development", captor.getValue().getCategoriaCorrigida());
    }

    @Test
    void deveLancarExcecaoAoModerarArtigoQueNaoEstaPendenteDeModeracao() {
        var artigo = new ArtigoClassificado("Título", "Texto", "Backend", 0.89, "APROVADO", List.of());
        when(artigoRepository.findById(3L)).thenReturn(Optional.of(artigo));

        assertThrows(ParametroInvalidoException.class,
                () -> artigoService.moderar(3L, new ModeracaoRequest("REJEITADO", null)));
        verify(artigoRepository, never()).save(any());
        verify(artigoFeedbackRepository, never()).save(any());
    }

    @Test
    void deveLancarExcecaoQuandoDecisaoDeModeracaoForInvalida() {
        var artigo = new ArtigoClassificado("Título", "Texto", "Backend", 0.50, "PENDENTE_MODERACAO", List.of());
        when(artigoRepository.findById(4L)).thenReturn(Optional.of(artigo));

        assertThrows(ParametroInvalidoException.class,
                () -> artigoService.moderar(4L, new ModeracaoRequest("PENDENTE_MODERACAO", null)));
    }

    @Test
    void deveLancarExcecaoAoModerarArtigoInexistente() {
        when(artigoRepository.findById(404L)).thenReturn(Optional.empty());

        assertThrows(RegistroNotFoundException.class,
                () -> artigoService.moderar(404L, new ModeracaoRequest("APROVADO", null)));
    }

    // ---- Edição ----

    @Test
    void deveAtualizarConteudoSemAlterarCategoriaOuStatus() {
        var artigo = new ArtigoClassificado("Título antigo", "Texto antigo", "Backend", 0.89,
                "APROVADO", List.of());
        when(artigoRepository.findById(1L)).thenReturn(Optional.of(artigo));

        var resposta = artigoService.atualizar(1L, new AtualizacaoArtigoRequest(
                "  Título novo  ", "  Texto novo com conteúdo suficiente  ", "Novo Autor", "https://novo.com", 2027));

        assertEquals("Título novo", resposta.titulo());
        assertEquals("Texto novo com conteúdo suficiente", resposta.texto());
        assertEquals("Novo Autor", resposta.autores());
        assertEquals("Backend", resposta.categoria());
        assertEquals("APROVADO", resposta.status());
        verify(artigoRepository).save(artigo);
        verify(predicaoClient, never()).predizer(any(), any());
    }

    @Test
    void deveLancarExcecaoAoAtualizarArtigoInexistente() {
        when(artigoRepository.findById(404L)).thenReturn(Optional.empty());

        assertThrows(RegistroNotFoundException.class, () -> artigoService.atualizar(404L,
                new AtualizacaoArtigoRequest("Título válido", "Texto com conteúdo suficiente", null, null, null)));
    }

    // ---- Exclusão ----

    @Test
    void deveExcluirArtigoExistente() {
        when(artigoRepository.existsById(1L)).thenReturn(true);

        artigoService.excluir(1L);

        verify(artigoRepository).deleteById(1L);
    }

    @Test
    void deveLancarExcecaoAoExcluirArtigoInexistente() {
        when(artigoRepository.existsById(404L)).thenReturn(false);

        assertThrows(RegistroNotFoundException.class, () -> artigoService.excluir(404L));
        verify(artigoRepository, never()).deleteById(any());
    }

    // ---- Estatísticas ----

    @Test
    void deveCalcularEstatisticasGlobais() {
        when(artigoRepository.count()).thenReturn(6L);
        when(artigoRepository.contarPorStatus()).thenReturn(List.of(
                contagem("APROVADO", 3), contagem("PENDENTE_MODERACAO", 2), contagem("REJEITADO", 1)));
        when(artigoRepository.contarPorCategoria()).thenReturn(List.of(
                contagem("Backend", 4), contagem("Frontend", 2)));
        when(artigoRepository.calcularConfiancaMedia()).thenReturn(0.735);

        var estatisticas = artigoService.estatisticas();

        assertEquals(6, estatisticas.total());
        assertEquals(3, estatisticas.aprovados());
        assertEquals(2, estatisticas.pendentesModeracao());
        assertEquals(1, estatisticas.rejeitados());
        assertEquals(2, estatisticas.quantidadeCategorias());
        assertEquals(0.735, estatisticas.confiancaMedia());
        assertEquals(4L, estatisticas.distribuicaoPorCategoria().get("Backend"));
    }

    @Test
    void deveRetornarEstatisticasZeradasQuandoNaoHaArtigos() {
        when(artigoRepository.count()).thenReturn(0L);
        when(artigoRepository.contarPorStatus()).thenReturn(List.of());
        when(artigoRepository.contarPorCategoria()).thenReturn(List.of());
        when(artigoRepository.calcularConfiancaMedia()).thenReturn(null);

        var estatisticas = artigoService.estatisticas();

        assertEquals(0, estatisticas.total());
        assertEquals(0.0, estatisticas.confiancaMedia());
    }

    // ---- Feedback ----

    @Test
    void deveListarFeedbackDoArtigo() {
        when(artigoRepository.existsById(5L)).thenReturn(true);
        when(artigoFeedbackRepository.findByArtigoIdOrderByDecididoEmDesc(5L)).thenReturn(List.of(
                new ArtigoFeedback(5L, "Mobile Development", "Backend Development", 0.24, "APROVADO")));

        var feedback = artigoService.buscarFeedback(5L);

        assertEquals(1, feedback.size());
        assertEquals("APROVADO", feedback.get(0).decisao());
        assertEquals("Backend Development", feedback.get(0).categoriaCorrigida());
    }

    @Test
    void deveLancarExcecaoAoBuscarFeedbackDeArtigoInexistente() {
        when(artigoRepository.existsById(404L)).thenReturn(false);

        assertThrows(RegistroNotFoundException.class, () -> artigoService.buscarFeedback(404L));
    }

    private static ArtigoRepository.ContagemProjecao contagem(String chave, long total) {
        return new ArtigoRepository.ContagemProjecao() {
            public String getChave() { return chave; }
            public long getTotal() { return total; }
        };
    }
}
