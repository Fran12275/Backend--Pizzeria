package org.pizzeria.controller.dao;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

// Operaciones comunes a todos los DAO. Las propias de cada entidad van en su interfaz (ClienteDao, ...)
public interface Dao<T> {

    // Guarda un objeto nuevo y le pone el id que genera la base de datos
    void save(T entidad) throws SQLException;

    // Optional vacio si no existe: quien llama decide que hacer, sin riesgo de NullPointerException
    Optional<T> findById(long id) throws SQLException;

    List<T> findAll() throws SQLException;

    // Guarda los datos actuales del objeto en la fila que tiene su id
    void update(T entidad) throws SQLException;

    // Para borrar basta con el id: no hace falta tener el objeto entero
    void delete(long id) throws SQLException;
}
