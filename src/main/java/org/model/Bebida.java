package org.model;

public class Bebida extends Producto{
    private Size size;

    public Bebida(int id, String nombre, double precio, Size size) {
        super(id, nombre, precio);
        this.size = size;
    }

    public Size getSize() {
        return size;
    }

    public void setSize(Size size) {
        this.size = size;
    }

    @Override
    public String getTipo() {
        return "Bebida";
    }
}
