package org.teamforge.model;

/**
 * Objek penghubung many-to-many antara Task dengan Task prasyaratnya (tabel task_dependencies).
 */
public class TaskDependency {
    private final int taskId;
    private final int prerequisiteTaskId;

    public TaskDependency(int taskId, int prerequisiteTaskId) {
        this.taskId = taskId;
        this.prerequisiteTaskId = prerequisiteTaskId;
    }

    public int getTaskId() {
        return taskId;
    }

    public int getPrerequisiteTaskId() {
        return prerequisiteTaskId;
    }
}
