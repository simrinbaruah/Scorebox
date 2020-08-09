package com.simrin.scorebox.HelperClass;

import android.content.Context;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Vibrator;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.StorageTask;

import java.io.File;
import java.io.IOException;

import static com.simrin.scorebox.HelperClass.UploadingProcess.uploadingProcess;

public class AudioHelper {

    public static void uploadAudio(ProgressBar progressBar, String fileName, String userid, Context context) {
        StorageReference storageReference = FirebaseStorage.getInstance().getReference("messages");
        FirebaseUser fuser = FirebaseAuth.getInstance().getCurrentUser();
        progressBar.setVisibility(View.VISIBLE);
        StorageReference filepath = storageReference.child("audio").child(System.currentTimeMillis()+".3gp");
        Uri uri = Uri.fromFile(new File(fileName));
        StorageTask uploadTask = filepath.putFile(uri);
        uploadingProcess("audio", fuser.getUid(), userid, filepath, progressBar,
                context, null, null, uploadTask);
    }
}
