package org.teamforge.service;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.teamforge.exception.AccessDeniedException;
import org.teamforge.exception.TeamForgeException;
import org.teamforge.exception.ValidationException;
import org.teamforge.model.Project;
import org.teamforge.model.ProjectMembership;
import org.teamforge.model.ProjectRole;
import org.teamforge.model.Task;
import org.teamforge.model.User;
import org.teamforge.dao.ProjectDAO;
import org.teamforge.dao.TaskDAO;
import org.teamforge.dao.UserDAO;

public class ProjectService extends BaseService {
    private final ProjectDAO projectDAO = new ProjectDAO();
    private final TaskDAO taskDAO = new TaskDAO();
    private final UserDAO userDAO = new UserDAO();

    // Project yang diikuti user beserta tugasnya, agar progres dapat dihitung
    public List<Project> getProjectsForUser(int userId) throws SQLException {
        List<Project> projects = projectDAO.findByUser(userId);
        for (Project project : projects) {
            for (Task task : taskDAO.findByProject(project.getId())) {
                project.addTask(task);
            }
        }
        return projects;
    }

    public Optional<ProjectMembership> getMembership(int projectId, int userId) throws SQLException {
        return membershipDAO.find(projectId, userId);
    }

    // Menyimpan project dan menjadikan pembuatnya COORDINATOR dalam satu transaksi
    public Project createProject(String name, String description, LocalDate deadline, User creator)
            throws SQLException, TeamForgeException {
        String cleanName = requireText(name, "Nama project");
        validateDeadline(deadline);
        if (deadline.isBefore(LocalDate.now())) {
            throw new ValidationException("Deadline tidak boleh sebelum hari ini.");
        }
        Project project = new Project(cleanName, description, deadline);
        runInTransaction(() -> {
            projectDAO.insert(project);
            membershipDAO.insert(project.getId(), creator.getId(), ProjectRole.COORDINATOR);
        });
        return project;
    }

    public void updateProject(Project project, String name, String description, LocalDate deadline, int actorId)
            throws SQLException, TeamForgeException {
        requireManager(project.getId(), actorId);
        String cleanName = requireText(name, "Nama project");
        validateDeadline(deadline);
        project.setName(cleanName);
        project.setDescription(description);
        project.setDeadline(deadline);
        projectDAO.update(project);
    }

    // Hanya project kosong (tanpa tugas) yang boleh dihapus
    public void deleteProject(int projectId, int actorId) throws SQLException, TeamForgeException {
        requireManager(projectId, actorId);
        if (!taskDAO.findByProject(projectId).isEmpty()) {
            throw new ValidationException("Project masih memiliki tugas. Hapus semua tugas terlebih dahulu.");
        }
        projectDAO.delete(projectId);
    }

    public List<ProjectMembership> getMembers(int projectId) throws SQLException {
        return membershipDAO.findByProject(projectId);
    }

    public void addMember(int projectId, int actorId, String email, ProjectRole role)
            throws SQLException, TeamForgeException {
        requireManager(projectId, actorId);
        String cleanEmail = requireText(email, "Email").toLowerCase();
        User user = userDAO.findByEmail(cleanEmail)
                .orElseThrow(() -> new ValidationException("Tidak ada pengguna terdaftar dengan email " + cleanEmail + "."));
        if (!user.isActive()) {
            throw new ValidationException("Akun pengguna tersebut tidak aktif.");
        }
        if (membershipDAO.find(projectId, user.getId()).isPresent()) {
            throw new ValidationException(user.getName() + " sudah menjadi anggota project ini.");
        }
        membershipDAO.insert(projectId, user.getId(), role);
    }

    public void changeRole(int projectId, int actorId, int targetUserId, ProjectRole newRole)
            throws SQLException, TeamForgeException {
        requireManager(projectId, actorId);
        ProjectMembership target = requireMembership(projectId, targetUserId);
        if (target.getRole() == newRole) {
            return;
        }
        if (target.getRole() == ProjectRole.COORDINATOR && membershipDAO.countCoordinators(projectId) == 1) {
            throw new ValidationException("Project harus memiliki minimal satu Coordinator.");
        }
        membershipDAO.updateRole(projectId, targetUserId, newRole);
    }

    public void removeMember(int projectId, int actorId, int targetUserId) throws SQLException, TeamForgeException {
        requireManager(projectId, actorId);
        ProjectMembership target = membershipDAO.find(projectId, targetUserId)
                .orElseThrow(() -> new AccessDeniedException("Pengguna tersebut bukan anggota project."));
        if (target.getRole() == ProjectRole.COORDINATOR && membershipDAO.countCoordinators(projectId) == 1) {
            throw new ValidationException("Coordinator terakhir tidak dapat dikeluarkan dari project.");
        }
        int activeTasks = taskDAO.countActiveAssignments(projectId, targetUserId);
        if (activeTasks > 0) {
            throw new ValidationException(target.getUser().getName() + " masih memiliki " + activeTasks
                    + " tugas aktif. Lepas atau selesaikan tugas tersebut terlebih dahulu.");
        }
        // Penugasan pada tugas DONE tetap disimpan sebagai riwayat
        membershipDAO.delete(projectId, targetUserId);
    }

    public int countActiveTasks(int projectId, int userId) throws SQLException {
        return taskDAO.countActiveAssignments(projectId, userId);
    }

    private void validateDeadline(LocalDate deadline) throws ValidationException {
        if (deadline == null) {
            throw new ValidationException("Deadline wajib diisi.");
        }
    }
}
