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
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
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

        card.getChildren().add(createActions(task));
        return card;
    }

    // Tombol Detail, Ubah, dan Hapus di kiri, tombol pindah status (Mulai / Selesai / Buka lagi) di kanan.
    // Member hanya mendapat tombol Detail.
    private HBox createActions(Task task) {
        Button view = new Button("Detail");
        view.getStyleClass().add("small");
        view.setOnAction(e -> showDetail(task));
        HBox box = new HBox(6, view);
        box.setAlignment(Pos.CENTER_LEFT);
        if (!isCoordinator()) {
            return box;
        }

        Button edit = new Button("Ubah");
        edit.getStyleClass().add("small");
        edit.setOnAction(e -> main.openTaskForm(task));
        box.getChildren().add(edit);

        if (task.getStatus() == TaskStatus.TODO) {
            Button delete = new Button("Hapus");
            delete.getStyleClass().addAll("small", "danger");
            delete.setDisable(deleteBlockers.containsKey(task.getId()));
            delete.setOnAction(e -> deleteTask(task));
            box.getChildren().add(delete);
        }

        Region space = new Region();
        HBox.setHgrow(space, Priority.ALWAYS);
        box.getChildren().addAll(space, createStatusButton(task));
        return box;
    }

    // Coordinator dapat memindahkan tugas siapa pun ke kolom berikutnya
    private Button createStatusButton(Task task) {
        Button button = new Button();
        button.getStyleClass().add("small");
        switch (task.getStatus()) {
            case TODO -> {
                button.setText("Mulai");
                button.getStyleClass().add("primary");
                button.setDisable(task.isBlocked());
                button.setOnAction(e -> startTask(task));
            }
            case IN_PROGRESS -> {
                button.setText("Selesai");
                button.getStyleClass().add("primary");
                button.setOnAction(e -> completeTask(task));
            }
            case DONE -> {
                button.setText("Buka lagi");
                button.setOnAction(e -> reopenTask(task));
            }
        }
        return button;
    }

    // Detail tugas beserta catatan tugas dan hasil.
    // Coordinator dapat mengubah dan menyimpan catatan, Member hanya membaca.
    private void showDetail(Task task) {
        Label title = new Label(task.getTitle());
        title.getStyleClass().add("h2");
        title.setWrapText(true);

        GridPane info = new GridPane();
        info.setHgap(16);
        info.setVgap(6);
        info.addRow(0, mutedLabel("Status"), StyleHelper.statusText(task.getStatus()));
        info.addRow(1, mutedLabel("Prioritas"), StyleHelper.priority(task));
        info.addRow(2, mutedLabel("Tenggat"), new Label(StyleHelper.formatDate(task.getDueAt())));
        info.addRow(3, mutedLabel("Penerima"), new Label(String.join(", ", assigneeNames(task))));

        VBox content = new VBox(8, title);
        if (task.getDescription() != null && !task.getDescription().isBlank()) {
            Label description = new Label(task.getDescription());
            description.setWrapText(true);
            content.getChildren().add(description);
        }

        Label notesLabel = new Label("Catatan tugas dan hasil");
        notesLabel.getStyleClass().add("section-label");
        TextArea notes = new TextArea(task.getNotes());
        notes.setWrapText(true);
        notes.setPrefRowCount(5);
        notes.setPromptText("Belum ada catatan");
        notes.setEditable(isCoordinator());
        content.getChildren().addAll(info, notesLabel, notes);
        content.setPrefWidth(460);

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Detail tugas");
        dialog.getDialogPane().setContent(content);
        AlertUtil.applyStyle(dialog);
        ButtonType close = new ButtonType("Tutup", ButtonBar.ButtonData.CANCEL_CLOSE);
        ButtonType save = new ButtonType("Simpan catatan", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().add(close);
        if (isCoordinator()) {
            dialog.getDialogPane().getButtonTypes().add(save);
        }

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == save) {
            saveNotes(task, notes.getText());
        }
    }

    private Label mutedLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("muted");
        return label;
    }

    private void saveNotes(Task task, String notes) {
        try {
            taskService.saveNotes(task.getId(), user.getId(), notes);
            AlertUtil.showInfo("Catatan disimpan.");
            main.refreshCurrentPage();
        } catch (ValidationException | SQLException e) {
            AlertUtil.showError(e);
        }
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

    private void startTask(Task task) {
        try {
            taskService.startTask(task.getId(), user.getId());
            main.refreshCurrentPage();
        } catch (ValidationException | SQLException e) {
            AlertUtil.showError(e);
        }
    }

    private void completeTask(Task task) {
        try {
            taskService.completeTask(task.getId(), user.getId());
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
