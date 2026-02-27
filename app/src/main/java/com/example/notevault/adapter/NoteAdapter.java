package com.example.notevault.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.notevault.R;
import com.example.notevault.model.Note;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * RecyclerView adapter that shows each note inside a Material-style card.
 */
public class NoteAdapter extends RecyclerView.Adapter<NoteAdapter.NoteViewHolder> {

    public interface OnNoteClickListener {
        void onNoteClick(Note note);

        void onNoteLongClick(Note note);
    }

    private final List<Note> notes = new ArrayList<>();
    private final OnNoteClickListener listener;
    private final DateFormat dateFormat;

    public NoteAdapter(OnNoteClickListener listener) {
        this.listener = listener;
        this.dateFormat = DateFormat.getDateTimeInstance();
    }

    public void setNotes(List<Note> newNotes) {
        notes.clear();
        if (newNotes != null) {
            notes.addAll(newNotes);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public NoteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_note, parent, false);
        return new NoteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NoteViewHolder holder, int position) {
        holder.bind(notes.get(position));
    }

    @Override
    public int getItemCount() {
        return notes.size();
    }

    class NoteViewHolder extends RecyclerView.ViewHolder {

        private final TextView tvTitle;
        private final TextView tvContent;
        private final TextView tvTimestamp;
        private final CardView cardNote;
        private final View colorStrip;

        NoteViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvContent = itemView.findViewById(R.id.tvContent);
            tvTimestamp = itemView.findViewById(R.id.tvTimestamp);
            cardNote = itemView.findViewById(R.id.cardNote);
            colorStrip = itemView.findViewById(R.id.colorStrip);
        }

        void bind(final Note note) {
            tvTitle.setText(note.getTitle());
            tvContent.setText(note.getContent());
            if (note.getTimestamp() != null) {
                tvTimestamp.setText(dateFormat.format(note.getTimestamp()));
            } else {
                tvTimestamp.setText("");
            }
            
            // Set color accent if available
            if (note.getColor() != null && colorStrip != null) {
                try {
                    int color = android.graphics.Color.parseColor(note.getColor());
                    colorStrip.setBackgroundColor(color);
                } catch (Exception e) {
                    // Use default color if parsing fails
                }
            }

            cardNote.setOnClickListener(v -> {
                if (listener != null) listener.onNoteClick(note);
            });

            cardNote.setOnLongClickListener(v -> {
                if (listener != null) listener.onNoteLongClick(note);
                return true;
            });
        }
    }
}

