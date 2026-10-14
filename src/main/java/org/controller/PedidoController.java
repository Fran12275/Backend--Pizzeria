package org.controller;

import org.exception.ClienteNoEncontradoException;
import org.exception.PedidoNoEncontradoException;
import org.exception.ProductoNoEncontradoException;
import org.model.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class PedidoController {
    private final List<Pedido> pedidos = new ArrayList<>();
    private final AtomicInteger secuenciaPedidoId = new AtomicInteger(1);
    private final AtomicInteger secuenciaLineaId = new AtomicInteger(1);

    private final ClienteController clienteController;
    private final ProductoController productoController;

    public PedidoController(ClienteController clienteController, ProductoController productoController){
        this.clienteController = clienteController;
        this.productoController = productoController;
    }

    public Pedido crearPedido(int clienteId) throws ClienteNoEncontradoException {
        Cliente cliente = clienteController.buscarPorId(clienteId);
        Pedido pedido = new Pedido(secuenciaPedidoId.getAndIncrement(),new Date(), cliente);
        cliente.addPedido(pedido);
        pedidos.add(pedido);
        return pedido;
    }

    public LineaPedido addLineaPedido(int pedidoId, int productoId, int cantidad)
        throws PedidoNoEncontradoException, ProductoNoEncontradoException {
        Pedido pedido = buscarPorId(pedidoId);
        Producto producto = productoController.buscarPorId(productoId);

        LineaPedido linea = new LineaPedido(secuenciaLineaId.getAndIncrement(), cantidad, producto);
        pedido.agregarLinea(linea);
        return linea;
    }

    public Pedido buscarPorId(int id) throws PedidoNoEncontradoException {
        return pedidos.stream()
                .filter(p ->p.getId() == id)
                .findFirst()
                .orElseThrow(() -> new PedidoNoEncontradoException(""));
    }

    public List<Pedido> listarPorCliente(int clienteId) throws ClienteNoEncontradoException{
        Cliente cliente = clienteController.buscarPorId(clienteId);
        return List.copyOf(cliente.getPedidos());
    }

    public List<Pedido> listarTodos() { return List.copyOf(pedidos);}

    public void pagarPedido(int pedidoId, Pagable metodoPago) throws PedidoNoEncontradoException {
        Pedido pedido = buscarPorId(pedidoId);
        if (pedido.getLineas().isEmpty()) {
            throw new IllegalStateException("No se puede pagar un pedido sin líneas");
        }
        pedido.setPagable(metodoPago);
        pedido.pagar();
    }
}
