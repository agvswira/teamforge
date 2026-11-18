package org.teamforge.exception;

/**
 * Dilempar ketika role pengguna pada project tidak mengizinkan tindakan tersebut.
 */
public class AccessDeniedException extends TeamForgeException {

    public AccessDeniedException(String message) {
        super(message);
    }
}
