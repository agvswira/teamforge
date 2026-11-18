package org.teamforge.controller;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import org.teamforge.exception.ValidationException;
import org.teamforge.model.ProjectMembership;
import org.teamforge.model.ProjectRole;
import org.teamforge.model.Task;
import org.teamforge.model.TaskStatus;
import org.teamforge.model.User;
import org.teamforge.util.AlertUtil;
import org.teamforge.util.StyleHelper;

/**
 * Halaman "Kelola anggota": CRUD pada tabel relasi project_members.
 */
public class MembersController extends ProjectPageController {
    // Jumlah tugas aktif dan selesai tiap anggota, key = id user
    private final Map<Integer, Integer> activeCount = new HashMap<>();
    private final Map<Integer, Integer> doneCount = new HashMap<>();

    @FXML private Label lblSubjudul;
    @FXML private TextField txtEmail;
    @FXML private ComboBox<ProjectRole> cmbRole;
    @FXML private Label lblPesan;
    @FXML private TableView<ProjectMembership> tblAnggota;
    @FXML private TableColumn<ProjectMembership, ProjectMembership> colAnggota;
    @FXML private TableColumn<ProjectMembership, ProjectMembership> colRole;
    @FXML private TableColumn<ProjectMembership, String> colAktif;
    @FXML private TableColumn<ProjectMembership, String> colSelesai;
    @FXML private TableColumn<ProjectMembership, ProjectMembership> colAksi;

    @FXML
    private void initialize() {
        cmbRole.setItems(FXCollections.observableArrayList(ProjectRole.values()));
        cmbRole.setValue(ProjectRole.MEMBER);

        colAnggota.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue()));
        colRole.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue()));
        colAktif.setCellValueFactory(data ->
                new SimpleStringProperty(String.valueOf(activeCount.get(data.getValue().getUserId()))));
        colSelesai.setCellValueFactory(data ->
                new SimpleStringProperty(String.valueOf(doneCount.get(data.getValue().getUserId()))));
        colAksi.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue()));

        colAnggota.setCellFactory(column -> new MemberCell());
        colRole.setCellFactory(column -> new RoleCell());
        colAksi.setCellFactory(column -> new ActionCell());
    }

    @Override
    public void refresh() {
        try {
            List<ProjectMembership> members = projectService.getMembers(project.getId());
            List<Task> tasks = taskService.getTasks(project.getId());

            activeCount.clear();
            doneCount.clear();
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
                activeCount.put(member.getUserId(), active);
                doneCount.put(member.getUserId(), done);
            }

            lblSubjudul.setText(members.size() + " orang di " + project.getName());
            tblAnggota.setItems(FXCollections.observableArrayList(members));
            tblAnggota.refresh();
        } catch (SQLException e) {
            AlertUtil.showError(e);
        }
    }

    @FXML
    private void handleTambah(ActionEvent event) {
        try {
            User added = projectService.addMember(project.getId(), user.getId(), txtEmail.getText(), cmbRole.getValue());
            showMessage(added.getName() + " ditambahkan sebagai " + cmbRole.getValue() + ".", true);
            txtEmail.clear();
            main.refreshCurrentPage();
        } catch (ValidationException e) {
            showMessage(e.getMessage(), false);
        } catch (SQLException e) {
            AlertUtil.showError(e);
        }
    }

    private void showMessage(String text, boolean success) {
        lblPesan.getStyleClass().removeAll("message-success", "message-error");
        lblPesan.getStyleClass().add(success ? "message-success" : "message-error");
        StyleHelper.showMessage(lblPesan, text);
    }

    private void changeRole(ProjectMembership member, ProjectRole role) {
        try {
            projectService.changeRole(project.getId(), user.getId(), member.getUserId(), role);
        } catch (ValidationException | SQLException e) {
            AlertUtil.showError(e);
        }
        main.refreshCurrentPage();
    }

    private void removeMember(ProjectMembership member) {
        if (!AlertUtil.confirm("Keluarkan " + member.getUser().getName() + " dari project?")) {
            return;
        }
        try {
            projectService.removeMember(project.getId(), user.getId(), member.getUserId());
            main.refreshCurrentPage();
        } catch (ValidationException | SQLException e) {
            AlertUtil.showError(e);
        }
    }

    private boolean isMe(ProjectMembership member) {
        return member.getUserId() == user.getId();
    }

    // Sel tabel

    private class MemberCell extends TableCell<ProjectMembership, ProjectMembership> {
        @Override
        protected void updateItem(ProjectMembership member, boolean empty) {
            super.updateItem(member, empty);
            setText(null);
            if (empty || member == null) {
                setGraphic(null);
                return;
            }
            Label name = new Label(member.getUser().getName() + (isMe(member) ? " (Anda)" : ""));
            name.getStyleClass().add("strong");
            Label email = new Label(member.getUser().getEmail());
            email.getStyleClass().add("caption");
            HBox box = new HBox(10, StyleHelper.avatar(member.getUser().getName()), new VBox(1, name, email));
            box.setAlignment(Pos.CENTER_LEFT);
            setGraphic(box);
        }
    }

    // Role diri sendiri ditampilkan sebagai badge, role anggota lain bisa diubah lewat ComboBox
    private class RoleCell extends TableCell<ProjectMembership, ProjectMembership> {
        @Override
        protected void updateItem(ProjectMembership member, boolean empty) {
            super.updateItem(member, empty);
            setText(null);
            if (empty || member == null) {
                setGraphic(null);
                return;
            }
            if (isMe(member)) {
                setGraphic(StyleHelper.chip(member.getRole().name(), "brand"));
                return;
            }
            ComboBox<ProjectRole> combo = new ComboBox<>(FXCollections.observableArrayList(ProjectRole.values()));
            combo.setValue(member.getRole());
            combo.setOnAction(e -> changeRole(member, combo.getValue()));
            setGraphic(combo);
        }
    }

    // Tombol keluarkan hanya aktif jika anggota tidak punya tugas aktif
    private class ActionCell extends TableCell<ProjectMembership, ProjectMembership> {
        @Override
        protected void updateItem(ProjectMembership member, boolean empty) {
            super.updateItem(member, empty);
            setText(null);
            if (empty || member == null || isMe(member)) {
                setGraphic(null);
                return;
            }
            Button remove = new Button("Keluarkan");
            remove.getStyleClass().addAll("danger", "small");
            remove.setDisable(activeCount.get(member.getUserId()) > 0);
            remove.setOnAction(e -> removeMember(member));
            setGraphic(remove);
        }
    }
}
