package org.model;

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

    public int getId() {return id;}
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

