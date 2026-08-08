package br.com.techmind.classificador.service;

import br.com.techmind.classificador.client.PredicaoGateway;
import br.com.techmind.classificador.dto.AtualizacaoArtigoRequest;
import br.com.techmind.classificador.dto.ClassificacaoRequest;
import br.com.techmind.classificador.dto.ClassificacaoResponse;
import br.com.techmind.classificador.dto.ArtigoResponse;
import br.com.techmind.classificador.dto.EstatisticasResponse;
import br.com.techmind.classificador.dto.FeedbackResponse;
import br.com.techmind.classificador.dto.ModeracaoRequest;
import br.com.techmind.classificador.dto.PaginaArtigosResponse;
import br.com.techmind.classificador.entity.ArtigoClassificado;
import br.com.techmind.classificador.entity.ArtigoFeedback;
import br.com.techmind.classificador.exception.ParametroInvalidoException;
import br.com.techmind.classificador.exception.RegistroNotFoundException;
import br.com.techmind.classificador.repository.ArtigoFeedbackRepository;
import br.com.techmind.classificador.repository.ArtigoRepository;
import br.com.techmind.classificador.repository.ArtigoSpecifications;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Diego Pitoco
 */
@Service
public class ArtigoService {
    private static final Set<String> STATUS_VALIDOS = Set.of("APROVADO", "PENDENTE_MODERACAO", "REJEITADO");
    private static final Set<String> DECISOES_MODERACAO_VALIDAS = Set.of("APROVADO", "REJEITADO");
    private static final String STATUS_AGUARDANDO_MODERACAO = "PENDENTE_MODERACAO";
    private static final Map<String, String> CAMPOS_ORDENACAO = Map.of(
            "criadoEm", "criadoEm",
            "titulo", "titulo",
            "categoria", "categoria",
            "status", "status",
            "confianca", "probabilidade");

    private final PredicaoGateway predicaoClient;
    private final ArtigoRepository artigoRepository;
    private final ArtigoFeedbackRepository artigoFeedbackRepository;
    private final int tamanhoMaximoPagina;

    public ArtigoService(PredicaoGateway predicaoClient, ArtigoRepository artigoRepository,
                         ArtigoFeedbackRepository artigoFeedbackRepository,
                         @Value("${artigos.paginacao.tamanho-maximo:100}") int tamanhoMaximoPagina) {
        this.predicaoClient = predicaoClient;
        this.artigoRepository = artigoRepository;
        this.artigoFeedbackRepository = artigoFeedbackRepository;
        this.tamanhoMaximoPagina = tamanhoMaximoPagina;
    }

    public ClassificacaoResponse classificar(ClassificacaoRequest request) {
        var titulo = request.titulo().trim();
        var texto = request.texto().trim();
        var predicao = predicaoClient.predizer(normalizarTexto(titulo), normalizarTexto(texto));
        var status = predicao.status();
        var informacoes = predicao.informacoesAdicionais() == null ? List.<String>of() : predicao.informacoesAdicionais();
        var relacionados = predicao.artigosRelacionados() == null ? List.<ClassificacaoResponse.ArtigoRelacionado>of() : predicao.artigosRelacionados();
        artigoRepository.save(new ArtigoClassificado(titulo, texto, predicao.categoria(), predicao.probabilidade(), status,
                informacoes, request.autores(), request.link(), request.ano()));
        return new ClassificacaoResponse(predicao.categoria(), predicao.probabilidade(), status, informacoes, relacionados);
    }

    @Transactional(readOnly = true)
    public PaginaArtigosResponse listar(int page, int size, String sort, String titulo, String categoria,
                                        String status, String palavraChave) {
        if (page < 0) {
            throw new ParametroInvalidoException("O parâmetro 'page' deve ser maior ou igual a 0.");
        }
        if (size <= 0) {
            throw new ParametroInvalidoException("O parâmetro 'size' deve ser maior que 0.");
        }
        var tamanho = Math.min(size, tamanhoMaximoPagina);

        var pageable = PageRequest.of(page, tamanho, resolverOrdenacao(sort));
        var especificacao = construirEspecificacao(titulo, categoria, normalizarStatus(status), palavraChave);
        var pagina = artigoRepository.findAll(especificacao, pageable);

        return new PaginaArtigosResponse(
                pagina.getContent().stream().map(this::toResponse).toList(),
                pagina.getNumber(),
                pagina.getSize(),
                pagina.getTotalElements(),
                pagina.getTotalPages(),
                pagina.isFirst(),
                pagina.isLast());
    }

    private Specification<ArtigoClassificado> construirEspecificacao(String titulo, String categoria,
                                                                      String status, String palavraChave) {
        Specification<ArtigoClassificado> especificacao = (root, query, cb) -> cb.conjunction();
        if (titulo != null && !titulo.isBlank()) {
            especificacao = especificacao.and(ArtigoSpecifications.tituloContem(titulo.trim()));
        }
        if (categoria != null && !categoria.isBlank()) {
            especificacao = especificacao.and(ArtigoSpecifications.categoriaIgual(categoria.trim()));
        }
        if (status != null) {
            especificacao = especificacao.and(ArtigoSpecifications.statusIgual(status));
        }
        if (palavraChave != null && !palavraChave.isBlank()) {
            especificacao = especificacao.and(ArtigoSpecifications.palavraChaveContem(palavraChave.trim()));
        }
        return especificacao;
    }

    private String normalizarStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        var normalizado = status.trim().toUpperCase();
        if (!STATUS_VALIDOS.contains(normalizado)) {
            throw new ParametroInvalidoException("Status inválido: '" + status + "'. Valores aceitos: " + STATUS_VALIDOS);
        }
        return normalizado;
    }

    private static String normalizarTexto(String texto) {
        return texto.replaceAll("[\\r\\n]+", " ").replaceAll(" {2,}", " ").trim();
    }

    private Sort resolverOrdenacao(String sort) {
        if (sort == null || sort.isBlank()) {
            return Sort.by(Sort.Direction.DESC, "criadoEm");
        }
        var partes = sort.split(",", 2);
        var campoPublico = partes[0].trim();
        var propriedade = CAMPOS_ORDENACAO.get(campoPublico);
        if (propriedade == null) {
            throw new ParametroInvalidoException(
                    "Campo de ordenação inválido: '" + campoPublico + "'. Valores aceitos: " + CAMPOS_ORDENACAO.keySet());
        }
        if (partes.length == 1) {
            return Sort.by(Sort.Direction.DESC, propriedade);
        }
        var direcaoTexto = partes[1].trim();
        if (!"asc".equalsIgnoreCase(direcaoTexto) && !"desc".equalsIgnoreCase(direcaoTexto)) {
            throw new ParametroInvalidoException("Direção de ordenação inválida: '" + direcaoTexto + "'. Use 'asc' ou 'desc'.");
        }
        return Sort.by(Sort.Direction.fromString(direcaoTexto), propriedade);
    }

    @Transactional(readOnly = true)
    public ArtigoResponse buscar(Long id) {
        return artigoRepository.findById(id).map(this::toResponse)
                .orElseThrow(() -> new RegistroNotFoundException(id));
    }

    private ArtigoResponse toResponse(ArtigoClassificado artigo) {
        return new ArtigoResponse(artigo.getId(), artigo.getTitulo(), artigo.getTexto(), artigo.getCategoria(),
                artigo.getCategoriaOriginal(), artigo.getProbabilidade(), artigo.getStatus(),
                artigo.getInformacoesAdicionais(), artigo.getAutores(), artigo.getLink(), artigo.getAno(),
                artigo.getCriadoEm());
    }

    @Transactional
    public ArtigoResponse moderar(Long id, ModeracaoRequest request) {
        var artigo = artigoRepository.findById(id).orElseThrow(() -> new RegistroNotFoundException(id));

        if (!STATUS_AGUARDANDO_MODERACAO.equals(artigo.getStatus())) {
            throw new ParametroInvalidoException(
                    "Transição inválida: artigo " + id + " está em status '" + artigo.getStatus()
                            + "', apenas artigos '" + STATUS_AGUARDANDO_MODERACAO + "' podem ser moderados.");
        }

        var decisao = request.decisao() == null ? null : request.decisao().trim().toUpperCase();
        if (!DECISOES_MODERACAO_VALIDAS.contains(decisao)) {
            throw new ParametroInvalidoException(
                    "Decisão inválida: '" + request.decisao() + "'. Valores aceitos: " + DECISOES_MODERACAO_VALIDAS);
        }

        var categoriaCorrigida = request.categoriaCorrigida() == null || request.categoriaCorrigida().isBlank()
                ? null : request.categoriaCorrigida().trim();
        if (categoriaCorrigida != null) {
            artigo.corrigirCategoria(categoriaCorrigida);
        }
        artigo.alterarStatus(decisao);
        artigoRepository.save(artigo);

        artigoFeedbackRepository.save(new ArtigoFeedback(artigo.getId(), artigo.getCategoriaOriginal(),
                categoriaCorrigida, artigo.getProbabilidade(), decisao));

        return toResponse(artigo);
    }

    @Transactional
    public ArtigoResponse atualizar(Long id, AtualizacaoArtigoRequest request) {
        var artigo = artigoRepository.findById(id).orElseThrow(() -> new RegistroNotFoundException(id));
        artigo.atualizarConteudo(request.titulo().trim(), request.texto().trim(), request.autores(),
                request.link(), request.ano());
        artigoRepository.save(artigo);
        return toResponse(artigo);
    }

    @Transactional
    public void excluir(Long id) {
        if (!artigoRepository.existsById(id)) {
            throw new RegistroNotFoundException(id);
        }
        artigoRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public EstatisticasResponse estatisticas() {
        var total = artigoRepository.count();
        var porStatus = artigoRepository.contarPorStatus().stream()
                .collect(java.util.stream.Collectors.toMap(
                        ArtigoRepository.ContagemProjecao::getChave, ArtigoRepository.ContagemProjecao::getTotal));
        var porCategoria = artigoRepository.contarPorCategoria().stream()
                .collect(java.util.stream.Collectors.toMap(
                        ArtigoRepository.ContagemProjecao::getChave, ArtigoRepository.ContagemProjecao::getTotal,
                        (a, b) -> a, LinkedHashMap::new));
        var confiancaMedia = artigoRepository.calcularConfiancaMedia();

        return new EstatisticasResponse(
                total,
                porStatus.getOrDefault("APROVADO", 0L),
                porStatus.getOrDefault("PENDENTE_MODERACAO", 0L),
                porStatus.getOrDefault("REJEITADO", 0L),
                porCategoria.size(),
                confiancaMedia == null ? 0.0 : confiancaMedia,
                porCategoria);
    }

    @Transactional(readOnly = true)
    public List<FeedbackResponse> buscarFeedback(Long artigoId) {
        if (!artigoRepository.existsById(artigoId)) {
            throw new RegistroNotFoundException(artigoId);
        }
        return artigoFeedbackRepository.findByArtigoIdOrderByDecididoEmDesc(artigoId).stream()
                .map(f -> new FeedbackResponse(f.getId(), f.getArtigoId(), f.getCategoriaOriginal(),
                        f.getCategoriaCorrigida(), f.getProbabilidadeOriginal(), f.getDecisao(), f.getDecididoEm()))
                .toList();
    }
}
