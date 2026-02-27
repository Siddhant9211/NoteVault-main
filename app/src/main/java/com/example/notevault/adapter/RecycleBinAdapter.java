package com.example.notevault.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.notevault.R;
import com.example.notevault.model.Folder;
import com.example.notevault.model.Note;
import com.google.android.material.button.MaterialButton;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for Recycle Bin: shows deleted folders and notes with Restore / Delete permanently.
 */
public class RecycleBinAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_FOLDER = 0;
    private static final int TYPE_NOTE = 1;

    public interface RecycleBinListener {
        void onRestoreFolder(Folder folder);
        void onDeletePermanentFolder(Folder folder);
        void onRestoreNote(Note note);
        void onDeletePermanentNote(Note note);
    }

    private final List<Folder> folders = new ArrayList<>();
    private final List<Note> notes = new ArrayList<>();
    private final RecycleBinListener listener;
    private final DateFormat dateFormat = DateFormat.getDateTimeInstance();

    public RecycleBinAdapter(RecycleBinListener listener) {
        this.listener = listener;
    }

    public void setItems(List<Folder> folderList, List<Note> noteList) {
        folders.clear();
        notes.clear();
        if (folderList != null) folders.addAll(folderList);
        if (noteList != null) notes.addAll(noteList);
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        if (position < folders.size()) return TYPE_FOLDER;
        return TYPE_NOTE;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_FOLDER) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_recycle_folder, parent, false);
            return new FolderHolder(v);
        } else {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_recycle_note, parent, false);
            return new NoteHolder(v);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof FolderHolder) {
            Folder f = folders.get(position);
            ((FolderHolder) holder).bind(f);
        } else {
            int noteIndex = position - folders.size();
            Note n = notes.get(noteIndex);
            ((NoteHolder) holder).bind(n);
        }
    }

    @Override
    public int getItemCount() {
        return folders.size() + notes.size();
    }

    class FolderHolder extends RecyclerView.ViewHolder {
        TextView tvFolderName, tvDeletedAt;
        MaterialButton btnRestore, btnDelete;

        FolderHolder(@NonNull View itemView) {
            super(itemView);
            tvFolderName = itemView.findViewById(R.id.tvFolderName);
            tvDeletedAt = itemView.findViewById(R.id.tvDeletedAt);
            btnRestore = itemView.findViewById(R.id.btnRestore);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }

        void bind(Folder folder) {
            tvFolderName.setText(folder.getName());
            tvDeletedAt.setText(folder.getDeletedAt() != null ? dateFormat.format(folder.getDeletedAt()) : "");
            btnRestore.setOnClickListener(v -> listener.onRestoreFolder(folder));
            btnDelete.setOnClickListener(v -> listener.onDeletePermanentFolder(folder));
        }
    }

    class NoteHolder extends RecyclerView.ViewHolder {
        TextView tvNoteTitle, tvNoteMeta;
        MaterialButton btnRestoreNote, btnDeleteNote;

        NoteHolder(@NonNull View itemView) {
            super(itemView);
            tvNoteTitle = itemView.findViewById(R.id.tvNoteTitle);
            tvNoteMeta = itemView.findViewById(R.id.tvNoteMeta);
            btnRestoreNote = itemView.findViewById(R.id.btnRestoreNote);
            btnDeleteNote = itemView.findViewById(R.id.btnDeleteNote);
        }

        void bind(Note note) {
            tvNoteTitle.setText(note.getTitle());
            tvNoteMeta.setText(note.getDeletedAt() != null ? dateFormat.format(note.getDeletedAt()) : "");
            btnRestoreNote.setOnClickListener(v -> listener.onRestoreNote(note));
            btnDeleteNote.setOnClickListener(v -> listener.onDeletePermanentNote(note));
        }
    }
}
