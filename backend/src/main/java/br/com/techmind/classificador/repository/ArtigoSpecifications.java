package br.com.techmind.classificador.repository;

import br.com.techmind.classificador.entity.ArtigoClassificado;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

/**
 * @author Diego Pitoco
 */
public final class ArtigoSpecifications {

    private ArtigoSpecifications() { }

    public static Specification<ArtigoClassificado> tituloContem(String titulo) {
        return (root, query, cb) -> cb.like(cb.lower(root.get("titulo")), "%" + titulo.toLowerCase() + "%");
    }

    public static Specification<ArtigoClassificado> categoriaIgual(String categoria) {
        return (root, query, cb) -> cb.equal(cb.lower(root.get("categoria")), categoria.toLowerCase());
    }

    public static Specification<ArtigoClassificado> statusIgual(String status) {
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<ArtigoClassificado> palavraChaveContem(String palavraChave) {
        return (root, query, cb) -> {
            Subquery<Long> subquery = query.subquery(Long.class);
            Root<ArtigoClassificado> subRoot = subquery.from(ArtigoClassificado.class);
            Join<ArtigoClassificado, String> informacoes = subRoot.join("informacoesAdicionais");
            subquery.select(subRoot.get("id"))
                    .where(cb.equal(subRoot.get("id"), root.get("id")),
                            cb.like(cb.lower(informacoes), "%" + palavraChave.toLowerCase() + "%"));
            return cb.exists(subquery);
        };
    }
}
