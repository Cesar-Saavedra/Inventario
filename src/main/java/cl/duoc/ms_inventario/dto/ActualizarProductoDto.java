package cl.duoc.ms_inventario.dto;

import java.math.BigDecimal;

import cl.duoc.ms_inventario.model.CategoriaProducto;
import lombok.Data;

@Data
public class ActualizarProductoDto {

    private String nombre;
    private String  descripcion;
    private BigDecimal precio;
    private Integer stock;
    private CategoriaProducto categoria;
}
