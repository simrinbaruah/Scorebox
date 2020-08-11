package com.simrin.scorebox.HelperClass;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.simrin.scorebox.Adapter.MessageAdapter;
import com.simrin.scorebox.MessageActivity;
import com.simrin.scorebox.Model.Chat;

import java.util.ArrayList;
import java.util.List;

public class ReadMessage {
    Context context;
    public ReadMessage(Context context){
        this.context = context;
    }
    public void readMessages(final String myid, final String userid, final RecyclerView recyclerView){
        final List<Chat> mChats = new ArrayList<>();

        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("Chats");
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
                MessageAdapter messageAdapter = new MessageAdapter(context, userid, mChats);
                 recyclerView.setAdapter(messageAdapter);

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }
}
