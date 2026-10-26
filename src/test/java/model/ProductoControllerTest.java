package model;

import org.controller.ProductoController;
import org.exception.ProductoNoEncontradoException;
import org.junit.jupiter.api.*;
import org.model.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ProductoControllerTest {

    private ProductoController productoController;

    @BeforeEach
    void setUp() {
        // El controlador arranca vacío: los productos de prueba los crea el propio test.
        productoController = new ProductoController();
    }

    private Producto bebida(String nombre, double precio) {
        return new Bebida(0, nombre, precio, Size.PEQUENYA);
    }

    @Nested
    @DisplayName("listarProducto()")
    class ListarCatalogo {

        @Test
        @DisplayName("el catálogo está vacío al crear el controlador")
        void catalogoInicialVacio() {
            assertTrue(productoController.listarProducto().isEmpty());
        }

        @Test
        @DisplayName("devuelve los productos agregados")
        void devuelveProductosAgregados() {
            productoController.agregarProducto(bebida("Agua", 1.5));
            productoController.agregarProducto(bebida("Refresco", 2.0));

            assertEquals(2, productoController.listarProducto().size());
        }

        @Test
        @DisplayName("devuelve una lista inmutable (no se puede modificar desde fuera)")
        void listaEsInmutable() {
            List<Producto> catalogo = productoController.listarProducto();
            assertThrows(UnsupportedOperationException.class,
                    () -> catalogo.add(bebida("Intrusa", 1.0)));
        }
    }

    @Nested
    @DisplayName("buscarPorId()")
    class BuscarPorId {

        @Test
        @DisplayName("encuentra un producto existente")
        void encuentraProductoExistente() throws ProductoNoEncontradoException {
            Producto agregado = productoController.agregarProducto(bebida("Agua", 1.5));

            Producto encontrado = productoController.buscarPorId(agregado.getId());

            assertEquals("Agua", encontrado.getNombre());
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
            Producto agregado = productoController.agregarProducto(bebida("Cerveza sin alcohol", 2.20));

            assertEquals(1, agregado.getId());
            assertEquals(1, productoController.listarProducto().size());
        }

        @Test
        @DisplayName("ids autogenerados sucesivos son distintos e incrementales")
        void idsSucesivosSonIncrementales() {
            Producto p1 = productoController.agregarProducto(bebida("Bebida A", 1.0));
            Producto p2 = productoController.agregarProducto(bebida("Bebida B", 1.0));

            assertEquals(1, p1.getId());
            assertEquals(2, p2.getId());
            assertNotEquals(p1.getId(), p2.getId());
        }

        @Test
        @DisplayName("lanza IllegalArgumentException si el producto es null")
        void rechazaProductoNull() {
            assertThrows(IllegalArgumentException.class,
                    () -> productoController.agregarProducto(null));
        }

        @Test
        @DisplayName("lanza IllegalStateException si se agrega dos veces el mismo producto")
        void rechazaAgregarDosVecesElMismoProducto() {
            Producto agua = productoController.agregarProducto(bebida("Agua", 1.5));

            assertThrows(IllegalStateException.class, () -> productoController.agregarProducto(agua));
            assertEquals(1, agua.getId());
            assertEquals(1, productoController.listarProducto().size());
        }

        @Test
        @DisplayName("un producto no puede tener precio 0 ni negativo")
        void rechazaPrecioCeroONegativo() {
            assertThrows(IllegalArgumentException.class, () -> bebida("Gratis", 0.0));
            assertThrows(IllegalArgumentException.class, () -> bebida("Negativa", -1.0));

            Producto agua = bebida("Agua", 1.5);
            assertThrows(IllegalArgumentException.class, () -> agua.setPrecio(0.0));
            assertEquals(1.5, agua.getPrecio(), 0.001);
        }
    }

    @Nested
    @DisplayName("actualizarProducto()")
    class ActualizarProducto {

        @Test
        @DisplayName("sustituye el producto existente por el actualizado, conservando el id")
        void actualizaProductoExistente() throws ProductoNoEncontradoException {
            Producto original = productoController.agregarProducto(bebida("Agua", 1.5));
            Producto actualizado = new Bebida(original.getId(), "Agua con gas", 1.8, Size.MEDIANA);

            productoController.actualizarProducto(actualizado);

            Producto encontrado = productoController.buscarPorId(original.getId());
            assertEquals("Agua con gas", encontrado.getNombre());
            assertEquals(1.8, encontrado.getPrecio(), 0.001);
            assertEquals(1, productoController.listarProducto().size());
        }

        @Test
        @DisplayName("lanza ProductoNoEncontradoException si el producto no existe")
        void rechazaProductoInexistente() {
            Producto inexistente = new Bebida(999, "Fantasma", 1.0, Size.PEQUENYA);

            assertThrows(ProductoNoEncontradoException.class,
                    () -> productoController.actualizarProducto(inexistente));
        }

        @Test
        @DisplayName("lanza IllegalArgumentException si el producto es null")
        void rechazaProductoNull() {
            assertThrows(IllegalArgumentException.class,
                    () -> productoController.actualizarProducto(null));
        }
    }

    @Nested
    @DisplayName("eliminarProducto()")
    class EliminarProducto {

        @Test
        @DisplayName("elimina el producto del catálogo")
        void eliminaProductoExistente() throws ProductoNoEncontradoException {
            Producto p1 = productoController.agregarProducto(bebida("Agua", 1.5));
            Producto p2 = productoController.agregarProducto(bebida("Refresco", 2.0));

            productoController.eliminarProducto(p1.getId());

            assertEquals(1, productoController.listarProducto().size());
            assertThrows(ProductoNoEncontradoException.class,
                    () -> productoController.buscarPorId(p1.getId()));
            assertEquals(p2, productoController.buscarPorId(p2.getId()));
        }

        @Test
        @DisplayName("lanza ProductoNoEncontradoException si el producto no existe")
        void rechazaProductoInexistente() {
            assertThrows(ProductoNoEncontradoException.class,
                    () -> productoController.eliminarProducto(999));
        }
    }
}