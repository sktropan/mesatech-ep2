package cl.duoc.mesatech.catalogo.web;

import cl.duoc.mesatech.catalogo.domain.Categoria;
import cl.duoc.mesatech.catalogo.domain.Prioridad;
import cl.duoc.mesatech.catalogo.repo.CategoriaRepository;
import cl.duoc.mesatech.catalogo.repo.PrioridadRepository;
import cl.duoc.mesatech.catalogo.service.CategoriaService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class CatalogoController {

    private final CategoriaRepository categorias;
    private final PrioridadRepository prioridades;
    private final CategoriaService categoriaService;

    public CatalogoController(
            CategoriaRepository categorias,
            PrioridadRepository prioridades,
            CategoriaService categoriaService
    ) {
        this.categorias = categorias;
        this.prioridades = prioridades;
        this.categoriaService = categoriaService;
    }

    @GetMapping("/v1/catalogo")
    public Map<String, Object> listar() {
        Map<String, Object> respuesta = new LinkedHashMap<>();
        respuesta.put("categorias", categorias.findAll());
        respuesta.put("prioridades", prioridades.findAll());
        return respuesta;
    }

    @PostMapping("/v1/catalogo/categorias")
    public Categoria crearCategoria(@RequestBody Categoria body) {
        return categoriaService.crearCategoria(body);
    }

    @PostMapping("/v1/catalogo/prioridades")
    public Prioridad crearPrioridad(@RequestBody Prioridad body) {
        body.setId(null);
        return prioridades.save(body);
    }

    @PutMapping("/v1/catalogo/categorias/{id}")
    public Categoria actualizarCategoria(@PathVariable Long id, @RequestBody Categoria body) {
        Categoria existente = categorias.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (body.getNombre() == null || body.getNombre().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debe indicar nombre");
        }
        existente.setNombre(body.getNombre().trim());
        return categorias.save(existente);
    }

    @DeleteMapping("/v1/catalogo/categorias/{id}")
    public void eliminarCategoria(@PathVariable Long id) {
        if (!categorias.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        categorias.deleteById(id);
    }

    @PutMapping("/v1/catalogo/prioridades/{id}")
    public Prioridad actualizarPrioridad(@PathVariable Long id, @RequestBody Prioridad body) {
        Prioridad existente = prioridades.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (body.getNombre() == null || body.getNombre().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debe indicar nombre");
        }
        if (body.getNivel() == null || body.getNivel() < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debe indicar nivel válido");
        }
        existente.setNombre(body.getNombre().trim());
        existente.setNivel(body.getNivel());
        return prioridades.save(existente);
    }

    @DeleteMapping("/v1/catalogo/prioridades/{id}")
    public void eliminarPrioridad(@PathVariable Long id) {
        if (!prioridades.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        prioridades.deleteById(id);
    }
}
