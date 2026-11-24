package org.pizzeria.utils;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

public final class EsquemaBaseDatos {

    // Direccion y telefono son opcionales (NULL); el resto, obligatorios. DNI y email no se pueden repetir
    private static final String CREAR_CLIENTE = """
            CREATE TABLE IF NOT EXISTS cliente (
                id BIGINT PRIMARY KEY AUTO_INCREMENT,
                dni VARCHAR(255) NOT NULL UNIQUE,
                nombre VARCHAR(255) NOT NULL,
                direccion VARCHAR(255) NULL,
                telefono VARCHAR(255) NULL,
                email VARCHAR(255) NOT NULL UNIQUE,
                password VARCHAR(255) NOT NULL
            )""";

    // Se crean en este orden: cada tabla despues de las tablas a las que apuntan sus claves foraneas
    private static final List<String> CREAR_TABLAS = List.of(CREAR_CLIENTE);

    // Se borran en el orden contrario: primero las tablas que apuntan a otras
    private static final List<String> BORRAR_TABLAS = List.of("DROP TABLE IF EXISTS cliente");

    private EsquemaBaseDatos() {
    }

    // Crea las tablas que falten. Las que ya existen no se tocan (IF NOT EXISTS), asi no se pierden datos
    public static void crearTablas() throws SQLException {
        ejecutar(CREAR_TABLAS);
    }

    // Deja la base de datos vacia: lo usan los tests para que cada uno empiece desde cero
    public static void borrarYCrearTablas() throws SQLException {
        ejecutar(BORRAR_TABLAS);
        crearTablas();
    }

    // Sentencias fijas, sin datos del usuario: no hace falta PreparedStatement
    private static void ejecutar(List<String> sentencias) throws SQLException {
        try (Connection conn = DatabaseConf.getConnection();
             Statement statement = conn.createStatement()) {
            for (String sql : sentencias) {
                statement.executeUpdate(sql);
            }
        }
    }
}
