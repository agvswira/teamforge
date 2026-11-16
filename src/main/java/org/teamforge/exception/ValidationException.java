package org.teamforge.exception;

/**
 * Dilempar ketika aturan aplikasi dilanggar, misalnya input kosong, email sudah dipakai,
 * atau pengguna tidak memiliki hak akses.
 */
public class ValidationException extends Exception {

    public ValidationException(String message) {
        super(message);
    }
}
