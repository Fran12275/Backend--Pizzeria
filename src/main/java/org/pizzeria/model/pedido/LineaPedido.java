package org.pizzeria.model.pedido;

import org.pizzeria.model.product.Producto;
import org.pizzeria.model.product.Size;

import java.math.BigDecimal;
import java.util.Objects;

public class LineaPedido {
    private long id;
    private int cantidad;
    // Precio de una unidad al pedirla: si despues cambia en el catalogo, la linea no. Sirve para quitarla del total
    private BigDecimal precioUnitario;
    // transient: el pedido crearia un bucle (Pedido -> lineas -> pedido) y Producto es abstracta.
    // Por eso las lineas importadas llegan sin producto
    private transient Pedido pedido;
    private transient Producto producto;

    // Solo lo usa Gson al importar: desde fuera hay que crear las lineas con producto y cantidad
    private LineaPedido(){

    }
    // El tamano solo sirve para buscar el precio (en la pasta puede ser null): si no se vende en ese tamano, no se crea
    public LineaPedido(long id, int cantidad, Producto producto, Size size) {
        this.producto = Objects.requireNonNull(producto, "El producto no puede ser null");
        if (cantidad <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor que 0");
        }
        this.precioUnitario = producto.getPrecio(size);
        this.cantidad = cantidad;
        this.id = id;
    }

    // Gson crea la linea sin pasar por el constructor: al importar se comprueba que sus datos sean validos
    boolean tieneDatosValidos() {
        return cantidad > 0 && precioUnitario != null && precioUnitario.signum() > 0;
    }


    public Pedido getPedido(){return pedido;}
    // Sin public: solo Pedido (mismo paquete) puede decir a que pedido pertenece la linea
    void setPedido(Pedido pedido) {this.pedido = pedido;}

    public long getId() {return id;}

    public int getCantidad() {return cantidad;}

    public BigDecimal getPrecioUnitario() {return precioUnitario;}

    // Lo que suma la linea en el total del pedido (y lo que habria que restar al quitarla)
    public BigDecimal getSubtotal() {return precioUnitario.multiply(BigDecimal.valueOf(cantidad));}

    public Producto getProducto() {return producto;}
}