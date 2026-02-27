package com.example.notevault.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.notevault.R;
import com.example.notevault.adapter.NoteAdapter;
import com.example.notevault.model.Note;
import com.example.notevault.utils.ColorUtils;
import com.example.notevault.utils.PasswordHashUtil;
import com.example.notevault.viewmodel.NoteViewModel;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import android.view.LayoutInflater;
import android.widget.EditText;

import java.util.List;

/**
 * Shows all notes within a specific folder.
 * Replaces the old flat note list with folder-organized notes.
 */
public class MainActivity extends AppCompatActivity implements NoteAdapter.OnNoteClickListener {

    private NoteViewModel noteViewModel;
    private NoteAdapter noteAdapter;
    private ProgressBar progressBar;
    private TextView tvEmpty;
    
    private String folderId;
    private String folderName;
    private String folderColor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Get folder details from intent
        folderId = getIntent().getStringExtra("FOLDER_ID");
        folderName = getIntent().getStringExtra("FOLDER_NAME");
        folderColor = getIntent().getStringExtra("FOLDER_COLOR");
        
        if (folderId == null) {
            Toast.makeText(this, "Error: No folder selected", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(folderName != null ? folderName : "Notes");
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
        
        // Set toolbar color based on folder color
        if (folderColor != null) {
            toolbar.setBackgroundColor(ColorUtils.parseColor(folderColor));
        }

        RecyclerView recyclerView = findViewById(R.id.recyclerViewNotes);
        FloatingActionButton fabAdd = findViewById(R.id.fabAddNote);
        progressBar = findViewById(R.id.progressBarMain);
        tvEmpty = findViewById(R.id.tvEmptyState);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        noteAdapter = new NoteAdapter(this);
        recyclerView.setAdapter(noteAdapter);

        noteViewModel = new ViewModelProvider(this).get(NoteViewModel.class);
        noteViewModel.setFolderId(folderId); // Set the folder context

        noteViewModel.getNotesLiveData().observe(this, this::updateNotes);
        noteViewModel.getLoading().observe(this, isLoading -> {
            if (isLoading != null) {
                progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            }
        });
        noteViewModel.getErrorMessage().observe(this, msg -> {
            if (msg != null && !msg.isEmpty()) {
                Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
            }
        });

        fabAdd.setOnClickListener(v -> openAddEditNote(null));
    }

    private void updateNotes(List<Note> notes) {
        noteAdapter.setNotes(notes);
        boolean isEmpty = notes == null || notes.isEmpty();
        tvEmpty.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
    }

    private void openAddEditNote(Note note) {
        Intent intent = new Intent(this, AddEditNoteActivity.class);
        intent.putExtra("FOLDER_ID", folderId);
        intent.putExtra("FOLDER_COLOR", folderColor);
        if (note != null) {
            intent.putExtra(AddEditNoteActivity.EXTRA_NOTE_ID, note.getId());
            intent.putExtra(AddEditNoteActivity.EXTRA_NOTE_TITLE, note.getTitle());
            intent.putExtra(AddEditNoteActivity.EXTRA_NOTE_CONTENT, note.getContent());
            intent.putExtra(AddEditNoteActivity.EXTRA_NOTE_COLOR, note.getColor());
        }
        startActivity(intent);
    }

    @Override
    public void onNoteClick(Note note) {
        if (note.isLocked()) {
            showUnlockNoteDialog(note);
            return;
        }
        openAddEditNote(note);
    }

    private void showUnlockNoteDialog(Note note) {
        View v = LayoutInflater.from(this).inflate(R.layout.dialog_edittext, null);
        EditText et = v.findViewById(R.id.dialogEditText);
        et.setHint(getString(R.string.lock_enter_password));
        et.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.lock_unlock)
                .setView(v)
                .setPositiveButton("Unlock", (d, w) -> {
                    String pass = et.getText().toString();
                    if (note.getPasswordHash() != null && PasswordHashUtil.verify(pass, note.getPasswordHash())) {
                        openAddEditNote(note);
                    } else {
                        Toast.makeText(this, R.string.lock_incorrect, Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onNoteLongClick(Note note) {
        // Long press: show options (Delete to bin, Hide, Lock)
        showNoteOptionsDialog(note);
    }

    private void showNoteOptionsDialog(Note note) {
        String[] options = new String[]{getString(R.string.folder_delete), getString(R.string.folder_hide), note.isLocked() ? getString(R.string.folder_unlock) : getString(R.string.folder_lock)};
        new MaterialAlertDialogBuilder(this)
                .setTitle(note.getTitle())
                .setItems(options, (d, which) -> {
                    if (which == 0) {
                        noteViewModel.deleteNote(note.getId(), (ok, msg) -> Toast.makeText(this, msg, Toast.LENGTH_SHORT).show());
                    } else if (which == 1) {
                        noteViewModel.hideNote(note.getId(), (ok, msg) -> Toast.makeText(this, msg, Toast.LENGTH_SHORT).show());
                    } else {
                        if (note.isLocked()) {
                            noteViewModel.unlockNote(note.getId(), (ok, msg) -> Toast.makeText(this, msg, Toast.LENGTH_SHORT).show());
                        } else {
                            showSetLockPasswordNoteDialog(note);
                        }
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showSetLockPasswordNoteDialog(Note note) {
        View v = LayoutInflater.from(this).inflate(R.layout.dialog_edittext, null);
        EditText et = v.findViewById(R.id.dialogEditText);
        et.setHint(getString(R.string.lock_set_password));
        et.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.folder_lock)
                .setView(v)
                .setPositiveButton("Lock", (d, w) -> {
                    String pass = et.getText().toString();
                    if (pass.length() >= 4) {
                        String hash = PasswordHashUtil.hash(pass);
                        noteViewModel.lockNote(note.getId(), hash, (ok, msg) -> Toast.makeText(this, msg, Toast.LENGTH_SHORT).show());
                    } else {
                        Toast.makeText(this, "Password must be at least 4 characters", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true; // No menu needed here
    }
}
