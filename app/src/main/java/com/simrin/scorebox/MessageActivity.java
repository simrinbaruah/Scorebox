package com.simrin.scorebox;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;
import androidx.swiperefreshlayout.widget.CircularProgressDrawable;

import de.hdodenhof.circleimageview.CircleImageView;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
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
import com.simrin.scorebox.Model.Chat;
import com.simrin.scorebox.Model.User;
import com.simrin.scorebox.Notifications.Client;
import com.simrin.scorebox.Notifications.Data;
import com.simrin.scorebox.Notifications.MyResponse;
import com.simrin.scorebox.Notifications.Sender;
import com.simrin.scorebox.Notifications.Token;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

public class MessageActivity extends AppCompatActivity {

    String userid;

    TextView username, status;
    CircleImageView profile_image;

    FirebaseUser fuser;
    DatabaseReference databaseReference;

    ImageButton btn_send, btn_attach;
    EditText text_send;
   ProgressBar progressBar;

    MessageAdapter messageAdapter;
    List<Chat> mChats;

    RecyclerView recyclerView;

    Intent intent;

    ValueEventListener seenListener, receiverseenListener;

    APIService apiService;

    private Uri imageUri;

    boolean notify = false;

    private static final int IMAGE_REQUEST = 1;
    private StorageTask uploadTask;
    StorageReference storageReference;

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

        profile_image = findViewById(R.id.profile_image);
        status = findViewById(R.id.status);
        username = findViewById(R.id.username);
        btn_send = findViewById(R.id.btn_send);
        text_send = findViewById(R.id.text_send);
        btn_attach = findViewById(R.id.btn_attach);
        progressBar = findViewById(R.id.progressBar);
        progressBar.setVisibility(View.GONE);

        storageReference = FirebaseStorage.getInstance().getReference("message_images");

         intent = getIntent();
         userid = intent.getStringExtra("userid");
         fuser = FirebaseAuth.getInstance().getCurrentUser();

         btn_attach.setOnClickListener(new View.OnClickListener() {
             @Override
             public void onClick(View view) {
                openImage();
             }
         });

         btn_send.setOnClickListener(new View.OnClickListener() {
             @Override
             public void onClick(View view) {
                 notify = true;
                 String msg = text_send.getText().toString();
                 if(!("".equals(msg.trim()))){
                     String type = "text";
                     sendMessage(fuser.getUid(), userid, msg, type);
                 }
                 text_send.setText("");
             }
         });

         databaseReference = FirebaseDatabase.getInstance().getReference("Users").child(userid);

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

    private void seenMessage(final String userid){

        final ArrayList<String> id = new ArrayList<>();
        databaseReference = FirebaseDatabase.getInstance().getReference("Chats").child(fuser.getUid()).child(userid);
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
        receiverseenListener = databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for(DataSnapshot snapshot: dataSnapshot.getChildren()){
                    Chat chat=snapshot.getValue(Chat.class);
                    for(String i : id){
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
                    sendNotification(receiver, user.getName(), message);
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
                    Data data = new Data(fuser.getUid(), R.mipmap.logo2_foreground, username+": "+message,
                            "New Message", receiver);

                    Sender sender = new Sender(data, token.getToken());

                    apiService.sendNotification(sender)
                            .enqueue(new Callback<MyResponse>() {
                                @Override
                                public void onResponse(Call<MyResponse> call, Response<MyResponse> response) {
                                    if(response.code() == 200){
                                        if(response.body().success!=1){
                                            Toast.makeText(MessageActivity.this, "Failed!", Toast.LENGTH_SHORT).show();
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
                messageAdapter = new MessageAdapter(MessageActivity.this, mChats, imageURL);
                recyclerView.setAdapter(messageAdapter);
                ((SimpleItemAnimator) recyclerView.getItemAnimator()).setSupportsChangeAnimations(false);

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
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent, IMAGE_REQUEST);
    }
    private String getFileExtension(Uri uri){
        ContentResolver contentResolver = getContentResolver();
        MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();
        return mimeTypeMap.getExtensionFromMimeType(contentResolver.getType(uri));
    }

    private void uploadImage(final String sender, final String receiver){

        if(imageUri != null){
            progressBar.setVisibility(View.VISIBLE);
            final StorageReference fileReference = storageReference.child(System.currentTimeMillis()
                    + "." +getFileExtension(imageUri));
            uploadTask = fileReference.putFile(imageUri);
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
                        String type = "image";
                       sendMessage(sender, receiver, mUri, type);
                        progressBar.setVisibility(View.GONE);

                    }else {
                        Toast.makeText(MessageActivity.this, "Failed to attach photo!", Toast.LENGTH_SHORT).show();
                        progressBar.setVisibility(View.GONE);
                    }
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Toast.makeText(MessageActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.GONE);
                }
            });
        }else{
            Toast.makeText(MessageActivity.this, "No image selected", Toast.LENGTH_SHORT).show();
            progressBar.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == IMAGE_REQUEST && resultCode == RESULT_OK
                && data!=null && data.getData() != null){
            fuser = FirebaseAuth.getInstance().getCurrentUser();

            imageUri = data.getData();

            if(uploadTask != null && uploadTask.isInProgress()){
                Toast.makeText(this, "Upload in progress", Toast.LENGTH_SHORT).show();
            }else{
                uploadImage(fuser.getUid(), userid);
            }
        }
    }
    private void status(String status){
        databaseReference = FirebaseDatabase.getInstance().getReference("Users").child(fuser.getUid());

        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("status", status);

        databaseReference.updateChildren(hashMap);
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
}
