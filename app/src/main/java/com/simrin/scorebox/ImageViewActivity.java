package com.simrin.scorebox;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.MediaController;
import android.widget.VideoView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.github.chrisbanes.photoview.PhotoView;

public class ImageViewActivity extends AppCompatActivity {
    private PhotoView full_image_view;
    private VideoView full_video_view;
    private Button send_image;
    private boolean isImage = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_view);

        full_image_view = findViewById(R.id.full_image_view);
        full_video_view = findViewById(R.id.full_video_view);
        send_image = findViewById(R.id.send_image);

        Intent intent = getIntent();
        String uri = null;
        String type;
        if(intent.getExtras().getString("URL") != null){
            uri = intent.getStringExtra("URL");
            type = intent.getStringExtra("type");
            send_image.setVisibility(View.GONE);
            if(type.equals("image")){
                isImage= true;
                full_video_view.setVisibility(View.GONE);
                full_image_view.setVisibility(View.VISIBLE);
            }else if(type.equals("video")){
                playVideo(uri);
            }

        }else if(intent.getExtras().getString("URLsender") != null){
            uri = intent.getStringExtra("URLsender");
            type = intent.getStringExtra("type");
            send_image.setVisibility(View.VISIBLE);
            if(type.equals("video")){
                playVideo(uri);
            }else{
                isImage = true;
                full_image_view.setVisibility(View.VISIBLE);
                full_video_view.setVisibility(View.GONE);
            }
        }

        if(isImage){
            Glide.with(this).load(uri).apply(new RequestOptions()
                    .fitCenter()
                    .skipMemoryCache(true))
                    .into(full_image_view);
        }

        send_image.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent=new Intent();
                setResult(RESULT_OK, intent);
                finish();
            }
        });
    }

    private void playVideo(String uri) {
        isImage = false;
        full_image_view.setVisibility(View.GONE);
        full_video_view.setVisibility(View.VISIBLE);
        full_video_view.setVideoURI(Uri.parse(uri));
        MediaController mediaController = new MediaController(this, false);
        mediaController.setMediaPlayer(full_video_view);
        full_video_view.setMediaController(mediaController);
        full_video_view.start();
        full_video_view.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                full_video_view.seekTo(0);
                full_video_view.start();
            }
        });
    }

    private void releasePlayer() {
        full_video_view.stopPlayback();
    }



        @Override
    public void onBackPressed() {
        super.onBackPressed();
        if(send_image.getVisibility() == View.VISIBLE){
            Intent intent=new Intent();
            setResult(RESULT_CANCELED, intent);
            finish();
        }
        finish();
    }

    @Override
    protected void onStop() {
        super.onStop();
        releasePlayer();
    }
}