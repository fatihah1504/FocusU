package com.example.focusu;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.focusu.databinding.ActivityAssignmentsBinding;
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

public class AssignmentsActivity extends AppCompatActivity {

    private ActivityAssignmentsBinding binding;
    private AssignmentsAdapter adapter;
    private final List<Assignment> filteredList = new ArrayList<>();
    private final List<Assignment> allAssignmentsList = new ArrayList<>();
    private String userId;

    // --- Database Reference ---
    private DatabaseReference db; // Changed to DatabaseReference

    private final SimpleDateFormat displayDateFormat = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());
    private final SimpleDateFormat dbDateFormat = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAssignmentsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Get user ID from session
        SessionManager sessionManager = new SessionManager(this);
        userId = sessionManager.getUserId();

        // If user is not logged in, redirect to login
        if (userId == null || userId.isEmpty()) {
            Toast.makeText(this, "Session expired. Please log in.", Toast.LENGTH_SHORT).show();
            sessionManager.logoutUser();
            return;
        }

        // Initialize Realtime Database
        db = FirebaseDatabase.getInstance().getReference();

        setupRecyclerView();
        setupClickListeners();
        setupSearchAndSort();
        setupBottomNavigation(R.id.nav_assignments);

        // Load data from Firebase
        loadAssignments();
    }

    private void setupRecyclerView() {
        binding.assignmentsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AssignmentsAdapter(filteredList, new AssignmentsAdapter.OnItemClickListener() {
            @Override
            public void onEditClick(Assignment assignment) {
                showAddOrUpdateDialog(assignment);
            }

            @Override
            public void onDeleteClick(Assignment assignment) {
                showDeleteConfirmation(assignment);
            }
        });
        binding.assignmentsRecyclerView.setAdapter(adapter);
    }

    private void setupClickListeners() {
        binding.btnBack.setOnClickListener(v -> finish());
        binding.fabAddAssignment.setOnClickListener(v -> showAddOrUpdateDialog(null));
    }

    private void loadAssignments() {
        Query userAssignmentsQuery = db.child("assignments").orderByChild("userId").equalTo(userId);

        userAssignmentsQuery.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                allAssignmentsList.clear();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Assignment assignment = snapshot.getValue(Assignment.class);
                    if (assignment != null) {
                        assignment.setId(snapshot.getKey()); // Set the key as the ID
                        allAssignmentsList.add(assignment);
                    }
                }
                sortAndFilter();
                updateUI();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(AssignmentsActivity.this, "Error loading assignments: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showAddOrUpdateDialog(final Assignment assignment) {
        final boolean isUpdating = assignment != null;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(isUpdating ? "Edit Assignment" : "New Assignment");

        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_assignment, null);
        builder.setView(view);

        final EditText inputTitle = view.findViewById(R.id.editTitle);
        final EditText inputSubject = view.findViewById(R.id.editSubject);
        final TextView textDate = view.findViewById(R.id.textDate);
        final EditText inputDesc = view.findViewById(R.id.editDescription);
        final Spinner spinnerPriority = view.findViewById(R.id.spinnerPriority);

        setupPrioritySpinner(spinnerPriority, null);
        setupDatePicker(textDate);

        if (isUpdating) {
            inputTitle.setText(assignment.getTitle());
            inputSubject.setText(assignment.getSubject());
            inputDesc.setText(assignment.getDescription());
            setupPrioritySpinner(spinnerPriority, assignment.getPriority());

            try {
                Date dateFromDb = dbDateFormat.parse(assignment.getDueDate());
                if (dateFromDb != null) {
                    textDate.setText(displayDateFormat.format(dateFromDb));
                    textDate.setTag(dbDateFormat.format(dateFromDb));
                }
            } catch (ParseException e) {
                textDate.setText("Select Date");
                textDate.setTag(null);
            }
        }

        builder.setPositiveButton(isUpdating ? "Update" : "Add", (dialog, which) -> {
            String title = inputTitle.getText().toString().trim();
            String subject = inputSubject.getText().toString().trim();
            String date = textDate.getTag() != null ? textDate.getTag().toString() : "";
            String desc = inputDesc.getText().toString().trim();
            String priority = spinnerPriority.getSelectedItem().toString();

            if (title.isEmpty() || date.isEmpty()) {
                Toast.makeText(this, "Title and Date are required!", Toast.LENGTH_SHORT).show();
                return;
            }

            Map<String, Object> assignmentData = new HashMap<>();
            assignmentData.put("title", title);
            assignmentData.put("subject", subject);
            assignmentData.put("dueDate", date);
            assignmentData.put("description", desc);
            assignmentData.put("priority", priority);
            assignmentData.put("userId", userId);
            assignmentData.put("timestamp", System.currentTimeMillis());
            DatabaseReference assignmentsRef = db.child("assignments");

            if (isUpdating) {
                assignmentsRef.child(assignment.getId()).updateChildren(assignmentData)
                        .addOnSuccessListener(aVoid -> showSuccessSnackbar("Assignment Updated!"))
                        .addOnFailureListener(e -> Toast.makeText(this, "Update Failed", Toast.LENGTH_SHORT).show()); // Keep Toast for errors
            } else {
                assignmentsRef.push().setValue(assignmentData)
                        .addOnSuccessListener(aVoid -> showSuccessSnackbar("Assignment Added!"))
                        .addOnFailureListener(e -> Toast.makeText(this, "Error saving assignment", Toast.LENGTH_SHORT).show()); // Keep Toast for errors
            }

        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void showDeleteConfirmation(Assignment assignment) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Assignment")
                .setMessage("Are you sure you want to delete '" + assignment.getTitle() + "'?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    db.child("assignments").child(assignment.getId())
                            .removeValue() // Use removeValue() for Realtime DB
                            .addOnSuccessListener(aVoid -> showSuccessSnackbar("Assignment Deleted"))
                            .addOnFailureListener(e -> Toast.makeText(this, "Deletion failed.", Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("No", null)
                .show();
    }

    private void setupSearchAndSort() {
        binding.searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                sortAndFilter();
                return false;
            }
            @Override
            public boolean onQueryTextChange(String newText) {
                sortAndFilter();
                return true;
            }
        });
        binding.sortChipGroup.setOnCheckedChangeListener((group, checkedId) -> sortAndFilter());
    }

    private void sortAndFilter() {
        List<Assignment> tempList = new ArrayList<>(allAssignmentsList);
        Comparator<Assignment> comparator;
        int checkedChipId = binding.sortChipGroup.getCheckedChipId();

        if (checkedChipId == R.id.chipSortPriority) {
            comparator = Comparator.comparingInt(a -> getPriorityValue(a.getPriority()));
        } else if (checkedChipId == R.id.chipSortSubject) {
            comparator = Comparator.comparing(Assignment::getSubject, String.CASE_INSENSITIVE_ORDER);
        } else { // Default sort by Due Date
            comparator = Comparator.comparing(Assignment::getDueDate);
        }
        Collections.sort(tempList, comparator);

        String query = binding.searchView.getQuery().toString().toLowerCase(Locale.ROOT);
        filteredList.clear();
        if (query.isEmpty()) {
            filteredList.addAll(tempList);
        } else {
            for (Assignment assignment : tempList) {
                if (assignment.getTitle().toLowerCase(Locale.ROOT).contains(query) ||
                        assignment.getSubject().toLowerCase(Locale.ROOT).contains(query)) {
                    filteredList.add(assignment);
                }
            }
        }
        adapter.notifyDataSetChanged();
        updateUI();
    }

    private void updateUI() {
        if (allAssignmentsList.isEmpty()) {
            binding.emptyStateLayout.setVisibility(View.VISIBLE);
            binding.searchView.setVisibility(View.GONE);
            binding.sortChipGroup.setVisibility(View.GONE);
            binding.assignmentsRecyclerView.setVisibility(View.GONE);
            binding.subtitleText.setText("No tasks pending");
        } else {
            binding.emptyStateLayout.setVisibility(View.GONE);
            binding.searchView.setVisibility(View.VISIBLE);
            binding.sortChipGroup.setVisibility(View.VISIBLE);
            binding.assignmentsRecyclerView.setVisibility(View.VISIBLE);
            binding.subtitleText.setText(allAssignmentsList.size() + " tasks total");
        }
    }

    private int getPriorityValue(String priority) {
        switch (priority) {
            case "High": return 1;
            case "Medium": return 2;
            case "Low": return 3;
            default: return 4;
        }
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
            if (textDate.getTag() != null) {
                try {
                    Date existingDate = dbDateFormat.parse(textDate.getTag().toString());
                    if (existingDate != null) {
                        cal.setTime(existingDate);
                    }
                } catch (ParseException e) { /* Ignore and use current date */ }
            }
            new DatePickerDialog(this, (view, year, month, day) -> {
                Calendar selectedCal = Calendar.getInstance();
                selectedCal.set(year, month, day);
                textDate.setText(displayDateFormat.format(selectedCal.getTime()));
                textDate.setTag(dbDateFormat.format(selectedCal.getTime()));
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
        });
    }

    private void setupBottomNavigation(int currentNavId) {
        binding.bottomNavigation.setSelectedItemId(currentNavId);
        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == currentNavId) return false;
            Intent intent = null;
            if (itemId == R.id.nav_home) {
                intent = new Intent(this, MainActivity.class);
            } else if (itemId == R.id.nav_exam) {
                intent = new Intent(this, ExamsActivity.class);
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
