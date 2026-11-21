package org.teamforge.controller;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import org.teamforge.exception.ValidationException;
import org.teamforge.model.Task;
import org.teamforge.model.TaskAssignment;
import org.teamforge.model.TaskStatus;
import org.teamforge.util.AlertUtil;
import org.teamforge.util.StyleHelper;

/**
 * Halaman "Kelola tugas" untuk Coordinator, atau "Daftar tugas" (hanya baca) untuk Member.
 * Tugas ditampilkan sebagai kanban board dengan kolom To do, In progress, dan Done.
 */
public class TaskListController extends ProjectPageController {
    private List<Task> allTasks = new ArrayList<>();
    // Alasan tugas tidak boleh dihapus, key = id tugas
    private final Map<Integer, String> deleteBlockers = new HashMap<>();

    @FXML private Label lblJudul;
    @FXML private Label lblSubjudul;
    @FXML private Button btnTugasBaru;
    @FXML private Label lblInfoMember;
    @FXML private TextField txtCari;
    @FXML private Label lblJumlahTodo;
    @FXML private Label lblJumlahProgress;
    @FXML private Label lblJumlahDone;
    @FXML private VBox kolomTodo;
    @FXML private VBox kolomProgress;
    @FXML private VBox kolomDone;

    @FXML
    private void initialize() {
        // Pencarian dijalankan setiap kali teks berubah
        txtCari.textProperty().addListener((obs, oldText, newText) -> showTasks());
    }

    @Override
    public void refresh() {
        try {
            allTasks = taskService.getTasks(project.getId());
            deleteBlockers.clear();
            for (Task task : allTasks) {
                Optional<String> blocker = taskService.getDeleteBlocker(task);
                if (blocker.isPresent()) {
                    deleteBlockers.put(task.getId(), blocker.get());
                }
            }
        } catch (SQLException e) {
            AlertUtil.showError(e);
            return;
        }

        lblSubjudul.setText(allTasks.size() + " tugas di " + project.getName());

        // Member hanya dapat melihat daftar tugas
        if (!isCoordinator()) {
            lblJudul.setText("Daftar tugas");
            StyleHelper.setShown(btnTugasBaru, false);
            StyleHelper.setShown(lblInfoMember, true);
        }
        showTasks();
    }

    // Mengisi ketiga kolom kanban sesuai status dan kata kunci pencarian
    private void showTasks() {
        String keyword = txtCari.getText() == null ? "" : txtCari.getText().trim().toLowerCase();
        List<Task> filtered = allTasks.stream()
                .filter(t -> t.getTitle().toLowerCase().contains(keyword))
                .collect(Collectors.toList());

        fillColumn(kolomTodo, lblJumlahTodo, filtered, TaskStatus.TODO);
        fillColumn(kolomProgress, lblJumlahProgress, filtered, TaskStatus.IN_PROGRESS);
        fillColumn(kolomDone, lblJumlahDone, filtered, TaskStatus.DONE);
    }

    private void fillColumn(VBox column, Label count, List<Task> tasks, TaskStatus status) {
        column.getChildren().clear();
        for (Task task : tasks) {
            if (task.getStatus() == status) {
                column.getChildren().add(createCard(task));
            }
        }
        count.setText(String.valueOf(column.getChildren().size()));
        if (column.getChildren().isEmpty()) {
            Label empty = new Label("Tidak ada tugas");
            empty.getStyleClass().add("kanban-empty");
            empty.setMaxWidth(Double.MAX_VALUE);
            column.getChildren().add(empty);
        }
    }

    // Kartu tugas: judul, keterangan, prioritas, tenggat, penerima, dan tombol aksi
    private VBox createCard(Task task) {
        Label title = new Label(task.getTitle());
        title.getStyleClass().add("strong");
        title.setWrapText(true);

        VBox card = new VBox(6, title);
        card.getStyleClass().add("task-card");

        if (task.isLate()) {
            card.getStyleClass().add("task-card-late");
            Label late = new Label("Lewat tenggat");
            late.getStyleClass().addAll("caption", "text-overdue");
            card.getChildren().add(late);
        } else if (task.isBlocked()) {
            Label waiting = new Label("Menunggu: " + waitingFor(task));
            waiting.getStyleClass().addAll("caption", "text-blocked");
            waiting.setWrapText(true);
            card.getChildren().add(waiting);
        }

        Label due = new Label("Tenggat " + StyleHelper.formatShortDate(task.getDueAt()));
        due.getStyleClass().add("caption");
        Region space = new Region();
        HBox.setHgrow(space, Priority.ALWAYS);
        HBox info = new HBox(8, StyleHelper.priority(task), due, space, StyleHelper.avatarGroup(assigneeNames(task)));
        info.setAlignment(Pos.CENTER_LEFT);
        card.getChildren().add(info);

        if (isCoordinator()) {
            card.getChildren().add(createActions(task));
        }
        return card;
    }

    // Tombol Ubah, lalu Hapus (tugas belum DONE) atau Buka lagi (tugas DONE)
    private HBox createActions(Task task) {
        Button edit = new Button("Ubah");
        edit.getStyleClass().add("small");
        edit.setOnAction(e -> main.openTaskForm(task));

        Button second;
        if (task.getStatus() == TaskStatus.DONE) {
            second = new Button("Buka lagi");
            second.setOnAction(e -> reopenTask(task));
        } else {
            second = new Button("Hapus");
            second.getStyleClass().add("danger");
            second.setDisable(deleteBlockers.containsKey(task.getId()));
            second.setOnAction(e -> deleteTask(task));
        }
        second.getStyleClass().add("small");
        HBox box = new HBox(6, edit, second);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    private List<String> assigneeNames(Task task) {
        List<String> names = new ArrayList<>();
        for (TaskAssignment assignment : task.getAssignments()) {
            names.add(assignment.getUserName());
        }
        return names;
    }

    // Nama prasyarat yang belum DONE, dipisahkan koma
    static String waitingFor(Task task) {
        List<String> names = new ArrayList<>();
        for (Task prerequisite : task.getPrerequisites()) {
            if (prerequisite.getStatus() != TaskStatus.DONE) {
                names.add(prerequisite.getTitle());
            }
        }
        return String.join(", ", names);
    }

    @FXML
    private void handleTugasBaru(ActionEvent event) {
        main.openTaskForm(null);
    }

    private void deleteTask(Task task) {
        if (!AlertUtil.confirm("Hapus tugas \"" + task.getTitle() + "\"?")) {
            return;
        }
        try {
            taskService.deleteTask(task.getId(), user.getId());
            main.refreshCurrentPage();
        } catch (ValidationException | SQLException e) {
            AlertUtil.showError(e);
        }
    }

    private void reopenTask(Task task) {
        try {
            taskService.reopenTask(task.getId(), user.getId());
            main.refreshCurrentPage();
        } catch (ValidationException | SQLException e) {
            AlertUtil.showError(e);
        }
    }
}
