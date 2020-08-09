package com.simrin.scorebox.HelperClass;

import android.content.Context;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.simrin.scorebox.Model.Chat;
import com.simrin.scorebox.Model.User;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import static com.simrin.scorebox.Notifications.SendNotification.sendNotification;

public class SendMessage {
    private static FirebaseUser fuser;
    private static boolean notify = false;
    public static void sendMessage(String sender, final String receiver, final String message,
                                      final String thumbnail, final String type, final Context context){
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference();
        fuser = FirebaseAuth.getInstance().getCurrentUser();
        notify = true;

        DateFormat df = new SimpleDateFormat("dd-MMM-yyyy HH:mm");
        Calendar send_time = Calendar.getInstance();
        String time = df.format(send_time.getTime());

        Chat chats = new Chat();
        chats.setSender(sender);
        chats.setReceiver(receiver);
        chats.setMessage(message);
        chats.setImg_place(thumbnail);
        chats.setTimestamp(time);
        chats.setType(type);
        chats.setIsseen(false);

        reference.child("Chats").push().setValue(chats);

        final DatabaseReference chatRef = FirebaseDatabase.getInstance().getReference("Chatlist")
                .child(fuser.getUid())
                .child(receiver);
        chatRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(!dataSnapshot.exists()){
                    chatRef.child("id").setValue(receiver);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        final DatabaseReference chatRefReceiver = FirebaseDatabase.getInstance().getReference("Chatlist")
                .child(receiver)
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
                    switch (type) {
                        case "audio":
                            media = "Sent an audio";
                            break;
                        case "image":
                            media = "Sent an image";
                            break;
                        case "video":
                            media = "Sent a video";
                            break;
                        default:
                            media = message;
                            break;
                    }
                    sendNotification(receiver, user.getName(), media, context);
                }
                notify = false;
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }
}
