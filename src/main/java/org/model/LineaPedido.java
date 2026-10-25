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

    public LineaPedido(){

    }
    public LineaPedido(int id, int cantidad, Producto producto) {
        this.producto = Objects.requireNonNull(producto, "El producto no puede ser null");
        this.cantidad = cantidad;
        this.id = id;
        this.productoId = producto.getId();
        this.precioUnitario = producto.getPrecio();
    }

    public double getSubtotal(){
        return this.precioUnitario * this.cantidad;
    }


    public Pedido getPedido(){return pedido;}
    public void setPedido(Pedido pedido) {this.pedido = pedido;}

    public int getId() {return id;}
    public void setId(int id) {this.id = id;}

    public int getCantidad() {return cantidad;}
    public void setCantidad(int cantidad) {this.cantidad = cantidad;}

    public int getProductoId() {return productoId;}

    public double getPrecioUnitario() {return precioUnitario;}

    public Producto getProducto() {return producto;}
}