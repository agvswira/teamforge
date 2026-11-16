package org.teamforge.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class Task extends BaseEntity {
    public static final int PRIORITY_LOW = 1;
    public static final int PRIORITY_MEDIUM = 2;
    public static final int PRIORITY_HIGH = 3;

    private int projectId;
    private String title;
    private String description;
    private TaskStatus status;
    private int priority;
    private LocalDate dueAt;
    private String notes;

    // Diisi oleh service saat detail tugas dibutuhkan
    private final List<TaskAssignment> assignments = new ArrayList<>();
    private final List<Task> prerequisites = new ArrayList<>();

    public Task(int projectId, String title, String description, int priority, LocalDate dueAt) {
        this(0, projectId, title, description, TaskStatus.TODO, priority, dueAt, null);
    }

    public Task(int id, int projectId, String title, String description, TaskStatus status,
                int priority, LocalDate dueAt, String notes) {
        super(id);
        this.projectId = projectId;
        this.title = title;
        this.description = description;
        this.status = status;
        this.priority = priority;
        this.dueAt = dueAt;
        this.notes = notes;
    }

    public int getProjectId() {
        return projectId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public LocalDate getDueAt() {
        return dueAt;
    }

    public void setDueAt(LocalDate dueAt) {
        this.dueAt = dueAt;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public List<TaskAssignment> getAssignments() {
        return assignments;
    }

    public List<Task> getPrerequisites() {
        return prerequisites;
    }

    @Override
    public String getDisplayName() {
        return title;
    }
}
