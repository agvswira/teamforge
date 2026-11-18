package org.teamforge.model;

public class User extends BaseEntity {
    private String name;
    private String email;
    private String passwordHash;
    private boolean active;

    public User(String name, String email, String passwordHash) {
        this(0, name, email, passwordHash, true);
    }

    public User(int id, String name, String email, String passwordHash, boolean active) {
        super(id);
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
        this.active = active;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public String getDisplayName() {
        return name;
    }
}
