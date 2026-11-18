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
