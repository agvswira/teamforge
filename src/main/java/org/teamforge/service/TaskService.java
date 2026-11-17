package org.teamforge.service;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.teamforge.exception.AccessDeniedException;
import org.teamforge.exception.DependencyException;
import org.teamforge.exception.TeamForgeException;
import org.teamforge.exception.ValidationException;
import org.teamforge.model.MemberContribution;
import org.teamforge.model.ProjectMembership;
import org.teamforge.model.Task;
import org.teamforge.model.TaskAssignment;
import org.teamforge.model.TaskDependency;
import org.teamforge.model.TaskStatus;
import org.teamforge.dao.TaskDAO;

public class TaskService extends BaseService {
    private final TaskDAO taskDAO = new TaskDAO();

    // ===================== membaca data =====================

    // Semua tugas project lengkap dengan penerima dan prasyaratnya
    public List<Task> getTasks(int projectId) throws SQLException {
        List<Task> tasks = taskDAO.findByProject(projectId);
        Map<Integer, Task> taskById = tasks.stream()
                .collect(Collectors.toMap(Task::getId, Function.identity()));

        for (Task task : tasks) {
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
        Task task = taskDAO.findById(taskId)
                .orElseThrow(() -> new ValidationException("Tugas tidak ditemukan."));
        task.getAssignments().addAll(taskDAO.findAssignments(taskId));
        task.getPrerequisites().addAll(taskDAO.findPrerequisites(taskId));
        return task;
    }

    public List<Task> getMyTasks(int userId) throws SQLException {
        return taskDAO.findByAssignee(userId);
    }

    public List<Task> getLateTasks(List<Task> tasks) {
        return tasks.stream().filter(Task::isLate).toList();
    }

    public List<Task> getBlockedTasks(List<Task> tasks) {
        return tasks.stream().filter(Task::isBlocked).toList();
    }

    public long countByStatus(List<Task> tasks, TaskStatus status) {
        return tasks.stream().filter(t -> t.getStatus() == status).count();
    }

    public List<MemberContribution> getContributions(List<ProjectMembership> members, List<Task> tasks) {
        return members.stream()
                .map(m -> new MemberContribution(
                        m.getUser().getName(),
                        m.getRoleName(),
                        tasks.stream().filter(t -> t.isAssignedTo(m.getUserId())).count(),
                        tasks.stream()
                                .filter(t -> t.isAssignedTo(m.getUserId()))
                                .filter(t -> t.getStatus() == TaskStatus.DONE)
                                .count()))
                .sorted((a, b) -> Long.compare(b.assigned(), a.assigned()))
                .toList();
    }

    // ===================== CRUD tugas (Coordinator) =====================

    public Task createTask(int projectId, int actorId, String title, String description, int priority,
                           LocalDate dueAt, List<Integer> assigneeIds, List<Integer> prerequisiteIds)
            throws SQLException, TeamForgeException {
        requireManager(projectId, actorId);
        Task task = new Task(projectId, requireText(title, "Judul tugas"), description, priority, dueAt);

        runInTransaction(() -> {
            taskDAO.insert(task);
            for (int userId : assigneeIds) {
                requireAssignableMember(projectId, userId);
                taskDAO.insertAssignment(task.getId(), userId, null);
            }
            for (int prerequisiteId : prerequisiteIds) {
                validateDependency(task, prerequisiteId);
                taskDAO.insertDependency(task.getId(), prerequisiteId);
            }
        });
        return task;
    }

    public void updateTask(int taskId, int actorId, String title, String description, int priority,
                           LocalDate dueAt, List<Integer> assigneeIds, List<Integer> prerequisiteIds)
            throws SQLException, TeamForgeException {
        Task task = getTask(taskId);
        requireManager(task.getProjectId(), actorId);
        task.setTitle(requireText(title, "Judul tugas"));
        task.setDescription(description);
        task.setPriority(priority);
        task.setDueAt(dueAt);

        Set<Integer> oldAssignees = task.getAssignments().stream()
                .map(TaskAssignment::getUserId).collect(Collectors.toSet());
        Set<Integer> oldPrerequisites = task.getPrerequisites().stream()
                .map(Task::getId).collect(Collectors.toSet());

        runInTransaction(() -> {
            taskDAO.update(task);

            for (int userId : oldAssignees) {
                if (!assigneeIds.contains(userId)) {
                    requireTodo(task, "Penerima hanya dapat dilepas dari tugas berstatus TODO.");
                    taskDAO.deleteAssignment(taskId, userId);
                }
            }
            for (int userId : assigneeIds) {
                if (!oldAssignees.contains(userId)) {
                    requireAssignableMember(task.getProjectId(), userId);
                    taskDAO.insertAssignment(taskId, userId, null);
                }
            }

            for (int prerequisiteId : oldPrerequisites) {
                if (!prerequisiteIds.contains(prerequisiteId)) {
                    taskDAO.deleteDependency(taskId, prerequisiteId);
                }
            }
            for (int prerequisiteId : prerequisiteIds) {
                if (!oldPrerequisites.contains(prerequisiteId)) {
                    validateDependency(task, prerequisiteId);
                    taskDAO.insertDependency(taskId, prerequisiteId);
                }
            }
        });
    }

    // Hanya tugas TODO yang tidak menjadi prasyarat tugas lain yang boleh dihapus
    public void deleteTask(int taskId, int actorId) throws SQLException, TeamForgeException {
        Task task = getTask(taskId);
        requireManager(task.getProjectId(), actorId);
        requireTodo(task, "Tugas tidak dapat dihapus karena statusnya " + task.getStatus()
                + ". Hanya tugas TODO yang dapat dihapus.");
        int dependents = taskDAO.countDependents(taskId);
        if (dependents > 0) {
            throw new ValidationException("Tugas tidak dapat dihapus karena menjadi prasyarat bagi "
                    + dependents + " tugas lain.");
        }
        taskDAO.delete(taskId);
    }

    // ===================== penerima tugas (task_assignments) =====================

    public void changeResponsibility(int taskId, int actorId, int userId, String responsibility)
            throws SQLException, TeamForgeException {
        Task task = getTask(taskId);
        requireManager(task.getProjectId(), actorId);
        if (!task.isAssignedTo(userId)) {
            throw new ValidationException("Pengguna tersebut bukan penerima tugas ini.");
        }
        String value = responsibility == null || responsibility.isBlank() ? null : responsibility.trim();
        taskDAO.updateResponsibility(taskId, userId, value);
    }

    public void removeAssignee(int taskId, int actorId, int userId) throws SQLException, TeamForgeException {
        Task task = getTask(taskId);
        requireManager(task.getProjectId(), actorId);
        requireTodo(task, "Penerima hanya dapat dilepas dari tugas berstatus TODO.");
        taskDAO.deleteAssignment(taskId, userId);
    }

    // ===================== prasyarat (task_dependencies) =====================

    public void replacePrerequisite(int taskId, int actorId, int oldPrerequisiteId, int newPrerequisiteId)
            throws SQLException, TeamForgeException {
        Task task = getTask(taskId);
        requireManager(task.getProjectId(), actorId);
        if (oldPrerequisiteId == newPrerequisiteId) {
            return;
        }
        boolean alreadyUsed = task.getPrerequisites().stream().anyMatch(p -> p.getId() == newPrerequisiteId);
        if (alreadyUsed) {
            throw new DependencyException("Tugas tersebut sudah menjadi prasyarat.");
        }
        validateDependency(task, newPrerequisiteId, oldPrerequisiteId);
        taskDAO.updateDependency(taskId, oldPrerequisiteId, newPrerequisiteId);
    }

    public void removePrerequisite(int taskId, int actorId, int prerequisiteId) throws SQLException, TeamForgeException {
        Task task = getTask(taskId);
        requireManager(task.getProjectId(), actorId);
        taskDAO.deleteDependency(taskId, prerequisiteId);
    }

    // ===================== mengerjakan tugas (Coordinator / Member) =====================

    public void startTask(int taskId, int actorId) throws SQLException, TeamForgeException {
        Task task = getTask(taskId);
        requireAssignee(task, actorId);
        List<Task> blockers = task.getPrerequisites().stream()
                .filter(p -> p.getStatus() != TaskStatus.DONE)
                .toList();
        if (!blockers.isEmpty()) {
            throw new DependencyException("Tugas belum dapat dimulai karena prasyarat berikut belum DONE.", blockers);
        }
        task.start();
        taskDAO.updateStatus(task);
    }

    public void completeTask(int taskId, int actorId) throws SQLException, TeamForgeException {
        Task task = getTask(taskId);
        requireAssignee(task, actorId);
        task.complete();
        taskDAO.updateStatus(task);
    }

    public void reopenTask(int taskId, int actorId) throws SQLException, TeamForgeException {
        Task task = getTask(taskId);
        requireManager(task.getProjectId(), actorId);
        task.reopen();
        taskDAO.updateStatus(task);
    }

    public void saveNotes(int taskId, int actorId, String notes) throws SQLException, TeamForgeException {
        Task task = getTask(taskId);
        requireAssignee(task, actorId);
        taskDAO.updateNotes(taskId, notes);
    }

    // ===================== aturan bantu =====================

    private void requireAssignee(Task task, int userId) throws SQLException, AccessDeniedException {
        requireMembership(task.getProjectId(), userId);
        if (!task.isAssignedTo(userId)) {
            throw new AccessDeniedException("Tugas ini tidak ditugaskan kepada Anda.");
        }
    }

    private void requireAssignableMember(int projectId, int userId) throws SQLException, ValidationException {
        if (membershipDAO.find(projectId, userId).isEmpty()) {
            throw new ValidationException("Penerima tugas harus anggota project.");
        }
    }

    private void requireTodo(Task task, String message) throws ValidationException {
        if (task.getStatus() != TaskStatus.TODO) {
            throw new ValidationException(message);
        }
    }

    private void validateDependency(Task task, int prerequisiteId) throws SQLException, TeamForgeException {
        validateDependency(task, prerequisiteId, -1);
    }

    // ignoredPrerequisiteId: hubungan lama yang sedang diganti, tidak ikut diperiksa
    private void validateDependency(Task task, int prerequisiteId, int ignoredPrerequisiteId)
            throws SQLException, TeamForgeException {
        if (prerequisiteId == task.getId()) {
            throw new DependencyException("Tugas tidak dapat menjadi prasyarat bagi dirinya sendiri.");
        }
        Task prerequisite = taskDAO.findById(prerequisiteId)
                .orElseThrow(() -> new ValidationException("Tugas prasyarat tidak ditemukan."));
        if (prerequisite.getProjectId() != task.getProjectId()) {
            throw new DependencyException("Prasyarat harus berasal dari project yang sama.");
        }

        // Graf: id tugas -> daftar id prasyaratnya
        Map<Integer, List<Integer>> graph = new HashMap<>();
        for (TaskDependency d : taskDAO.findDependencies(task.getProjectId())) {
            boolean ignored = d.getTaskId() == task.getId() && d.getPrerequisiteTaskId() == ignoredPrerequisiteId;
            if (!ignored) {
                graph.computeIfAbsent(d.getTaskId(), k -> new ArrayList<>()).add(d.getPrerequisiteTaskId());
            }
        }
        if (isReachable(graph, prerequisiteId, task.getId())) {
            throw new DependencyException("Dependency ditolak karena akan membentuk siklus: \""
                    + prerequisite.getTitle() + "\" sudah (secara langsung atau tidak langsung) menunggu \""
                    + task.getTitle() + "\".");
        }
    }

    // Menelusuri prasyarat mulai dari start; true jika sampai ke target
    private boolean isReachable(Map<Integer, List<Integer>> graph, int start, int target) {
        Deque<Integer> stack = new ArrayDeque<>();
        Set<Integer> visited = new HashSet<>();
        stack.push(start);
        while (!stack.isEmpty()) {
            int current = stack.pop();
            if (current == target) {
                return true;
            }
            if (visited.add(current)) {
                for (int next : graph.getOrDefault(current, List.of())) {
                    stack.push(next);
                }
            }
        }
        return false;
    }
}
