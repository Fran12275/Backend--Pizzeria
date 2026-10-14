package org.controller;


import org.exception.ProductoNoEncontradoException;
import org.model.*;

import java.util.ArrayList;
import java.util.List;

public class ProductoController {
    private final List<Producto> catalogo = new ArrayList<>();
    private int siguienteId = 1;

    public ProductoController() { cargarCatalogoInicial();}

    private void cargarCatalogoInicial(){
        Ingrediente tomate = new Ingrediente(1,"Tomate",100, true);
        Ingrediente mozzarella = new Ingrediente(2,"Mozzarella", 100,false);
        Ingrediente jamon = new Ingrediente(3, "Jamon", 20, false);
        Ingrediente albahaca = new Ingrediente(4, "Albahaca", 20, true);

        catalogo.add(new Pizza(siguienteId++, "Margarita", 8.5, Size.PEQUENYA,
                List.of(tomate, mozzarella, albahaca)));
        catalogo.add(new Pizza(siguienteId++, "Jamon y queso", 14.5, Size.GRANDE,
                List.of(tomate,mozzarella,jamon)));
        catalogo.add(new Pasta(siguienteId++, "Carbonara", 7.50, Size.MEDIANA,
                List.of(jamon, mozzarella)));
        catalogo.add(new Bebida(siguienteId++, "Agua 50cl", 1.50, Size.PEQUENYA));
        catalogo.add(new Bebida(siguienteId++, "Refresco 33cl", 2.00, Size.PEQUENYA));

    }

    public List<Producto> listarProducto(){return List.copyOf(catalogo);}

    public Producto buscarPorId(int id) throws ProductoNoEncontradoException {
        return catalogo.stream()
                .filter(p -> p.getId() == id)
                .findFirst()
                .orElseThrow(() -> new ProductoNoEncontradoException(""));
    }

    public Producto agregarProducto(Producto producto) {
        if (producto == null){
            throw new IllegalArgumentException("");
        }
        producto.setId(siguienteId++);
        catalogo.add(producto);
        return producto;
    }
}
