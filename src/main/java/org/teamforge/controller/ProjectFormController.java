package org.teamforge.controller;

import java.sql.SQLException;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import org.teamforge.exception.ValidationException;
import org.teamforge.model.Project;
import org.teamforge.service.ProjectService;
import org.teamforge.util.AlertUtil;
import org.teamforge.util.Session;
import org.teamforge.util.StyleHelper;

/**
 * Dialog membuat project baru atau mengubah project yang sudah ada.
 */
public class ProjectFormController {
    private final ProjectService projectService = new ProjectService();
    private Project project;
    private boolean deleted = false;

    @FXML private Label lblJudul;
    @FXML private TextField txtNama;
    @FXML private TextArea txtDeskripsi;
    @FXML private DatePicker dpDeadline;
    @FXML private Label lblPesan;
    @FXML private Button btnHapus;
    @FXML private Button btnSimpan;

    // project bernilai null berarti membuat project baru
    public void setProject(Project project) {
        this.project = project;
        if (project != null) {
            lblJudul.setText("Ubah project");
            txtNama.setText(project.getName());
            txtDeskripsi.setText(project.getDescription());
            dpDeadline.setValue(project.getDeadline());
            StyleHelper.setShown(btnHapus, true);
        }
    }

    public boolean isDeleted() {
        return deleted;
    }

    @FXML
    private void handleSimpan(ActionEvent event) {
        try {
            if (project == null) {
                projectService.createProject(txtNama.getText(), txtDeskripsi.getText(),
                        dpDeadline.getValue(), Session.getCurrentUser());
            } else {
                projectService.updateProject(project, txtNama.getText(), txtDeskripsi.getText(),
                        dpDeadline.getValue(), Session.getCurrentUser().getId());
            }
            close();
        } catch (ValidationException e) {
            StyleHelper.showMessage(lblPesan, e.getMessage());
        } catch (SQLException e) {
            AlertUtil.showError(e);
        }
    }

    @FXML
    private void handleHapus(ActionEvent event) {
        if (!AlertUtil.confirm("Hapus project \"" + project.getName() + "\"?")) {
            return;
        }
        try {
            projectService.deleteProject(project.getId(), Session.getCurrentUser().getId());
            deleted = true;
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
        Stage stage = (Stage) btnSimpan.getScene().getWindow();
        stage.close();
    }
}
