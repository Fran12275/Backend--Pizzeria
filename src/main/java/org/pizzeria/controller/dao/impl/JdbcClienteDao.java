package org.pizzeria.controller.dao.impl;

import org.pizzeria.controller.dao.ClienteDao;
import org.pizzeria.model.Cliente;
import org.pizzeria.utils.DatabaseConf;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class JdbcClienteDao implements ClienteDao {

    // Singleton: la unica instancia se crea al cargar la clase, asi no hay problemas con varios hilos
    private static final JdbcClienteDao INSTANCIA = new JdbcClienteDao();

    // Los datos siempre van como parametros "?" del PreparedStatement, nunca pegados al SQL: asi se evita la inyeccion SQL
    private static final String INSERT =
            "INSERT INTO cliente (dni, nombre, direccion, telefono, email, password) VALUES (?, ?, ?, ?, ?, ?)";
    private static final String SELECT_ALL = "SELECT id, dni, nombre, direccion, telefono, email, password FROM cliente";
    private static final String SELECT_BY_ID = SELECT_ALL + " WHERE id = ?";
    // MySQL compara los textos sin distinguir mayusculas (utf8mb4_0900_ai_ci): ANA@TEST.COM encuentra a ana@test.com
    private static final String SELECT_BY_EMAIL = SELECT_ALL + " WHERE email = ?";
    private static final String SELECT_BY_DNI = SELECT_ALL + " WHERE dni = ?";

    private JdbcClienteDao() {
    }

    public static JdbcClienteDao getInstance() {
        return INSTANCIA;
    }

    @Override
    public void save(Cliente cliente) throws SQLException {
        DatabaseConf.enTransaccion(conn -> {
            try (PreparedStatement ps = conn.prepareStatement(INSERT, Statement.RETURN_GENERATED_KEYS)) {
                asignarDatos(ps, cliente);
                ps.executeUpdate();
                // Lo ultimo: si el cliente ya tenia id, setId lanza IllegalStateException y el rollback deshace el INSERT
                cliente.setId(leerIdGenerado(ps));
            }
        });
    }

    @Override
    public Optional<Cliente> findById(long id) throws SQLException {
        return buscarUno(SELECT_BY_ID, id);
    }

    @Override
    public List<Cliente> findAll() throws SQLException {
        return buscar(SELECT_ALL);
    }

    @Override
    public Optional<Cliente> findByEmail(String email) throws SQLException {
        return buscarUno(SELECT_BY_EMAIL, email);
    }

    @Override
    public Optional<Cliente> findByDni(String dni) throws SQLException {
        return buscarUno(SELECT_BY_DNI, dni);
    }

    // Direccion y telefono pueden ser null: setString con null guarda NULL en la columna
    private static void asignarDatos(PreparedStatement ps, Cliente cliente) throws SQLException {
        ps.setString(1, cliente.getDni());
        ps.setString(2, cliente.getNombre());
        ps.setString(3, cliente.getDireccion());
        ps.setString(4, cliente.getTelefono());
        ps.setString(5, cliente.getEmail());
        ps.setString(6, cliente.getPassword());
    }

    private static long leerIdGenerado(PreparedStatement ps) throws SQLException {
        try (ResultSet claves = ps.getGeneratedKeys()) {
            if (!claves.next()) {
                throw new SQLException("MySQL no ha devuelto el id del cliente guardado");
            }
            return claves.getLong(1);
        }
    }

    private static Optional<Cliente> buscarUno(String sql, Object parametro) throws SQLException {
        return buscar(sql, parametro).stream().findFirst();
    }

    private static List<Cliente> buscar(String sql, Object... parametros) throws SQLException {
        try (Connection conn = DatabaseConf.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < parametros.length; i++) {
                ps.setObject(i + 1, parametros[i]);
            }
            return leerClientes(ps);
        }
    }

    private static List<Cliente> leerClientes(PreparedStatement ps) throws SQLException {
        try (ResultSet rs = ps.executeQuery()) {
            List<Cliente> clientes = new ArrayList<>();
            while (rs.next()) {
                clientes.add(mapear(rs));
            }
            // Copia que no se puede modificar: quien la recibe no puede cambiar la lista
            return List.copyOf(clientes);
        }
    }

    private static Cliente mapear(ResultSet rs) throws SQLException {
        return new Cliente(
                rs.getLong("id"),
                rs.getString("dni"),
                rs.getString("nombre"),
                rs.getString("direccion"),
                rs.getString("telefono"),
                rs.getString("email"),
                rs.getString("password"));
    }
}
