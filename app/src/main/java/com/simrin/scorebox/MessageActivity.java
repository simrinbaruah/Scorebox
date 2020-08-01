package com.simrin.scorebox;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import de.hdodenhof.circleimageview.CircleImageView;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Vibrator;

import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnPausedListener;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.StorageTask;
import com.google.firebase.storage.UploadTask;
import com.simrin.scorebox.Adapter.MessageAdapter;
import com.simrin.scorebox.Fragments.APIService;
import com.simrin.scorebox.HelperClass.CompressFile;
import com.simrin.scorebox.HelperClass.VideoCompressor.VideoCompress;
import com.simrin.scorebox.Model.Chat;
import com.simrin.scorebox.Model.User;
import com.simrin.scorebox.Notifications.Client;
import com.simrin.scorebox.Notifications.Data;
import com.simrin.scorebox.Notifications.MyResponse;
import com.simrin.scorebox.Notifications.Sender;
import com.simrin.scorebox.Notifications.Token;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class MessageActivity extends AppCompatActivity {

    String userid, currentPhotoPath, imageFileName, videoFileName;

    RelativeLayout messageLayout;

    TextView username, status;
    CircleImageView profile_image;

    FirebaseUser fuser;
    DatabaseReference databaseReference;

    ImageButton btn_send, btn_attach, btn_voice, btn_camera;
    EditText text_send;
    ProgressBar progressBar;

    MessageAdapter messageAdapter;
    List<Chat> mChats;

    RecyclerView recyclerView;

    Intent intent;

    ValueEventListener seenListener, receiverseenListener;

    APIService apiService;

    private Uri mediaUri;

    boolean notify = false;

    private static final int MEDIA_REQUEST = 1;
    private static final int IMAGE_REQUEST = 2;
    private static final int CAMERA_REQUEST = 3;
    private static final int VIDEO_REQUEST = 4;
    private StorageTask uploadTask;

    StorageReference storageReference;

    private static final String LOG_TAG = "AudioRecordTest";
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private static final int CAMERA_PERMISSION_CODE = 100;
    private static final int STORAGE_PERMISSION_CODE = 101;
    private File tempVideoFile = null;
    private File tempImageFile = null;
    private static String fileName = null;
    private static final int MIN_SWIPE_DISTANCE = 150;
    private float x1,x2;
    private boolean isAudioCancel = false;
    private MediaRecorder recorder;
    private static int CLICK_THRESHOLD = 1000;

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

        apiService = Client.getClient("https://fcm.googleapis.com/").create(APIService.class);


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
                            notify = true;
                            String type = "text";
                            sendMessage(fuser.getUid(), userid, msg, type);
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

        fileName = getExternalCacheDir().getAbsolutePath();
        fileName += "/"+System.currentTimeMillis()+".3gp";
        final Vibrator v = (Vibrator)getSystemService(Context.VIBRATOR_SERVICE);


        btn_voice.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if(ContextCompat.checkSelfPermission(MessageActivity.this,
                        Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED){
                    checkPermission(
                            Manifest.permission.RECORD_AUDIO,
                            REQUEST_RECORD_AUDIO_PERMISSION);
                }else{
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
                    checkPermission(
                            Manifest.permission.CAMERA,
                            CAMERA_PERMISSION_CODE);
                    checkPermission(
                            Manifest.permission.READ_EXTERNAL_STORAGE,
                            STORAGE_PERMISSION_CODE);
                }else{
                    Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    // Ensure that there's a camera activity to handle the intent
                    if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                        // Create the File where the photo should go
                        File photoFile = null;
                        try {
                            photoFile = createImageFile();
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
            }
        });

         databaseReference = FirebaseDatabase.getInstance().getReference("Users").child(userid);
         databaseReference.keepSynced(true);
         databaseReference.addValueEventListener(new ValueEventListener() {
             @Override
             public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                 User user = dataSnapshot.getValue(User.class);
                 username.setText(user.getName());
                 if(user.getImageURL().equals("default")){
                     profile_image.setImageResource(R.mipmap.ic_launcher);
                 } else {
                     Glide.with(getApplicationContext()).load(user.getImageURL()).into(profile_image);
                 }
                 if(user.getStatus().equals("online")){
                     status.setText("online");
                 }else{
                     status.setText("offline");
                 }
                 readMessages(fuser.getUid(), userid, user.getImageURL());
             }

             @Override
             public void onCancelled(@NonNull DatabaseError databaseError) {

             }
         });

         seenMessage(userid);
    }

    public void checkPermission(String permission, int requestCode) {
        if (ContextCompat.checkSelfPermission(MessageActivity.this, permission)
                == PackageManager.PERMISSION_DENIED) {
            // Requesting the permission
            ActivityCompat.requestPermissions(MessageActivity.this,
                    new String[]{permission},
                    requestCode);
        }
    }

    private void seenMessage(final String userid){

        final ArrayList<String> id = new ArrayList<>();
        databaseReference = FirebaseDatabase.getInstance().getReference("Chats").child(fuser.getUid()).child(userid);
        databaseReference.keepSynced(true);
        seenListener = databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for(DataSnapshot snapshot: dataSnapshot.getChildren()){
                    Chat chat=snapshot.getValue(Chat.class);
                    if(chat.getReceiver().equals(fuser.getUid())
                            && chat.getSender().equals(userid)){
                        HashMap<String, Object> hashMap = new HashMap<>();
                        hashMap.put("isseen", true);
                        id.add(chat.getId());
                        snapshot.getRef().updateChildren(hashMap);
                        FirebaseDatabase.getInstance().getReference("Chats").child(userid).child(fuser.getUid())
                                .child(chat.getId()).updateChildren(hashMap);

                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });


        databaseReference = FirebaseDatabase.getInstance().getReference("Chats").child(userid).child(fuser.getUid());
        databaseReference.keepSynced(true);
        receiverseenListener = databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for(DataSnapshot snapshot: dataSnapshot.getChildren()){
                    Chat chat=snapshot.getValue(Chat.class);
                    for(String i : id){
                        //id error
                        if(chat.getId().equals(i)){
                            HashMap<String, Object> hashMap = new HashMap<>();
                            hashMap.put("isseen", true);
                            snapshot.getRef().updateChildren(hashMap);
                            FirebaseDatabase.getInstance().getReference("Chats").child(fuser.getUid())
                                    .child(userid).child(chat.getId()).updateChildren(hashMap);
                        }
                    }

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void sendMessage(String sender, final String receiver, final String message, final String type){
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference();

        DateFormat df = new SimpleDateFormat("dd-MMM-yyyy HH:mm");
        Calendar send_time = Calendar.getInstance();
        String time = df.format(send_time.getTime());

        Chat chats = new Chat();
        chats.setSender(sender);
        chats.setReceiver(receiver);
        chats.setMessage(message);
        chats.setTimestamp(time);
        chats.setType(type);
        chats.setIsseen(false);


        String key = reference.child("Chats").child(fuser.getUid()).child(userid).push().getKey();
        Log.d("key", key);
        chats.setId(key);
        reference.child("Chats").child(fuser.getUid()).child(userid).child(key).setValue(chats);
        reference.child("Chats").child(userid).child(fuser.getUid()).child(key).setValue(chats);

        final DatabaseReference chatRef = FirebaseDatabase.getInstance().getReference("Chatlist")
                .child(fuser.getUid())
                .child(userid);
        chatRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(!dataSnapshot.exists()){
                    chatRef.child("id").setValue(userid);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        final DatabaseReference chatRefReceiver = FirebaseDatabase.getInstance().getReference("Chatlist")
                .child(userid)
                .child(fuser.getUid());
        chatRefReceiver.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                if(!dataSnapshot.exists()){
                    chatRefReceiver.child("id").setValue(fuser.getUid());
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });


        reference = FirebaseDatabase.getInstance().getReference("Users").child(fuser.getUid());
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                User user = dataSnapshot.getValue(User.class);
                if(notify) {
                    String media;
                    if(type.equals("audio")){
                        media="Sent an audio";
                    }else if(type.equals("image")){
                        media ="Sent an image";
                    }else{
                        media = message;
                    }
                    sendNotification(receiver, user.getName(), media);
                }
                    notify = false;
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void sendNotification(final String receiver, final String username, final String message){
        DatabaseReference tokens = FirebaseDatabase.getInstance().getReference("Tokens");
        Query query = tokens.orderByKey().equalTo(receiver);
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for(DataSnapshot snapshot: dataSnapshot.getChildren()){
                    Token token = snapshot.getValue(Token.class);
                    Data data = new Data(fuser.getUid(), R.drawable.ic_noti, username+": "+message,
                            "New Message", receiver);

                    Sender sender = new Sender(data, token.getToken());
                    apiService.sendNotification(sender).enqueue(new Callback<MyResponse>() {
                        @Override
                        public void onResponse(Call<MyResponse> call, Response<MyResponse> response) {
                            if(response.code() == 200){
                                if(response.body().success!=1){
                                    Toast.makeText(MessageActivity.this, "Failed! (Sending notification)", Toast.LENGTH_SHORT).show();
                                }
                            }
                        }
                        @Override
                        public void onFailure(Call<MyResponse> call, Throwable t) {

                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void readMessages(final String myid, final String userid, final String imageURL){
        mChats = new ArrayList<>();

        databaseReference = FirebaseDatabase.getInstance().getReference("Chats").child(myid).child(userid);
        databaseReference.keepSynced(true);
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                mChats.clear();
                for(DataSnapshot snapshot : dataSnapshot.getChildren()){
                    Chat chats = snapshot.getValue(Chat.class);

                    if(chats.getReceiver().equals(myid) && chats.getSender().equals(userid) ||
                            chats.getReceiver().equals(userid) && chats.getSender().equals(myid))
                        mChats.add(chats);
                }
                messageAdapter = new MessageAdapter(MessageActivity.this, userid, mChats, imageURL);
                recyclerView.setAdapter(messageAdapter);

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void currentUser(String userid){
        SharedPreferences.Editor editor = getSharedPreferences("PREFS", MODE_PRIVATE).edit();
        editor.putString("currentuser", userid);
        editor.apply();
    }

    private void openImage() {
        Intent intent = new Intent();
        intent.setType("image/* video/*");
        intent.setAction(Intent.ACTION_PICK);
        startActivityForResult(intent, MEDIA_REQUEST);
    }
    private String getFileExtension(Uri uri){
        ContentResolver contentResolver = getContentResolver();
        MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();
        return mimeTypeMap.getExtensionFromMimeType(contentResolver.getType(uri));
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        imageFileName = "JPEG_" + timeStamp;
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );
        // Save a file: path for use with ACTION_VIEW intents
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    private void uploadImage(final String sender, final String receiver){

        if(mediaUri != null){
            progressBar.setVisibility(View.VISIBLE);
            String extension = getFileExtension(mediaUri);
            imageFileName = imageFileName.split("\\.", -1)[0];
            Log.d("ImageFileName", imageFileName);
            if(extension == null) extension = "jpg";
            final StorageReference fileReference = storageReference.child("images").child(imageFileName
                    + "." +extension);
            uploadTask = fileReference.putFile(mediaUri);
            uploadingProcess("image", sender, receiver, fileReference);
        }else{
            Toast.makeText(MessageActivity.this, "No image selected", Toast.LENGTH_SHORT).show();
            progressBar.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        int height = messageLayout.getHeight();
        int width = messageLayout.getWidth();
        if(requestCode==IMAGE_REQUEST && resultCode == RESULT_OK){
            if(mediaUri!=null){
                final File permImageFile = new File(getFilesDir() + File.separator + userid +
                        File.separator + "images" + File.separator + imageFileName + ".jpg");
                try {
                    copyFile(tempImageFile, permImageFile);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                sendPreview();
            }
        }
        if(requestCode==IMAGE_REQUEST && resultCode == RESULT_CANCELED){
            tempImageFile.delete();
        }
        if(requestCode == VIDEO_REQUEST && resultCode==RESULT_OK){
            try {
                tempVideoFile = createVideoFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
            if(tempVideoFile!=null){
                Log.d("videoFileName", videoFileName);
                compressVideo(tempVideoFile.getPath());
            }

        }
        if(requestCode == CAMERA_REQUEST && resultCode == RESULT_OK) {
            if (mediaUri != null && height != 0 && width != 0) {
                CompressFile.compressImage(currentPhotoPath, currentPhotoPath, height, width, this);
                final File permImageFile = new File(getFilesDir() + File.separator + userid + "images" +
                        File.separator + imageFileName);
                try {
                    copyFile(new File(currentPhotoPath), permImageFile);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                mediaUri = Uri.fromFile(new File(currentPhotoPath));
                sendPreview();
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
                Bundle extras = new Bundle();
                try {
                    tempImageFile = createImageFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if(tempImageFile!=null && height != 0 && width != 0){
                    String path = getRealPathFromURI(mediaUri.toString());
                    String destPath = tempImageFile.getAbsolutePath();
                    CompressFile.compressImage(path, destPath, height, width, this);
                    mediaUri = Uri.fromFile(new File(destPath));
                }
                extras.putString("URLsender", String.valueOf(mediaUri));
                extras.putString("type", "image");

                            startActivityForResult(new Intent(this, ImageViewActivity.class)
                    .putExtras(extras), IMAGE_REQUEST);
            }
        }
    }

    private void compressVideo(String path) {
        final ProgressDialog pd = new ProgressDialog(this);
        VideoCompress.compressVideoLow(getRealPathFromURI(mediaUri.toString()), path, new VideoCompress.CompressListener() {
            @Override
            public void onStart() {

            }

            @Override
            public void onSuccess() {
                pd.dismiss();
                try {
                    uploadVideo();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFail() {
                Toast.makeText(MessageActivity.this, "Compression Failed", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onProgress(float percent) {
                pd.setMessage("Compressing:"+(int)percent+"%");
                pd.show();
            }
        });
    }

    private void sendPreview(){
        fuser = FirebaseAuth.getInstance().getCurrentUser();
        if(uploadTask != null && uploadTask.isInProgress()){
            Toast.makeText(this, "Upload in progress", Toast.LENGTH_SHORT).show();
        }else{
            uploadImage(fuser.getUid(), userid);
        }

    }

    private void startRecording() {
        recorder = new MediaRecorder();
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        recorder.setOutputFile(fileName);
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

        try {
            recorder.prepare();
        } catch (IOException e) {
            Log.e(LOG_TAG, "prepare() failed");
        }

        recorder.start();
    }

    private void stopRecording() {
        recorder.stop();
        recorder.release();
        recorder = null;
        if(isAudioCancel){
            isAudioCancel=false;
        }else{
            uploadAudio();
        }
    }

    private void uploadAudio() {
        progressBar.setVisibility(View.VISIBLE);
        StorageReference filepath = storageReference.child("audio").child(System.currentTimeMillis()+".3gp");
        Uri uri = Uri.fromFile(new File(fileName));
        uploadTask = filepath.putFile(uri);
        uploadingProcess("audio", fuser.getUid(), userid, filepath);
    }

    private void uploadVideo() throws IOException {
        progressBar.setVisibility(View.VISIBLE);
        StorageReference filepath = storageReference.child("video").child(videoFileName + ".mp4");
        final File permVideoFile = new File(getFilesDir() + File.separator + userid
                + File.separator + "video" + File.separator + videoFileName + ".mp4");
        copyFile(tempVideoFile, permVideoFile);
        Uri uri = Uri.fromFile(tempVideoFile);
        uploadTask = filepath.putFile(uri);
        uploadingProcess("video", fuser.getUid(), userid, filepath);
    }

    private void status(String status){
        databaseReference = FirebaseDatabase.getInstance().getReference("Users").child(fuser.getUid());

        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("status", status);

        databaseReference.updateChildren(hashMap);
    }

    private void uploadingProcess(final String type, final String sender, final String receiver, final StorageReference fileReference){
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
        });;
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
                    String mUri = downloadUri.toString();
                    sendMessage(sender, receiver, mUri, type);
                    progressBar.setVisibility(View.GONE);

                }else {
                    Toast.makeText(MessageActivity.this, "Failed to attach photo/audio!", Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.GONE);
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(MessageActivity.this, e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                progressBar.setVisibility(View.GONE);
            }
        });
    }


    private String getRealPathFromURI(String contentURI) {
        Uri contentUri = Uri.parse(contentURI);
        Cursor cursor = getContentResolver().query(contentUri, null, null, null, null);
        if (cursor == null) {
            return contentUri.getPath();
        } else {
            cursor.moveToFirst();
            int index = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
            return cursor.getString(index);
        }
    }

    private File createVideoFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        videoFileName = "VID_" + timeStamp;
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_MOVIES);
        File video = File.createTempFile(
                videoFileName,  /* prefix */
                ".mp4",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        return video;
    }

    public static void copyFile(File sourceFile, File destFile) throws IOException {
        if (!destFile.getParentFile().exists())
            destFile.getParentFile().mkdirs();

        if (!destFile.exists()) {
            destFile.createNewFile();
        }

        FileChannel source = null;
        FileChannel destination = null;

        try {
            source = new FileInputStream(sourceFile).getChannel();
            destination = new FileOutputStream(destFile).getChannel();
            destination.transferFrom(source, 0, source.size());
        } finally {
            if (source != null) {
                source.close();
            }
            if (destination != null) {
                destination.close();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        status("online");
        currentUser(userid);

    }

    @Override
    protected void onPause() {
        super.onPause();
        databaseReference.removeEventListener(seenListener);
        databaseReference.removeEventListener(receiverseenListener);
        status("offline");
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
