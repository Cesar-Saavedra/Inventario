package cl.duoc.ms_inventario.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import cl.duoc.ms_inventario.model.CategoriaProducto;
import cl.duoc.ms_inventario.model.Producto;

@Repository
public interface ProductoRepositorio extends JpaRepository<Producto, Integer> {

    // Todos los productos de una tienda (activos e inactivos)
    // El dueno los ve todos para gestionar su catalogo
    List<Producto> findByTiendaId(Integer tiendaId);

    // Solo los productos VISIBLES de una tienda
    // Esta es la vista que ven los compradores (activo = true)
    List<Producto> findByTiendaIdAndActivo(Integer tiendaId, Boolean activo);

    // Productos activos de una categoria en toda la plataforma
    // Para un buscador global: "muéstrame todos los SOBRES disponibles"
    List<Producto> findByCategoriaAndActivo(CategoriaProducto categoria, Boolean activo);
    
}
