package com.example.notevault.model;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;

/**
 * Model class for a Note document in Firestore.
 * Supports recycle bin (soft delete), hide, and lock with password.
 */
public class Note {

    private String id;
    private String userId;  // Owner user id (for collection group queries in recycle bin)
    private String folderId; // Which folder this note belongs to (for recycle bin display)
    private String title;
    private String content;
    private String color;

    @ServerTimestamp
    private Date timestamp;
    @ServerTimestamp
    private Date updatedAt;

    /** Soft delete: when true, note appears in Recycle Bin only */
    private boolean isDeleted;
    private Date deletedAt;
    /** When true, note appears in Hidden section only */
    private boolean isHidden;
    /** When true, user must enter password to open note */
    private boolean isLocked;
    /** SHA-256 hash of lock password */
    private String passwordHash;

    public Note() {
    }

    public Note(String id, String title, String content, String color, Date timestamp) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.color = color;
        this.timestamp = timestamp;
    }

    @Exclude
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFolderId() {
        return folderId;
    }

    public void setFolderId(String folderId) {
        this.folderId = folderId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
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
