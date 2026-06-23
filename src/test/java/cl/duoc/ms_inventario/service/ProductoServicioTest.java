package cl.duoc.ms_inventario.service;

/*
 * NOTA IMPORTANTE:
 *
 * Todos los metodos publicos de ProductoServicio (agregarProducto,
 * listarCatalogoPorTienda, listarTodosPorTienda, obtenerPorId,
 * actualizarProducto, desactivarProducto) dependen internamente de
 * TiendaFeignClient para verificar/consultar datos de ms-tiendas.
 *
 * Por decision del equipo, las pruebas unitarias de este proyecto NO
 * deben requerir invocar (ni siquiera mockeada) la logica que solo
 * existe para comunicarse con otro microservicio. Como no hay ningun
 * metodo en este servicio libre de esa dependencia, este servicio
 * queda sin pruebas unitarias propias; la cobertura de sus reglas de
 * negocio (validaciones, errores) se realiza en ProductoControladorTest,
 * donde ProductoServicio se mockea por completo.
 */
public class ProductoServicioTest {
}
