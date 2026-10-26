package cl.duoc.mesatech.solicitudes.service;

import cl.duoc.mesatech.solicitudes.domain.CategoriaCatalogo;
import cl.duoc.mesatech.solicitudes.messaging.CategoriaCreadaEvento;
import cl.duoc.mesatech.solicitudes.repo.CategoriaCatalogoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CopiaCategoriaService {

    private final CategoriaCatalogoRepository repository;

    public CopiaCategoriaService(CategoriaCatalogoRepository repository) {
        this.repository = repository;
    }

    public List<CategoriaCatalogo> listar() {
        return repository.findAll();
    }

    @Transactional
    public void guardarSiNoExiste(CategoriaCreadaEvento evento) {
        if (repository.existsByCatalogoId(evento.id())) {
            return;
        }
        CategoriaCatalogo copia = new CategoriaCatalogo();
        copia.setCatalogoId(evento.id());
        copia.setNombre(evento.nombre() == null ? "" : evento.nombre());
        repository.saveAndFlush(copia);
    }
}
