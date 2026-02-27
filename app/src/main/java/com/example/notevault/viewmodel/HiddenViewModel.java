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

public class HiddenViewModel extends ViewModel {

    private final FirebaseManager firebaseManager = FirebaseManager.getInstance();
    private final MutableLiveData<List<Folder>> hiddenFolders = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<Note>> hiddenNotes = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);

    public HiddenViewModel() {
        FirebaseUser user = firebaseManager.getCurrentUser();
        if (user == null) return;
        String uid = user.getUid();
        loading.setValue(true);
        firebaseManager.listenToHiddenFolders(uid, new EventListener<QuerySnapshot>() {
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
                hiddenFolders.setValue(list);
            }
        });
        firebaseManager.listenToAllHiddenNotes(uid, new EventListener<QuerySnapshot>() {
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
                hiddenNotes.setValue(list);
            }
        });
    }

    public LiveData<List<Folder>> getHiddenFolders() {
        return hiddenFolders;
    }

    public LiveData<List<Note>> getHiddenNotes() {
        return hiddenNotes;
    }

    public LiveData<Boolean> getLoading() {
        return loading;
    }

    public void unhideFolder(String folderId, FirebaseManager.OperationCallback callback) {
        FirebaseUser user = firebaseManager.getCurrentUser();
        if (user == null) { if (callback != null) callback.onComplete(false, "Not logged in"); return; }
        firebaseManager.unhideFolder(user.getUid(), folderId, callback);
    }

    public void unhideNote(String folderId, String noteId, FirebaseManager.OperationCallback callback) {
        FirebaseUser user = firebaseManager.getCurrentUser();
        if (user == null) { if (callback != null) callback.onComplete(false, "Not logged in"); return; }
        firebaseManager.unhideNote(user.getUid(), folderId, noteId, callback);
    }
}
