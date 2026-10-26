package cl.duoc.mesatech.catalogo.service;

import cl.duoc.mesatech.catalogo.domain.Categoria;
import cl.duoc.mesatech.catalogo.messaging.CategoriaCreadaEvento;
import cl.duoc.mesatech.catalogo.messaging.PublicadorCategoriaCreada;
import cl.duoc.mesatech.catalogo.repo.CategoriaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CategoriaService {

    private final CategoriaRepository categorias;
    private final PublicadorCategoriaCreada publicador;

    public CategoriaService(CategoriaRepository categorias, PublicadorCategoriaCreada publicador) {
        this.categorias = categorias;
        this.publicador = publicador;
    }

    /**
     * Guarda y publica en la misma transacción. Si el broker no confirma,
     * la excepción revierte el INSERT: no queda categoría sin evento.
     * Límite conocido: si el broker aceptó y el timeout corta después, el mensaje
     * puede existir y la fila no. Preferimos ese caso raro antes que un 500 o una fila huérfana.
     */
    @Transactional
    public Categoria crearCategoria(Categoria body) {
        if (body == null || body.getNombre() == null || body.getNombre().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debe indicar nombre");
        }
        body.setId(null);
        body.setNombre(body.getNombre().trim());
        Categoria guardada = categorias.saveAndFlush(body);
        publicador.publicar(new CategoriaCreadaEvento(guardada.getId(), guardada.getNombre()));
        return guardada;
    }
}
