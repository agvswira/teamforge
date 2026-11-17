package org.teamforge.controller;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import org.teamforge.model.ProjectMembership;
import org.teamforge.model.Task;
import org.teamforge.model.TaskStatus;
import org.teamforge.util.AlertUtil;
import org.teamforge.util.StyleHelper;

/**
 * Halaman Dashboard: ringkasan angka, tugas yang perlu perhatian, dan kontribusi anggota.
 */
public class DashboardController extends ProjectPageController {

    @FXML private Label lblJudul;
    @FXML private Label lblSubjudul;
    @FXML private Button btnUbahProject;
    @FXML private Button btnTugasBaru;
    @FXML private Label lblProgres;
    @FXML private ProgressBar pbProgres;
    @FXML private Label lblInProgress;
    @FXML private Label lblTerlambat;
    @FXML private Label lblTerhambat;
    @FXML private VBox listPerhatian;
    @FXML private VBox listAnggota;
    @FXML private Hyperlink linkKelolaAnggota;

    @Override
    public void refresh() {
        try {
            List<Task> tasks = taskService.getTasks(project.getId());
            showHeader();
            showStats(tasks);
            showAttention(tasks);
            showMembers(projectService.getMembers(project.getId()), tasks);
        } catch (SQLException e) {
            AlertUtil.showError(e);
        }
    }

    private void showHeader() {
        lblJudul.setText(project.getName());
        lblSubjudul.setText(project.getDescription() + " · Tenggat "
                + StyleHelper.formatDate(project.getDeadline()));
        StyleHelper.setShown(btnUbahProject, isCoordinator());
        StyleHelper.setShown(btnTugasBaru, isCoordinator());
        StyleHelper.setShown(linkKelolaAnggota, isCoordinator());
    }

    private void showStats(List<Task> tasks) {
        long done = taskService.countByStatus(tasks, TaskStatus.DONE);
        double progress = tasks.isEmpty() ? 0 : (double) done / tasks.size();
        lblProgres.setText(Math.round(progress * 100) + "%");
        pbProgres.setProgress(progress);
        lblInProgress.setText(String.valueOf(taskService.countByStatus(tasks, TaskStatus.IN_PROGRESS)));
        lblTerlambat.setText(String.valueOf(taskService.getLateTasks(tasks).size()));
        lblTerhambat.setText(String.valueOf(taskService.getBlockedTasks(tasks).size()));
    }

    // Daftar tugas yang terlambat atau masih menunggu prasyarat
    private void showAttention(List<Task> tasks) {
        listPerhatian.getChildren().clear();
        List<Task> rows = new ArrayList<>(taskService.getLateTasks(tasks));
        for (Task task : taskService.getBlockedTasks(tasks)) {
            if (!rows.contains(task)) {
                rows.add(task);
            }
        }
        if (rows.isEmpty()) {
            Label empty = new Label("Tidak ada tugas yang terlambat atau terhambat.");
            empty.getStyleClass().addAll("muted", "list-row");
            listPerhatian.getChildren().add(empty);
        }
        for (Task task : rows) {
            listPerhatian.getChildren().add(createAttentionRow(task));
        }
    }

    private HBox createAttentionRow(Task task) {
        HBox state;
        String detail;
        if (task.isLate()) {
            state = StyleHelper.dotText("Terlambat", "overdue");
            detail = "Lewat tenggat · " + task.getAssigneeNames();
        } else {
            state = StyleHelper.dotText("Terhambat", "blocked");
            detail = "Menunggu: " + TaskListController.waitingFor(task);
        }
        state.setMinWidth(100);

        Label title = new Label(task.getTitle());
        title.getStyleClass().add("strong");
        Label sub = new Label(detail);
        sub.getStyleClass().add("caption");
        VBox text = new VBox(2, title, sub);
        HBox.setHgrow(text, Priority.ALWAYS);

        Label due = new Label(StyleHelper.formatShortDate(task.getDueAt()));
        if (task.isLate()) {
            due.getStyleClass().add("text-overdue");
        }

        HBox row = new HBox(12, state, text, due);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("list-row");
        return row;
    }

    // Kontribusi: jumlah tugas aktif dan selesai milik tiap anggota
    private void showMembers(List<ProjectMembership> members, List<Task> tasks) {
        listAnggota.getChildren().clear();
        for (ProjectMembership member : members) {
            int active = 0;
            int done = 0;
            for (Task task : tasks) {
                if (task.isAssignedTo(member.getUserId())) {
                    if (task.getStatus() == TaskStatus.DONE) {
                        done++;
                    } else {
                        active++;
                    }
                }
            }
            Label name = new Label(member.getUser().getName());
            name.getStyleClass().add("strong");
            String variant = member.canManageProject() ? "brand" : "neutral";
            HBox top = new HBox(8, name, StyleHelper.chip(member.getRole().name(), variant));
            top.setAlignment(Pos.CENTER_LEFT);
            Label info = new Label(active + " aktif · " + done + " selesai");
            info.getStyleClass().add("caption");
            HBox row = new HBox(10, StyleHelper.avatar(member.getUser().getName()), new VBox(2, top, info));
            row.setAlignment(Pos.CENTER_LEFT);
            listAnggota.getChildren().add(row);
        }
    }

    @FXML
    private void handleUbahProject(ActionEvent event) {
        main.openProjectForm();
    }

    @FXML
    private void handleTugasBaru(ActionEvent event) {
        main.openTaskForm(null);
    }

    @FXML
    private void handleLihatTugas(ActionEvent event) {
        main.showPage(MainController.DAFTAR_TUGAS);
    }

    @FXML
    private void handleKelolaAnggota(ActionEvent event) {
        main.showPage(MainController.ANGGOTA);
    }
}
