package cl.duoc.ms_inventario.dto;

import java.math.BigDecimal;

import cl.duoc.ms_inventario.model.CategoriaProducto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductoRespuestaDto {

    private Integer id;
    private Integer tiendaId;
    private String nombreTienda; // dato obtenido desde ms-tiendas
    private String nombre;
    private String descripcion;
    private BigDecimal precio;
    private Integer stock;
    private CategoriaProducto categoria;
    private Boolean  activo;

}
