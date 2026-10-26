package model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
            ProductoNoEncontradoException error = assertThrows(ProductoNoEncontradoException.class,
                    () -> productoController.buscarPorId(999));
            assertEquals("No existe ningún producto con id 999", error.getMessage());
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

        @Test
        @DisplayName("una pizza o una pasta deben tener al menos un ingrediente")
        void rechazaListaDeIngredientesVacia() {
            IllegalArgumentException errorPizza = assertThrows(IllegalArgumentException.class,
                    () -> new Pizza(0, "Vacía", 8.5, Size.MEDIANA, List.of()));
            IllegalArgumentException errorPasta = assertThrows(IllegalArgumentException.class,
                    () -> new Pasta(0, "Vacía", 9.0, Size.GRANDE, List.of()));

            assertEquals("La pizza debe tener al menos un ingrediente", errorPizza.getMessage());
            assertEquals("La pasta debe tener al menos un ingrediente", errorPasta.getMessage());
        }

        @Test
        @DisplayName("no se puede añadir un ingrediente null a una pizza o a una pasta")
        void rechazaAnadirIngredienteNull() {
            Ingrediente tomate = new Ingrediente(1, "Tomate", 100, true);
            Pizza pizza = new Pizza(0, "Margarita", 8.5, Size.MEDIANA, List.of(tomate));
            Pasta pasta = new Pasta(0, "Napolitana", 9.0, Size.GRANDE, List.of(tomate));

            NullPointerException errorPizza = assertThrows(NullPointerException.class, () -> pizza.addIngrediente(null));
            NullPointerException errorPasta = assertThrows(NullPointerException.class, () -> pasta.addIngrediente(null));

            assertEquals("El ingrediente no puede ser null", errorPizza.getMessage());
            assertEquals("El ingrediente no puede ser null", errorPasta.getMessage());
            assertEquals(1, pizza.getIngredientes().size());
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
        @DisplayName("lanza IOException si el stock del CSV no es un número y dice en qué línea está")
        void importarCsvInvalido() throws IOException {
            Files.writeString(fichero, "NOMBRE;STOCK DISPONIBLE;VEGANO\nTomate;mucho;true\n");

            IOException error = assertThrows(IOException.class, () -> productoController.importarIngredientes(fichero));
            assertTrue(error.getMessage().contains("linea 2"));
        }

        @Test
        @DisplayName("lanza IOException si el CSV tiene unas comillas sin cerrar")
        void importarCsvConComillasSinCerrar() throws IOException {
            Files.writeString(fichero, "NOMBRE;STOCK DISPONIBLE;VEGANO\n\"Tomate;100;true\n");

            assertThrows(IOException.class, () -> productoController.importarIngredientes(fichero));
        }

        @Test
        @DisplayName("lanza NullPointerException si la lista a exportar es null")
        void exportarListaNull() {
            assertThrows(NullPointerException.class,
                    () -> productoController.exportarIngredientes(null, fichero));
        }
    }

    @Nested
    @DisplayName("importarProductos() y exportarProductos()")
    class ProductosJson {

        private Path fichero;

        @BeforeEach
        void crearFicheroTemporal() throws IOException {
            fichero = Files.createTempFile("productos", ".json");
        }

        @AfterEach
        void borrarFicheroTemporal() throws IOException {
            Files.deleteIfExists(fichero);
        }

        private void agregarProductosDeEjemplo() {
            Ingrediente tomate = new Ingrediente(7, "Tomate", 100, true);
            Ingrediente queso = new Ingrediente(8, "Mozzarella", 50, false);
            productoController.agregarProducto(new Pizza(0, "Margarita", 8.5, Size.MEDIANA, List.of(tomate, queso)));
            productoController.agregarProducto(new Pasta(0, "Carbonara", 9.0, Size.GRANDE, List.of(queso)));
            productoController.agregarProducto(new Bebida(0, "Agua", 1.5, Size.PEQUENYA));
        }

        @Test
        @DisplayName("el JSON no contiene ningún id ni el atributo calculado, y cada producto lleva su 'tipo'")
        void jsonSinIdsNiAtributosCalculados() throws IOException {
            agregarProductosDeEjemplo();
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
        void exportarEImportarConservaLosDatos() throws IOException {
            agregarProductosDeEjemplo();
            productoController.exportarProductos(fichero);

            List<Producto> importados = productoController.importarProductos(fichero);

            assertEquals(3, importados.size());

            Producto p1 = importados.get(0);
            assertTrue(p1 instanceof Pizza);
            assertEquals("Margarita", p1.getNombre());
            assertEquals(8.5, p1.getPrecio(), 0.001);
            assertEquals(Size.MEDIANA, ((Pizza) p1).getSize());
            assertEquals(2, ((Pizza) p1).getIngredientes().size());
            assertEquals("Tomate", ((Pizza) p1).getIngredientes().get(0).getNombre());
            assertEquals(100, ((Pizza) p1).getIngredientes().get(0).getStock());
            assertTrue(((Pizza) p1).getIngredientes().get(0).isEsVegano());

            Producto p2 = importados.get(1);
            assertTrue(p2 instanceof Pasta);
            assertEquals(1, ((Pasta) p2).getIngredientes().size());

            Producto p3 = importados.get(2);
            assertTrue(p3 instanceof Bebida);
            assertEquals("Agua", p3.getNombre());
        }

        @Test
        @DisplayName("importar no toca el catálogo y los productos llegan sin id: el id se asigna al agregarlos")
        void importadosSinIdHastaAgregarlosAlCatalogo() throws Exception {
            agregarProductosDeEjemplo();
            productoController.exportarProductos(fichero);
            ProductoController otroCatalogo = new ProductoController();

            List<Producto> importados = otroCatalogo.importarProductos(fichero);
            assertTrue(importados.stream().allMatch(p -> p.getId() == 0));
            assertTrue(otroCatalogo.listarProducto().isEmpty());

            importados.forEach(otroCatalogo::agregarProducto);

            assertEquals(3, otroCatalogo.listarProducto().size());
            assertEquals("Carbonara", otroCatalogo.buscarPorId(2).getNombre());
        }

        @Test
        @DisplayName("los productos importados sin id son distintos entre si")
        void importadosSinIdSonDistintos() throws IOException {
            agregarProductosDeEjemplo();
            productoController.exportarProductos(fichero);

            List<Producto> importados = productoController.importarProductos(fichero);

            assertNotEquals(importados.get(0), importados.get(1));
            assertEquals(3, new HashSet<>(importados).size());
        }

        @Test
        @DisplayName("lanza IOException si el fichero a importar no existe")
        void importarFicheroInexistente() throws IOException {
            Files.deleteIfExists(fichero);

            assertThrows(IOException.class, () -> productoController.importarProductos(fichero));
        }

        @Test
        @DisplayName("con el catálogo vacío exporta una lista vacía")
        void exportarCatalogoVacio() throws IOException {
            productoController.exportarProductos(fichero);

            assertTrue(productoController.importarProductos(fichero).isEmpty());
        }

        @Test
        @DisplayName("un fichero que solo contiene null se importa como una lista vacía")
        void importarFicheroConNull() throws IOException {
            Files.writeString(fichero, "null");

            assertTrue(productoController.importarProductos(fichero).isEmpty());
        }

        @Test
        @DisplayName("lanza IOException si la lista contiene un producto null y dice en qué posición está")
        void importarListaConProductoNull() throws IOException {
            Files.writeString(fichero, "[{\"tipo\":\"bebida\",\"nombre\":\"Agua\",\"precio\":1.5,\"size\":\"PEQUENYA\"}, null]");

            IOException error = assertThrows(IOException.class, () -> productoController.importarProductos(fichero));
            assertTrue(error.getMessage().contains("posicion 2"));
        }
    }
}