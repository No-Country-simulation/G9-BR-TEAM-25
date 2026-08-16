package br.com.techmind.classificador.repository;

import br.com.techmind.classificador.entity.ArtigoClassificado;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("local")
class ArtigoRepositorySpecificationTest {

    @Autowired
    private ArtigoRepository artigoRepository;

    @Test
    void deveListarSemFiltrosRetornandoTodosOsRegistros() {
        salvar("Artigo A", "Backend", "APROVADO", "Java");
        salvar("Artigo B", "Frontend", "PENDENTE", "React");

        var pagina = artigoRepository.findAll((Specification<ArtigoClassificado>) null, PageRequest.of(0, 10));

        assertEquals(2, pagina.getTotalElements());
    }

    @Test
    void devePaginarCorretamente() {
        for (var i = 1; i <= 15; i++) {
            salvar("Artigo " + i, "Backend", "APROVADO", "Java");
        }

        var pagina1 = artigoRepository.findAll((Specification<ArtigoClassificado>) null, PageRequest.of(0, 10));
        var pagina2 = artigoRepository.findAll((Specification<ArtigoClassificado>) null, PageRequest.of(1, 10));

        assertEquals(10, pagina1.getContent().size());
        assertEquals(5, pagina2.getContent().size());
        assertEquals(15, pagina1.getTotalElements());
        assertEquals(2, pagina1.getTotalPages());
        assertTrue(pagina1.isFirst());
        assertTrue(pagina2.isLast());
    }

    @Test
    void deveOrdenarPeloCampoInformado() {
        salvar("Zebra", "Backend", "APROVADO", "Java");
        salvar("Alfa", "Backend", "APROVADO", "Java");

        var pagina = artigoRepository.findAll((Specification<ArtigoClassificado>) null,
                PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "titulo")));

        assertEquals("Alfa", pagina.getContent().get(0).getTitulo());
        assertEquals("Zebra", pagina.getContent().get(1).getTitulo());
    }

    @Test
    void deveFiltrarPorTituloParcialIgnorandoCaixa() {
        salvar("Introdução ao Spring Boot", "Backend", "APROVADO", "Java");
        salvar("React na prática", "Frontend", "APROVADO", "React");

        var pagina = artigoRepository.findAll(ArtigoSpecifications.tituloContem("SPRING"), PageRequest.of(0, 10));

        assertEquals(1, pagina.getTotalElements());
        assertEquals("Introdução ao Spring Boot", pagina.getContent().get(0).getTitulo());
    }

    @Test
    void deveFiltrarPorCategoria() {
        salvar("Artigo A", "Backend", "APROVADO", "Java");
        salvar("Artigo B", "Frontend", "APROVADO", "React");

        var pagina = artigoRepository.findAll(ArtigoSpecifications.categoriaIgual("backend"), PageRequest.of(0, 10));

        assertEquals(1, pagina.getTotalElements());
        assertEquals("Backend", pagina.getContent().get(0).getCategoria());
    }

    @Test
    void deveFiltrarPorStatus() {
        salvar("Artigo A", "Backend", "APROVADO", "Java");
        salvar("Artigo B", "Backend", "PENDENTE", "Java");

        var pagina = artigoRepository.findAll(ArtigoSpecifications.statusIgual("PENDENTE"), PageRequest.of(0, 10));

        assertEquals(1, pagina.getTotalElements());
        assertEquals("PENDENTE", pagina.getContent().get(0).getStatus());
    }

    @Test
    void deveFiltrarPorPalavraChave() {
        salvar("Artigo A", "Backend", "APROVADO", "Java", "Spring Boot");
        salvar("Artigo B", "Frontend", "APROVADO", "React");

        var pagina = artigoRepository.findAll(ArtigoSpecifications.palavraChaveContem("spring"), PageRequest.of(0, 10));

        assertEquals(1, pagina.getTotalElements());
        assertEquals("Artigo A", pagina.getContent().get(0).getTitulo());
    }

    @Test
    void deveCombinarFiltros() {
        salvar("Artigo A", "Backend", "APROVADO", "Java");
        salvar("Artigo B", "Backend", "PENDENTE", "Java");
        salvar("Artigo C", "Frontend", "APROVADO", "React");

        var especificacao = ArtigoSpecifications.categoriaIgual("Backend")
                .and(ArtigoSpecifications.statusIgual("APROVADO"));

        var pagina = artigoRepository.findAll(especificacao, PageRequest.of(0, 10));

        assertEquals(1, pagina.getTotalElements());
        assertEquals("Artigo A", pagina.getContent().get(0).getTitulo());
    }

    @Test
    void deveRetornarPaginaVaziaQuandoNadaCorresponder() {
        salvar("Artigo A", "Backend", "APROVADO", "Java");

        var pagina = artigoRepository.findAll(ArtigoSpecifications.categoriaIgual("Inexistente"), PageRequest.of(0, 10));

        assertEquals(0, pagina.getTotalElements());
        assertTrue(pagina.getContent().isEmpty());
    }

    private void salvar(String titulo, String categoria, String status, String... palavrasChave) {
        artigoRepository.save(new ArtigoClassificado(titulo, "Texto de teste com conteúdo suficiente.",
                categoria, 0.9, status, List.of(palavrasChave)));
    }
}
