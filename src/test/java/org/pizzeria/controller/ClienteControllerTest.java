package org.pizzeria.controller;

import org.pizzeria.exception.ClienteNoEncontradoException;
import org.pizzeria.exception.ClienteYaExisteException;
import org.pizzeria.exception.CredencialesInvalidasException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.pizzeria.model.Cliente;
import org.pizzeria.utils.EsquemaBaseDatos;

import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;


class ClienteControllerTest {

    private ClienteController clienteController;

    @BeforeEach
    void setUp() throws SQLException {
        // Los clientes se guardan en MySQL: cada test empieza con las tablas vacias (y los ids otra vez desde 1)
        EsquemaBaseDatos.borrarYCrearTablas();
        clienteController = new ClienteController();
    }

    private int siguienteNumeroDni = 10000000;

    // Cada cliente de prueba lleva un DNI distinto, para que solo choquen cuando el test lo busca
    private Cliente clienteValido(String email) {
        return clienteValido(email, (siguienteNumeroDni++) + "A");
    }

    private Cliente clienteValido(String email, String dni) {
        return new Cliente(0, dni, "Ana García", "Calle Falsa 123",
                "600111222", email, "password123");
    }

    @Nested
    @DisplayName("registrar()")
    class Registrar {

        @Test
        @DisplayName("registra un cliente válido y le asigna un id autoincremental")
        void registraClienteValido() throws ClienteYaExisteException, SQLException {
            Cliente registrado = clienteController.registrar(clienteValido("ana@test.com"));

            assertEquals(1, registrado.getId());
            assertEquals("ana@test.com", registrado.getEmail());
            assertEquals(1, clienteController.listarTodos().size());
        }

        @Test
        @DisplayName("asigna ids incrementales a clientes sucesivos")
        void asignaIdsIncrementales() throws ClienteYaExisteException, SQLException {
            Cliente c1 = clienteController.registrar(clienteValido("uno@test.com"));
            Cliente c2 = clienteController.registrar(clienteValido("dos@test.com"));

            assertEquals(1, c1.getId());
            assertEquals(2, c2.getId());
        }

        @Test
        @DisplayName("lanza ClienteYaExisteException si el email ya está registrado")
        void rechazaEmailDuplicado() throws ClienteYaExisteException, SQLException {
            clienteController.registrar(clienteValido("dup@test.com"));

            assertThrows(ClienteYaExisteException.class,
                    () -> clienteController.registrar(clienteValido("dup@test.com")));
        }

        @Test
        @DisplayName("la comprobación de email duplicado ignora mayúsculas/minúsculas")
        void emailDuplicadoEsCaseInsensitive() throws ClienteYaExisteException, SQLException {
            clienteController.registrar(clienteValido("Dup@Test.com"));

            assertThrows(ClienteYaExisteException.class,
                    () -> clienteController.registrar(clienteValido("dup@test.com")));
        }

        @Test
        @DisplayName("lanza ClienteYaExisteException si el DNI ya está registrado")
        void rechazaDniDuplicado() throws ClienteYaExisteException, SQLException {
            clienteController.registrar(clienteValido("ana@test.com", "12345678A"));

            assertThrows(ClienteYaExisteException.class,
                    () -> clienteController.registrar(clienteValido("otra@test.com", "12345678A")));
            assertEquals(1, clienteController.listarTodos().size());
        }

        @Test
        @DisplayName("la comprobación de DNI duplicado ignora mayúsculas/minúsculas en la letra")
        void dniDuplicadoEsCaseInsensitive() throws ClienteYaExisteException, SQLException {
            clienteController.registrar(clienteValido("ana@test.com", "12345678A"));

            assertThrows(ClienteYaExisteException.class,
                    () -> clienteController.registrar(clienteValido("otra@test.com", "12345678a")));
        }

        @Test
        @DisplayName("lanza IllegalArgumentException si el email está vacío")
        void rechazaEmailVacio() {
            Cliente invalido = new Cliente(0, "12345678A", "Ana", "Dir", "600111222", "", "pass123");
            IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                    () -> clienteController.registrar(invalido));
            assertEquals("El email es un dato obligatorio", error.getMessage());
        }

        @Test
        @DisplayName("lanza IllegalArgumentException si el email es null")
        void rechazaEmailNull() {
            Cliente invalido = new Cliente(0, "12345678A", "Ana", "Dir", "600111222", null, "pass123");
            IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                    () -> clienteController.registrar(invalido));
            assertEquals("El email es un dato obligatorio", error.getMessage());
        }

        @Test
        @DisplayName("lanza IllegalArgumentException si la password está vacía")
        void rechazaPasswordVacia() {
            Cliente invalido = new Cliente(0, "12345678A", "Ana", "Dir", "600111222", "ana@test.com", "  ");
            IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                    () -> clienteController.registrar(invalido));
            assertEquals("La contraseña es un dato obligatorio", error.getMessage());
        }

        @Test
        @DisplayName("lanza IllegalArgumentException si el nombre está vacío")
        void rechazaNombreVacio() {
            Cliente invalido = new Cliente(0, "12345678A", "", "Dir", "600111222", "ana@test.com", "pass123");
            IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                    () -> clienteController.registrar(invalido));
            assertEquals("El nombre es un dato obligatorio", error.getMessage());
        }

        @Test
        @DisplayName("lanza IllegalArgumentException si el DNI está vacío")
        void rechazaDniVacio() {
            Cliente invalido = new Cliente(0, "", "Ana", "Dir", "600111222", "ana@test.com", "pass123");
            IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                    () -> clienteController.registrar(invalido));
            assertEquals("El DNI es un dato obligatorio", error.getMessage());
        }

        @Test
        @DisplayName("lanza IllegalArgumentException si el cliente es null")
        void rechazaClienteNull() {
            IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                    () -> clienteController.registrar(null));
            assertEquals("El cliente no puede ser null", error.getMessage());
        }

        @Test
        @DisplayName("guarda en la base de datos todos los datos del cliente")
        void guardaTodosLosDatos() throws ClienteYaExisteException, ClienteNoEncontradoException, SQLException {
            Cliente registrado = clienteController.registrar(clienteValido("ana@test.com", "12345678A"));

            // buscarPorId crea un objeto nuevo con lo que hay guardado en MySQL
            Cliente leido = clienteController.buscarPorId(registrado.getId());

            assertEquals("12345678A", leido.getDni());
            assertEquals("Ana García", leido.getNombre());
            assertEquals("Calle Falsa 123", leido.getDireccion());
            assertEquals("600111222", leido.getTelefono());
            assertEquals("ana@test.com", leido.getEmail());
            assertEquals("password123", leido.getPassword());
        }

        @Test
        @DisplayName("registra un cliente sin dirección ni teléfono, porque son opcionales")
        void registraClienteSinDatosOpcionales() throws ClienteYaExisteException, ClienteNoEncontradoException, SQLException {
            Cliente sinOpcionales = new Cliente(0, "12345678A", "Ana García", null, null, "ana@test.com", "password123");

            Cliente leido = clienteController.buscarPorId(clienteController.registrar(sinOpcionales).getId());

            assertNull(leido.getDireccion());
            assertNull(leido.getTelefono());
        }

        @Test
        @DisplayName("lanza IllegalStateException si el cliente ya tiene id, y no lo deja guardado")
        void rechazaClienteQueYaTieneId() throws SQLException {
            Cliente conId = new Cliente(5, "12345678A", "Ana García", "Calle Falsa 123",
                    "600111222", "ana@test.com", "password123");

            IllegalStateException error = assertThrows(IllegalStateException.class,
                    () -> clienteController.registrar(conId));

            assertEquals("El cliente ya tiene el id 5", error.getMessage());
            assertEquals(5, conId.getId());
            assertTrue(clienteController.listarTodos().isEmpty());
        }
    }

    @Nested
    @DisplayName("login()")
    class Login {

        @BeforeEach
        void registrarClientePrevio() throws ClienteYaExisteException, SQLException {
            clienteController.registrar(clienteValido("ana@test.com"));
        }

        @Test
        @DisplayName("login correcto devuelve el cliente registrado")
        void loginCorrecto() throws CredencialesInvalidasException, SQLException {
            Cliente logueado = clienteController.login("ana@test.com", "password123");

            assertEquals("ana@test.com", logueado.getEmail());
            assertEquals("Ana García", logueado.getNombre());
        }

        @Test
        @DisplayName("login es case-insensitive para el email")
        void loginEmailCaseInsensitive() throws CredencialesInvalidasException, SQLException {
            Cliente logueado = clienteController.login("ANA@TEST.COM", "password123");
            assertEquals("ana@test.com", logueado.getEmail());
        }

        @Test
        @DisplayName("lanza CredencialesInvalidasException si el email no existe")
        void rechazaEmailInexistente() {
            CredencialesInvalidasException error = assertThrows(CredencialesInvalidasException.class,
                    () -> clienteController.login("noexiste@test.com", "password123"));
            assertEquals("El email o la contraseña no son correctos", error.getMessage());
        }

        @Test
        @DisplayName("lanza CredencialesInvalidasException si la password es incorrecta")
        void rechazaPasswordIncorrecta() {
            CredencialesInvalidasException error = assertThrows(CredencialesInvalidasException.class,
                    () -> clienteController.login("ana@test.com", "password_mala"));
            assertEquals("El email o la contraseña no son correctos", error.getMessage());
        }
    }

    @Nested
    @DisplayName("buscarPorId() y buscarPorEmail()")
    class Busquedas {

        @Test
        @DisplayName("buscarPorId encuentra un cliente existente")
        void buscarPorIdExistente() throws ClienteYaExisteException, ClienteNoEncontradoException, SQLException {
            Cliente registrado = clienteController.registrar(clienteValido("ana@test.com"));

            Cliente encontrado = clienteController.buscarPorId(registrado.getId());

            assertEquals(registrado, encontrado);
        }

        @Test
        @DisplayName("buscarPorId lanza ClienteNoEncontradoException si no existe")
        void buscarPorIdInexistente() {
            ClienteNoEncontradoException error = assertThrows(ClienteNoEncontradoException.class,
                    () -> clienteController.buscarPorId(999));
            assertEquals("No existe ningún cliente con id 999", error.getMessage());
        }

        @Test
        @DisplayName("buscarPorEmail devuelve Optional con el cliente si existe")
        void buscarPorEmailExistente() throws ClienteYaExisteException, SQLException {
            clienteController.registrar(clienteValido("ana@test.com"));

            Optional<Cliente> resultado = clienteController.buscarPorEmail("ana@test.com");

            assertTrue(resultado.isPresent());
            assertEquals("ana@test.com", resultado.get().getEmail());
        }

        @Test
        @DisplayName("buscarPorEmail devuelve Optional vacío si no existe")
        void buscarPorEmailInexistente() throws SQLException {
            Optional<Cliente> resultado = clienteController.buscarPorEmail("noexiste@test.com");
            assertFalse(resultado.isPresent());
        }

        @Test
        @DisplayName("buscarPorEmail no se deja engañar por una inyección SQL: busca el texto tal cual")
        void buscarPorEmailEvitaInyeccionSql() throws ClienteYaExisteException, SQLException {
            clienteController.registrar(clienteValido("ana@test.com"));

            // Si el email se pegara al SQL, quedaria WHERE email = '' OR '1'='1' y saldria Ana
            assertTrue(clienteController.buscarPorEmail("' OR '1'='1").isEmpty());
        }
    }

    @Nested
    @DisplayName("listarTodos()")
    class ListarTodos {

        @Test
        @DisplayName("devuelve todos los clientes registrados")
        void listaTodosLosClientes() throws ClienteYaExisteException, SQLException {
            clienteController.registrar(clienteValido("uno@test.com"));
            clienteController.registrar(clienteValido("dos@test.com"));

            List<Cliente> todos = clienteController.listarTodos();

            assertEquals(2, todos.size());
        }

        @Test
        @DisplayName("devuelve una lista inmutable (no se puede modificar desde fuera)")
        void listaEsInmutable() throws ClienteYaExisteException, SQLException {
            clienteController.registrar(clienteValido("uno@test.com"));
            List<Cliente> todos = clienteController.listarTodos();

            assertThrows(UnsupportedOperationException.class,
                    () -> todos.add(clienteValido("intruso@test.com")));
        }
    }

    @Nested
    @DisplayName("actualizar()")
    class Actualizar {

        @Test
        @DisplayName("guarda los datos nuevos del cliente y no toca a los demás")
        void actualizaSoloEseCliente() throws ClienteYaExisteException, ClienteNoEncontradoException, SQLException {
            Cliente ana = clienteController.registrar(clienteValido("ana@test.com"));
            Cliente carlos = clienteController.registrar(clienteValido("carlos@test.com"));
            Cliente anaMudada = new Cliente(ana.getId(), ana.getDni(), ana.getNombre(), "Calle Mayor 1",
                    "699000111", ana.getEmail(), ana.getPassword());

            clienteController.actualizar(anaMudada);

            Cliente anaLeida = clienteController.buscarPorId(ana.getId());
            assertEquals("Calle Mayor 1", anaLeida.getDireccion());
            assertEquals("699000111", anaLeida.getTelefono());
            assertEquals("Calle Falsa 123", clienteController.buscarPorId(carlos.getId()).getDireccion());
        }

        @Test
        @DisplayName("lanza ClienteNoEncontradoException si el cliente no existe")
        void rechazaClienteInexistente() {
            Cliente inexistente = new Cliente(999, "12345678A", "Ana García", "Calle Falsa 123",
                    "600111222", "ana@test.com", "password123");

            ClienteNoEncontradoException error = assertThrows(ClienteNoEncontradoException.class,
                    () -> clienteController.actualizar(inexistente));
            assertEquals("No existe ningún cliente con id 999", error.getMessage());
        }

        @Test
        @DisplayName("comprueba los datos obligatorios igual que registrar")
        void rechazaDatoObligatorioVacio() throws ClienteYaExisteException, SQLException {
            Cliente ana = clienteController.registrar(clienteValido("ana@test.com"));
            Cliente sinEmail = new Cliente(ana.getId(), ana.getDni(), ana.getNombre(), ana.getDireccion(),
                    ana.getTelefono(), "", ana.getPassword());

            IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                    () -> clienteController.actualizar(sinEmail));
            assertEquals("El email es un dato obligatorio", error.getMessage());
        }

        @Test
        @DisplayName("no deja poner el email de otro cliente: lo impide el UNIQUE de la tabla")
        void rechazaEmailDeOtroCliente() throws ClienteYaExisteException, ClienteNoEncontradoException, SQLException {
            clienteController.registrar(clienteValido("ana@test.com"));
            Cliente carlos = clienteController.registrar(clienteValido("carlos@test.com"));
            Cliente carlosConEmailDeAna = new Cliente(carlos.getId(), carlos.getDni(), carlos.getNombre(),
                    carlos.getDireccion(), carlos.getTelefono(), "ana@test.com", carlos.getPassword());

            assertThrows(SQLIntegrityConstraintViolationException.class,
                    () -> clienteController.actualizar(carlosConEmailDeAna));
            assertEquals("carlos@test.com", clienteController.buscarPorId(carlos.getId()).getEmail());
        }
    }

    @Nested
    @DisplayName("eliminar()")
    class Eliminar {

        @Test
        @DisplayName("borra el cliente de la base de datos y no toca a los demás")
        void eliminaSoloEseCliente() throws ClienteYaExisteException, ClienteNoEncontradoException, SQLException {
            Cliente ana = clienteController.registrar(clienteValido("ana@test.com"));
            Cliente carlos = clienteController.registrar(clienteValido("carlos@test.com"));

            clienteController.eliminar(ana.getId());

            assertThrows(ClienteNoEncontradoException.class, () -> clienteController.buscarPorId(ana.getId()));
            assertEquals(List.of(carlos), clienteController.listarTodos());
        }

        @Test
        @DisplayName("lanza ClienteNoEncontradoException si el cliente no existe")
        void rechazaClienteInexistente() {
            ClienteNoEncontradoException error = assertThrows(ClienteNoEncontradoException.class,
                    () -> clienteController.eliminar(999));
            assertEquals("No existe ningún cliente con id 999", error.getMessage());
        }
    }

    @Nested
    @DisplayName("equals() y hashCode() de Cliente")
    class IgualdadPorId {

        @Test
        @DisplayName("dos clientes con el mismo id son iguales y tienen el mismo hashCode; con distinto id no")
        void clientesIgualesPorId() {
            Cliente ana = new Cliente(3, "12345678A", "Ana García", "Calle Falsa 123",
                    "600111222", "ana@test.com", "password123");
            Cliente anaMudada = new Cliente(3, "12345678A", "Ana García", "Calle Mayor 1",
                    "600999888", "ana@test.com", "password123");

            assertEquals(ana, anaMudada);
            assertEquals(ana.hashCode(), anaMudada.hashCode());
            assertNotEquals(ana, new Cliente(4, "12345678A", "Ana García", "Calle Falsa 123",
                    "600111222", "ana@test.com", "password123"));
        }

        @Test
        @DisplayName("dos clientes sin id (id 0) solo son iguales a sí mismos, aunque tengan los mismos datos")
        void clientesSinIdSoloIgualesASiMismos() {
            Cliente uno = clienteValido("ana@test.com", "12345678A");
            Cliente otro = clienteValido("ana@test.com", "12345678A");

            assertEquals(uno, uno);
            assertNotEquals(uno, otro);
        }
    }
}