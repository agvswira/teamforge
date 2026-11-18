package org.teamforge.exception;

/**
 * Exception induk untuk semua pelanggaran aturan aplikasi TeamForge.
 */
public class TeamForgeException extends Exception {

    public TeamForgeException(String message) {
        super(message);
    }
}
