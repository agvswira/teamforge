package org.teamforge.util;

import java.sql.SQLException;
import java.util.Optional;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

import org.teamforge.exception.DependencyException;
import org.teamforge.exception.ValidationException;
import org.teamforge.model.Task;

/**
 * Menampilkan pesan hasil operasi kepada pengguna.
 */
public class AlertUtil {

    private AlertUtil() {
    }

    public static void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, message);
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    public static void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message);
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    // Pesan disesuaikan dengan jenis exception yang terjadi
    public static void showError(Exception e) {
        if (e instanceof DependencyException) {
            DependencyException de = (DependencyException) e;
            String message = de.getMessage();
            for (Task blocker : de.getBlockers()) {
                message = message + "\n- " + blocker.getTitle() + " (" + blocker.getStatus() + ")";
            }
            showError(message);
        } else if (e instanceof ValidationException) {
            showError(e.getMessage());
        } else if (e instanceof SQLException) {
            showError("Gagal mengakses database. Pastikan MySQL sudah berjalan dan pengaturan"
                    + " di db.properties benar.\n\nDetail: " + e.getMessage());
        } else {
            showError("Terjadi kesalahan: " + e.getMessage());
        }
    }

    public static boolean confirm(String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, message, ButtonType.YES, ButtonType.NO);
        alert.setHeaderText(null);
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.YES;
    }
}
