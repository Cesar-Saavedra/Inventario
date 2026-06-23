package cl.duoc.ms_inventario.controller;

import java.math.BigDecimal;
import java.util.Arrays;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cl.duoc.ms_inventario.dto.ActualizarProductoDto;
import cl.duoc.ms_inventario.dto.AgregarProductoDto;
import cl.duoc.ms_inventario.dto.ProductoRespuestaDto;
import cl.duoc.ms_inventario.model.CategoriaProducto;
import cl.duoc.ms_inventario.security.JwtUtil;
import cl.duoc.ms_inventario.service.ProductoServicio;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(ProductoControlador.class)
public class ProductoControladorTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductoServicio productoServicio;

    @MockitoBean
    private JwtUtil jwtUtil;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private ProductoRespuestaDto productoEjemplo;

    @BeforeEach
    void setUp(){
        productoEjemplo = new ProductoRespuestaDto(1, 3, "Carta Magica TCG", "Sobre Charizard ex",
                "Sobre de 10 cartas", new BigDecimal("5990"), 100, CategoriaProducto.SOBRE, true);
    }

    // =====================================================================
    // GET /api/productos/tienda/{tiendaId}
    // =====================================================================

    @Test
    void listarCatalogo_sinToken_retorna401() throws Exception {
        mockMvc.perform(get("/api/productos/tienda/3"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listarCatalogo_retorna200() throws Exception {
        when(jwtUtil.obtenerTokenDelHeader("Bearer token-bueno")).thenReturn("token-bueno");
        when(jwtUtil.esTokenValido("token-bueno")).thenReturn(true);
        when(productoServicio.listarCatalogoPorTienda(eq(3), anyString())).thenReturn(Arrays.asList(productoEjemplo));

        mockMvc.perform(get("/api/productos/tienda/3").header("Authorization", "Bearer token-bueno"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("Sobre Charizard ex"));
    }

    // =====================================================================
    // GET /api/productos/tienda/{tiendaId}/todos
    // =====================================================================

    @Test
    void listarTodosLosMios_sinToken_retorna401() throws Exception {
        mockMvc.perform(get("/api/productos/tienda/3/todos"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listarTodosLosMios_rolNoEsTienda_retorna401() throws Exception {
        when(jwtUtil.obtenerTokenDelHeader("Bearer token-jugador")).thenReturn("token-jugador");
        when(jwtUtil.esTokenValido("token-jugador")).thenReturn(true);
        when(jwtUtil.extraerRol("token-jugador")).thenReturn("JUGADOR");

        mockMvc.perform(get("/api/productos/tienda/3/todos").header("Authorization", "Bearer token-jugador"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listarTodosLosMios_retorna200() throws Exception {
        when(jwtUtil.obtenerTokenDelHeader("Bearer token-tienda")).thenReturn("token-tienda");
        when(jwtUtil.esTokenValido("token-tienda")).thenReturn(true);
        when(jwtUtil.extraerRol("token-tienda")).thenReturn("TIENDA");
        when(productoServicio.listarTodosPorTienda(eq(3), anyString())).thenReturn(Arrays.asList(productoEjemplo));

        mockMvc.perform(get("/api/productos/tienda/3/todos").header("Authorization", "Bearer token-tienda"))
                .andExpect(status().isOk());
    }

    // =====================================================================
    // GET /api/productos/producto/{id}
    // =====================================================================

    @Test
    void verProducto_sinToken_retorna401() throws Exception {
        mockMvc.perform(get("/api/productos/producto/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void verProducto_encontrado_retorna200() throws Exception {
        when(jwtUtil.obtenerTokenDelHeader("Bearer token-bueno")).thenReturn("token-bueno");
        when(jwtUtil.esTokenValido("token-bueno")).thenReturn(true);
        when(productoServicio.obtenerPorId(1, "Bearer token-bueno")).thenReturn(productoEjemplo);

        mockMvc.perform(get("/api/productos/producto/1").header("Authorization", "Bearer token-bueno"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void verProducto_noEncontrado_retorna404() throws Exception {
        when(jwtUtil.obtenerTokenDelHeader("Bearer token-bueno")).thenReturn("token-bueno");
        when(jwtUtil.esTokenValido("token-bueno")).thenReturn(true);
        when(productoServicio.obtenerPorId(99, "Bearer token-bueno"))
                .thenThrow(new RuntimeException("Producto no encontrado con id: 99"));

        mockMvc.perform(get("/api/productos/producto/99").header("Authorization", "Bearer token-bueno"))
                .andExpect(status().isNotFound());
    }

    // =====================================================================
    // POST /api/productos/tienda/{tiendaId}
    // =====================================================================

    @Test
    void agregarProducto_sinToken_retorna401() throws Exception {
        AgregarProductoDto dto = new AgregarProductoDto();
        dto.setNombre("Sobre Charizard ex");
        dto.setPrecio(new BigDecimal("5990"));
        dto.setStock(100);
        dto.setCategoria(CategoriaProducto.SOBRE);

        mockMvc.perform(post("/api/productos/tienda/3")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void agregarProducto_rolNoEsTienda_retorna401() throws Exception {
        AgregarProductoDto dto = new AgregarProductoDto();
        dto.setNombre("Sobre Charizard ex");
        dto.setPrecio(new BigDecimal("5990"));
        dto.setStock(100);
        dto.setCategoria(CategoriaProducto.SOBRE);

        when(jwtUtil.obtenerTokenDelHeader("Bearer token-jugador")).thenReturn("token-jugador");
        when(jwtUtil.esTokenValido("token-jugador")).thenReturn(true);
        when(jwtUtil.extraerRol("token-jugador")).thenReturn("JUGADOR");

        mockMvc.perform(post("/api/productos/tienda/3")
                        .header("Authorization", "Bearer token-jugador")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void agregarProducto_exitoso_retorna201() throws Exception {
        AgregarProductoDto dto = new AgregarProductoDto();
        dto.setNombre("Sobre Charizard ex");
        dto.setPrecio(new BigDecimal("5990"));
        dto.setStock(100);
        dto.setCategoria(CategoriaProducto.SOBRE);

        when(jwtUtil.obtenerTokenDelHeader("Bearer token-tienda")).thenReturn("token-tienda");
        when(jwtUtil.esTokenValido("token-tienda")).thenReturn(true);
        when(jwtUtil.extraerRol("token-tienda")).thenReturn("TIENDA");
        when(jwtUtil.extraerId("token-tienda")).thenReturn(5);
        when(productoServicio.agregarProducto(eq(3), any(AgregarProductoDto.class), eq(5), anyString()))
                .thenReturn(productoEjemplo);

        mockMvc.perform(post("/api/productos/tienda/3")
                        .header("Authorization", "Bearer token-tienda")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void agregarProducto_errorDeNegocio_retorna400() throws Exception {
        AgregarProductoDto dto = new AgregarProductoDto();
        dto.setNombre("Sobre Charizard ex");
        dto.setPrecio(new BigDecimal("5990"));
        dto.setStock(100);
        dto.setCategoria(CategoriaProducto.SOBRE);

        when(jwtUtil.obtenerTokenDelHeader("Bearer token-tienda")).thenReturn("token-tienda");
        when(jwtUtil.esTokenValido("token-tienda")).thenReturn(true);
        when(jwtUtil.extraerRol("token-tienda")).thenReturn("TIENDA");
        when(jwtUtil.extraerId("token-tienda")).thenReturn(5);
        when(productoServicio.agregarProducto(eq(3), any(AgregarProductoDto.class), eq(5), anyString()))
                .thenThrow(new RuntimeException("No tienes permiso para agregar productos a esta tienda."));

        mockMvc.perform(post("/api/productos/tienda/3")
                        .header("Authorization", "Bearer token-tienda")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    // =====================================================================
    // PUT /api/productos/producto/{id}/tienda/{tiendaId}
    // =====================================================================

    @Test
    void actualizarProducto_sinToken_retorna401() throws Exception {
        ActualizarProductoDto dto = new ActualizarProductoDto();
        dto.setPrecio(new BigDecimal("4990"));

        mockMvc.perform(put("/api/productos/producto/1/tienda/3")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void actualizarProducto_exitoso_retorna200() throws Exception {
        ActualizarProductoDto dto = new ActualizarProductoDto();
        dto.setPrecio(new BigDecimal("4990"));

        when(jwtUtil.obtenerTokenDelHeader("Bearer token-tienda")).thenReturn("token-tienda");
        when(jwtUtil.esTokenValido("token-tienda")).thenReturn(true);
        when(jwtUtil.extraerRol("token-tienda")).thenReturn("TIENDA");
        when(jwtUtil.extraerId("token-tienda")).thenReturn(5);
        when(productoServicio.actualizarProducto(eq(1), any(ActualizarProductoDto.class), eq(3), eq(5), anyString()))
                .thenReturn(productoEjemplo);

        mockMvc.perform(put("/api/productos/producto/1/tienda/3")
                        .header("Authorization", "Bearer token-tienda")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());
    }

    // =====================================================================
    // DELETE /api/productos/producto/{id}/tienda/{tiendaId}
    // =====================================================================

    @Test
    void desactivarProducto_sinToken_retorna401() throws Exception {
        mockMvc.perform(delete("/api/productos/producto/1/tienda/3"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void desactivarProducto_exitoso_retorna200() throws Exception {
        when(jwtUtil.obtenerTokenDelHeader("Bearer token-tienda")).thenReturn("token-tienda");
        when(jwtUtil.esTokenValido("token-tienda")).thenReturn(true);
        when(jwtUtil.extraerRol("token-tienda")).thenReturn("TIENDA");
        when(jwtUtil.extraerId("token-tienda")).thenReturn(5);

        mockMvc.perform(delete("/api/productos/producto/1/tienda/3").header("Authorization", "Bearer token-tienda"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").value("Producto desactivado. Ya no aparece en el catalogo publico."));
    }
}
