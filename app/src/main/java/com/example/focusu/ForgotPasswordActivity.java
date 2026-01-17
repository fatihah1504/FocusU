package com.example.focusu;
import android.util.Log;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.example.focusu.databinding.ActivityForgotPasswordBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

public class ForgotPasswordActivity extends AppCompatActivity {

    private ActivityForgotPasswordBinding binding;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityForgotPasswordBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mAuth = FirebaseAuth.getInstance();

        binding.btnUpdatePassword.setOnClickListener(v -> handleChangePassword());
        binding.btnBack.setOnClickListener(v -> finish());
    }

    private void handleChangePassword() {
        String email = binding.editEmail.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            binding.emailLayout.setError("Please enter your email");
            return;
        }

        // Show a loading toast
        Toast.makeText(this, "Requesting reset link...", Toast.LENGTH_SHORT).show();

        mAuth.sendPasswordResetEmail(email)
                .addOnSuccessListener(aVoid -> {
                    new AlertDialog.Builder(this)
                            .setTitle("Email Sent")
                            .setMessage("A reset link has been sent to " + email + ". Please check your inbox.")
                            .setPositiveButton("OK", (dialog, which) -> finish())
                            .setCancelable(false)
                            .show();
                })
                .addOnFailureListener(e -> {
                    String error = e.getMessage();
                    Log.e("AUTH_ERROR", "Reason: " + error);
                    if (error != null && error.contains("network error")) {
                        Toast.makeText(this, "Network Error: Please check your internet or emulator connection.", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(this, "Error: " + error, Toast.LENGTH_LONG).show();
                    }
                });
    }




}
