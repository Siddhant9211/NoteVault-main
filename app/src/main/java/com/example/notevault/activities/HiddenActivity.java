package com.example.notevault.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.notevault.R;
import com.example.notevault.adapter.HiddenAdapter;
import com.example.notevault.model.Folder;
import com.example.notevault.model.Note;
import com.example.notevault.viewmodel.HiddenViewModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Shows hidden folders and notes. Tap Unhide to restore to main list.
 */
public class HiddenActivity extends AppCompatActivity implements HiddenAdapter.HiddenListener {

    private HiddenViewModel viewModel;
    private HiddenAdapter adapter;
    private ProgressBar progressBar;
    private TextView tvEmpty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hidden);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> finish());

        progressBar = findViewById(R.id.progressBarHidden);
        tvEmpty = findViewById(R.id.tvEmptyHidden);
        RecyclerView recyclerView = findViewById(R.id.recyclerHidden);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new HiddenAdapter(this);
        recyclerView.setAdapter(adapter);

        viewModel = new ViewModelProvider(this).get(HiddenViewModel.class);

        final List<Folder>[] lastFolders = new List[]{new ArrayList<>()};
        final List<Note>[] lastNotes = new List[]{new ArrayList<>()};
        viewModel.getHiddenFolders().observe(this, folders -> {
            lastFolders[0] = folders != null ? folders : new ArrayList<>();
            adapter.setItems(lastFolders[0], lastNotes[0]);
            tvEmpty.setVisibility((lastFolders[0].isEmpty() && lastNotes[0].isEmpty()) ? View.VISIBLE : View.GONE);
        });
        viewModel.getHiddenNotes().observe(this, notes -> {
            lastNotes[0] = notes != null ? notes : new ArrayList<>();
            adapter.setItems(lastFolders[0], lastNotes[0]);
            tvEmpty.setVisibility((lastFolders[0].isEmpty() && lastNotes[0].isEmpty()) ? View.VISIBLE : View.GONE);
        });
        viewModel.getLoading().observe(this, loading -> {
            if (loading != null) progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        });
    }

    @Override
    public void onUnhideFolder(Folder folder) {
        viewModel.unhideFolder(folder.getId(), (ok, msg) -> Toast.makeText(this, msg, Toast.LENGTH_SHORT).show());
    }

    @Override
    public void onUnhideNote(Note note) {
        if (note.getFolderId() == null) return;
        viewModel.unhideNote(note.getFolderId(), note.getId(), (ok, msg) -> Toast.makeText(this, msg, Toast.LENGTH_SHORT).show());
    }
}
