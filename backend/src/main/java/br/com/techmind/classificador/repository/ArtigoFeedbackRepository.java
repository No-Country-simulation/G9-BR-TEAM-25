package br.com.techmind.classificador.repository;

import br.com.techmind.classificador.entity.ArtigoFeedback;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * @author Diego Pitoco
 */
public interface ArtigoFeedbackRepository extends JpaRepository<ArtigoFeedback, Long> {
    List<ArtigoFeedback> findByArtigoIdOrderByDecididoEmDesc(Long artigoId);
}
