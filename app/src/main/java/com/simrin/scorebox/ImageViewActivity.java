package com.simrin.scorebox;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.MediaController;
import android.widget.Toast;
import android.widget.VideoView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.github.chrisbanes.photoview.PhotoView;
import com.simrin.scorebox.HelperClass.BasicImageDownloader;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class ImageViewActivity extends AppCompatActivity {
    
    private static final int STORAGE_PERMISSION_CODE = 102;
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
            if(type.equals("image")){
                isImage= true;
                full_video_view.setVisibility(View.GONE);
                full_image_view.setVisibility(View.VISIBLE);
                send_image.setVisibility(View.GONE);

                final String name = getFileNameFromURL(uri);
                final ProgressDialog pd = new ProgressDialog(this);
                final String finalImage_URL = uri;
                full_image_view.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View view) {
                        if(ContextCompat.checkSelfPermission(ImageViewActivity.this,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
                            checkPermission(
                                    Manifest.permission.WRITE_EXTERNAL_STORAGE, STORAGE_PERMISSION_CODE);
                        }
                        final BasicImageDownloader downloader = new BasicImageDownloader(new BasicImageDownloader.OnImageLoaderListener() {
                            @Override
                            public void onError(BasicImageDownloader.ImageError error) {
                                Toast.makeText(ImageViewActivity.this, "Error code " + error.getErrorCode() + ": " +
                                        error.getMessage(), Toast.LENGTH_LONG).show();
                                error.printStackTrace();
                                pd.dismiss();
                            }

                            @Override
                            public void onProgressChange(int percent) {
                                pd.setMessage("Downloading:"+percent+"%");
                                pd.show();
                            }

                            @Override
                            public void onComplete(Bitmap result) {

                                final Bitmap.CompressFormat mFormat = Bitmap.CompressFormat.JPEG;
                                final File imageFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath() +
                                        File.separator + "Scorebox" + File.separator + name + "." + mFormat.name().toLowerCase());
                                BasicImageDownloader.writeToDisk(imageFile, result, new BasicImageDownloader.OnBitmapSaveListener() {
                                    @Override
                                    public void onBitmapSaved() {
                                        galleryAddPic(imageFile.getAbsolutePath());
                                        Toast.makeText(ImageViewActivity.this, "Image Saved", Toast.LENGTH_SHORT).show();
                                    }

                                    @Override
                                    public void onBitmapSaveError(BasicImageDownloader.ImageError error) {
                                        Toast.makeText(ImageViewActivity.this, "Error code " + error.getErrorCode() + ": " +
                                                error.getMessage(), Toast.LENGTH_LONG).show();
                                        error.printStackTrace();

                                    }
                                }, mFormat, true);

                                pd.dismiss();
                                full_image_view.startAnimation(AnimationUtils.loadAnimation(ImageViewActivity.this, android.R.anim.fade_in));
                            }
                        });
                        downloader.download(finalImage_URL, true);
                        return true;
                    }
                });
            }else if(type.equals("video")){
                playVideo(uri);
            }

        }else if(!intent.getExtras().getString("URLsender").isEmpty()){
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
                    .thumbnail(0.1f)
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

    public void checkPermission(String permission, int requestCode) {
        if (ContextCompat.checkSelfPermission(ImageViewActivity.this, permission)
                == PackageManager.PERMISSION_DENIED) {
            // Requesting the permission
            ActivityCompat.requestPermissions(ImageViewActivity.this,
                    new String[]{permission},
                    requestCode);
        }
    }

    public static String getFileNameFromURL(String url) {
        if (url == null) {
            return "";
        }
        try {
            URL resource = new URL(url);
            String host = resource.getHost();
            if (host.length() > 0 && url.endsWith(host)) {
                // handle ...example.com
                return "";
            }
        }
        catch(MalformedURLException e) {
            return "";
        }

        int startIndex = url.lastIndexOf('/') + 1;
        int length = url.length();

        // find end index for ?
        int lastQMPos = url.lastIndexOf('?');
        if (lastQMPos == -1) {
            lastQMPos = length;
        }

        // find end index for #
        int lastHashPos = url.lastIndexOf('#');
        if (lastHashPos == -1) {
            lastHashPos = length;
        }

        // calculate the end index
        int endIndex = Math.min(lastQMPos, lastHashPos);
        return url.substring(startIndex, endIndex);
    }

    private void galleryAddPic(String currentPhotoPath) {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File f = new File(currentPhotoPath);
        Uri contentUri = Uri.fromFile(f);
        mediaScanIntent.setData(contentUri);
        this.sendBroadcast(mediaScanIntent);
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