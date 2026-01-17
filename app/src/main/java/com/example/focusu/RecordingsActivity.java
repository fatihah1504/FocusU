package com.example.focusu;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.focusu.databinding.ActivityRecordingsBinding;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;


import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class RecordingsActivity extends AppCompatActivity {

    private static final String TAG = "RecordingsActivity";
    private ActivityRecordingsBinding binding;

    // Firebase Services
    private DatabaseReference db;
    private String userId;

    // UI and Adapter
    private RecordingsAdapter adapter;
    private final List<Recording> filteredRecordingList = new ArrayList<>();
    private final List<Recording> recordingList = new ArrayList<>();

    // Media Recording
    private MediaRecorder mediaRecorder;
    private String currentRecordingFilePath = null;
    private boolean isRecording = false;
    private long recordingStartTime = 0;
    private boolean sortBySubject = false;
    // Media Playback
    private MediaPlayer mediaPlayer;
    private Recording currentlyPlaying = null;

    // Permissions Launcher
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    startRecording();
                } else {
                    Toast.makeText(this, "Audio permission is required to record.", Toast.LENGTH_LONG).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRecordingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // --- 1. INITIALIZE FIREBASE and SESSION ---
        db = FirebaseDatabase.getInstance().getReference();
//        storage = FirebaseStorage.getInstance();

        SessionManager sessionManager = new SessionManager(this);
        userId = sessionManager.getUserId();

        if (userId == null || userId.isEmpty()) {
            Toast.makeText(this, "Session expired. Please log in.", Toast.LENGTH_SHORT).show();
            sessionManager.logoutUser();
            return;
        }

        // --- 2. SETUP UI and ADAPTER ---
        setupRecyclerView();
        setupClickListeners();
        setupBottomNavigation(R.id.nav_recording);
        setupSearch();
        // --- 3. LOAD DATA FROM FIREBASE ---
        loadRecordings();

        // Inside onCreate
        binding.sortChipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (!checkedIds.isEmpty()) {
                int checkedId = checkedIds.get(0);
                sortBySubject = (checkedId == R.id.chipSubject);

                // Refresh with current search text
                filter(binding.searchView.getQuery().toString());
            }
        });
    }
    private void applySortAndRefresh() {
        if (sortBySubject) {
            // Sort alphabetically by Subject (A-Z)
            Collections.sort(recordingList, (o1, o2) -> {
                String s1 = (o1.getSubject() != null) ? o1.getSubject() : "";
                String s2 = (o2.getSubject() != null) ? o2.getSubject() : "";
                return s1.toLowerCase().compareTo(s2.toLowerCase());
            });
        } else {
            // Default: Sort by Timestamp (Newest first)
            Collections.sort(recordingList, (o1, o2) ->
                    Long.compare(o2.getTimestamp(), o1.getTimestamp()));
        }
        adapter.notifyDataSetChanged();
    }

    private void setupRecyclerView() {
        binding.recordingsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        // Use filteredRecordingList here
        adapter = new RecordingsAdapter(filteredRecordingList, new RecordingsAdapter.OnItemClickListener() {
            @Override
            public void onPlayClick(Recording recording) { handlePlayback(recording); }
            @Override
            public void onMenuClick(Recording recording) { showActionModal(recording); }
        });
        binding.recordingsRecyclerView.setAdapter(adapter);
    }

    private void setupSearch() {
        binding.searchView.setOnQueryTextListener(new androidx.appcompat.widget.SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                filter(query);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filter(newText);
                return true;
            }
        });
    }
    private void filter(String text) {
        List<Recording> tempList = new ArrayList<>(recordingList);


        if (sortBySubject) {
            Collections.sort(tempList, (o1, o2) -> {
                String s1 = (o1.getSubject() != null) ? o1.getSubject() : "";
                String s2 = (o2.getSubject() != null) ? o2.getSubject() : "";
                return s1.toLowerCase().compareTo(s2.toLowerCase());
            });
        } else {
            Collections.sort(tempList, (o1, o2) -> Long.compare(o2.getTimestamp(), o1.getTimestamp()));
        }

        filteredRecordingList.clear();
        String query = text.toLowerCase(Locale.ROOT);

        for (Recording rec : tempList) {
            if (query.isEmpty() ||
                    rec.getTitle().toLowerCase().contains(query) ||
                    (rec.getSubject() != null && rec.getSubject().toLowerCase().contains(query))) {
                filteredRecordingList.add(rec);
            }
        }

        adapter.notifyDataSetChanged();
        updateUI();
    }


    private void setupClickListeners() {
        binding.btnBack.setOnClickListener(v -> finish());
        binding.fabRecord.setOnClickListener(v -> toggleRecording());
        binding.btnStartRecording.setOnClickListener(v -> toggleRecording());
        binding.btnStopRecording.setOnClickListener(v -> toggleRecording());
    }

    private void loadRecordings() {
        Query userRecordingsQuery = db.child("recordings").orderByChild("userId").equalTo(userId);
        userRecordingsQuery.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                recordingList.clear();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Recording recording = snapshot.getValue(Recording.class);
                    if (recording != null) {
                        recording.setId(snapshot.getKey());
                        recordingList.add(recording);
                    }
                }
                filter(binding.searchView.getQuery().toString());
                applySortAndRefresh();

                updateUI();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "Error loading recordings", databaseError.toException());
            }
        });
    }


    private void toggleRecording() {
        if (isRecording) {
            stopRecordingAndShowSaveDialog();
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                startRecording();
            } else {
                requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO);
            }
        }
    }

    private void startRecording() {
        stopPlayback();
        File storageDir = getExternalFilesDir("recordings");
        if (storageDir == null) {
            Toast.makeText(this, "Failed to access storage.", Toast.LENGTH_LONG).show();
            return;
        }
        if (!storageDir.exists()) storageDir.mkdirs();
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        currentRecordingFilePath = new File(storageDir, "REC_" + timeStamp + ".3gp").getAbsolutePath();

        mediaRecorder = new MediaRecorder();
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mediaRecorder.setOutputFile(currentRecordingFilePath);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

        try {
            mediaRecorder.prepare();
            mediaRecorder.start();
            isRecording = true;
            recordingStartTime = System.currentTimeMillis();

            binding.fabRecord.setVisibility(View.GONE);
            binding.emptyStateLayout.setVisibility(View.GONE);
            binding.bottomNavigation.setVisibility(View.GONE);
            binding.recordingStateLayout.setVisibility(View.VISIBLE);
            binding.chronometer.setBase(android.os.SystemClock.elapsedRealtime());
            binding.chronometer.start();

        } catch (IOException e) {
            Log.e(TAG, "mediaRecorder.prepare() failed", e);
            Toast.makeText(this, "Recording failed to start", Toast.LENGTH_SHORT).show();
        }
    }

    private void stopRecordingAndShowSaveDialog() {
        if (!isRecording || mediaRecorder == null) return;
        try {
            binding.chronometer.stop();
            mediaRecorder.stop();
            mediaRecorder.release();
            mediaRecorder = null;
            isRecording = false;
            long duration = System.currentTimeMillis() - recordingStartTime;
            if (duration > 1000) {
                showSaveRecordingDialog(duration, null);
            } else {
                new File(currentRecordingFilePath).delete();
                Toast.makeText(this, "Recording too short, discarded.", Toast.LENGTH_SHORT).show();
                resetRecordingUI();
            }
        } catch (RuntimeException stopException) {
            Log.e(TAG, "Stop failed", stopException);
            if (currentRecordingFilePath != null) new File(currentRecordingFilePath).delete();
            resetRecordingUI();
        }
    }

    private void resetRecordingUI() {
        binding.recordingStateLayout.setVisibility(View.GONE);
        binding.fabRecord.setVisibility(View.VISIBLE);
        binding.bottomNavigation.setVisibility(View.VISIBLE);
        updateUI();
    }



    private void showSaveRecordingDialog(long duration, Recording recordingToUpdate) {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_save_recording, null);
        final EditText inputTitle = view.findViewById(R.id.editTitle);
        final EditText inputSubject = view.findViewById(R.id.editSubject);

        boolean isUpdating = (recordingToUpdate != null);

        if (isUpdating) {
            inputTitle.setText(recordingToUpdate.getTitle());
            inputSubject.setText(recordingToUpdate.getSubject());
        }

        new AlertDialog.Builder(this)
                .setTitle(isUpdating ? "Update Recording" : "Save Recording")
                .setView(view)
                .setPositiveButton(isUpdating ? "Update" : "Save", (dialog, which) -> {
                    String title = inputTitle.getText().toString().trim();
                    String subject = inputSubject.getText().toString().trim();

                    if (title.isEmpty()) title = "Recording " + new SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(new Date());

                    if (isUpdating) {
                        updateRecordingMetadata(recordingToUpdate.getId(), title, subject);
                    } else {
                        uploadRecordingToStorage(title, subject, duration);
                    }
                    resetRecordingUI();
                })
                .setNegativeButton("Discard", (dialog, which) -> {
                    if (!isUpdating) {
                        new File(currentRecordingFilePath).delete();
                    }
                    resetRecordingUI();
                })
                .setCancelable(false)
                .show();
    }
    private void updateRecordingMetadata(String recordingId, String title, String subject) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("title", title);
        updates.put("subject", subject);

        db.child("recordings").child(recordingId).updateChildren(updates)
                .addOnSuccessListener(aVoid -> Toast.makeText(this, "Recording Updated!", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(this, "Update failed", Toast.LENGTH_SHORT).show());
    }
    private void uploadRecordingToStorage(String title, String subject, long duration) {
        if (currentRecordingFilePath == null || currentRecordingFilePath.isEmpty()) {
            Toast.makeText(this, "Error: Recording file not found.", Toast.LENGTH_SHORT).show();
            return;
        }
        saveRecordingMetadataToDatabase(title, subject, duration, currentRecordingFilePath);
    }



    private void saveRecordingMetadataToDatabase(String title, String subject, long duration, String localFilePath) {
        Map<String, Object> recordingData = new HashMap<>();
        recordingData.put("title", title);
        recordingData.put("subject", subject);
        recordingData.put("duration", duration);
        recordingData.put("timestamp", System.currentTimeMillis());
        recordingData.put("filePath", localFilePath);
        recordingData.put("userId", userId);

        db.child("recordings").push().setValue(recordingData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Recording Saved!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Database save failed", e);
                    Toast.makeText(RecordingsActivity.this, "Failed to save metadata.", Toast.LENGTH_SHORT).show();
                    if (currentRecordingFilePath != null) {
                        new File(currentRecordingFilePath).delete();
                    }
                });
    }


    private void handlePlayback(Recording recordingToPlay) {
        // If a recording is currently playing, stop it.
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
            if (currentlyPlaying != null) {
                currentlyPlaying.setPlaying(false);
            }
        }

        if (currentlyPlaying == recordingToPlay) {
            currentlyPlaying = null;
            adapter.notifyDataSetChanged();
            return;
        }

        stopRecordingAndShowSaveDialog();

        currentlyPlaying = recordingToPlay;
        currentlyPlaying.setPlaying(true);

        mediaPlayer = new MediaPlayer();
        try {
            mediaPlayer.setDataSource(recordingToPlay.getFilePath());
            mediaPlayer.setOnPreparedListener(MediaPlayer::start);
            mediaPlayer.prepareAsync();

            mediaPlayer.setOnCompletionListener(mp -> {

                if (currentlyPlaying != null) {
                    currentlyPlaying.setPlaying(false);
                    currentlyPlaying = null;
                    adapter.notifyDataSetChanged(); // Update the UI
                }
            });

        } catch (IOException e) {
            Log.e(TAG, "handlePlayback: mediaPlayer.prepare() failed", e);
            Toast.makeText(this, "Failed to play recording.", Toast.LENGTH_SHORT).show();
            if (currentlyPlaying != null) {
                currentlyPlaying.setPlaying(false);
                currentlyPlaying = null;
            }
        }

        for (Recording rec : recordingList) {
            if (rec != currentlyPlaying) {
                rec.setPlaying(false);
            }
        }
        adapter.notifyDataSetChanged();
    }


    private void startPlayback(Recording recording) {
        if (recording.getFilePath() == null || recording.getFilePath().isEmpty()) {
            Toast.makeText(this, "Recording path not found.", Toast.LENGTH_SHORT).show();
            return;
        }
        currentlyPlaying = recording;
        currentlyPlaying.setPlaying(true);
        adapter.notifyDataSetChanged();
        mediaPlayer = new MediaPlayer();
        try {
            mediaPlayer.setDataSource(recording.getFilePath());
            mediaPlayer.prepareAsync();
            mediaPlayer.setOnPreparedListener(mp -> {
                mp.start();
                Toast.makeText(this, "Playing...", Toast.LENGTH_SHORT).show();
            });
            mediaPlayer.setOnCompletionListener(mp -> stopPlayback());
            mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                Toast.makeText(this, "Playback Error", Toast.LENGTH_SHORT).show();
                stopPlayback();
                return true;
            });
        } catch (IOException e) {
            Log.e(TAG, "startPlayback failed", e);
            Toast.makeText(this, "Could not play recording.", Toast.LENGTH_SHORT).show();
            stopPlayback();
        }
    }

    private void pausePlayback() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) mediaPlayer.pause();
        if (currentlyPlaying != null) {
            currentlyPlaying.setPlaying(false);
            adapter.notifyDataSetChanged();
        }
    }

    private void resumePlayback() {
        if (mediaPlayer != null && !mediaPlayer.isPlaying()) mediaPlayer.start();
        if (currentlyPlaying != null) {
            currentlyPlaying.setPlaying(true);
            adapter.notifyDataSetChanged();
        }
    }

    private void stopPlayback() {
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
        if (currentlyPlaying != null) {
            currentlyPlaying.setPlaying(false);
            currentlyPlaying = null;
            adapter.notifyDataSetChanged();
        }
    }

    private void showActionModal(Recording recording) {

        String[] options = {"Edit Name/Subject", "Delete"};
        new AlertDialog.Builder(this)
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        showSaveRecordingDialog(recording.getDuration(), recording);
                    } else if (which == 1) {
                        showDeleteConfirmation(recording);
                    }
                }).show();
    }

    private void showDeleteConfirmation(Recording recording) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Recording")
                .setMessage("Are you sure you want to delete this recording?")
                .setPositiveButton("Delete", (dialog, which) -> deleteRecordingFromFirebase(recording))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteRecordingFromFirebase(Recording recording) {
        if (recording.getFilePath() != null && !recording.getFilePath().isEmpty()) {
            File fileToDelete = new File(recording.getFilePath());
            if (fileToDelete.exists()) {
                if (fileToDelete.delete()) {
                    Log.d(TAG, "Local recording file deleted successfully.");
                } else {
                    Log.e(TAG, "Failed to delete local recording file.");
                }
            }
        }

        db.child("recordings").child(recording.getId()).removeValue()
                .addOnSuccessListener(aVoid -> showSuccessSnackbar("Recording Deleted!"))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error deleting recording from database", e);
                    Toast.makeText(this, "Failed to delete recording.", Toast.LENGTH_SHORT).show();
                });
    }



    private void updateUI() {
        if (recordingList.isEmpty()) {
            binding.emptyStateLayout.setVisibility(View.VISIBLE);
            binding.recordingsRecyclerView.setVisibility(View.GONE);
            binding.subtitleText.setText("No recordings yet");
        } else {
            binding.emptyStateLayout.setVisibility(View.GONE);
            binding.recordingsRecyclerView.setVisibility(View.VISIBLE);
            long totalDurationMs = 0;
            for (Recording r : recordingList) totalDurationMs += r.getDuration();
            long totalMin = java.util.concurrent.TimeUnit.MILLISECONDS.toMinutes(totalDurationMs);
            binding.subtitleText.setText(recordingList.size() + " recordings • " + totalMin + "m total");
        }
    }

    private void setupBottomNavigation(int currentNavId) {
        binding.bottomNavigation.setSelectedItemId(currentNavId);
        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == currentNavId) return false;

            Intent intent = null;
            if (itemId == R.id.nav_home) intent = new Intent(this, MainActivity.class);
            else if (itemId == R.id.nav_assignments) intent = new Intent(this, AssignmentsActivity.class);
            else if (itemId == R.id.nav_exam) intent = new Intent(this, ExamsActivity.class);
            else if (itemId == R.id.nav_notes) intent = new Intent(this, NotesActivity.class);

            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            }
            return true;
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        stopPlayback();
        if (mediaRecorder != null) {
            mediaRecorder.release();
            mediaRecorder = null;
        }
    }
    private void showSuccessSnackbar(String message) {
        View rootView = binding.getRoot();

        com.google.android.material.snackbar.Snackbar snackbar = com.google.android.material.snackbar.Snackbar.make(rootView, message, com.google.android.material.snackbar.Snackbar.LENGTH_LONG);

        snackbar.setBackgroundTint(ContextCompat.getColor(this, R.color.snackbar_success_background));
        // Set text color to white
        snackbar.setTextColor(ContextCompat.getColor(this, R.color.snackbar_text_color));

        snackbar.show();
    }
}
