package org.teamforge.controller;

import java.sql.SQLException;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import org.teamforge.exception.ValidationException;
import org.teamforge.service.AuthService;
import org.teamforge.util.AlertUtil;
import org.teamforge.util.Session;
import org.teamforge.util.StyleHelper;

/**
 * Dialog untuk mengubah nama dan email, atau menghapus akun.
 */
public class AccountController {
    private final AuthService authService = new AuthService();

    @FXML private TextField txtNama;
    @FXML private TextField txtEmail;
    @FXML private Label lblPesan;
    @FXML private Button btnBatal;

    @FXML
    private void initialize() {
        txtNama.setText(Session.getCurrentUser().getName());
        txtEmail.setText(Session.getCurrentUser().getEmail());
    }

    @FXML
    private void handleSimpan(ActionEvent event) {
        try {
            authService.updateProfile(Session.getCurrentUser(), txtNama.getText(), txtEmail.getText());
            close();
        } catch (ValidationException e) {
            StyleHelper.showMessage(lblPesan, e.getMessage());
        } catch (SQLException e) {
            AlertUtil.showError(e);
        }
    }

    @FXML
    private void handleHapus(ActionEvent event) {
        if (!AlertUtil.confirm("Hapus akun ini?")) {
            return;
        }
        try {
            authService.deleteAccount(Session.getCurrentUser());
            close();
        } catch (ValidationException e) {
            StyleHelper.showMessage(lblPesan, e.getMessage());
        } catch (SQLException e) {
            AlertUtil.showError(e);
        }
    }

    @FXML
    private void handleBatal(ActionEvent event) {
        close();
    }

    private void close() {
        Stage stage = (Stage) btnBatal.getScene().getWindow();
        stage.close();
    }
}
