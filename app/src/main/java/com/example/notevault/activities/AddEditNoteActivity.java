package com.example.notevault.activities;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.notevault.R;
import com.example.notevault.utils.ColorUtils;
import com.example.notevault.viewmodel.NoteViewModel;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;

import java.util.List;

/**
 * Screen where user can create a new note or edit an existing one.
 * Now includes color selection for notes.
 */
public class AddEditNoteActivity extends AppCompatActivity {

    public static final String EXTRA_NOTE_ID = "extra_note_id";
    public static final String EXTRA_NOTE_TITLE = "extra_note_title";
    public static final String EXTRA_NOTE_CONTENT = "extra_note_content";
    public static final String EXTRA_NOTE_COLOR = "extra_note_color";

    private EditText etTitle;
    private EditText etContent;
    private ProgressBar progressBar;
    private View colorPreview;
    private String currentNoteId;
    private String folderId;
    private String selectedColor;

    private NoteViewModel noteViewModel;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_note);

        MaterialToolbar toolbar = findViewById(R.id.toolbarAddEdit);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        // Get folder context
        folderId = getIntent().getStringExtra("FOLDER_ID");
        String folderColor = getIntent().getStringExtra("FOLDER_COLOR");
        
        if (folderId == null) {
            Toast.makeText(this, "Error: No folder context", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        // Set toolbar color
        if (folderColor != null) {
            toolbar.setBackgroundColor(ColorUtils.parseColor(folderColor));
        }

        etTitle = findViewById(R.id.etTitle);
        etContent = findViewById(R.id.etContent);
        progressBar = findViewById(R.id.progressBarAddEdit);
        colorPreview = findViewById(R.id.colorPreview);
        MaterialButton btnSave = findViewById(R.id.btnSave);
        MaterialButton btnPickColor = findViewById(R.id.btnPickColor);

        noteViewModel = new ViewModelProvider(this).get(NoteViewModel.class);

        currentNoteId = getIntent().getStringExtra(EXTRA_NOTE_ID);
        String title = getIntent().getStringExtra(EXTRA_NOTE_TITLE);
        String content = getIntent().getStringExtra(EXTRA_NOTE_CONTENT);
        selectedColor = getIntent().getStringExtra(EXTRA_NOTE_COLOR);

        if (selectedColor == null) {
            selectedColor = ColorUtils.getDefaultColor();
        }

        if (title != null) {
            etTitle.setText(title);
        }
        if (content != null) {
            etContent.setText(content);
        }
        
        updateColorPreview();

        btnSave.setOnClickListener(v -> saveNote());
        btnPickColor.setOnClickListener(v -> showColorPicker());
    }

    private void showColorPicker() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_color_picker, null);
        RecyclerView recyclerColors = dialogView.findViewById(R.id.recyclerColors);

        ColorPickerAdapter colorAdapter = new ColorPickerAdapter(ColorUtils.getColorPalette(), color -> {
            selectedColor = color;
            updateColorPreview();
        });
        recyclerColors.setLayoutManager(new GridLayoutManager(this, 5));
        recyclerColors.setAdapter(colorAdapter);

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Choose Color")
                .setView(dialogView)
                .setPositiveButton("OK", null)
                .show();
    }

    private void updateColorPreview() {
        if (colorPreview != null && selectedColor != null) {
            colorPreview.setBackgroundColor(ColorUtils.parseColor(selectedColor));
        }
    }

    private void saveNote() {
        String title = etTitle.getText().toString().trim();
        String content = etContent.getText().toString().trim();

        if (TextUtils.isEmpty(title)) {
            etTitle.setError("Title is required");
            return;
        }
        if (TextUtils.isEmpty(content)) {
            etContent.setError("Content is required");
            return;
        }

        setLoading(true);

        noteViewModel.saveNote(folderId, currentNoteId, title, content, selectedColor, (success, message) -> {
            setLoading(false);
            Toast.makeText(AddEditNoteActivity.this, message, Toast.LENGTH_SHORT).show();
            if (success) {
                finish();
            }
        });
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
    }

    /**
     * Inner adapter for color selection
     */
    private static class ColorPickerAdapter extends RecyclerView.Adapter<ColorPickerAdapter.ColorViewHolder> {
        
        private final List<String> colors;
        private final OnColorSelectedListener listener;
        private int selectedPosition = 0;

        interface OnColorSelectedListener {
            void onColorSelected(String color);
        }

        ColorPickerAdapter(List<String> colors, OnColorSelectedListener listener) {
            this.colors = colors;
            this.listener = listener;
        }

        @androidx.annotation.NonNull
        @Override
        public ColorViewHolder onCreateViewHolder(@androidx.annotation.NonNull android.view.ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_color, parent, false);
            return new ColorViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@androidx.annotation.NonNull ColorViewHolder holder, int position) {
            String color = colors.get(position);
            holder.bind(color, position == selectedPosition);
            holder.itemView.setOnClickListener(v -> {
                int oldPosition = selectedPosition;
                selectedPosition = holder.getAdapterPosition();
                notifyItemChanged(oldPosition);
                notifyItemChanged(selectedPosition);
                listener.onColorSelected(color);
            });
        }

        @Override
        public int getItemCount() {
            return colors.size();
        }

        static class ColorViewHolder extends RecyclerView.ViewHolder {
            private final View colorView;
            private final View selectedIndicator;

            ColorViewHolder(@androidx.annotation.NonNull View itemView) {
                super(itemView);
                colorView = itemView.findViewById(R.id.viewColor);
                selectedIndicator = itemView.findViewById(R.id.viewSelected);
            }

            void bind(String color, boolean isSelected) {
                colorView.setBackgroundColor(ColorUtils.parseColor(color));
                selectedIndicator.setVisibility(isSelected ? View.VISIBLE : View.GONE);
            }
        }
    }
}
