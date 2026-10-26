package org.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

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
        if (ingredientes.stream().anyMatch(Objects::isNull)){
            throw new IllegalArgumentException("Ningún ingrediente de la pasta puede ser null");
        }
        this.ingredientes.addAll(ingredientes);
    }

    @JsonCreator
    public Pasta(@JsonProperty("nombre") String nombre,
                 @JsonProperty("precio") double precio,
                 @JsonProperty("size") Size size,
                 @JsonProperty("ingredientes") List<Ingrediente> ingredientes) {
        this(0, nombre, precio, size, ingredientes);
    }

    public Size getSize() {return size;}

    public void setSize(Size size) {this.size = size;}

    public List<Ingrediente> getIngredientes()  {return List.copyOf(ingredientes);}
    public void addIngrediente(Ingrediente ingrediente) {ingredientes.add(Objects.requireNonNull(ingrediente));}

    @Override
    public String getTipo() {
        return "Pasta";
    }


}