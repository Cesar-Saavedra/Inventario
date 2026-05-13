package cl.duoc.ms_inventario.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import cl.duoc.ms_inventario.dto.ActualizarProductoDto;
import cl.duoc.ms_inventario.dto.AgregarProductoDto;
import cl.duoc.ms_inventario.dto.ProductoRespuestaDto;
import cl.duoc.ms_inventario.model.Producto;
import cl.duoc.ms_inventario.repository.ProductoRepositorio;



@Service
public class ProductoServicio {

    @Autowired
    private ProductoRepositorio productoRepositorio;
    
    @Autowired
    private RestTemplate restTemplate;

    @Value("{ms.tiendas.url}")
    private String urlMsTiendas;


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
    public ProductoRespuestaDto agregarProducto(Integer tiendaId, AgregarProductoDto dto, String authHeader) {

        // Paso 1: consultar ms-tiendas para verificar que la tienda exista y este activa
        Map<String, Object> datosTienda = consultarResumenTienda(tiendaId, authHeader);
        if (datosTienda == null) {
            throw new RuntimeException("No se encontro la tienda con id: " + tiendaId
                    + ". Verifica que ms-tiendas este corriendo.");
        }

        // Verificar que la tienda este en estado ACTIVA
        // No se pueden agregar productos a tiendas PENDIENTES o INACTIVAS
        String estadoTienda = (String) datosTienda.get("estado");
        if (!"ACTIVA".equals(estadoTienda)) {
            throw new RuntimeException("La tienda no esta activa. Estado actual: " + estadoTienda
                + ". Solo las tiendas ACTIVAS pueden tener productos en catalogo.");
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
        String nombreTienda = (String) datosTienda.get("nombre");
        return construirRespuesta(guardado, nombreTienda);
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
        Map<String, Object> datosTienda = consultarResumenTienda(tiendaId, authHeader);
        String nombreTienda = datosTienda != null
                ? (String) datosTienda.get("nombre")
                : "Tienda desconocida";

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

        Map<String, Object> datosTienda = consultarResumenTienda(tiendaId, authHeader);
        String nombreTienda = datosTienda != null
                ? (String) datosTienda.get("nombre")
                : "Tienda desconocida";

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
        Map<String, Object> datosTienda = consultarResumenTienda(producto.getTiendaId(), authHeader);
        String nombreTienda = datosTienda != null
                ? (String) datosTienda.get("nombre")
                : "Tienda desconocida";

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
                                                    Integer tiendaId, String authHeader) {

        // Buscar el producto en la BD
        Producto producto = productoRepositorio.findById(id)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado con id: " + id));

        // Verificar que el producto pertenezca a la tienda del usuario
        if (!producto.getTiendaId().equals(tiendaId)) {
            throw new RuntimeException("Este producto no pertenece a tu tienda.");
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
        Map<String, Object> datosTienda = consultarResumenTienda(producto.getTiendaId(), authHeader);
        String nombreTienda = datosTienda != null
                ? (String) datosTienda.get("nombre")
                : "Tienda desconocida";

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
     * Se usa DELETE en la URL porque semanticamente estamos
     * "eliminando" el producto del catalogo visible.
     *
     * @param id       id del producto
     * @param tiendaId id de la tienda (para verificar que le pertenece)
     */
    public void desactivarProducto(Integer id, Integer tiendaId) {

        Producto producto = productoRepositorio.findById(id)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado con id: " + id));

        // Verificar que el producto pertenece a la tienda del usuario autenticado
        if (!producto.getTiendaId().equals(tiendaId)) {
            throw new RuntimeException("Este producto no pertenece a tu tienda.");
        }

        // Desactivar sin borrar de la BD
        producto.setActivo(false);
        productoRepositorio.save(producto);
    }

    // =========================================================
    // METODOS PRIVADOS DE AYUDA
    // =========================================================

    /*
     * Llama a ms-tiendas para obtener el resumen de una tienda.
     *
     * Usa RestTemplate para hacer una peticion HTTP GET.
     * Envia el token JWT en el header Authorization para que
     * ms-tiendas sepa que somos un microservicio autorizado.
     *
     * Devuelve un Map con los datos de la tienda:
     *   { "id": 3, "nombre": "Carta Magica TCG",
     *     "horarioAtencion": "...", "estado": "ACTIVA" }
     *
     * Devuelve null si la tienda no existe o ms-tiendas no responde.
     *
     * @param tiendaId    id de la tienda a consultar
     * @param authHeader  header completo "Bearer eyJ..." para la peticion
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> consultarResumenTienda(Integer tiendaId, String authHeader) {
        try {
            // Construir la URL completa del endpoint de resumen en ms-tiendas
            String url = urlMsTiendas + "/api/tiendas/" + tiendaId + "/resumen";

            // Crear los headers de la peticion HTTP con el token JWT
            // Sin este header, ms-tiendas puede rechazar la peticion
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", authHeader); // "Bearer eyJ..."

            // Crear la entidad HTTP (solo headers, sin body porque es GET)
            HttpEntity<Void> peticion = new HttpEntity<>(headers);

            // Hacer la peticion GET y mapear la respuesta JSON a un Map de Java
            // Map.class le dice a RestTemplate que convierta el JSON a Map<String,Object>
            ResponseEntity<Map> respuesta = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    peticion,
                    Map.class
            );

            return respuesta.getBody();

        } catch (Exception e) {
            // Si ms-tiendas no responde (no esta corriendo, timeout, etc.)
            // devolvemos null para que el metodo que llama decida que hacer
            System.out.println("[ms-inventario] No se pudo consultar ms-tiendas para tienda "
                    + tiendaId + ": " + e.getMessage());
            return null;
        }
    }

    /*
     * Convierte una entidad Producto al DTO de respuesta.
     * Recibe el nombreTienda por parametro porque viene de ms-tiendas,
     * no de esta BD.
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