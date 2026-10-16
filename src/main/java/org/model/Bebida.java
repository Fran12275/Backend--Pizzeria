package org.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Bebida extends Producto{
    private Size size;

    public Bebida(int id, String nombre, double precio, Size size) {
        super(id, nombre, precio);
        this.size = size;
    }

    @JsonCreator
    public Bebida(@JsonProperty("nombre") String nombre,
                  @JsonProperty("precio") double precio,
                  @JsonProperty("size") Size size) {
        this(0, nombre, precio, size);
    }

    public Size getSize() {
        return size;
    }

    public void setSize(Size size) {
        this.size = size;
    }

    @Override
    public String getTipo() {
        return "Bebida";
    }
}
