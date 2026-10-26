package org.pizzeria.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.pizzeria.model.Producto;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

public final class ProductoJsonUtils {
    public static final String FICHERO_PRODUCTOS = "productos.json";

    private static final TypeReference<List<Producto>> TIPO_LISTA_PRODUCTOS = new TypeReference<List<Producto>>() {};

    private static final ObjectMapper MAPPER = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    private ProductoJsonUtils() {
    }

    public static void exportarProductos(List<Producto> productos) throws IOException {
        exportarProductos(productos, Path.of(FICHERO_PRODUCTOS));
    }

    public static void exportarProductos(List<Producto> productos,Path ruta) throws IOException {
        Objects.requireNonNull(productos, "La lista de productos no puede ser null");
        Objects.requireNonNull(ruta, "La ruta no puede ser null");
        MAPPER.writerFor(TIPO_LISTA_PRODUCTOS).writeValue(ruta.toFile(), productos);
    }

    public static List<Producto> importarProductos() throws IOException {
        return importarProductos(Path.of(FICHERO_PRODUCTOS));
    }

    public static List<Producto> importarProductos(Path ruta) throws IOException {
        Objects.requireNonNull(ruta, "La ruta no puede ser null");
        List<Producto> productos = MAPPER.readValue(ruta.toFile(), TIPO_LISTA_PRODUCTOS);
        if (productos == null) {
            return List.of();
        }
        comprobarSinNulos(productos, ruta);
        return productos;
    }

    // Un null en la lista solo puede venir de un JSON escrito a mano: se rechaza el fichero entero
    private static void comprobarSinNulos(List<Producto> productos, Path ruta) throws IOException {
        int posicion = productos.indexOf(null);
        if (posicion != -1) {
            throw new IOException("El fichero " + ruta + " contiene un producto null en la posicion " + (posicion + 1));
        }
    }
}
