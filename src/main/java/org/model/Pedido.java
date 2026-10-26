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
    // transient: Gson no puede crear una interfaz al importar, asi que se excluye del JSON
    private transient Pagable pagable;
    private final Cliente cliente;


    public Pedido(int id, Date fecha, Cliente cliente) {
        this.id = id;
        Objects.requireNonNull(fecha, "La fecha no puede ser null");
        // Copia: si quien pasa la fecha la cambia despues, el pedido no se ve afectado
        this.fecha = new Date(fecha.getTime());
        this.estado = EstadoPedido.PENDIENTE;
        this.cliente = Objects.requireNonNull(cliente);
    }

    public void agregarLinea(LineaPedido linea) {
        if (linea == null) {
            throw new IllegalArgumentException();
        }
        comprobarQueEstaPendiente();
        lineas.add(linea);
        linea.setPedido(this);
    }

    public double calcularPrecioTotal() {
         return lineas.stream()
                .mapToDouble(LineaPedido::getSubtotal)
                .sum();
    }

    public void pagar(Pagable metodoPago) {
        if (metodoPago == null) {
            throw new IllegalArgumentException("El método de pago no puede ser null");
        }
        comprobarQueEstaPendiente();
        if (lineas.isEmpty()) {
            throw new IllegalStateException("No se puede pagar un pedido sin líneas");
        }
        metodoPago.pagar(calcularPrecioTotal());
        // Solo se guarda el método de pago y se finaliza si el cobro ha ido bien
        this.pagable = metodoPago;
        this.estado = EstadoPedido.FINALIZADO;
    }

    private void comprobarQueEstaPendiente() {
        if (estado != EstadoPedido.PENDIENTE) {
            throw new IllegalStateException("El pedido " + id + " ya no está pendiente");
        }
    }

    // Al importar con Gson las lineas llegan sin su pedido (es transient): aqui se vuelven a enlazar
    public void reenlazarLineas() {
        for (LineaPedido linea : lineas) {
            linea.setPedido(this);
        }
    }

    public int getId() {
        return id;
    }

    public Date getFecha() {
        // Copia: quien la recibe no puede cambiar la fecha del pedido
        return new Date(fecha.getTime());
    }

    public EstadoPedido getEstado() {
        return estado;
    }

    public List<LineaPedido> getLineas() {
        return List.copyOf(lineas);
    }

    public Pagable getPagable() {
        return pagable;
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


