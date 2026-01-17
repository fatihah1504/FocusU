package com.example.focusu;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import java.util.HashMap;

public class SessionManager {

    private static final String PREF_NAME = "FocusUSession";
    private static final String KEY_USER_ID = "userId";
    public static final String KEY_USERNAME = "username";
    public static final String KEY_USER_EMAIL = "email";
    private static final String KEY_IS_LOGGED_IN = "isLoggedIn";
    public static final String KEY_PROFILE_IMAGE_URL = "profileImageUrl";
    private final SharedPreferences sharedPreferences;
    private final SharedPreferences.Editor editor;
    private final Context _context;



    // Constructor
    public SessionManager(Context context) {
        this._context = context;
        sharedPreferences = _context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
    }

    public HashMap<String, String> getUserDetails(){
        HashMap<String, String> user = new HashMap<>();

        user.put(KEY_USERNAME, sharedPreferences.getString(KEY_USERNAME, null));
        user.put(KEY_USER_EMAIL, sharedPreferences.getString(KEY_USER_EMAIL, null));

        return user;
    }


    public void createLoginSession(String userId, String username, String email) {
        // Storing login value as TRUE
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.putString(KEY_USER_ID, userId);
        editor.putString(KEY_USERNAME, username);
        editor.putString(KEY_USER_EMAIL, email);

        editor.commit();
    }

    public String getUserId() {
        return sharedPreferences.getString(KEY_USER_ID,null);
    }

    public boolean isLoggedIn() {
        return sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false);
    }
    public void logoutUser() {
        editor.clear();
        editor.commit();

        Intent i = new Intent(_context, LoginActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        _context.startActivity(i);
    }
    public void saveUserName(String userName) {
        editor.putString(KEY_USERNAME, userName);
        editor.apply();
    }

    public String getUserName() {
        return sharedPreferences.getString(KEY_USERNAME,"Guest");
    }

    public void saveProfileImagePath(String imageUrl) {
        editor.putString(KEY_PROFILE_IMAGE_URL, imageUrl);
        editor.apply();
    }

    public String getProfileImagePath() {
        return sharedPreferences.getString(KEY_PROFILE_IMAGE_URL, null);
    }

    public void createLoginSession(String userId, String username, String email, String imageUrl) {
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.putString(KEY_USER_ID, userId);
        editor.putString(KEY_USERNAME, username);
        editor.putString(KEY_USER_EMAIL, email);
        editor.putString(KEY_PROFILE_IMAGE_URL, imageUrl);

        editor.commit();
    }


}
