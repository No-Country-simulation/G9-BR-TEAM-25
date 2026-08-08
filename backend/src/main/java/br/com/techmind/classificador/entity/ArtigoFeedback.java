package br.com.techmind.classificador.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * @author Diego Pitoco
 */
@Entity
@Table(name = "artigos_feedback")
public class ArtigoFeedback {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "artigo_id", nullable = false)
    private Long artigoId;

    @Column(name = "categoria_original", length = 80)
    private String categoriaOriginal;

    @Column(name = "categoria_corrigida", length = 80)
    private String categoriaCorrigida;

    @Column(name = "probabilidade_original")
    private Double probabilidadeOriginal;

    @Column(nullable = false, length = 20)
    private String decisao;

    @Column(name = "decidido_em", nullable = false)
    private LocalDateTime decididoEm;

    protected ArtigoFeedback() { }

    public ArtigoFeedback(Long artigoId, String categoriaOriginal, String categoriaCorrigida,
                          Double probabilidadeOriginal, String decisao) {
        this.artigoId = artigoId;
        this.categoriaOriginal = categoriaOriginal;
        this.categoriaCorrigida = categoriaCorrigida;
        this.probabilidadeOriginal = probabilidadeOriginal;
        this.decisao = decisao;
        this.decididoEm = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Long getArtigoId() { return artigoId; }
    public String getCategoriaOriginal() { return categoriaOriginal; }
    public String getCategoriaCorrigida() { return categoriaCorrigida; }
    public Double getProbabilidadeOriginal() { return probabilidadeOriginal; }
    public String getDecisao() { return decisao; }
    public LocalDateTime getDecididoEm() { return decididoEm; }
}
