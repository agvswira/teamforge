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

import org.teamforge.model.Project;
import org.teamforge.util.DatabaseConnection;

/**
 * Query CRUD untuk tabel projects.
 */
public class ProjectDAO implements CrudDAO<Project> {

    @Override
    public int insert(Project project) throws SQLException {
        String sql = "INSERT INTO projects (name, description, deadline) VALUES (?, ?, ?)";
        Connection conn = DatabaseConnection.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, project.getName());
            ps.setString(2, project.getDescription());
            ps.setObject(3, project.getDeadline());
            ps.executeUpdate();

            ResultSet rs = ps.getGeneratedKeys();
            rs.next();
            project.setId(rs.getInt(1));
            return project.getId();
        }
    }

    @Override
    public Optional<Project> findById(int id) throws SQLException {
        String sql = "SELECT * FROM projects WHERE id = ?";
        Connection conn = DatabaseConnection.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return Optional.of(readProject(rs));
            }
            return Optional.empty();
        }
    }

    // Project yang diikuti user, dibaca melalui tabel relasi project_members
    public List<Project> findByUser(int userId) throws SQLException {
        String sql = "SELECT p.* FROM projects p"
                + " JOIN project_members pm ON pm.project_id = p.id"
                + " WHERE pm.user_id = ? ORDER BY p.deadline";
        List<Project> projects = new ArrayList<>();
        Connection conn = DatabaseConnection.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                projects.add(readProject(rs));
            }
        }
        return projects;
    }

    @Override
    public void update(Project project) throws SQLException {
        String sql = "UPDATE projects SET name = ?, description = ?, deadline = ? WHERE id = ?";
        Connection conn = DatabaseConnection.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, project.getName());
            ps.setString(2, project.getDescription());
            ps.setObject(3, project.getDeadline());
            ps.setInt(4, project.getId());
            ps.executeUpdate();
        }
    }

    @Override
    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM projects WHERE id = ?";
        Connection conn = DatabaseConnection.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    private Project readProject(ResultSet rs) throws SQLException {
        return new Project(
                rs.getInt("id"),
                rs.getString("name"),
                rs.getString("description"),
                rs.getObject("deadline", LocalDate.class));
    }
}
