package model;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.model.Ingrediente;
import org.utils.IngredienteCsvUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class IngredienteCsvUtilsTest {

    private Path fichero;

    @BeforeEach
    void setUp() throws IOException {
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
        IngredienteCsvUtils.exportarIngredientes(ingredientesDeEjemplo(), fichero);

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
        IngredienteCsvUtils.exportarIngredientes(ingredientesDeEjemplo(), fichero);

        List<Ingrediente> importados = IngredienteCsvUtils.importarIngredientes(fichero);

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
    @DisplayName("lanza IOException si el fichero a importar no existe")
    void importarFicheroInexistente() throws IOException {
        Files.deleteIfExists(fichero);

        assertThrows(IOException.class, () -> IngredienteCsvUtils.importarIngredientes(fichero));
    }

    @Test
    @DisplayName("lanza IOException si el stock del CSV no es un numero")
    void importarCsvInvalido() throws IOException {
        Files.writeString(fichero, "NOMBRE;STOCK DISPONIBLE;VEGANO\nTomate;mucho;true\n");

        assertThrows(IOException.class, () -> IngredienteCsvUtils.importarIngredientes(fichero));
    }

    @Test
    @DisplayName("lanza NullPointerException si la lista a exportar es null")
    void exportarListaNull() {
        assertThrows(NullPointerException.class, () -> IngredienteCsvUtils.exportarIngredientes(null, fichero));
    }
}
