package org.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

public class Pedido {
    private int id;
    private Date fecha;
    private float precioTotal;
    private EstadoPedido estado;
    private final List<LineaPedido> lineas = new ArrayList<>();
    private Pagable pagable;
    private final Cliente cliente;


    public Pedido(int id, Date fecha, Cliente cliente) {
        this.id = id;
        this.fecha = fecha;
        this.estado = EstadoPedido.PENDIENTE;
        this.precioTotal = 0f;
        this.cliente = Objects.requireNonNull(cliente);
    }

    public void agregarLinea(LineaPedido linea) {
        if (linea == null) {
            throw new IllegalArgumentException();
        }
        lineas.add(linea);
        linea.setPedido(this);
        recalcularPrecioTotal();
    }

    private void recalcularPrecioTotal() {
        this.precioTotal = (float) lineas.stream()
                .mapToDouble(LineaPedido::getSubtotal)
                .sum();
    }

    public void pagar() {
        pagable.pagar(precioTotal);
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

    public float getPrecioTotal() {
        return precioTotal;
    }

    public void setPrecioTotal(float precioTotal) {
        this.precioTotal = precioTotal;
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


    @Override
    public String toString() {
        return "Pedido{" +
                "id=" + id +
                ", fecha=" + fecha +
                ", precioTotal=" + precioTotal +
                ", estado=" + estado +
                ", lineas=" + lineas.size() +
                '}';
    }
}


