package com.example.notevault.viewmodel;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.notevault.firebase.FirebaseManager;
import com.example.notevault.model.Note;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * ViewModel that holds the list of notes for a specific folder.
 * Updated to support folder-based note structure.
 */
public class NoteViewModel extends ViewModel {

    private final FirebaseManager firebaseManager;
    private final MutableLiveData<List<Note>> notesLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    private final List<Note> internalNotes = new ArrayList<>();
    private String currentFolderId;

    public NoteViewModel() {
        firebaseManager = FirebaseManager.getInstance();
    }

    public void setFolderId(String folderId) {
        this.currentFolderId = folderId;
        subscribeToNotes();
    }

    private void subscribeToNotes() {
        FirebaseUser user = firebaseManager.getCurrentUser();
        if (user == null || currentFolderId == null) {
            return;
        }

        loading.setValue(true);

        firebaseManager.listenToNotes(user.getUid(), currentFolderId, new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                loading.setValue(false);
                if (error != null) {
                    errorMessage.setValue(error.getMessage());
                    return;
                }
                internalNotes.clear();
                if (value != null) {
                    for (QueryDocumentSnapshot doc : value) {
                        Note note = doc.toObject(Note.class);
                        note.setId(doc.getId());
                        if (note.isDeleted() || note.isHidden()) {
                            continue;
                        }
                        internalNotes.add(note);
                    }
                }
                notesLiveData.setValue(new ArrayList<>(internalNotes));
            }
        });
    }

    public LiveData<List<Note>> getNotesLiveData() {
        return notesLiveData;
    }

    public LiveData<Boolean> getLoading() {
        return loading;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public void saveNote(String folderId,
                         String id,
                         String title,
                         String content,
                         String color,
                         FirebaseManager.OperationCallback callback) {
        FirebaseUser user = firebaseManager.getCurrentUser();
        if (user == null) {
            if (callback != null) callback.onComplete(false, "User not logged in");
            return;
        }

        Note note = new Note();
        note.setId(id);
        note.setFolderId(folderId);
        note.setUserId(user.getUid());
        note.setTitle(title);
        note.setContent(content);
        note.setColor(color);
        note.setTimestamp(new Date());
        note.setUpdatedAt(new Date());

        firebaseManager.addOrUpdateNote(user.getUid(), folderId, note, callback);
    }

    /** Move note to recycle bin (soft delete). */
    public void deleteNote(String noteId, FirebaseManager.OperationCallback callback) {
        FirebaseUser user = firebaseManager.getCurrentUser();
        if (user == null || currentFolderId == null) {
            if (callback != null) callback.onComplete(false, "User not logged in");
            return;
        }
        firebaseManager.moveNoteToRecycleBin(user.getUid(), currentFolderId, noteId, callback);
    }

    public void hideNote(String noteId, FirebaseManager.OperationCallback callback) {
        FirebaseUser user = firebaseManager.getCurrentUser();
        if (user == null || currentFolderId == null) {
            if (callback != null) callback.onComplete(false, "User not logged in");
            return;
        }
        firebaseManager.hideNote(user.getUid(), currentFolderId, noteId, callback);
    }

    public void lockNote(String noteId, String passwordHash, FirebaseManager.OperationCallback callback) {
        FirebaseUser user = firebaseManager.getCurrentUser();
        if (user == null || currentFolderId == null) {
            if (callback != null) callback.onComplete(false, "User not logged in");
            return;
        }
        firebaseManager.lockNote(user.getUid(), currentFolderId, noteId, passwordHash, callback);
    }

    public void unlockNote(String noteId, FirebaseManager.OperationCallback callback) {
        FirebaseUser user = firebaseManager.getCurrentUser();
        if (user == null || currentFolderId == null) {
            if (callback != null) callback.onComplete(false, "User not logged in");
            return;
        }
        firebaseManager.unlockNote(user.getUid(), currentFolderId, noteId, callback);
    }
}
