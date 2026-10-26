package org.pizzeria.utils;

import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import com.opencsv.bean.StatefulBeanToCsv;
import com.opencsv.bean.StatefulBeanToCsvBuilder;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import com.opencsv.exceptions.CsvException;
import com.opencsv.exceptions.CsvRequiredFieldEmptyException;
import org.pizzeria.model.Ingrediente;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

public final class IngredienteCsvUtils {
    public static final String FICHERO_INGREDIENTES = "ingredientes.csv";

    private static final char SEPARADOR = ';';

    private IngredienteCsvUtils() {
    }

    public static void exportarIngredientes(List<Ingrediente> ingredientes) throws IOException {
        exportarIngredientes(ingredientes, Path.of(FICHERO_INGREDIENTES));
    }

    public static void exportarIngredientes(List<Ingrediente> ingredientes, Path ruta) throws IOException {
        Objects.requireNonNull(ingredientes, "La lista de ingredientes no puede ser null");
        Objects.requireNonNull(ruta, "La ruta no puede ser null");
        try (Writer writer = Files.newBufferedWriter(ruta, StandardCharsets.UTF_8)) {
            StatefulBeanToCsv<Ingrediente> csv = new StatefulBeanToCsvBuilder<Ingrediente>(writer)
                    .withSeparator(SEPARADOR)
                    .build();
            csv.write(ingredientes);
        } catch (CsvDataTypeMismatchException | CsvRequiredFieldEmptyException e) {
            throw new IOException("No se han podido exportar los ingredientes a " + ruta, e);
        }
    }

    public static List<Ingrediente> importarIngredientes() throws IOException {
        return importarIngredientes(Path.of(FICHERO_INGREDIENTES));
    }

    public static List<Ingrediente> importarIngredientes(Path ruta) throws IOException {
        Objects.requireNonNull(ruta, "La ruta no puede ser null");
        try (Reader reader = Files.newBufferedReader(ruta, StandardCharsets.UTF_8)) {
            // Sin lanzar excepciones: OpenCSV guarda el error de cada linea en vez de imprimir su traza por consola
            CsvToBean<Ingrediente> csv = new CsvToBeanBuilder<Ingrediente>(reader)
                    .withType(Ingrediente.class)
                    .withSeparator(SEPARADOR)
                    .withThrowExceptions(false)
                    .build();
            List<Ingrediente> ingredientes = leer(csv, ruta);
            comprobarSinErrores(csv.getCapturedExceptions(), ruta);
            return ingredientes;
        }
    }

    // OpenCSV envuelve en RuntimeException los fallos al leer el fichero (por ejemplo, unas comillas sin cerrar):
    // solo esos se convierten en IOException; un error de programacion no se disfraza de "CSV no valido"
    private static List<Ingrediente> leer(CsvToBean<Ingrediente> csv, Path ruta) throws IOException {
        try {
            return csv.parse();
        } catch (RuntimeException e) {
            if (e.getCause() instanceof IOException causa) {
                throw new IOException("El fichero " + ruta + " no tiene un formato CSV valido: " + causa.getMessage(), causa);
            }
            throw e;
        }
    }

    // Si una sola linea no es valida se rechaza el fichero entero, diciendo cual falla y por que
    private static void comprobarSinErrores(List<CsvException> errores, Path ruta) throws IOException {
        if (!errores.isEmpty()) {
            CsvException primero = errores.get(0);
            throw new IOException("El fichero " + ruta + " no tiene un formato CSV valido (linea "
                    + primero.getLineNumber() + ": " + primero.getMessage() + ")", primero);
        }
    }
}
