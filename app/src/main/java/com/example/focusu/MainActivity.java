package com.example.focusu;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.Data;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.focusu.databinding.ActivityMainBinding;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import android.os.Handler;
import android.os.Looper;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private DatabaseReference db;
    private String userId;
    private final Handler autoDismissHandler = new Handler(Looper.getMainLooper());
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    Toast.makeText(this, "Notifications enabled!", Toast.LENGTH_SHORT).show();
                    scheduleDailyNotificationWorker();
                } else {
                    Toast.makeText(this, "Notifications are disabled.", Toast.LENGTH_SHORT).show();
                }
            });

    private final ActivityResultLauncher<Intent> profileLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {

                if (result.getResultCode() == RESULT_OK) {
                    loadProfileIcon();
                }
            });

    private RecentActivityAdapter recentActivityAdapter;
    private final List<RecentItem> recentItemsList = new ArrayList<>();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = FirebaseDatabase.getInstance().getReference();
        SessionManager sessionManager = new SessionManager(this);

        userId = sessionManager.getUserId();
        if (userId == null || userId.isEmpty()) {
            sessionManager.logoutUser();
            return;
        }

        String userName = sessionManager.getUserName();
        binding.greetingText.setText("Good Morning, " + userName + "!");
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault());
        String currentDate = sdf.format(new Date());
        binding.dateText.setText(currentDate);

        loadDashboardStats();
        setupRecentActivity();
        loadRecentActivity();
        setupClickListeners();
        setupBottomNavigation();
        loadProfileIcon();

//        binding.notificationIcon.setOnClickListener(v -> showNotificationPopup(false));
//        binding.profileIcon.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, ProfileActivity.class)));

        NotificationHelper.createNotificationChannel(this);
        askNotificationPermission();
        if (getIntent().getBooleanExtra("SHOW_POPUP_ON_LOAD", false)) {
            // We use a small delay to allow the main screen to draw itself first,
            // which makes the popup appear more smoothly.
            new Handler(Looper.getMainLooper()).postDelayed(() -> showNotificationPopup(true), 3000); // 500ms delay
        }
    }

    // In MainActivity.java

    private void showNotificationPopup(boolean autoDismiss) {
        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        View popupView = inflater.inflate(R.layout.popup_notifications, null);

        RecyclerView recyclerView = popupView.findViewById(R.id.popupRecyclerView);
        TextView noNotificationsText = popupView.findViewById(R.id.textNoNotifications);

        int width = (int) (300 * getResources().getDisplayMetrics().density);
        int height = ViewGroup.LayoutParams.WRAP_CONTENT;
        final PopupWindow popupWindow = new PopupWindow(popupView, width, height, true);
        popupWindow.setElevation(20);

        // --- MODIFICATION: Use the new PopupReminderItem model ---
        List<PopupReminderItem> reminders = new ArrayList<>();
        NotificationPopupAdapter popupAdapter = new NotificationPopupAdapter(reminders);
        recyclerView.setAdapter(popupAdapter);

        // --- Fetch Data from Firebase ---
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat dbFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String todayDate = dbFormat.format(calendar.getTime());
        calendar.add(Calendar.DAY_OF_YEAR, 1);
        String tomorrowDate = dbFormat.format(calendar.getTime());

        // 1. Fetch Assignments
        Query assignmentsQuery = db.child("assignments").orderByChild("userId").equalTo(userId);
        assignmentsQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Assignment assignment = snapshot.getValue(Assignment.class);
                    if (assignment != null && assignment.getDueDate() != null && !"Done".equals(assignment.getDbStatus())) {
                        //same like exams
                        if (assignment.getDueDate().equals(todayDate)) {
                            String title = assignment.getSubject() + " (" + assignment.getTitle() + ")";
                            String subtitle ="Due Today";
                            reminders.add(new PopupReminderItem(title, subtitle, "assignment"));

                        } else if (assignment.getDueDate().equals(tomorrowDate)) {
                            String title = assignment.getSubject() + " (" + assignment.getTitle() + ")";
                            String subtitle = "Due Tomorrow";
                            reminders.add(new PopupReminderItem(title, subtitle, "assignment"));


                        }
                    }
                }
                popupAdapter.notifyDataSetChanged();
                updatePopupUI(reminders, noNotificationsText, recyclerView);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

        // 2. Fetch Exams
        Query examsQuery = db.child("exams").orderByChild("userId").equalTo(userId);
        examsQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Exam exam = snapshot.getValue(Exam.class);
                    if (exam != null && exam.getDate() != null) {
                        if (exam.getDate().equals(todayDate)) {
                            // --- NEW: Create a PopupReminderItem ---
                            String title = exam.getSubject() + " (" + exam.getType() + ")";
                            String subtitle = "Today at " + exam.getTime();
                            reminders.add(new PopupReminderItem(title, subtitle, "exam"));
                        } else if (exam.getDate().equals(tomorrowDate)) {
                            String title = exam.getSubject() + " (" + exam.getType() + ")";
                            String subtitle = "Tomorrow at " + exam.getTime();
                            reminders.add(new PopupReminderItem(title, subtitle, "exam"));
                        }
                    }
                }
                Collections.sort(reminders, Comparator.comparing(PopupReminderItem::getSubtitle)); // Sort for consistency
                popupAdapter.notifyDataSetChanged();
                updatePopupUI(reminders, noNotificationsText, recyclerView);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

        popupWindow.showAsDropDown(binding.notificationIcon, -width / 2 + binding.notificationIcon.getWidth() / 2, 0);

        if (autoDismiss) {
            autoDismissHandler.postDelayed(() -> {
                if (popupWindow.isShowing()) {
                    popupWindow.dismiss();
                }
            }, 6000);
        }
    }


    private void updatePopupUI(List<PopupReminderItem> reminders, TextView noNotificationsText, RecyclerView recyclerView) {
        if (reminders.isEmpty()) {
            noNotificationsText.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            noNotificationsText.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }



    private void loadDashboardStats() {
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        // --- 1. Get Upcoming Assignments Count ---
        Query assignmentsQuery = db.child("assignments").orderByChild("userId").equalTo(userId);
        assignmentsQuery.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                long pendingCount = 0;
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Assignment assignment = snapshot.getValue(Assignment.class);
                    if (assignment != null && !"Done".equals(assignment.getDbStatus()) && assignment.getDueDate() != null && assignment.getDueDate().compareTo(today) >= 0) {
                        pendingCount++;
                    }
                }
                binding.statPendingCount.setText(String.valueOf(pendingCount));
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Handle error
            }
        });

        // --- 2. Get Upcoming Exams Count ---
        Query examsQuery = db.child("exams").orderByChild("userId").equalTo(userId);
        examsQuery.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                long upcomingCount = 0;
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Exam exam = snapshot.getValue(Exam.class);
                    if (exam != null && exam.getDate() != null && exam.getDate().compareTo(today) >= 0) {
                        upcomingCount++;
                    }
                }
                binding.statExamsCount.setText(String.valueOf(upcomingCount));
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Handle error
            }
        });


        // --- 3. Get Notes Count ---
        Query notesQuery = db.child("notes").orderByChild("userId").equalTo(userId);
        notesQuery.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                binding.statNotesCount.setText(String.valueOf(dataSnapshot.getChildrenCount()));
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Handle error
            }
        });

        // --- 4. Get Recordings Count ---
        Query recordingsQuery = db.child("recordings").orderByChild("userId").equalTo(userId);
        recordingsQuery.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                binding.statRecordingsCount.setText(String.valueOf(dataSnapshot.getChildrenCount()));
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Handle error
            }
        });
    }

    private void setupClickListeners() {
        binding.cardAssignmentsFeature.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, AssignmentsActivity.class)));
        binding.cardRecordingsFeature.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, RecordingsActivity.class)));
        binding.cardNotesFeature.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, NotesActivity.class)));
        binding.cardExamsFeature.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, ExamsActivity.class)));
        binding.cardPending.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, AssignmentsActivity.class)));
        binding.cardRecordings.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, RecordingsActivity.class)));
        binding.cardNotes.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, NotesActivity.class)));
        binding.cardExams.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, ExamsActivity.class)));
        binding.notificationIcon.setOnClickListener(v -> showNotificationPopup(false));
        binding.profileIcon.setOnClickListener(v -> profileLauncher.launch(new Intent(MainActivity.this, ProfileActivity.class)));
    }

    private void setupBottomNavigation() {
        binding.bottomNavigation.setSelectedItemId(R.id.nav_home);
        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) return false;
            Intent intent = null;
            if (itemId == R.id.nav_assignments) intent = new Intent(this, AssignmentsActivity.class);
            else if (itemId == R.id.nav_exam) intent = new Intent(this, ExamsActivity.class);
            else if (itemId == R.id.nav_notes) intent = new Intent(this, NotesActivity.class);
            else if (itemId == R.id.nav_recording) intent = new Intent(this, RecordingsActivity.class);

            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
                return true;
            }
            return false;
        });
    }

    // Add these methods to MainActivity.java

    private void askNotificationPermission() {
        // This is only necessary for API level 33+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                    PackageManager.PERMISSION_GRANTED) {
                // Directly ask for the permission
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            } else {
                // Permission is already granted, schedule the worker
                scheduleDailyNotificationWorker();
            }
        } else {
            // For older versions, permission is granted by default, just schedule.
            scheduleDailyNotificationWorker();
        }
    }

    private void scheduleDailyNotificationWorker() {
        // Pass the user ID to the worker
        Data inputData = new Data.Builder()
                .putString("USER_ID_KEY", userId)
                .build();

        PeriodicWorkRequest dailyNotificationRequest =
                new PeriodicWorkRequest.Builder(NotificationWorker.class, 24, TimeUnit.HOURS)
                        .setInputData(inputData)
                        .build();

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "DailyNotificationWork",
                ExistingPeriodicWorkPolicy.KEEP,
                dailyNotificationRequest
        );

        Toast.makeText(this, "Daily reminders are scheduled!", Toast.LENGTH_SHORT).show();
    }


    private void setupRecentActivity() {
        binding.recentActivityRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        recentActivityAdapter = new RecentActivityAdapter(recentItemsList);
        binding.recentActivityRecyclerView.setAdapter(recentActivityAdapter);
    }



    private void loadRecentActivity() {
        List<RecentItem> combinedList = new ArrayList<>();
        final int TOTAL_QUERIES = 4;
        final int[] queriesCompleted = {0};

        Runnable onQueryComplete = () -> {
            queriesCompleted[0]++;
            // If all 4 queries (Assignments, Exams, Notes, Recordings) are done
            if (queriesCompleted[0] == TOTAL_QUERIES) {

                Collections.sort(combinedList, (o1, o2) -> Long.compare(o2.getTimestamp(), o1.getTimestamp()));

                recentItemsList.clear();

                int limit = Math.min(4, combinedList.size());
                if (limit > 0) {
                    recentItemsList.addAll(combinedList.subList(0, limit));
                }

                runOnUiThread(() -> {
                    if (recentItemsList.isEmpty()) {
                        binding.recentActivityRecyclerView.setVisibility(View.GONE);
                        binding.noRecentActivityText.setVisibility(View.VISIBLE);
                    } else {
                        binding.recentActivityRecyclerView.setVisibility(View.VISIBLE);
                        binding.noRecentActivityText.setVisibility(View.GONE);
                        recentActivityAdapter.notifyDataSetChanged();
                    }
                });
            }
        };

        // --- 1. Fetch recent Assignments ---
        db.child("assignments").orderByChild("userId").equalTo(userId).limitToLast(4)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot postSnapshot : snapshot.getChildren()) {
                            Assignment item = postSnapshot.getValue(Assignment.class);
                            if (item != null) {
                                combinedList.add(new RecentItem("Assignment Added", item.getTitle(), "assignment", item.getTimestamp()));
                            }
                        }
                        onQueryComplete.run();
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) { onQueryComplete.run(); }
                });

        // --- 2. Fetch recent Exams ---
        db.child("exams").orderByChild("userId").equalTo(userId).limitToLast(4)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot postSnapshot : snapshot.getChildren()) {
                            Exam item = postSnapshot.getValue(Exam.class);
                            if (item != null) {
                                combinedList.add(new RecentItem("Exam Scheduled", item.getSubject(), "exam", item.getTimestamp()));
                            }
                        }
                        onQueryComplete.run();
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) { onQueryComplete.run(); }
                });

        // --- 3. Fetch recent Notes ---
        db.child("notes").orderByChild("userId").equalTo(userId).limitToLast(4)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot ds : snapshot.getChildren()) {
                            Note item = ds.getValue(Note.class);
                            if (item != null) {
                                combinedList.add(new RecentItem("Note Saved", item.getTitle(), "note", item.getTimestamp()));
                            }
                        }
                        onQueryComplete.run();
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) { onQueryComplete.run(); }
                });

        // --- 4. Fetch recent Recordings ---
        db.child("recordings").orderByChild("userId").equalTo(userId).limitToLast(4)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot snapshotChild : snapshot.getChildren()) {
                            Recording item = snapshotChild.getValue(Recording.class);
                            if (item != null) {
                                combinedList.add(new RecentItem("Recording Saved", item.getTitle(), "recording", item.getTimestamp()));
                            }
                        }
                        onQueryComplete.run();
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) { onQueryComplete.run(); }
                });
    }
    private void processAndDisplayRecentItems() {
        Collections.sort(recentItemsList, (o1, o2) -> Long.compare(o2.getTimestamp(), o1.getTimestamp()));

        List<RecentItem> limitedList;
        if (recentItemsList.size() > 4) {
            limitedList = new ArrayList<>(recentItemsList.subList(0, 4));
        } else {
            limitedList = new ArrayList<>(recentItemsList);
        }

        recentActivityAdapter.updateList(limitedList);

        // Show/Hide Empty State
        if (limitedList.isEmpty()) {
            binding.recentActivityRecyclerView.setVisibility(View.GONE);
            binding.noRecentActivityText.setVisibility(View.VISIBLE);
        } else {
            binding.recentActivityRecyclerView.setVisibility(View.VISIBLE);
            binding.noRecentActivityText.setVisibility(View.GONE);
        }
    }

    private void loadProfileIcon() {
        SessionManager sessionManager = new SessionManager(this);
        String imagePath = sessionManager.getProfileImagePath();

        if (imagePath != null && !imagePath.isEmpty()) {
            // In MainActivity.java -> loadProfileIcon()
            Glide.with(this)
                    .load(new File(imagePath))
                    .placeholder(R.drawable.ic_profile_placeholder)
                    .error(R.drawable.ic_profile_placeholder)
                    .skipMemoryCache(true) // Add this
                    .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.NONE)
                    .into(binding.profileIcon);

        } else {
            binding.profileIcon.setImageResource(R.drawable.ic_profile_placeholder);
        }
    }





}
