package com.simrin.scorebox;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.media.Image;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

public class ImageViewActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_view);

        Intent intent = getIntent();
        String image_URL = intent.getStringExtra("URL");

        ImageView full_image_view = findViewById(R.id.full_image_view);
        Glide.with(this).load(image_URL).apply(new RequestOptions()
                .fitCenter()
                .skipMemoryCache(true))
                .thumbnail(0.1f)
                .into(full_image_view);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}