package org.pizzeria.model.product;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Map;

public class Bebida extends Producto {
    // Precio de cada tamano en el que se vende
    private final Map<Size, BigDecimal> configuraciones;

    public Bebida(long id, String nombre, Map<Size, BigDecimal> configuraciones) {
        super(id, nombre);
        this.configuraciones = copiarConfiguraciones(configuraciones);
    }

    @JsonCreator
    public Bebida(@JsonProperty("nombre") String nombre,
                  @JsonProperty("configuraciones") Map<Size, BigDecimal> configuraciones) {
        this(0, nombre, configuraciones);
    }

    public Map<Size, BigDecimal> getConfiguraciones() {
        return Collections.unmodifiableMap(configuraciones);
    }

    @Override
    public BigDecimal getPrecio(Size size) {
        return precioSegunTamanyo(configuraciones, size);
    }

    @Override
    public String getTipo() {
        return "Bebida";
    }
}
