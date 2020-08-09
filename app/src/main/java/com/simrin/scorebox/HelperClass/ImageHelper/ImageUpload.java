package com.simrin.scorebox.HelperClass.ImageHelper;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.StorageTask;
import com.simrin.scorebox.HelperClass.FileExtension;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static com.simrin.scorebox.HelperClass.CopyFile.copyFile;
import static com.simrin.scorebox.HelperClass.ImageHelper.CompressFile.compressImage;
import static com.simrin.scorebox.HelperClass.ImageHelper.Thumbnail.createThumbnail;
import static com.simrin.scorebox.HelperClass.UploadingProcess.uploadingProcess;
import static com.simrin.scorebox.HelperClass.UriPath.getRealPathFromURI;

public class ImageUpload {

    private Context context;
    private String timeStamp;
    private StorageTask uploadTask;
    StorageReference storageReference;

    public ImageUpload(Context context){
        this.context = context;
        storageReference = FirebaseStorage.getInstance().getReference("messages");
        timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmssSSS").format(new Date());
    }


    public void uploadImage(final String sender, final String receiver, Uri mediaUri, ProgressBar progressBar, File thumbFile){

        if(mediaUri != null){
            progressBar.setVisibility(View.VISIBLE);
            String extension = FileExtension.getFileExtension(mediaUri, context);
            if(extension == null) extension = "jpg";
            final StorageReference fileReference = storageReference.child("images").child("JPEG_"+timeStamp
                    + "." +extension);

            uploadTask = fileReference.putFile(mediaUri);
            uploadingProcess("image", sender, receiver, fileReference,
                    progressBar, context, thumbFile, timeStamp, uploadTask);
        }else{
            Toast.makeText(context, "No image selected", Toast.LENGTH_SHORT).show();
            progressBar.setVisibility(View.GONE);
        }
    }

    public File createTempImageFile(int height, int width, Uri mediaUri) {
        File tempImageFile = null;
        try {
            tempImageFile = createImageFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if(tempImageFile!=null && height != 0 && width != 0){
//                    String path = fileUtils.getPath(mediaUri);
            String path = getRealPathFromURI(mediaUri.toString(), context);
            String destPath = tempImageFile.getAbsolutePath();
            compressImage(path, destPath, height, width, context);
           // mediaUri = Uri.fromFile(new File(destPath));
        }
        return tempImageFile;
    }

    public File createImageFile() throws IOException {
        // Create an image file name
        String imageFileName = "JPEG_" + timeStamp +"_";
        File storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );
        // Save a file: path for use with ACTION_VIEW intents
        return image;
    }

    public String copyToPermanent(String userid, File tempImageFile){
        final File permImageFile = new File(context.getFilesDir() + File.separator + userid +
                File.separator + "images" + File.separator + "JPEG_" + timeStamp + ".jpg");
        try {
            copyFile(tempImageFile, permImageFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return timeStamp;
    }

}
