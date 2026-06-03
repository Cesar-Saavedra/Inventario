package cl.duoc.ms_inventario.model;

import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;



@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "productos")
public class Producto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "ID unico del producto", example = "1")
    private Integer id;

    @Column(nullable=false)
    @Schema(description = "ID de la tienda a la que pertenece el producto", example = "3")
    private Integer tiendaId;

    @Column(nullable=false)
    @Schema(description = "Nombre del producto", example = "Baraja de Magic}")
    private String nombre;

    @Column(nullable=false)
    @Schema(description = "Descripcion del producto", example = "Baraja de Magic: The Gathering, edicion Core Set 2021, con 60 cartas.")
    private String descripcion;

    @Column(nullable=false, precision=10, scale=0)
    
    private BigDecimal precio;

    @Column(nullable=false)
    private Integer stock = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable=false)
    private CategoriaProducto categoria;

    @Column(nullable=false)
    private Boolean activo = true;

}
