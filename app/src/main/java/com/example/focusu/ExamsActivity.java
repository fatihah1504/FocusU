package com.example.focusu;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.google.android.libraries.places.api.model.AutocompleteSessionToken;
import com.example.focusu.databinding.ActivityExamsBinding;
import com.google.android.gms.common.api.Status;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.widget.Autocomplete;
import com.google.android.libraries.places.widget.AutocompleteActivity;
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
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
    private DatabaseReference db;


    private final SimpleDateFormat displayDateFormat = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());
    private final SimpleDateFormat dbDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());

    // Variable to hold the EditText currently being edited
    private EditText currentLocationInput;

    private final ActivityResultLauncher<Intent> autocompleteLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Place place = Autocomplete.getPlaceFromIntent(result.getData());
                    if (currentLocationInput != null) {
                        // In v5.1.1, use getName() (for the place name) or getFormattedAddress()
                        String locStr = (place.getDisplayName() != null) ? place.getDisplayName() : place.getFormattedAddress();                        currentLocationInput.setText(locStr);
                    }
                } else if (result.getResultCode() == AutocompleteActivity.RESULT_ERROR) {
                    Status status = Autocomplete.getStatusFromIntent(result.getData());
                    Log.e("PLACES_ERROR", status.getStatusMessage());
                    Toast.makeText(this, "Location Error: " + status.getStatusMessage(), Toast.LENGTH_SHORT).show();
                }
            });
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityExamsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

//        if (!Places.isInitialized()) {
//            Places.initialize(getApplicationContext(), "YOUR_ACTUAL_API_KEY_HERE");
//        }

        SessionManager sessionManager = new SessionManager(this);
        userId = sessionManager.getUserId();

        if (userId == null || userId.isEmpty()) {
            sessionManager.logoutUser();
            return;
        }

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
            public void onEditClick(Exam exam) { showAddOrUpdateExamDialog(exam); }
            @Override
            public void onDeleteClick(Exam exam) { showDeleteConfirmation(exam); }
        });
        binding.examsRecyclerView.setAdapter(adapter);
    }

    private void setupClickListeners() {
        binding.btnBack.setOnClickListener(v -> finish());
        binding.fabAddExam.setOnClickListener(v -> showAddOrUpdateExamDialog(null));
        binding.btnAddNewExam.setOnClickListener(v -> showAddOrUpdateExamDialog(null));
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
        final Spinner spinnerPriority = view.findViewById(R.id.spinnerPriority);
        final EditText inputLocation = view.findViewById(R.id.editLocation);

        currentLocationInput = inputLocation;

//        inputLocation.setOnClickListener(v -> {
//            List<Place.Field> fields = Arrays.asList(
//                    Place.Field.ID,
//                    Place.Field.DISPLAY_NAME,
//                    Place.Field.FORMATTED_ADDRESS, // Use this for the address string
//                    Place.Field.LOCATION           // This replaces LatLng
//            );
//
//            Intent intent = new Autocomplete.IntentBuilder(AutocompleteActivityMode.OVERLAY, fields)
//                    .setCountries(Collections.singletonList("MY")) // Use this for version 4.0.0+
//                    .build(this);
//            autocompleteLauncher.launch(intent);
//        });

        List<Place.Field> fields = Arrays.asList(
                Place.Field.ID,
                Place.Field.DISPLAY_NAME,      // Replaces Place.Field.NAME
                Place.Field.FORMATTED_ADDRESS,
                Place.Field.LOCATION
        );

        Intent intent = new Autocomplete.IntentBuilder(AutocompleteActivityMode.OVERLAY, fields)
                .setCountries(Collections.singletonList("MY"))
                .build(this);

        setupPrioritySpinner(spinnerPriority, isUpdating ? exam.getPriority() : "Medium");
        setupDatePicker(textDate);
        setupTimePicker(textTime);

        if (isUpdating) {
            inputSubject.setText(exam.getSubject());
            if ("Quiz".equals(exam.getType())) radioGroupType.check(R.id.radioQuiz);
            else if ("Final".equals(exam.getType())) radioGroupType.check(R.id.radioFinal);
            else radioGroupType.check(R.id.radioMidterm);

            try {
                Date date = dbDateFormat.parse(exam.getDate());
                if (date != null) textDate.setText(displayDateFormat.format(date));
            } catch (Exception e) {
                textDate.setText(exam.getDate());
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

            if (isUpdating) {
                db.child("exams").child(exam.getId()).updateChildren(examData)
                        .addOnSuccessListener(aVoid -> showSuccessSnackbar("Exam Updated!"))
                        .addOnFailureListener(e -> Toast.makeText(this, "Update failed", Toast.LENGTH_SHORT).show());
            } else {
                db.child("exams").push().setValue(examData)
                        .addOnSuccessListener(aVoid -> showSuccessSnackbar("Exam Saved!"))
                        .addOnFailureListener(e -> Toast.makeText(this, "Failed to add exam", Toast.LENGTH_SHORT).show());
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    // --- HELPER METHODS ---

    private void loadExams() {
        db.child("exams").orderByChild("userId").equalTo(userId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        examList.clear();
                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            Exam exam = snapshot.getValue(Exam.class);
                            if (exam != null) {
                                exam.setId(snapshot.getKey());
                                examList.add(exam);
                            }
                        }
                        Collections.sort(examList, Comparator.comparing(Exam::getDate));
                        adapter.notifyDataSetChanged();
                        updateUI();
                        updateHeaderStats();
                    }
                    @Override public void onCancelled(@NonNull DatabaseError databaseError) {}
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

    private void setupBottomNavigation(int currentNavId) {
        binding.bottomNavigation.setSelectedItemId(currentNavId);
        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == currentNavId) return false;

            Intent intent = null;
            if (itemId == R.id.nav_home) intent = new Intent(this, MainActivity.class);
            else if (itemId == R.id.nav_assignments) intent = new Intent(this, AssignmentsActivity.class);
            else if (itemId == R.id.nav_notes) intent = new Intent(this, NotesActivity.class);
            else if (itemId == R.id.nav_recording) intent = new Intent(this, RecordingsActivity.class);

            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            }
            return true;
        });
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

    private void showDeleteConfirmation(Exam exam) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Exam")
                .setMessage("Are you sure you want to delete the " + exam.getSubject() + " exam?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    db.child("exams").child(exam.getId()).removeValue()
                            .addOnSuccessListener(aVoid -> showSuccessSnackbar("Exam Deleted!"))
                            .addOnFailureListener(e -> Toast.makeText(this, "Failed to delete", Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("No", null)
                .show();
    }

    private void showSuccessSnackbar(String message) {
        com.google.android.material.snackbar.Snackbar snackbar =
                com.google.android.material.snackbar.Snackbar.make(binding.getRoot(), message, com.google.android.material.snackbar.Snackbar.LENGTH_LONG);
        snackbar.setBackgroundTint(ContextCompat.getColor(this, R.color.snackbar_success_background));
        snackbar.setTextColor(ContextCompat.getColor(this, R.color.snackbar_text_color));
        snackbar.show();
    }
}