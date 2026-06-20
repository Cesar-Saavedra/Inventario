package cl.duoc.ms_inventario.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/*
 * DTO que representa el resumen de una tienda recibido desde ms-tiendas.
 * Solo contiene los campos que ms-inventario necesita: id, nombre y estado.
 * El estado se usa para validar que la tienda este ACTIVA antes de agregar productos.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TiendaResumenDTO {

    private Integer id;
    private String nombre;
    private String horarioAtencion;
    private String estado;
}
