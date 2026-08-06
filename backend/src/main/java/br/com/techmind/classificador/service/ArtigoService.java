package br.com.techmind.classificador.service;

import br.com.techmind.classificador.client.PredicaoGateway;
import br.com.techmind.classificador.dto.ClassificacaoRequest;
import br.com.techmind.classificador.dto.ClassificacaoResponse;
import br.com.techmind.classificador.dto.ArtigoResponse;
import br.com.techmind.classificador.dto.PaginaArtigosResponse;
import br.com.techmind.classificador.entity.ArtigoClassificado;
import br.com.techmind.classificador.exception.ParametroInvalidoException;
import br.com.techmind.classificador.exception.RegistroNotFoundException;
import br.com.techmind.classificador.repository.ArtigoRepository;
import br.com.techmind.classificador.repository.ArtigoSpecifications;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class ArtigoService {
    public static final double LIMIAR_APROVACAO = 0.70;

    private static final Set<String> STATUS_VALIDOS = Set.of("APROVADO", "PENDENTE");
    private static final Map<String, String> CAMPOS_ORDENACAO = Map.of(
            "criadoEm", "criadoEm",
            "titulo", "titulo",
            "categoria", "categoria",
            "status", "status",
            "confianca", "probabilidade");

    private final PredicaoGateway predicaoClient;
    private final ArtigoRepository artigoRepository;
    private final int tamanhoMaximoPagina;

    public ArtigoService(PredicaoGateway predicaoClient, ArtigoRepository artigoRepository,
                         @Value("${artigos.paginacao.tamanho-maximo:100}") int tamanhoMaximoPagina) {
        this.predicaoClient = predicaoClient;
        this.artigoRepository = artigoRepository;
        this.tamanhoMaximoPagina = tamanhoMaximoPagina;
    }

    public ClassificacaoResponse classificar(ClassificacaoRequest request) {
        var titulo = request.titulo().trim();
        var texto = request.texto().trim();
        var predicao = predicaoClient.predizer(titulo + "\n" + texto);
        var status = predicao.probabilidade() >= LIMIAR_APROVACAO ? "APROVADO" : "PENDENTE";
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
                artigo.getProbabilidade(), artigo.getStatus(), artigo.getInformacoesAdicionais(), artigo.getAutores(),
                artigo.getLink(), artigo.getAno(), artigo.getCriadoEm());
    }
}
