package br.com.techmind.classificador.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "artigos_classificados")
public class ArtigoClassificado {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String titulo;

    @Lob
    @Column(nullable = false)
    private String texto;

    @Column(length = 255)
    private String autores;
    @Column(length = 500)
    private String link;
    private Integer ano;

    @Column(nullable = false, length = 80)
    private String categoria;

    @Column(nullable = false)
    private double probabilidade;

    @Column(nullable = false, length = 20)
    private String status;

    @ElementCollection
    @CollectionTable(name = "artigos_informacoes", joinColumns = @JoinColumn(name = "artigo_id"))
    @Column(name = "informacao", length = 100)
    private List<String> informacoesAdicionais = new ArrayList<>();

    @Column(nullable = false)
    private LocalDateTime criadoEm;

    protected ArtigoClassificado() { }

    public ArtigoClassificado(String titulo, String texto, String categoria, double probabilidade,
                              String status, List<String> informacoesAdicionais) {
        this(titulo, texto, categoria, probabilidade, status, informacoesAdicionais, null, null, null);
    }

    public ArtigoClassificado(String titulo, String texto, String categoria, double probabilidade,
                              String status, List<String> informacoesAdicionais,
                              String autores, String link, Integer ano) {
        this.titulo = titulo;
        this.texto = texto;
        this.categoria = categoria;
        this.probabilidade = probabilidade;
        this.status = status;
        this.informacoesAdicionais = new ArrayList<>(informacoesAdicionais);
        this.autores = autores;
        this.link = link;
        this.ano = ano;
        this.criadoEm = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getTitulo() { return titulo; }
    public String getTexto() { return texto; }
    public String getAutores() { return autores; }
    public String getLink() { return link; }
    public Integer getAno() { return ano; }
    public String getCategoria() { return categoria; }
    public double getProbabilidade() { return probabilidade; }
    public String getStatus() { return status; }
    public List<String> getInformacoesAdicionais() { return List.copyOf(informacoesAdicionais); }
    public LocalDateTime getCriadoEm() { return criadoEm; }
}
