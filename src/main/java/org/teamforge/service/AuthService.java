package org.teamforge.service;

import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.Optional;

import org.teamforge.dao.UserDAO;
import org.teamforge.exception.ValidationException;
import org.teamforge.model.User;
import org.teamforge.util.PasswordUtil;
import org.teamforge.util.Session;

/**
 * Registrasi, login, logout, ubah profil, dan hapus akun.
 * Password disimpan sebagai hash dengan format salt$sha256(salt + password), bukan password asli.
 */
public class AuthService {
    private static final int MIN_PASSWORD_LENGTH = 6;

    private final UserDAO userDAO = new UserDAO();

    public User register(String name, String email, String password, String confirmPassword)
            throws SQLException, ValidationException {
        if (name == null || name.isBlank()) {
            throw new ValidationException("Nama wajib diisi.");
        }
        String cleanEmail = checkEmail(email);
        if (password == null || password.length() < MIN_PASSWORD_LENGTH) {
            throw new ValidationException("Password minimal " + MIN_PASSWORD_LENGTH + " karakter.");
        }
        if (!password.equals(confirmPassword)) {
            throw new ValidationException("Ulangi password tidak sama.");
        }
        if (userDAO.findByEmail(cleanEmail).isPresent()) {
            throw new ValidationException("Email sudah terdaftar. Masuk atau pakai email lain.");
        }
        User user = new User(name.trim(), cleanEmail, PasswordUtil.hash(password));
        userDAO.insert(user);
        return user;
    }

    public User login(String email, String password) throws SQLException, ValidationException {
        if (email == null || email.isBlank() || password == null || password.isEmpty()) {
            throw new ValidationException("Email dan password wajib diisi.");
        }
        Optional<User> found = userDAO.findByEmail(email.trim().toLowerCase());
        if (found.isEmpty() || !PasswordUtil.verify(password, found.get().getPasswordHash())) {
            throw new ValidationException("Email atau password salah.");
        }
        User user = found.get();
        if (!user.isActive()) {
            throw new ValidationException("Akun ini sudah dinonaktifkan.");
        }
        Session.setCurrentUser(user);
        return user;
    }

    public void logout() {
        Session.clear();
    }

    public void updateProfile(User user, String name, String email) throws SQLException, ValidationException {
        if (name == null || name.isBlank()) {
            throw new ValidationException("Nama wajib diisi.");
        }
        String cleanEmail = checkEmail(email);
        Optional<User> other = userDAO.findByEmail(cleanEmail);
        if (other.isPresent() && other.get().getId() != user.getId()) {
            throw new ValidationException("Email sudah dipakai akun lain.");
        }
        user.setName(name.trim());
        user.setEmail(cleanEmail);
        userDAO.update(user);
    }

    // Foreign key menolak penghapusan jika akun masih tercatat di project atau tugas
    public void deleteAccount(User user) throws SQLException, ValidationException {
        try {
            userDAO.delete(user.getId());
        } catch (SQLIntegrityConstraintViolationException e) {
            throw new ValidationException("Akun masih tercatat di project atau tugas, jadi belum dapat dihapus.");
        }
        Session.clear();
    }

    private String checkEmail(String email) throws ValidationException {
        if (email == null || email.isBlank()) {
            throw new ValidationException("Email wajib diisi.");
        }
        String cleanEmail = email.trim().toLowerCase();
        if (!cleanEmail.contains("@") || !cleanEmail.contains(".")) {
            throw new ValidationException("Format email tidak valid.");
        }
        return cleanEmail;
    }
}
