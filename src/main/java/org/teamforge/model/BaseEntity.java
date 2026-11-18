package org.teamforge.model;

/**
 * Kelas induk untuk semua entitas yang memiliki id.
 */
public abstract class BaseEntity {
    private int id;

    protected BaseEntity() {
    }

    protected BaseEntity(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    // Setiap turunan menentukan sendiri teks yang ditampilkan di layar
    public abstract String getDisplayName();

    @Override
    public String toString() {
        return getDisplayName();
    }
}
