package org.teamforge.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class Project extends BaseEntity {
    private String name;
    private String description;
    private LocalDate deadline;
    private final List<Task> tasks = new ArrayList<>();

    public Project(String name, String description, LocalDate deadline) {
        this(0, name, description, deadline);
    }

    public Project(int id, String name, String description, LocalDate deadline) {
        super(id);
        this.name = name;
        this.description = description;
        this.deadline = deadline;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDate getDeadline() {
        return deadline;
    }

    public void setDeadline(LocalDate deadline) {
        this.deadline = deadline;
    }

    public List<Task> getTasks() {
        return tasks;
    }

    public void addTask(Task task) {
        tasks.add(task);
    }

    public void removeTask(Task task) {
        tasks.remove(task);
    }

    // Persentase tugas DONE dari seluruh tugas, 0 jika belum ada tugas
    public double getProgress() {
        if (tasks.isEmpty()) {
            return 0;
        }
        long selesai = tasks.stream()
                .filter(t -> t.getStatus() == TaskStatus.DONE)
                .count();
        return (double) selesai / tasks.size() * 100;
    }

    public boolean isOverdue() {
        return deadline != null && deadline.isBefore(LocalDate.now()) && getProgress() < 100;
    }

    @Override
    public String getDisplayName() {
        return name;
    }
}
