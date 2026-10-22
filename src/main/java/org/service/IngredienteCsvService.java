package org.service;

import com.opencsv.bean.CsvToBeanBuilder;
import com.opencsv.bean.StatefulBeanToCsv;
import com.opencsv.bean.StatefulBeanToCsvBuilder;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import com.opencsv.exceptions.CsvRequiredFieldEmptyException;
import org.model.Ingrediente;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

public final class IngredienteCsvService {
    public static final String FICHERO_INGREDIENTES = "ingredientes.csv";

    private static final char SEPARADOR = ';';

    private IngredienteCsvService() {
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
            return new CsvToBeanBuilder<Ingrediente>(reader)
                    .withType(Ingrediente.class)
                    .withSeparator(SEPARADOR)
                    .build()
                    .parse();
        } catch (RuntimeException e) {
            // OpenCSV envuelve los errores de lectura (por ejemplo, un stock que no es numero) en RuntimeException
            throw new IOException("El fichero " + ruta + " no tiene un formato CSV valido", e);
        }
    }
}
