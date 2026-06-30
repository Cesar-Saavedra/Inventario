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
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/inventario")
@Tag(name = "Productos", description = "Endpoints para gestionar los productos del catalogo")
public class ProductoControlador {

    @Autowired
    private ProductoServicio productoServicio;

    @Autowired
    private JwtUtil jwtUtil;

    @GetMapping("/tienda/{tiendaId}")
    @Operation(summary = "Catálogo público de la tienda", description = "Devuelve los productos activos de una tienda visibles para cualquier usuario autenticado.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Catálogo de productos activos de la tienda"),
            @ApiResponse(responseCode = "401", description = "Token ausente, inválido o expirado")
    })
    public ResponseEntity<?> listarCatalogo(
            @Parameter(description = "Token JWT con formato 'Bearer {token}'", required = true)
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @Parameter(description = "ID de la tienda", required = true, example = "3")
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
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Catálogo completo de la tienda (activos e inactivos)"),
            @ApiResponse(responseCode = "401", description = "Token ausente, inválido, o rol distinto de TIENDA")
    })
    public ResponseEntity<?> listarTodosLosMios(
            @Parameter(description = "Token JWT con formato 'Bearer {token}', debe pertenecer a un usuario con rol TIENDA", required = true)
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @Parameter(description = "ID de la tienda", required = true, example = "3")
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
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Producto encontrado"),
            @ApiResponse(responseCode = "401", description = "Token ausente, inválido o expirado"),
            @ApiResponse(responseCode = "404", description = "El producto no existe")
    })
    public ResponseEntity<?> verProducto(
            @Parameter(description = "Token JWT con formato 'Bearer {token}'", required = true)
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @Parameter(description = "ID del producto", required = true, example = "1")
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
    @PostMapping("/tienda/{tiendaId}")
    @Operation(summary = "Agregar un producto al catalogo de la tienda",
               description = "Solo los usuarios con rol TIENDA pueden agregar productos. " +
               "Requiere un token valido. Respuesta 201 con el producto creado.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Producto creado", content = @Content(
                    examples = @ExampleObject(name = "ProductoCreado", value = """
                            {
                              "id": 1,
                              "nombre": "Sobre Charizard ex - Pokemon SV",
                              "descripcion": "Sobre de 10 cartas de la expansion Scarlet & Violet",
                              "precio": 5990,
                              "stock": 100,
                              "categoria": "SOBRE",
                              "activo": true
                            }
                            """))),
            @ApiResponse(responseCode = "400", description = "Datos inválidos o la tienda no está ACTIVA"),
            @ApiResponse(responseCode = "401", description = "Token ausente, inválido, o rol distinto de TIENDA")
    })
    public ResponseEntity<?> agregarProducto(
            @Parameter(description = "Token JWT con formato 'Bearer {token}', debe pertenecer a un usuario con rol TIENDA", required = true)
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @Parameter(description = "ID de la tienda dueña del producto", required = true, example = "3")
            @PathVariable Integer tiendaId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Datos del producto a agregar", required = true,
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "nombre": "Sobre Charizard ex - Pokemon SV",
                              "descripcion": "Sobre de 10 cartas de la expansion Scarlet & Violet",
                              "precio": 5990,
                              "stock": 100,
                              "categoria": "SOBRE"
                            }
                            """)))
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

        Integer usuarioId = jwtUtil.extraerId(token);

        try {
            // Pasamos authHeader completo porque el servicio lo usa para llamar a ms-tiendas
            ProductoRespuestaDto producto = productoServicio.agregarProducto(tiendaId, dto, usuarioId, authHeader);
            return ResponseEntity.status(HttpStatus.CREATED).body(producto);
        } catch (RuntimeException e) {
            return respuestaError(e.getMessage());
        }
    }

    // =========================================================
    // PUT /api/inventario/producto/{id}/tienda/{tiendaId}
    // Actualizar los datos de un producto existente
    // =========================================================
    @PutMapping("/producto/{id}/tienda/{tiendaId}")
    @Operation(summary = "Actualizar los datos de un producto existente",
               description = "Solo los usuarios con rol TIENDA pueden actualizar productos. " +
               "Requiere un token valido. Respuesta 200 con el producto actualizado.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Producto actualizado correctamente"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos o el producto no pertenece a la tienda indicada"),
            @ApiResponse(responseCode = "401", description = "Token ausente, inválido, o rol distinto de TIENDA")
    })
    public ResponseEntity<?> actualizarProducto(
            @Parameter(description = "Token JWT con formato 'Bearer {token}', debe pertenecer a un usuario con rol TIENDA", required = true)
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @Parameter(description = "ID del producto a actualizar", required = true, example = "1")
            @PathVariable Integer id,
            @Parameter(description = "ID de la tienda (para verificar que el producto le pertenece)", required = true, example = "3")
            @PathVariable Integer tiendaId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Solo los campos que se desean cambiar", required = true,
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "precio": 4990,
                              "stock": 75
                            }
                            """)))
            @Valid @RequestBody ActualizarProductoDto dto) {

        String token = validarHeader(authHeader);
        if (token == null) {
            return respuestaNoAutorizado("Token requerido.");
        }

        String rol = jwtUtil.extraerRol(token);
        if (!"TIENDA".equals(rol)) {
            return respuestaNoAutorizado("Solo los usuarios con rol TIENDA pueden actualizar productos.");
        }

        Integer usuarioId = jwtUtil.extraerId(token);

        try {
            ProductoRespuestaDto actualizado =
                    productoServicio.actualizarProducto(id, dto, tiendaId, usuarioId, authHeader);
            return ResponseEntity.ok(actualizado);
        } catch (RuntimeException e) {
            return respuestaError(e.getMessage());
        }
    }

    // =========================================================
    // DELETE /api/inventario/producto/{id}/tienda/{tiendaId}
    // Desactivar un producto (ocultar del catalogo sin borrarlo)
    // =========================================================
    @DeleteMapping("/producto/{id}/tienda/{tiendaId}")
    @Operation(summary = "Desactivar un producto (ocultar del catalogo sin borrarlo)",
               description = "Solo los usuarios con rol TIENDA pueden desactivar productos. " +
               "Requiere un token valido. Respuesta 200 con mensaje de confirmacion.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Producto desactivado correctamente"),
            @ApiResponse(responseCode = "400", description = "El producto no pertenece a la tienda indicada u otro error de negocio"),
            @ApiResponse(responseCode = "401", description = "Token ausente, inválido, o rol distinto de TIENDA")
    })
    public ResponseEntity<?> desactivarProducto(
            @Parameter(description = "Token JWT con formato 'Bearer {token}', debe pertenecer a un usuario con rol TIENDA", required = true)
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @Parameter(description = "ID del producto a desactivar", required = true, example = "1")
            @PathVariable Integer id,
            @Parameter(description = "ID de la tienda (para verificar que el producto le pertenece)", required = true, example = "3")
            @PathVariable Integer tiendaId) {

        String token = validarHeader(authHeader);
        if (token == null) {
            return respuestaNoAutorizado("Token requerido.");
        }

        String rol = jwtUtil.extraerRol(token);
        if (!"TIENDA".equals(rol)) {
            return respuestaNoAutorizado("Solo los usuarios con rol TIENDA pueden desactivar productos.");
        }

        Integer usuarioId = jwtUtil.extraerId(token);

        try {
            productoServicio.desactivarProducto(id, tiendaId, usuarioId, authHeader);
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
