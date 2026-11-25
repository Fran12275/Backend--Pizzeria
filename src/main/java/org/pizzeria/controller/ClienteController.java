package org.pizzeria.controller;

import org.pizzeria.controller.dao.ClienteDao;
import org.pizzeria.controller.dao.impl.JdbcClienteDao;
import org.pizzeria.exception.ClienteNoEncontradoException;
import org.pizzeria.exception.ClienteYaExisteException;
import org.pizzeria.exception.CredencialesInvalidasException;
import org.pizzeria.model.Cliente;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class ClienteController {
    private final ClienteDao clienteDao = JdbcClienteDao.getInstance();

    public Cliente registrar(Cliente clienteSinId) throws ClienteYaExisteException, SQLException {
        validarDatosCliente(clienteSinId);

        if (buscarPorEmail(clienteSinId.getEmail()).isPresent()){
            throw new ClienteYaExisteException("Ya existe un cliente registrado con el email: " +
                    clienteSinId.getEmail());
        }
        if (dniEnUso(clienteSinId.getDni())){
            throw new ClienteYaExisteException("Ya existe un cliente registrado con el DNI: " +
                    clienteSinId.getDni());
        }
        // Al guardarlo, MySQL genera el id y save se lo pone a este mismo objeto
        clienteDao.save(clienteSinId);
        return clienteSinId;

    }
    public Cliente login(String email, String password) throws CredencialesInvalidasException, SQLException {
        // El mismo mensaje tanto si falla el email como la password: asi no se revela que emails estan registrados
        return buscarPorEmail(email)
                .filter(c -> c.getPassword().equals(password))
                .orElseThrow(() -> new CredencialesInvalidasException("El email o la contraseña no son correctos"));
    }

    public Cliente buscarPorId(long id) throws ClienteNoEncontradoException, SQLException {
        return clienteDao.findById(id)
                .orElseThrow(() -> new ClienteNoEncontradoException("No existe ningún cliente con id " + id));
    }

    public Optional<Cliente> buscarPorEmail(String email) throws SQLException {
        return clienteDao.findByEmail(email);
    }
    public List<Cliente> listarTodos() throws SQLException { return clienteDao.findAll();}

    public void actualizar(Cliente cliente) throws ClienteNoEncontradoException, SQLException {
        validarDatosCliente(cliente);
        // Solo se actualiza un cliente que ya existe: si no, ClienteNoEncontradoException
        buscarPorId(cliente.getId());
        clienteDao.update(cliente);
    }

    public void eliminar(long id) throws ClienteNoEncontradoException, SQLException {
        buscarPorId(id);
        clienteDao.delete(id);
    }

    // La letra del DNI puede venir en mayuscula o minuscula: MySQL no las distingue, asi que 12345678a es el mismo DNI que 12345678A
    private boolean dniEnUso(String dni) throws SQLException {
        return clienteDao.findByDni(dni).isPresent();
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
