package com.simrin.scorebox.HelperClass.VideoHelper;

import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.StorageTask;
import com.simrin.scorebox.HelperClass.VideoCompressor.VideoCompress;
import com.simrin.scorebox.MessageActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import static com.simrin.scorebox.HelperClass.CopyFile.copyFile;
import static com.simrin.scorebox.HelperClass.ImageHelper.Thumbnail.createThumbnail;
import static com.simrin.scorebox.HelperClass.UploadingProcess.uploadingProcess;
import static com.simrin.scorebox.HelperClass.UriPath.getRealPathFromURI;

public class VideoUpload {
    private final Context context;
    private File video;
    final FirebaseUser fuser;
    final StorageReference storageReference;

    public VideoUpload(Context context){
        this.context = context;
         fuser = FirebaseAuth.getInstance().getCurrentUser();
        storageReference = FirebaseStorage.getInstance().getReference("messages");
    }
    String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmssSSS").format(new Date());
    public File createVideoFile() throws IOException {
        // Create an image file name

        String videoFileName = "VID_" + timeStamp  +"_";
        File storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        video = File.createTempFile(
                videoFileName,  /* prefix */
                ".mp4",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        return video;
    }

    public void compressVideo(String path, Uri mediaUri, final String userid, final ProgressBar progressBar) {
        final ProgressDialog pd = new ProgressDialog(context);
        VideoCompress.compressVideoLow(getRealPathFromURI(mediaUri.toString(), context),
                path, new VideoCompress.CompressListener() {
                    @Override
                    public void onStart() {
                    }

                    @Override
                    public void onSuccess() {
                        pd.dismiss();
                        try {
                            uploadVideo(userid, progressBar);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onFail() {
                        Toast.makeText(context, "Compression Failed", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onProgress(float percent) {
                        pd.setMessage("Compressing:"+(int)percent+"%");
                        pd.show();
                    }
                });
    }

    private void uploadVideo(String userid, ProgressBar progressBar) throws IOException {
        Bitmap bmThumbnail = ThumbnailUtils.createVideoThumbnail(video.getAbsolutePath(), MediaStore.Video.Thumbnails.MINI_KIND);
        String imageFileName = "THUMB_" + timeStamp + ".jpg";
        File storageDir = new File(context.getFilesDir() + File.separator + "Thumbnails");
        boolean success = true;
        if (!storageDir.exists()) {
            success = storageDir.mkdirs();
        }
        File tempFile = null;
        if (success) {
            tempFile = new File(storageDir, imageFileName);
            try {
                OutputStream fOut = new FileOutputStream(tempFile);
                bmThumbnail.compress(Bitmap.CompressFormat.JPEG, 100, fOut);
                fOut.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        Uri thumb = Uri.fromFile(tempFile);
        File thumbFile = createThumbnail(thumb, context, timeStamp, userid);
        tempFile.delete();
        progressBar.setVisibility(View.VISIBLE);
        StorageReference filepath = storageReference.child("video").child("VID_"+timeStamp + ".mp4");
        final File permVideoFile = new File(context.getFilesDir() + File.separator + userid
                + File.separator + "video" + File.separator + "VID_" + timeStamp + ".mp4");
        copyFile(video, permVideoFile);
        Uri uri = Uri.fromFile(video);
        StorageTask uploadTask = filepath.putFile(uri);
        uploadingProcess("video", fuser.getUid(), userid, filepath, progressBar,
                context, thumbFile, timeStamp, uploadTask);
    }
}
