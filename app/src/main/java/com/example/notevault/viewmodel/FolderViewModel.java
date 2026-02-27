package com.example.notevault.viewmodel;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.notevault.firebase.FirebaseManager;
import com.example.notevault.model.Folder;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * ViewModel that manages folders for the current user.
 */
public class FolderViewModel extends ViewModel {

    private final FirebaseManager firebaseManager;
    private final MutableLiveData<List<Folder>> foldersLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    private final List<Folder> internalFolders = new ArrayList<>();

    public FolderViewModel() {
        firebaseManager = FirebaseManager.getInstance();
        subscribeToFolders();
    }

    private void subscribeToFolders() {
        FirebaseUser user = firebaseManager.getCurrentUser();
        if (user == null) {
            return;
        }

        loading.setValue(true);

        firebaseManager.listenToFolders(user.getUid(), new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                loading.setValue(false);
                if (error != null) {
                    errorMessage.setValue(error.getMessage());
                    return;
                }
                internalFolders.clear();
                if (value != null) {
                    for (QueryDocumentSnapshot doc : value) {
                        Folder folder = doc.toObject(Folder.class);
                        folder.setId(doc.getId());
                        if (folder.isDeleted() || folder.isHidden()) {
                            continue;
                        }
                        internalFolders.add(folder);
                    }
                }
                foldersLiveData.setValue(new ArrayList<>(internalFolders));
            }
        });
    }

    public LiveData<List<Folder>> getFoldersLiveData() {
        return foldersLiveData;
    }

    public LiveData<Boolean> getLoading() {
        return loading;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public void saveFolder(Folder folder) {
        FirebaseUser user = firebaseManager.getCurrentUser();
        if (user == null) {
            errorMessage.setValue("User not logged in");
            return;
        }

        firebaseManager.addOrUpdateFolder(user.getUid(), folder, (success, message) -> {
            if (!success) {
                errorMessage.setValue(message);
            }
        });
    }

    /** Move folder to recycle bin (soft delete). */
    public void deleteFolder(String folderId, FirebaseManager.OperationCallback callback) {
        FirebaseUser user = firebaseManager.getCurrentUser();
        if (user == null) {
            if (callback != null) callback.onComplete(false, "User not logged in");
            return;
        }
        firebaseManager.moveFolderToRecycleBin(user.getUid(), folderId, callback);
    }

    public void hideFolder(String folderId, FirebaseManager.OperationCallback callback) {
        FirebaseUser user = firebaseManager.getCurrentUser();
        if (user == null) {
            if (callback != null) callback.onComplete(false, "User not logged in");
            return;
        }
        firebaseManager.hideFolder(user.getUid(), folderId, callback);
    }

    public void lockFolder(String folderId, String passwordHash, FirebaseManager.OperationCallback callback) {
        FirebaseUser user = firebaseManager.getCurrentUser();
        if (user == null) {
            if (callback != null) callback.onComplete(false, "User not logged in");
            return;
        }
        firebaseManager.lockFolder(user.getUid(), folderId, passwordHash, callback);
    }

    public void unlockFolder(String folderId, FirebaseManager.OperationCallback callback) {
        FirebaseUser user = firebaseManager.getCurrentUser();
        if (user == null) {
            if (callback != null) callback.onComplete(false, "User not logged in");
            return;
        }
        firebaseManager.unlockFolder(user.getUid(), folderId, callback);
    }
}
