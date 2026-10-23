package model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.controller.ProductoController;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.model.Bebida;
import org.model.Ingrediente;
import org.model.Pasta;
import org.model.Pizza;
import org.model.Producto;
import org.model.Size;
import org.utils.ProductoJsonUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

    class ProductoJsonUtilsTest {

        private Path fichero;

        @BeforeEach
        void setUp() throws IOException {
            fichero = Files.createTempFile("productos", ".json");
        }

        @AfterEach
        void borrarFicheroTemporal() throws IOException {
            Files.deleteIfExists(fichero);
        }

        private List<Producto> productosDeEjemplo() {
            Ingrediente tomate = new Ingrediente(7, "Tomate", 100, true);
            Ingrediente queso = new Ingrediente(8, "Mozzarella", 50, false);
            return List.of(
                    new Pizza(1, "Margarita", 8.5, Size.MEDIANA, List.of(tomate, queso)),
                    new Pasta(2, "Carbonara", 9.0, Size.GRANDE, List.of(queso)),
                    new Bebida(3, "Agua", 1.5, Size.PEQUENYA));
        }

        @Test
        @DisplayName("el JSON no contiene ningún id ni el atributo calculado, y cada producto lleva su 'tipo'")
        void jsonSinIdsNiAtributosCalculados() throws IOException {
            ProductoJsonUtils.exportarProductos(productosDeEjemplo(), fichero);

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
            ProductoJsonUtils.exportarProductos(productosDeEjemplo(), fichero);

            List<Producto> importados = ProductoJsonUtils.importarProductos(fichero);

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
        @DisplayName("los productos importados no traen id: lo asigna el catálogo al agregarlos")
        void importadosSinIdHastaAgregarlosAlCatalogo() throws Exception {
            ProductoJsonUtils.exportarProductos(productosDeEjemplo(), fichero);
            ProductoController catalogo = new ProductoController();

            List<Producto> importados = ProductoJsonUtils.importarProductos(fichero);
            assertTrue(importados.stream().allMatch(p -> p.getId() == 0));

            importados.forEach(catalogo::agregarProducto);

            assertEquals(3, catalogo.listarProducto().size());
            assertEquals("Carbonara", catalogo.buscarPorId(2).getNombre());
        }

        @Test
        @DisplayName("lanza IOException si el fichero a importar no existe")
        void importarFicheroInexistente() throws IOException {
            Files.deleteIfExists(fichero);

            assertThrows(IOException.class, () -> ProductoJsonUtils.importarProductos(fichero));
        }

        @Test
        @DisplayName("lanza NullPointerException si la lista a exportar es null")
        void exportarListaNull() {
            assertThrows(NullPointerException.class, () -> ProductoJsonUtils.exportarProductos(null, fichero));
        }
    }
