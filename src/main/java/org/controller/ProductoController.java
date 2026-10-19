package org.controller;

import org.exception.ProductoNoEncontradoException;
import org.model.*;

import java.util.ArrayList;
import java.util.List;

public class ProductoController {

    private final List<Producto> catalogo = new ArrayList<>();
    private int siguienteId = 1;

    public List<Producto> listarProducto() {
        return List.copyOf(catalogo);
    }

    public Producto buscarPorId(int id) throws ProductoNoEncontradoException {
        return catalogo.stream()
                .filter(p -> p.getId() == id)
                .findFirst()
                .orElseThrow(() -> new ProductoNoEncontradoException("No existe ningún prodcuto con id" + id));
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



}
