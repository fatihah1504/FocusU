package com.example.focusu;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.example.focusu.databinding.ActivityLoginBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;


public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnLogin.setOnClickListener(v -> {
            String email = binding.editEmail.getText().toString().trim();
            String password = binding.editPassword.getText().toString().trim();

            if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
                Toast.makeText(LoginActivity.this, "Please enter email and password", Toast.LENGTH_SHORT).show();
                return;
            }

            FirebaseAuth mAuth = FirebaseAuth.getInstance();
            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, task -> {
                        if (task.isSuccessful()) {
                            FirebaseUser firebaseUser = mAuth.getCurrentUser();
                            String uid = firebaseUser.getUid();

                            DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(uid);
                            userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                    if (dataSnapshot.exists()) {
                                        String userName = dataSnapshot.child("name").getValue(String.class);
                                        String imageUrl = dataSnapshot.child("profileImageUrl").getValue(String.class);
                                        SessionManager sessionManager = new SessionManager(LoginActivity.this);
                                        sessionManager.createLoginSession(uid, userName, email, imageUrl);

                                        Toast.makeText(LoginActivity.this, "Login Successful!", Toast.LENGTH_SHORT).show();
                                        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                                        intent.putExtra("SHOW_POPUP_ON_LOAD", true);
                                        startActivity(intent);
                                        finish();
                                    } else {
                                        Toast.makeText(LoginActivity.this, "User data not found in database.", Toast.LENGTH_SHORT).show();
                                        mAuth.signOut();
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError databaseError) {
                                    Toast.makeText(LoginActivity.this, "Failed to fetch user data: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                                    mAuth.signOut();
                                }
                            });
                        } else {
                            Toast.makeText(LoginActivity.this, "Authentication failed. Check your email and password.", Toast.LENGTH_LONG).show();
                        }
                    });
        });

        binding.btnSignUp.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, SignupActivity.class);
            startActivity(intent);
        });

        binding.btnForgotPassword.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, ForgotPasswordActivity.class);
            startActivity(intent);
        });

    }



}
