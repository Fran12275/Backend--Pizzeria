package org.controller;

import org.exception.ClienteNoEncontradoException;
import org.exception.PedidoNoEncontradoException;
import org.exception.ProductoNoEncontradoException;
import org.model.*;
import org.utils.PedidoGsonUtils;

import java.io.IOException;
import java.nio.file.Path;
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

    public LineaPedido addLineaPedido(int clienteId, int productoId, int cantidad)
            throws ProductoNoEncontradoException, ClienteNoEncontradoException {
        if (cantidad <= 0) { throw new IllegalArgumentException();}
        Cliente cliente = clienteController.buscarPorId(clienteId);
        Producto producto = productoController.buscarPorId(productoId);

        Pedido pedido = buscarPedidoPendiente(cliente);

        if (pedido == null){
            pedido = new Pedido(secuenciaPedidoId.getAndIncrement(), new Date(),cliente);
            pedidos.add(pedido);
        }

        LineaPedido linea = new LineaPedido(secuenciaLineaId.getAndIncrement(), cantidad, producto);
        pedido.agregarLinea(linea);
        return linea;
    }
    public Pedido buscarPedidoPendiente(Cliente cliente){
        return pedidos.stream()
                .filter(p -> p.getCliente().equals(cliente))
                .filter(p -> p.getEstado() == EstadoPedido.PENDIENTE)
                .findFirst()
                .orElse(null);
    }
    public Pedido buscarPorId(int id) throws PedidoNoEncontradoException {
        return pedidos.stream()
                .filter(p ->p.getId() == id)
                .findFirst()
                .orElseThrow(() -> new PedidoNoEncontradoException(""));
    }

    public List<Pedido> listarPorCliente(int clienteId) throws ClienteNoEncontradoException{
        Cliente cliente = clienteController.buscarPorId(clienteId);
        return pedidos.stream()
                .filter(p -> p.getCliente().equals(cliente))
                .toList();
    }

    public List<Pedido> listarTodos() { return List.copyOf(pedidos);}

    public void pagarPedido(int pedidoId, Pagable metodoPago) throws PedidoNoEncontradoException {
        Pedido pedido = buscarPorId(pedidoId);
        pedido.pagar(metodoPago);
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
