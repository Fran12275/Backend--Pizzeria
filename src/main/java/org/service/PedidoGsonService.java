package org.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.model.LineaPedido;
import org.model.Pedido;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

public class PedidoGsonService {
    public static final String FICHERO_PEDIDOS = "pedidos.json";

    private static final Type TIPO_LISTA_PEDIDOS = new TypeToken<List<Pedido>>() {}.getType();

    private final Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .setDateFormat("yyyy-MM-dd HH:mm:ss")
            .create();

    public void exportarPedidos(List<Pedido> pedidos) throws IOException {
        exportarPedidos(pedidos, Path.of(FICHERO_PEDIDOS));
    }

    public void exportarPedidos(List<Pedido> pedidos, Path ruta) throws IOException {
        Objects.requireNonNull(pedidos, "La lista de pedidos no puede ser null");
        Objects.requireNonNull(ruta, "La ruta no puede ser null");
        try (Writer writer = Files.newBufferedWriter(ruta, StandardCharsets.UTF_8)) {
            gson.toJson(pedidos, TIPO_LISTA_PEDIDOS, writer);
        }
    }

    public List<Pedido> importarPedidos() throws IOException {
        return importarPedidos(Path.of(FICHERO_PEDIDOS));
    }

    public List<Pedido> importarPedidos(Path ruta) throws IOException {
        Objects.requireNonNull(ruta, "La ruta no puede ser null");
        List<Pedido> pedidos;
        try (Reader reader = Files.newBufferedReader(ruta, StandardCharsets.UTF_8)) {
            pedidos = gson.fromJson(reader, TIPO_LISTA_PEDIDOS);
        }
        if (pedidos == null) {
            return List.of();
        }
        // El pedido de cada linea no esta en el JSON: se vuelve a enlazar al importar
        for (Pedido pedido : pedidos) {
            for (LineaPedido linea : pedido.getLineas()) {
                linea.setPedido(pedido);
            }
        }
        return pedidos;
    }
}
