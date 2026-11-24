package org.pizzeria.utils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BaseDatosTest {

    // Sin direccion ni telefono: al ser opcionales se quedan a NULL
    private static final String INSERTAR_CLIENTE =
            "INSERT INTO cliente (dni, nombre, email, password) VALUES (?, ?, ?, ?)";
    private static final String CONTAR_CLIENTES = "SELECT COUNT(*) FROM cliente";

    @BeforeEach
    void setUp() throws SQLException {
        // Cada test empieza con las tablas vacias
        EsquemaBaseDatos.borrarYCrearTablas();
    }

    private static void insertarCliente(Connection conn, String dni, String nombre, String email, String password)
            throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(INSERTAR_CLIENTE)) {
            ps.setString(1, dni);
            ps.setString(2, nombre);
            ps.setString(3, email);
            ps.setString(4, password);
            ps.executeUpdate();
        }
    }

    private static void insertarCliente(String dni, String nombre, String email, String password) throws SQLException {
        try (Connection conn = DatabaseConf.getConnection()) {
            insertarCliente(conn, dni, nombre, email, password);
        }
    }

    private static int contarClientes() throws SQLException {
        try (Connection conn = DatabaseConf.getConnection();
             PreparedStatement ps = conn.prepareStatement(CONTAR_CLIENTES);
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getInt(1);
        }
    }

    @Nested
    @DisplayName("DatabaseConf")
    class Conexion {

        @Test
        @DisplayName("getConnection() se conecta al esquema pizzeria-jdbc")
        void seConectaAlEsquema() throws SQLException {
            try (Connection conn = DatabaseConf.getConnection()) {
                assertEquals("pizzeria-jdbc", conn.getCatalog());
            }
        }

        @Test
        @DisplayName("enTransaccion() guarda todas las sentencias si ninguna falla (commit)")
        void guardaTodoSiNadaFalla() throws SQLException {
            DatabaseConf.enTransaccion(conn -> {
                insertarCliente(conn, "11111111A", "Ana García", "ana@test.com", "password123");
                insertarCliente(conn, "22222222B", "Carlos Ruiz", "carlos@test.com", "password456");
            });

            assertEquals(2, contarClientes());
        }

        @Test
        @DisplayName("enTransaccion() deshace también lo anterior si falla una sentencia SQL (rollback) y relanza la excepción")
        void deshaceTodoSiFallaUnaSentencia() throws SQLException {
            assertThrows(SQLIntegrityConstraintViolationException.class, () -> DatabaseConf.enTransaccion(conn -> {
                insertarCliente(conn, "11111111A", "Ana García", "ana@test.com", "password123");
                // Email repetido: lo rechaza el UNIQUE de la tabla
                insertarCliente(conn, "22222222B", "Carlos Ruiz", "ana@test.com", "password456");
            }));

            assertEquals(0, contarClientes());
        }

        @Test
        @DisplayName("enTransaccion() también deshace todo si falla el código Java a mitad (RuntimeException)")
        void deshaceTodoSiFallaElCodigoJava() throws SQLException {
            assertThrows(IllegalStateException.class, () -> DatabaseConf.enTransaccion(conn -> {
                insertarCliente(conn, "11111111A", "Ana García", "ana@test.com", "password123");
                throw new IllegalStateException("Fallo a mitad de la transacción");
            }));

            assertEquals(0, contarClientes());
        }
    }

    @Nested
    @DisplayName("EsquemaBaseDatos")
    class Esquema {

        @Test
        @DisplayName("borrarYCrearTablas() deja la tabla cliente creada y vacía")
        void borrarYCrearTablasDejaLaTablaVacia() throws SQLException {
            insertarCliente("11111111A", "Ana García", "ana@test.com", "password123");

            EsquemaBaseDatos.borrarYCrearTablas();

            assertEquals(0, contarClientes());
        }

        @Test
        @DisplayName("crearTablas() no borra los datos que ya hay")
        void crearTablasConservaLosDatos() throws SQLException {
            insertarCliente("11111111A", "Ana García", "ana@test.com", "password123");

            EsquemaBaseDatos.crearTablas();

            assertEquals(1, contarClientes());
        }

        @Test
        @DisplayName("la dirección y el teléfono son opcionales: un cliente sin ellos se guarda")
        void direccionYTelefonoSonOpcionales() throws SQLException {
            insertarCliente("11111111A", "Ana García", "ana@test.com", "password123");

            assertEquals(1, contarClientes());
        }

        @Test
        @DisplayName("el DNI, el nombre, el email y la contraseña son obligatorios")
        void restoDeCamposObligatorios() throws SQLException {
            assertThrows(SQLIntegrityConstraintViolationException.class,
                    () -> insertarCliente(null, "Ana García", "ana@test.com", "password123"));
            assertThrows(SQLIntegrityConstraintViolationException.class,
                    () -> insertarCliente("11111111A", null, "ana@test.com", "password123"));
            assertThrows(SQLIntegrityConstraintViolationException.class,
                    () -> insertarCliente("11111111A", "Ana García", null, "password123"));
            assertThrows(SQLIntegrityConstraintViolationException.class,
                    () -> insertarCliente("11111111A", "Ana García", "ana@test.com", null));
            assertEquals(0, contarClientes());
        }

        @Test
        @DisplayName("no admite dos clientes con el mismo DNI ni con el mismo email")
        void dniYEmailNoSeRepiten() throws SQLException {
            insertarCliente("11111111A", "Ana García", "ana@test.com", "password123");

            assertThrows(SQLIntegrityConstraintViolationException.class,
                    () -> insertarCliente("11111111A", "Carlos Ruiz", "carlos@test.com", "password456"));
            assertThrows(SQLIntegrityConstraintViolationException.class,
                    () -> insertarCliente("22222222B", "Carlos Ruiz", "ana@test.com", "password456"));
            assertEquals(1, contarClientes());
        }
    }
}
