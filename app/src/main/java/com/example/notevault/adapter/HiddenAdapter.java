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

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for Hidden screen. Long-press to unhide folder or note.
 */
public class HiddenAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_FOLDER = 0;
    private static final int TYPE_NOTE = 1;

    public interface HiddenListener {
        void onUnhideFolder(Folder folder);
        void onUnhideNote(Note note);
    }

    private final List<Folder> folders = new ArrayList<>();
    private final List<Note> notes = new ArrayList<>();
    private final HiddenListener listener;

    public HiddenAdapter(HiddenListener listener) {
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
        return position < folders.size() ? TYPE_FOLDER : TYPE_NOTE;
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
            ((FolderHolder) holder).bind(folders.get(position));
        } else {
            ((NoteHolder) holder).bind(notes.get(position - folders.size()));
        }
    }

    @Override
    public int getItemCount() {
        return folders.size() + notes.size();
    }

    class FolderHolder extends RecyclerView.ViewHolder {
        TextView tvFolderName, tvDeletedAt;
        View btnRestore, btnDelete;

        FolderHolder(@NonNull View itemView) {
            super(itemView);
            tvFolderName = itemView.findViewById(R.id.tvFolderName);
            tvDeletedAt = itemView.findViewById(R.id.tvDeletedAt);
            btnRestore = itemView.findViewById(R.id.btnRestore);
            btnDelete = itemView.findViewById(R.id.btnDelete);
            btnRestore.setVisibility(View.VISIBLE);
            btnDelete.setVisibility(View.GONE);
            if (btnRestore instanceof MaterialButton) ((MaterialButton) btnRestore).setText(R.string.hidden_unhide);
        }

        void bind(Folder folder) {
            tvFolderName.setText(folder.getName());
            tvDeletedAt.setText("Folder");
            btnRestore.setOnClickListener(v -> listener.onUnhideFolder(folder));
        }
    }

    class NoteHolder extends RecyclerView.ViewHolder {
        TextView tvNoteTitle, tvNoteMeta;
        View btnRestoreNote, btnDeleteNote;

        NoteHolder(@NonNull View itemView) {
            super(itemView);
            tvNoteTitle = itemView.findViewById(R.id.tvNoteTitle);
            tvNoteMeta = itemView.findViewById(R.id.tvNoteMeta);
            btnRestoreNote = itemView.findViewById(R.id.btnRestoreNote);
            btnDeleteNote = itemView.findViewById(R.id.btnDeleteNote);
            btnDeleteNote.setVisibility(View.GONE);
            if (btnRestoreNote instanceof MaterialButton) ((MaterialButton) btnRestoreNote).setText(R.string.hidden_unhide);
        }

        void bind(Note note) {
            tvNoteTitle.setText(note.getTitle());
            tvNoteMeta.setText("Note");
            btnRestoreNote.setOnClickListener(v -> listener.onUnhideNote(note));
        }
    }
}
