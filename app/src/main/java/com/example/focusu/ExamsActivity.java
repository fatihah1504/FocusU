package com.example.focusu;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.focusu.databinding.ActivityExamsBinding;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ExamsActivity extends AppCompatActivity {

    private ActivityExamsBinding binding;
    private ExamsAdapter adapter;
    private final List<Exam> examList = new ArrayList<>();
    private String userId;

    // --- Realtime Database Reference ---
    private DatabaseReference db;

    // Define consistent date/time formats
    private final SimpleDateFormat displayDateFormat = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());
    private final SimpleDateFormat dbDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault()); // 12-hour format with AM/PM

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityExamsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        SessionManager sessionManager = new SessionManager(this);
        userId = sessionManager.getUserId();

        // If userId is null, the user is not logged in. Redirect them.
        if (userId == null || userId.isEmpty()) {
            Toast.makeText(this, "Session expired. Please log in again.", Toast.LENGTH_SHORT).show();
            sessionManager.logoutUser();
            return;
        }

        // --- Initialize Realtime Database ---
        db = FirebaseDatabase.getInstance().getReference();

        setupRecyclerView();
        setupClickListeners();
        setupBottomNavigation(R.id.nav_exam);

        loadExams();
    }

    private void setupRecyclerView() {
        binding.examsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ExamsAdapter(examList, new ExamsAdapter.OnItemClickListener() {
            @Override
            public void onEditClick(Exam exam) {
                showAddOrUpdateExamDialog(exam);
            }

            @Override
            public void onDeleteClick(Exam exam) {
                showDeleteConfirmation(exam);
            }
        });
        binding.examsRecyclerView.setAdapter(adapter);
    }

    private void setupClickListeners() {
        binding.btnBack.setOnClickListener(v -> finish());
        binding.fabAddExam.setOnClickListener(v -> showAddOrUpdateExamDialog(null));
        binding.btnAddNewExam.setOnClickListener(v -> showAddOrUpdateExamDialog(null));
    }

    private void setupBottomNavigation(int currentNavId) {
        binding.bottomNavigation.setSelectedItemId(currentNavId);
        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == currentNavId) {
                return false;
            }
            Intent intent = null;
            if (itemId == R.id.nav_home) {
                intent = new Intent(this, MainActivity.class);
            } else if (itemId == R.id.nav_assignments) {
                intent = new Intent(this, AssignmentsActivity.class);
            } else if (itemId == R.id.nav_notes) {
                intent = new Intent(this, NotesActivity.class);
            } else if (itemId == R.id.nav_recording) {
                intent = new Intent(this, RecordingsActivity.class);
            }

            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            }
            return true;
        });
    }

    private void loadExams() {
        // Create a query to get exams for the current user, ordered by date
        Query userExamsQuery = db.child("exams").orderByChild("userId").equalTo(userId);

        userExamsQuery.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                examList.clear();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Exam exam = snapshot.getValue(Exam.class);
                    if (exam != null) {
                        exam.setId(snapshot.getKey()); // Store the unique key from Realtime DB
                        examList.add(exam);
                    }
                }
                Collections.sort(examList, Comparator.comparing(Exam::getDate));

                adapter.notifyDataSetChanged();
                updateUI();
                updateHeaderStats();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(ExamsActivity.this, "Failed to load exams: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateHeaderStats() {
        String todayDbFormat = dbDateFormat.format(new Date());
        long completedCount = examList.stream().filter(e -> e.getDate() != null && e.getDate().compareTo(todayDbFormat) < 0).count();
        long upcomingCount = examList.size() - completedCount;

        binding.statTotalExamsCount.setText(String.valueOf(examList.size()));
        binding.statUpcomingCount.setText(String.valueOf(upcomingCount));
        binding.statCompletedCount.setText(String.valueOf(completedCount));
        binding.subtitleText.setText(new SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(new Date()));
    }

    private void updateUI() {
        if (examList.isEmpty()) {
            binding.emptyStateLayout.setVisibility(View.VISIBLE);
            binding.examsRecyclerView.setVisibility(View.GONE);
        } else {
            binding.emptyStateLayout.setVisibility(View.GONE);
            binding.examsRecyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void showAddOrUpdateExamDialog(final Exam exam) {
        final boolean isUpdating = exam != null;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(isUpdating ? "Edit Exam" : "Add New Exam");

        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_exam, null);
        builder.setView(view);

        final EditText inputSubject = view.findViewById(R.id.editSubject);
        final RadioGroup radioGroupType = view.findViewById(R.id.radioGroupExamType);
        final TextView textDate = view.findViewById(R.id.textDate);
        final TextView textTime = view.findViewById(R.id.textTime);
        final EditText inputLocation = view.findViewById(R.id.editLocation);
        final Spinner spinnerPriority = view.findViewById(R.id.spinnerPriority);

        setupPrioritySpinner(spinnerPriority, isUpdating ? exam.getPriority() : "Medium");
        setupDatePicker(textDate);
        setupTimePicker(textTime);

        if (isUpdating) {
            inputSubject.setText(exam.getSubject());
            if ("Quiz".equals(exam.getType())) radioGroupType.check(R.id.radioQuiz);
            else if ("Final".equals(exam.getType())) radioGroupType.check(R.id.radioFinal);
            else radioGroupType.check(R.id.radioMidterm);

            try {
                // Set date and time from existing exam object
                Date date = dbDateFormat.parse(exam.getDate());
                if (date != null) textDate.setText(displayDateFormat.format(date));
            } catch (Exception e) {
                textDate.setText(exam.getDate()); // Fallback to raw string
            }
            textTime.setText(exam.getTime());
            inputLocation.setText(exam.getLocation());
        }

        builder.setPositiveButton(isUpdating ? "Update" : "Save", (dialog, which) -> {
            String subject = inputSubject.getText().toString().trim();
            int selectedTypeId = radioGroupType.getCheckedRadioButtonId();
            RadioButton selectedRadioButton = view.findViewById(selectedTypeId);
            String type = selectedRadioButton.getText().toString();
            String displayDate = textDate.getText().toString().trim();
            String time = textTime.getText().toString().trim();
            String location = inputLocation.getText().toString().trim();
            String priority = spinnerPriority.getSelectedItem().toString();

            if (subject.isEmpty() || displayDate.equals("Select Date") || time.equals("Select Time")) {
                Toast.makeText(this, "Subject, Date, and Time are required!", Toast.LENGTH_SHORT).show();
                return;
            }

            String dbDate;
            try {
                Date parsedDate = displayDateFormat.parse(displayDate);
                dbDate = (parsedDate != null) ? dbDateFormat.format(parsedDate) : "";
            } catch (ParseException e) {
                Toast.makeText(this, "Invalid date format.", Toast.LENGTH_SHORT).show();
                return;
            }

            Map<String, Object> examData = new HashMap<>();
            examData.put("subject", subject);
            examData.put("type", type);
            examData.put("date", dbDate);
            examData.put("time", time);
            examData.put("location", location);
            examData.put("priority", priority);
            examData.put("userId", userId);
            examData.put("timestamp", System.currentTimeMillis());
            DatabaseReference examsRef = db.child("exams");

            if (isUpdating) {
                examsRef.child(exam.getId()).updateChildren(examData)
                        .addOnSuccessListener(aVoid -> showSuccessSnackbar("Exam Updated!"))
                        .addOnFailureListener(e -> Toast.makeText(this, "Update failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            } else {
                examsRef.push().setValue(examData)
                        .addOnSuccessListener(aVoid -> showSuccessSnackbar("Exam Updated!"))
                        .addOnFailureListener(e -> Toast.makeText(this, "Failed to add exam: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void showDeleteConfirmation(Exam exam) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Exam")
                .setMessage("Are you sure you want to delete the " + exam.getSubject() + " exam?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    db.child("exams").child(exam.getId()).removeValue()
                            .addOnSuccessListener(aVoid ->showSuccessSnackbar("Exam Updated!"))
                            .addOnFailureListener(e -> Toast.makeText(this, "Failed to delete: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("No", null)
                .show();
    }

    private void setupPrioritySpinner(Spinner spinner, String currentPriority) {
        String[] priorities = {"High", "Medium", "Low"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, priorities);
        spinner.setAdapter(adapter);
        if (currentPriority != null) {
            for (int i = 0; i < priorities.length; i++) {
                if (priorities[i].equals(currentPriority)) {
                    spinner.setSelection(i);
                    break;
                }
            }
        }
    }

    private void setupDatePicker(TextView textDate) {
        textDate.setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            if (!textDate.getText().toString().equals("Select Date")) {
                try {
                    Date date = displayDateFormat.parse(textDate.getText().toString());
                    if (date != null) cal.setTime(date);
                } catch (ParseException e) {

                }
            }
            new DatePickerDialog(this, (view, year, month, day) -> {
                Calendar selectedCal = Calendar.getInstance();
                selectedCal.set(year, month, day);
                textDate.setText(displayDateFormat.format(selectedCal.getTime()));
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
        });
    }

    private void setupTimePicker(TextView textTime) {
        textTime.setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            new TimePickerDialog(this, (view, hourOfDay, minute) -> {
                Calendar selectedTimeCal = Calendar.getInstance();
                selectedTimeCal.set(Calendar.HOUR_OF_DAY, hourOfDay);
                selectedTimeCal.set(Calendar.MINUTE, minute);
                textTime.setText(timeFormat.format(selectedTimeCal.getTime()));
            }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), false).show();
        });
    }

    // Add this method inside the AssignmentsActivity class
    private void showSuccessSnackbar(String message) {
        // A Snackbar needs a root view to attach to. The binding.getRoot() is perfect for this.
        View rootView = binding.getRoot();

        com.google.android.material.snackbar.Snackbar snackbar = com.google.android.material.snackbar.Snackbar.make(rootView, message, com.google.android.material.snackbar.Snackbar.LENGTH_LONG);

        snackbar.setBackgroundTint(ContextCompat.getColor(this, R.color.snackbar_success_background));
        // Set text color to white
        snackbar.setTextColor(ContextCompat.getColor(this, R.color.snackbar_text_color));

        snackbar.show();
    }
}
