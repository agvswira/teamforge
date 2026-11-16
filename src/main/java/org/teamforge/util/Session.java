package org.teamforge.util;

import org.teamforge.model.Project;
import org.teamforge.model.User;

/**
 * Menyimpan pengguna yang sedang login dan project yang sedang dibuka.
 */
public class Session {
    private static User currentUser;
    private static Project currentProject;

    private Session() {
    }

    public static User getCurrentUser() {
        return currentUser;
    }

    public static void setCurrentUser(User user) {
        currentUser = user;
    }

    public static Project getCurrentProject() {
        return currentProject;
    }

    public static void setCurrentProject(Project project) {
        currentProject = project;
    }

    public static void clear() {
        currentUser = null;
        currentProject = null;
    }
}
