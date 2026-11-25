package org.pizzeria.model.pedido;

import org.pizzeria.model.product.Producto;

import java.util.Objects;

public class LineaPedido {
    private long id;
    private int cantidad;
    // transient: el pedido crearia un bucle (Pedido -> lineas -> pedido) y Producto es abstracta.
    // Por eso las lineas importadas llegan sin producto
    private transient Pedido pedido;
    private transient Producto producto;

    // Solo lo usa Gson al importar: desde fuera hay que crear las lineas con producto y cantidad
    private LineaPedido(){

    }
    public LineaPedido(long id, int cantidad, Producto producto) {
        this.producto = Objects.requireNonNull(producto, "El producto no puede ser null");
        if (cantidad <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor que 0");
        }
        this.cantidad = cantidad;
        this.id = id;
    }

    // Gson crea la linea sin pasar por el constructor: al importar se comprueba que sus datos sean validos
    boolean tieneDatosValidos() {
        return cantidad > 0;
    }


    public Pedido getPedido(){return pedido;}
    // Sin public: solo Pedido (mismo paquete) puede decir a que pedido pertenece la linea
    void setPedido(Pedido pedido) {this.pedido = pedido;}

    public long getId() {return id;}

    public int getCantidad() {return cantidad;}

    public Producto getProducto() {return producto;}
}