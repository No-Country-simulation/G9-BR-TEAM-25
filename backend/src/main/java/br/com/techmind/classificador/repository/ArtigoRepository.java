package br.com.techmind.classificador.repository;

import br.com.techmind.classificador.entity.ArtigoClassificado;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ArtigoRepository extends JpaRepository<ArtigoClassificado, Long> {
    List<ArtigoClassificado> findAllByOrderByCriadoEmDesc();
}
