package com.example.focusu;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.focusu.databinding.ActivitySignupBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Map;
import java.util.HashMap;


public class SignupActivity extends AppCompatActivity {

    private ActivitySignupBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySignupBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnSignUp.setOnClickListener(v -> handleSignUp());

        binding.btnLogin.setOnClickListener(v -> {
            finish();
        });
    }

    private void handleSignUp() {
        String name = binding.editName.getText().toString().trim();
        String email = binding.editEmail.getText().toString().trim();
        String password = binding.editPassword.getText().toString().trim();
        String confirmPassword = binding.editConfirmPassword.getText().toString().trim();

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.emailLayout.setError("Enter a valid email address");
            return;
        } else {
            binding.emailLayout.setError(null);
        }
        if (!password.equals(confirmPassword)) {
            binding.confirmPasswordLayout.setError("Passwords do not match");
            return;
        } else {
            binding.confirmPasswordLayout.setError(null);
        }

        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = mAuth.getCurrentUser();
                        String uid = firebaseUser.getUid();

                        Map<String, Object> user = new HashMap<>();
                        user.put("uid", uid);
                        user.put("name", name);
                        user.put("email", email);

                        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference("users");
                        dbRef.child(uid).setValue(user)
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(SignupActivity.this, "Account Created. Please Login.", Toast.LENGTH_LONG).show();
                                    finish();
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(SignupActivity.this, "Database Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });

                    } else {
                        Toast.makeText(SignupActivity.this, "Authentication failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }


}
