package org.teamforge.exception;

import java.util.ArrayList;
import java.util.List;

import org.teamforge.model.Task;

/**
 * Dilempar ketika aturan prasyarat dilanggar: hubungan membentuk siklus,
 * atau tugas dimulai sebelum semua prasyaratnya DONE.
 */
public class DependencyException extends ValidationException {
    private final List<Task> blockers;

    public DependencyException(String message) {
        super(message);
        this.blockers = new ArrayList<>();
    }

    public DependencyException(String message, List<Task> blockers) {
        super(message);
        this.blockers = blockers;
    }

    // Daftar tugas prasyarat yang belum DONE
    public List<Task> getBlockers() {
        return blockers;
    }
}
