package com.simrin.scorebox;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.media.Image;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

public class ImageViewActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_view);

        ImageView full_image_view = findViewById(R.id.full_image_view);
        Button send_image = findViewById(R.id.send_image);

        Intent intent = getIntent();
        String image_URL = null;
        if(intent.getStringExtra("URL") != null){
            image_URL = intent.getStringExtra("URL");
            send_image.setVisibility(View.GONE);
        }else if(!intent.getStringExtra("URLsender").isEmpty()){
            image_URL = intent.getStringExtra("URLsender");
            send_image.setVisibility(View.VISIBLE);
        }

        Glide.with(this).load(image_URL).apply(new RequestOptions()
                .fitCenter()
                .skipMemoryCache(true))
                .thumbnail(0.1f)
                .into(full_image_view);

        send_image.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent intent=new Intent();
        setResult(RESULT_OK, intent);
        finish();
    }
}