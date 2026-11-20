package org.teamforge.dao;

import java.sql.SQLException;
import java.util.Optional;

/**
 * Operasi CRUD umum untuk tabel yang memiliki id tunggal.
 * T adalah tipe objek yang disimpan, misalnya User, Project, atau Task.
 */
public interface CrudDAO<T> {

    // CREATE: menyimpan data baru dan mengembalikan id yang dibuat database
    int insert(T item) throws SQLException;

    Optional<T> findById(int id) throws SQLException;

    void update(T item) throws SQLException;

    void delete(int id) throws SQLException;
}
