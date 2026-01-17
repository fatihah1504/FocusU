package com.example.focusu;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import java.io.File;

public class ImagePreviewActivity extends AppCompatActivity {

    public static final String EXTRA_IMAGE_PATH = "extra_image_path";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_preview);

        ImageView imageView = findViewById(R.id.image_preview);
        findViewById(R.id.btn_back_preview).setOnClickListener(v -> finish());

        String imagePath = getIntent().getStringExtra(EXTRA_IMAGE_PATH);

        if (imagePath != null && !imagePath.isEmpty()) {
            Glide.with(this)
                    .load(new File(imagePath))
                    .error(R.drawable.ic_image_broken) // Show a broken image icon on error
                    .into(imageView);
        } else {
            Toast.makeText(this, "Image not found.", Toast.LENGTH_SHORT).show();
            finish(); // Close if no image path is provided
        }
    }
}
