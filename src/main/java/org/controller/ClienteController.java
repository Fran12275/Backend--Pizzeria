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
