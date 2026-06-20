package cl.duoc.ms_inventario.client;

import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

import cl.duoc.ms_inventario.dto.TiendaResumenDTO;

/*
 * Cliente Feign para comunicarse con ms-tiendas.
 *
 * name = "ms-tiendas" → Feign resuelve el host via Eureka (lb://ms-tiendas),
 * sin URLs hardcodeadas en el yaml.
 */
@FeignClient(name = "ms-tiendas")
public interface TiendaFeignClient {

    /*
     * GET /api/tiendas/{id}/resumen
     * Devuelve nombre y estado de una tienda.
     * Se usa para verificar que la tienda existe y esta ACTIVA
     * antes de agregar productos a su catalogo.
     */
    @GetMapping("/api/tiendas/{id}/resumen")
    TiendaResumenDTO obtenerResumenTienda(
            @PathVariable("id") Integer idTienda,
            @RequestHeader("Authorization") String authHeader
    );

    /*
     * GET /api/tiendas/dueno/{usuarioId}
     * Devuelve todas las tiendas que pertenecen a un usuario.
     * Se usa para verificar que la tienda indicada en la URL realmente
     * pertenece al usuario autenticado antes de dejarlo modificar su catalogo
     * (el id de la tienda NO es el mismo que el id del usuario dueno).
     */
    @GetMapping("/api/tiendas/dueno/{usuarioId}")
    List<TiendaResumenDTO> obtenerTiendasPorDueno(
            @PathVariable("usuarioId") Integer usuarioId,
            @RequestHeader("Authorization") String authHeader
    );
}
