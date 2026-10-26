package org.pizzeria.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import org.pizzeria.model.Pedido;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

public final class PedidoGsonUtils {
    public static final String FICHERO_PEDIDOS = "pedidos.json";

    private static final Type TIPO_LISTA_PEDIDOS = new TypeToken<List<Pedido>>() {}.getType();

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .setDateFormat("yyyy-MM-dd HH:mm:ss")
            .create();

    private PedidoGsonUtils() {
    }

    public static void exportarPedidos(List<Pedido> pedidos) throws IOException {
        exportarPedidos(pedidos, Path.of(FICHERO_PEDIDOS));
    }

    public static void exportarPedidos(List<Pedido> pedidos, Path ruta) throws IOException {
        Objects.requireNonNull(pedidos, "La lista de pedidos no puede ser null");
        Objects.requireNonNull(ruta, "La ruta no puede ser null");
        try (Writer writer = Files.newBufferedWriter(ruta, StandardCharsets.UTF_8)) {
            GSON.toJson(pedidos, TIPO_LISTA_PEDIDOS, writer);
        }
    }

    public static List<Pedido> importarPedidos() throws IOException {
        return importarPedidos(Path.of(FICHERO_PEDIDOS));
    }

    public static List<Pedido> importarPedidos(Path ruta) throws IOException {
        Objects.requireNonNull(ruta, "La ruta no puede ser null");
        List<Pedido> pedidos;
        try (Reader reader = Files.newBufferedReader(ruta, StandardCharsets.UTF_8)) {
            pedidos = GSON.fromJson(reader, TIPO_LISTA_PEDIDOS);
        } catch (JsonParseException e) {
            throw new IOException("El fichero " + ruta + " no tiene un formato JSON valido", e);
        }
        if (pedidos == null) {
            return List.of();
        }
        comprobarPedidos(pedidos, ruta);
        enlazarLineasConSuPedido(pedidos);
        return pedidos;
    }

    // Si un solo pedido no es valido se rechaza el fichero entero, diciendo cual falla y por que
    private static void comprobarPedidos(List<Pedido> pedidos, Path ruta) throws IOException {
        for (int i = 0; i < pedidos.size(); i++) {
            comprobarPedido(pedidos.get(i), i + 1, ruta);
        }
    }

    private static void comprobarPedido(Pedido pedido, int posicion, Path ruta) throws IOException {
        if (pedido == null) {
            throw new IOException("El fichero " + ruta + " contiene un pedido null en la posicion " + posicion);
        }
        try {
            pedido.comprobarQueEstaCompleto();
        } catch (IllegalStateException e) {
            throw new IOException("El fichero " + ruta + " tiene un pedido no valido en la posicion "
                    + posicion + ": " + e.getMessage(), e);
        }
    }

    // El pedido de cada linea no esta en el JSON: se vuelve a enlazar al importar
    private static void enlazarLineasConSuPedido(List<Pedido> pedidos) {
        for (Pedido pedido : pedidos) {
            pedido.reenlazarLineas();
        }
    }
}
