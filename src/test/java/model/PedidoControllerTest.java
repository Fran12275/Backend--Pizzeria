package model;


import org.controller.ClienteController;
import org.controller.PedidoController;
import org.controller.ProductoController;
import org.exception.ClienteNoEncontradoException;
import org.exception.ClienteYaExisteException;
import org.exception.PedidoNoEncontradoException;
import org.exception.ProductoNoEncontradoException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.model.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PedidoControllerTest {

    private ClienteController clienteController;
    private PedidoController pedidoController;
    private int clienteId;

    @BeforeEach
    void setUp() throws ClienteYaExisteException {
        clienteController = new ClienteController();
        ProductoController productoController = new ProductoController();
        pedidoController = new PedidoController(clienteController, productoController);

        Cliente cliente = clienteController.registrar(new Cliente(0, "12345678A", "Ana García",
                "Calle Falsa 123", "600111222", "ana@test.com", "password123"));
        clienteId = cliente.getId();
    }

    /**
     * Pagable de prueba que solo registra la última cantidad cobrada,
     * para poder comprobar que pagarPedido() llega a invocar el cobro real.
     */
    private static class PagableFalso implements Pagable {
        Double cantidadCobrada;

        @Override
        public void pagar(double cantidad) {
            this.cantidadCobrada = cantidad;
        }
    }

    @Nested
    @DisplayName("crearPedido()")
    class CrearPedido {

        @Test
        @DisplayName("crea un pedido vacío asociado al cliente, en estado PENDIENTE")
        void creaPedidoVacio() throws ClienteNoEncontradoException {
            Pedido pedido = pedidoController.crearPedido(clienteId);

            assertEquals(clienteId, pedido.getCliente().getId());
            assertTrue(pedido.getLineas().isEmpty());
            assertEquals(0f, pedido.getPrecioTotal());
            assertEquals(EstadoPedido.PENDIENTE, pedido.getEstado());
        }

        @Test
        @DisplayName("el pedido creado queda registrado también dentro del cliente")
        void pedidoQuedaAsociadoAlCliente() throws ClienteNoEncontradoException {
            Pedido pedido = pedidoController.crearPedido(clienteId);
            Cliente cliente = clienteController.buscarPorId(clienteId);

            assertTrue(cliente.getPedidos().contains(pedido));
        }

        @Test
        @DisplayName("lanza ClienteNoEncontradoException si el cliente no existe")
        void rechazaClienteInexistente() {
            assertThrows(ClienteNoEncontradoException.class,
                    () -> pedidoController.crearPedido(999));
        }

        @Test
        @DisplayName("asigna ids incrementales a pedidos sucesivos")
        void asignaIdsIncrementales() throws ClienteNoEncontradoException {
            Pedido p1 = pedidoController.crearPedido(clienteId);
            Pedido p2 = pedidoController.crearPedido(clienteId);

            assertEquals(1, p1.getId());
            assertEquals(2, p2.getId());
        }
    }

    @Nested
    @DisplayName("addLineaPedido()")
    class AddLineaPedido {

        @Test
        @DisplayName("añade la línea y recalcula el precio total del pedido")
        void addLineaRecalculaPrecioTotal() throws Exception {
            Pedido pedido = pedidoController.crearPedido(clienteId);
            // Producto id=1 precargado en ProductoController: "Margarita", 8.50€
            LineaPedido linea = pedidoController.addLineaPedido(pedido.getId(), 1, 2);

            assertEquals(1, pedido.getLineas().size());
            assertEquals(2, linea.getCantidad());
            assertEquals(17.0f, pedido.getPrecioTotal(), 0.001);
        }

        @Test
        @DisplayName("varias líneas se acumulan en el precio total")
        void variasLineasAcumulanPrecioTotal() throws Exception {
            Pedido pedido = pedidoController.crearPedido(clienteId);
            pedidoController.addLineaPedido(pedido.getId(), 1, 1); // Margarita 8.50€
            pedidoController.addLineaPedido(pedido.getId(), 4, 2); // Agua 50cl 1.50€ x2

            assertEquals(2, pedido.getLineas().size());
            assertEquals(11.50f, pedido.getPrecioTotal(), 0.001);
        }

        @Test
        @DisplayName("lanza PedidoNoEncontradoException si el pedido no existe")
        void rechazaPedidoInexistente() {
            assertThrows(PedidoNoEncontradoException.class,
                    () -> pedidoController.addLineaPedido(999, 1, 1));
        }

        @Test
        @DisplayName("lanza ProductoNoEncontradoException si el producto no existe")
        void rechazaProductoInexistente() throws ClienteNoEncontradoException {
            Pedido pedido = pedidoController.crearPedido(clienteId);

            assertThrows(ProductoNoEncontradoException.class,
                    () -> pedidoController.addLineaPedido(pedido.getId(), 999, 1));
        }
    }

    @Nested
    @DisplayName("pagarPedido()")
    class PagarPedido {

        @Test
        @DisplayName("cobra el importe total del pedido a través del método de pago")
        void pagaElImporteTotal() throws Exception {
            Pedido pedido = pedidoController.crearPedido(clienteId);
            pedidoController.addLineaPedido(pedido.getId(), 1, 2); // 17.0€

            PagableFalso pagable = new PagableFalso();
            pedidoController.pagarPedido(pedido.getId(), pagable);

            assertEquals(17.0, pagable.cantidadCobrada, 0.001);
        }

        @Test
        @DisplayName("lanza IllegalStateException si el pedido no tiene líneas")
        void rechazaPedidoSinLineas() throws ClienteNoEncontradoException {
            Pedido pedido = pedidoController.crearPedido(clienteId);

            assertThrows(IllegalStateException.class,
                    () -> pedidoController.pagarPedido(pedido.getId(), new PagableFalso()));
        }

        @Test
        @DisplayName("lanza PedidoNoEncontradoException si el pedido no existe")
        void rechazaPedidoInexistente() {
            assertThrows(PedidoNoEncontradoException.class,
                    () -> pedidoController.pagarPedido(999, new PagableFalso()));
        }
    }

    @Nested
    @DisplayName("buscarPorId()")
    class BuscarPorId {

        @Test
        @DisplayName("encuentra un pedido existente")
        void encuentraPedidoExistente() throws Exception {
            Pedido creado = pedidoController.crearPedido(clienteId);
            Pedido encontrado = pedidoController.buscarPorId(creado.getId());

            assertEquals(creado.getId(), encontrado.getId());
        }

        @Test
        @DisplayName("lanza PedidoNoEncontradoException si no existe")
        void lanzaExcepcionSiNoExiste() {
            assertThrows(PedidoNoEncontradoException.class,
                    () -> pedidoController.buscarPorId(999));
        }
    }

    @Nested
    @DisplayName("listarPorCliente() y listarTodos()")
    class Listados {

        @Test
        @DisplayName("listarPorCliente devuelve solo los pedidos de ese cliente")
        void listarPorClienteDevuelveSusPedidos() throws Exception {
            pedidoController.crearPedido(clienteId);
            pedidoController.crearPedido(clienteId);

            List<Pedido> pedidos = pedidoController.listarPorCliente(clienteId);

            assertEquals(2, pedidos.size());
        }

        @Test
        @DisplayName("listarPorCliente lanza ClienteNoEncontradoException si el cliente no existe")
        void listarPorClienteRechazaClienteInexistente() {
            assertThrows(ClienteNoEncontradoException.class,
                    () -> pedidoController.listarPorCliente(999));
        }

        @Test
        @DisplayName("listarTodos devuelve todos los pedidos creados, de cualquier cliente")
        void listarTodosDevuelveTodosLosPedidos() throws Exception {
            Cliente otroCliente = clienteController.registrar(new Cliente(0, "87654321B", "Carlos Ruiz",
                    "Calle Otra 456", "600333444", "carlos@test.com", "password456"));

            pedidoController.crearPedido(clienteId);
            pedidoController.crearPedido(otroCliente.getId());

            assertEquals(2, pedidoController.listarTodos().size());
        }
    }
}
