package cl.duoc.ms_inventario.model;

import java.math.BigDecimal;

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
    private Integer id;

    @Column(nullable=false)
    private String tiendaId;

    @Column(nullable=false)
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
