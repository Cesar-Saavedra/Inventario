package cl.duoc.ms_inventario.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cl.duoc.ms_inventario.dto.ActualizarProductoDto;
import cl.duoc.ms_inventario.dto.AgregarProductoDto;
import cl.duoc.ms_inventario.dto.ProductoRespuestaDto;
import cl.duoc.ms_inventario.security.JwtUtil;
import cl.duoc.ms_inventario.service.ProductoServicio;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/productos")
@Tag(name = "Productos", description = "Endpoints para gestionar los productos del catalogo")

public class ProductoControlador {

    @Autowired
    private ProductoServicio productoServicio;

     // Aquí irían los endpoints, por ejemplo:
     // @GetMapping("/tienda/{tiendaId}")
     // public List<ProductoRespuestaDto> obtenerProductosPorTienda(@PathVariable Integer tiendaId, @RequestHeader("Authorization") String authHeader) {
     //     return productoServicio.obtenerProductosPorTienda(tiendaId, authHeader);
     // }

    @Autowired
    private JwtUtil jwtUtil;
@GetMapping("/tienda/{tiendaId}")
    @Operation(summary = "Catálogo público de la tienda", description = "Devuelve los productos activos de una tienda visibles para cualquier usuario autenticado.")
    public ResponseEntity<?> listarCatalogo(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable Integer tiendaId) {

        // Validar que el token sea correcto antes de responder
        String token = validarHeader(authHeader);
        if (token == null) {
            return respuestaNoAutorizado("Token requerido para ver el catalogo.");
        }

        try {
            List<ProductoRespuestaDto> productos = productoServicio.listarCatalogoPorTienda(tiendaId, authHeader);
            return ResponseEntity.ok(productos);
        } catch (RuntimeException e) {
            return respuestaError(e.getMessage());
        }
    }
// =========================================================
    // GET /api/inventario/tienda/{tiendaId}/todos
    // Ver TODOS los productos de la tienda, incluidos los inactivos
    // =========================================================
    /*
     * Header: Authorization: Bearer {token}
     * Solo usuarios con rol TIENDA pueden usar este endpoint.
     *
     * Es la vista de gestion del dueno: ve todos sus productos
     * incluyendo los que estan ocultos (activo=false).
     *
     * Respuesta 200: lista de ProductoRespuestaDto (con activos e inactivos)
     */
    @GetMapping("/tienda/{tiendaId}/todos")
    @Operation(summary = "Listar todos los productos de la tienda, incluidos los inactivos",
               description = "Solo los usuarios con rol TIENDA pueden usar este endpoint." +
               "Es la vista de gestion del dueno: ve todos sus productos incluyendo los que estan ocultos (activo=false).")
    public ResponseEntity<?> listarTodosLosMios(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable Integer tiendaId) {

        String token = validarHeader(authHeader);
        if (token == null) {
            return respuestaNoAutorizado("Token requerido.");
        }

        // Solo el rol TIENDA puede ver el catalogo completo con inactivos
        String rol = jwtUtil.extraerRol(token);
        if (!"TIENDA".equals(rol)) {
            return respuestaNoAutorizado("Solo los usuarios con rol TIENDA pueden ver el catalogo completo.");
        }

        try {
            List<ProductoRespuestaDto> productos =
                    productoServicio.listarTodosPorTienda(tiendaId, authHeader);
            return ResponseEntity.ok(productos);
        } catch (RuntimeException e) {
            return respuestaError(e.getMessage());
        }
    }

    // =========================================================
    // GET /api/inventario/producto/{id}
    // Ver los datos de un producto especifico
    // =========================================================
    /*
     * Header: Authorization: Bearer {token}
     * Path param: id = id del producto
     *
     * Respuesta 200: ProductoRespuestaDto
     * Respuesta 404: si el producto no existe
     *
     * Ejemplo en Postman:
     * GET http://localhost:8085/api/inventario/producto/1
     */
    @GetMapping("/producto/{id}")
    @Operation(summary = "Ver los datos de un producto especifico",
               description = "Requiere un token valido. Respuesta 404 si el producto no existe.")
    public ResponseEntity<?> verProducto(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable Integer id) {

        String token = validarHeader(authHeader);
        if (token == null) {
            return respuestaNoAutorizado("Token requerido.");
        }

        try {
            ProductoRespuestaDto producto = productoServicio.obtenerPorId(id, authHeader);
            return ResponseEntity.ok(producto);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // =========================================================
    // POST /api/inventario/tienda/{tiendaId}
    // Agregar un producto al catalogo de la tienda
    // =========================================================
    /*
     * Header: Authorization: Bearer {token}
     * Path param: tiendaId = id de la tienda duena del producto
     * Body JSON:
     * {
     *   "nombre": "Sobre Charizard ex - Pokemon SV",
     *   "descripcion": "Sobre de 10 cartas de la expansion Scarlet & Violet",
     *   "precio": 5990,
     *   "stock": 100,
     *   "categoria": "SOBRE"
     * }
     *
     * Solo los usuarios con rol TIENDA pueden agregar productos.
     * Respuesta 201: el producto creado con sus datos completos
     */
    @PostMapping("/tienda/{tiendaId}")
    @Operation(summary = "Agregar un producto al catalogo de la tienda",
               description = "Solo los usuarios con rol TIENDA pueden agregar productos. " +
               "Requiere un token valido. Respuesta 201 con el producto creado.")
    public ResponseEntity<?> agregarProducto(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable Integer tiendaId,
            @Valid @RequestBody AgregarProductoDto dto) {

        String token = validarHeader(authHeader);
        if (token == null) {
            return respuestaNoAutorizado("Token requerido para agregar productos.");
        }

        // Verificar que el usuario tenga rol TIENDA
        String rol = jwtUtil.extraerRol(token);
        if (!"TIENDA".equals(rol)) {
            return respuestaNoAutorizado("Solo los usuarios con rol TIENDA pueden agregar productos al catalogo.");
        }

        try {
            // Pasamos authHeader completo porque el servicio lo usa para llamar a ms-tiendas
            ProductoRespuestaDto producto = productoServicio.agregarProducto(tiendaId, dto, authHeader);
            return ResponseEntity.status(HttpStatus.CREATED).body(producto);
        } catch (RuntimeException e) {
            return respuestaError(e.getMessage());
        }
    }

    // =========================================================
    // PUT /api/inventario/producto/{id}/tienda/{tiendaId}
    // Actualizar los datos de un producto existente
    // =========================================================
    /*
     * Header: Authorization: Bearer {token}
     * Path params:
     *   id       = id del producto a actualizar
     *   tiendaId = id de la tienda (para verificar que el producto le pertenece)
     * Body JSON: solo los campos que quieres cambiar
     *
     * Ejemplo en Postman:
     * PUT http://localhost:8085/api/inventario/producto/1/tienda/3
     * Body: { "precio": 4990, "stock": 75 }
     *
     * Respuesta 200: el producto con los datos actualizados
     */
    @PutMapping("/producto/{id}/tienda/{tiendaId}")
    @Operation(summary = "Actualizar los datos de un producto existente",
               description = "Solo los usuarios con rol TIENDA pueden actualizar productos. " +
               "Requiere un token valido. Respuesta 200 con el producto actualizado.")
    public ResponseEntity<?> actualizarProducto(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable Integer id,
            @PathVariable Integer tiendaId,
            @Valid @RequestBody ActualizarProductoDto dto) {

        String token = validarHeader(authHeader);
        if (token == null) {
            return respuestaNoAutorizado("Token requerido.");
        }

        String rol = jwtUtil.extraerRol(token);
        if (!"TIENDA".equals(rol)) {
            return respuestaNoAutorizado("Solo los usuarios con rol TIENDA pueden actualizar productos.");
        }

        try {
            ProductoRespuestaDto actualizado =
                    productoServicio.actualizarProducto(id, dto, tiendaId, authHeader);
            return ResponseEntity.ok(actualizado);
        } catch (RuntimeException e) {
            return respuestaError(e.getMessage());
        }
    }

    // =========================================================
    // DELETE /api/inventario/producto/{id}/tienda/{tiendaId}
    // Desactivar un producto (ocultar del catalogo sin borrarlo)
    // =========================================================
    /*
     * Header: Authorization: Bearer {token}
     * Path params:
     *   id       = id del producto a desactivar
     *   tiendaId = id de la tienda (para verificar que le pertenece)
     *
     * El producto queda con activo=false: sigue en la BD
     * pero ya no aparece en el catalogo publico.
     *
     * Respuesta 200: mensaje de confirmacion
     *
     * Ejemplo en Postman:
     * DELETE http://localhost:8085/api/inventario/producto/1/tienda/3
     */
    @DeleteMapping("/producto/{id}/tienda/{tiendaId}")
    @Operation(summary = "Desactivar un producto (ocultar del catalogo sin borrarlo)",
               description = "Solo los usuarios con rol TIENDA pueden desactivar productos. " +
               "Requiere un token valido. Respuesta 200 con mensaje de confirmacion.")
    public ResponseEntity<?> desactivarProducto(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable Integer id,
            @PathVariable Integer tiendaId) {

        String token = validarHeader(authHeader);
        if (token == null) {
            return respuestaNoAutorizado("Token requerido.");
        }

        String rol = jwtUtil.extraerRol(token);
        if (!"TIENDA".equals(rol)) {
            return respuestaNoAutorizado("Solo los usuarios con rol TIENDA pueden desactivar productos.");
        }

        try {
            productoServicio.desactivarProducto(id, tiendaId);
            Map<String, String> respuesta = new HashMap<>();
            respuesta.put("mensaje", "Producto desactivado. Ya no aparece en el catalogo publico.");
            return ResponseEntity.ok(respuesta);
        } catch (RuntimeException e) {
            return respuestaError(e.getMessage());
        }
    }

    // =========================================================
    // METODOS PRIVADOS DE AYUDA
    // =========================================================

    /*
     * Valida el header Autorizacion y extrae el token limpio.
     * Devuelve el token si es valido, o null si es invalido/falta.
     */
    private String validarHeader(String authHeader) {
        String token = jwtUtil.obtenerTokenDelHeader(authHeader);
        if (token == null || !jwtUtil.esTokenValido(token)) {
            return null;
        }
        return token;
    }

    // Respuesta estandar 401 Unauthorized
    private ResponseEntity<?> respuestaNoAutorizado(String mensaje) {
        Map<String, String> error = new HashMap<>();
        error.put("error", mensaje);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    // Respuesta estandar 400 Bad Request para errores de negocio
    private ResponseEntity<?> respuestaError(String mensaje) {
        Map<String, String> error = new HashMap<>();
        error.put("error", mensaje);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

}
