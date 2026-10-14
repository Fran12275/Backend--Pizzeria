package org.model;

public class LineaPedido {
    private int id;
    private int cantidad;
    private Pedido pedido;
    private Producto producto;

    public LineaPedido(){

    }
    public LineaPedido(int id, int cantidad, Producto producto) {
        this.producto = producto;
        this.cantidad = cantidad;
        this.id = id;
    }

    public double getSubtotal(){
        return this.producto.getPrecio() * (double)this.cantidad;
    }


    public Pedido getPedido(){return pedido;}
    public void setPedido(Pedido pedido) {this.pedido = pedido;}

    public int getId() {return id;}
    public void setId(int id) {this.id = id;}

    public int getCantidad() {return cantidad;}
    public void setCantidad(int cantidad) {this.cantidad = cantidad;}

    public Producto getProducto() {return producto;}
    public void setProducto(Producto producto) {this.producto = producto;}
}