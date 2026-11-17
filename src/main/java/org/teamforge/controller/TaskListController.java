package org.teamforge.controller;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import org.teamforge.exception.ValidationException;
import org.teamforge.model.Task;
import org.teamforge.model.TaskAssignment;
import org.teamforge.model.TaskStatus;
import org.teamforge.util.AlertUtil;
import org.teamforge.util.StyleHelper;

/**
 * Halaman "Kelola tugas" untuk Coordinator, atau "Daftar tugas" (hanya baca) untuk Member.
 */
public class TaskListController extends ProjectPageController {
    private List<Task> allTasks = new ArrayList<>();
    // Alasan tugas tidak boleh dihapus, key = id tugas
    private final Map<Integer, String> deleteBlockers = new HashMap<>();

    @FXML private Label lblJudul;
    @FXML private Label lblSubjudul;
    @FXML private Button btnTugasBaru;
    @FXML private Label lblInfoMember;
    @FXML private ToggleButton tabSemua;
    @FXML private ToggleButton tabTodo;
    @FXML private ToggleButton tabProgress;
    @FXML private ToggleButton tabDone;
    @FXML private TextField txtCari;
    @FXML private TableView<Task> tblTugas;
    @FXML private TableColumn<Task, Task> colTugas;
    @FXML private TableColumn<Task, Task> colStatus;
    @FXML private TableColumn<Task, Task> colPrioritas;
    @FXML private TableColumn<Task, String> colTenggat;
    @FXML private TableColumn<Task, Task> colPenerima;
    @FXML private TableColumn<Task, Task> colAksi;

    @FXML
    private void initialize() {
        colTugas.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue()));
        colStatus.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue()));
        colPrioritas.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue()));
        colTenggat.setCellValueFactory(data ->
                new SimpleStringProperty(StyleHelper.formatShortDate(data.getValue().getDueAt())));
        colPenerima.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue()));
        colAksi.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue()));

        colTugas.setCellFactory(column -> new TitleCell());
        colStatus.setCellFactory(column -> new StatusCell());
        colPrioritas.setCellFactory(column -> new PriorityCell());
        colPenerima.setCellFactory(column -> new AssigneeCell());
        colAksi.setCellFactory(column -> new ActionCell());

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

        tabSemua.setText("Semua " + allTasks.size());
        tabTodo.setText("To do " + taskService.countByStatus(allTasks, TaskStatus.TODO));
        tabProgress.setText("In progress " + taskService.countByStatus(allTasks, TaskStatus.IN_PROGRESS));
        tabDone.setText("Done " + taskService.countByStatus(allTasks, TaskStatus.DONE));
        lblSubjudul.setText(allTasks.size() + " tugas di " + project.getName());

        // Member hanya dapat melihat daftar tugas
        if (!isCoordinator()) {
            lblJudul.setText("Daftar tugas");
            StyleHelper.setShown(btnTugasBaru, false);
            StyleHelper.setShown(lblInfoMember, true);
            colAksi.setVisible(false);
        }
        showTasks();
    }

    // Menampilkan tugas sesuai tab status dan kata kunci pencarian
    private void showTasks() {
        String keyword = txtCari.getText() == null ? "" : txtCari.getText().trim().toLowerCase();
        List<Task> filtered = allTasks.stream()
                .filter(t -> t.getTitle().toLowerCase().contains(keyword))
                .filter(t -> matchesTab(t))
                .collect(Collectors.toList());
        tblTugas.setItems(FXCollections.observableArrayList(filtered));
    }

    private boolean matchesTab(Task task) {
        if (tabTodo.isSelected()) {
            return task.getStatus() == TaskStatus.TODO;
        }
        if (tabProgress.isSelected()) {
            return task.getStatus() == TaskStatus.IN_PROGRESS;
        }
        if (tabDone.isSelected()) {
            return task.getStatus() == TaskStatus.DONE;
        }
        return true;
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
    private void handleFilter(ActionEvent event) {
        // Satu tab harus selalu terpilih
        if (!tabSemua.isSelected() && !tabTodo.isSelected() && !tabProgress.isSelected() && !tabDone.isSelected()) {
            tabSemua.setSelected(true);
        }
        showTasks();
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

    // Sel tabel

    // Judul tugas, di bawahnya keterangan terlambat atau menunggu prasyarat
    private class TitleCell extends TableCell<Task, Task> {
        @Override
        protected void updateItem(Task task, boolean empty) {
            super.updateItem(task, empty);
            setText(null);
            if (empty || task == null) {
                setGraphic(null);
                return;
            }
            Label title = new Label(task.getTitle());
            title.getStyleClass().add("strong");
            VBox box = new VBox(2, title);
            box.setAlignment(Pos.CENTER_LEFT);
            if (task.isLate()) {
                Label late = new Label("Lewat tenggat");
                late.getStyleClass().addAll("caption", "text-overdue");
                box.getChildren().add(late);
            } else if (task.isBlocked()) {
                Label waiting = new Label("Menunggu: " + waitingFor(task));
                waiting.getStyleClass().addAll("caption", "text-blocked");
                box.getChildren().add(waiting);
            }
            setGraphic(box);
        }
    }

    private class StatusCell extends TableCell<Task, Task> {
        @Override
        protected void updateItem(Task task, boolean empty) {
            super.updateItem(task, empty);
            setText(null);
            setGraphic(empty || task == null ? null : StyleHelper.statusText(task.getStatus()));
        }
    }

    private class PriorityCell extends TableCell<Task, Task> {
        @Override
        protected void updateItem(Task task, boolean empty) {
            super.updateItem(task, empty);
            setText(null);
            setGraphic(empty || task == null ? null : StyleHelper.priority(task));
        }
    }

    // Avatar setiap penerima tugas
    private class AssigneeCell extends TableCell<Task, Task> {
        @Override
        protected void updateItem(Task task, boolean empty) {
            super.updateItem(task, empty);
            setText(null);
            if (empty || task == null) {
                setGraphic(null);
                return;
            }
            List<String> names = new ArrayList<>();
            for (TaskAssignment assignment : task.getAssignments()) {
                names.add(assignment.getUserName());
            }
            setGraphic(StyleHelper.avatarGroup(names));
        }
    }

    // Tombol Ubah, lalu Hapus (tugas belum DONE) atau Buka lagi (tugas DONE)
    private class ActionCell extends TableCell<Task, Task> {
        @Override
        protected void updateItem(Task task, boolean empty) {
            super.updateItem(task, empty);
            setText(null);
            if (empty || task == null) {
                setGraphic(null);
                return;
            }
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
            setGraphic(box);
        }
    }
}
