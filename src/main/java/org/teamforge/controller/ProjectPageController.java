package org.teamforge.controller;

import org.teamforge.model.Project;
import org.teamforge.model.ProjectMembership;
import org.teamforge.model.User;
import org.teamforge.service.ProjectService;
import org.teamforge.service.TaskService;
import org.teamforge.util.Session;

/**
 * Kelas induk untuk halaman di dalam project (Dashboard, Kelola tugas, Kelola anggota,
 * dan Tugas saya). Menyimpan data yang dipakai bersama oleh semua halaman.
 */
public abstract class ProjectPageController {
    protected final ProjectService projectService = new ProjectService();
    protected final TaskService taskService = new TaskService();

    protected MainController main;
    protected Project project;
    protected ProjectMembership membership;
    protected User user;

    // Dipanggil oleh MainController setelah halaman dimuat
    public void setMain(MainController main) {
        this.main = main;
        this.project = main.getProject();
        this.membership = main.getMembership();
        this.user = Session.getCurrentUser();
    }

    protected boolean isCoordinator() {
        return membership.canManageProject();
    }

    // Setiap halaman menentukan sendiri cara memuat ulang datanya
    public abstract void refresh();
}
