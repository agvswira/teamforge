package org.teamforge.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.teamforge.model.ProjectMembership;
import org.teamforge.model.ProjectRole;
import org.teamforge.model.User;
import org.teamforge.util.DatabaseConnection;

/**
 * Query CRUD untuk tabel relasi many-to-many project_members (users <-> projects).
 */
public class MembershipDAO {
    private static final String SELECT_WITH_USER = "SELECT pm.project_id, pm.role, u.*"
            + " FROM project_members pm JOIN users u ON u.id = pm.user_id";

    // CREATE
    public void insert(int projectId, int userId, ProjectRole role) throws SQLException {
        String sql = "INSERT INTO project_members (project_id, user_id, role) VALUES (?, ?, ?)";
        Connection conn = DatabaseConnection.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, projectId);
            ps.setInt(2, userId);
            ps.setString(3, role.name());
            ps.executeUpdate();
        }
    }

    // READ: seluruh anggota sebuah project
    public List<ProjectMembership> findByProject(int projectId) throws SQLException {
        String sql = SELECT_WITH_USER + " WHERE pm.project_id = ? ORDER BY pm.role, u.name";
        List<ProjectMembership> members = new ArrayList<>();
        Connection conn = DatabaseConnection.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, projectId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                members.add(readMembership(rs));
            }
        }
        return members;
    }

    // READ: membership satu user pada satu project
    public Optional<ProjectMembership> find(int projectId, int userId) throws SQLException {
        String sql = SELECT_WITH_USER + " WHERE pm.project_id = ? AND pm.user_id = ?";
        Connection conn = DatabaseConnection.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, projectId);
            ps.setInt(2, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return Optional.of(readMembership(rs));
            }
            return Optional.empty();
        }
    }

    // UPDATE
    public void updateRole(int projectId, int userId, ProjectRole role) throws SQLException {
        String sql = "UPDATE project_members SET role = ? WHERE project_id = ? AND user_id = ?";
        Connection conn = DatabaseConnection.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, role.name());
            ps.setInt(2, projectId);
            ps.setInt(3, userId);
            ps.executeUpdate();
        }
    }

    // DELETE
    public void delete(int projectId, int userId) throws SQLException {
        String sql = "DELETE FROM project_members WHERE project_id = ? AND user_id = ?";
        Connection conn = DatabaseConnection.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, projectId);
            ps.setInt(2, userId);
            ps.executeUpdate();
        }
    }

    public int countCoordinators(int projectId) throws SQLException {
        String sql = "SELECT COUNT(*) AS total FROM project_members WHERE project_id = ? AND role = 'COORDINATOR'";
        Connection conn = DatabaseConnection.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, projectId);
            ResultSet rs = ps.executeQuery();
            rs.next();
            return rs.getInt("total");
        }
    }

    // Membuat objek CoordinatorMembership atau MemberMembership sesuai kolom role
    private ProjectMembership readMembership(ResultSet rs) throws SQLException {
        User user = UserDAO.readUser(rs);
        ProjectRole role = ProjectRole.valueOf(rs.getString("role"));
        return ProjectMembership.of(rs.getInt("project_id"), user, role);
    }
}
