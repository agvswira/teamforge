-- Database TeamForge
-- Jalankan file ini di MySQL Workbench atau: mysql -u root -p < database/schema.sql

DROP DATABASE IF EXISTS teamforge;
CREATE DATABASE teamforge;
USE teamforge;

CREATE TABLE users (
    id            INT AUTO_INCREMENT PRIMARY KEY,
    name          VARCHAR(100) NOT NULL,
    email         VARCHAR(150) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    active        BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE projects (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(150) NOT NULL,
    description TEXT,
    deadline    DATE NOT NULL
);

-- Relasi many-to-many users <-> projects, role disimpan di sini
CREATE TABLE project_members (
    project_id INT NOT NULL,
    user_id    INT NOT NULL,
    role       VARCHAR(20) NOT NULL,
    PRIMARY KEY (project_id, user_id),
    CONSTRAINT fk_pm_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
    CONSTRAINT fk_pm_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT chk_pm_role CHECK (role IN ('COORDINATOR', 'MEMBER'))
);

CREATE TABLE tasks (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    project_id  INT NOT NULL,
    title       VARCHAR(150) NOT NULL,
    description TEXT,
    status      VARCHAR(20) NOT NULL DEFAULT 'TODO',
    priority    INT NOT NULL DEFAULT 2,
    due_at      DATE,
    notes       TEXT,
    CONSTRAINT fk_task_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
    CONSTRAINT chk_task_status CHECK (status IN ('TODO', 'IN_PROGRESS', 'DONE')),
    CONSTRAINT chk_task_priority CHECK (priority BETWEEN 1 AND 3)
);

-- Relasi many-to-many users <-> tasks
CREATE TABLE task_assignments (
    task_id        INT NOT NULL,
    user_id        INT NOT NULL,
    responsibility VARCHAR(100),
    PRIMARY KEY (task_id, user_id),
    CONSTRAINT fk_ta_task FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE CASCADE,
    CONSTRAINT fk_ta_user FOREIGN KEY (user_id) REFERENCES users(id)
);

-- Relasi many-to-many tasks <-> tasks (prasyarat)
CREATE TABLE task_dependencies (
    task_id              INT NOT NULL,
    prerequisite_task_id INT NOT NULL,
    PRIMARY KEY (task_id, prerequisite_task_id),
    CONSTRAINT fk_td_task FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE CASCADE,
    CONSTRAINT fk_td_prerequisite FOREIGN KEY (prerequisite_task_id) REFERENCES tasks(id) ON DELETE CASCADE,
    CONSTRAINT chk_td_not_self CHECK (task_id <> prerequisite_task_id)
);
