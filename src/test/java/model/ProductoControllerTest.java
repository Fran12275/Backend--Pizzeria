package model;

import org.controller.ProductoController;
import org.exception.ProductoNoEncontradoException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.model.Bebida;
import org.model.Producto;
import org.model.Size;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProductoControllerTest {

        private ProductoController productoController;

        @BeforeEach
        void setUp() {
            // Nueva instancia antes de cada test para que el catálogo (y sus ids)
            // arranquen siempre desde el mismo estado inicial ("seed").
            productoController = new ProductoController();
        }

        @Nested
        @DisplayName("listarCatalogo()")
        class ListarCatalogo {

            @Test
            @DisplayName("el catálogo precargado tiene los 5 productos de ejemplo")
            void catalogoInicialTieneCincoProductos() {
                List<Producto> catalogo = productoController.listarProducto();
                assertEquals(5, catalogo.size());
            }

            @Test
            @DisplayName("devuelve una lista inmutable (no se puede modificar desde fuera)")
            void listaEsInmutable() {
                List<Producto> catalogo = productoController.listarProducto();
                assertThrows(UnsupportedOperationException.class,
                        () -> catalogo.add(new Bebida(0, "Intrusa", 1.0, Size.PEQUENYA)));
            }
        }

        @Nested
        @DisplayName("buscarPorId()")
        class BuscarPorId {

            @Test
            @DisplayName("encuentra un producto existente del catálogo precargado")
            void encuentraProductoExistente() throws ProductoNoEncontradoException {
                Producto producto = productoController.buscarPorId(1);
                assertEquals(1, producto.getId());
                assertEquals("Margarita", producto.getNombre());
            }

            @Test
            @DisplayName("lanza ProductoNoEncontradoException si el id no existe")
            void lanzaExcepcionSiNoExiste() {
                assertThrows(ProductoNoEncontradoException.class,
                        () -> productoController.buscarPorId(999));
            }
        }

        @Nested
        @DisplayName("agregarProducto()")
        class AgregarProducto {

            @Test
            @DisplayName("añade el producto al catálogo y le asigna un id autogenerado")
            void agregaProductoConIdAutogenerado() {
                Producto nuevo = new Bebida(0, "Cerveza sin alcohol", 2.20, Size.PEQUENYA);

                Producto agregado = productoController.agregarProducto(nuevo);

                // El catálogo precargado usa los ids 1-5, así que el siguiente debe ser 6
                assertEquals(6, agregado.getId());
                assertEquals(6, productoController.listarProducto().size());
            }

            @Test
            @DisplayName("el producto agregado se puede volver a buscar por su nuevo id")
            void productoAgregadoEsBuscable() throws ProductoNoEncontradoException {
                Producto nuevo = new Bebida(0, "Cerveza sin alcohol", 2.20, Size.PEQUENYA);
                Producto agregado = productoController.agregarProducto(nuevo);

                Producto encontrado = productoController.buscarPorId(agregado.getId());

                assertEquals("Cerveza sin alcohol", encontrado.getNombre());
            }

            @Test
            @DisplayName("ids autogenerados sucesivos son distintos e incrementales")
            void idsSucesivosSonIncrementales() {
                Producto p1 = productoController.agregarProducto(new Bebida(0, "Bebida A", 1.0, Size.PEQUENYA));
                Producto p2 = productoController.agregarProducto(new Bebida(0, "Bebida B", 1.0, Size.PEQUENYA));

                assertEquals(6, p1.getId());
                assertEquals(7, p2.getId());
                assertNotEquals(p1.getId(), p2.getId());
            }

            @Test
            @DisplayName("lanza IllegalArgumentException si el producto es null")
            void rechazaProductoNull() {
                assertThrows(IllegalArgumentException.class,
                        () -> productoController.agregarProducto(null));
            }
        }
}
