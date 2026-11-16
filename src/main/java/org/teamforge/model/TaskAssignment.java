package org.teamforge.model;

/**
 * Objek penghubung many-to-many antara Task dan User (tabel task_assignments).
 */
public class TaskAssignment {
    private final int taskId;
    private final User user;
    private String responsibility;

    public TaskAssignment(int taskId, User user, String responsibility) {
        this.taskId = taskId;
        this.user = user;
        this.responsibility = responsibility;
    }

    public void changeResponsibility(String responsibility) {
        this.responsibility = responsibility;
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

    public String getResponsibility() {
        return responsibility;
    }
}
