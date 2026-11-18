package org.teamforge.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.teamforge.model.Task;
import org.teamforge.model.TaskAssignment;
import org.teamforge.model.TaskDependency;
import org.teamforge.model.TaskStatus;
import org.teamforge.util.DatabaseConnection;

/**
 * Query CRUD untuk tabel tasks beserta dua tabel relasinya:
 * task_assignments (users <-> tasks) dan task_dependencies (tasks <-> tasks).
 */
public class TaskDAO implements CrudDAO<Task> {

    // Tabel tasks

    @Override
    public int insert(Task task) throws SQLException {
        String sql = "INSERT INTO tasks (project_id, title, description, status, priority, due_at, notes)"
                + " VALUES (?, ?, ?, ?, ?, ?, ?)";
        Connection conn = DatabaseConnection.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, task.getProjectId());
            ps.setString(2, task.getTitle());
            ps.setString(3, task.getDescription());
            ps.setString(4, task.getStatus().name());
            ps.setInt(5, task.getPriority());
            ps.setObject(6, task.getDueAt());
            ps.setString(7, task.getNotes());
            ps.executeUpdate();

            ResultSet rs = ps.getGeneratedKeys();
            rs.next();
            task.setId(rs.getInt(1));
            return task.getId();
        }
    }

    @Override
    public Optional<Task> findById(int id) throws SQLException {
        String sql = "SELECT * FROM tasks WHERE id = ?";
        Connection conn = DatabaseConnection.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return Optional.of(readTask(rs));
            }
            return Optional.empty();
        }
    }

    public List<Task> findByProject(int projectId) throws SQLException {
        String sql = "SELECT * FROM tasks WHERE project_id = ? ORDER BY due_at IS NULL, due_at, id";
        List<Task> tasks = new ArrayList<>();
        Connection conn = DatabaseConnection.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, projectId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                tasks.add(readTask(rs));
            }
        }
        return tasks;
    }

    @Override
    public void update(Task task) throws SQLException {
        String sql = "UPDATE tasks SET title = ?, description = ?, priority = ?, due_at = ? WHERE id = ?";
        Connection conn = DatabaseConnection.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, task.getTitle());
            ps.setString(2, task.getDescription());
            ps.setInt(3, task.getPriority());
            ps.setObject(4, task.getDueAt());
            ps.setInt(5, task.getId());
            ps.executeUpdate();
        }
    }

    public void updateStatus(Task task) throws SQLException {
        String sql = "UPDATE tasks SET status = ? WHERE id = ?";
        Connection conn = DatabaseConnection.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, task.getStatus().name());
            ps.setInt(2, task.getId());
            ps.executeUpdate();
        }
    }

    public void updateNotes(int taskId, String notes) throws SQLException {
        String sql = "UPDATE tasks SET notes = ? WHERE id = ?";
        Connection conn = DatabaseConnection.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, notes);
            ps.setInt(2, taskId);
            ps.executeUpdate();
        }
    }

    @Override
    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM tasks WHERE id = ?";
        Connection conn = DatabaseConnection.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    // Tabel task_assignments

    public void insertAssignment(int taskId, int userId, String responsibility) throws SQLException {
        String sql = "INSERT INTO task_assignments (task_id, user_id, responsibility) VALUES (?, ?, ?)";
        Connection conn = DatabaseConnection.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, taskId);
            ps.setInt(2, userId);
            ps.setString(3, responsibility);
            ps.executeUpdate();
        }
    }

    public List<TaskAssignment> findAssignments(int taskId) throws SQLException {
        String sql = "SELECT ta.task_id, ta.responsibility, u.* FROM task_assignments ta"
                + " JOIN users u ON u.id = ta.user_id WHERE ta.task_id = ? ORDER BY u.name";
        List<TaskAssignment> assignments = new ArrayList<>();
        Connection conn = DatabaseConnection.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, taskId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                assignments.add(new TaskAssignment(rs.getInt("task_id"), UserDAO.readUser(rs),
                        rs.getString("responsibility")));
            }
        }
        return assignments;
    }

    public void updateResponsibility(int taskId, int userId, String responsibility) throws SQLException {
        String sql = "UPDATE task_assignments SET responsibility = ? WHERE task_id = ? AND user_id = ?";
        Connection conn = DatabaseConnection.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, responsibility);
            ps.setInt(2, taskId);
            ps.setInt(3, userId);
            ps.executeUpdate();
        }
    }

    public void deleteAssignment(int taskId, int userId) throws SQLException {
        String sql = "DELETE FROM task_assignments WHERE task_id = ? AND user_id = ?";
        Connection conn = DatabaseConnection.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, taskId);
            ps.setInt(2, userId);
            ps.executeUpdate();
        }
    }

    // Jumlah tugas belum DONE milik user pada sebuah project
    public int countActiveAssignments(int projectId, int userId) throws SQLException {
        String sql = "SELECT COUNT(*) AS total FROM task_assignments ta"
                + " JOIN tasks t ON t.id = ta.task_id"
                + " WHERE t.project_id = ? AND ta.user_id = ? AND t.status <> 'DONE'";
        Connection conn = DatabaseConnection.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, projectId);
            ps.setInt(2, userId);
            ResultSet rs = ps.executeQuery();
            rs.next();
            return rs.getInt("total");
        }
    }

    // Tabel task_dependencies

    public void insertDependency(int taskId, int prerequisiteTaskId) throws SQLException {
        String sql = "INSERT INTO task_dependencies (task_id, prerequisite_task_id) VALUES (?, ?)";
        Connection conn = DatabaseConnection.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, taskId);
            ps.setInt(2, prerequisiteTaskId);
            ps.executeUpdate();
        }
    }

    // Seluruh hubungan prasyarat dalam satu project
    public List<TaskDependency> findDependencies(int projectId) throws SQLException {
        String sql = "SELECT td.task_id, td.prerequisite_task_id FROM task_dependencies td"
                + " JOIN tasks t ON t.id = td.task_id WHERE t.project_id = ?";
        List<TaskDependency> dependencies = new ArrayList<>();
        Connection conn = DatabaseConnection.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, projectId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                dependencies.add(new TaskDependency(rs.getInt("task_id"), rs.getInt("prerequisite_task_id")));
            }
        }
        return dependencies;
    }

    public void updateDependency(int taskId, int oldPrerequisiteId, int newPrerequisiteId) throws SQLException {
        String sql = "UPDATE task_dependencies SET prerequisite_task_id = ?"
                + " WHERE task_id = ? AND prerequisite_task_id = ?";
        Connection conn = DatabaseConnection.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, newPrerequisiteId);
            ps.setInt(2, taskId);
            ps.setInt(3, oldPrerequisiteId);
            ps.executeUpdate();
        }
    }

    public void deleteDependency(int taskId, int prerequisiteTaskId) throws SQLException {
        String sql = "DELETE FROM task_dependencies WHERE task_id = ? AND prerequisite_task_id = ?";
        Connection conn = DatabaseConnection.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, taskId);
            ps.setInt(2, prerequisiteTaskId);
            ps.executeUpdate();
        }
    }

    // Jumlah tugas lain yang menjadikan tugas ini sebagai prasyarat
    public int countDependents(int taskId) throws SQLException {
        String sql = "SELECT COUNT(*) AS total FROM task_dependencies WHERE prerequisite_task_id = ?";
        Connection conn = DatabaseConnection.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, taskId);
            ResultSet rs = ps.executeQuery();
            rs.next();
            return rs.getInt("total");
        }
    }

    private Task readTask(ResultSet rs) throws SQLException {
        return new Task(
                rs.getInt("id"),
                rs.getInt("project_id"),
                rs.getString("title"),
                rs.getString("description"),
                TaskStatus.valueOf(rs.getString("status")),
                rs.getInt("priority"),
                rs.getObject("due_at", LocalDate.class),
                rs.getString("notes"));
    }
}
