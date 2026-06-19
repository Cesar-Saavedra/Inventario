package cl.duoc.ms_inventario.dto;

import java.math.BigDecimal;

import cl.duoc.ms_inventario.model.CategoriaProducto;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

@Data
public class ActualizarProductoDto {

    private String nombre;
    private String  descripcion;

    @DecimalMin(value = "0.0", inclusive = false, message = "El precio debe ser mayor que 0")
    private BigDecimal precio;

    @PositiveOrZero(message = "El stock no puede ser negativo")
    private Integer stock;

    private CategoriaProducto categoria;
}
