package com.example.notevault.model;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;

/**
 * Model class for a Folder document in Firestore.
 * Supports recycle bin (soft delete), hide, and lock with password.
 */
public class Folder {

    private String id;
    private String name;
    private String color;

    @ServerTimestamp
    private Date createdAt;

    /** Soft delete: when true, folder appears in Recycle Bin only */
    private boolean isDeleted;
    /** When folder was moved to recycle bin (for 30-day retention) */
    private Date deletedAt;
    /** When true, folder appears in Hidden section only */
    private boolean isHidden;
    /** When true, user must enter password to open folder */
    private boolean isLocked;
    /** SHA-256 hash of lock password (never store plain password) */
    private String passwordHash;

    public Folder() {
    }

    public Folder(String id, String name, String color, Date createdAt) {
        this.id = id;
        this.name = name;
        this.color = color;
        this.createdAt = createdAt;
    }

    @Exclude
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isDeleted() {
        return isDeleted;
    }

    public void setDeleted(boolean deleted) {
        isDeleted = deleted;
    }

    public Date getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(Date deletedAt) {
        this.deletedAt = deletedAt;
    }

    public boolean isHidden() {
        return isHidden;
    }

    public void setHidden(boolean hidden) {
        isHidden = hidden;
    }

    public boolean isLocked() {
        return isLocked;
    }

    public void setLocked(boolean locked) {
        isLocked = locked;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }
}
