package org.pizzeria.model.product;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.opencsv.bean.CsvBindByName;
import com.opencsv.bean.CsvIgnore;

import java.util.Objects;

public class Ingrediente {
    @CsvIgnore
    private long id;
    @CsvBindByName(column = "Nombre")
    private String nombre;
    @CsvBindByName(column = "Stock disponible")
    private int stock;

    public Ingrediente() {
    }

    public Ingrediente(long id, String nombre, int stock) {
        this.id = id;
        this.nombre = nombre;
        this.stock = stock;
    }
    @JsonCreator
    public Ingrediente(@JsonProperty("nombre") String nombre,
                       @JsonProperty("stock") int stock) {
        this(0, nombre, stock);
    }

    @JsonIgnore
    public long getId() {return id;}
    @JsonIgnore
    public void setId(long id) {this.id = id;}

    public String getNombre() {return nombre;}
    public void setNombre(String nombre) {this.nombre = nombre;}

    public int getStock() {return stock;}
    public void setStock(int stock) {this.stock = stock;}

    @Override
    public String toString() {
        return "Ingrediente{" +
                "id=" + id +
                ", nombre='" + nombre + '\'' +
                ", stock=" + stock +
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

