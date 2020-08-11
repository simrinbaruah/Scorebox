package com.simrin.scorebox.HelperClass;

import android.content.Context;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.simrin.scorebox.MessageActivity;
import com.simrin.scorebox.Model.User;
import com.simrin.scorebox.R;

import java.util.HashMap;

import de.hdodenhof.circleimageview.CircleImageView;

public class UserStatus {
    final  FirebaseUser fuser = FirebaseAuth.getInstance().getCurrentUser();
    public UserStatus(){

    }
    public  void updateStatus(final TextView username, final TextView status, final RecyclerView recyclerView,
                                    final CircleImageView profile_image, final String userid, final Context context){
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("Users").child(userid);
        databaseReference.keepSynced(true);
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                User user = dataSnapshot.getValue(User.class);
                username.setText(user.getName());
                if(user.getImageURL().equals("default")){
                    profile_image.setImageResource(R.mipmap.ic_launcher);
                } else {
                    Glide.with(context).load(user.getImageURL()).into(profile_image);
                }
                if(user.getStatus().equals("online")){
                    status.setText("online");
                }else{
                    status.setText("offline");
                }
                ReadMessage read = new ReadMessage(context);
                read.readMessages(fuser.getUid(), userid, recyclerView);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    public void status(String status){
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("Users").child(fuser.getUid());
        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("status", status);
        databaseReference.updateChildren(hashMap);
    }
}
