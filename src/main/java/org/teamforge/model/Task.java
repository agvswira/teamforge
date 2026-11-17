package org.teamforge.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.teamforge.exception.ValidationException;

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

    // Status TODO menjadi IN_PROGRESS
    public void start() throws ValidationException {
        if (status != TaskStatus.TODO) {
            throw new ValidationException("Hanya tugas berstatus TODO yang dapat dimulai.");
        }
        status = TaskStatus.IN_PROGRESS;
    }

    // Status IN_PROGRESS menjadi DONE
    public void complete() throws ValidationException {
        if (status != TaskStatus.IN_PROGRESS) {
            throw new ValidationException("Hanya tugas berstatus IN_PROGRESS yang dapat diselesaikan.");
        }
        status = TaskStatus.DONE;
    }

    // Status DONE kembali menjadi TODO
    public void reopen() throws ValidationException {
        if (status != TaskStatus.DONE) {
            throw new ValidationException("Hanya tugas berstatus DONE yang dapat dibuka kembali.");
        }
        status = TaskStatus.TODO;
    }

    public boolean isLate() {
        return isLate(LocalDate.now());
    }

    // Overloading: tanggal pembanding dapat ditentukan sendiri
    public boolean isLate(LocalDate today) {
        return dueAt != null && status != TaskStatus.DONE && dueAt.isBefore(today);
    }

    public boolean isBlocked() {
        return status == TaskStatus.TODO && prerequisites.stream()
                .anyMatch(p -> p.getStatus() != TaskStatus.DONE);
    }

    public boolean isAssignedTo(int userId) {
        return assignments.stream().anyMatch(a -> a.getUserId() == userId);
    }

    public String getPriorityLabel() {
        return switch (priority) {
            case PRIORITY_LOW -> "Rendah";
            case PRIORITY_HIGH -> "Tinggi";
            default -> "Sedang";
        };
    }

    public String getAssigneeNames() {
        if (assignments.isEmpty()) {
            return "-";
        }
        List<String> names = assignments.stream()
                .map(a -> a.getUserName())
                .collect(Collectors.toList());
        return String.join(", ", names);
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
