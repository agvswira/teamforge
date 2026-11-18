package org.teamforge.service;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.teamforge.dao.TaskDAO;
import org.teamforge.exception.ValidationException;
import org.teamforge.model.Task;
import org.teamforge.model.TaskDependency;
import org.teamforge.model.TaskStatus;

/**
 * Aturan untuk tugas, penerima tugas (task_assignments), dan prasyarat (task_dependencies).
 */
public class TaskService {
    private final TaskDAO taskDAO = new TaskDAO();
    private final ProjectService projectService = new ProjectService();

    // Membaca data

    // Semua tugas project lengkap dengan penerima dan prasyaratnya
    public List<Task> getTasks(int projectId) throws SQLException {
        List<Task> tasks = taskDAO.findByProject(projectId);

        // Simpan tugas ke HashMap agar mudah dicari berdasarkan id
        Map<Integer, Task> taskById = new HashMap<>();
        for (Task task : tasks) {
            taskById.put(task.getId(), task);
            task.getAssignments().addAll(taskDAO.findAssignments(task.getId()));
        }

        for (TaskDependency dependency : taskDAO.findDependencies(projectId)) {
            Task task = taskById.get(dependency.getTaskId());
            Task prerequisite = taskById.get(dependency.getPrerequisiteTaskId());
            if (task != null && prerequisite != null) {
                task.getPrerequisites().add(prerequisite);
            }
        }
        return tasks;
    }

    public Task getTask(int taskId) throws SQLException, ValidationException {
        Optional<Task> found = taskDAO.findById(taskId);
        if (found.isEmpty()) {
            throw new ValidationException("Tugas tidak ditemukan.");
        }
        // Dicari dari daftar tugas project agar penerima dan prasyaratnya ikut terisi
        for (Task task : getTasks(found.get().getProjectId())) {
            if (task.getId() == taskId) {
                return task;
            }
        }
        return found.get();
    }

    public List<Task> getLateTasks(List<Task> tasks) {
        return tasks.stream()
                .filter(t -> t.isLate())
                .collect(Collectors.toList());
    }

    public List<Task> getBlockedTasks(List<Task> tasks) {
        return tasks.stream()
                .filter(t -> t.isBlocked())
                .collect(Collectors.toList());
    }

    public long countByStatus(List<Task> tasks, TaskStatus status) {
        return tasks.stream()
                .filter(t -> t.getStatus() == status)
                .count();
    }

}
