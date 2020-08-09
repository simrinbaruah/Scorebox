package com.simrin.scorebox.HelperClass;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.storage.OnPausedListener;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.StorageTask;
import com.google.firebase.storage.UploadTask;

import java.io.File;

import static com.simrin.scorebox.HelperClass.SendMessage.sendMessage;
import static com.simrin.scorebox.HelperClass.ImageHelper.Thumbnail.storeThumbnail;

public class UploadingProcess {

    public static void uploadingProcess(final String type, final String sender, final String receiver, final StorageReference fileReference,
                                        final ProgressBar progressBar, final Context context, final File thumbFile, final String timestamp,
                                        final StorageTask uploadTask){
        uploadTask.addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onProgress(@NonNull UploadTask.TaskSnapshot taskSnapshot) {
                double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
                Log.d("PROGRESS", "Upload is " + progress + "% done");
                int currentprogress = (int) Math.round(progress);
                progressBar.setProgress(currentprogress);
            }
        }).addOnPausedListener(new OnPausedListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onPaused(UploadTask.TaskSnapshot taskSnapshot) {
                System.out.println("Upload is paused");
            }
        });
        uploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
            @Override
            public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                if (!task.isSuccessful())
                    throw task.getException();
                return fileReference.getDownloadUrl();
            }
        }).addOnCompleteListener(new OnCompleteListener<Uri>() {
            @Override
            public void onComplete(@NonNull Task<Uri> task) {
                if(task.isSuccessful()) {
                    Uri downloadUri = task.getResult();
                    final String mUri = downloadUri.toString();

                    if(type.equals("image") || type.equals("video")){
                        Uri thumbUri = Uri.fromFile(thumbFile);
                        StorageReference thumbRef = storeThumbnail(context, type, timestamp, thumbUri);
                        StorageTask uploadThumb;
                        uploadThumb = thumbRef.putFile(thumbUri);
                        final StorageReference finalThumbRef = thumbRef;
                        uploadThumb.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                            @Override
                            public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                                if (!task.isSuccessful())
                                    throw task.getException();
                                return finalThumbRef.getDownloadUrl();
                            }
                        }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                            @Override
                            public void onComplete(@NonNull Task<Uri> task) {
                                if(task.isSuccessful()) {
                                    Uri downloadUri = task.getResult();
                                    String mThumbUri = downloadUri.toString();
                                    sendMessage(sender, receiver, mUri, mThumbUri, type, context);
                                }
                            }
                        });
                    }else{
                        sendMessage(sender, receiver, mUri, null, type, context);
                    }
                    progressBar.setVisibility(View.GONE);

                }else {
                    Toast.makeText(context, "Failed to attach media!", Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.GONE);
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(context, e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                progressBar.setVisibility(View.GONE);
            }
        });
    }
}
