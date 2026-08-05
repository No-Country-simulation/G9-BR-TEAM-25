package br.com.techmind.classificador.service;

import br.com.techmind.classificador.client.PredicaoGateway;
import br.com.techmind.classificador.dto.ClassificacaoRequest;
import br.com.techmind.classificador.dto.ClassificacaoResponse;
import br.com.techmind.classificador.dto.ArtigoResponse;
import br.com.techmind.classificador.entity.ArtigoClassificado;
import br.com.techmind.classificador.exception.RegistroNotFoundException;
import br.com.techmind.classificador.repository.ArtigoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ArtigoService {
    public static final double LIMIAR_APROVACAO = 0.70;
    private final PredicaoGateway predicaoClient;
    private final ArtigoRepository artigoRepository;

    public ArtigoService(PredicaoGateway predicaoClient, ArtigoRepository artigoRepository) {
        this.predicaoClient = predicaoClient;
        this.artigoRepository = artigoRepository;
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
    public List<ArtigoResponse> listar() {
        return artigoRepository.findAllByOrderByCriadoEmDesc().stream().map(this::toResponse).toList();
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
