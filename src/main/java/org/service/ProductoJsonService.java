package org.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.model.Producto;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

public class ProductoJsonService {
    public static final String FICHERO_PRODUCTOS = "productos.json";

    private static final TypeReference<List<Producto>> TIPO_LISTA_PRODUCTOS = new TypeReference<List<Producto>>() {};

    private final ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    public void exportarProductos(List<Producto> productos) throws IOException {
        exportarProductos(productos, Path.of(FICHERO_PRODUCTOS));
    }

    public void exportarProductos(List<Producto> productos,Path ruta) throws IOException {
        Objects.requireNonNull(productos, "La lista de productos no puede ser null");
        Objects.requireNonNull(ruta, "La ruta no puede ser null");
        mapper.writerFor(TIPO_LISTA_PRODUCTOS).writeValue(ruta.toFile(), productos);
    }

    public List<Producto> importarProductos() throws IOException {
        return importarProductos(Path.of(FICHERO_PRODUCTOS));
    }

    public List<Producto> importarProductos(Path ruta) throws IOException {
        Objects.requireNonNull(ruta, "La ruta no puede ser null");
        return mapper.readValue(ruta.toFile(), TIPO_LISTA_PRODUCTOS);
    }
}
