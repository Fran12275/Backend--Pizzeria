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
import org.pizzeria.model.product.Bebida;
import org.pizzeria.model.Cliente;
import org.pizzeria.model.pedido.EstadoPedido;
import org.pizzeria.model.product.Ingrediente;
import org.pizzeria.model.pedido.LineaPedido;
import org.pizzeria.model.Pagable;
import org.pizzeria.model.PagoConTarjeta;
import org.pizzeria.model.PagoEnEfectivo;
import org.pizzeria.model.pedido.Pedido;
import org.pizzeria.model.product.Pasta;
import org.pizzeria.model.product.Pizza;
import org.pizzeria.model.product.Size;
import org.pizzeria.utils.EsquemaBaseDatos;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

class PedidoControllerTest {

    private ClienteController clienteController;
    private ProductoController productoController;
    private PedidoController pedidoController;
    private long clienteId;
    private long margaritaId;   // 8.50 € pequeña, 12.00 € grande
    private long aguaId;        // 1.50 €, solo pequeña

    @BeforeEach
    void setUp() throws ClienteYaExisteException, SQLException {
        // Los clientes ya se guardan en MySQL: cada test empieza con las tablas vacias
        EsquemaBaseDatos.borrarYCrearTablas();
        clienteController = new ClienteController();
        productoController = new ProductoController();
        pedidoController = new PedidoController(clienteController, productoController);

        // Los productos de prueba los crea el test; el controlador no trae nada precargado.
        Ingrediente tomate = new Ingrediente(1, "Tomate", 100);
        Ingrediente mozzarella = new Ingrediente(2, "Mozzarella", 100);
        margaritaId = productoController.agregarProducto(new Pizza(0, "Margarita",
                Map.of(Size.PEQUENYA, new BigDecimal("8.50"), Size.GRANDE, new BigDecimal("12.00")),
                List.of(tomate, mozzarella))).getId();
        aguaId = productoController.agregarProducto(
                new Bebida(0, "Agua 50cl", Map.of(Size.PEQUENYA, new BigDecimal("1.50")))).getId();

        Cliente cliente = clienteController.registrar(new Cliente(0, "12345678A", "Ana García",
                "Calle Falsa 123", "600111222", "ana@test.com", "password123"));
        clienteId = cliente.getId();
    }

    /**
     * Pagable de prueba que solo registra la última cantidad cobrada,
     * para poder comprobar que pagarPedido() llega a invocar el cobro real.
     */
    private static class PagableFalso implements Pagable {
        BigDecimal cantidadCobrada;

        @Override
        public void pagar(BigDecimal cantidad) {
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

            LineaPedido linea = pedidoController.addLineaPedido(clienteId, margaritaId, 2, Size.PEQUENYA);
            Pedido pedido = linea.getPedido();

            assertEquals(1, pedidoController.listarTodos().size());
            assertEquals(clienteId, pedido.getCliente().getId());
            assertEquals(EstadoPedido.PENDIENTE, pedido.getEstado());
            assertEquals(1, pedido.getLineas().size());
            assertEquals(2, linea.getCantidad());
            assertEquals(new BigDecimal("17.00"), pedido.getPrecioTotal());
        }

        @Test
        @DisplayName("la línea guarda el tamaño elegido, su precio y su subtotal")
        void lineaGuardaTamanyoYPrecioElegidos() throws Exception {
            LineaPedido linea = pedidoController.addLineaPedido(clienteId, margaritaId, 2, Size.GRANDE);

            assertEquals(Optional.of(Size.GRANDE), linea.getSize());
            assertEquals(new BigDecimal("12.00"), linea.getPrecioUnitario());
            assertEquals(new BigDecimal("24.00"), linea.getSubtotal());
        }

        @Test
        @DisplayName("las listas que devuelven getLineas() y listarPorCliente() no se pueden modificar desde fuera")
        void listasDevueltasSonInmutables() throws Exception {
            Pedido pedido = pedidoController.addLineaPedido(clienteId, margaritaId, 1, Size.PEQUENYA).getPedido();

            assertThrows(UnsupportedOperationException.class, () -> pedido.getLineas().clear());
            assertThrows(UnsupportedOperationException.class, () -> pedidoController.listarPorCliente(clienteId).clear());
        }

        @Test
        @DisplayName("la fecha que devuelve el pedido es una copia: cambiarla no cambia el pedido")
        void fechaNoSeModificaDesdeFuera() throws Exception {
            Pedido pedido = pedidoController.addLineaPedido(clienteId, margaritaId, 1, Size.PEQUENYA).getPedido();
            long fechaOriginal = pedido.getFecha().getTime();

            pedido.getFecha().setTime(0);

            assertEquals(fechaOriginal, pedido.getFecha().getTime());
        }

        @Test
        @DisplayName("con pedido pendiente añade la línea a ese mismo pedido, sin crear otro")
        void reutilizaPedidoPendiente() throws Exception {
            Pedido primero = pedidoController.addLineaPedido(clienteId, margaritaId, 1, Size.PEQUENYA).getPedido();
            Pedido segundo = pedidoController.addLineaPedido(clienteId, aguaId, 2, Size.PEQUENYA).getPedido();

            assertSame(primero, segundo);
            assertEquals(1, pedidoController.listarTodos().size());
            assertEquals(2, primero.getLineas().size());
        }

        @Test
        @DisplayName("varias líneas se acumulan en el precio total")
        void variasLineasAcumulanPrecioTotal() throws Exception {
            pedidoController.addLineaPedido(clienteId, margaritaId, 1, Size.PEQUENYA);                     // 8.50 €
            Pedido pedido = pedidoController.addLineaPedido(clienteId, aguaId, 2, Size.PEQUENYA).getPedido(); // 1.50 € x2

            assertEquals(new BigDecimal("11.50"), pedido.getPrecioTotal());
        }

        @Test
        @DisplayName("el total guarda el precio de cada momento: si después cambia el precio del producto, lo ya añadido no cambia")
        void totalCongelaElPrecioDelProducto() throws Exception {
            Pedido pedido = pedidoController.addLineaPedido(clienteId, margaritaId, 2, Size.PEQUENYA).getPedido(); // 8.50 € x2
            Ingrediente tomate = new Ingrediente(1, "Tomate", 100);
            productoController.actualizarProducto(new Pizza(margaritaId, "Margarita",
                    Map.of(Size.PEQUENYA, new BigDecimal("20.00")), List.of(tomate)));

            assertEquals(new BigDecimal("17.00"), pedido.getPrecioTotal());

            pedidoController.addLineaPedido(clienteId, margaritaId, 1, Size.PEQUENYA);                     // ya a 20.00 €
            assertEquals(new BigDecimal("37.00"), pedido.getPrecioTotal());
        }

        @Test
        @DisplayName("suma al total el precio del tamaño elegido")
        void sumaElPrecioDelTamanyoElegido() throws Exception {
            pedidoController.addLineaPedido(clienteId, margaritaId, 1, Size.PEQUENYA);                     // 8.50 €
            Pedido pedido = pedidoController.addLineaPedido(clienteId, margaritaId, 2, Size.GRANDE).getPedido(); // 12.00 € x2

            assertEquals(new BigDecimal("32.50"), pedido.getPrecioTotal());
            assertEquals(2, pedido.getLineas().size());
        }

        @Test
        @DisplayName("rechaza un tamaño que el producto no tiene, o ningún tamaño, y no deja un pedido vacío")
        void rechazaTamanyoNoValido() {
            IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                    () -> pedidoController.addLineaPedido(clienteId, aguaId, 1, Size.GRANDE));
            assertEquals("Bebida Agua 50cl no se vende en tamaño GRANDE", error.getMessage());
            assertThrows(IllegalArgumentException.class,
                    () -> pedidoController.addLineaPedido(clienteId, margaritaId, 1, null));
            assertTrue(pedidoController.listarTodos().isEmpty());
        }

        @Test
        @DisplayName("un tamaño no válido no cambia el pedido pendiente que ya había")
        void tamanyoNoValidoNoCambiaElPedido() throws Exception {
            Pedido pedido = pedidoController.addLineaPedido(clienteId, margaritaId, 1, Size.PEQUENYA).getPedido(); // 8.50 €

            assertThrows(IllegalArgumentException.class,
                    () -> pedidoController.addLineaPedido(clienteId, aguaId, 1, Size.GRANDE));

            assertEquals(1, pedido.getLineas().size());
            assertEquals(new BigDecimal("8.50"), pedido.getPrecioTotal());
        }

        @Test
        @DisplayName("una pasta no necesita tamaño: cuesta lo mismo se pida como se pida")
        void pastaSinTamanyo() throws Exception {
            Ingrediente tomate = new Ingrediente(1, "Tomate", 100);
            long arrabbiataId = productoController.agregarProducto(
                    new Pasta(0, "Arrabbiata", new BigDecimal("9.00"), List.of(tomate))).getId();

            LineaPedido linea = pedidoController.addLineaPedido(clienteId, arrabbiataId, 2, null);

            assertTrue(linea.getSize().isEmpty());
            assertEquals(new BigDecimal("18.00"), linea.getPedido().getPrecioTotal());
        }

        @Test
        @DisplayName("si el pedido anterior ya está pagado (FINALIZADO) se crea un pedido nuevo")
        void creaPedidoNuevoTrasPagarElAnterior() throws Exception {
            Pedido primero = pedidoController.addLineaPedido(clienteId, margaritaId, 1, Size.PEQUENYA).getPedido();
            pedidoController.pagarPedido(primero.getId(), new PagableFalso());

            Pedido segundo = pedidoController.addLineaPedido(clienteId, aguaId, 1, Size.PEQUENYA).getPedido();

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

            Pedido deAna = pedidoController.addLineaPedido(clienteId, margaritaId, 1, Size.PEQUENYA).getPedido();
            Pedido deCarlos = pedidoController.addLineaPedido(otro.getId(), aguaId, 1, Size.PEQUENYA).getPedido();

            assertNotSame(deAna, deCarlos);
            assertEquals(1, deAna.getId());
            assertEquals(2, deCarlos.getId());
            assertEquals(2, pedidoController.listarTodos().size());
        }

        @Test
        @DisplayName("lanza ClienteNoEncontradoException si el cliente no existe")
        void rechazaClienteInexistente() {
            assertThrows(ClienteNoEncontradoException.class,
                    () -> pedidoController.addLineaPedido(999, margaritaId, 1, Size.PEQUENYA));
        }

        @Test
        @DisplayName("lanza ProductoNoEncontradoException si el producto no existe y no deja un pedido vacío")
        void rechazaProductoInexistente() {
            assertThrows(ProductoNoEncontradoException.class,
                    () -> pedidoController.addLineaPedido(clienteId, 999, 1, Size.PEQUENYA));
            assertTrue(pedidoController.listarTodos().isEmpty());
        }

        @Test
        @DisplayName("lanza IllegalArgumentException si la cantidad no es positiva")
        void rechazaCantidadNoPositiva() {
            IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                    () -> pedidoController.addLineaPedido(clienteId, margaritaId, 0, Size.PEQUENYA));
            assertEquals("La cantidad debe ser mayor que 0", error.getMessage());
            assertTrue(pedidoController.listarTodos().isEmpty());
        }

        @Test
        @DisplayName("una línea de pedido no se puede crear con cantidad 0 o negativa")
        void lineaConCantidadNoPositivaSeRechaza() {
            Bebida agua = new Bebida(0, "Agua", Map.of(Size.PEQUENYA, new BigDecimal("1.50")));

            assertThrows(IllegalArgumentException.class, () -> new LineaPedido(1, 0, agua, Size.PEQUENYA));
            assertThrows(IllegalArgumentException.class, () -> new LineaPedido(1, -5, agua, Size.PEQUENYA));
        }

        @Test
        @DisplayName("no se puede añadir una línea null a un pedido")
        void rechazaLineaNull() throws Exception {
            Pedido pedido = pedidoController.addLineaPedido(clienteId, margaritaId, 1, Size.PEQUENYA).getPedido();

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
            Pedido pedido = pedidoController.addLineaPedido(clienteId, margaritaId, 2, Size.PEQUENYA).getPedido(); // 17.0€

            PagableFalso pagable = new PagableFalso();
            pedidoController.pagarPedido(pedido.getId(), pagable);

            assertEquals(new BigDecimal("17.00"), pagable.cantidadCobrada);
        }

        @Test
        @DisplayName("tras pagar, el pedido pasa a FINALIZADO")
        void pedidoPagadoQuedaFinalizado() throws Exception {
            Pedido pedido = pedidoController.addLineaPedido(clienteId, margaritaId, 1, Size.PEQUENYA).getPedido();

            pedidoController.pagarPedido(pedido.getId(), new PagableFalso());

            assertEquals(EstadoPedido.FINALIZADO, pedido.getEstado());
        }

        @Test
        @DisplayName("pagar el pedido directamente por segunda vez lanza IllegalStateException y no vuelve a cobrar")
        void pagarDirectamenteDosVecesNoCobraDosVeces() throws Exception {
            Pedido pedido = pedidoController.addLineaPedido(clienteId, margaritaId, 1, Size.PEQUENYA).getPedido();
            PagableFalso pagable = new PagableFalso();
            pedido.pagar(pagable);
            pagable.cantidadCobrada = null;

            assertThrows(IllegalStateException.class, () -> pedido.pagar(pagable));
            assertNull(pagable.cantidadCobrada);
        }

        @Test
        @DisplayName("un pedido ya pagado no admite más líneas")
        void pedidoPagadoNoAdmiteLineas() throws Exception {
            Pedido pedido = pedidoController.addLineaPedido(clienteId, margaritaId, 1, Size.PEQUENYA).getPedido();
            pedidoController.pagarPedido(pedido.getId(), new PagableFalso());
            LineaPedido extra = new LineaPedido(99, 1, new Bebida(0, "Cola", Map.of(Size.PEQUENYA, new BigDecimal("2.00"))), Size.PEQUENYA);

            assertThrows(IllegalStateException.class, () -> pedido.agregarLinea(extra));
            assertEquals(1, pedido.getLineas().size());
        }

        @Test
        @DisplayName("lanza IllegalArgumentException si el método de pago es null")
        void rechazaMetodoPagoNull() throws Exception {
            Pedido pedido = pedidoController.addLineaPedido(clienteId, margaritaId, 1, Size.PEQUENYA).getPedido();

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
                    () -> new PagoConTarjeta().pagar(BigDecimal.ZERO));
            IllegalArgumentException efectivo = assertThrows(IllegalArgumentException.class,
                    () -> new PagoEnEfectivo().pagar(new BigDecimal("-1")));

            assertEquals("La cantidad a pagar debe ser mayor que 0", tarjeta.getMessage());
            assertEquals("La cantidad a pagar debe ser mayor que 0", efectivo.getMessage());
        }
    }

    @Nested
    @DisplayName("cancelarPedido()")
    class CancelarPedido {

        @Test
        @DisplayName("un pedido pendiente pasa a CANCELADO")
        void pedidoPendienteQuedaCancelado() throws Exception {
            Pedido pedido = pedidoController.addLineaPedido(clienteId, margaritaId, 1, Size.PEQUENYA).getPedido();

            pedidoController.cancelarPedido(pedido.getId());

            assertEquals(EstadoPedido.CANCELADO, pedido.getEstado());
        }

        @Test
        @DisplayName("un pedido cancelado no admite más líneas ni se puede pagar, y no se cobra nada")
        void pedidoCanceladoNoAdmiteLineasNiPago() throws Exception {
            Pedido pedido = pedidoController.addLineaPedido(clienteId, margaritaId, 1, Size.PEQUENYA).getPedido();
            pedidoController.cancelarPedido(pedido.getId());
            PagableFalso pagable = new PagableFalso();

            assertThrows(IllegalStateException.class,
                    () -> pedido.agregarLinea(new LineaPedido(99, 1, new Bebida(0, "Cola",
                            Map.of(Size.PEQUENYA, new BigDecimal("2.00"))), Size.PEQUENYA)));
            assertThrows(IllegalStateException.class, () -> pedidoController.pagarPedido(pedido.getId(), pagable));
            assertNull(pagable.cantidadCobrada);
        }

        @Test
        @DisplayName("tras cancelar, la siguiente línea del cliente abre un pedido nuevo")
        void trasCancelarSeCreaPedidoNuevo() throws Exception {
            Pedido cancelado = pedidoController.addLineaPedido(clienteId, margaritaId, 1, Size.PEQUENYA).getPedido();
            pedidoController.cancelarPedido(cancelado.getId());

            Pedido nuevo = pedidoController.addLineaPedido(clienteId, aguaId, 1, Size.PEQUENYA).getPedido();

            assertNotSame(cancelado, nuevo);
            assertEquals(EstadoPedido.PENDIENTE, nuevo.getEstado());
        }

        @Test
        @DisplayName("lanza IllegalStateException si el pedido ya estaba pagado, y sigue FINALIZADO")
        void rechazaCancelarPedidoPagado() throws Exception {
            Pedido pedido = pedidoController.addLineaPedido(clienteId, margaritaId, 1, Size.PEQUENYA).getPedido();
            pedidoController.pagarPedido(pedido.getId(), new PagableFalso());

            assertThrows(IllegalStateException.class, () -> pedidoController.cancelarPedido(pedido.getId()));
            assertEquals(EstadoPedido.FINALIZADO, pedido.getEstado());
        }

        @Test
        @DisplayName("lanza PedidoNoEncontradoException si el pedido no existe")
        void rechazaPedidoInexistente() {
            assertThrows(PedidoNoEncontradoException.class, () -> pedidoController.cancelarPedido(999));
        }
    }

    @Nested
    @DisplayName("buscarPorId()")
    class BuscarPorId {

        @Test
        @DisplayName("encuentra un pedido existente")
        void encuentraPedidoExistente() throws Exception {
            Pedido creado = pedidoController.addLineaPedido(clienteId, margaritaId, 1, Size.PEQUENYA).getPedido();
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

            Pedido primero = pedidoController.addLineaPedido(clienteId, margaritaId, 1, Size.PEQUENYA).getPedido();
            pedidoController.pagarPedido(primero.getId(), new PagableFalso());
            pedidoController.addLineaPedido(clienteId, aguaId, 1, Size.PEQUENYA);   // segundo pedido de Ana
            pedidoController.addLineaPedido(otro.getId(), aguaId, 1, Size.PEQUENYA); // pedido de Carlos

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
            pedidoController.addLineaPedido(clienteId, margaritaId, 2, Size.PEQUENYA);
            return pedidoController.addLineaPedido(clienteId, aguaId, 1, Size.PEQUENYA).getPedido();
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
            assertTrue(pedido.has("precioTotal"));
            for (JsonElement linea : pedido.get("lineas").getAsJsonArray()) {
                assertFalse(linea.getAsJsonObject().has("producto"));
                assertFalse(linea.getAsJsonObject().has("pedido"));
                assertTrue(linea.getAsJsonObject().has("cantidad"));
                assertTrue(linea.getAsJsonObject().has("precioUnitario"));
                assertTrue(linea.getAsJsonObject().has("size"));
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
            assertEquals(new BigDecimal("8.50"), pedido.getLineas().get(0).getPrecioUnitario());
            assertEquals(Optional.of(Size.PEQUENYA), pedido.getLineas().get(0).getSize());
            assertNull(pedido.getLineas().get(0).getProducto());
            assertSame(pedido, pedido.getLineas().get(0).getPedido());
            assertEquals(1, pedidoController.listarTodos().size());
        }

        @Test
        @DisplayName("un pedido importado conserva su precio total")
        void pedidoImportadoConservaSuTotal() throws Exception {
            crearPedidoDeEjemplo();
            pedidoController.exportarPedidos(fichero);

            Pedido pedido = pedidoController.importarPedidos(fichero).get(0);

            assertEquals(new BigDecimal("18.50"), pedido.getPrecioTotal()); // 2 x 8.50 + 1 x 1.50
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
        @ValueSource(strings = {"fecha", "estado", "precioTotal", "cliente", "lineas"})
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
        @DisplayName("lanza IOException si a una línea le falta el precio")
        void importarPedidoConLineaSinPrecio() throws Exception {
            exportarYEstropear(pedido -> pedido.getAsJsonArray("lineas").get(0).getAsJsonObject().remove("precioUnitario"));

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