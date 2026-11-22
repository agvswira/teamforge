TEAMFORGE - Sistem Manajemen Proyek (JavaFX + MySQL)
PBO Kelas B - Kelompok 3, Informatika Universitas Udayana
=====================================================

1. KEBUTUHAN
   - JDK 21
   - Maven 3.9 atau lebih baru (JavaFX 21 dan MySQL Connector/J diunduh otomatis oleh Maven)
   - MySQL Server 8.0 / 9.x

2. MENYIAPKAN DATABASE
   a. Pastikan service MySQL berjalan.
      Windows: buka "Services", jalankan MySQL80 / MySQL93,
      atau buka Command Prompt sebagai Administrator lalu ketik:  net start MySQL93
   b. Buat database dan tabel, lalu isi data:
         mysql -u root -p -e "source database/schema.sql"
         mysql -u root -p -e "source database/seed.sql"
         mysql -u root -p -e "source database/demo.sql"
      (atau buka ketiga file di MySQL Workbench lalu jalankan berurutan)
      - schema.sql menghapus database teamforge lama jika sudah ada.
      - seed.sql berisi data contoh sesuai Actual Data Model laporan TK I.
      - demo.sql berisi data contoh sesuai desain Figma (boleh dijalankan bersama seed.sql).
   c. Salin db.properties.example menjadi db.properties, lalu isi password root MySQL Anda.
      File db.properties tidak ikut di-commit ke GitHub.
         db.url=jdbc:mysql://localhost:3306/teamforge
         db.user=root
         db.password=<password MySQL Anda>

3. MENJALANKAN APLIKASI
   Dari folder proyek ini:
         mvn clean javafx:run

   Semua akun contoh (seed.sql dan demo.sql) memakai password: password123
   Data seed.sql:
         example@gmail.com  -> Coordinator pada "TeamForge Uji", Member pada "Project Uji B"
         contoh@gmail.com   -> Member pada "TeamForge Uji", Coordinator pada "Project Uji B"
         test@gmail.com     -> belum mengikuti project
   Data demo.sql:
         wira@teamforge.id  -> Coordinator "Aplikasi TeamForge" dan "Website Himpunan Informatika",
                               Member "Laporan Basis Data Lanjut"
         nata@teamforge.id  -> Coordinator "Laporan Basis Data Lanjut"
         semaya@teamforge.id, megadhana@teamforge.id, dewi@teamforge.id -> Member
   Akun baru juga dapat dibuat melalui halaman "Daftar".

4. STRUKTUR PROYEK (pola MVC + DAO seperti materi pertemuan 13)
   database/                     schema.sql, seed.sql, demo.sql
   src/main/java/module-info.java
   src/main/java/org/teamforge/
       App.java                  titik masuk JavaFX, perpindahan tampilan, jendela modal
       model/                    BaseEntity, User, Project, Task, ProjectMembership,
                                 CoordinatorMembership, MemberMembership, TaskAssignment,
                                 TaskDependency, TaskStatus, ProjectRole
       dao/                      CrudDAO<T>, UserDAO, ProjectDAO, MembershipDAO, TaskDAO (query JDBC)
       service/                  AuthService, ProjectService, TaskService
                                 (aturan role, prasyarat, dan transaksi)
       exception/                ValidationException, DependencyException
       controller/               satu controller untuk setiap file FXML, ProjectPageController
                                 sebagai kelas induk halaman di dalam project
       util/                     DatabaseConnection, Session, AlertUtil, StyleHelper
   src/main/resources/org/teamforge/view/   file tampilan *.fxml

5. TABEL RELASI MANY-TO-MANY DAN OPERASI CRUD
   project_members    (users <-> projects)  : MembershipDAO
       C: tambah anggota / buat project   R: Kelola anggota, Dashboard
       U: ubah role                       D: keluarkan anggota
   task_assignments   (users <-> tasks)     : TaskDAO
       C: centang penerima di dialog tugas      R: Kelola tugas, Tugas saya
       D: hapus centang penerima
   task_dependencies  (tasks <-> tasks)     : TaskDAO
       C: tambah prasyarat di dialog tugas      R: Kelola tugas, Tugas saya
       U: lepas satu prasyarat lalu tambah yang lain dalam sekali simpan
       D: lepas prasyarat

6. MASALAH UMUM
   - "Gagal mengakses database ... Communications link failure"
       MySQL belum berjalan atau port/password di db.properties salah.
   - "Access denied for user 'root'"
       Isi db.password di db.properties.
   - "Unknown database 'teamforge'"
       Jalankan database/schema.sql terlebih dahulu.
