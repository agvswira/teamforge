package org.teamforge.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Menyediakan satu koneksi MySQL yang dipakai bersama oleh semua DAO.
 * Pengaturan dibaca dari file db.properties di folder proyek; jika file tidak ada,
 * nilai default di bawah yang dipakai.
 */
public class DatabaseConnection {
    private static final String JDBC_DRIVER = "com.mysql.cj.jdbc.Driver";
    private static final String CONFIG_FILE = "db.properties";
    private static final String DEFAULT_URL = "jdbc:mysql://localhost:3306/teamforge";
    private static final String DEFAULT_USER = "root";
    private static final String DEFAULT_PASS = "";

    private static Connection connection;

    private DatabaseConnection() {
    }

    public static Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            try {
                Class.forName(JDBC_DRIVER);
            } catch (ClassNotFoundException e) {
                throw new SQLException("Driver MySQL tidak ditemukan.", e);
            }
            Properties config = loadConfig();
            connection = DriverManager.getConnection(
                    config.getProperty("db.url", DEFAULT_URL),
                    config.getProperty("db.user", DEFAULT_USER),
                    config.getProperty("db.password", DEFAULT_PASS));
        }
        return connection;
    }

    public static void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            System.out.println("Gagal menutup koneksi: " + e.getMessage());
        }
    }

    private static Properties loadConfig() {
        Properties config = new Properties();
        File file = new File(CONFIG_FILE);
        if (file.exists()) {
            try (FileInputStream input = new FileInputStream(file)) {
                config.load(input);
            } catch (IOException e) {
                System.out.println("db.properties tidak dapat dibaca, memakai pengaturan default.");
            }
        }
        return config;
    }
}
