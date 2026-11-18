package org.teamforge.model;

/**
 * Objek penghubung many-to-many antara User dan Project (tabel project_members).
 * Hak akses ditentukan oleh subclass, bukan oleh akun User.
 */
public abstract class ProjectMembership {
    private final int projectId;
    private final User user;
    private final ProjectRole role;

    protected ProjectMembership(int projectId, User user, ProjectRole role) {
        this.projectId = projectId;
        this.user = user;
        this.role = role;
    }

    // Membuat objek membership sesuai role yang tersimpan di database
    public static ProjectMembership of(int projectId, User user, ProjectRole role) {
        if (role == ProjectRole.COORDINATOR) {
            return new CoordinatorMembership(projectId, user);
        }
        return new MemberMembership(projectId, user);
    }

    public abstract String getRoleName();

    public abstract boolean canManageProject();

    public int getProjectId() {
        return projectId;
    }

    public int getUserId() {
        return user.getId();
    }

    public User getUser() {
        return user;
    }

    public ProjectRole getRole() {
        return role;
    }

    @Override
    public String toString() {
        return user.getName() + " (" + getRoleName() + ")";
    }
}
