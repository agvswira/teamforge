package org.teamforge.controller;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import org.teamforge.exception.ValidationException;
import org.teamforge.model.Task;
import org.teamforge.model.TaskStatus;
import org.teamforge.util.AlertUtil;
import org.teamforge.util.StyleHelper;

/**
 * Halaman "Tugas saya": daftar tugas milik pengguna dan panel detail untuk mengerjakannya.
 */
public class MyTasksController extends ProjectPageController {
    private Task selected;
    private int selectedId = -1;

    @FXML private VBox listTugas;
    @FXML private Label lblKosong;
    @FXML private ScrollPane panelDetail;
    @FXML private HBox boxChips;
    @FXML private Label lblJudulTugas;
    @FXML private Label lblDeskripsi;
    @FXML private Label lblTenggat;
    @FXML private Label lblPrioritas;
    @FXML private Label lblPenerima;
    @FXML private VBox listPrasyarat;
    @FXML private TextArea txtCatatan;
    @FXML private Button btnAksi;

    @Override
    public void refresh() {
        List<Task> myTasks = new ArrayList<>();
        try {
            for (Task task : taskService.getTasks(project.getId())) {
                if (task.isAssignedTo(user.getId())) {
                    myTasks.add(task);
                }
            }
        } catch (SQLException e) {
            AlertUtil.showError(e);
            return;
        }

        // Tugas dikelompokkan per status, yang sedang dikerjakan paling atas
        listTugas.getChildren().clear();
        TaskStatus[] order = {TaskStatus.IN_PROGRESS, TaskStatus.TODO, TaskStatus.DONE};
        Task toSelect = null;
        for (TaskStatus status : order) {
            boolean headerAdded = false;
            for (Task task : myTasks) {
                if (task.getStatus() != status) {
                    continue;
                }
                if (!headerAdded) {
                    Label header = new Label(status.getLabel());
                    header.getStyleClass().add("section-label");
                    listTugas.getChildren().add(header);
                    headerAdded = true;
                }
                listTugas.getChildren().add(createCard(task));
                if (toSelect == null || task.getId() == selectedId) {
                    toSelect = task;
                }
            }
        }

        StyleHelper.setShown(lblKosong, myTasks.isEmpty());
        StyleHelper.setShown(panelDetail, !myTasks.isEmpty());
        if (toSelect != null) {
            select(toSelect);
        }
    }

    private VBox createCard(Task task) {
        Label title = new Label(task.getTitle());
        title.getStyleClass().add("strong");
        title.setWrapText(true);
        Label due = new Label("Tenggat " + StyleHelper.formatShortDate(task.getDueAt()));
        due.getStyleClass().add("caption");
        if (task.isLate()) {
            due.getStyleClass().add("text-overdue");
        }
        VBox card = new VBox(4, title, due);
        if (task.isBlocked()) {
            Label waiting = new Label("Menunggu: " + TaskListController.waitingFor(task));
            waiting.getStyleClass().addAll("caption", "text-blocked");
            waiting.setWrapText(true);
            card.getChildren().add(waiting);
        }
        card.getStyleClass().add("task-card");
        // userData menyimpan id tugas agar kartu yang dipilih bisa ditandai
        card.setUserData(task.getId());
        card.setOnMouseClicked(event -> select(task));
        return card;
    }

    // Mengisi panel detail di sebelah kanan
    private void select(Task task) {
        selected = task;
        selectedId = task.getId();
        for (Node node : listTugas.getChildren()) {
            node.getStyleClass().remove("task-card-selected");
            if (node.getUserData() != null && node.getUserData().equals(selectedId)) {
                node.getStyleClass().add("task-card-selected");
            }
        }

        boxChips.getChildren().setAll(StyleHelper.statusChip(task.getStatus()));
        if (task.isLate()) {
            boxChips.getChildren().add(StyleHelper.chip("Terlambat", "overdue"));
        }
        if (task.isBlocked()) {
            boxChips.getChildren().add(StyleHelper.chip("Terhambat", "blocked"));
        }
        lblJudulTugas.setText(task.getTitle());
        lblDeskripsi.setText(task.getDescription() == null ? "" : task.getDescription());
        lblTenggat.setText(StyleHelper.formatDate(task.getDueAt()));
        lblPrioritas.setText(task.getPriorityLabel());
        lblPenerima.setText(task.getAssigneeNames());

        listPrasyarat.getChildren().clear();
        if (task.getPrerequisites().isEmpty()) {
            Label none = new Label("Tanpa prasyarat");
            none.getStyleClass().add("muted");
            listPrasyarat.getChildren().add(none);
        }
        for (Task prerequisite : task.getPrerequisites()) {
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            HBox row = new HBox(8, new Label(prerequisite.getTitle()), spacer,
                    StyleHelper.statusText(prerequisite.getStatus()));
            row.setAlignment(Pos.CENTER_LEFT);
            row.getStyleClass().add("prereq-row");
            listPrasyarat.getChildren().add(row);
        }

        txtCatatan.setText(task.getNotes());
        if (task.getStatus() == TaskStatus.TODO) {
            btnAksi.setText("Mulai");
        } else if (task.getStatus() == TaskStatus.IN_PROGRESS) {
            btnAksi.setText("Tandai selesai");
        }
        StyleHelper.setShown(btnAksi, task.getStatus() != TaskStatus.DONE);
    }

    @FXML
    private void handleAksi(ActionEvent event) {
        try {
            if (selected.getStatus() == TaskStatus.TODO) {
                taskService.startTask(selected.getId(), user.getId());
            } else {
                taskService.completeTask(selected.getId(), user.getId());
            }
            main.refreshCurrentPage();
        } catch (ValidationException | SQLException e) {
            // Jika prasyarat belum DONE, AlertUtil menampilkan daftar penghambatnya
            AlertUtil.showError(e);
        }
    }

    @FXML
    private void handleSimpanCatatan(ActionEvent event) {
        try {
            taskService.saveNotes(selected.getId(), user.getId(), txtCatatan.getText());
            AlertUtil.showInfo("Catatan disimpan.");
        } catch (ValidationException | SQLException e) {
            AlertUtil.showError(e);
        }
    }
}
