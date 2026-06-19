package cl.duoc.ms_inventario.dto;

import java.math.BigDecimal;

import cl.duoc.ms_inventario.model.CategoriaProducto;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

@Data
public class AgregarProductoDto {

    @NotBlank(message = "El nombre del producto es obligatorio")
    private String nombre;

    private String  descripcion;

    @NotNull(message = "El precio es obligatorio")
    @DecimalMin(value = "0.0", inclusive = false, message = "El precio debe ser mayor que 0")
    private BigDecimal  precio;

    @NotNull(message = "El stock es obligatorio")
    @PositiveOrZero(message = "El stock no puede ser negativo")
    private Integer  stock;

    @NotNull(message = "La categoria es obligatoria")
    private CategoriaProducto categoria;

}
