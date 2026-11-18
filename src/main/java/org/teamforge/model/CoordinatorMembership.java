package org.teamforge.model;

public class CoordinatorMembership extends ProjectMembership {

    public CoordinatorMembership(int projectId, User user) {
        super(projectId, user, ProjectRole.COORDINATOR);
    }

    @Override
    public String getRoleName() {
        return "Coordinator";
    }

    @Override
    public boolean canManageProject() {
        return true;
    }
}
