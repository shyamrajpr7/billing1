package com.shop.model;

import java.time.LocalDateTime;

public class User {
    private int id;
    private String username;
    private String passwordHash;
    private String fullName;
    private Role role;
    private boolean active;
    private boolean darkTheme;
    private LocalDateTime createdAt;

    public User() {
        this.active = true;
        this.createdAt = LocalDateTime.now();
    }

    public User(String username, String passwordHash, String fullName, Role role) {
        this();
        this.username = username;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
        this.role = role;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public boolean isDarkTheme() { return darkTheme; }
    public void setDarkTheme(boolean darkTheme) { this.darkTheme = darkTheme; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getRoleDisplay() { return role != null ? role.getDisplayName() : ""; }
    public String getStatus() { return active ? "Active" : "Inactive"; }

    @Override
    public String toString() { return fullName + " (" + getRoleDisplay() + ")"; }
}
