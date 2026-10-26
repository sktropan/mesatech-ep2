package cl.duoc.mesatech.solicitudes.repo;

import cl.duoc.mesatech.solicitudes.domain.CategoriaCatalogo;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoriaCatalogoRepository extends JpaRepository<CategoriaCatalogo, Long> {

    boolean existsByCatalogoId(Long catalogoId);
}
