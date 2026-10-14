package org.controller;

import org.exception.ClienteNoEncontradoException;
import org.exception.ClienteYaExisteException;
import org.exception.CredencialesInvalidasException;
import org.model.Cliente;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

public class ClienteController {
    private final List<Cliente> clientes = new ArrayList<>();
    private final AtomicInteger secuenciaId = new AtomicInteger(1);

    public Cliente registrar(Cliente clienteSinId) throws ClienteYaExisteException {
        validarDatosCliente(clienteSinId);

        boolean emailEnUso = clientes.stream()
                .anyMatch(c -> c.getEmail().equalsIgnoreCase(clienteSinId.getEmail()));
        if (emailEnUso){
            throw new ClienteYaExisteException("Ya existe un cliente registrado con el email: " +
                    clienteSinId.getEmail());
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
        return clientes.stream()
                .filter(c -> c.getEmail().equalsIgnoreCase(email))
                .filter(c -> c.getPassword().equals(password))
                .findFirst()
                .orElseThrow(() -> new CredencialesInvalidasException(""));
    }

    public Cliente buscarPorId(int id) throws ClienteNoEncontradoException {
        return clientes.stream()
                .filter(c -> c.getId() == id)
                .findFirst()
                .orElseThrow(() -> new ClienteNoEncontradoException(""));
    }

    public Optional<Cliente> buscarPorEmail(String email){
        return clientes.stream()
                .filter(c -> c.getEmail().equalsIgnoreCase(email))
                .findFirst();
    }
    public List<Cliente> listarTodos(){ return List.copyOf(clientes);}

    private void validarDatosCliente(Cliente cliente){
        if (cliente == null){
            throw new IllegalArgumentException("");
        }
        if (cliente.getEmail() == null || cliente.getEmail().isBlank()){
            throw new IllegalArgumentException();
        }
        if (cliente.getNombre() == null || cliente.getNombre().isBlank()){
            throw new IllegalArgumentException("");
        }
        if (cliente.getDni() == null || cliente.getDni().isBlank()){
            throw new IllegalArgumentException("");
        }
        if (cliente.getPassword() == null || cliente.getPassword().isBlank()) {
            throw new IllegalArgumentException("La password es obligatoria");
        }
    }
}
