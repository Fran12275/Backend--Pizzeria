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
        validarPrecio(precio);
        this.id = id;
        this.nombre = nombre;
        this.precio = precio;
    }


    public double getPrecio() { return this.precio; }
    public void setPrecio(double precio) {
        validarPrecio(precio);
        this.precio = precio;
    }

    // Misma regla al crear el producto y al cambiarle el precio: siempre mayor que 0
    private static void validarPrecio(double precio) {
        if (precio <= 0) {
            throw new IllegalArgumentException("El precio debe ser mayor que 0");
        }
    }


    public int getId() {
        return id;
    }

    // El id lo asigna el catalogo una sola vez: cambiarlo despues romperia equals/hashCode
    public void setId(int id) {
        if (this.id != 0) {
            throw new IllegalStateException("El producto ya tiene el id " + this.id);
        }
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
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Producto otro = (Producto) o;
        // id 0 = todavia sin id asignado: solo es igual a si mismo
        return this.id != 0 && this.id == otro.id;
    }

    @Override
    public int hashCode() {
        // Coherente con equals: sin id, cada objeto tiene su propio hash
        return this.id != 0 ? Objects.hash(this.id) : System.identityHashCode(this);
    }

    @Override
    public String toString() {
        return getTipo() + "{" +
                "id=" + id +
                ", nombre='" + nombre + '\'' +
                ", precio=" + precio +
                '}';
    }
}