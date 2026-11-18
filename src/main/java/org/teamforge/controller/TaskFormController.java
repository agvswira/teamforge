package org.teamforge.controller;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import org.teamforge.exception.ValidationException;
import org.teamforge.model.Project;
import org.teamforge.model.ProjectMembership;
import org.teamforge.model.Task;
import org.teamforge.service.ProjectService;
import org.teamforge.service.TaskService;
import org.teamforge.util.AlertUtil;
import org.teamforge.util.Session;
import org.teamforge.util.StyleHelper;

/**
 * Dialog membuat atau mengubah tugas, termasuk penerima (task_assignments)
 * dan prasyarat (task_dependencies).
 */
public class TaskFormController {
    private final TaskService taskService = new TaskService();
    private final ProjectService projectService = new ProjectService();

    private Project project;
    private Task task;
    private List<Task> projectTasks = new ArrayList<>();
    private final List<Task> prerequisites = new ArrayList<>();
    // Satu CheckBox untuk setiap anggota, userData berisi id user
    private final List<CheckBox> assigneeChecks = new ArrayList<>();

    @FXML private Label lblJudul;
    @FXML private TextField txtJudul;
    @FXML private TextArea txtDeskripsi;
    @FXML private ComboBox<String> cmbPrioritas;
    @FXML private DatePicker dpTenggat;
    @FXML private VBox listPenerima;
    @FXML private VBox listPrasyarat;
    @FXML private ComboBox<Task> cmbTambahPrasyarat;
    @FXML private Label lblPesan;
    @FXML private Button btnHapus;
    @FXML private Button btnSimpan;

    @FXML
    private void initialize() {
        cmbPrioritas.setItems(FXCollections.observableArrayList("Rendah", "Sedang", "Tinggi"));
        cmbPrioritas.setValue("Sedang");
    }

    // task bernilai null berarti membuat tugas baru
    public void setData(Project project, Task task) {
        this.project = project;
        this.task = task;
        try {
            projectTasks = taskService.getTasks(project.getId());
            for (ProjectMembership member : projectService.getMembers(project.getId())) {
                CheckBox check = new CheckBox(member.getUser().getName());
                check.setUserData(member.getUserId());
                check.setSelected(task != null && task.isAssignedTo(member.getUserId()));
                assigneeChecks.add(check);
                listPenerima.getChildren().add(check);
            }

            if (task == null) {
                StyleHelper.setShown(btnHapus, false);
            } else {
                lblJudul.setText("Ubah tugas");
                txtJudul.setText(task.getTitle());
                txtDeskripsi.setText(task.getDescription());
                cmbPrioritas.getSelectionModel().select(task.getPriority() - 1);
                dpTenggat.setValue(task.getDueAt());
                prerequisites.addAll(task.getPrerequisites());
                Optional<String> blocker = taskService.getDeleteBlocker(task);
                btnHapus.setDisable(blocker.isPresent());
            }
            showPrerequisites();
        } catch (SQLException e) {
            AlertUtil.showError(e);
        }
    }

    // Menampilkan prasyarat terpilih dan memperbarui pilihan ComboBox
    private void showPrerequisites() {
        listPrasyarat.getChildren().clear();
        for (Task prerequisite : prerequisites) {
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            Button remove = new Button("Lepas");
            remove.getStyleClass().add("small");
            remove.setOnAction(e -> {
                prerequisites.remove(prerequisite);
                showPrerequisites();
            });
            HBox row = new HBox(8, new Label(prerequisite.getTitle()), spacer, remove);
            row.setAlignment(Pos.CENTER_LEFT);
            row.getStyleClass().add("prereq-row");
            listPrasyarat.getChildren().add(row);
        }

        // Pilihan: tugas lain di project yang belum menjadi prasyarat
        List<Task> candidates = new ArrayList<>();
        for (Task other : projectTasks) {
            boolean isSelf = task != null && other.getId() == task.getId();
            boolean alreadyChosen = false;
            for (Task chosen : prerequisites) {
                if (chosen.getId() == other.getId()) {
                    alreadyChosen = true;
                }
            }
            if (!isSelf && !alreadyChosen) {
                candidates.add(other);
            }
        }
        cmbTambahPrasyarat.setItems(FXCollections.observableArrayList(candidates));
    }

    // Prasyarat diperiksa dulu sebelum ditambahkan ke daftar
    @FXML
    private void handleTambahPrasyarat(ActionEvent event) {
        Task chosen = cmbTambahPrasyarat.getValue();
        if (chosen == null) {
            return;
        }
        Task checked = task;
        if (checked == null) {
            checked = new Task(project.getId(), "Tugas baru", null, Task.PRIORITY_MEDIUM, null);
        }
        try {
            taskService.checkPrerequisite(checked, chosen.getId());
            prerequisites.add(chosen);
            StyleHelper.hideMessage(lblPesan);
            // Daftar pilihan diperbarui setelah event ComboBox selesai diproses
            Platform.runLater(() -> showPrerequisites());
        } catch (ValidationException e) {
            StyleHelper.showMessage(lblPesan, e.getMessage());
        } catch (SQLException e) {
            AlertUtil.showError(e);
        }
    }

    @FXML
    private void handleSimpan(ActionEvent event) {
        List<Integer> assigneeIds = new ArrayList<>();
        for (CheckBox check : assigneeChecks) {
            if (check.isSelected()) {
                assigneeIds.add((Integer) check.getUserData());
            }
        }
        List<Integer> prerequisiteIds = new ArrayList<>();
        for (Task prerequisite : prerequisites) {
            prerequisiteIds.add(prerequisite.getId());
        }
        int priority = cmbPrioritas.getSelectionModel().getSelectedIndex() + 1;
        int actorId = Session.getCurrentUser().getId();

        try {
            if (task == null) {
                taskService.createTask(project.getId(), actorId, txtJudul.getText(), txtDeskripsi.getText(),
                        priority, dpTenggat.getValue(), assigneeIds, prerequisiteIds);
            } else {
                taskService.updateTask(task.getId(), actorId, txtJudul.getText(), txtDeskripsi.getText(),
                        priority, dpTenggat.getValue(), assigneeIds, prerequisiteIds);
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
        if (!AlertUtil.confirm("Hapus tugas \"" + task.getTitle() + "\"?")) {
            return;
        }
        try {
            taskService.deleteTask(task.getId(), Session.getCurrentUser().getId());
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
