package org.pizzeria.controller;

import org.pizzeria.exception.ClienteNoEncontradoException;
import org.pizzeria.exception.PedidoNoEncontradoException;
import org.pizzeria.exception.ProductoNoEncontradoException;
import org.pizzeria.model.Cliente;
import org.pizzeria.model.pedido.EstadoPedido;
import org.pizzeria.model.pedido.LineaPedido;
import org.pizzeria.model.Pagable;
import org.pizzeria.model.pedido.Pedido;
import org.pizzeria.model.product.Producto;
import org.pizzeria.model.product.Size;
import org.pizzeria.utils.PedidoGsonUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

public class PedidoController {
    private final List<Pedido> pedidos = new ArrayList<>();
    private final AtomicLong secuenciaPedidoId = new AtomicLong(1);
    private final AtomicLong secuenciaLineaId = new AtomicLong(1);

    private final ClienteController clienteController;
    private final ProductoController productoController;

    public PedidoController(ClienteController clienteController, ProductoController productoController){
        this.clienteController = clienteController;
        this.productoController = productoController;
    }

    // Anadir al carrito: producto, cantidad y tamano. El tamano solo sirve para el precio (en la pasta puede ser null)
    public LineaPedido addLineaPedido(long clienteId, long productoId, int cantidad, Size size)
            throws ProductoNoEncontradoException, ClienteNoEncontradoException, SQLException {
        if (cantidad <= 0) { throw new IllegalArgumentException("La cantidad debe ser mayor que 0");}
        Cliente cliente = clienteController.buscarPorId(clienteId);
        Producto producto = productoController.buscarPorId(productoId);

        Pedido pedido = buscarPedidoPendiente(cliente)
                .orElseGet(() -> crearPedido(cliente));

        LineaPedido linea = new LineaPedido(secuenciaLineaId.getAndIncrement(), cantidad, producto, size);
        pedido.agregarLinea(linea);
        guardarSiEsNuevo(pedido);
        return linea;
    }
    private Optional<Pedido> buscarPedidoPendiente(Cliente cliente){
        return pedidos.stream()
                .filter(p -> p.getCliente().equals(cliente))
                .filter(p -> p.getEstado() == EstadoPedido.PENDIENTE)
                .findFirst();
    }
    private Pedido crearPedido(Cliente cliente){
        return new Pedido(secuenciaPedidoId.getAndIncrement(), new Date(), cliente);
    }
    // Un pedido nuevo solo se guarda cuando ya tiene su primera linea: si el tamano no valia, no queda un pedido vacio
    private void guardarSiEsNuevo(Pedido pedido){
        if (!pedidos.contains(pedido)) {
            pedidos.add(pedido);
        }
    }
    public Pedido buscarPorId(long id) throws PedidoNoEncontradoException {
        return pedidos.stream()
                .filter(p ->p.getId() == id)
                .findFirst()
                .orElseThrow(() -> new PedidoNoEncontradoException("No existe ningún pedido con id " + id));
    }

    public List<Pedido> listarPorCliente(long clienteId) throws ClienteNoEncontradoException, SQLException{
        Cliente cliente = clienteController.buscarPorId(clienteId);
        return pedidos.stream()
                .filter(p -> p.getCliente().equals(cliente))
                .toList();
    }

    public List<Pedido> listarTodos() { return List.copyOf(pedidos);}

    public void pagarPedido(long pedidoId, Pagable metodoPago) throws PedidoNoEncontradoException {
        Pedido pedido = buscarPorId(pedidoId);
        pedido.pagar(metodoPago);
    }

    public void cancelarPedido(long pedidoId) throws PedidoNoEncontradoException {
        Pedido pedido = buscarPorId(pedidoId);
        pedido.cancelar();
    }

    public List<Pedido> importarPedidos() throws IOException {
        return PedidoGsonUtils.importarPedidos();
    }

    public List<Pedido> importarPedidos(Path ruta) throws IOException {
        return PedidoGsonUtils.importarPedidos(ruta);
    }

    // Exporta todos los pedidos del controlador
    public void exportarPedidos() throws IOException {
        PedidoGsonUtils.exportarPedidos(listarTodos());
    }

    public void exportarPedidos(Path ruta) throws IOException {
        PedidoGsonUtils.exportarPedidos(listarTodos(), ruta);
    }
}
