package org.teamforge.model;

public class MemberMembership extends ProjectMembership {

    public MemberMembership(int projectId, User user) {
        super(projectId, user, ProjectRole.MEMBER);
    }

    @Override
    public String getRoleName() {
        return "Member";
    }

    @Override
    public boolean canManageProject() {
        return false;
    }
}
