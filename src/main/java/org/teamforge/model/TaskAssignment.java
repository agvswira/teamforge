package org.teamforge.model;

/**
 * Objek penghubung many-to-many antara Task dan User (tabel task_assignments).
 */
public class TaskAssignment {
    private final int taskId;
    private final User user;

    public TaskAssignment(int taskId, User user) {
        this.taskId = taskId;
        this.user = user;
    }

    public int getTaskId() {
        return taskId;
    }

    public int getUserId() {
        return user.getId();
    }

    public String getUserName() {
        return user.getName();
    }

    public User getUser() {
        return user;
    }
}
