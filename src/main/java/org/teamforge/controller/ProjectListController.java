package org.teamforge.controller;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Separator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;

import org.teamforge.App;
import org.teamforge.model.Project;
import org.teamforge.model.ProjectMembership;
import org.teamforge.model.Task;
import org.teamforge.model.TaskStatus;
import org.teamforge.model.User;
import org.teamforge.service.AuthService;
import org.teamforge.service.ProjectService;
import org.teamforge.service.TaskService;
import org.teamforge.util.AlertUtil;
import org.teamforge.util.Session;
import org.teamforge.util.StyleHelper;

/**
 * Halaman "Project saya": kartu semua project yang diikuti pengguna.
 */
public class ProjectListController {
    private final ProjectService projectService = new ProjectService();
    private final TaskService taskService = new TaskService();
    private final AuthService authService = new AuthService();

    @FXML private Label lblNama;
    @FXML private Label lblEmail;
    @FXML private Label lblSubjudul;
    @FXML private TilePane paneProject;

    @FXML
    private void initialize() {
        User user = Session.getCurrentUser();
        lblNama.setText(user.getName());
        lblEmail.setText(user.getEmail());
        loadData();
    }

    private void loadData() {
        try {
            List<Project> projects = projectService.getProjectsForUser(Session.getCurrentUser().getId());
            lblSubjudul.setText(projects.size() + " project · role Anda bisa berbeda di setiap project");
            paneProject.getChildren().clear();
            for (Project project : projects) {
                paneProject.getChildren().add(createCard(project));
            }
        } catch (SQLException e) {
            AlertUtil.showError(e);
        }
    }

    private VBox createCard(Project project) throws SQLException {
        List<Task> tasks = taskService.getTasks(project.getId());
        List<ProjectMembership> members = projectService.getMembers(project.getId());
        ProjectMembership membership = projectService.getMembership(project.getId(),
                Session.getCurrentUser().getId()).get();

        Label name = new Label(project.getName());
        name.getStyleClass().add("card-title");
        name.setWrapText(true);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        String variant = membership.canManageProject() ? "brand" : "neutral";
        HBox header = new HBox(10, name, spacer, StyleHelper.chip(membership.getRole().name(), variant));

        Label description = new Label(project.getDescription());
        description.getStyleClass().add("muted");
        description.setWrapText(true);

        // Progres dihitung oleh method getProgress() milik Project
        project.getTasks().clear();
        for (Task task : tasks) {
            project.addTask(task);
        }
        long done = taskService.countByStatus(tasks, TaskStatus.DONE);
        Label progress = new Label(Math.round(project.getProgress()) + "% · " + done + " dari "
                + tasks.size() + " tugas DONE");
        progress.getStyleClass().add("caption");
        ProgressBar bar = new ProgressBar(project.getProgress() / 100);
        bar.setMaxWidth(Double.MAX_VALUE);

        VBox card = new VBox(10, header, description, progress, bar);

        // Badge hanya muncul jika ada tugas terlambat atau terhambat
        int late = taskService.getLateTasks(tasks).size();
        int blocked = taskService.getBlockedTasks(tasks).size();
        if (late > 0 || blocked > 0) {
            HBox chips = new HBox(6);
            if (late > 0) {
                chips.getChildren().add(StyleHelper.chip(late + " terlambat", "overdue"));
            }
            if (blocked > 0) {
                chips.getChildren().add(StyleHelper.chip(blocked + " terhambat", "blocked"));
            }
            card.getChildren().add(chips);
        }

        List<String> names = new ArrayList<>();
        for (ProjectMembership member : members) {
            names.add(member.getUser().getName());
        }
        Region footerSpacer = new Region();
        HBox.setHgrow(footerSpacer, Priority.ALWAYS);
        Label deadline = new Label("Tenggat " + StyleHelper.formatDate(project.getDeadline()));
        deadline.getStyleClass().add("caption");
        HBox footer = new HBox(StyleHelper.avatarGroup(names), footerSpacer, deadline);
        footer.setAlignment(Pos.CENTER_LEFT);
        // Ruang kosong agar garis pemisah dan footer selalu menempel di bawah kartu
        Region cardSpacer = new Region();
        VBox.setVgrow(cardSpacer, Priority.ALWAYS);
        card.getChildren().addAll(cardSpacer, new Separator(), footer);

        card.getStyleClass().add("project-card");
        card.setPrefWidth(300);
        card.setOnMouseClicked(event -> openProject(project));
        return card;
    }

    @FXML
    private void handleBuatProject(ActionEvent event) {
        try {
            App.<ProjectFormController>openDialog("ProjectFormView", "Buat project", c -> c.setProject(null));
            loadData();
        } catch (IOException e) {
            AlertUtil.showError(e);
        }
    }

    @FXML
    private void handleAkun(ActionEvent event) {
        try {
            App.<AccountController>openDialog("AccountView", "Akun saya", c -> { });
            // Jika akun dihapus, Session sudah kosong dan pengguna kembali ke halaman login
            if (Session.getCurrentUser() == null) {
                App.setRoot("LoginView");
            } else {
                lblNama.setText(Session.getCurrentUser().getName());
                lblEmail.setText(Session.getCurrentUser().getEmail());
            }
        } catch (IOException e) {
            AlertUtil.showError(e);
        }
    }

    @FXML
    private void handleKeluar(ActionEvent event) {
        authService.logout();
        try {
            App.setRoot("LoginView");
        } catch (IOException e) {
            AlertUtil.showError(e);
        }
    }

    private void openProject(Project project) {
        Session.setCurrentProject(project);
        try {
            App.setRoot("MainView");
        } catch (IOException e) {
            AlertUtil.showError(e);
        }
    }
}
