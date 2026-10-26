package cl.duoc.mesatech.solicitudes.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "categorias_catalogo")
@Getter
@Setter
public class CategoriaCatalogo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "catalogo_id", nullable = false, unique = true)
    private Long catalogoId;

    @Column(nullable = false)
    private String nombre;
}
