package org.teamforge.service;

import java.sql.SQLException;

import org.teamforge.exception.TeamForgeException;

/**
 * Sekumpulan perintah database yang harus berhasil bersama atau dibatalkan bersama.
 */
@FunctionalInterface
public interface TransactionAction {
    void execute() throws SQLException, TeamForgeException;
}
