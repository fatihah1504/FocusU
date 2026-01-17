package com.example.focusu;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

// --- FIX: Import Realtime Database classes ---
import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;

public class NotificationWorker extends Worker {

    private static final String TAG = "NotificationWorker";

    public NotificationWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();

        Data inputData = getInputData();
        String userId = inputData.getString("USER_ID_KEY");

        if (userId == null || userId.isEmpty()) {
            Log.e(TAG, "User ID is missing, cannot perform work.");
            return Result.failure();
        }

        FirebaseApp.initializeApp(context);

        // --- 2. SETUP DATES AND FIREBASE ---
        DatabaseReference db = FirebaseDatabase.getInstance().getReference();
        SimpleDateFormat dbFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        Calendar calendar = Calendar.getInstance();

        String todayDate = dbFormat.format(calendar.getTime());
        calendar.add(Calendar.DAY_OF_YEAR, 1);
        String tomorrowDate = dbFormat.format(calendar.getTime());

        // --- 3. FETCH DATA SYNCHRONOUSLY ---
        final CountDownLatch latch = new CountDownLatch(2); // We have two queries (assignments and exams)

        final int[] upcomingAssignmentsCount = {0};
        final int[] upcomingExamsCount = {0};

        // Query for assignments
        Query assignmentsQuery = db.child("assignments").orderByChild("userId").equalTo(userId);
        assignmentsQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                int count = 0;
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Assignment assignment = snapshot.getValue(Assignment.class);
                    if (assignment != null && assignment.getDueDate() != null) {
                        String dueDate = assignment.getDueDate();
                        // Also check that the assignment is not marked as "Done"
                        if ((dueDate.equals(todayDate) || dueDate.equals(tomorrowDate)) && !"Done".equals(assignment.getDbStatus())) {
                            count++;
                        }
                    }
                }
                upcomingAssignmentsCount[0] = count;
                latch.countDown();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "Assignment query cancelled", databaseError.toException());
                latch.countDown();
            }
        });

        // Query for exams
        Query examsQuery = db.child("exams").orderByChild("userId").equalTo(userId);
        examsQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                int count = 0;
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Exam exam = snapshot.getValue(Exam.class);
                    if (exam != null && exam.getDate() != null) {
                        String examDate = exam.getDate();
                        if (examDate.equals(todayDate) || examDate.equals(tomorrowDate)) {
                            count++;
                        }
                    }
                }
                upcomingExamsCount[0] = count;
                latch.countDown();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "Exam query cancelled", databaseError.toException());
                latch.countDown();
            }
        });

        try {
            latch.await();

            // --- 4. SHOW NOTIFICATIONS ---
            if (upcomingAssignmentsCount[0] > 0) {
                String title = "Upcoming Assignments";
                String content = "You have " + upcomingAssignmentsCount[0] + " assignment(s) due soon.";
                NotificationHelper.showNotification(context, title, content, 101);
            }

            if (upcomingExamsCount[0] > 0) {
                String title = "Upcoming Exams";
                String content = "You have " + upcomingExamsCount[0] + " exam(s) coming up. Good luck!";
                NotificationHelper.showNotification(context, title, content, 102);
            }

            Log.d(TAG, "Work finished successfully for user: " + userId);
            return Result.success();

        } catch (InterruptedException e) {
            Log.e(TAG, "Worker interrupted while waiting for data", e);
            Thread.currentThread().interrupt();
            return Result.failure();
        }
    }
}
