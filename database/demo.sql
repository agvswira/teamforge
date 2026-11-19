-- Data demo sesuai contoh pada desain Figma "PBO · TeamForge (Tugas 2)".
-- Jalankan setelah schema.sql. Boleh dijalankan bersama seed.sql karena id dan email berbeda.
-- Semua akun demo memakai password yang sama dengan akun pada seed.sql.
USE teamforge;

INSERT INTO users (id, name, email, password_hash, active) VALUES
(11, 'Wira',      'wira@teamforge.id',      'a1b2c3d4e5f60718$b90f4bebd6a802058aa94ae1342fa683358e42caa59b78f1c1fbbe854ff18c07', TRUE),
(12, 'Nata',      'nata@teamforge.id',      'a1b2c3d4e5f60718$b90f4bebd6a802058aa94ae1342fa683358e42caa59b78f1c1fbbe854ff18c07', TRUE),
(13, 'Semaya',    'semaya@teamforge.id',    'a1b2c3d4e5f60718$b90f4bebd6a802058aa94ae1342fa683358e42caa59b78f1c1fbbe854ff18c07', TRUE),
(14, 'Megadhana', 'megadhana@teamforge.id', 'a1b2c3d4e5f60718$b90f4bebd6a802058aa94ae1342fa683358e42caa59b78f1c1fbbe854ff18c07', TRUE),
(15, 'Dewi',      'dewi@teamforge.id',      'a1b2c3d4e5f60718$b90f4bebd6a802058aa94ae1342fa683358e42caa59b78f1c1fbbe854ff18c07', TRUE);

INSERT INTO projects (id, name, description, deadline) VALUES
(20, 'Aplikasi TeamForge', 'Aplikasi desktop manajemen proyek untuk tugas kelompok PBO.', '2026-12-12'),
(21, 'Laporan Basis Data Lanjut', 'Studi kasus normalisasi, query laporan, dan uji performa index.', '2026-11-25'),
(22, 'Website Himpunan Informatika', 'Profil himpunan, agenda kegiatan, dan arsip dokumentasi.', '2027-01-20');

INSERT INTO project_members (project_id, user_id, role) VALUES
(20, 11, 'COORDINATOR'), (20, 12, 'MEMBER'), (20, 13, 'MEMBER'), (20, 14, 'MEMBER'), (20, 15, 'MEMBER'),
(21, 12, 'COORDINATOR'), (21, 11, 'MEMBER'), (21, 13, 'MEMBER'), (21, 14, 'MEMBER'),
(22, 11, 'COORDINATOR'), (22, 13, 'MEMBER'), (22, 15, 'MEMBER');

INSERT INTO tasks (id, project_id, title, description, status, priority, due_at, notes) VALUES
(101, 20, 'Setup Maven & Git', 'Struktur proyek Maven modular dan repository GitHub.', 'DONE', 2, '2026-10-30', NULL),
(102, 20, 'Rancang database', 'Tabel users, projects, tasks, dan tiga tabel relasi.', 'DONE', 3, '2026-11-02', NULL),
(103, 20, 'Implementasi AuthService', 'Registrasi, login, dan hash password.', 'DONE', 3, '2026-11-06', NULL),
(104, 20, 'DAO project & membership', 'Query membuat project, membaca project yang diikuti, dan mengelola anggota beserta rolenya.', 'IN_PROGRESS', 3, '2026-11-08',
      'Query project dan anggota sudah jalan. Sisa: validasi role saat Coordinator mengubah anggota.'),
(105, 20, 'Formulir login & registrasi', 'Tampilan FXML login dan registrasi.', 'IN_PROGRESS', 2, '2026-11-24', NULL),
(106, 20, 'Tema CSS & ikon', 'Stylesheet sesuai design system OS5.', 'TODO', 1, '2026-12-01', NULL),
(107, 20, 'Halaman kelola tugas', 'Tabel tugas dengan filter status, tombol ubah, dan tombol hapus sesuai hak akses Coordinator.', 'TODO', 2, '2026-12-03', NULL),
(108, 20, 'Validasi dependency', 'Tolak dependency ke diri sendiri, lintas project, atau siklus.', 'TODO', 3, '2026-12-04', NULL),
(109, 20, 'Uji coba test case', 'Jalankan TC-01 sampai TC-10.', 'TODO', 2, '2026-12-08', NULL),
(110, 20, 'Susun laporan akhir', 'Lengkapi bab implementasi, test case, dan peer review.', 'TODO', 1, '2026-12-10', NULL),
(111, 21, 'Tentukan studi kasus', NULL, 'DONE', 2, '2026-10-28', NULL),
(112, 21, 'Normalisasi tabel', NULL, 'DONE', 3, '2026-11-03', NULL),
(113, 21, 'Tulis query DDL', NULL, 'DONE', 3, '2026-11-05', NULL),
(114, 21, 'Tulis query laporan', NULL, 'DONE', 2, '2026-11-07', NULL),
(115, 21, 'Uji performa index', 'Bandingkan waktu query sebelum dan sesudah index.', 'IN_PROGRESS', 2, '2026-11-20', NULL),
(116, 21, 'Susun laporan', NULL, 'TODO', 1, '2026-11-24', NULL);

INSERT INTO task_assignments (task_id, user_id, responsibility) VALUES
(101, 14, NULL),
(102, 11, 'Physical Data Model'), (102, 12, 'DDL dan data contoh'),
(103, 14, NULL),
(104, 14, 'Query project dan anggota'), (104, 11, 'Validasi role saat anggota diubah'),
(105, 12, NULL),
(106, 13, NULL),
(107, 13, 'UI tabel dan filter status'), (107, 12, 'Hubungkan tabel ke TaskService'),
(108, 11, NULL),
(109, 12, NULL), (109, 14, NULL),
(110, 11, NULL), (110, 14, NULL),
(111, 12, NULL),
(112, 11, NULL), (112, 13, NULL),
(113, 13, NULL),
(114, 11, NULL),
(115, 14, NULL), (115, 11, NULL),
(116, 12, NULL), (116, 11, NULL);

INSERT INTO task_dependencies (task_id, prerequisite_task_id) VALUES
(103, 102), (104, 103), (105, 102),
(107, 104), (108, 104),
(109, 107), (109, 108),
(110, 109),
(113, 112), (114, 113), (115, 113), (116, 115);
