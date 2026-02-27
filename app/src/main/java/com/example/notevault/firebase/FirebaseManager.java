package com.example.notevault.firebase;

import com.example.notevault.model.Folder;
import com.example.notevault.model.Note;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Centralized Firebase Authentication, Firestore and Storage manager.
 * Handles folders/notes with recycle bin, hide, and lock.
 * Firestore: users/{userId}/folders/{folderId}/notes/{noteId}
 */
public class FirebaseManager {

    private static final String COLLECTION_USERS = "users";
    private static final String COLLECTION_FOLDERS = "folders";
    private static final String COLLECTION_NOTES = "notes";
    private static final int RECYCLE_BIN_DAYS = 30;

    private static FirebaseManager instance;
    private final FirebaseAuth auth;
    private final FirebaseFirestore firestore;
    private final FirebaseStorage storage;

    public interface AuthCallback {
        void onSuccess(FirebaseUser user);
        void onError(String message);
    }

    public interface OperationCallback {
        void onComplete(boolean success, String message);
    }

    public interface ProfilePhotoCallback {
        void onSuccess(String downloadUrl);
        void onError(String message);
    }

    public interface UserPhotoUrlCallback {
        void onLoaded(String photoUrl);
    }

    private FirebaseManager() {
        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .build();
        firestore.setFirestoreSettings(settings);
    }

    public static FirebaseManager getInstance() {
        if (instance == null) {
            instance = new FirebaseManager();
        }
        return instance;
    }

    public FirebaseUser getCurrentUser() {
        return auth.getCurrentUser();
    }

    // ==================== AUTHENTICATION ====================

    public void registerWithEmail(String email, String password, final AuthCallback callback) {
        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = auth.getCurrentUser();
                        if (user != null) createUserDocument(user);
                        if (callback != null) callback.onSuccess(user);
                    } else {
                        if (callback != null) {
                            String msg = task.getException() != null ? task.getException().getMessage() : "Registration failed";
                            callback.onError(msg);
                        }
                    }
                });
    }

    public void signInWithEmail(String email, String password, final AuthCallback callback) {
        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (callback != null) callback.onSuccess(auth.getCurrentUser());
                    } else {
                        if (callback != null) {
                            String msg = task.getException() != null ? task.getException().getMessage() : "Authentication failed";
                            callback.onError(msg);
                        }
                    }
                });
    }

    public void signOut() {
        auth.signOut();
    }

    // ==================== USER PROFILE ====================

    public void uploadProfilePhoto(String userId, byte[] imageBytes, ProfilePhotoCallback callback) {
        StorageReference ref = storage.getReference().child("profile_photos").child(userId + ".jpg");
        UploadTask task = ref.putBytes(imageBytes);
        task.addOnSuccessListener(t -> ref.getDownloadUrl().addOnSuccessListener(uri -> {
            if (callback != null) callback.onSuccess(uri.toString());
        }).addOnFailureListener(e -> {
            if (callback != null) callback.onError(e.getMessage());
        })).addOnFailureListener(e -> {
            if (callback != null) callback.onError(e.getMessage());
        });
    }

    public void updateUserPhotoUrl(String userId, String photoUrl, OperationCallback callback) {
        firestore.collection(COLLECTION_USERS).document(userId)
                .update("photoUrl", photoUrl)
                .addOnCompleteListener(t -> {
                    if (callback != null) {
                        callback.onComplete(t.isSuccessful(), t.getException() != null ? t.getException().getMessage() : "Updated");
                    }
                });
    }

    /** Load user profile photo URL from Firestore for drawer header. */
    public void getUserPhotoUrl(String userId, UserPhotoUrlCallback callback) {
        firestore.collection(COLLECTION_USERS).document(userId).get()
                .addOnSuccessListener(doc -> {
                    if (callback != null && doc != null && doc.contains("photoUrl")) {
                        Object url = doc.get("photoUrl");
                        callback.onLoaded(url != null ? url.toString() : null);
                    } else if (callback != null) {
                        callback.onLoaded(null);
                    }
                })
                .addOnFailureListener(e -> { if (callback != null) callback.onLoaded(null); });
    }

    private void createUserDocument(FirebaseUser user) {
        Map<String, Object> data = new HashMap<>();
        data.put("uid", user.getUid());
        data.put("email", user.getEmail());
        firestore.collection(COLLECTION_USERS).document(user.getUid()).set(data);
    }

    // ==================== FOLDERS (main list: not deleted, not hidden) ====================

    public void listenToFolders(String userId, EventListener<QuerySnapshot> listener) {
        getFoldersCollection(userId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener(listener);
    }

    public void listenToDeletedFolders(String userId, EventListener<QuerySnapshot> listener) {
        getFoldersCollection(userId)
                .whereEqualTo("isDeleted", true)
                .orderBy("deletedAt", Query.Direction.DESCENDING)
                .addSnapshotListener(listener);
    }

    public void listenToHiddenFolders(String userId, EventListener<QuerySnapshot> listener) {
        getFoldersCollection(userId)
                .whereEqualTo("isHidden", true)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener(listener);
    }

    public void addOrUpdateFolder(String userId, Folder folder, final OperationCallback callback) {
        CollectionReference ref = getFoldersCollection(userId);
        DocumentReference docRef = (folder.getId() == null || folder.getId().isEmpty())
                ? ref.document() : ref.document(folder.getId());
        if (folder.getId() == null || folder.getId().isEmpty()) folder.setId(docRef.getId());

        Map<String, Object> data = new HashMap<>();
        data.put("name", folder.getName());
        data.put("color", folder.getColor() != null ? folder.getColor() : "#4ECDC4");
        data.put("createdAt", folder.getCreatedAt() != null ? folder.getCreatedAt() : FieldValue.serverTimestamp());
        data.put("isDeleted", folder.isDeleted());
        data.put("isHidden", folder.isHidden());
        data.put("isLocked", folder.isLocked());
        if (folder.getDeletedAt() != null) data.put("deletedAt", folder.getDeletedAt());
        if (folder.getPasswordHash() != null) data.put("passwordHash", folder.getPasswordHash());

        docRef.set(data).addOnCompleteListener(task -> {
            if (callback != null) callback.onComplete(task.isSuccessful(),
                    task.getException() != null ? task.getException().getMessage() : "Folder saved");
        });
    }

    /** Move folder to recycle bin (soft delete). Also marks all notes in folder as deleted. */
    public void moveFolderToRecycleBin(String userId, String folderId, OperationCallback callback) {
        Date now = new Date();
        DocumentReference folderRef = getFoldersCollection(userId).document(folderId);
        folderRef.update("isDeleted", true, "deletedAt", now).addOnSuccessListener(aVoid -> {
            getNotesCollection(userId, folderId).get().addOnSuccessListener(snap -> {
                for (int i = 0; i < snap.size(); i++) {
                    snap.getDocuments().get(i).getReference().update("isDeleted", true, "deletedAt", now);
                }
                if (callback != null) callback.onComplete(true, "Moved to Recycle Bin");
            }).addOnFailureListener(e -> {
                if (callback != null) callback.onComplete(false, e.getMessage());
            });
        }).addOnFailureListener(e -> {
            if (callback != null) callback.onComplete(false, e.getMessage());
        });
    }

    /** Restore folder from recycle bin; restores all notes in folder. */
    public void restoreFolder(String userId, String folderId, OperationCallback callback) {
        DocumentReference folderRef = getFoldersCollection(userId).document(folderId);
        Map<String, Object> updates = new HashMap<>();
        updates.put("isDeleted", false);
        updates.put("deletedAt", FieldValue.delete());
        folderRef.update(updates).addOnSuccessListener(aVoid -> {
            getNotesCollection(userId, folderId).get().addOnSuccessListener(snap -> {
                for (int i = 0; i < snap.size(); i++) {
                    Map<String, Object> noteUpdates = new HashMap<>();
                    noteUpdates.put("isDeleted", false);
                    noteUpdates.put("deletedAt", FieldValue.delete());
                    snap.getDocuments().get(i).getReference().update(noteUpdates);
                }
                if (callback != null) callback.onComplete(true, "Restored");
            }).addOnFailureListener(e -> {
                if (callback != null) callback.onComplete(false, e.getMessage());
            });
        }).addOnFailureListener(e -> {
            if (callback != null) callback.onComplete(false, e.getMessage());
        });
    }

    /** Permanent delete folder and all its notes. */
    public void permanentDeleteFolder(String userId, String folderId, OperationCallback callback) {
        getNotesCollection(userId, folderId).get().addOnSuccessListener(snap -> {
            for (int i = 0; i < snap.size(); i++) {
                snap.getDocuments().get(i).getReference().delete();
            }
            getFoldersCollection(userId).document(folderId).delete()
                    .addOnCompleteListener(t -> {
                        if (callback != null) callback.onComplete(t.isSuccessful(),
                                t.getException() != null ? t.getException().getMessage() : "Deleted permanently");
                    });
        }).addOnFailureListener(e -> {
            if (callback != null) callback.onComplete(false, e.getMessage());
        });
    }

    public void hideFolder(String userId, String folderId, OperationCallback callback) {
        getFoldersCollection(userId).document(folderId).update("isHidden", true)
                .addOnCompleteListener(t -> {
                    if (callback != null) callback.onComplete(t.isSuccessful(), t.getException() != null ? t.getException().getMessage() : "Hidden");
                });
        getNotesCollection(userId, folderId).get().addOnSuccessListener(snap -> {
            for (int i = 0; i < snap.size(); i++) {
                snap.getDocuments().get(i).getReference().update("isHidden", true);
            }
        });
    }

    public void unhideFolder(String userId, String folderId, OperationCallback callback) {
        getFoldersCollection(userId).document(folderId).update("isHidden", false)
                .addOnSuccessListener(aVoid -> {
                    getNotesCollection(userId, folderId).get().addOnSuccessListener(snap -> {
                        for (int i = 0; i < snap.size(); i++) {
                            snap.getDocuments().get(i).getReference().update("isHidden", false);
                        }
                        if (callback != null) callback.onComplete(true, "Unhidden");
                    });
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onComplete(false, e.getMessage());
                });
    }

    public void lockFolder(String userId, String folderId, String passwordHash, OperationCallback callback) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("isLocked", true);
        updates.put("passwordHash", passwordHash);
        getFoldersCollection(userId).document(folderId).update(updates)
                .addOnCompleteListener(t -> {
                    if (callback != null) callback.onComplete(t.isSuccessful(), t.getException() != null ? t.getException().getMessage() : "Locked");
                });
    }

    public void unlockFolder(String userId, String folderId, OperationCallback callback) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("isLocked", false);
        updates.put("passwordHash", FieldValue.delete());
        getFoldersCollection(userId).document(folderId).update(updates)
                .addOnCompleteListener(t -> {
                    if (callback != null) callback.onComplete(t.isSuccessful(), t.getException() != null ? t.getException().getMessage() : "Unlocked");
                });
    }

    // ==================== NOTES ====================

    public void listenToNotes(String userId, String folderId, EventListener<QuerySnapshot> listener) {
        getNotesCollection(userId, folderId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener(listener);
    }

    public void listenToDeletedNotesInFolder(String userId, String folderId, EventListener<QuerySnapshot> listener) {
        getNotesCollection(userId, folderId)
                .whereEqualTo("isDeleted", true)
                .orderBy("deletedAt", Query.Direction.DESCENDING)
                .addSnapshotListener(listener);
    }

    /** Listen to all deleted notes for this user (for Recycle Bin). Uses collection group. */
    public void listenToAllDeletedNotes(String userId, EventListener<QuerySnapshot> listener) {
        firestore.collectionGroup(COLLECTION_NOTES)
                .whereEqualTo("userId", userId)
                .whereEqualTo("isDeleted", true)
                .orderBy("deletedAt", Query.Direction.DESCENDING)
                .addSnapshotListener(listener);
    }

    public void listenToHiddenNotes(String userId, String folderId, EventListener<QuerySnapshot> listener) {
        getNotesCollection(userId, folderId)
                .whereEqualTo("isHidden", true)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener(listener);
    }

    /** Listen to all hidden notes for this user (for Hidden screen). */
    public void listenToAllHiddenNotes(String userId, EventListener<QuerySnapshot> listener) {
        firestore.collectionGroup(COLLECTION_NOTES)
                .whereEqualTo("userId", userId)
                .whereEqualTo("isHidden", true)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener(listener);
    }

    public void addOrUpdateNote(String userId, String folderId, Note note, final OperationCallback callback) {
        CollectionReference notesRef = getNotesCollection(userId, folderId);
        DocumentReference docRef = (note.getId() == null || note.getId().isEmpty())
                ? notesRef.document() : notesRef.document(note.getId());
        if (note.getId() == null || note.getId().isEmpty()) note.setId(docRef.getId());
        if (note.getFolderId() == null) note.setFolderId(folderId);
        if (note.getUserId() == null) note.setUserId(userId);

        Map<String, Object> data = new HashMap<>();
        data.put("title", note.getTitle());
        data.put("content", note.getContent());
        data.put("color", note.getColor() != null ? note.getColor() : "#4ECDC4");
        data.put("folderId", note.getFolderId());
        data.put("userId", note.getUserId());
        data.put("timestamp", note.getTimestamp() != null ? note.getTimestamp() : FieldValue.serverTimestamp());
        data.put("updatedAt", note.getUpdatedAt() != null ? note.getUpdatedAt() : FieldValue.serverTimestamp());
        data.put("isDeleted", note.isDeleted());
        data.put("isHidden", note.isHidden());
        data.put("isLocked", note.isLocked());
        if (note.getDeletedAt() != null) data.put("deletedAt", note.getDeletedAt());
        if (note.getPasswordHash() != null) data.put("passwordHash", note.getPasswordHash());

        docRef.set(data).addOnCompleteListener(task -> {
            if (callback != null) callback.onComplete(task.isSuccessful(),
                    task.getException() != null ? task.getException().getMessage() : "Note saved");
        });
    }

    /** Move note to recycle bin (soft delete). */
    public void moveNoteToRecycleBin(String userId, String folderId, String noteId, OperationCallback callback) {
        Date now = new Date();
        getNotesCollection(userId, folderId).document(noteId)
                .update("isDeleted", true, "deletedAt", now)
                .addOnCompleteListener(t -> {
                    if (callback != null) callback.onComplete(t.isSuccessful(), t.getException() != null ? t.getException().getMessage() : "Moved to Recycle Bin");
                });
    }

    public void restoreNote(String userId, String folderId, String noteId, OperationCallback callback) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("isDeleted", false);
        updates.put("deletedAt", FieldValue.delete());
        getNotesCollection(userId, folderId).document(noteId).update(updates)
                .addOnCompleteListener(t -> {
                    if (callback != null) callback.onComplete(t.isSuccessful(), t.getException() != null ? t.getException().getMessage() : "Restored");
                });
    }

    public void permanentDeleteNote(String userId, String folderId, String noteId, OperationCallback callback) {
        getNotesCollection(userId, folderId).document(noteId).delete()
                .addOnCompleteListener(t -> {
                    if (callback != null) callback.onComplete(t.isSuccessful(), t.getException() != null ? t.getException().getMessage() : "Deleted");
                });
    }

    public void hideNote(String userId, String folderId, String noteId, OperationCallback callback) {
        getNotesCollection(userId, folderId).document(noteId).update("isHidden", true)
                .addOnCompleteListener(t -> {
                    if (callback != null) callback.onComplete(t.isSuccessful(), t.getException() != null ? t.getException().getMessage() : "Hidden");
                });
    }

    public void unhideNote(String userId, String folderId, String noteId, OperationCallback callback) {
        getNotesCollection(userId, folderId).document(noteId).update("isHidden", false)
                .addOnCompleteListener(t -> {
                    if (callback != null) callback.onComplete(t.isSuccessful(), t.getException() != null ? t.getException().getMessage() : "Unhidden");
                });
    }

    public void lockNote(String userId, String folderId, String noteId, String passwordHash, OperationCallback callback) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("isLocked", true);
        updates.put("passwordHash", passwordHash);
        getNotesCollection(userId, folderId).document(noteId).update(updates)
                .addOnCompleteListener(t -> {
                    if (callback != null) callback.onComplete(t.isSuccessful(), t.getException() != null ? t.getException().getMessage() : "Locked");
                });
    }

    public void unlockNote(String userId, String folderId, String noteId, OperationCallback callback) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("isLocked", false);
        updates.put("passwordHash", FieldValue.delete());
        getNotesCollection(userId, folderId).document(noteId).update(updates)
                .addOnCompleteListener(t -> {
                    if (callback != null) callback.onComplete(t.isSuccessful(), t.getException() != null ? t.getException().getMessage() : "Unlocked");
                });
    }

    /** Delete items in recycle bin older than RECYCLE_BIN_DAYS. Call when opening Recycle Bin. */
    public void cleanupOldRecycleBinItems(String userId, OperationCallback callback) {
        long cutoff = System.currentTimeMillis() - (RECYCLE_BIN_DAYS * 24L * 60 * 60 * 1000);
        Date cutoffDate = new Date(cutoff);
        final int[] deleted = {0};
        getFoldersCollection(userId).whereEqualTo("isDeleted", true).get().addOnSuccessListener(folderSnap -> {
            for (int i = 0; i < folderSnap.size(); i++) {
                var doc = folderSnap.getDocuments().get(i);
                Object deletedAt = doc.get("deletedAt");
                if (deletedAt instanceof com.google.firebase.Timestamp) {
                    if (((com.google.firebase.Timestamp) deletedAt).toDate().before(cutoffDate)) {
                        doc.getReference().delete();
                        deleted[0]++;
                        getNotesCollection(userId, doc.getId()).get().addOnSuccessListener(noteSnap -> {
                            for (int j = 0; j < noteSnap.size(); j++) noteSnap.getDocuments().get(j).getReference().delete();
                        });
                    }
                }
            }
            firestore.collectionGroup(COLLECTION_NOTES).whereEqualTo("userId", userId).whereEqualTo("isDeleted", true).get()
                    .addOnSuccessListener(noteSnap -> {
                        for (int i = 0; i < noteSnap.size(); i++) {
                            var doc = noteSnap.getDocuments().get(i);
                            Object deletedAt = doc.get("deletedAt");
                            if (deletedAt instanceof com.google.firebase.Timestamp) {
                                if (((com.google.firebase.Timestamp) deletedAt).toDate().before(cutoffDate)) {
                                    doc.getReference().delete();
                                    deleted[0]++;
                                }
                            }
                        }
                        if (callback != null) callback.onComplete(true, "Cleanup done");
                    });
        });
    }

    // ==================== HELPERS ====================

    private CollectionReference getFoldersCollection(String userId) {
        return firestore.collection(COLLECTION_USERS).document(userId).collection(COLLECTION_FOLDERS);
    }

    private CollectionReference getNotesCollection(String userId, String folderId) {
        return firestore.collection(COLLECTION_USERS).document(userId).collection(COLLECTION_FOLDERS).document(folderId).collection(COLLECTION_NOTES);
    }
}
