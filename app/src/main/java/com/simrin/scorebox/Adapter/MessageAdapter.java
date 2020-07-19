package com.simrin.scorebox.Adapter;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.simrin.scorebox.ImageViewActivity;
import com.simrin.scorebox.Model.Chat;
import com.simrin.scorebox.R;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.ViewHolder> {

   public static final int MSG_TYPE_LEFT=0;
   public static final int MSG_TYPE_RIGHT=1;

    private Context mContext;
    private List<Chat> mChats;
    private String imageURL;
    private MediaPlayer player;
    private boolean complete = false;

    FirebaseUser fuser;

    public MessageAdapter(Context mContext, List<Chat> mChats, String imageURL){
        this.mChats = mChats;
        this.mContext = mContext;
        this.imageURL=imageURL;
    }

    @NonNull
    @Override
    public MessageAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if(viewType==MSG_TYPE_RIGHT) {
            View view = LayoutInflater.from(mContext).inflate(R.layout.chat_item_right, parent, false);
            return new MessageAdapter.ViewHolder(view);
        }else{
            View view = LayoutInflater.from(mContext).inflate(R.layout.chat_item_left, parent, false);
            return new MessageAdapter.ViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull final MessageAdapter.ViewHolder holder, int position) {
        final Chat chat=mChats.get(position);
        holder.sent_time.setText(chat.getTimestamp().split(" ")[1]);
        String message_type = chat.getType();
        if(message_type.equals("image")) {
            holder.show_message.setVisibility(View.GONE);
            holder.play_audio.setVisibility(View.GONE);
            holder.image_message.setVisibility(View.VISIBLE);
            if (isValidContextForGlide(mContext)) {
                Glide.with(mContext).load(chat.getMessage()).apply(new RequestOptions()
                        .fitCenter()
                        .skipMemoryCache(true))
                        .thumbnail(0.1f)
                        .into(holder.image_message);
            }
            holder.image_message.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mContext.startActivity(new Intent(mContext, ImageViewActivity.class).putExtra("URL", chat.getMessage()));
                }
            });
        }else if(message_type.equals("audio")){
            holder.show_message.setVisibility(View.GONE);
            holder.image_message.setVisibility(View.GONE);
            holder.play_audio.setVisibility(View.VISIBLE);
            holder.play_audio.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(holder.play_audio.getText().equals("Play Audio")){
                        holder.play_audio.setText("Stop Audio");
                        startPlaying(chat.getMessage());
                        player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                            @Override
                            public void onCompletion(MediaPlayer mediaPlayer) {
                                holder.play_audio.setText("Play Audio");
                            }
                        });
                    }else if(holder.play_audio.getText().equals("Stop Audio")){
                        holder.play_audio.setText("Play Audio");
                        stopPlaying();
                    }
                }
            });
            if(player==null){
                holder.play_audio.setText("Play Audio");
            }
        }else{
            holder.show_message.setVisibility(View.VISIBLE);
            holder.show_message.setText(chat.getMessage());
            holder.image_message.setVisibility(View.GONE);
            holder.play_audio.setVisibility(View.GONE);
        }

        if(position == mChats.size() - 1){
            if(chat.isIsseen()){
                holder.txt_seen.setText("Seen");
            }else{
                holder.txt_seen.setText("Delivered");
            }
        } else {
            holder.txt_seen.setVisibility(View.GONE);
        }

        String previous_date = "";
        if(position>=1){
            Chat previous_message = mChats.get(position-1);
            previous_date = previous_message.getTimestamp().split(" ")[0];
        }
        setDateTextVisibility(chat.getTimestamp().split(" ")[0], previous_date, holder.date);

    }

    private void setDateTextVisibility(String new_date, String previous_date, TextView date) {
        if(previous_date.isEmpty()){
            date.setVisibility(View.VISIBLE);
            String final_date = convertDate(new_date);
            date.setText(final_date);
        }else {
            if (previous_date.equals(new_date)) {
                date.setVisibility(View.GONE);
                date.setText("");
            } else {
                date.setVisibility(View.VISIBLE);
                String final_date = convertDate(new_date);
                date.setText(final_date);
            }
        }
    }

    private String convertDate(String string_date){
        SimpleDateFormat format = new SimpleDateFormat("dd-MMM-yyyy");
        try {
            Date date = format.parse(string_date);
            if(DateUtils.isToday(date.getTime())){
                string_date = "Today";
            }else if(DateUtils.isToday(date.getTime() + DateUtils.DAY_IN_MILLIS)){
                string_date = "Yesterday";
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return string_date;
    }

    @Override
    public int getItemCount() {
        return mChats.size();

    }

    public class ViewHolder extends RecyclerView.ViewHolder{
        public TextView show_message;
        public TextView sent_time;
        public ImageView image_message;
        public TextView txt_seen;
        public TextView date;
        public Button play_audio;

        public ViewHolder(View itemView){
            super(itemView);

            show_message = itemView.findViewById(R.id.show_message);
            image_message = itemView.findViewById(R.id.image_message);
            play_audio = itemView.findViewById(R.id.play_audio);
            txt_seen = itemView.findViewById(R.id.txt_seen);
            sent_time = itemView.findViewById(R.id.sent_time);
            date = itemView.findViewById(R.id.date);
            date.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemViewType(int position) {
        fuser= FirebaseAuth.getInstance().getCurrentUser();
        if(mChats.get(position).getSender().equals(fuser.getUid()))
            return MSG_TYPE_RIGHT;
        else
            return MSG_TYPE_LEFT;
    }

    public static boolean isValidContextForGlide(final Context context) {
        if (context == null) {
            return false;
        }
        if (context instanceof Activity) {
            final Activity activity = (Activity) context;
            if (activity.isDestroyed() || activity.isFinishing()) {
                return false;
            }
        }
        return true;
    }

    private void startPlaying(String fileName) {
        player = new MediaPlayer();
        try {
            player.setDataSource(fileName);
            player.prepare();
            player.start();
        } catch (IOException e) {
            Log.e("AUDIO PLAYING", "prepare() failed");
        }
    }


    private void stopPlaying() {
        player.release();
        player = null;
    }
}
