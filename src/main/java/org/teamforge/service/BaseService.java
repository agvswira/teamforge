package org.teamforge.service;

import java.sql.Connection;
import java.sql.SQLException;

import org.teamforge.exception.AccessDeniedException;
import org.teamforge.exception.TeamForgeException;
import org.teamforge.exception.ValidationException;
import org.teamforge.model.ProjectMembership;
import org.teamforge.dao.MembershipDAO;
import org.teamforge.util.DatabaseConnection;

/**
 * Bagian yang dipakai bersama oleh semua service: transaksi JDBC dan pemeriksaan hak akses.
 */
public abstract class BaseService {
    protected final MembershipDAO membershipDAO = new MembershipDAO();

    protected void runInTransaction(TransactionAction action) throws SQLException, TeamForgeException {
        Connection conn = DatabaseConnection.getConnection();
        conn.setAutoCommit(false);
        try {
            action.execute();
            conn.commit();
        } catch (SQLException | TeamForgeException | RuntimeException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
    }

    protected ProjectMembership requireMembership(int projectId, int userId)
            throws SQLException, AccessDeniedException {
        return membershipDAO.find(projectId, userId)
                .orElseThrow(() -> new AccessDeniedException("Anda bukan anggota project ini."));
    }

    // Polimorfisme: hasil canManageProject() bergantung pada jenis membership
    protected ProjectMembership requireManager(int projectId, int userId)
            throws SQLException, AccessDeniedException {
        ProjectMembership membership = requireMembership(projectId, userId);
        if (!membership.canManageProject()) {
            throw new AccessDeniedException("Tindakan ini hanya dapat dilakukan oleh Coordinator.");
        }
        return membership;
    }

    protected String requireText(String value, String fieldName) throws ValidationException {
        if (value == null || value.isBlank()) {
            throw new ValidationException(fieldName + " wajib diisi.");
        }
        return value.trim();
    }
}
