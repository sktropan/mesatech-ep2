package cl.duoc.mesatech.solicitudes.web;

import cl.duoc.mesatech.solicitudes.domain.CategoriaCatalogo;
import cl.duoc.mesatech.solicitudes.service.CopiaCategoriaService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class CategoriaCatalogoController {

    private final CopiaCategoriaService copias;

    public CategoriaCatalogoController(CopiaCategoriaService copias) {
        this.copias = copias;
    }

    @GetMapping("/v1/categorias-catalogo")
    public List<CategoriaCatalogo> listar() {
        return copias.listar();
    }
}
