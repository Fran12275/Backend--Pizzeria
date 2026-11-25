package org.pizzeria.model.product;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Pasta extends Producto {
    // La pasta no tiene tamano: un solo precio
    private final BigDecimal precio;
    private final List<Ingrediente> ingredientes = new ArrayList<>();
    // Por defecto false (no vegana): no va en el constructor y se cambia con setVegano
    private boolean vegano;

    public Pasta(long id, String nombre, BigDecimal precio, List<Ingrediente> ingredientes){
        super(id, nombre);
        this.precio = validarPrecio(precio);
        if (ingredientes == null || ingredientes.isEmpty()){
            throw new IllegalArgumentException("La pasta debe tener al menos un ingrediente");
        }
        if (ingredientes.stream().anyMatch(Objects::isNull)){
            throw new IllegalArgumentException("Ningún ingrediente de la pasta puede ser null");
        }
        this.ingredientes.addAll(ingredientes);
    }

    @JsonCreator
    public Pasta(@JsonProperty("nombre") String nombre,
                 @JsonProperty("precio") BigDecimal precio,
                 @JsonProperty("ingredientes") List<Ingrediente> ingredientes) {
        this(0, nombre, precio, ingredientes);
    }

    public BigDecimal getPrecio() {return precio;}

    // Sin tamano: cuesta lo mismo se pida como se pida
    @Override
    public BigDecimal getPrecio(Size size) {return precio;}

    public List<Ingrediente> getIngredientes()  {return List.copyOf(ingredientes);}
    public void addIngrediente(Ingrediente ingrediente) {ingredientes.add(Objects.requireNonNull(ingrediente, "El ingrediente no puede ser null"));}

    public boolean isVegano() {return vegano;}
    public void setVegano(boolean vegano) {this.vegano = vegano;}

    @Override
    public String getTipo() {
        return "Pasta";
    }


}