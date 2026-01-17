package com.example.focusu;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
// --- FIX: Import Realtime Database classes ---
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;


import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ProfileActivity extends AppCompatActivity {

    private ImageView profileImage;
    private TextView profileEmail;
    private EditText editProfileName;
    private Button btnUpdateName, btnUpdatePassword, btnLogout;
    private SessionManager sessionManager;
    private DatabaseReference db;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private String userId;

    private final ActivityResultLauncher<Intent> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    uploadImageToFirebaseStorage(imageUri);
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // --- FIX: Initialize Realtime Database ---
        db = FirebaseDatabase.getInstance().getReference();
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        sessionManager = new SessionManager(this);

        // Check login status
        if (!sessionManager.isLoggedIn() || currentUser == null) {
            sessionManager.logoutUser(); // Redirect to login
            return;
        }

        // Get user data from session and Firebase
        userId = currentUser.getUid();

        initViews();
        loadUserData();
        setupClickListeners();
    }

    private void initViews() {
        profileImage = findViewById(R.id.profile_image);
        profileEmail = findViewById(R.id.profile_email);
        editProfileName = findViewById(R.id.edit_profile_name);
        btnUpdateName = findViewById(R.id.btn_update_name);
        btnUpdatePassword = findViewById(R.id.btn_update_password);
        btnLogout = findViewById(R.id.btn_logout);
        findViewById(R.id.btn_back_profile).setOnClickListener(v -> finish());
    }

    private void loadUserData() {
        // --- FIX: Load data from Realtime Database ---
        db.child("users").child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    String name = dataSnapshot.child("name").getValue(String.class);
                    String email = dataSnapshot.child("email").getValue(String.class);
                    String imageUrl = dataSnapshot.child("profileImageUrl").getValue(String.class);

                    editProfileName.setText(name);
                    profileEmail.setText(email);

                    // Inside loadUserData()
                    if (imageUrl != null && !imageUrl.isEmpty()) {
                        // FIX APPLIED: Force Glide to ignore its cache and load from the file
                        Glide.with(ProfileActivity.this)
                                .load(new File(imageUrl))
                                .placeholder(R.drawable.ic_profile_placeholder)
                                .error(R.drawable.ic_profile_placeholder)
                                .skipMemoryCache(true)
                                .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.NONE)
                                .into(profileImage);
                    } else {
                        profileImage.setImageResource(R.drawable.ic_profile_placeholder);
                    }

                } else {
                    Toast.makeText(ProfileActivity.this, "User data not found.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(ProfileActivity.this, "Failed to load profile.", Toast.LENGTH_SHORT).show();
                Log.e("ProfileActivity", "Error loading user data", databaseError.toException());
            }
        });
    }

    private void setupClickListeners() {
        profileImage.setOnClickListener(v -> openGallery());
        btnUpdateName.setOnClickListener(v -> handleUpdateName());
        btnUpdatePassword.setOnClickListener(v -> handleUpdatePassword());
        btnLogout.setOnClickListener(v -> showLogoutConfirmationDialog());
    }

    private void openGallery() {
        Intent galleryIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        galleryLauncher.launch(galleryIntent);
    }

    // NEW, SAFE METHOD - DOES NOT USE STORAGE
    // NEW METHOD that saves the image locally
    private void uploadImageToFirebaseStorage(Uri imageUri) {
        if (imageUri == null) return;

        File localFile = saveUriToInternalStorage(imageUri);

        if (localFile != null) {
            updateUserProfileImageUrl(localFile.getAbsolutePath());
        } else {
            Toast.makeText(this, "Failed to save profile image.", Toast.LENGTH_SHORT).show();
        }
    }

    private File saveUriToInternalStorage(Uri uri) {
        try {
            java.io.InputStream inputStream = getContentResolver().openInputStream(uri);
            if (inputStream == null) return null;

            File directory = new File(getFilesDir(), "profile_images");
            if (!directory.exists()) {
                directory.mkdirs();
            }

            File destinationFile = new File(directory, userId + "_profile.jpg");

            if (destinationFile.exists()) {
                destinationFile.delete();
            }

            java.io.OutputStream outputStream = new java.io.FileOutputStream(destinationFile);
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }

            outputStream.close();
            inputStream.close();

            return destinationFile;

        } catch (java.io.IOException e) {
            Log.e("ProfileActivity", "Failed to save URI to internal storage", e);
            return null;
        }
    }

    @Override
    public void finish() {
        setResult(RESULT_OK);
        super.finish();
    }

    private void updateUserProfileImageUrl(String imageUrl) {
        db.child("users").child(userId).child("profileImageUrl").setValue(imageUrl)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Profile image updated!", Toast.LENGTH_SHORT).show();
                    sessionManager.saveProfileImagePath(imageUrl);
                    Glide.with(this)
                            .load(new File(imageUrl))
                            .skipMemoryCache(true)
                            .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.NONE) // Don't use the on-disk cache
                            .into(profileImage);
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to save image URL.", Toast.LENGTH_SHORT).show());
    }


    private void handleUpdateName() {
        String newName = editProfileName.getText().toString().trim();
        if (TextUtils.isEmpty(newName)) {
            Toast.makeText(this, "Name cannot be empty.", Toast.LENGTH_SHORT).show();
            return;
        }
        db.child("users").child(userId).child("name").setValue(newName)
                .addOnSuccessListener(aVoid -> {
                    // IMPORTANT: Update the session as well!
                    sessionManager.saveUserName(newName);
                    Toast.makeText(this, "Name updated successfully!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to update name.", Toast.LENGTH_SHORT).show());
    }

    private void handleUpdatePassword() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_update_password, null);
        EditText editCurrentPassword = dialogView.findViewById(R.id.edit_current_password);
        EditText editNewPassword = dialogView.findViewById(R.id.edit_new_password);

        new AlertDialog.Builder(this)
                .setTitle("Update Password")
                .setView(dialogView)
                .setPositiveButton("Update", (dialog, which) -> {
                    String currentPass = editCurrentPassword.getText().toString();
                    String newPass = editNewPassword.getText().toString();

                    if (TextUtils.isEmpty(currentPass) || TextUtils.isEmpty(newPass)) {
                        Toast.makeText(this, "Please fill in both password fields.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if(newPass.length() < 6){
                        Toast.makeText(this, "New password must be at least 6 characters.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    reauthenticateAndChangePassword(currentPass, newPass);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void reauthenticateAndChangePassword(String currentPassword, String newPassword) {
        AuthCredential credential = EmailAuthProvider.getCredential(currentUser.getEmail(), currentPassword);

        currentUser.reauthenticate(credential)
                .addOnSuccessListener(aVoid -> {
                    currentUser.updatePassword(newPassword)
                            .addOnSuccessListener(aVoid2 -> Toast.makeText(ProfileActivity.this, "Password updated successfully!", Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e -> Toast.makeText(ProfileActivity.this, "Password update failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                })
                .addOnFailureListener(e -> Toast.makeText(ProfileActivity.this, "Authentication failed. Incorrect current password.", Toast.LENGTH_SHORT).show());
    }

    private void showLogoutConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Yes", (dialog, which) -> sessionManager.logoutUser())
                .setNegativeButton("No", null)
                .show();
    }
}
