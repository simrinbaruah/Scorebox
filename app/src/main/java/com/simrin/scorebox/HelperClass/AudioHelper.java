package com.simrin.scorebox.HelperClass;

import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.view.View;
import android.widget.ProgressBar;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.StorageTask;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

import static com.simrin.scorebox.HelperClass.UploadingProcess.uploadingProcess;

public class AudioHelper {

    private String timeStamp, userid;
    private Context context;

    public AudioHelper(String userid, Context context){
        timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmssSSS").format(new Date());
        this.context = context;
        this.userid = userid;
    }

    public void uploadAudio(ProgressBar progressBar, String fileName) {
        StorageReference storageReference = FirebaseStorage.getInstance().getReference("messages");
        FirebaseUser fuser = FirebaseAuth.getInstance().getCurrentUser();
        progressBar.setVisibility(View.VISIBLE);
        StorageReference filepath = storageReference.child("audio").child("AUD_"+timeStamp+".3gp");
        Uri uri = Uri.fromFile(new File(fileName));
        StorageTask uploadTask = filepath.putFile(uri);
        uploadingProcess("audio", fuser.getUid(), userid, filepath, progressBar,
                context, null, null, uploadTask);
    }

    public String createAudioFile(){
        File audioFile = new File(context.getFilesDir() + File.separator + userid +
                File.separator + "audio");
        if(!audioFile.exists()){
            audioFile.mkdirs();
        }
        String fileName = audioFile + File.separator + "AUD_"+timeStamp+ ".3gp";
        return fileName;
    }

}
