package com.simrin.scorebox.Adapter;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.simrin.scorebox.MessageActivity;
import com.simrin.scorebox.Model.Chat;
import com.simrin.scorebox.Model.User;
import com.simrin.scorebox.R;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.ViewHolder> {

    private Context mContext;
    private List<User> mUsers;
    private boolean ischat;

    String theLastMessage;
    String type;

    public UserAdapter(Context mContext, List<User> mUsers, boolean ischat){
        this.mUsers = mUsers;
        this.mContext = mContext;
        this.ischat = ischat;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.user_item, parent, false);
        return new UserAdapter.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        final User user = mUsers.get(position);
        holder.username.setText(user.getName());
        if(user.getImageURL().equals("default")){
            holder.profile_image.setImageResource(R.mipmap.ic_launcher);
        }
        else {
            Glide.with(mContext).load(user.getImageURL()).into(holder.profile_image);
        }

        if(ischat){
            lastMessage(user.getId(), holder.last_msg);
        }else{
            holder.last_msg.setVisibility(View.GONE);
        }
        if(ischat){
            if(user.getStatus().equals("online")){
                holder.img_on.setVisibility(View.VISIBLE);
                holder.img_off.setVisibility(View.GONE);
            } else {
                holder.img_on.setVisibility(View.GONE);
                holder.img_off.setVisibility(View.VISIBLE);
            }
        }

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(mContext, MessageActivity.class);
                intent.putExtra("userid", user.getId());
                mContext.startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return mUsers.size();

    }
    public class ViewHolder extends RecyclerView.ViewHolder{
        public TextView username;
        public ImageView profile_image;
        private ImageView img_on;
        private ImageView img_off;
        private TextView last_msg;

        public ViewHolder(View itemView){
            super(itemView);

            username = itemView.findViewById(R.id.username);
            profile_image = itemView.findViewById(R.id.profile_image);
            img_on = itemView.findViewById(R.id.img_on);
            img_off = itemView.findViewById(R.id.img_off);
            last_msg = itemView.findViewById(R.id.last_msg);
        }
    }

    private void lastMessage(final String userid, final TextView last_msg){
        theLastMessage = "default";
        type = "text";
        final FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Chats").child(firebaseUser.getUid()).child(userid);
        reference.keepSynced(true);

        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for(DataSnapshot snapshot: dataSnapshot.getChildren()){
                    Chat chat = snapshot.getValue(Chat.class);
                    assert chat != null;
                    //changes here app crashes when logout
                    if(chat.getReceiver().equals(userid) && chat.getSender().equals(firebaseUser.getUid()) ||
                            chat.getReceiver().equals(firebaseUser.getUid()) && chat.getSender().equals(userid)){
                        theLastMessage = chat.getMessage();
                        type = chat.getType();
                    }
                }

                if ("default".equals(theLastMessage)) {
                    last_msg.setText("No Message");
                } else {
                    if (type.equals("image")) {
                        last_msg.setText("Image");
                    } else if(type.equals("audio")){
                        last_msg.setText("Audio Message");
                    } else if(type.equals("video")) {
                        last_msg.setText("Video Message");
                    }else{
                        last_msg.setText(theLastMessage);
                    }
                }

                theLastMessage = "default";
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }
}
