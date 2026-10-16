package org.controller;


import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.exception.ProductoNoEncontradoException;
import org.model.*;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ProductoController {
    public static final String FICHERO_PRODUCTOS = "productos.json";

    private static final TypeReference<List<Producto>> TIPO_LISTA_PRODUCTOS = new TypeReference<>() {};
    private final ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    private final List<Producto> catalogo = new ArrayList<>();
    private int siguienteId = 1;

    public List<Producto> listarProducto() {
        return List.copyOf(catalogo);
    }

    public Producto buscarPorId(int id) throws ProductoNoEncontradoException {
        return catalogo.stream()
                .filter(p -> p.getId() == id)
                .findFirst()
                .orElseThrow(() -> new ProductoNoEncontradoException(""));
    }

    public Producto agregarProducto(Producto producto) {
        if (producto == null) {
            throw new IllegalArgumentException("");
        }
        producto.setId(siguienteId++);
        catalogo.add(producto);
        return producto;
    }

    public Producto actualizarProducto(Producto producto) throws ProductoNoEncontradoException {
        if (producto == null) {
            throw new IllegalArgumentException();
        }
        Producto existente = buscarPorId(producto.getId());
        catalogo.set(catalogo.indexOf(existente), producto);
        return producto;
    }

    public void eliminarProducto(int id) throws ProductoNoEncontradoException {
        catalogo.remove(buscarPorId(id));
    }

    public void exportarProductos() throws IOException {
        exportarProductos(Path.of(FICHERO_PRODUCTOS));
    }

    public void exportarProductos(Path ruta) throws IOException {
        mapper.writerFor(TIPO_LISTA_PRODUCTOS).writeValue(ruta.toFile(), catalogo);
    }

    public List<Producto> importarProductos() throws IOException {
        return importarProductos(Path.of(FICHERO_PRODUCTOS));
    }

    public List<Producto> importarProductos(Path ruta) throws IOException {
        List<Producto> leidos = mapper.readValue(ruta.toFile(), TIPO_LISTA_PRODUCTOS);
        leidos.forEach(this::agregarProducto);
        return List.copyOf(leidos);
    }

}
