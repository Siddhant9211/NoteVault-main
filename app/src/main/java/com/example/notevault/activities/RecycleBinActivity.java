package com.example.notevault.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.notevault.R;
import com.example.notevault.adapter.RecycleBinAdapter;
import com.example.notevault.firebase.FirebaseManager;
import com.example.notevault.model.Folder;
import com.example.notevault.model.Note;
import com.example.notevault.viewmodel.RecycleBinViewModel;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * Shows deleted folders and notes. Restore or permanently delete within 30 days.
 */
public class RecycleBinActivity extends AppCompatActivity implements RecycleBinAdapter.RecycleBinListener {

    private RecycleBinViewModel viewModel;
    private RecycleBinAdapter adapter;
    private ProgressBar progressBar;
    private TextView tvEmpty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recycle_bin);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        progressBar = findViewById(R.id.progressBarRecycle);
        tvEmpty = findViewById(R.id.tvEmptyRecycle);
        RecyclerView recyclerView = findViewById(R.id.recyclerRecycleBin);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new RecycleBinAdapter(this);
        recyclerView.setAdapter(adapter);

        viewModel = new ViewModelProvider(this).get(RecycleBinViewModel.class);

        // Cleanup items older than 30 days when opening recycle bin
        if (FirebaseManager.getInstance().getCurrentUser() != null) {
            FirebaseManager.getInstance().cleanupOldRecycleBinItems(
                    FirebaseManager.getInstance().getCurrentUser().getUid(), (ok, msg) -> {});
        }

        final List<Folder>[] lastFolders = new List[]{new ArrayList<>()};
        final List<Note>[] lastNotes = new List[]{new ArrayList<>()};
        viewModel.getDeletedFolders().observe(this, folders -> {
            lastFolders[0] = folders != null ? folders : new ArrayList<>();
            adapter.setItems(lastFolders[0], lastNotes[0]);
            tvEmpty.setVisibility((lastFolders[0].isEmpty() && lastNotes[0].isEmpty()) ? View.VISIBLE : View.GONE);
        });
        viewModel.getDeletedNotes().observe(this, notes -> {
            lastNotes[0] = notes != null ? notes : new ArrayList<>();
            adapter.setItems(lastFolders[0], lastNotes[0]);
            tvEmpty.setVisibility((lastFolders[0].isEmpty() && lastNotes[0].isEmpty()) ? View.VISIBLE : View.GONE);
        });
        viewModel.getLoading().observe(this, loading -> {
            if (loading != null) progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        });
    }

    @Override
    public void onRestoreFolder(Folder folder) {
        viewModel.restoreFolder(folder.getId(), (ok, msg) -> {
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public void onDeletePermanentFolder(Folder folder) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.recycle_delete_permanent)
                .setMessage("Permanently delete folder '" + folder.getName() + "' and all its notes?")
                .setPositiveButton("Delete", (d, w) -> viewModel.permanentDeleteFolder(folder.getId(), (ok, msg) -> Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()))
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onRestoreNote(Note note) {
        if (note.getFolderId() == null) return;
        viewModel.restoreNote(note.getFolderId(), note.getId(), (ok, msg) -> Toast.makeText(this, msg, Toast.LENGTH_SHORT).show());
    }

    @Override
    public void onDeletePermanentNote(Note note) {
        if (note.getFolderId() == null) return;
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.recycle_delete_permanent)
                .setMessage("Permanently delete this note?")
                .setPositiveButton("Delete", (d, w) -> viewModel.permanentDeleteNote(note.getFolderId(), note.getId(), (ok, msg) -> Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()))
                .setNegativeButton("Cancel", null)
                .show();
    }
}
