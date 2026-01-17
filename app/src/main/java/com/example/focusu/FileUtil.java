package com.example.focusu;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

public class FileUtil {
    public static String getPath(Context context, Uri uri) {
        String[] projection = { MediaStore.Images.Media.DATA };
        Cursor cursor = context.getContentResolver().query(uri, projection, null, null, null);
        if (cursor != null) {
            try {
                int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                cursor.moveToFirst();
                String path = cursor.getString(columnIndex);
                return path;
            } finally {
                cursor.close();
            }
        }
        // Fallback for certain Uris
        return uri.getPath();
    }
}
