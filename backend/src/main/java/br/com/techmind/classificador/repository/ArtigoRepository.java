package br.com.techmind.classificador.repository;

import br.com.techmind.classificador.entity.ArtigoClassificado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ArtigoRepository extends JpaRepository<ArtigoClassificado, Long>,
        JpaSpecificationExecutor<ArtigoClassificado> {
}
