package org.pizzeria.model.product;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "tipo" // Este campo aparecerá en el JSON
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = Pizza.class, name = "pizza"),
        @JsonSubTypes.Type(value = Bebida.class, name = "bebida"),
        @JsonSubTypes.Type(value = Pasta.class, name = "pasta"),
})
public abstract class Producto {

    @JsonIgnore
    private long id;
    private String nombre;

    protected Producto(long id, String nombre){
        this.id = id;
        this.nombre = nombre;
    }

    // Precio de una unidad en ese tamano: lo usa el pedido al anadir una linea.
    // Pizza y Bebida lo buscan en sus configuraciones; la pasta no tiene tamano y siempre cuesta lo mismo
    public abstract BigDecimal getPrecio(Size size);

    // Misma regla para todos los precios: obligatorio y mayor que 0
    protected static BigDecimal validarPrecio(BigDecimal precio) {
        if (precio == null || precio.signum() <= 0) {
            throw new IllegalArgumentException("El precio debe ser mayor que 0");
        }
        return precio;
    }

    // Pizza y Bebida: al menos un tamano, ningun tamano null y cada precio mayor que 0.
    // Se copia en un EnumMap: quien paso el mapa ya no puede cambiarlo y los tamanos quedan en orden
    protected static Map<Size, BigDecimal> copiarConfiguraciones(Map<Size, BigDecimal> configuraciones) {
        if (configuraciones == null || configuraciones.isEmpty()) {
            throw new IllegalArgumentException("Debe tener al menos un tamaño con su precio");
        }
        Map<Size, BigDecimal> copia = new EnumMap<>(Size.class);
        for (Map.Entry<Size, BigDecimal> configuracion : configuraciones.entrySet()) {
            if (configuracion.getKey() == null) {
                throw new IllegalArgumentException("Ningún tamaño puede ser null");
            }
            copia.put(configuracion.getKey(), validarPrecio(configuracion.getValue()));
        }
        return copia;
    }

    // Pizza y Bebida: hay que indicar un tamano y que se venda en el
    protected BigDecimal precioSegunTamanyo(Map<Size, BigDecimal> configuraciones, Size size) {
        if (size == null) {
            throw new IllegalArgumentException(getTipo() + " " + nombre + " necesita un tamaño");
        }
        BigDecimal precio = configuraciones.get(size);
        if (precio == null) {
            throw new IllegalArgumentException(getTipo() + " " + nombre + " no se vende en tamaño " + size);
        }
        return precio;
    }


    public long getId() {
        return id;
    }

    // El id lo asigna el catalogo una sola vez: cambiarlo despues romperia equals/hashCode
    public void setId(long id) {
        if (this.id != 0) {
            throw new IllegalStateException("El producto ya tiene el id " + this.id);
        }
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    @JsonIgnore
    public abstract String getTipo();

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Producto otro = (Producto) o;
        // id 0 = todavia sin id asignado: solo es igual a si mismo
        return this.id != 0 && this.id == otro.id;
    }

    @Override
    public int hashCode() {
        // Coherente con equals: sin id, cada objeto tiene su propio hash
        return this.id != 0 ? Objects.hash(this.id) : System.identityHashCode(this);
    }

    @Override
    public String toString() {
        return getTipo() + "{" +
                "id=" + id +
                ", nombre='" + nombre + '\'' +
                '}';
    }
}