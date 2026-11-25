package org.pizzeria.model.pedido;

import org.pizzeria.model.Cliente;
import org.pizzeria.model.Pagable;
import org.pizzeria.model.product.Size;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

public class Pedido {
    private long id;
    private Date fecha;
    private EstadoPedido estado;
    private final List<LineaPedido> lineas = new ArrayList<>();
    // Se le suma cada linea al anadirla, con el precio de ese momento: si despues cambia el precio, el total no
    private BigDecimal precioTotal = BigDecimal.ZERO;
    // transient: Gson no puede crear una interfaz al importar, asi que se excluye del JSON
    private transient Pagable pagable;
    private final Cliente cliente;


    public Pedido(long id, Date fecha, Cliente cliente) {
        this.id = id;
        Objects.requireNonNull(fecha, "La fecha no puede ser null");
        // Copia: si quien pasa la fecha la cambia despues, el pedido no se ve afectado
        this.fecha = new Date(fecha.getTime());
        this.estado = EstadoPedido.PENDIENTE;
        this.cliente = Objects.requireNonNull(cliente, "El cliente no puede ser null");
    }

    // El tamano solo sirve para el precio: en pizzas y bebidas es obligatorio; en la pasta puede ser null
    public void agregarLinea(LineaPedido linea, Size size) {
        if (linea == null) {
            throw new IllegalArgumentException("La línea de pedido no puede ser null");
        }
        comprobarQueEstaPendiente();
        // Primero el importe: si el producto no se vende en ese tamano, lanza la excepcion y el pedido no cambia
        BigDecimal importe = linea.getProducto().getPrecio(size).multiply(BigDecimal.valueOf(linea.getCantidad()));
        lineas.add(linea);
        linea.setPedido(this);
        precioTotal = precioTotal.add(importe);
    }

    public void pagar(Pagable metodoPago) {
        if (metodoPago == null) {
            throw new IllegalArgumentException("El método de pago no puede ser null");
        }
        comprobarQueEstaPendiente();
        if (lineas.isEmpty()) {
            throw new IllegalStateException("No se puede pagar un pedido sin líneas");
        }
        metodoPago.pagar(precioTotal);
        // Solo se guarda el método de pago y se finaliza si el cobro ha ido bien
        this.pagable = metodoPago;
        this.estado = EstadoPedido.FINALIZADO;
    }

    // Solo se puede cancelar un pedido pendiente: uno pagado o ya cancelado no cambia
    public void cancelar() {
        comprobarQueEstaPendiente();
        this.estado = EstadoPedido.CANCELADO;
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

    // Gson crea el pedido sin pasar por el constructor: al importar se comprueba que cumpla lo mismo que exige el constructor
    public void comprobarQueEstaCompleto() {
        comprobarQueExiste(fecha, "la fecha");
        comprobarQueExiste(estado, "el estado");
        comprobarQueExiste(precioTotal, "el precio total");
        comprobarQueExiste(cliente, "el cliente");
        comprobarQueExiste(lineas, "la lista de líneas");
        comprobarLineas();
    }

    private void comprobarQueExiste(Object dato, String nombre) {
        if (dato == null) {
            throw new IllegalStateException("Al pedido " + id + " le falta " + nombre);
        }
    }

    private void comprobarLineas() {
        for (int i = 0; i < lineas.size(); i++) {
            LineaPedido linea = lineas.get(i);
            if (linea == null || !linea.tieneDatosValidos()) {
                throw new IllegalStateException("La línea " + (i + 1) + " del pedido " + id
                        + " está vacía o tiene una cantidad no válida");
            }
        }
    }

    public long getId() {
        return id;
    }

    public Date getFecha() {
        // Copia: quien la recibe no puede cambiar la fecha del pedido
        return new Date(fecha.getTime());
    }

    public EstadoPedido getEstado() {
        return estado;
    }

    public BigDecimal getPrecioTotal() {
        return precioTotal;
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


