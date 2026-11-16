-- Data contoh sesuai Actual Data Model pada laporan TK I.
-- Ketiga akun uji memakai password: password123
-- Format password_hash: salt$sha256(salt + password)
USE teamforge;

INSERT INTO users (id, name, email, password_hash, active) VALUES
(1, 'Pengguna Uji A', 'example@gmail.com', 'a1b2c3d4e5f60718$b90f4bebd6a802058aa94ae1342fa683358e42caa59b78f1c1fbbe854ff18c07', TRUE),
(2, 'Pengguna Uji B', 'contoh@gmail.com',  '0f1e2d3c4b5a6978$431e12094b57b10756044f6b51a2a72fd32e9e4aef59ca3a5739c24d200fe271', TRUE),
(3, 'Pengguna Uji C', 'test@gmail.com',    '9a8b7c6d5e4f3021$fbbc651b6344de89ab298ac895db789d4b2383d8c1650cea496cb62d9276f733', TRUE);

INSERT INTO projects (id, name, description, deadline) VALUES
(10, 'TeamForge Uji', 'Project contoh untuk pengujian TeamForge', '2026-12-20'),
(11, 'Project Uji B', 'Project contoh kedua', '2026-12-31');

INSERT INTO project_members (project_id, user_id, role) VALUES
(10, 1, 'COORDINATOR'),
(10, 2, 'MEMBER'),
(11, 2, 'COORDINATOR'),
(11, 1, 'MEMBER');

INSERT INTO tasks (id, project_id, title, description, status, priority, due_at, notes) VALUES
(50, 10, 'Rancang database', 'Membuat PDM dan tabel MySQL', 'DONE', 3, '2026-11-10', NULL),
(51, 10, 'Buat formulir', 'Membuat formulir login dan registrasi', 'TODO', 2, '2026-11-25', NULL);

INSERT INTO task_assignments (task_id, user_id, responsibility) VALUES
(50, 1, NULL),
(50, 2, NULL),
(51, 2, NULL);

INSERT INTO task_dependencies (task_id, prerequisite_task_id) VALUES
(51, 50);
