package cl.duoc.ms_inventario.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import cl.duoc.ms_inventario.client.TiendaFeignClient;
import cl.duoc.ms_inventario.dto.ActualizarProductoDto;
import cl.duoc.ms_inventario.dto.AgregarProductoDto;
import cl.duoc.ms_inventario.dto.ProductoRespuestaDto;
import cl.duoc.ms_inventario.dto.TiendaResumenDTO;
import cl.duoc.ms_inventario.model.Producto;
import cl.duoc.ms_inventario.repository.ProductoRepositorio;


@Service
public class ProductoServicio {

    @Autowired
    private ProductoRepositorio productoRepositorio;

    // Cliente Feign para consultar ms-tiendas via Eureka (sin URL hardcodeada)
    @Autowired
    private TiendaFeignClient tiendaFeignClient;


    // =========================================================
    // AGREGAR PRODUCTO AL CATALOGO
    // =========================================================

    /*
     * Agrega un producto nuevo al catalogo de una tienda.
     *
     * Proceso:
     * 1. Llama a ms-tiendas para verificar que la tienda existe y esta ACTIVA.
     * 2. Valida los datos obligatorios del formulario.
     * 3. Guarda el producto en la BD con activo = true.
     * 4. Devuelve el producto creado con el nombre de la tienda incluido.
     *
     * @param tiendaId    id de la tienda duena del producto (viene de la URL)
     * @param dto         datos del formulario (nombre, precio, stock, etc.)
     * @param authHeader  el header completo "Bearer eyJ..." para llamar a ms-tiendas
     */
    public ProductoRespuestaDto agregarProducto(Integer tiendaId, AgregarProductoDto dto,
                                                 Integer usuarioId, String authHeader) {

        // Paso 1: consultar ms-tiendas para verificar que la tienda exista y este activa
        TiendaResumenDTO datosTienda = consultarResumenTienda(tiendaId, authHeader);
        if (datosTienda == null) {
            throw new RuntimeException("No se encontro la tienda con id: " + tiendaId
                    + ". Verifica que ms-tiendas este corriendo.");
        }

        // Verificar que la tienda este en estado ACTIVA
        // No se pueden agregar productos a tiendas PENDIENTES o INACTIVAS
        if (!"ACTIVA".equals(datosTienda.getEstado())) {
            throw new RuntimeException("La tienda no esta activa. Estado actual: " + datosTienda.getEstado()
                + ". Solo las tiendas ACTIVAS pueden tener productos en catalogo.");
        }

        // Verificar que la tienda de la URL realmente pertenezca al usuario autenticado
        // (el id de la tienda NO es el mismo que el id del usuario dueno)
        if (!esTiendaDelUsuario(tiendaId, usuarioId, authHeader)) {
            throw new RuntimeException("No tienes permiso para agregar productos a esta tienda.");
        }

        // Paso 2: validar datos obligatorios del formulario
        if (dto.getNombre() == null || dto.getNombre().isBlank()) {
            throw new RuntimeException("El nombre del producto es obligatorio.");
        }
        if (dto.getPrecio() == null || dto.getPrecio().intValue() <= 0) {
            throw new RuntimeException("El precio debe ser un numero mayor a cero.");
        }
        if (dto.getCategoria() == null) {
            throw new RuntimeException("Debes seleccionar una categoria para el producto.");
        }

        // Paso 3: crear el objeto Producto y guardarlo en la BD
        Producto nuevo = new Producto();
        nuevo.setTiendaId(tiendaId);
        nuevo.setNombre(dto.getNombre());
        nuevo.setDescripcion(dto.getDescripcion());
        nuevo.setPrecio(dto.getPrecio());
        // Si no se indica stock, empieza en 0
        nuevo.setStock(dto.getStock() != null ? dto.getStock() : 0);
        nuevo.setCategoria(dto.getCategoria());
        nuevo.setActivo(true); // todo producto nuevo empieza activo

        Producto guardado = productoRepositorio.save(nuevo);

        // Paso 4: armar respuesta incluyendo el nombre de la tienda
        return construirRespuesta(guardado, datosTienda.getNombre());
    }

    // =========================================================
    // LISTAR CATALOGO ACTIVO DE UNA TIENDA (vista publica)
    // =========================================================

    /*
     * Devuelve todos los productos con activo=true de una tienda.
     * Es la vista que ven los jugadores al entrar a una tienda.
     *
     * @param tiendaId    id de la tienda
     * @param authHeader  header completo "Bearer eyJ..." para llamar a ms-tiendas
     */
    public List<ProductoRespuestaDto> listarCatalogoPorTienda(Integer tiendaId, String authHeader) {

        // Consultar el nombre de la tienda para mostrarlo en cada producto
        TiendaResumenDTO datosTienda = consultarResumenTienda(tiendaId, authHeader);
        String nombreTienda = datosTienda != null ? datosTienda.getNombre() : "Tienda desconocida";

        // Traer solo los productos activos (los que ven los compradores)
        List<Producto> productos = productoRepositorio.findByTiendaIdAndActivo(tiendaId, true);

        // Convertir cada entidad Producto a su DTO de respuesta
        List<ProductoRespuestaDto> listaRespuesta = new ArrayList<>();
        for (Producto producto : productos) {
            listaRespuesta.add(construirRespuesta(producto, nombreTienda));
        }

        return listaRespuesta;
    }

    // =========================================================
    // LISTAR TODOS LOS PRODUCTOS DE UNA TIENDA (vista del dueno)
    // =========================================================

    /*
     * Devuelve TODOS los productos de una tienda, activos e inactivos.
     * Solo el dueno los usa para gestionar su catalogo completo.
     *
     * @param tiendaId    id de la tienda
     * @param authHeader  header completo para llamar a ms-tiendas
     */
    public List<ProductoRespuestaDto> listarTodosPorTienda(Integer tiendaId, String authHeader) {

        TiendaResumenDTO datosTienda = consultarResumenTienda(tiendaId, authHeader);
        String nombreTienda = datosTienda != null ? datosTienda.getNombre() : "Tienda desconocida";

        // Traer todos sin filtrar por activo
        List<Producto> productos = productoRepositorio.findByTiendaId(tiendaId);

        List<ProductoRespuestaDto> listaRespuesta = new ArrayList<>();
        for (Producto producto : productos) {
            listaRespuesta.add(construirRespuesta(producto, nombreTienda));
        }

        return listaRespuesta;
    }

    // =========================================================
    // VER UN PRODUCTO POR ID
    // =========================================================

    /*
     * Devuelve los datos completos de un producto especifico.
     *
     * @param id          id del producto
     * @param authHeader  header para enriquecer con nombre de tienda
     */
    public ProductoRespuestaDto obtenerPorId(Integer id, String authHeader) {

        // Buscar el producto en la BD, lanzar error si no existe
        Producto producto = productoRepositorio.findById(id)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado con id: " + id));

        // Consultar el nombre de la tienda para la respuesta
        TiendaResumenDTO datosTienda = consultarResumenTienda(producto.getTiendaId(), authHeader);
        String nombreTienda = datosTienda != null ? datosTienda.getNombre() : "Tienda desconocida";

        return construirRespuesta(producto, nombreTienda);
    }

    // =========================================================
    // ACTUALIZAR DATOS DE UN PRODUCTO
    // =========================================================

    /*
     * Actualiza los datos de un producto existente.
     * Solo actualiza los campos que llegaron con valor (no nulos).
     *
     * @param id          id del producto a actualizar
     * @param dto         campos a actualizar (solo los que cambiaron)
     * @param tiendaId    id de la tienda (para verificar que el producto le pertenece)
     * @param authHeader  header para llamar a ms-tiendas
     */
    public ProductoRespuestaDto actualizarProducto(Integer id, ActualizarProductoDto dto,
                                                    Integer tiendaId, Integer usuarioId, String authHeader) {

        // Buscar el producto en la BD
        Producto producto = productoRepositorio.findById(id)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado con id: " + id));

        // Verificar que el producto pertenezca a la tienda indicada
        if (!producto.getTiendaId().equals(tiendaId)) {
            throw new RuntimeException("Este producto no pertenece a tu tienda.");
        }

        // Verificar que esa tienda realmente pertenezca al usuario autenticado
        // (el id de la tienda NO es el mismo que el id del usuario dueno)
        if (!esTiendaDelUsuario(tiendaId, usuarioId, authHeader)) {
            throw new RuntimeException("No tienes permiso para modificar productos de esta tienda.");
        }

        // Actualizar solo los campos que llegaron con valor
        // Si un campo es null, significa que el usuario no quiere cambiarlo
        if (dto.getNombre() != null && !dto.getNombre().isBlank()) {
            producto.setNombre(dto.getNombre());
        }
        if (dto.getDescripcion() != null) {
            producto.setDescripcion(dto.getDescripcion());
        }
        if (dto.getPrecio() != null) {
            producto.setPrecio(dto.getPrecio());
        }
        if (dto.getStock() != null) {
            producto.setStock(dto.getStock());
        }
        if (dto.getCategoria() != null) {
            producto.setCategoria(dto.getCategoria());
        }

        Producto actualizado = productoRepositorio.save(producto);

        // Consultar nombre de la tienda para la respuesta
        TiendaResumenDTO datosTienda = consultarResumenTienda(producto.getTiendaId(), authHeader);
        String nombreTienda = datosTienda != null ? datosTienda.getNombre() : "Tienda desconocida";

        return construirRespuesta(actualizado, nombreTienda);
    }

    // =========================================================
    // DESACTIVAR PRODUCTO (no se borra, solo se oculta)
    // =========================================================

    /*
     * Pone activo=false en el producto.
     * El producto deja de aparecer en el catalogo publico.
     * Sigue en la BD y el dueno lo puede ver desde su gestion.
     *
     * @param id       id del producto
     * @param tiendaId id de la tienda (para verificar que le pertenece)
     */
    public void desactivarProducto(Integer id, Integer tiendaId, Integer usuarioId, String authHeader) {

        Producto producto = productoRepositorio.findById(id)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado con id: " + id));

        // Verificar que el producto pertenece a la tienda indicada
        if (!producto.getTiendaId().equals(tiendaId)) {
            throw new RuntimeException("Este producto no pertenece a tu tienda.");
        }

        // Verificar que esa tienda realmente pertenezca al usuario autenticado
        // (el id de la tienda NO es el mismo que el id del usuario dueno)
        if (!esTiendaDelUsuario(tiendaId, usuarioId, authHeader)) {
            throw new RuntimeException("No tienes permiso para desactivar productos de esta tienda.");
        }

        // Desactivar sin borrar de la BD
        producto.setActivo(false);
        productoRepositorio.save(producto);
    }

    // =========================================================
    // METODOS PRIVADOS DE AYUDA
    // =========================================================

    /*
     * Llama a ms-tiendas via Feign para obtener el resumen de una tienda.
     * Feign usa Eureka para resolver "ms-tiendas" sin URL hardcodeada.
     *
     * Devuelve null si la tienda no existe o ms-tiendas no responde.
     *
     * @param tiendaId    id de la tienda a consultar
     * @param authHeader  header completo "Bearer eyJ..." para la peticion
     */
    private TiendaResumenDTO consultarResumenTienda(Integer tiendaId, String authHeader) {
        try {
            return tiendaFeignClient.obtenerResumenTienda(tiendaId, authHeader);
        } catch (Exception e) {
            System.out.println("[ms-inventario] No se pudo consultar ms-tiendas para tienda "
                    + tiendaId + ": " + e.getMessage());
            return null;
        }
    }

    /*
     * Verifica si una tienda (por su id real en ms-tiendas) pertenece
     * al usuario autenticado. El id de la tienda es distinto del id
     * del usuario dueno, por lo que hay que resolverlo consultando
     * ms-tiendas en lugar de confiar en el tiendaId que llega por la URL.
     *
     * @param tiendaId   id de la tienda indicada en la URL
     * @param usuarioId  id del usuario autenticado (viene del token)
     * @param authHeader header completo "Bearer eyJ..." para la peticion
     */
    private boolean esTiendaDelUsuario(Integer tiendaId, Integer usuarioId, String authHeader) {
        try {
            List<TiendaResumenDTO> tiendasDelUsuario =
                    tiendaFeignClient.obtenerTiendasPorDueno(usuarioId, authHeader);

            if (tiendasDelUsuario == null) {
                return false;
            }

            for (TiendaResumenDTO tienda : tiendasDelUsuario) {
                if (tienda.getId() != null && tienda.getId().equals(tiendaId)) {
                    return true;
                }
            }
            return false;

        } catch (Exception e) {
            System.out.println("[ms-inventario] No se pudo verificar el dueno de la tienda "
                    + tiendaId + ": " + e.getMessage());
            return false;
        }
    }

    /*
     * Convierte una entidad Producto al DTO de respuesta.
     * Recibe el nombreTienda por parametro porque viene de ms-tiendas.
     */
    private ProductoRespuestaDto construirRespuesta(Producto producto, String nombreTienda) {
        ProductoRespuestaDto respuesta = new ProductoRespuestaDto();
        respuesta.setId(producto.getId());
        respuesta.setTiendaId(producto.getTiendaId());
        respuesta.setNombreTienda(nombreTienda);
        respuesta.setNombre(producto.getNombre());
        respuesta.setDescripcion(producto.getDescripcion());
        respuesta.setPrecio(producto.getPrecio());
        respuesta.setStock(producto.getStock());
        respuesta.setCategoria(producto.getCategoria());
        respuesta.setActivo(producto.getActivo());
        return respuesta;
    }
}
