package com.example.notevault.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.bumptech.glide.request.RequestOptions;
import com.example.notevault.R;
import com.example.notevault.adapter.FolderAdapter;
import com.example.notevault.firebase.FirebaseManager;
import com.example.notevault.model.Folder;
import com.example.notevault.utils.ColorUtils;
import com.example.notevault.utils.PasswordHashUtil;
import com.example.notevault.viewmodel.FolderViewModel;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseUser;

import java.util.Date;
import java.util.List;

import androidx.drawerlayout.widget.DrawerLayout;

/**
 * Main folders screen with Navigation Drawer and folder overflow menu.
 */
public class FolderActivity extends AppCompatActivity implements FolderAdapter.OnFolderClickListener {

    private FolderViewModel folderViewModel;
    private FolderAdapter folderAdapter;
    private ProgressBar progressBar;
    private TextView tvEmpty;
    private DrawerLayout drawerLayout;
    private NavigationView navView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_folder);

        drawerLayout = findViewById(R.id.drawerLayout);
        navView = findViewById(R.id.navView);
        progressBar = findViewById(R.id.progressBarFolder);
        tvEmpty = findViewById(R.id.tvEmptyState);

        setupToolbar();
        setupDrawer();
        setupRecyclerView();
    }

    private void setupToolbar() {
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_menu);
        }
        toolbar.setNavigationOnClickListener(v -> drawerLayout.openDrawer(navView));
    }

    private void setupDrawer() {
        View header = navView.getHeaderView(0);
        ImageView profileImage = header.findViewById(R.id.navHeaderProfileImage);
        TextView emailView = header.findViewById(R.id.navHeaderEmail);

        FirebaseUser user = FirebaseManager.getInstance().getCurrentUser();
        if (user != null) {
            String email = user.getEmail();
            emailView.setText(email != null ? email : "");
            // Load profile photo from Firestore user doc or placeholder
            loadProfilePhoto(profileImage, user.getUid());
            profileImage.setOnClickListener(v -> openProfilePhotoPicker());
        }

        navView.setNavigationItemSelectedListener(item -> {
            drawerLayout.closeDrawers();
            int id = item.getItemId();
            if (id == R.id.nav_recycle_bin) {
                startActivity(new Intent(this, RecycleBinActivity.class));
                return true;
            }
            if (id == R.id.nav_hidden) {
                startActivity(new Intent(this, HiddenActivity.class));
                return true;
            }
            if (id == R.id.nav_logout) {
                doLogout();
                return true;
            }
            return false;
        });
    }

    private void loadProfilePhoto(ImageView imageView, String userId) {
        FirebaseManager.getInstance().getUserPhotoUrl(userId, url -> {
            if (url != null && !url.isEmpty()) {
                Glide.with(this).load(url)
                        .apply(RequestOptions.bitmapTransform(new CircleCrop()))
                        .into(imageView);
            } else {
                Glide.with(this).load(R.drawable.ic_profile_placeholder)
                        .apply(RequestOptions.bitmapTransform(new CircleCrop()))
                        .into(imageView);
            }
        });
    }

    private void openProfilePhotoPicker() {
        Intent i = new Intent(Intent.ACTION_GET_CONTENT);
        i.setType("image/*");
        startActivityForResult(Intent.createChooser(i, "Select photo"), 100);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100 && resultCode == RESULT_OK && data != null && data.getData() != null) {
            try {
                java.io.InputStream is = getContentResolver().openInputStream(data.getData());
                if (is == null) return;
                java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
                byte[] buf = new byte[8192];
                int n;
                while ((n = is.read(buf)) > 0) baos.write(buf, 0, n);
                is.close();
                byte[] bytes = baos.toByteArray();
                if (bytes.length > 0) {
                    FirebaseUser user = FirebaseManager.getInstance().getCurrentUser();
                    if (user != null) {
                        FirebaseManager.getInstance().uploadProfilePhoto(user.getUid(), bytes, new FirebaseManager.ProfilePhotoCallback() {
                            @Override
                            public void onSuccess(String downloadUrl) {
                                FirebaseManager.getInstance().updateUserPhotoUrl(user.getUid(), downloadUrl, (ok, msg) -> {
                                    if (ok) {
                                        ImageView iv = navView.getHeaderView(0).findViewById(R.id.navHeaderProfileImage);
                                        Glide.with(FolderActivity.this).load(downloadUrl).apply(RequestOptions.bitmapTransform(new CircleCrop())).into(iv);
                                        Toast.makeText(FolderActivity.this, "Profile photo updated", Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }
                            @Override
                            public void onError(String message) {
                                Toast.makeText(FolderActivity.this, "Upload failed: " + message, Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }
            } catch (Exception e) {
                Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void setupRecyclerView() {
        RecyclerView recyclerView = findViewById(R.id.recyclerViewFolders);
        FloatingActionButton fabAdd = findViewById(R.id.fabAddFolder);

        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        folderAdapter = new FolderAdapter(this);
        recyclerView.setAdapter(folderAdapter);

        folderViewModel = new ViewModelProvider(this).get(FolderViewModel.class);
        folderViewModel.getFoldersLiveData().observe(this, this::updateFolders);
        folderViewModel.getLoading().observe(this, isLoading -> {
            if (isLoading != null) progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        });
        folderViewModel.getErrorMessage().observe(this, msg -> {
            if (msg != null && !msg.isEmpty()) Toast.makeText(FolderActivity.this, msg, Toast.LENGTH_SHORT).show();
        });

        fabAdd.setOnClickListener(v -> showAddFolderDialog());
    }

    private void updateFolders(List<Folder> folders) {
        folderAdapter.setFolders(folders);
        tvEmpty.setVisibility((folders == null || folders.isEmpty()) ? View.VISIBLE : View.GONE);
    }

    private void showAddFolderDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_folder, null);
        EditText etFolderName = dialogView.findViewById(R.id.etFolderName);
        RecyclerView recyclerColors = dialogView.findViewById(R.id.recyclerColors);

        final String[] selectedColor = {ColorUtils.getDefaultColor()};
        ColorPickerAdapter colorAdapter = new ColorPickerAdapter(ColorUtils.getColorPalette(), color -> selectedColor[0] = color);
        recyclerColors.setLayoutManager(new GridLayoutManager(this, 5));
        recyclerColors.setAdapter(colorAdapter);

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.create_folder)
                .setView(dialogView)
                .setPositiveButton("Create", (dialog, which) -> {
                    String folderName = etFolderName.getText().toString().trim();
                    if (!folderName.isEmpty()) {
                        Folder folder = new Folder();
                        folder.setName(folderName);
                        folder.setColor(selectedColor[0]);
                        folder.setCreatedAt(new Date());
                        folderViewModel.saveFolder(folder);
                    } else {
                        Toast.makeText(this, "Folder name is required", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onFolderClick(Folder folder) {
        if (folder.isLocked()) {
            showUnlockFolderDialog(folder);
            return;
        }
        openFolder(folder);
    }

    private void showUnlockFolderDialog(Folder folder) {
        View v = LayoutInflater.from(this).inflate(R.layout.dialog_edittext, null);
        EditText et = v.findViewById(R.id.dialogEditText);
        et.setHint(getString(R.string.lock_enter_password));
        et.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.lock_unlock)
                .setView(v)
                .setPositiveButton("Unlock", (d, w) -> {
                    String pass = et.getText().toString();
                    if (folder.getPasswordHash() != null && PasswordHashUtil.verify(pass, folder.getPasswordHash())) {
                        openFolder(folder);
                    } else {
                        Toast.makeText(this, R.string.lock_incorrect, Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void openFolder(Folder folder) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("FOLDER_ID", folder.getId());
        intent.putExtra("FOLDER_NAME", folder.getName());
        intent.putExtra("FOLDER_COLOR", folder.getColor());
        startActivity(intent);
    }

    @Override
    public void onFolderLongClick(Folder folder) {
        // Optional: could show same overflow or do nothing (overflow is primary)
    }

    @Override
    public void onFolderOverflowClick(Folder folder, View anchor) {
        View sheet = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_folder_options, null);
        AlertDialog dialog = new MaterialAlertDialogBuilder(this).setView(sheet).create();

        TextView optionRename = sheet.findViewById(R.id.optionRename);
        TextView optionColor = sheet.findViewById(R.id.optionColor);
        TextView optionDelete = sheet.findViewById(R.id.optionDelete);
        TextView optionHide = sheet.findViewById(R.id.optionHide);
        TextView optionLock = sheet.findViewById(R.id.optionLock);

        optionLock.setText(folder.isLocked() ? R.string.folder_unlock : R.string.folder_lock);

        optionRename.setOnClickListener(v -> {
            dialog.dismiss();
            showRenameDialog(folder);
        });
        optionColor.setOnClickListener(v -> {
            dialog.dismiss();
            showChangeColorDialog(folder);
        });
        optionDelete.setOnClickListener(v -> {
            dialog.dismiss();
            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.folder_delete)
                    .setMessage("Move '" + folder.getName() + "' to Recycle Bin?")
                    .setPositiveButton("Move to Bin", (d, w) -> folderViewModel.deleteFolder(folder.getId(), (ok, msg) -> Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()))
                    .setNegativeButton("Cancel", null)
                    .show();
        });
        optionHide.setOnClickListener(v -> {
            dialog.dismiss();
            folderViewModel.hideFolder(folder.getId(), (ok, msg) -> Toast.makeText(this, msg, Toast.LENGTH_SHORT).show());
        });
        optionLock.setOnClickListener(v -> {
            dialog.dismiss();
            if (folder.isLocked()) {
                folderViewModel.unlockFolder(folder.getId(), (ok, msg) -> Toast.makeText(this, msg, Toast.LENGTH_SHORT).show());
            } else {
                showSetLockPasswordDialog(folder);
            }
        });

        dialog.show();
    }

    private void showRenameDialog(Folder folder) {
        View v = LayoutInflater.from(this).inflate(R.layout.dialog_edittext, null);
        EditText et = v.findViewById(R.id.dialogEditText);
        et.setText(folder.getName());
        et.setHint(getString(R.string.hint_folder_name));
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.folder_rename)
                .setView(v)
                .setPositiveButton("Save", (d, w) -> {
                    String name = et.getText().toString().trim();
                    if (!name.isEmpty()) {
                        folder.setName(name);
                        folderViewModel.saveFolder(folder);
                        Toast.makeText(this, "Renamed", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showChangeColorDialog(Folder folder) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_folder, null);
        EditText etFolderName = dialogView.findViewById(R.id.etFolderName);
        etFolderName.setVisibility(View.GONE);
        RecyclerView recyclerColors = dialogView.findViewById(R.id.recyclerColors);
        final String[] selectedColor = {folder.getColor()};
        ColorPickerAdapter colorAdapter = new ColorPickerAdapter(ColorUtils.getColorPalette(), c -> selectedColor[0] = c);
        recyclerColors.setLayoutManager(new GridLayoutManager(this, 5));
        recyclerColors.setAdapter(colorAdapter);
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.folder_change_color)
                .setView(dialogView)
                .setPositiveButton("Apply", (d, w) -> {
                    folder.setColor(selectedColor[0]);
                    folderViewModel.saveFolder(folder);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showSetLockPasswordDialog(Folder folder) {
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
                        folder.setLocked(true);
                        folder.setPasswordHash(hash);
                        folderViewModel.lockFolder(folder.getId(), hash, (ok, msg) -> Toast.makeText(this, msg, Toast.LENGTH_SHORT).show());
                    } else {
                        Toast.makeText(this, "Password must be at least 4 characters", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void doLogout() {
        FirebaseManager.getInstance().signOut();
        Toast.makeText(this, "Signed out", Toast.LENGTH_SHORT).show();
        startActivity(new Intent(this, LoginActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK));
        finish();
    }

    private static class ColorPickerAdapter extends RecyclerView.Adapter<ColorPickerAdapter.ColorViewHolder> {
        private final List<String> colors;
        private final OnColorSelectedListener listener;
        private int selectedPosition = 0;

        interface OnColorSelectedListener { void onColorSelected(String color); }

        ColorPickerAdapter(List<String> colors, OnColorSelectedListener listener) {
            this.colors = colors;
            this.listener = listener;
        }

        @NonNull
        @Override
        public ColorViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_color, parent, false);
            return new ColorViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ColorViewHolder holder, int position) {
            String color = colors.get(position);
            holder.bind(color, position == selectedPosition);
            holder.itemView.setOnClickListener(v -> {
                int old = selectedPosition;
                selectedPosition = holder.getAdapterPosition();
                notifyItemChanged(old);
                notifyItemChanged(selectedPosition);
                listener.onColorSelected(color);
            });
        }

        @Override
        public int getItemCount() { return colors.size(); }

        static class ColorViewHolder extends RecyclerView.ViewHolder {
            private final View colorView, selectedIndicator;

            ColorViewHolder(@NonNull View itemView) {
                super(itemView);
                colorView = itemView.findViewById(R.id.viewColor);
                selectedIndicator = itemView.findViewById(R.id.viewSelected);
            }

            void bind(String color, boolean selected) {
                colorView.setBackgroundColor(ColorUtils.parseColor(color));
                selectedIndicator.setVisibility(selected ? View.VISIBLE : View.GONE);
            }
        }
    }
}
