package org.pizzeria.controller;

import org.pizzeria.exception.ProductoNoEncontradoException;
import org.pizzeria.model.*;
import org.pizzeria.utils.IngredienteCsvUtils;
import org.pizzeria.utils.ProductoJsonUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ProductoController {

    private final List<Producto> catalogo = new ArrayList<>();
    private int siguienteId = 1;

    public List<Producto> listarTodos() {
        return List.copyOf(catalogo);
    }

    public Producto buscarPorId(int id) throws ProductoNoEncontradoException {
        return catalogo.stream()
                .filter(p -> p.getId() == id)
                .findFirst()
                .orElseThrow(() -> new ProductoNoEncontradoException("No existe ningún producto con id " + id));
    }

    public Producto agregarProducto(Producto producto) {
        if (producto == null) {
            throw new IllegalArgumentException("El producto no puede ser null");
        }
        producto.setId(siguienteId++);
        catalogo.add(producto);
        return producto;
    }

    public Producto actualizarProducto(Producto producto) throws ProductoNoEncontradoException {
        if (producto == null) {
            throw new IllegalArgumentException("El producto no puede ser null");
        }
        Producto existente = buscarPorId(producto.getId());
        catalogo.set(catalogo.indexOf(existente), producto);
        return producto;
    }

    public void eliminarProducto(int id) throws ProductoNoEncontradoException {
        catalogo.remove(buscarPorId(id));
    }

    public List<Ingrediente> importarIngredientes() throws IOException {
        return IngredienteCsvUtils.importarIngredientes();
    }

    public List<Ingrediente> importarIngredientes(Path ruta) throws IOException {
        return IngredienteCsvUtils.importarIngredientes(ruta);
    }

    public void exportarIngredientes(List<Ingrediente> ingredientes) throws IOException {
        IngredienteCsvUtils.exportarIngredientes(ingredientes);
    }

    public void exportarIngredientes(List<Ingrediente> ingredientes, Path ruta) throws IOException {
        IngredienteCsvUtils.exportarIngredientes(ingredientes, ruta);
    }

    public List<Producto> importarProductos() throws IOException {
        return ProductoJsonUtils.importarProductos();
    }

    public List<Producto> importarProductos(Path ruta) throws IOException {
        return ProductoJsonUtils.importarProductos(ruta);
    }

    // Exporta el catalogo completo
    public void exportarProductos() throws IOException {
        ProductoJsonUtils.exportarProductos(listarTodos());
    }

    public void exportarProductos(Path ruta) throws IOException {
        ProductoJsonUtils.exportarProductos(listarTodos(), ruta);
    }


}
