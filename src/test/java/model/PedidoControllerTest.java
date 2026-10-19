package model;

import org.controller.ClienteController;
import org.controller.PedidoController;
import org.controller.ProductoController;
import org.exception.ClienteNoEncontradoException;
import org.exception.ClienteYaExisteException;
import org.exception.PedidoNoEncontradoException;
import org.exception.ProductoNoEncontradoException;
import org.model.Bebida;
import org.model.Cliente;
import org.model.EstadoPedido;
import org.model.Ingrediente;
import org.model.LineaPedido;
import org.model.Pagable;
import org.model.Pedido;
import org.model.Pizza;
import org.model.Size;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PedidoControllerTest {

    private ClienteController clienteController;
    private PedidoController pedidoController;
    private int clienteId;
    private int margaritaId;   // 8.50 €
    private int aguaId;        // 1.50 €

    @BeforeEach
    void setUp() throws ClienteYaExisteException {
        clienteController = new ClienteController();
        ProductoController productoController = new ProductoController();
        pedidoController = new PedidoController(clienteController, productoController);

        // Los productos de prueba los crea el test; el controlador no trae nada precargado.
        Ingrediente tomate = new Ingrediente(1, "Tomate", 100, true);
        Ingrediente mozzarella = new Ingrediente(2, "Mozzarella", 100, false);
        margaritaId = productoController.agregarProducto(
                new Pizza(0, "Margarita", 8.5, Size.PEQUENYA, List.of(tomate, mozzarella))).getId();
        aguaId = productoController.agregarProducto(
                new Bebida(0, "Agua 50cl", 1.5, Size.PEQUENYA)).getId();

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
    @DisplayName("addLineaPedido()")
    class AddLineaPedido {

        @Test
        @DisplayName("sin pedido pendiente crea un pedido nuevo en estado PENDIENTE asociado al cliente")
        void creaPedidoNuevoSiNoHayPendiente() throws Exception {
            assertTrue(pedidoController.listarTodos().isEmpty());

            LineaPedido linea = pedidoController.addLineaPedido(clienteId, margaritaId, 2);
            Pedido pedido = linea.getPedido();

            assertEquals(1, pedidoController.listarTodos().size());
            assertEquals(clienteId, pedido.getCliente().getId());
            assertEquals(EstadoPedido.PENDIENTE, pedido.getEstado());
            assertEquals(1, pedido.getLineas().size());
            assertEquals(2, linea.getCantidad());
            assertEquals(17.0, pedido.getPrecioTotal(), 0.001);
        }

        @Test
        @DisplayName("el pedido creado queda registrado también dentro del cliente")
        void pedidoQuedaAsociadoAlCliente() throws Exception {
            Pedido pedido = pedidoController.addLineaPedido(clienteId, margaritaId, 1).getPedido();
            Cliente cliente = clienteController.buscarPorId(clienteId);

            assertTrue(cliente.getPedidos().contains(pedido));
        }

        @Test
        @DisplayName("las listas que devuelven getLineas() y getPedidos() no se pueden modificar desde fuera")
        void listasDevueltasSonInmutables() throws Exception {
            Pedido pedido = pedidoController.addLineaPedido(clienteId, margaritaId, 1).getPedido();
            Cliente cliente = clienteController.buscarPorId(clienteId);

            assertThrows(UnsupportedOperationException.class, () -> pedido.getLineas().clear());
            assertThrows(UnsupportedOperationException.class, () -> cliente.getPedidos().clear());
        }

        @Test
        @DisplayName("con pedido pendiente añade la línea a ese mismo pedido, sin crear otro")
        void reutilizaPedidoPendiente() throws Exception {
            Pedido primero = pedidoController.addLineaPedido(clienteId, margaritaId, 1).getPedido();
            Pedido segundo = pedidoController.addLineaPedido(clienteId, aguaId, 2).getPedido();

            assertSame(primero, segundo);
            assertEquals(1, pedidoController.listarTodos().size());
            assertEquals(2, primero.getLineas().size());
        }

        @Test
        @DisplayName("varias líneas se acumulan en el precio total")
        void variasLineasAcumulanPrecioTotal() throws Exception {
            pedidoController.addLineaPedido(clienteId, margaritaId, 1);                     // 8.50 €
            Pedido pedido = pedidoController.addLineaPedido(clienteId, aguaId, 2).getPedido(); // 1.50 € x2

            assertEquals(11.50, pedido.getPrecioTotal(), 0.001);
            assertEquals(11.50, pedido.calcularPrecioTotal(), 0.001);
        }

        @Test
        @DisplayName("si el pedido anterior ya está pagado (FINALIZADO) se crea un pedido nuevo")
        void creaPedidoNuevoTrasPagarElAnterior() throws Exception {
            Pedido primero = pedidoController.addLineaPedido(clienteId, margaritaId, 1).getPedido();
            pedidoController.pagarPedido(primero.getId(), new PagableFalso());

            Pedido segundo = pedidoController.addLineaPedido(clienteId, aguaId, 1).getPedido();

            assertNotSame(primero, segundo);
            assertEquals(2, segundo.getId());
            assertEquals(EstadoPedido.PENDIENTE, segundo.getEstado());
            assertEquals(1, segundo.getLineas().size());
            assertEquals(2, pedidoController.listarPorCliente(clienteId).size());
        }

        @Test
        @DisplayName("cada cliente tiene su propio pedido pendiente")
        void pedidosPendientesIndependientesPorCliente() throws Exception {
            Cliente otro = clienteController.registrar(new Cliente(0, "87654321B", "Carlos Ruiz",
                    "Calle Otra 456", "600333444", "carlos@test.com", "password456"));

            Pedido deAna = pedidoController.addLineaPedido(clienteId, margaritaId, 1).getPedido();
            Pedido deCarlos = pedidoController.addLineaPedido(otro.getId(), aguaId, 1).getPedido();

            assertNotSame(deAna, deCarlos);
            assertEquals(1, deAna.getId());
            assertEquals(2, deCarlos.getId());
            assertEquals(2, pedidoController.listarTodos().size());
        }

        @Test
        @DisplayName("lanza ClienteNoEncontradoException si el cliente no existe")
        void rechazaClienteInexistente() {
            assertThrows(ClienteNoEncontradoException.class,
                    () -> pedidoController.addLineaPedido(999, margaritaId, 1));
        }

        @Test
        @DisplayName("lanza ProductoNoEncontradoException si el producto no existe y no deja un pedido vacío")
        void rechazaProductoInexistente() {
            assertThrows(ProductoNoEncontradoException.class,
                    () -> pedidoController.addLineaPedido(clienteId, 999, 1));
            assertTrue(pedidoController.listarTodos().isEmpty());
        }

        @Test
        @DisplayName("lanza IllegalArgumentException si la cantidad no es positiva")
        void rechazaCantidadNoPositiva() {
            assertThrows(IllegalArgumentException.class,
                    () -> pedidoController.addLineaPedido(clienteId, margaritaId, 0));
            assertTrue(pedidoController.listarTodos().isEmpty());
        }
    }

    @Nested
    @DisplayName("pagarPedido()")
    class PagarPedido {

        @Test
        @DisplayName("cobra el importe total del pedido a través del método de pago")
        void pagaElImporteTotal() throws Exception {
            Pedido pedido = pedidoController.addLineaPedido(clienteId, margaritaId, 2).getPedido(); // 17.0€

            PagableFalso pagable = new PagableFalso();
            pedidoController.pagarPedido(pedido.getId(), pagable);

            assertEquals(17.0, pagable.cantidadCobrada, 0.001);
        }

        @Test
        @DisplayName("tras pagar, el pedido pasa a FINALIZADO")
        void pedidoPagadoQuedaFinalizado() throws Exception {
            Pedido pedido = pedidoController.addLineaPedido(clienteId, margaritaId, 1).getPedido();

            pedidoController.pagarPedido(pedido.getId(), new PagableFalso());

            assertEquals(EstadoPedido.FINALIZADO, pedido.getEstado());
        }

        @Test
        @DisplayName("lanza IllegalStateException si el pedido ya estaba pagado")
        void rechazaPedidoYaPagado() throws Exception {
            Pedido pedido = pedidoController.addLineaPedido(clienteId, margaritaId, 1).getPedido();
            pedidoController.pagarPedido(pedido.getId(), new PagableFalso());

            assertThrows(IllegalStateException.class,
                    () -> pedidoController.pagarPedido(pedido.getId(), new PagableFalso()));
        }

        @Test
        @DisplayName("lanza IllegalArgumentException si el método de pago es null")
        void rechazaMetodoPagoNull() throws Exception {
            Pedido pedido = pedidoController.addLineaPedido(clienteId, margaritaId, 1).getPedido();

            assertThrows(IllegalArgumentException.class,
                    () -> pedidoController.pagarPedido(pedido.getId(), null));
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
            Pedido creado = pedidoController.addLineaPedido(clienteId, margaritaId, 1).getPedido();
            Pedido encontrado = pedidoController.buscarPorId(creado.getId());

            assertSame(creado, encontrado);
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
            Cliente otro = clienteController.registrar(new Cliente(0, "87654321B", "Carlos Ruiz",
                    "Calle Otra 456", "600333444", "carlos@test.com", "password456"));

            Pedido primero = pedidoController.addLineaPedido(clienteId, margaritaId, 1).getPedido();
            pedidoController.pagarPedido(primero.getId(), new PagableFalso());
            pedidoController.addLineaPedido(clienteId, aguaId, 1);   // segundo pedido de Ana
            pedidoController.addLineaPedido(otro.getId(), aguaId, 1); // pedido de Carlos

            List<Pedido> pedidos = pedidoController.listarPorCliente(clienteId);

            assertEquals(2, pedidos.size());
            assertTrue(pedidos.stream().allMatch(p -> p.getCliente().getId() == clienteId));
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
            Cliente otro = clienteController.registrar(new Cliente(0, "87654321B", "Carlos Ruiz",
                    "Calle Otra 456", "600333444", "carlos@test.com", "password456"));

            pedidoController.addLineaPedido(clienteId, margaritaId, 1);
            pedidoController.addLineaPedido(otro.getId(), aguaId, 1);

            assertEquals(2, pedidoController.listarTodos().size());
        }
    }
}