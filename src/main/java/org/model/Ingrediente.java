package org.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

public class Ingrediente {
    private int id;
    private String nombre;
    private int stock;
    private boolean esVegano;

    public Ingrediente(int id, String nombre, int stock, boolean esVegano) {
        this.id = id;
        this.nombre = nombre;
        this.stock = stock;
        this.esVegano = esVegano;
    }
    @JsonCreator
    public Ingrediente(@JsonProperty("nombre") String nombre,
                       @JsonProperty("stock") int stock,
                       @JsonProperty("esVegano") boolean esVegano) {
        this(0, nombre, stock, esVegano);
    }

    @JsonIgnore
    public int getId() {return id;}
    @JsonIgnore
    public void setId(int id) {this.id = id;}

    public String getNombre() {return nombre;}
    public void setNombre(String nombre) {this.nombre = nombre;}

    public int getStock() {return stock;}
    public void setStock(int stock) {this.stock = stock;}

    public boolean isEsVegano() {return esVegano;}
    public void setEsVegano(boolean esVegano) {this.esVegano = esVegano;}

    @Override
    public String toString() {
        return "Ingrediente{" +
                "id=" + id +
                ", nombre='" + nombre + '\'' +
                ", stock=" + stock +
                ", esVegano=" + esVegano +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Ingrediente that)) return false;
        return id == that.id;
    }

    @Override
    public int hashCode() {return Objects.hashCode(id);}
}

