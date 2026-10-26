package org.pizzeria.controller;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.pizzeria.exception.ClienteNoEncontradoException;
import org.pizzeria.exception.ClienteYaExisteException;
import org.pizzeria.exception.PedidoNoEncontradoException;
import org.pizzeria.exception.ProductoNoEncontradoException;
import org.pizzeria.model.Bebida;
import org.pizzeria.model.Cliente;
import org.pizzeria.model.EstadoPedido;
import org.pizzeria.model.Ingrediente;
import org.pizzeria.model.LineaPedido;
import org.pizzeria.model.Pagable;
import org.pizzeria.model.PagoConTarjeta;
import org.pizzeria.model.PagoEnEfectivo;
import org.pizzeria.model.Pedido;
import org.pizzeria.model.Pizza;
import org.pizzeria.model.Size;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;
import java.util.List;
import java.util.function.Consumer;

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
            assertEquals(17.0, pedido.calcularPrecioTotal(), 0.001);
        }

        @Test
        @DisplayName("el pedido creado aparece en los pedidos del cliente")
        void pedidoQuedaAsociadoAlCliente() throws Exception {
            Pedido pedido = pedidoController.addLineaPedido(clienteId, margaritaId, 1).getPedido();

            assertTrue(pedidoController.listarPorCliente(clienteId).contains(pedido));
        }

        @Test
        @DisplayName("las listas que devuelven getLineas() y listarPorCliente() no se pueden modificar desde fuera")
        void listasDevueltasSonInmutables() throws Exception {
            Pedido pedido = pedidoController.addLineaPedido(clienteId, margaritaId, 1).getPedido();

            assertThrows(UnsupportedOperationException.class, () -> pedido.getLineas().clear());
            assertThrows(UnsupportedOperationException.class, () -> pedidoController.listarPorCliente(clienteId).clear());
        }

        @Test
        @DisplayName("la fecha que devuelve el pedido es una copia: cambiarla no cambia el pedido")
        void fechaNoSeModificaDesdeFuera() throws Exception {
            Pedido pedido = pedidoController.addLineaPedido(clienteId, margaritaId, 1).getPedido();
            long fechaOriginal = pedido.getFecha().getTime();

            pedido.getFecha().setTime(0);

            assertEquals(fechaOriginal, pedido.getFecha().getTime());
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
            IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                    () -> pedidoController.addLineaPedido(clienteId, margaritaId, 0));
            assertEquals("La cantidad debe ser mayor que 0", error.getMessage());
            assertTrue(pedidoController.listarTodos().isEmpty());
        }

        @Test
        @DisplayName("una línea de pedido no se puede crear con cantidad 0 o negativa")
        void lineaConCantidadNoPositivaSeRechaza() {
            Bebida agua = new Bebida(0, "Agua", 1.5, Size.PEQUENYA);

            assertThrows(IllegalArgumentException.class, () -> new LineaPedido(1, 0, agua));
            assertThrows(IllegalArgumentException.class, () -> new LineaPedido(1, -5, agua));
        }

        @Test
        @DisplayName("no se puede añadir una línea null a un pedido")
        void rechazaLineaNull() throws Exception {
            Pedido pedido = pedidoController.addLineaPedido(clienteId, margaritaId, 1).getPedido();

            IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> pedido.agregarLinea(null));
            assertEquals("La línea de pedido no puede ser null", error.getMessage());
        }

        @Test
        @DisplayName("un pedido no se puede crear sin cliente")
        void pedidoSinClienteSeRechaza() {
            NullPointerException error = assertThrows(NullPointerException.class, () -> new Pedido(1, new Date(), null));
            assertEquals("El cliente no puede ser null", error.getMessage());
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
        @DisplayName("pagar el pedido directamente por segunda vez lanza IllegalStateException y no vuelve a cobrar")
        void pagarDirectamenteDosVecesNoCobraDosVeces() throws Exception {
            Pedido pedido = pedidoController.addLineaPedido(clienteId, margaritaId, 1).getPedido();
            PagableFalso pagable = new PagableFalso();
            pedido.pagar(pagable);
            pagable.cantidadCobrada = null;

            assertThrows(IllegalStateException.class, () -> pedido.pagar(pagable));
            assertNull(pagable.cantidadCobrada);
        }

        @Test
        @DisplayName("un pedido ya pagado no admite más líneas")
        void pedidoPagadoNoAdmiteLineas() throws Exception {
            Pedido pedido = pedidoController.addLineaPedido(clienteId, margaritaId, 1).getPedido();
            pedidoController.pagarPedido(pedido.getId(), new PagableFalso());
            LineaPedido extra = new LineaPedido(99, 1, new Bebida(0, "Cola", 2.0, Size.PEQUENYA));

            assertThrows(IllegalStateException.class, () -> pedido.agregarLinea(extra));
            assertEquals(1, pedido.getLineas().size());
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

        @Test
        @DisplayName("pagar con tarjeta o en efectivo exige una cantidad mayor que 0")
        void metodosDePagoRechazanCantidadNoPositiva() {
            IllegalArgumentException tarjeta = assertThrows(IllegalArgumentException.class,
                    () -> new PagoConTarjeta().pagar(0));
            IllegalArgumentException efectivo = assertThrows(IllegalArgumentException.class,
                    () -> new PagoEnEfectivo().pagar(-1));

            assertEquals("La cantidad a pagar debe ser mayor que 0", tarjeta.getMessage());
            assertEquals("La cantidad a pagar debe ser mayor que 0", efectivo.getMessage());
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
            PedidoNoEncontradoException error = assertThrows(PedidoNoEncontradoException.class,
                    () -> pedidoController.buscarPorId(999));
            assertEquals("No existe ningún pedido con id 999", error.getMessage());
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

    @Nested
    @DisplayName("importarPedidos() y exportarPedidos()")
    class PedidosGson {

        private Path fichero;

        @BeforeEach
        void crearFicheroTemporal() throws IOException {
            fichero = Files.createTempFile("pedidos", ".json");
        }

        @AfterEach
        void borrarFicheroTemporal() throws IOException {
            Files.deleteIfExists(fichero);
        }

        // Pedido de Ana con 2 margaritas (8.50 €) y 1 agua (1.50 €): 18.50 € en total
        private Pedido crearPedidoDeEjemplo() throws Exception {
            pedidoController.addLineaPedido(clienteId, margaritaId, 2);
            return pedidoController.addLineaPedido(clienteId, aguaId, 1).getPedido();
        }

        // Exporta el pedido de ejemplo y lo estropea a mano, como si alguien hubiera editado el JSON
        private void exportarYEstropear(Consumer<JsonObject> estropear) throws Exception {
            crearPedidoDeEjemplo();
            pedidoController.exportarPedidos(fichero);
            JsonArray raiz = JsonParser.parseString(Files.readString(fichero)).getAsJsonArray();
            estropear.accept(raiz.get(0).getAsJsonObject());
            Files.writeString(fichero, raiz.toString());
        }

        @Test
        @DisplayName("el JSON no contiene pagable, ni producto ni pedido dentro de las lineas")
        void jsonSinCamposExcluidos() throws Exception {
            Pedido creado = crearPedidoDeEjemplo();
            pedidoController.pagarPedido(creado.getId(), new PagableFalso()); // pagado: tiene un Pagable que no debe salir en el JSON
            pedidoController.exportarPedidos(fichero);

            JsonArray raiz = JsonParser.parseString(Files.readString(fichero)).getAsJsonArray();
            assertEquals(1, raiz.size());
            JsonObject pedido = raiz.get(0).getAsJsonObject();
            assertFalse(pedido.has("pagable"));
            assertTrue(pedido.has("cliente"));
            for (JsonElement linea : pedido.get("lineas").getAsJsonArray()) {
                assertFalse(linea.getAsJsonObject().has("producto"));
                assertFalse(linea.getAsJsonObject().has("pedido"));
                assertTrue(linea.getAsJsonObject().has("cantidad"));
            }
        }

        @Test
        @DisplayName("exportar e importar conserva el pedido, su cliente y sus lineas, sin añadirlo al controlador")
        void exportarEImportarConservaLosDatos() throws Exception {
            Pedido creado = crearPedidoDeEjemplo();
            pedidoController.exportarPedidos(fichero);

            List<Pedido> importados = pedidoController.importarPedidos(fichero);

            assertEquals(1, importados.size());
            Pedido pedido = importados.get(0);
            assertEquals(creado.getId(), pedido.getId());
            assertEquals(EstadoPedido.PENDIENTE, pedido.getEstado());
            assertNotNull(pedido.getFecha());
            assertEquals("Ana García", pedido.getCliente().getNombre());
            assertNull(pedido.getPagable());
            assertEquals(2, pedido.getLineas().size());
            assertEquals(2, pedido.getLineas().get(0).getCantidad());
            assertNull(pedido.getLineas().get(0).getProducto());
            assertSame(pedido, pedido.getLineas().get(0).getPedido());
            assertEquals(1, pedidoController.listarTodos().size());
        }

        @Test
        @DisplayName("un pedido importado calcula su total y sabe que producto tenia cada linea")
        void pedidoImportadoCalculaSuTotal() throws Exception {
            crearPedidoDeEjemplo();
            pedidoController.exportarPedidos(fichero);

            Pedido pedido = pedidoController.importarPedidos(fichero).get(0);

            assertEquals(18.5, pedido.calcularPrecioTotal(), 0.001); // 2 x 8.50 + 1 x 1.50
            assertEquals(margaritaId, pedido.getLineas().get(0).getProductoId());
            assertEquals(8.5, pedido.getLineas().get(0).getPrecioUnitario(), 0.001);
        }

        @Test
        @DisplayName("lanza IOException si el fichero a importar no existe")
        void importarFicheroInexistente() throws IOException {
            Files.deleteIfExists(fichero);

            assertThrows(IOException.class, () -> pedidoController.importarPedidos(fichero));
        }

        @Test
        @DisplayName("lanza IOException si el fichero no tiene un formato JSON valido")
        void importarJsonInvalido() throws IOException {
            Files.writeString(fichero, "esto no es JSON [");

            assertThrows(IOException.class, () -> pedidoController.importarPedidos(fichero));
        }

        @Test
        @DisplayName("sin pedidos exporta una lista vacía")
        void exportarSinPedidos() throws IOException {
            pedidoController.exportarPedidos(fichero);

            assertTrue(pedidoController.importarPedidos(fichero).isEmpty());
        }

        @Test
        @DisplayName("lanza IOException si la lista contiene un pedido null y dice en qué posición está")
        void importarListaConPedidoNull() throws IOException {
            Files.writeString(fichero, "[null]");

            IOException error = assertThrows(IOException.class, () -> pedidoController.importarPedidos(fichero));
            assertTrue(error.getMessage().contains("posicion 1"));
        }

        @ParameterizedTest
        @ValueSource(strings = {"fecha", "estado", "cliente", "lineas"})
        @DisplayName("lanza IOException si a un pedido le falta un dato obligatorio")
        void importarPedidoIncompleto(String campo) throws Exception {
            exportarYEstropear(pedido -> pedido.remove(campo));

            IOException error = assertThrows(IOException.class, () -> pedidoController.importarPedidos(fichero));
            assertTrue(error.getMessage().contains("le falta"));
        }

        @Test
        @DisplayName("lanza IOException si una línea tiene cantidad 0")
        void importarPedidoConLineaDeCantidadCero() throws Exception {
            exportarYEstropear(pedido -> pedido.getAsJsonArray("lineas").get(0).getAsJsonObject().addProperty("cantidad", 0));

            IOException error = assertThrows(IOException.class, () -> pedidoController.importarPedidos(fichero));
            assertTrue(error.getMessage().contains("línea 1"));
        }

        @Test
        @DisplayName("lanza IOException si una línea es null")
        void importarPedidoConLineaNull() throws Exception {
            exportarYEstropear(pedido -> pedido.getAsJsonArray("lineas").set(0, JsonNull.INSTANCE));

            IOException error = assertThrows(IOException.class, () -> pedidoController.importarPedidos(fichero));
            assertTrue(error.getMessage().contains("línea 1"));
        }
    }
}