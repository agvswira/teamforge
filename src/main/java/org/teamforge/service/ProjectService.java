package org.teamforge.service;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.teamforge.dao.MembershipDAO;
import org.teamforge.dao.ProjectDAO;
import org.teamforge.dao.TaskDAO;
import org.teamforge.dao.UserDAO;
import org.teamforge.exception.ValidationException;
import org.teamforge.model.Project;
import org.teamforge.model.ProjectMembership;
import org.teamforge.model.ProjectRole;
import org.teamforge.model.User;
import org.teamforge.util.DatabaseConnection;

/**
 * Aturan untuk project dan anggotanya (tabel projects dan project_members).
 */
public class ProjectService {
    private final ProjectDAO projectDAO = new ProjectDAO();
    private final MembershipDAO membershipDAO = new MembershipDAO();
    private final UserDAO userDAO = new UserDAO();
    private final TaskDAO taskDAO = new TaskDAO();

    // Membaca data

    public List<Project> getProjectsForUser(int userId) throws SQLException {
        return projectDAO.findByUser(userId);
    }

    public Optional<ProjectMembership> getMembership(int projectId, int userId) throws SQLException {
        return membershipDAO.find(projectId, userId);
    }

    public List<ProjectMembership> getMembers(int projectId) throws SQLException {
        return membershipDAO.findByProject(projectId);
    }

    // Pemeriksaan hak akses

    public ProjectMembership requireMembership(int projectId, int userId) throws SQLException, ValidationException {
        Optional<ProjectMembership> found = membershipDAO.find(projectId, userId);
        if (found.isEmpty()) {
            throw new ValidationException("Anda bukan anggota project ini.");
        }
        return found.get();
    }

    // Polimorfisme: hasil canManageProject() bergantung pada jenis membership
    public ProjectMembership requireCoordinator(int projectId, int userId) throws SQLException, ValidationException {
        ProjectMembership membership = requireMembership(projectId, userId);
        if (!membership.canManageProject()) {
            throw new ValidationException("Tindakan ini hanya dapat dilakukan oleh Coordinator.");
        }
        return membership;
    }

    // CRUD project

    // Menyimpan project dan menjadikan pembuatnya COORDINATOR dalam satu transaksi
    public Project createProject(String name, String description, LocalDate deadline, User creator)
            throws SQLException, ValidationException {
        checkProjectInput(name, deadline);
        if (deadline.isBefore(LocalDate.now())) {
            throw new ValidationException("Tenggat tidak boleh sebelum hari ini.");
        }
        Project project = new Project(name.trim(), description, deadline);

        Connection conn = DatabaseConnection.getConnection();
        try {
            conn.setAutoCommit(false);
            projectDAO.insert(project);
            membershipDAO.insert(project.getId(), creator.getId(), ProjectRole.COORDINATOR);
            conn.commit();
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
        return project;
    }

    public void updateProject(Project project, String name, String description, LocalDate deadline, int actorId)
            throws SQLException, ValidationException {
        requireCoordinator(project.getId(), actorId);
        checkProjectInput(name, deadline);
        project.setName(name.trim());
        project.setDescription(description);
        project.setDeadline(deadline);
        projectDAO.update(project);
    }

    // Hanya project tanpa tugas yang boleh dihapus
    public void deleteProject(int projectId, int actorId) throws SQLException, ValidationException {
        requireCoordinator(projectId, actorId);
        if (!taskDAO.findByProject(projectId).isEmpty()) {
            throw new ValidationException("Project masih memiliki tugas. Hapus semua tugas terlebih dahulu.");
        }
        projectDAO.delete(projectId);
    }

    private void checkProjectInput(String name, LocalDate deadline) throws ValidationException {
        if (name == null || name.isBlank()) {
            throw new ValidationException("Nama project wajib diisi.");
        }
        if (deadline == null) {
            throw new ValidationException("Tenggat wajib diisi.");
        }
    }
}
