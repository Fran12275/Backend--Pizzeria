package model;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.model.Bebida;
import org.model.Cliente;
import org.model.EstadoPedido;
import org.model.LineaPedido;
import org.model.PagarEfectivo;
import org.model.Pedido;
import org.model.Size;
import org.utils.PedidoGsonUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PedidoGsonUtilsTest {

    private Path fichero;

    @BeforeEach
    void setUp() throws IOException {
        fichero = Files.createTempFile("pedidos", ".json");
    }

    @AfterEach
    void borrarFicheroTemporal() throws IOException {
        Files.deleteIfExists(fichero);
    }

    private List<Pedido> pedidosDeEjemplo() {
        Cliente cliente = new Cliente(1, "12345678A", "Ana", "Calle Mayor 1", "600000000", "ana@mail.com", "1234");
        Pedido pedido = new Pedido(10, new Date(), cliente);
        pedido.agregarLinea(new LineaPedido(1, 2, new Bebida(3, "Agua", 1.5, Size.PEQUENYA)));
        pedido.agregarLinea(new LineaPedido(2, 1, new Bebida(4, "Cola", 2.0, Size.GRANDE)));
        pedido.setPagable(new PagarEfectivo());
        return List.of(pedido);
    }

    @Test
    @DisplayName("el JSON no contiene pagable, ni producto ni pedido dentro de las lineas")
    void jsonSinCamposExcluidos() throws IOException {
        PedidoGsonUtils.exportarPedidos(pedidosDeEjemplo(), fichero);

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
    @DisplayName("exportar e importar conserva el pedido, su cliente y sus lineas")
    void exportarEImportarConservaLosDatos() throws IOException {
        PedidoGsonUtils.exportarPedidos(pedidosDeEjemplo(), fichero);

        List<Pedido> importados = PedidoGsonUtils.importarPedidos(fichero);

        assertEquals(1, importados.size());
        Pedido pedido = importados.get(0);
        assertEquals(10, pedido.getId());
        assertEquals(EstadoPedido.PENDIENTE, pedido.getEstado());
        assertNotNull(pedido.getFecha());
        assertEquals("Ana", pedido.getCliente().getNombre());
        assertNull(pedido.getPagable());
        assertEquals(2, pedido.getLineas().size());
        assertEquals(2, pedido.getLineas().get(0).getCantidad());
        assertNull(pedido.getLineas().get(0).getProducto());
        assertSame(pedido, pedido.getLineas().get(0).getPedido());
    }

    @Test
    @DisplayName("lanza IOException si el fichero a importar no existe")
    void importarFicheroInexistente() throws IOException {
        Files.deleteIfExists(fichero);

        assertThrows(IOException.class, () -> PedidoGsonUtils.importarPedidos(fichero));
    }

    @Test
    @DisplayName("lanza IOException si el fichero no tiene un formato JSON valido")
    void importarJsonInvalido() throws IOException {
        Files.writeString(fichero, "esto no es JSON [");

        assertThrows(IOException.class, () -> PedidoGsonUtils.importarPedidos(fichero));
    }

    @Test
    @DisplayName("lanza NullPointerException si la lista a exportar es null")
    void exportarListaNull() {
        assertThrows(NullPointerException.class, () -> PedidoGsonUtils.exportarPedidos(null, fichero));
    }
}
