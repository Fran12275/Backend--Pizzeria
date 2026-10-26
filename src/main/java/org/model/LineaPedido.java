package org.model;

import java.util.Objects;

public class LineaPedido {
    private int id;
    private int cantidad;
    private int productoId;
    // Precio del producto al crear la linea: si despues cambia, el pedido no se ve afectado
    private double precioUnitario;
    // transient: el pedido crearia un bucle (Pedido -> lineas -> pedido) y Producto es abstracta.
    // Por eso en las lineas importadas producto es null: el producto se identifica por productoId
    private transient Pedido pedido;
    private transient Producto producto;

    // Solo lo usa Gson al importar: desde fuera hay que crear las lineas con producto y cantidad
    private LineaPedido(){

    }
    public LineaPedido(int id, int cantidad, Producto producto) {
        this.producto = Objects.requireNonNull(producto, "El producto no puede ser null");
        if (cantidad <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor que 0");
        }
        this.cantidad = cantidad;
        this.id = id;
        this.productoId = producto.getId();
        this.precioUnitario = producto.getPrecio();
    }

    public double getSubtotal(){
        return this.precioUnitario * this.cantidad;
    }


    public Pedido getPedido(){return pedido;}
    // Sin public: solo Pedido (mismo paquete) puede decir a que pedido pertenece la linea
    void setPedido(Pedido pedido) {this.pedido = pedido;}

    public int getId() {return id;}

    public int getCantidad() {return cantidad;}

    public int getProductoId() {return productoId;}

    public double getPrecioUnitario() {return precioUnitario;}

    public Producto getProducto() {return producto;}
}