package cl.duoc.ms_inventario.config;

import org.springframework.context.annotation.Configuration;

/*
 * Configuracion general de ms-inventario.
 * El bean RestTemplate fue eliminado: la comunicacion con ms-tiendas
 * ahora se hace via Feign (TiendaFeignClient), que usa Eureka para
 * resolver el host sin URLs hardcodeadas.
 */
@Configuration
public class AppConfig {
    // Vacio: Feign no necesita un bean RestTemplate manual.
}
