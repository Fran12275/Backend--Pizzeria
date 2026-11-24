package org.pizzeria.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class DatabaseConf {

    // El enunciado fija el esquema "pizzeria-jdbc" de MySQL en el puerto 3306
    private static final String URL = "jdbc:mysql://localhost:3306/pizzeria-jdbc";
    private static final String USER = "root";
    private static final String PASS = "root";

    private DatabaseConf() {
    }

    // Cada operacion pide su propia conexion y la cierra al terminar (try-with-resources)
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASS);
    }

    // Lo que se ejecuta dentro de una transaccion. Consumer<Connection> no sirve: no puede lanzar SQLException
    @FunctionalInterface
    public interface OperacionJdbc {
        void ejecutar(Connection conn) throws SQLException;
    }

    // Todo o nada: commit si todas las sentencias van bien y rollback si falla cualquiera,
    // asi nunca queda guardada solo una parte (por ejemplo, un pedido sin sus lineas)
    public static void enTransaccion(OperacionJdbc operacion) throws SQLException {
        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try {
                operacion.ejecutar(conn);
                conn.commit();
            } catch (SQLException | RuntimeException e) {
                conn.rollback();
                throw e;
            }
        }
    }
}
