package org.teamforge.service;

import java.sql.SQLException;
import java.util.Optional;

import org.teamforge.exception.ValidationException;
import org.teamforge.model.User;
import org.teamforge.dao.UserDAO;
import org.teamforge.util.PasswordUtil;
import org.teamforge.util.Session;

public class AuthService extends BaseService {
    private static final int MIN_PASSWORD_LENGTH = 6;

    private final UserDAO userDAO = new UserDAO();

    public User register(String name, String email, String password, String confirmPassword)
            throws SQLException, ValidationException {
        String cleanName = requireText(name, "Nama");
        String cleanEmail = validateEmail(email);
        if (password == null || password.length() < MIN_PASSWORD_LENGTH) {
            throw new ValidationException("Password minimal " + MIN_PASSWORD_LENGTH + " karakter.");
        }
        if (!password.equals(confirmPassword)) {
            throw new ValidationException("Konfirmasi password tidak sama.");
        }
        if (userDAO.findByEmail(cleanEmail).isPresent()) {
            throw new ValidationException("Email sudah digunakan.");
        }
        User user = new User(cleanName, cleanEmail, PasswordUtil.hash(password));
        userDAO.insert(user);
        return user;
    }

    public User login(String email, String password) throws SQLException, ValidationException {
        requireText(email, "Email");
        requireText(password, "Password");
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

    public void updateProfile(User user, String name, String email) throws SQLException, ValidationException {
        String cleanName = requireText(name, "Nama");
        String cleanEmail = validateEmail(email);
        Optional<User> other = userDAO.findByEmail(cleanEmail);
        if (other.isPresent() && other.get().getId() != user.getId()) {
            throw new ValidationException("Email sudah digunakan akun lain.");
        }
        user.setName(cleanName);
        user.setEmail(cleanEmail);
        userDAO.update(user);
    }

    public void deleteAccount(User user) throws SQLException, ValidationException {
        if (userDAO.isUsed(user.getId())) {
            throw new ValidationException("Akun masih tercatat sebagai anggota project atau penerima tugas,"
                    + " sehingga belum dapat dihapus.");
        }
        userDAO.delete(user.getId());
        Session.clear();
    }

    public void logout() {
        Session.clear();
    }

    private String validateEmail(String email) throws ValidationException {
        String cleanEmail = requireText(email, "Email").toLowerCase();
        if (!cleanEmail.matches("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+$")) {
            throw new ValidationException("Format email tidak valid.");
        }
        return cleanEmail;
    }
}
