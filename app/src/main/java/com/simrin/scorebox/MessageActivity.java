package com.simrin.scorebox;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import de.hdodenhof.circleimageview.CircleImageView;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Vibrator;

import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.StorageTask;
import com.simrin.scorebox.HelperClass.AudioHelper;
import com.simrin.scorebox.HelperClass.ImageHelper.CheckPermission;
import com.simrin.scorebox.HelperClass.ImageHelper.CompressFile;
import com.simrin.scorebox.HelperClass.FileUtils;
import com.simrin.scorebox.HelperClass.ImageHelper.ImageUpload;
import com.simrin.scorebox.HelperClass.SeenMessage;
import com.simrin.scorebox.HelperClass.UserStatus;
import com.simrin.scorebox.HelperClass.VideoHelper.VideoUpload;

import java.io.File;
import java.io.IOException;

import static com.simrin.scorebox.HelperClass.SendMessage.sendMessage;
import static com.simrin.scorebox.HelperClass.ImageHelper.Thumbnail.createThumbnail;

public class MessageActivity extends AppCompatActivity {

    String userid, currentPhotoPath, timeStamp;
    File thumbFile;

    RelativeLayout messageLayout;

    TextView username, status;
    CircleImageView profile_image;

    FirebaseUser fuser;

    ImageButton btn_send, btn_attach, btn_voice, btn_camera;
    EditText text_send;
    ProgressBar progressBar;

    RecyclerView recyclerView;

    Intent intent;
    ImageUpload imageUpload, cameraImage;

    private Uri mediaUri;
    SeenMessage sm;
    AudioHelper audioHelper;
    UserStatus userStatus;

    StorageTask uploadTask;

    private static final int MEDIA_REQUEST = 1;
    private static final int IMAGE_REQUEST = 2;
    private static final int CAMERA_REQUEST = 3;
    private static final int VIDEO_REQUEST = 4;

    StorageReference storageReference;

    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private static final int CAMERA_PERMISSION_CODE = 100;
    private static final int STORAGE_PERMISSION_CODE = 101;
    private File tempVideoFile = null;
    private File tempImageFile = null;
    private File photoFile = null;
    private static final int MIN_SWIPE_DISTANCE = 150;
    private float x1,x2;
    private static int CLICK_THRESHOLD = 1000;
    private MediaRecorder recorder;
    private boolean isAudioCancel = false;
    private String audioFile;

    private static final String LOG_TAG = "AudioRecordTest";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                startActivity(new Intent(MessageActivity.this, MainActivity.class)
//                        .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
                finish();
            }
        });

        recyclerView=findViewById(R.id.recycler_view);
        recyclerView.setHasFixedSize(true);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getApplicationContext());
        linearLayoutManager.setStackFromEnd(true);
        recyclerView.setLayoutManager(linearLayoutManager);

        messageLayout = findViewById(R.id.message_layout);
        profile_image = findViewById(R.id.profile_image);
        status = findViewById(R.id.status);
        username = findViewById(R.id.username);
        btn_send = findViewById(R.id.btn_send);
        btn_send.setVisibility(View.GONE);
        text_send = findViewById(R.id.text_send);
        btn_attach = findViewById(R.id.btn_attach);
        btn_voice = findViewById(R.id.btn_voice);
        btn_camera = findViewById(R.id.btn_camera);
        progressBar = findViewById(R.id.progressBar);
        progressBar.setVisibility(View.GONE);

        storageReference = FirebaseStorage.getInstance().getReference("messages");

         intent = getIntent();
         userid = intent.getStringExtra("userid");
         fuser = FirebaseAuth.getInstance().getCurrentUser();
         btn_attach.setOnClickListener(new View.OnClickListener() {
             @Override
             public void onClick(View view) {
                openImage();
             }
         });

        text_send.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                final String msg = text_send.getText().toString();
                if(!("".equals(msg.trim()))){
                    btn_voice.setVisibility(View.GONE);
                    btn_send.setVisibility(View.VISIBLE);
                    btn_send.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            String type = "text";
                            sendMessage(fuser.getUid(), userid, msg, null, type, MessageActivity.this);
                            text_send.setText("");
                        }
                    });
                }else{
                    btn_voice.setVisibility(View.VISIBLE);
                    btn_send.setVisibility(View.GONE);
                }
            }
            @Override
            public void afterTextChanged(Editable editable) {
            }
        });

        //For audio
        final Vibrator v = (Vibrator)getSystemService(Context.VIBRATOR_SERVICE);
        audioHelper = new AudioHelper(userid, MessageActivity.this);
        audioFile = audioHelper.createAudioFile();
        btn_voice.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if(ContextCompat.checkSelfPermission(MessageActivity.this,
                        Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED){
                    CheckPermission.checkPermission(
                            Manifest.permission.RECORD_AUDIO,
                            REQUEST_RECORD_AUDIO_PERMISSION, MessageActivity.this);
                }
                long duration = motionEvent.getEventTime() - motionEvent.getDownTime();
                if(motionEvent.getAction() == MotionEvent.ACTION_DOWN){
                    x1=motionEvent.getX();
                    v.vibrate(100);
                    startRecording();
                    text_send.setText("Recording.. Swipe to cancel");
                }else if(motionEvent.getAction() == MotionEvent.ACTION_UP){
                    x2=motionEvent.getX();
                    if(duration < CLICK_THRESHOLD){
                        Toast.makeText(MessageActivity.this, "Press and hold to record", Toast.LENGTH_SHORT).show();
                    }else{
                        v.vibrate(100);
                        if(x1 - x2 > MIN_SWIPE_DISTANCE){
                            isAudioCancel = true;
                            Toast.makeText(MessageActivity.this, "Recording cancelled", Toast.LENGTH_SHORT).show();
                        }
                        stopRecording();
                    }
                    text_send.setText("");
                }
                return false;
            }
        });

        btn_camera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(ContextCompat.checkSelfPermission(MessageActivity.this,
                        Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
                        ContextCompat.checkSelfPermission(MessageActivity.this,
                                Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
                    CheckPermission.checkPermission(
                            Manifest.permission.CAMERA,
                            CAMERA_PERMISSION_CODE, MessageActivity.this);
                    CheckPermission.checkPermission(
                            Manifest.permission.READ_EXTERNAL_STORAGE,
                            STORAGE_PERMISSION_CODE, MessageActivity.this);
                }
                Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                // Ensure that there's a camera activity to handle the intent
                if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                    // Create the File where the photo should go
                    photoFile = null;
                    cameraImage = new ImageUpload(MessageActivity.this);
                    try {
                        photoFile = cameraImage.createImageFile();
                    } catch (IOException ex) {
                        // Error occurred while creating the File
                        Toast.makeText(MessageActivity.this, ex.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                    // Continue only if the File was successfully created
                    if (photoFile != null) {
                        mediaUri = FileProvider.getUriForFile(MessageActivity.this,
                                "com.simrin.chatapp.provider",
                                photoFile);
                        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, mediaUri);
                        startActivityForResult(takePictureIntent, CAMERA_REQUEST);
                    }
                }
            }
        });
        userStatus = new UserStatus();
        userStatus.updateStatus(username, status, recyclerView, profile_image, userid, MessageActivity.this);
         sm = new SeenMessage();
         sm.seenMessage(userid);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        FileUtils fileUtils = new FileUtils(this);
        int height = messageLayout.getHeight();
        int width = messageLayout.getWidth();
        if(requestCode==IMAGE_REQUEST){
            if(resultCode == RESULT_OK){
                if(mediaUri!=null){
                    timeStamp = imageUpload.copyToPermanent(userid, tempImageFile);
                    thumbFile = createThumbnail(mediaUri, MessageActivity.this, timeStamp, userid);
                    sendPreview(imageUpload);
                }
            }else if(resultCode == RESULT_CANCELED){
                tempImageFile.delete();
            }
        }
        if(requestCode == VIDEO_REQUEST && resultCode==RESULT_OK){
            VideoUpload videoUpload = new VideoUpload(MessageActivity.this);
            try {
                tempVideoFile = videoUpload.createVideoFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
            if(tempVideoFile!=null){
                videoUpload.compressVideo(tempVideoFile.getPath(), mediaUri, userid, progressBar);
            }
        }
        if(requestCode == CAMERA_REQUEST && resultCode == RESULT_OK) {
            if (mediaUri != null && height != 0 && width != 0) {
                currentPhotoPath = photoFile.getAbsolutePath();
               CompressFile.compressImage(currentPhotoPath, currentPhotoPath, height, width, this);
                timeStamp = cameraImage.copyToPermanent(userid, photoFile);
                mediaUri = Uri.fromFile(new File(currentPhotoPath));
                thumbFile = createThumbnail(mediaUri, MessageActivity.this, timeStamp, userid);
                sendPreview(cameraImage);
            }
        }
        if(requestCode == MEDIA_REQUEST && resultCode == RESULT_OK &&
                data!= null && data.getData()!=null){
            mediaUri = data.getData();
            ContentResolver cr = getContentResolver();
            String mime = cr.getType(mediaUri);
            if(mime.toLowerCase().contains("video")) {
                Bundle extras = new Bundle();
                extras.putString("URLsender", String.valueOf(mediaUri));
                extras.putString("type", "video");
                startActivityForResult(new Intent(this, ImageViewActivity.class)
                    .putExtras(extras), VIDEO_REQUEST);
            }else if(mime.toLowerCase().contains("image")){
                imageUpload = new ImageUpload(MessageActivity.this);
                tempImageFile = imageUpload.createTempImageFile(height, width, mediaUri);
                mediaUri = Uri.fromFile(tempImageFile.getAbsoluteFile());
                final Bundle extras = new Bundle();
                extras.putString("URLsender", String.valueOf(mediaUri));
                extras.putString("type", "image");
                startActivityForResult(new Intent(this, ImageViewActivity.class)
                        .putExtras(extras), IMAGE_REQUEST);
            }
        }
    }

    public void openImage() {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_PICK);
        intent.setType("image/* | video/*");
//        String[] mimeTypes = {"image/*", "video/*"};
//        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
//        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(Intent.createChooser(intent, "Select Media"), MEDIA_REQUEST);
    }

    private void sendPreview(ImageUpload image){
        fuser = FirebaseAuth.getInstance().getCurrentUser();
        if(uploadTask != null && uploadTask.isInProgress()){
            Toast.makeText(this, "Upload in progress", Toast.LENGTH_SHORT).show();
        }else{
            image.uploadImage(fuser.getUid(), userid, mediaUri, progressBar, thumbFile);
        }

    }

    public void startRecording() {
        recorder = new MediaRecorder();
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        recorder.setOutputFile(audioFile);
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        try {
            recorder.prepare();
        } catch (IOException e) {
            Log.e(LOG_TAG, "prepare() failed");
        }
        recorder.start();
    }

    public void stopRecording() {
        if(recorder == null)
            Log.d("recorder", "null");
        else{
            recorder.stop();
            recorder.release();
            recorder = null;
            if(isAudioCancel){
                File tempFile = new File(audioFile);
                tempFile.delete();
                isAudioCancel=false;
            }else{
                audioHelper.uploadAudio(progressBar, audioFile);
            }
        }
    }

    public void currentUser(String userid){
        SharedPreferences.Editor editor = getSharedPreferences("PREFS", MODE_PRIVATE).edit();
        editor.putString("currentuser", userid);
        editor.apply();
    }

    @Override
    protected void onResume() {
        super.onResume();
        userStatus.status("online");
        currentUser(userid);
    }

    @Override
    protected void onPause() {
        super.onPause();
        sm.databaseReference.removeEventListener(sm.seenListener);
        userStatus.status("offline");
        currentUser("none");
    }

    @Override
    public void onStop() {
        super.onStop();
        if (recorder != null) {
            recorder.release();
            recorder = null;
        }
    }
}
