package com.example.notevault.adapter;

import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.notevault.R;
import com.example.notevault.model.Folder;
import com.example.notevault.utils.ColorUtils;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;

/**
 * RecyclerView adapter for folders in a grid.
 * Supports overflow menu: Rename, Color, Delete, Hide, Lock.
 */
public class FolderAdapter extends RecyclerView.Adapter<FolderAdapter.FolderViewHolder> {

    public interface OnFolderClickListener {
        void onFolderClick(Folder folder);
        void onFolderLongClick(Folder folder);
        void onFolderOverflowClick(Folder folder, View anchor);
    }

    private final List<Folder> folders = new ArrayList<>();
    private final OnFolderClickListener listener;

    public FolderAdapter(OnFolderClickListener listener) {
        this.listener = listener;
    }

    public void setFolders(List<Folder> newFolders) {
        folders.clear();
        if (newFolders != null) {
            folders.addAll(newFolders);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public FolderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_folder, parent, false);
        return new FolderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FolderViewHolder holder, int position) {
        holder.bind(folders.get(position));
    }

    @Override
    public int getItemCount() {
        return folders.size();
    }

    class FolderViewHolder extends RecyclerView.ViewHolder {

        private final TextView tvFolderName;
        private final MaterialCardView cardFolder;
        private final View colorBar;
        private final ImageView btnFolderOverflow;

        FolderViewHolder(@NonNull View itemView) {
            super(itemView);
            tvFolderName = itemView.findViewById(R.id.tvFolderName);
            cardFolder = itemView.findViewById(R.id.cardFolder);
            colorBar = itemView.findViewById(R.id.colorBar);
            btnFolderOverflow = itemView.findViewById(R.id.btnFolderOverflow);
        }

        void bind(final Folder folder) {
            tvFolderName.setText(folder.getName());

            int color = ColorUtils.parseColor(folder.getColor());
            GradientDrawable drawable = new GradientDrawable();
            drawable.setColor(color);
            drawable.setCornerRadius(8f);
            colorBar.setBackground(drawable);

            cardFolder.setCardBackgroundColor(ColorUtils.getLighterColor(folder.getColor()));

            cardFolder.setOnClickListener(v -> {
                if (listener != null) listener.onFolderClick(folder);
            });

            cardFolder.setOnLongClickListener(v -> {
                if (listener != null) listener.onFolderLongClick(folder);
                return true;
            });

            btnFolderOverflow.setOnClickListener(v -> {
                if (listener != null) listener.onFolderOverflowClick(folder, v);
            });
        }
    }
}
