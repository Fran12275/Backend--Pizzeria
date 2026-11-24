package org.pizzeria.controller.dao;

import org.pizzeria.model.Cliente;

import java.sql.SQLException;
import java.util.Optional;

public interface ClienteDao extends Dao<Cliente> {

    Optional<Cliente> findByEmail(String email) throws SQLException;

    Optional<Cliente> findByDni(String dni) throws SQLException;
}
