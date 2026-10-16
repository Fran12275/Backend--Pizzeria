package org.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.Objects;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "tipo" // Este campo aparecerá en el JSON
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = Pizza.class, name = "pizza"),
        @JsonSubTypes.Type(value = Bebida.class, name = "bebida"),
        @JsonSubTypes.Type(value = Pasta.class, name = "pasta"),
})
public abstract class Producto {

    @JsonIgnore
    private int id;
    private String nombre;
    private double precio;

    protected Producto(int id, String nombre, double precio){
        if (precio < 0){
            throw new IllegalArgumentException("");
        }
        else {
            this.id = id;
            this.nombre = nombre;
            this.precio = precio;
        }
    }


    public double getPrecio() { return this.precio; }
    public void setPrecio(double precio) {
        if (precio < 0) {
            throw new IllegalArgumentException("El precio no puede ser negativo");
        } else {
            this.precio = precio;
        }
    }


    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    @JsonIgnore
    public abstract String getTipo();

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (!(o instanceof Producto producto)) {
            return false;
        } else {
            return this.id == producto.id;
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    @Override
    public String toString() {
        String var10000 = this.getTipo();
        return var10000 + "{id=" + this.id + ", nombre='" + this.nombre + "', precio=" + this.precio + "}";
    }
}