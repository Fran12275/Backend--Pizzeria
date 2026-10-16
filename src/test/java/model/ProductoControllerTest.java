package model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.controller.ProductoController;
import org.exception.ProductoNoEncontradoException;
import org.junit.jupiter.api.*;
import org.model.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
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
    @Nested
    @DisplayName("exportarProductos() / importarProductos() con Jackson")
    class ExportarImportarJson {

        private Path fichero;

        @BeforeEach
        void crearFicheroTemporal() throws IOException {
            fichero = Files.createTempFile("productos", ".json");
        }

        @AfterEach
        void borrarFicheroTemporal() throws IOException {
            Files.deleteIfExists(fichero);
        }

        private void cargarCatalogoDeEjemplo() {
            Ingrediente tomate = new Ingrediente(7, "Tomate", 100, true);
            Ingrediente queso = new Ingrediente(8, "Mozzarella", 50, false);
            productoController.agregarProducto(new Pizza(0, "Margarita", 8.5, Size.MEDIANA, List.of(tomate, queso)));
            productoController.agregarProducto(new Pasta(0, "Carbonara", 9.0, Size.GRANDE, List.of(queso)));
            productoController.agregarProducto(new Bebida(0, "Agua", 1.5, Size.PEQUENYA));
        }

        @Test
        @DisplayName("el JSON no contiene ningún id ni el atributo calculado, y cada producto lleva su 'tipo'")
        void jsonSinIdsNiAtributosCalculados() throws IOException {
            cargarCatalogoDeEjemplo();

            productoController.exportarProductos(fichero);

            JsonNode raiz = new ObjectMapper().readTree(fichero.toFile());
            assertEquals(3, raiz.size());
            for (JsonNode producto : raiz) {
                assertFalse(producto.has("id"));
                assertTrue(producto.has("tipo"));
                if (producto.has("ingredientes")) {
                    for (JsonNode ingrediente : producto.get("ingredientes")) {
                        assertFalse(ingrediente.has("id"));
                    }
                }
            }
            assertEquals("pizza", raiz.get(0).get("tipo").asText());
            assertEquals("pasta", raiz.get(1).get("tipo").asText());
            assertEquals("bebida", raiz.get(2).get("tipo").asText());
        }

        @Test
        @DisplayName("exportar e importar conserva tipos, atributos e ingredientes anidados")
        void exportarEImportarConservaLosDatos() throws Exception {
            cargarCatalogoDeEjemplo();
            productoController.exportarProductos(fichero);

            ProductoController otro = new ProductoController();
            List<Producto> importados = otro.importarProductos(fichero);

            assertEquals(3, importados.size());
            assertEquals(3, otro.listarProducto().size());

            Producto p1 = otro.buscarPorId(1);
            assertTrue(p1 instanceof Pizza);
            assertEquals("Margarita", p1.getNombre());
            assertEquals(8.5, p1.getPrecio(), 0.001);
            assertEquals(Size.MEDIANA, ((Pizza) p1).getSize());
            assertEquals(2, ((Pizza) p1).getIngredientes().size());
            assertEquals("Tomate", ((Pizza) p1).getIngredientes().get(0).getNombre());
            assertEquals(100, ((Pizza) p1).getIngredientes().get(0).getStock());
            assertTrue(((Pizza) p1).getIngredientes().get(0).isEsVegano());

            Producto p2 = otro.buscarPorId(2);
            assertTrue(p2 instanceof Pasta);
            assertEquals(1, ((Pasta) p2).getIngredientes().size());

            Producto p3 = otro.buscarPorId(3);
            assertTrue(p3 instanceof Bebida);
            assertEquals("Agua", p3.getNombre());
        }

        @Test
        @DisplayName("al importar se asignan ids nuevos a continuación de los existentes")
        void importarAsignaIdsNuevos() throws Exception {
            cargarCatalogoDeEjemplo();
            productoController.exportarProductos(fichero);

            productoController.importarProductos(fichero);

            assertEquals(6, productoController.listarProducto().size());
            assertEquals(6, productoController.buscarPorId(6).getId());
        }

        @Test
        @DisplayName("lanza IOException si el fichero a importar no existe")
        void importarFicheroInexistente() throws IOException {
            Files.deleteIfExists(fichero);

            assertThrows(IOException.class, () -> productoController.importarProductos(fichero));
        }
    }
}