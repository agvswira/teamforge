package org.teamforge.service;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.teamforge.dao.TaskDAO;
import org.teamforge.exception.DependencyException;
import org.teamforge.exception.ValidationException;
import org.teamforge.model.ProjectMembership;
import org.teamforge.model.Task;
import org.teamforge.model.TaskAssignment;
import org.teamforge.model.TaskDependency;
import org.teamforge.model.TaskStatus;
import org.teamforge.util.DatabaseConnection;

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

    // CRUD tugas (Coordinator)

    public Task createTask(int projectId, int actorId, String title, String description, int priority,
                           LocalDate dueAt, List<Integer> assigneeIds, List<Integer> prerequisiteIds)
            throws SQLException, ValidationException {
        projectService.requireCoordinator(projectId, actorId);
        checkTitle(title);
        Task task = new Task(projectId, title.trim(), description, priority, dueAt);

        // Semua pemeriksaan dilakukan sebelum data disimpan
        for (int userId : assigneeIds) {
            checkMember(projectId, userId);
        }
        for (int prerequisiteId : prerequisiteIds) {
            validateDependency(task, prerequisiteId);
        }

        Connection conn = DatabaseConnection.getConnection();
        try {
            conn.setAutoCommit(false);
            taskDAO.insert(task);
            for (int userId : assigneeIds) {
                taskDAO.insertAssignment(task.getId(), userId);
            }
            for (int prerequisiteId : prerequisiteIds) {
                taskDAO.insertDependency(task.getId(), prerequisiteId);
            }
            conn.commit();
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
        return task;
    }

    public void updateTask(int taskId, int actorId, String title, String description, int priority,
                           LocalDate dueAt, List<Integer> assigneeIds, List<Integer> prerequisiteIds)
            throws SQLException, ValidationException {
        Task task = getTask(taskId);
        projectService.requireCoordinator(task.getProjectId(), actorId);
        checkTitle(title);

        List<Integer> oldAssignees = new ArrayList<>();
        for (TaskAssignment assignment : task.getAssignments()) {
            oldAssignees.add(assignment.getUserId());
        }
        List<Integer> oldPrerequisites = new ArrayList<>();
        for (Task prerequisite : task.getPrerequisites()) {
            oldPrerequisites.add(prerequisite.getId());
        }

        for (int userId : oldAssignees) {
            if (!assigneeIds.contains(userId) && task.getStatus() != TaskStatus.TODO) {
                throw new ValidationException("Penerima hanya dapat dilepas dari tugas berstatus TODO.");
            }
        }
        for (int userId : assigneeIds) {
            if (!oldAssignees.contains(userId)) {
                checkMember(task.getProjectId(), userId);
            }
        }

        // Memisahkan prasyarat yang dilepas dan yang baru
        List<Integer> removed = new ArrayList<>();
        for (int id : oldPrerequisites) {
            if (!prerequisiteIds.contains(id)) {
                removed.add(id);
            }
        }
        List<Integer> added = new ArrayList<>();
        for (int id : prerequisiteIds) {
            if (!oldPrerequisites.contains(id)) {
                added.add(id);
            }
        }
        // Prasyarat yang dilepas lalu diganti pada penyimpanan yang sama disimpan dengan UPDATE
        int replaced = Math.min(removed.size(), added.size());
        for (int i = 0; i < added.size(); i++) {
            if (i < replaced) {
                validateDependency(task, added.get(i), removed.get(i));
            } else {
                validateDependency(task, added.get(i));
            }
        }

        task.setTitle(title.trim());
        task.setDescription(description);
        task.setPriority(priority);
        task.setDueAt(dueAt);

        Connection conn = DatabaseConnection.getConnection();
        try {
            conn.setAutoCommit(false);
            taskDAO.update(task);

            // task_assignments: DELETE penerima yang dilepas, INSERT penerima baru
            for (int userId : oldAssignees) {
                if (!assigneeIds.contains(userId)) {
                    taskDAO.deleteAssignment(taskId, userId);
                }
            }
            for (int userId : assigneeIds) {
                if (!oldAssignees.contains(userId)) {
                    taskDAO.insertAssignment(taskId, userId);
                }
            }

            // task_dependencies: UPDATE, DELETE, atau INSERT
            for (int i = 0; i < replaced; i++) {
                taskDAO.updateDependency(taskId, removed.get(i), added.get(i));
            }
            for (int i = replaced; i < removed.size(); i++) {
                taskDAO.deleteDependency(taskId, removed.get(i));
            }
            for (int i = replaced; i < added.size(); i++) {
                taskDAO.insertDependency(taskId, added.get(i));
            }
            conn.commit();
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
    }

    // Mengembalikan alasan tugas tidak boleh dihapus, atau Optional kosong jika boleh dihapus
    public Optional<String> getDeleteBlocker(Task task) throws SQLException {
        if (task.getStatus() != TaskStatus.TODO) {
            return Optional.of("Hanya tugas TODO yang dapat dihapus.");
        }
        int dependents = taskDAO.countDependents(task.getId());
        if (dependents > 0) {
            return Optional.of("Menjadi prasyarat bagi " + dependents + " tugas lain.");
        }
        return Optional.empty();
    }

    public void deleteTask(int taskId, int actorId) throws SQLException, ValidationException {
        Task task = getTask(taskId);
        projectService.requireCoordinator(task.getProjectId(), actorId);
        Optional<String> blocker = getDeleteBlocker(task);
        if (blocker.isPresent()) {
            throw new ValidationException("Tugas tidak dapat dihapus. " + blocker.get());
        }
        taskDAO.delete(taskId);
    }

    // Dipakai formulir tugas untuk memeriksa prasyarat sebelum disimpan
    public void checkPrerequisite(Task task, int prerequisiteId) throws SQLException, ValidationException {
        validateDependency(task, prerequisiteId);
    }

    // Mengerjakan tugas (Coordinator / Member)

    public void startTask(int taskId, int actorId) throws SQLException, ValidationException {
        Task task = getTask(taskId);
        checkWorker(task, actorId);
        List<Task> blockers = task.getPrerequisites().stream()
                .filter(p -> p.getStatus() != TaskStatus.DONE)
                .collect(Collectors.toList());
        if (!blockers.isEmpty()) {
            throw new DependencyException("Masih ada " + blockers.size()
                    + " prasyarat yang belum DONE. Tugas baru bisa dimulai setelah semua prasyarat selesai.", blockers);
        }
        task.start();
        taskDAO.updateStatus(task);
    }

    public void completeTask(int taskId, int actorId) throws SQLException, ValidationException {
        Task task = getTask(taskId);
        checkWorker(task, actorId);
        task.complete();
        taskDAO.updateStatus(task);
    }

    public void reopenTask(int taskId, int actorId) throws SQLException, ValidationException {
        Task task = getTask(taskId);
        projectService.requireCoordinator(task.getProjectId(), actorId);
        task.reopen();
        taskDAO.updateStatus(task);
    }

    public void saveNotes(int taskId, int actorId, String notes) throws SQLException, ValidationException {
        Task task = getTask(taskId);
        checkAssignee(task, actorId);
        taskDAO.updateNotes(taskId, notes);
    }

    // Aturan bantu

    private void checkTitle(String title) throws ValidationException {
        if (title == null || title.isBlank()) {
            throw new ValidationException("Judul tugas wajib diisi.");
        }
    }

    private void checkAssignee(Task task, int userId) throws SQLException, ValidationException {
        projectService.requireMembership(task.getProjectId(), userId);
        if (!task.isAssignedTo(userId)) {
            throw new ValidationException("Tugas ini tidak ditugaskan kepada Anda.");
        }
    }

    // Status tugas boleh diubah oleh penerima tugas, atau oleh Coordinator untuk tugas siapa pun
    private void checkWorker(Task task, int userId) throws SQLException, ValidationException {
        ProjectMembership membership = projectService.requireMembership(task.getProjectId(), userId);
        if (!membership.canManageProject() && !task.isAssignedTo(userId)) {
            throw new ValidationException("Tugas ini tidak ditugaskan kepada Anda.");
        }
    }

    private void checkMember(int projectId, int userId) throws SQLException, ValidationException {
        if (projectService.getMembership(projectId, userId).isEmpty()) {
            throw new ValidationException("Penerima tugas harus anggota project.");
        }
    }

    private void validateDependency(Task task, int prerequisiteId) throws SQLException, ValidationException {
        validateDependency(task, prerequisiteId, -1);
    }

    // Overloading: ignoredPrerequisiteId adalah prasyarat lama yang sedang diganti, tidak ikut diperiksa
    private void validateDependency(Task task, int prerequisiteId, int ignoredPrerequisiteId)
            throws SQLException, ValidationException {
        if (prerequisiteId == task.getId()) {
            throw new DependencyException("Tugas tidak dapat menjadi prasyarat bagi dirinya sendiri.");
        }
        Optional<Task> found = taskDAO.findById(prerequisiteId);
        if (found.isEmpty()) {
            throw new ValidationException("Tugas prasyarat tidak ditemukan.");
        }
        Task prerequisite = found.get();
        if (prerequisite.getProjectId() != task.getProjectId()) {
            throw new DependencyException("Prasyarat harus berasal dari project yang sama.");
        }

        // Peta hubungan: id tugas -> daftar id prasyaratnya
        Map<Integer, List<Integer>> graph = new HashMap<>();
        for (TaskDependency d : taskDAO.findDependencies(task.getProjectId())) {
            boolean ignored = d.getTaskId() == task.getId() && d.getPrerequisiteTaskId() == ignoredPrerequisiteId;
            if (ignored) {
                continue;
            }
            if (!graph.containsKey(d.getTaskId())) {
                graph.put(d.getTaskId(), new ArrayList<>());
            }
            graph.get(d.getTaskId()).add(d.getPrerequisiteTaskId());
        }

        if (isWaitingFor(graph, prerequisiteId, task.getId(), new HashSet<>())) {
            throw new DependencyException("Tidak bisa dipilih. " + prerequisite.getTitle()
                    + " sudah menunggu tugas ini, jadi hubungan baru akan membentuk siklus.");
        }
    }

    // Rekursi: apakah tugas "start" (langsung atau tidak langsung) menunggu tugas "target"
    private boolean isWaitingFor(Map<Integer, List<Integer>> graph, int start, int target, Set<Integer> visited) {
        if (start == target) {
            return true;
        }
        if (visited.contains(start) || !graph.containsKey(start)) {
            return false;
        }
        visited.add(start);
        for (int next : graph.get(start)) {
            if (isWaitingFor(graph, next, target, visited)) {
                return true;
            }
        }
        return false;
    }
}
