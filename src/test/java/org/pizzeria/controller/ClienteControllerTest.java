package org.pizzeria.controller;

import org.pizzeria.exception.ClienteNoEncontradoException;
import org.pizzeria.exception.ClienteYaExisteException;
import org.pizzeria.exception.CredencialesInvalidasException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.pizzeria.model.Cliente;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;


class ClienteControllerTest {

    private ClienteController clienteController;

    @BeforeEach
    void setUp() {
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
        void registraClienteValido() throws ClienteYaExisteException {
            Cliente registrado = clienteController.registrar(clienteValido("ana@test.com"));

            assertEquals(1, registrado.getId());
            assertEquals("ana@test.com", registrado.getEmail());
            assertEquals(1, clienteController.listarTodos().size());
        }

        @Test
        @DisplayName("asigna ids incrementales a clientes sucesivos")
        void asignaIdsIncrementales() throws ClienteYaExisteException {
            Cliente c1 = clienteController.registrar(clienteValido("uno@test.com"));
            Cliente c2 = clienteController.registrar(clienteValido("dos@test.com"));

            assertEquals(1, c1.getId());
            assertEquals(2, c2.getId());
        }

        @Test
        @DisplayName("lanza ClienteYaExisteException si el email ya está registrado")
        void rechazaEmailDuplicado() throws ClienteYaExisteException {
            clienteController.registrar(clienteValido("dup@test.com"));

            assertThrows(ClienteYaExisteException.class,
                    () -> clienteController.registrar(clienteValido("dup@test.com")));
        }

        @Test
        @DisplayName("la comprobación de email duplicado ignora mayúsculas/minúsculas")
        void emailDuplicadoEsCaseInsensitive() throws ClienteYaExisteException {
            clienteController.registrar(clienteValido("Dup@Test.com"));

            assertThrows(ClienteYaExisteException.class,
                    () -> clienteController.registrar(clienteValido("dup@test.com")));
        }

        @Test
        @DisplayName("lanza ClienteYaExisteException si el DNI ya está registrado")
        void rechazaDniDuplicado() throws ClienteYaExisteException {
            clienteController.registrar(clienteValido("ana@test.com", "12345678A"));

            assertThrows(ClienteYaExisteException.class,
                    () -> clienteController.registrar(clienteValido("otra@test.com", "12345678A")));
            assertEquals(1, clienteController.listarTodos().size());
        }

        @Test
        @DisplayName("la comprobación de DNI duplicado ignora mayúsculas/minúsculas en la letra")
        void dniDuplicadoEsCaseInsensitive() throws ClienteYaExisteException {
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
    }

    @Nested
    @DisplayName("login()")
    class Login {

        @BeforeEach
        void registrarClientePrevio() throws ClienteYaExisteException {
            clienteController.registrar(clienteValido("ana@test.com"));
        }

        @Test
        @DisplayName("login correcto devuelve el cliente registrado")
        void loginCorrecto() throws CredencialesInvalidasException {
            Cliente logueado = clienteController.login("ana@test.com", "password123");

            assertEquals("ana@test.com", logueado.getEmail());
            assertEquals("Ana García", logueado.getNombre());
        }

        @Test
        @DisplayName("login es case-insensitive para el email")
        void loginEmailCaseInsensitive() throws CredencialesInvalidasException {
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
        void buscarPorIdExistente() throws ClienteYaExisteException, ClienteNoEncontradoException {
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
        void buscarPorEmailExistente() throws ClienteYaExisteException {
            clienteController.registrar(clienteValido("ana@test.com"));

            Optional<Cliente> resultado = clienteController.buscarPorEmail("ana@test.com");

            assertTrue(resultado.isPresent());
            assertEquals("ana@test.com", resultado.get().getEmail());
        }

        @Test
        @DisplayName("buscarPorEmail devuelve Optional vacío si no existe")
        void buscarPorEmailInexistente() {
            Optional<Cliente> resultado = clienteController.buscarPorEmail("noexiste@test.com");
            assertFalse(resultado.isPresent());
        }
    }

    @Nested
    @DisplayName("listarTodos()")
    class ListarTodos {

        @Test
        @DisplayName("devuelve todos los clientes registrados")
        void listaTodosLosClientes() throws ClienteYaExisteException {
            clienteController.registrar(clienteValido("uno@test.com"));
            clienteController.registrar(clienteValido("dos@test.com"));

            List<Cliente> todos = clienteController.listarTodos();

            assertEquals(2, todos.size());
        }

        @Test
        @DisplayName("devuelve una lista inmutable (no se puede modificar desde fuera)")
        void listaEsInmutable() throws ClienteYaExisteException {
            clienteController.registrar(clienteValido("uno@test.com"));
            List<Cliente> todos = clienteController.listarTodos();

            assertThrows(UnsupportedOperationException.class,
                    () -> todos.add(clienteValido("intruso@test.com")));
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