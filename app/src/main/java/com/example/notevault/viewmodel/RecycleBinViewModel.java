package com.example.notevault.viewmodel;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.notevault.firebase.FirebaseManager;
import com.example.notevault.model.Folder;
import com.example.notevault.model.Note;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class RecycleBinViewModel extends ViewModel {

    private final FirebaseManager firebaseManager = FirebaseManager.getInstance();
    private final MutableLiveData<List<Folder>> deletedFolders = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<Note>> deletedNotes = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);

    public RecycleBinViewModel() {
        FirebaseUser user = firebaseManager.getCurrentUser();
        if (user == null) return;
        String uid = user.getUid();
        loading.setValue(true);
        firebaseManager.listenToDeletedFolders(uid, new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                loading.setValue(false);
                List<Folder> list = new ArrayList<>();
                if (value != null) {
                    for (QueryDocumentSnapshot doc : value) {
                        Folder f = doc.toObject(Folder.class);
                        f.setId(doc.getId());
                        list.add(f);
                    }
                }
                deletedFolders.setValue(list);
            }
        });
        firebaseManager.listenToAllDeletedNotes(uid, new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                List<Note> list = new ArrayList<>();
                if (value != null) {
                    for (QueryDocumentSnapshot doc : value) {
                        Note n = doc.toObject(Note.class);
                        n.setId(doc.getId());
                        list.add(n);
                    }
                }
                deletedNotes.setValue(list);
            }
        });
    }

    public LiveData<List<Folder>> getDeletedFolders() {
        return deletedFolders;
    }

    public LiveData<List<Note>> getDeletedNotes() {
        return deletedNotes;
    }

    public LiveData<Boolean> getLoading() {
        return loading;
    }

    public void restoreFolder(String folderId, FirebaseManager.OperationCallback callback) {
        FirebaseUser user = firebaseManager.getCurrentUser();
        if (user == null) { if (callback != null) callback.onComplete(false, "Not logged in"); return; }
        firebaseManager.restoreFolder(user.getUid(), folderId, callback);
    }

    public void permanentDeleteFolder(String folderId, FirebaseManager.OperationCallback callback) {
        FirebaseUser user = firebaseManager.getCurrentUser();
        if (user == null) { if (callback != null) callback.onComplete(false, "Not logged in"); return; }
        firebaseManager.permanentDeleteFolder(user.getUid(), folderId, callback);
    }

    public void restoreNote(String folderId, String noteId, FirebaseManager.OperationCallback callback) {
        FirebaseUser user = firebaseManager.getCurrentUser();
        if (user == null) { if (callback != null) callback.onComplete(false, "Not logged in"); return; }
        firebaseManager.restoreNote(user.getUid(), folderId, noteId, callback);
    }

    public void permanentDeleteNote(String folderId, String noteId, FirebaseManager.OperationCallback callback) {
        FirebaseUser user = firebaseManager.getCurrentUser();
        if (user == null) { if (callback != null) callback.onComplete(false, "Not logged in"); return; }
        firebaseManager.permanentDeleteNote(user.getUid(), folderId, noteId, callback);
    }
}
