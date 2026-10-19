package org.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Cliente {

    public Cliente() {

    }

    public Cliente(int id, String dni, String nombre, String direccion, String telefono, String email, String password) {
        this.id = id;
        this.dni = dni;
        this.nombre = nombre;
        this.direccion = direccion;
        this.telefono = telefono;
        this.email = email;
        this.password = password;
    }

    public void addPedido(Pedido pedido) {
        Objects.requireNonNull(pedido, "El pedido no puede ser null");
        if (pedido.getCliente() != this) {
            throw new IllegalArgumentException("El pedido no pertenece a este cliente");
        }
        pedidos.add(pedido);
    }
    private int id;
    private String dni;
    private String nombre;
    private String direccion;
    private String telefono;
    private String email;
    private String password;
    private final List<Pedido> pedidos =new ArrayList<>();

    public int getId() {
        return id;
    }
    public void setId(int id) {this.id = id;}

    public String getDni() {
        return dni;
    }
    public void setDni(String dni) {
        this.dni = dni;
    }

    public String getNombre() {
        return nombre;
    }
    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDireccion() {
        return direccion;
    }
    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public String getTelefono() {
        return telefono;
    }
    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getEmail() {
        return email;
    }
    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }
    public void setPassword(String password) {
        this.password = password;
    }

    public List<Pedido> getPedidos() {return List.copyOf(pedidos);}

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Cliente cliente)) return false;
        return id == cliente.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Cliente{" +
                "id=" + id +
                ", dni='" + dni + '\'' +
                ", nombre='" + nombre + '\'' +
                ", email='" + email + '\'' +
                '}';
    }
}


