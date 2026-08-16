package br.com.techmind.classificador.repository;

import br.com.techmind.classificador.entity.ArtigoClassificado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

/**
 * @author Diego Pitoco
 */
public interface ArtigoRepository extends JpaRepository<ArtigoClassificado, Long>,
        JpaSpecificationExecutor<ArtigoClassificado> {

    @Query("select a.status as chave, count(a) as total from ArtigoClassificado a group by a.status")
    List<ContagemProjecao> contarPorStatus();

    @Query("select a.categoria as chave, count(a) as total from ArtigoClassificado a group by a.categoria")
    List<ContagemProjecao> contarPorCategoria();

    @Query("select avg(a.probabilidade) from ArtigoClassificado a")
    Double calcularConfiancaMedia();

    interface ContagemProjecao {
        String getChave();
        long getTotal();
    }
}
