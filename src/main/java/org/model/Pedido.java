package org.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

public class Pedido {
    private int id;
    private Date fecha;
    private EstadoPedido estado;
    private final List<LineaPedido> lineas = new ArrayList<>();
    private Pagable pagable;
    private final Cliente cliente;


    public Pedido(int id, Date fecha, Cliente cliente) {
        this.id = id;
        this.fecha = fecha;
        this.estado = EstadoPedido.PENDIENTE;
        this.cliente = Objects.requireNonNull(cliente);
    }

    public void agregarLinea(LineaPedido linea) {
        if (linea == null) {
            throw new IllegalArgumentException();
        }
        lineas.add(linea);
        linea.setPedido(this);
    }

    public double calcularPrecioTotal() {
         return lineas.stream()
                .mapToDouble(LineaPedido::getSubtotal)
                .sum();
    }

    public void pagar() {
        if(pagable == null){throw  new IllegalStateException("");}
        pagable.pagar(calcularPrecioTotal());
        this.estado = EstadoPedido.FINALIZADO;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Date getFecha() {
        return fecha;
    }

    public void setFecha(Date fecha) {
        this.fecha = fecha;
    }

    public EstadoPedido getEstado() {
        return estado;
    }

    public void setEstado(EstadoPedido estado) {
        this.estado = estado;
    }

    public List<LineaPedido> getLineas() {
        return lineas;
    }

    public Pagable getPagable() {
        return pagable;
    }

    public void setPagable(Pagable pagable) {
        this.pagable = pagable;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public double getPrecioTotal() {return calcularPrecioTotal();}


    @Override
    public String toString() {
        return "Pedido{" +
                "id=" + id +
                ", fecha=" + fecha +
                ", precioTotal=" + calcularPrecioTotal() +
                ", estado=" + estado +
                ", lineas=" + lineas.size() +
                '}';
    }
}


