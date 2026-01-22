package com.example.focusu;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import java.util.Date;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bumptech.glide.Glide;
import com.example.focusu.databinding.ActivityNotesBinding;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class NotesActivity extends AppCompatActivity {

    private ActivityNotesBinding binding;
    private NotesAdapter adapter;
    private final List<Note> filteredNotesList = new ArrayList<>();
    private final List<Note> allNotesList = new ArrayList<>();
    private String userId;
    private DatabaseReference db;

    // View for the Attachment inside the Dialog
    private ImageView dialogPreviewImage;
    private String tempImageUriString = null;
    private boolean sortByTitle = false;
    // --- Activity Result Launchers ---
    private final ActivityResultLauncher<Intent> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Bitmap imageBitmap = (Bitmap) result.getData().getExtras().get("data");
                    saveBitmapAndSetPreview(imageBitmap);
                }
            }
    );

    private final ActivityResultLauncher<String> filePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    File localFile = saveUriToInternalStorage(uri);
                    if (localFile != null) {
                        tempImageUriString = localFile.getAbsolutePath();
                        updateDialogAttachmentView();
                    }
                }
            }
    );

    private final ActivityResultLauncher<String> requestPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> {
                if (isGranted) launchCamera();
                else Toast.makeText(this, "Camera permission required", Toast.LENGTH_SHORT).show();
            }
    );

    private final ActivityResultLauncher<Intent> qrScannerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    String scannedUrl = result.getData().getStringExtra("SCANNED_QR_TEXT");

                    showAddOrUpdateNoteDialog(null, scannedUrl);                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityNotesBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        SessionManager sessionManager = new SessionManager(this);
        userId = sessionManager.getUserId();

        if (userId == null) {
            sessionManager.logoutUser();
            return;
        }

        db = FirebaseDatabase.getInstance().getReference();

        setupRecyclerView();
        loadNotes();
        setupSearch();
        setupBottomNavigation(R.id.nav_notes);
        // ADD THIS: Listen for sorting chip changes
        binding.sortChipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (!checkedIds.isEmpty()) {
                int checkedId = checkedIds.get(0);
                sortByTitle = (checkedId == R.id.chipSortTitle);

                filter(binding.searchView.getQuery().toString());
            }
        });
        binding.btnBack.setOnClickListener(v -> finish());
        binding.fabAddNote.setOnClickListener(v -> showAddOrUpdateNoteDialog(null, null));    }

    private void setupRecyclerView() {
        binding.notesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new NotesAdapter(filteredNotesList, new NotesAdapter.OnItemClickListener() {
            @Override
            public void onEditClick(Note note) {
                showAddOrUpdateNoteDialog(note, null);
            }

            @Override
            public void onDeleteClick(Note note) {
                showDeleteConfirmation(note);
            }

            @Override
            public void onImageClick(Note note) {
                String path = note.getImagePath();
                if (path == null || path.isEmpty()) return;

                File file = new File(path);
                String lower = path.toLowerCase();
                if (lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png")) {
                    Intent intent = new Intent(NotesActivity.this, ImagePreviewActivity.class);
                    intent.putExtra(ImagePreviewActivity.EXTRA_IMAGE_PATH, path);
                    startActivity(intent);
                } else {
                    openDocument(file);
                }
            }
        });
        binding.notesRecyclerView.setAdapter(adapter);
    }

    private void processQrCode(Bitmap bitmap, EditText editContent) {
        com.google.mlkit.vision.common.InputImage image = com.google.mlkit.vision.common.InputImage.fromBitmap(bitmap, 0);

        // Correct scanner initialization
        com.google.mlkit.vision.barcode.BarcodeScanner scanner = com.google.mlkit.vision.barcode.BarcodeScanning.getClient();

        scanner.process(image)
                .addOnSuccessListener(barcodes -> {
                    // Use the ML Kit Barcode class: com.google.mlkit.vision.barcode.common.Barcode
                    for (com.google.mlkit.vision.barcode.common.Barcode barcode : barcodes) {
                        String rawValue = barcode.getRawValue();
                        if (rawValue != null) {
                            String currentText = editContent.getText().toString();
                            editContent.setText(currentText + "\n\nResource Link: " + rawValue);
                            Toast.makeText(this, "URL Added!", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to read QR code", Toast.LENGTH_SHORT).show();
                });
    }
    private void openDocument(File file) {
        try {
            Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".provider", file);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, getMimeType(file.getAbsolutePath()));
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(intent, "Open with..."));
        } catch (Exception e) {
            Toast.makeText(this, "No app found to open this file.", Toast.LENGTH_SHORT).show();
        }
    }

    private String getMimeType(String path) {
        String extension = MimeTypeMap.getFileExtensionFromUrl(path);
        if (extension == null || extension.isEmpty()) {
            int i = path.lastIndexOf('.');
            if (i > 0) extension = path.substring(i + 1);
        }
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.toLowerCase());
    }

    private void loadNotes() {
        db.child("notes").orderByChild("userId").equalTo(userId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                allNotesList.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Note note = ds.getValue(Note.class);
                    if (note != null) {
                        note.setId(ds.getKey());
                        allNotesList.add(note);
                    }
                }
                Collections.sort(allNotesList, (o1, o2) -> Long.compare(o2.getTimestamp(), o1.getTimestamp()));
                filter(binding.searchView.getQuery().toString());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    private void showAddOrUpdateNoteDialog(Note note, String scannedUrl) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_note, null);
        builder.setView(view);

        EditText editTitle = view.findViewById(R.id.editNoteTitle);
        EditText editContent = view.findViewById(R.id.editNoteContent);
        dialogPreviewImage = view.findViewById(R.id.imgDialogPreview);
        Button btnScan = view.findViewById(R.id.btnScanNote);
        Button btnScanQR = view.findViewById(R.id.btnScanQR);

        Button btnAttachFile = view.findViewById(R.id.btnAttachFile);

        btnAttachFile.setOnClickListener(v -> {
            filePickerLauncher.launch("application/pdf");
        });

        if (note != null) {
            editTitle.setText(note.getTitle());
            editContent.setText(note.getContent());
            tempImageUriString = note.getImagePath();
            updateDialogAttachmentView();
        } else {
            tempImageUriString = null;

            if (scannedUrl != null) {
                editContent.setText("Resource Link: " + scannedUrl);
            }
            updateDialogAttachmentView();
        }

        btnScan.setOnClickListener(v -> checkCameraPermissionAndLaunch());
        btnScanQR.setOnClickListener(v -> checkCameraPermissionAndLaunchForQR());

        builder.setPositiveButton(note == null ? "Save" : "Update", (dialog, which) -> {
            String title = editTitle.getText().toString().trim();
            String content = editContent.getText().toString().trim();

            if (title.isEmpty()) {
                Toast.makeText(this, "Title Required", Toast.LENGTH_SHORT).show();
                return;
            }

            String currentDate = new SimpleDateFormat("dd-MMM-yyyy", Locale.getDefault()).format(new Date());

            if (note == null) {
                // Create New Note
                String id = db.child("notes").push().getKey();
                Note newNote = new Note(id, userId, title, content, tempImageUriString, currentDate, System.currentTimeMillis());
                if (id != null) {
                    db.child("notes").child(id).setValue(newNote);
                }
            } else {
                note.setTitle(title);
                note.setContent(content);
                note.setImagePath(tempImageUriString);
                note.setDate(currentDate);
                db.child("notes").child(note.getId()).setValue(note);
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.create().show();
    }



    private void checkCameraPermissionAndLaunchForQR() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            // Replace QRScannerActivity.class with your actual QR scanning activity
            Intent intent = new Intent(this, QRScannerActivity.class);
            qrScannerLauncher.launch(intent);
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }


    private void updateDialogAttachmentView() {
        if (tempImageUriString == null || tempImageUriString.isEmpty()) {
            dialogPreviewImage.setVisibility(View.GONE);
            return;
        }
        dialogPreviewImage.setVisibility(View.VISIBLE);
        String path = tempImageUriString.toLowerCase();
        if (path.endsWith(".jpg") || path.endsWith(".jpeg") || path.endsWith(".png")) {
            Glide.with(this).load(new File(tempImageUriString)).into(dialogPreviewImage);
        } else {
            dialogPreviewImage.setImageResource(R.drawable.ic_pdf_file);
        }
    }

    private File saveUriToInternalStorage(Uri uri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            File directory = new File(getFilesDir(), "note_attachments");
            if (!directory.exists()) directory.mkdirs();

            String extension = ".file";
            String mime = getContentResolver().getType(uri);
            if (mime != null) {
                String ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(mime);
                if (ext != null) extension = "." + ext;
            }

            File destFile = new File(directory, UUID.randomUUID().toString() + extension);
            FileOutputStream out = new FileOutputStream(destFile);
            byte[] buffer = new byte[1024];
            int bytes;
            while ((bytes = inputStream.read(buffer)) != -1) out.write(buffer, 0, bytes);
            out.close();
            inputStream.close();
            return destFile;
        } catch (IOException e) {
            return null;
        }
    }

    private void saveBitmapAndSetPreview(Bitmap bitmap) {
        File directory = new File(getFilesDir(), "note_attachments");
        if (!directory.exists()) directory.mkdirs();
        File file = new File(directory, UUID.randomUUID().toString() + ".jpg");
        try (FileOutputStream out = new FileOutputStream(file)) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
            tempImageUriString = file.getAbsolutePath();
            updateDialogAttachmentView();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void setupSearch() {
        binding.searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String q) {
                filter(q);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String q) {
                filter(q);
                return true;
            }
        });
    }


    private void filter(String text) {
        // 1. Start with the full list of notes
        List<Note> tempList = new ArrayList<>(allNotesList);

        // 2. Apply Sorting logic
        if (sortByTitle) {
            // Sort alphabetically by Title (A-Z)
            Collections.sort(tempList, (o1, o2) ->
                    o1.getTitle().toLowerCase().compareTo(o2.getTitle().toLowerCase()));
        } else {
            // Sort by Date/Timestamp (Newest First)
            Collections.sort(tempList, (o1, o2) ->
                    Long.compare(o2.getTimestamp(), o1.getTimestamp()));
        }

        // 3. Apply Search filtering
        filteredNotesList.clear();
        String query = text.toLowerCase(Locale.ROOT);
        for (Note note : tempList) {
            if (query.isEmpty() ||
                    note.getTitle().toLowerCase().contains(query) ||
                    note.getContent().toLowerCase().contains(query)) {
                filteredNotesList.add(note);
            }
        }

        // 4. Update the UI
        adapter.notifyDataSetChanged();
        updateUI();
    }

    private void updateUI() {
        if (filteredNotesList.isEmpty()) {
            binding.emptyStateLayout.setVisibility(View.VISIBLE);
            binding.notesRecyclerView.setVisibility(View.GONE);
            binding.subtitleText.setText("No notes found");
        } else {
            binding.emptyStateLayout.setVisibility(View.GONE);
            binding.notesRecyclerView.setVisibility(View.VISIBLE);
            binding.subtitleText.setText(allNotesList.size() + " notes saved");
        }
    }
    private void showDeleteConfirmation(Note note) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Note")
                .setMessage("Are you sure you want to delete this note?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    db.child("notes").child(note.getId()).removeValue()
                            .addOnSuccessListener(aVoid -> Toast.makeText(this, "Note deleted", Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void setupBottomNavigation(int id) {
        binding.bottomNavigation.setSelectedItemId(id);
        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            int target = item.getItemId();
            if (target == id) return false;

            Intent intent = null;
            if (target == R.id.nav_home) intent = new Intent(this, MainActivity.class);
            else if (target == R.id.nav_assignments)
                intent = new Intent(this, AssignmentsActivity.class);
            else if (target == R.id.nav_exam) intent = new Intent(this, ExamsActivity.class);
            else if (target == R.id.nav_recording)
                intent = new Intent(this, RecordingsActivity.class);

            if (intent != null) {
                startActivity(intent);
                finish();
            }
            return true;
        });
    }


    private void checkCameraPermissionAndLaunch() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
            launchCamera();
        else requestPermissionLauncher.launch(Manifest.permission.CAMERA);
    }

    private void launchCamera() {
        cameraLauncher.launch(new Intent(MediaStore.ACTION_IMAGE_CAPTURE));
    }


}