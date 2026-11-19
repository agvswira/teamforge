package org.teamforge.controller;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.StackPane;

import org.teamforge.App;
import org.teamforge.model.Project;
import org.teamforge.model.ProjectMembership;
import org.teamforge.model.Task;
import org.teamforge.model.User;
import org.teamforge.service.AuthService;
import org.teamforge.service.ProjectService;
import org.teamforge.service.TaskService;
import org.teamforge.util.AlertUtil;
import org.teamforge.util.Session;
import org.teamforge.util.StyleHelper;

/**
 * Kerangka halaman project: sidebar menu di kiri dan halaman aktif di kanan.
 */
public class MainController {
    // Nama file FXML setiap halaman di dalam project
    public static final String DASHBOARD = "DashboardView";
    public static final String DAFTAR_TUGAS = "TaskListView";
    public static final String ANGGOTA = "MembersView";
    public static final String TUGAS_SAYA = "MyTasksView";

    private final ProjectService projectService = new ProjectService();
    private final TaskService taskService = new TaskService();
    private final AuthService authService = new AuthService();

    private Project project;
    private ProjectMembership membership;
    private ProjectPageController currentPage;
    // Controller dialog ubah project yang terakhir dibuka, untuk mengetahui apakah project dihapus
    private ProjectFormController projectForm;

    @FXML private Label lblProjectAktif;
    @FXML private Label lblRole;
    @FXML private ToggleButton navDashboard;
    @FXML private ToggleButton navTugas;
    @FXML private ToggleButton navAnggota;
    @FXML private ToggleButton navTugasSaya;
    @FXML private Label lblNama;
    @FXML private Label lblEmail;
    @FXML private Label lblBreadcrumb;
    @FXML private StackPane paneKonten;

    @FXML
    private void initialize() {
        project = Session.getCurrentProject();
        User user = Session.getCurrentUser();
        lblNama.setText(user.getName());
        lblEmail.setText(user.getEmail());

        try {
            Optional<ProjectMembership> found = projectService.getMembership(project.getId(), user.getId());
            if (found.isEmpty()) {
                // Dijalankan setelah halaman selesai dimuat agar pergantian tampilan tidak bertabrakan
                Platform.runLater(() -> {
                    AlertUtil.showError("Anda sudah tidak menjadi anggota project ini.");
                    openProjectList();
                });
                return;
            }
            membership = found.get();
        } catch (SQLException e) {
            AlertUtil.showError(e);
            return;
        }

        // Member tidak mengelola anggota dan hanya melihat daftar tugas
        if (!membership.canManageProject()) {
            StyleHelper.setShown(navAnggota, false);
        }
        updateSidebar();
        showPage(DASHBOARD);
    }

    // Memperbarui nama project, role, dan jumlah tugas pada menu
    public void updateSidebar() {
        lblProjectAktif.setText(project.getName());
        lblBreadcrumb.setText(project.getName());
        lblRole.setText(membership.getRole().name());
        try {
            List<Task> tasks = taskService.getTasks(project.getId());
            int myTasks = 0;
            for (Task task : tasks) {
                if (task.isAssignedTo(Session.getCurrentUser().getId())) {
                    myTasks++;
                }
            }
            String taskMenu = membership.canManageProject() ? "Kelola tugas" : "Daftar tugas";
            navTugas.setText(taskMenu + " (" + tasks.size() + ")");
            navTugasSaya.setText("Tugas saya (" + myTasks + ")");
        } catch (SQLException e) {
            AlertUtil.showError(e);
        }
    }

    // Memuat file FXML halaman lalu menampilkannya di area konten
    public ProjectPageController showPage(String fxml) {
        try {
            FXMLLoader loader = new FXMLLoader(App.class.getResource("view/" + fxml + ".fxml"));
            Parent view = loader.load();
            ProjectPageController controller = loader.getController();
            controller.setMain(this);
            controller.refresh();
            paneKonten.getChildren().setAll(view);
            currentPage = controller;
            navDashboard.setSelected(fxml.equals(DASHBOARD));
            navTugas.setSelected(fxml.equals(DAFTAR_TUGAS));
            navAnggota.setSelected(fxml.equals(ANGGOTA));
            navTugasSaya.setSelected(fxml.equals(TUGAS_SAYA));
            return controller;
        } catch (IOException e) {
            AlertUtil.showError(e);
            return null;
        }
    }

    public void refreshCurrentPage() {
        updateSidebar();
        if (currentPage != null) {
            currentPage.refresh();
        }
    }

    // task bernilai null berarti membuat tugas baru
    public void openTaskForm(Task task) {
        try {
            String title = task == null ? "Tugas baru" : "Ubah tugas";
            App.<TaskFormController>openDialog("TaskFormView", title, c -> c.setData(project, task));
            refreshCurrentPage();
        } catch (IOException e) {
            AlertUtil.showError(e);
        }
    }

    public void openProjectForm() {
        try {
            App.<ProjectFormController>openDialog("ProjectFormView", "Ubah project", c -> {
                c.setProject(project);
                projectForm = c;
            });
            if (projectForm.isDeleted()) {
                openProjectList();
            } else {
                refreshCurrentPage();
            }
        } catch (IOException e) {
            AlertUtil.showError(e);
        }
    }

    @FXML
    private void handleMenu(ActionEvent event) {
        Object source = event.getSource();
        if (source == navDashboard) {
            showPage(DASHBOARD);
        } else if (source == navTugas) {
            showPage(DAFTAR_TUGAS);
        } else if (source == navAnggota) {
            showPage(ANGGOTA);
        } else if (source == navTugasSaya) {
            showPage(TUGAS_SAYA);
        }
    }

    @FXML
    private void handleDaftarProject(ActionEvent event) {
        openProjectList();
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

    private void openProjectList() {
        Session.setCurrentProject(null);
        try {
            App.setRoot("ProjectListView");
        } catch (IOException e) {
            AlertUtil.showError(e);
        }
    }

    public Project getProject() {
        return project;
    }

    public ProjectMembership getMembership() {
        return membership;
    }
}
