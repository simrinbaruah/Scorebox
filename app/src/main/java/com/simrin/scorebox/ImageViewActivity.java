package com.simrin.scorebox;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.media.Image;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.github.chrisbanes.photoview.PhotoView;
import com.simrin.scorebox.HelperClass.BasicImageDownloader;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

public class ImageViewActivity extends AppCompatActivity {
    
    private static final int STORAGE_PERMISSION_CODE = 102;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_view);

        final PhotoView full_image_view = findViewById(R.id.full_image_view);
        Button send_image = findViewById(R.id.send_image);

        Intent intent = getIntent();
        String image_URL = null;
        if(intent.getStringExtra("URL") != null){
            image_URL = intent.getStringExtra("URL");
            send_image.setVisibility(View.GONE);

            final String name = getFileNameFromURL(image_URL);
            final ProgressDialog pd = new ProgressDialog(this);
            final String finalImage_URL = image_URL;
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
                Intent intent=new Intent();
                setResult(RESULT_OK, intent);
                finish();
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

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}