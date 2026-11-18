package org.teamforge.controller;

import java.io.IOException;
import java.sql.SQLException;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import org.teamforge.App;
import org.teamforge.exception.ValidationException;
import org.teamforge.service.AuthService;
import org.teamforge.util.AlertUtil;
import org.teamforge.util.StyleHelper;

/**
 * Halaman masuk dan daftar akun. Kedua form ada di satu FXML dan ditampilkan bergantian.
 */
public class LoginController {
    private final AuthService authService = new AuthService();

    @FXML private VBox formMasuk;
    @FXML private TextField txtEmail;
    @FXML private PasswordField txtPassword;
    @FXML private Label lblPesanMasuk;

    @FXML private VBox formDaftar;
    @FXML private TextField txtNama;
    @FXML private TextField txtEmailBaru;
    @FXML private PasswordField txtPasswordBaru;
    @FXML private PasswordField txtUlangiPassword;
    @FXML private Label lblPesanDaftar;

    @FXML
    private void handleMasuk(ActionEvent event) {
        try {
            authService.login(txtEmail.getText(), txtPassword.getText());
            App.setRoot("ProjectListView");
        } catch (ValidationException e) {
            StyleHelper.showMessage(lblPesanMasuk, e.getMessage());
            txtPassword.clear();
        } catch (SQLException | IOException e) {
            AlertUtil.showError(e);
        }
    }

    @FXML
    private void handleDaftar(ActionEvent event) {
        try {
            authService.register(txtNama.getText(), txtEmailBaru.getText(),
                    txtPasswordBaru.getText(), txtUlangiPassword.getText());
            AlertUtil.showInfo("Akun berhasil dibuat. Silakan masuk.");
            txtEmail.setText(txtEmailBaru.getText());
            handleKeMasuk(null);
        } catch (ValidationException e) {
            StyleHelper.showMessage(lblPesanDaftar, e.getMessage());
        } catch (SQLException e) {
            AlertUtil.showError(e);
        }
    }

    @FXML
    private void handleKeDaftar(ActionEvent event) {
        StyleHelper.setShown(formMasuk, false);
        StyleHelper.setShown(formDaftar, true);
    }

    @FXML
    private void handleKeMasuk(ActionEvent event) {
        StyleHelper.setShown(formDaftar, false);
        StyleHelper.setShown(formMasuk, true);
    }
}
