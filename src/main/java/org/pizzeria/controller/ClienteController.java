package org.pizzeria.controller;

import org.pizzeria.exception.ClienteNoEncontradoException;
import org.pizzeria.exception.ClienteYaExisteException;
import org.pizzeria.exception.CredencialesInvalidasException;
import org.pizzeria.model.Cliente;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

public class ClienteController {
    private final List<Cliente> clientes = new ArrayList<>();
    private final AtomicInteger secuenciaId = new AtomicInteger(1);

    public Cliente registrar(Cliente clienteSinId) throws ClienteYaExisteException {
        validarDatosCliente(clienteSinId);

        if (buscarPorEmail(clienteSinId.getEmail()).isPresent()){
            throw new ClienteYaExisteException("Ya existe un cliente registrado con el email: " +
                    clienteSinId.getEmail());
        }
        if (dniEnUso(clienteSinId.getDni())){
            throw new ClienteYaExisteException("Ya existe un cliente registrado con el DNI: " +
                    clienteSinId.getDni());
        }
        Cliente nuevoCliente = new Cliente(
                secuenciaId.getAndIncrement(),
                clienteSinId.getDni(),
                clienteSinId.getNombre(),
                clienteSinId.getDireccion(),
                clienteSinId.getTelefono(),
                clienteSinId.getEmail(),
                clienteSinId.getPassword()
        );
        clientes.add(nuevoCliente);
        return nuevoCliente;

    }
    public Cliente login(String email, String password) throws CredencialesInvalidasException {
        // El mismo mensaje tanto si falla el email como la password: asi no se revela que emails estan registrados
        return buscarPorEmail(email)
                .filter(c -> c.getPassword().equals(password))
                .orElseThrow(() -> new CredencialesInvalidasException("El email o la contraseña no son correctos"));
    }

    public Cliente buscarPorId(int id) throws ClienteNoEncontradoException {
        return clientes.stream()
                .filter(c -> c.getId() == id)
                .findFirst()
                .orElseThrow(() -> new ClienteNoEncontradoException("No existe ningún cliente con id " + id));
    }

    public Optional<Cliente> buscarPorEmail(String email){
        return clientes.stream()
                .filter(c -> c.getEmail().equalsIgnoreCase(email))
                .findFirst();
    }
    public List<Cliente> listarTodos(){ return List.copyOf(clientes);}

    // La letra del DNI puede venir en mayuscula o minuscula: 12345678a es el mismo DNI que 12345678A
    private boolean dniEnUso(String dni) {
        return clientes.stream()
                .anyMatch(c -> c.getDni().equalsIgnoreCase(dni));
    }

    private void validarDatosCliente(Cliente cliente){
        if (cliente == null){
            throw new IllegalArgumentException("El cliente no puede ser null");
        }
        validarObligatorio(cliente.getEmail(), "El email");
        validarObligatorio(cliente.getNombre(), "El nombre");
        validarObligatorio(cliente.getDni(), "El DNI");
        validarObligatorio(cliente.getPassword(), "La contraseña");
    }

    // Un dato obligatorio no puede faltar ni estar en blanco (solo espacios tampoco vale)
    private static void validarObligatorio(String valor, String dato) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(dato + " es un dato obligatorio");
        }
    }
}
