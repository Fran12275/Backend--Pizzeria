package org.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Pasta extends Producto{
    private Size size;
    private final List<Ingrediente> ingredientes = new ArrayList<>();

    public Pasta(int id, String nombre, double precio, Size size, List<Ingrediente> ingredientes){
        super(id, nombre, precio);
        this.size = size;
        if (ingredientes == null || ingredientes.isEmpty()){
            throw new IllegalArgumentException();
        }
        this.ingredientes.addAll(ingredientes);
    }

    public Size getSize() {return size;}

    public void setSize(Size size) {this.size = size;}

    public List<Ingrediente> getIngredientes()  {return ingredientes;}
    public void addIngrediente(Ingrediente ingrediente) {ingredientes.add(Objects.requireNonNull(ingrediente));}

    @Override
    public String getTipo() {
        return "Pasta";
    }


}