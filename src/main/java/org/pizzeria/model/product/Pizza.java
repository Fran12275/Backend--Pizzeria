package org.pizzeria.model.product;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;


public class Pizza extends Producto {
    // Precio de cada tamano en el que se vende
    private final Map<Size, BigDecimal> configuraciones;
    private final List<Ingrediente> ingredientes = new ArrayList<>();
    // Por defecto false (no vegana): no va en el constructor y se cambia con setVegano
    private boolean vegano;

    public Pizza(long id, String nombre, Map<Size, BigDecimal> configuraciones, List<Ingrediente> ingredientes) {
        super(id, nombre);
        this.configuraciones = copiarConfiguraciones(configuraciones);
        if (ingredientes == null || ingredientes.isEmpty()) {
            throw new IllegalArgumentException("La pizza debe tener al menos un ingrediente");
        }
        if (ingredientes.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("Ningún ingrediente de la pizza puede ser null");
        }
        this.ingredientes.addAll(ingredientes);
    }
    @JsonCreator
    public Pizza(@JsonProperty("nombre") String nombre,
                 @JsonProperty("configuraciones") Map<Size, BigDecimal> configuraciones,
                 @JsonProperty("ingredientes") List<Ingrediente> ingredientes) {
        this(0, nombre, configuraciones, ingredientes);
    }

    public Map<Size, BigDecimal> getConfiguraciones() {
        return Collections.unmodifiableMap(configuraciones);
    }

    @Override
    public BigDecimal getPrecio(Size size) {
        return precioSegunTamanyo(configuraciones, size);
    }

    public List<Ingrediente> getIngredientes() {
        return List.copyOf(ingredientes);
    }

    public void addIngrediente(Ingrediente ingrediente) {
        ingredientes.add(Objects.requireNonNull(ingrediente, "El ingrediente no puede ser null"));
    }

    public boolean isVegano() {
        return vegano;
    }

    public void setVegano(boolean vegano) {
        this.vegano = vegano;
    }

    @Override
    public String getTipo() {
        return "Pizza";
    }


}
