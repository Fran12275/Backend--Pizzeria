package org.pizzeria.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.opencsv.bean.CsvBindByName;
import com.opencsv.bean.CsvIgnore;

import java.util.Objects;

public class Ingrediente {
    @CsvIgnore
    private int id;
    @CsvBindByName(column = "Nombre")
    private String nombre;
    @CsvBindByName(column = "Stock disponible")
    private int stock;
    @CsvBindByName(column = "Vegano")
    private boolean esVegano;

    public Ingrediente() {
    }

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
        if (this == o) return true;
        if (!(o instanceof Ingrediente that)) return false;
        // id 0 = todavia sin id asignado: solo es igual a si mismo
        return id != 0 && id == that.id;
    }

    @Override
    public int hashCode() {
        // Coherente con equals: sin id, cada objeto tiene su propio hash
        return id != 0 ? Objects.hashCode(id) : System.identityHashCode(this);
    }
}

