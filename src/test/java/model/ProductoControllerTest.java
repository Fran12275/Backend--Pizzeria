package model;

import org.controller.ProductoController;
import org.exception.ProductoNoEncontradoException;
import org.junit.jupiter.api.*;
import org.model.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
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

        @Test
        @DisplayName("una pizza o una pasta no pueden tener ingredientes null")
        void rechazaIngredientesNull() {
            Ingrediente tomate = new Ingrediente(1, "Tomate", 100, true);
            List<Ingrediente> conNull = Arrays.asList(tomate, null);

            assertThrows(IllegalArgumentException.class,
                    () -> new Pizza(0, "Rota", 8.5, Size.MEDIANA, conNull));
            assertThrows(IllegalArgumentException.class,
                    () -> new Pasta(0, "Rota", 9.0, Size.GRANDE, conNull));
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
    @DisplayName("importarIngredientes() y exportarIngredientes()")
    class IngredientesCsv {

        private Path fichero;

        @BeforeEach
        void crearFicheroTemporal() throws IOException {
            fichero = Files.createTempFile("ingredientes", ".csv");
        }

        @AfterEach
        void borrarFicheroTemporal() throws IOException {
            Files.deleteIfExists(fichero);
        }

        private List<Ingrediente> ingredientesDeEjemplo() {
            return List.of(
                    new Ingrediente(7, "Tomate", 100, true),
                    new Ingrediente(8, "Mozzarella", 50, false));
        }

        @Test
        @DisplayName("el CSV usa ';' como separador y no contiene la columna id")
        void csvConSeparadorPuntoYComaSinId() throws IOException {
            productoController.exportarIngredientes(ingredientesDeEjemplo(), fichero);

            List<String> lineas = Files.readAllLines(fichero);
            assertEquals(3, lineas.size());
            String cabecera = lineas.get(0).toUpperCase();
            assertTrue(cabecera.contains(";"));
            assertFalse(cabecera.contains("ID"));
            assertTrue(cabecera.contains("NOMBRE"));
            assertTrue(cabecera.contains("STOCK DISPONIBLE"));
            assertTrue(cabecera.contains("VEGANO"));
        }

        @Test
        @DisplayName("exportar e importar conserva nombre, stock y si es vegano; el id llega a 0")
        void exportarEImportarConservaLosDatos() throws IOException {
            productoController.exportarIngredientes(ingredientesDeEjemplo(), fichero);

            List<Ingrediente> importados = productoController.importarIngredientes(fichero);

            assertEquals(2, importados.size());
            Ingrediente tomate = importados.stream()
                    .filter(i -> i.getNombre().equals("Tomate"))
                    .findFirst()
                    .orElseThrow();
            assertEquals(100, tomate.getStock());
            assertTrue(tomate.isEsVegano());
            assertEquals(0, tomate.getId());
        }

        @Test
        @DisplayName("los ingredientes importados sin id son distintos entre si")
        void importadosSinIdSonDistintos() throws IOException {
            productoController.exportarIngredientes(ingredientesDeEjemplo(), fichero);

            List<Ingrediente> importados = productoController.importarIngredientes(fichero);

            assertNotEquals(importados.get(0), importados.get(1));
            assertEquals(2, new HashSet<>(importados).size());
        }

        @Test
        @DisplayName("lanza IOException si el fichero a importar no existe")
        void importarFicheroInexistente() throws IOException {
            Files.deleteIfExists(fichero);

            assertThrows(IOException.class, () -> productoController.importarIngredientes(fichero));
        }

        @Test
        @DisplayName("lanza IOException si el stock del CSV no es un numero")
        void importarCsvInvalido() throws IOException {
            Files.writeString(fichero, "NOMBRE;STOCK DISPONIBLE;VEGANO\nTomate;mucho;true\n");

            assertThrows(IOException.class, () -> productoController.importarIngredientes(fichero));
        }

        @Test
        @DisplayName("lanza NullPointerException si la lista a exportar es null")
        void exportarListaNull() {
            assertThrows(NullPointerException.class,
                    () -> productoController.exportarIngredientes(null, fichero));
        }
    }
}