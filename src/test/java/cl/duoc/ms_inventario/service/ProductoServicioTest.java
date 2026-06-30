package cl.duoc.ms_inventario.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import cl.duoc.ms_inventario.client.TiendaFeignClient;
import cl.duoc.ms_inventario.dto.AgregarProductoDto;
import cl.duoc.ms_inventario.dto.TiendaResumenDTO;
import cl.duoc.ms_inventario.dto.ProductoRespuestaDto;
import cl.duoc.ms_inventario.model.CategoriaProducto;
import cl.duoc.ms_inventario.model.Producto;
import cl.duoc.ms_inventario.repository.ProductoRepositorio;

/*
 * El TiendaFeignClient (dependencia hacia ms-tiendas) se mockea con Mockito
 * en lugar de invocarse de verdad: estas pruebas validan solo las reglas de
 * negocio de ProductoServicio (tienda activa, dueño correcto, datos del
 * formulario), no la comunicacion HTTP real entre microservicios.
 */
@ExtendWith(MockitoExtension.class)
class ProductoServicioTest {

    @Mock
    private ProductoRepositorio productoRepositorio;

    @Mock
    private TiendaFeignClient tiendaFeignClient;

    @InjectMocks
    private ProductoServicio productoServicio;

    private static final String AUTH_HEADER = "Bearer token-fake";
    private static final Integer TIENDA_ID = 3;
    private static final Integer USUARIO_ID = 5;

    private TiendaResumenDTO tiendaActiva;
    private AgregarProductoDto agregarProductoDto;

    @BeforeEach
    void setUp() {
        tiendaActiva = new TiendaResumenDTO(TIENDA_ID, "Carta Magica TCG", "Lun-Sab 11:00-20:00", "ACTIVA");

        agregarProductoDto = new AgregarProductoDto();
        agregarProductoDto.setNombre("Sobre Charizard ex - Pokemon SV");
        agregarProductoDto.setDescripcion("Sobre de 10 cartas");
        agregarProductoDto.setPrecio(new BigDecimal("5990"));
        agregarProductoDto.setStock(100);
        agregarProductoDto.setCategoria(CategoriaProducto.SOBRE);
    }

    // =====================================================================
    // agregarProducto
    // =====================================================================

    @Test
    void agregarProducto_exitoso() {
        // ARRANGE
        when(tiendaFeignClient.obtenerResumenTienda(TIENDA_ID, AUTH_HEADER)).thenReturn(tiendaActiva);
        when(tiendaFeignClient.obtenerTiendasPorDueno(USUARIO_ID, AUTH_HEADER))
                .thenReturn(List.of(tiendaActiva));
        when(productoRepositorio.save(any(Producto.class))).thenAnswer(invocacion -> {
            Producto p = invocacion.getArgument(0);
            p.setId(1);
            return p;
        });

        // ACT
        ProductoRespuestaDto respuesta = productoServicio.agregarProducto(
                TIENDA_ID, agregarProductoDto, USUARIO_ID, AUTH_HEADER);

        // ASSERT
        assertEquals(1, respuesta.getId());
        assertEquals("Sobre Charizard ex - Pokemon SV", respuesta.getNombre());
        assertEquals("Carta Magica TCG", respuesta.getNombreTienda());
        assertTrue(respuesta.getActivo());
    }

    @Test
    void agregarProducto_tiendaNoExiste() {
        when(tiendaFeignClient.obtenerResumenTienda(TIENDA_ID, AUTH_HEADER))
                .thenThrow(new RuntimeException("404"));

        RuntimeException error = assertThrows(RuntimeException.class, () ->
                productoServicio.agregarProducto(TIENDA_ID, agregarProductoDto, USUARIO_ID, AUTH_HEADER));

        assertTrue(error.getMessage().contains("No se encontro la tienda"));
    }

    @Test
    void agregarProducto_tiendaNoActiva() {
        TiendaResumenDTO tiendaPendiente = new TiendaResumenDTO(TIENDA_ID, "Carta Magica TCG", "Lun-Sab", "PENDIENTE");
        when(tiendaFeignClient.obtenerResumenTienda(TIENDA_ID, AUTH_HEADER)).thenReturn(tiendaPendiente);

        RuntimeException error = assertThrows(RuntimeException.class, () ->
                productoServicio.agregarProducto(TIENDA_ID, agregarProductoDto, USUARIO_ID, AUTH_HEADER));

        assertTrue(error.getMessage().contains("no esta activa"));
    }

    @Test
    void agregarProducto_usuarioNoEsDuenoDeLaTienda() {
        when(tiendaFeignClient.obtenerResumenTienda(TIENDA_ID, AUTH_HEADER)).thenReturn(tiendaActiva);
        // El usuario autenticado no tiene ninguna tienda a su nombre
        when(tiendaFeignClient.obtenerTiendasPorDueno(USUARIO_ID, AUTH_HEADER)).thenReturn(List.of());

        RuntimeException error = assertThrows(RuntimeException.class, () ->
                productoServicio.agregarProducto(TIENDA_ID, agregarProductoDto, USUARIO_ID, AUTH_HEADER));

        assertTrue(error.getMessage().contains("No tienes permiso"));
    }

    @Test
    void agregarProducto_precioInvalido() {
        when(tiendaFeignClient.obtenerResumenTienda(TIENDA_ID, AUTH_HEADER)).thenReturn(tiendaActiva);
        when(tiendaFeignClient.obtenerTiendasPorDueno(USUARIO_ID, AUTH_HEADER))
                .thenReturn(List.of(tiendaActiva));
        agregarProductoDto.setPrecio(BigDecimal.ZERO);

        RuntimeException error = assertThrows(RuntimeException.class, () ->
                productoServicio.agregarProducto(TIENDA_ID, agregarProductoDto, USUARIO_ID, AUTH_HEADER));

        assertTrue(error.getMessage().contains("precio"));
    }

    // =====================================================================
    // listarCatalogoPorTienda
    // =====================================================================

    @Test
    void listarCatalogoPorTienda_devuelveSoloActivos() {
        when(tiendaFeignClient.obtenerResumenTienda(TIENDA_ID, AUTH_HEADER)).thenReturn(tiendaActiva);

        Producto activo = construirProducto(1, "Sobre Pokemon", true);
        when(productoRepositorio.findByTiendaIdAndActivo(TIENDA_ID, true)).thenReturn(List.of(activo));

        List<ProductoRespuestaDto> catalogo = productoServicio.listarCatalogoPorTienda(TIENDA_ID, AUTH_HEADER);

        assertEquals(1, catalogo.size());
        assertEquals("Sobre Pokemon", catalogo.get(0).getNombre());
        assertEquals("Carta Magica TCG", catalogo.get(0).getNombreTienda());
    }

    @Test
    void listarCatalogoPorTienda_tiendaCaida_usaNombreDesconocido() {
        // Si ms-tiendas no responde, el servicio sigue funcionando con un nombre por defecto
        when(tiendaFeignClient.obtenerResumenTienda(TIENDA_ID, AUTH_HEADER))
                .thenThrow(new RuntimeException("Connection refused"));
        when(productoRepositorio.findByTiendaIdAndActivo(TIENDA_ID, true)).thenReturn(List.of());

        List<ProductoRespuestaDto> catalogo = productoServicio.listarCatalogoPorTienda(TIENDA_ID, AUTH_HEADER);

        assertTrue(catalogo.isEmpty());
    }

    // =====================================================================
    // obtenerPorId
    // =====================================================================

    @Test
    void obtenerPorId_encontrado() {
        Producto producto = construirProducto(1, "Sobre Pokemon", true);
        when(productoRepositorio.findById(1)).thenReturn(Optional.of(producto));
        when(tiendaFeignClient.obtenerResumenTienda(TIENDA_ID, AUTH_HEADER)).thenReturn(tiendaActiva);

        ProductoRespuestaDto respuesta = productoServicio.obtenerPorId(1, AUTH_HEADER);

        assertEquals("Sobre Pokemon", respuesta.getNombre());
    }

    @Test
    void obtenerPorId_noEncontrado() {
        when(productoRepositorio.findById(99)).thenReturn(Optional.empty());

        RuntimeException error = assertThrows(RuntimeException.class, () ->
                productoServicio.obtenerPorId(99, AUTH_HEADER));

        assertTrue(error.getMessage().contains("no encontrado"));
    }

    // =====================================================================
    // actualizarProducto
    // =====================================================================

    @Test
    void actualizarProducto_productoNoPerteneceATienda() {
        Producto producto = construirProducto(1, "Sobre Pokemon", true);
        producto.setTiendaId(99); // pertenece a otra tienda
        when(productoRepositorio.findById(1)).thenReturn(Optional.of(producto));

        RuntimeException error = assertThrows(RuntimeException.class, () ->
                productoServicio.actualizarProducto(1, new cl.duoc.ms_inventario.dto.ActualizarProductoDto(),
                        TIENDA_ID, USUARIO_ID, AUTH_HEADER));

        assertTrue(error.getMessage().contains("no pertenece a tu tienda"));
    }

    @Test
    void actualizarProducto_actualizaSoloLosCamposEnviados() {
        Producto producto = construirProducto(1, "Sobre Pokemon", true);
        when(productoRepositorio.findById(1)).thenReturn(Optional.of(producto));
        when(tiendaFeignClient.obtenerTiendasPorDueno(USUARIO_ID, AUTH_HEADER))
                .thenReturn(List.of(tiendaActiva));
        when(tiendaFeignClient.obtenerResumenTienda(TIENDA_ID, AUTH_HEADER)).thenReturn(tiendaActiva);
        when(productoRepositorio.save(any(Producto.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        cl.duoc.ms_inventario.dto.ActualizarProductoDto dto = new cl.duoc.ms_inventario.dto.ActualizarProductoDto();
        dto.setPrecio(new BigDecimal("4990"));
        dto.setStock(75);

        ProductoRespuestaDto respuesta = productoServicio.actualizarProducto(
                1, dto, TIENDA_ID, USUARIO_ID, AUTH_HEADER);

        assertEquals(new BigDecimal("4990"), respuesta.getPrecio());
        assertEquals(75, respuesta.getStock());
        // El nombre no se toco porque el dto no lo traia
        assertEquals("Sobre Pokemon", respuesta.getNombre());
    }

    // =====================================================================
    // desactivarProducto
    // =====================================================================

    @Test
    void desactivarProducto_marcaInactivoSinBorrar() {
        Producto producto = construirProducto(1, "Sobre Pokemon", true);
        when(productoRepositorio.findById(1)).thenReturn(Optional.of(producto));
        when(tiendaFeignClient.obtenerTiendasPorDueno(USUARIO_ID, AUTH_HEADER))
                .thenReturn(List.of(tiendaActiva));

        productoServicio.desactivarProducto(1, TIENDA_ID, USUARIO_ID, AUTH_HEADER);

        ArgumentCaptor<Producto> captor = ArgumentCaptor.forClass(Producto.class);
        org.mockito.Mockito.verify(productoRepositorio).save(captor.capture());
        assertEquals(false, captor.getValue().getActivo());
    }

    @Test
    void desactivarProducto_usuarioSinPermiso() {
        Producto producto = construirProducto(1, "Sobre Pokemon", true);
        when(productoRepositorio.findById(1)).thenReturn(Optional.of(producto));
        when(tiendaFeignClient.obtenerTiendasPorDueno(USUARIO_ID, AUTH_HEADER)).thenReturn(List.of());

        RuntimeException error = assertThrows(RuntimeException.class, () ->
                productoServicio.desactivarProducto(1, TIENDA_ID, USUARIO_ID, AUTH_HEADER));

        assertTrue(error.getMessage().contains("No tienes permiso"));
    }

    // =====================================================================
    // Helpers
    // =====================================================================

    private Producto construirProducto(Integer id, String nombre, boolean activo) {
        Producto producto = new Producto();
        producto.setId(id);
        producto.setTiendaId(TIENDA_ID);
        producto.setNombre(nombre);
        producto.setDescripcion("Descripcion de prueba");
        producto.setPrecio(new BigDecimal("5990"));
        producto.setStock(10);
        producto.setCategoria(CategoriaProducto.SOBRE);
        producto.setActivo(activo);
        return producto;
    }
}
