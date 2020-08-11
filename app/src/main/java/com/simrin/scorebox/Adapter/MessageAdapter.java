package com.simrin.scorebox.Adapter;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.simrin.scorebox.HelperClass.ImageHelper.BasicImageDownloader;
import com.simrin.scorebox.HelperClass.ImageHelper.CheckPermission;
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
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.ViewHolder> {

   public static final int MSG_TYPE_LEFT=0;
   public static final int MSG_TYPE_RIGHT=1;
    private static final int STORAGE_PERMISSION_CODE = 102;

    private Context mContext;
    private List<Chat> mChats;
    private String userid;
    private MediaPlayer player;
    private File imageDirectory, videoDirectory, thumbDirectory, sbDirectory, audioDirectory;
    private boolean tobeDeleted = false;

    FirebaseUser fuser;

    public MessageAdapter(Context mContext, String userid, List<Chat> mChats){
        this.mChats = mChats;
        this.mContext = mContext;
        this.userid = userid;
        File sentImagesFolder = new File(mContext.getFilesDir() + File.separator + userid +
                File.separator + "images");
        Log.d("ImgFiles", "Path: " + sentImagesFolder.getPath());
        imageDirectory = new File(sentImagesFolder.getPath());

        File sentVideoFolder = new File(mContext.getFilesDir() + File.separator + userid +
                File.separator + "video");
        Log.d("VidFiles", "Path: " + sentVideoFolder.getPath());
        videoDirectory = new File(sentVideoFolder.getPath());

        File thumbImagesFolder =  new File(mContext.getFilesDir() + File.separator + userid +
                File.separator + "images" + File.separator + "thumbnail");
        Log.d("ThumbFiles", "Path: " + thumbImagesFolder.getPath());
        thumbDirectory = new File(thumbImagesFolder.getPath());

        File sbImagesFolder =  new File(Environment.getExternalStorageDirectory().getAbsolutePath() +
                File.separator + "Scorebox");
        Log.d("sbFiles", "Path: " + sbImagesFolder.getPath());
        sbDirectory = new File(sbImagesFolder.getPath());

        File audioFolder = new File(mContext.getFilesDir() + File.separator + userid +
                File.separator + "audio");
        Log.d("AudFiles", "Path: " + audioFolder.getPath());
        audioDirectory = new File(audioFolder.getPath());

    }

    @NonNull
    @Override
    public MessageAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if(viewType==MSG_TYPE_RIGHT) {
            View view = LayoutInflater.from(mContext).inflate(R.layout.chat_item_right, parent, false);
            return new ViewHolder(view);
        }else{
            View view = LayoutInflater.from(mContext).inflate(R.layout.chat_item_left, parent, false);
            return new ViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull final MessageAdapter.ViewHolder holder, int position) {
        final Chat chat=mChats.get(position);
        holder.sent_time.setText(chat.getTimestamp().split(" ")[1]);
        String message_type = chat.getType();
        switch (message_type) {
            case "image":
                holder.download_btn.setVisibility(View.VISIBLE);
                holder.show_message.setVisibility(View.GONE);
                holder.play_audio.setVisibility(View.GONE);
                holder.play_btn.setVisibility(View.GONE);
                holder.image_message.setVisibility(View.VISIBLE);
                String imageName = chat.getMessage();
                String placeholder = chat.getImg_place();
                String imageFileName = null;
                File finalImageFile = null;
                File thumbFile = null;
                if(placeholder!=null && placeholder.contains("THUMB")){
                    boolean thumbExists = false;
                    thumbFile = setThumbnail(placeholder, holder, chat);
                    if(thumbFile != null)
                        thumbExists = true;
                    if (imageName.contains("JPEG")) {
                        imageName = imageName.substring(imageName.indexOf("JPEG"), imageName.indexOf(".jpg"));
                        if(chat.getSender().equals(userid) && chat.getReceiver().equals(fuser.getUid())){
                            if(sbDirectory.exists()){
                                File sbFile =  new File(Environment.getExternalStorageDirectory().getAbsolutePath() +
                                        File.separator + "Scorebox" + File.separator + imageName + ".jpg");
                                if(sbFile.exists() && thumbExists && thumbFile.exists()){
                                    setGlideImage(sbFile, thumbFile, holder);
                                    chat.setImageExists(true);
                                    finalImageFile = sbFile;
                                }else{
                                    chat.setImageExists(false);
                                }
                            }
                        }else{
                            if (imageDirectory.exists()) {
                                File internalFile = new File(mContext.getFilesDir() + File.separator + userid +
                                        File.separator + "images" + File.separator + imageName + ".jpg");
                                if(internalFile.exists() && thumbExists && thumbFile.exists()){
                                    setGlideImage(internalFile, thumbFile, holder);
                                    chat.setImageExists(true);
                                    finalImageFile = internalFile;
                                }else{
                                    chat.setImageExists(false);
                                }
                            }
                        }
                        imageFileName = imageName;
                    }
                }
                else{
                    if (isValidContextForGlide(mContext)) {
                        Glide.with(mContext).load(chat.getMessage()).apply(new RequestOptions()
                                .fitCenter()
                                .diskCacheStrategy(DiskCacheStrategy.ALL))
                                .thumbnail(0.5f)
                                .into(holder.image_message);
                        tobeDeleted = true;
                    }
                }
                final String finalImageFileName = imageFileName;
                final File finalThumbFile = thumbFile;
                holder.download_btn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Log.d("finalImageFileName", finalImageFileName);
                        DownloadFile(chat.getMessage(), finalImageFileName, true, finalThumbFile, holder, chat);
                    }
                });

                if(chat.isImageExists()){
                    final File finalImageFile1 = finalImageFile;
                    holder.download_btn.setVisibility(View.GONE);
                    holder.image_message.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            Bundle extras = new Bundle();
                            if(finalImageFile1 != null){
                                extras.putString("URL", finalImageFile1.getAbsolutePath());
                                extras.putString("type", "image");
                                mContext.startActivity(new Intent(mContext, ImageViewActivity.class).putExtras(extras));
                            }
                        }
                    });
                }else if(!tobeDeleted){
                    holder.image_message.setOnClickListener(null);
                    if(chat.getSender().equals(userid) && chat.getReceiver().equals(fuser.getUid())){
                        holder.download_btn.setVisibility(View.VISIBLE);
                    }
                }
                if(tobeDeleted){
                    //holder.download_btn.setVisibility(View.GONE);
                    holder.image_message.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            Bundle extras = new Bundle();
                            extras.putString("URL", chat.getMessage());
                            extras.putString("type", "image");
                            mContext.startActivity(new Intent(mContext, ImageViewActivity.class).putExtras(extras));
                        }
                    });
                }
                break;
            case "video":
                holder.show_message.setVisibility(View.GONE);
                holder.play_audio.setVisibility(View.GONE);
                holder.play_btn.setVisibility(View.VISIBLE);
                holder.image_message.setVisibility(View.VISIBLE);
                String videoName = chat.getMessage();
                placeholder = chat.getImg_place();
                String videoFileName = null;
                File finalVideoFile = null;
                thumbFile = null;
                if(placeholder!=null && placeholder.contains("THUMB")) {
                    boolean thumbExists = false;
                    thumbFile = setThumbnail(placeholder, holder, chat);
                    if (thumbFile != null)
                        thumbExists = true;
                    if (videoName.contains("VID")) {
                        videoName = videoName.substring(videoName.indexOf("VID"), videoName.indexOf(".mp4"));
                        if(chat.getSender().equals(userid) && chat.getReceiver().equals(fuser.getUid())){
                            if(sbDirectory.exists()){
                                File sbFile =  new File(Environment.getExternalStorageDirectory().getAbsolutePath() +
                                        File.separator + "Scorebox" + File.separator + videoName + ".mp4");
                                if(sbFile.exists() && thumbExists && thumbFile.exists()){
                                    setGlideVideo(sbFile, thumbFile, holder);
                                    chat.setImageExists(true);
                                    finalVideoFile = sbFile;
                                }else{
                                    chat.setImageExists(false);

                                }
                            }
                        }else{
                            if (videoDirectory.exists()) {
                                File internalFile = new File(mContext.getFilesDir() + File.separator + userid
                                        + File.separator + "video" + File.separator + videoName + ".mp4");
                                if(internalFile.exists() && thumbExists && thumbFile.exists()){
                                    //setGlideforVideo
                                    setGlideVideo(internalFile, thumbFile, holder);
                                    chat.setImageExists(true);
                                    finalVideoFile = internalFile;
                                }else{
                                    chat.setImageExists(false);
                                }
                            }
                        }
                        videoFileName = videoName;
                    }
                }
                else{
                    if (isValidContextForGlide(mContext)) {
                        Glide.with(mContext).load(chat.getMessage()).apply(new RequestOptions()
                                .fitCenter()
                                .diskCacheStrategy(DiskCacheStrategy.ALL))
                                .thumbnail(0.5f)
                                .into(holder.image_message);
                        tobeDeleted = true;
                    }
                }
                final String finalVideoFileName = videoFileName;
                final File finalThumbVidFile = thumbFile;
                holder.download_btn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Log.d("finalImageFileName", finalVideoFileName);
                        downloadVideo(finalVideoFileName, finalThumbVidFile, holder, chat);
                    }
                });

                if(chat.isImageExists()){
                    final File finalVideoFile1 = finalVideoFile;
                    holder.download_btn.setVisibility(View.GONE);
                    holder.play_btn.setVisibility(View.VISIBLE);
                    holder.image_message.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            Bundle extras = new Bundle();
                            if(finalVideoFile1 != null){
                                extras.putString("URL", finalVideoFile1.getAbsolutePath());
                                extras.putString("type", "video");
                                mContext.startActivity(new Intent(mContext, ImageViewActivity.class).putExtras(extras));
                            }
                        }
                    });
                }else if(!tobeDeleted){
                    holder.image_message.setOnClickListener(null);
                    if(chat.getSender().equals(userid) && chat.getReceiver().equals(fuser.getUid())){
                        holder.download_btn.setVisibility(View.VISIBLE);
                        holder.play_btn.setVisibility(View.GONE);
                    }
                }
                if(tobeDeleted){
                    //holder.download_btn.setVisibility(View.GONE);
                    holder.image_message.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            Bundle extras = new Bundle();
                            extras.putString("URL", chat.getMessage());
                            extras.putString("type", "image");
                            mContext.startActivity(new Intent(mContext, ImageViewActivity.class).putExtras(extras));
                        }
                    });
                }
                break;
            case "audio":
                holder.download_btn.setVisibility(View.GONE);
                holder.show_message.setVisibility(View.GONE);
                holder.image_message.setVisibility(View.GONE);
                holder.play_btn.setVisibility(View.GONE);
                holder.play_audio.setVisibility(View.VISIBLE);
                String audioName = chat.getMessage();
                String audioFile = null;
                if(audioName.contains("AUD")){
                    audioName = audioName.substring(audioName.indexOf("AUD"), audioName.indexOf(".3gp"));
                    if(audioDirectory.exists()){
                        File internalFile = new File(mContext.getFilesDir() + File.separator + userid
                                + File.separator + "audio" + File.separator + audioName + ".3gp");
                        if(internalFile.exists()){
                            holder.play_audio.setText("Play Audio");
                            audioFile = internalFile.getAbsolutePath();
                            chat.setImageExists(true);
                        }else{
                            holder.play_audio.setText("Download");
                            chat.setImageExists(false);
                        }
                    }
                    final String finalAudioFile = audioFile;
                    final String finalAudioName = audioName;

                    holder.play_audio.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                if (holder.play_audio.getText().equals("Play Audio")) {
                            holder.play_audio.setText("Stop Audio");
                            startPlaying(finalAudioFile);
                            player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                                @Override
                                public void onCompletion(MediaPlayer mediaPlayer) {
                                    holder.play_audio.setText("Play Audio");
                                }
                            });
                        } else if (holder.play_audio.getText().equals("Stop Audio")) {
                            holder.play_audio.setText("Play Audio");
                            stopPlaying();
                        } else if(holder.play_audio.getText().equals("Download")){
                            downloadAudio(finalAudioName, holder, chat);
                        }
                    }
                        });
                        if (player == null) {
                            if(chat.isImageExists()){
                                holder.play_audio.setText("Play Audio");
                            }else{
                                holder.play_audio.setText("Download");
                            }
                        }

                }else{
                    holder.play_audio.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            if (holder.play_audio.getText().equals("Play Audio")) {
                                holder.play_audio.setText("Stop Audio");
                                startPlaying(chat.getMessage());
                                player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                                    @Override
                                    public void onCompletion(MediaPlayer mediaPlayer) {
                                        holder.play_audio.setText("Play Audio");
                                    }
                                });
                            } else if (holder.play_audio.getText().equals("Stop Audio")) {
                                holder.play_audio.setText("Play Audio");
                                stopPlaying();
                            }
                        }
                    });
                    if (player == null) {
                        holder.play_audio.setText("Play Audio");
                    }
                }
                break;

            default:
                holder.download_btn.setVisibility(View.GONE);
                holder.show_message.setVisibility(View.VISIBLE);
                holder.show_message.setText(chat.getMessage());
                holder.image_message.setVisibility(View.GONE);
                holder.play_btn.setVisibility(View.GONE);
                holder.play_audio.setVisibility(View.GONE);
                break;
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

    private File setThumbnail(String placeholder, ViewHolder holder, Chat chat){
        placeholder = placeholder.substring(placeholder.indexOf("THUMB"), placeholder.indexOf(".jpg"));
        holder.image_message.setOnClickListener(null);
        File thumbFile;
        if(thumbDirectory.exists()){
            File file = new File(mContext.getFilesDir() + File.separator + userid +
                    File.separator + "images" + File.separator + "thumbnail"
                    + File.separator + placeholder + ".jpg");
            if(file.exists()){
                setGlideThumbnail(file, holder);
                thumbFile = file;
                return thumbFile;
            }
            DownloadFile(chat.getImg_place(), placeholder, false, null, null, null);
            setGlideThumbnail(file, holder);
            thumbFile = file;
            return thumbFile;
        }else{
            boolean success = thumbDirectory.mkdirs();
            if(success){
                DownloadFile(chat.getImg_place(), placeholder, false, null, null, null);
                File file = new File(mContext.getFilesDir() + File.separator + userid +
                        File.separator + "images" + File.separator + "thumbnail"
                        + File.separator + placeholder + ".jpg");
                setGlideThumbnail(file, holder);
                thumbFile = file;
                return thumbFile;
            }
        }
        return null;
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

    public static class ViewHolder extends RecyclerView.ViewHolder{
        public TextView show_message;
        public TextView sent_time;
        public ImageView image_message;
        public ImageView play_btn;
        public TextView txt_seen;
        public TextView date;
        public Button play_audio;
        public ImageView download_btn;

        public ViewHolder(View itemView){
            super(itemView);

            show_message = itemView.findViewById(R.id.show_message);
            image_message = itemView.findViewById(R.id.image_message);
            play_btn = itemView.findViewById(R.id.play_btn);
            play_audio = itemView.findViewById(R.id.play_audio);
            txt_seen = itemView.findViewById(R.id.txt_seen);
            sent_time = itemView.findViewById(R.id.sent_time);
            date = itemView.findViewById(R.id.date);
            download_btn = itemView.findViewById(R.id.download_btn);
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

    public void DownloadFile(final String imageURL, final String name, final boolean isImage,
                             final File finalThumbFile, final ViewHolder holder, final Chat chat) {
        if(ContextCompat.checkSelfPermission(mContext,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            CheckPermission.checkPermission(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE, STORAGE_PERMISSION_CODE, mContext);
        }
        final ProgressDialog pd = new ProgressDialog(mContext);
        final BasicImageDownloader downloader = new BasicImageDownloader(new BasicImageDownloader.OnImageLoaderListener() {
            @Override
            public void onError(BasicImageDownloader.ImageError error) {
                Toast.makeText(mContext, "Error code " + error.getErrorCode() + ": " +
                        error.getMessage(), Toast.LENGTH_LONG).show();
                error.printStackTrace();
                if(isImage){
                    pd.dismiss();
                }
            }
            @Override
            public void onProgressChange(int percent) {
                if(isImage){
                    pd.setMessage("Downloading:"+percent+"%");
                    pd.show();
                }
            }

            @Override
            public void onComplete(Bitmap result) {

                final Bitmap.CompressFormat mFormat = Bitmap.CompressFormat.JPEG;
                final File imageFile;
                if(isImage){
                    imageFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath() +
                            File.separator + "Scorebox" + File.separator + name + ".jpg");
                }else{
                    imageFile = new File(mContext.getFilesDir() + File.separator + userid +
                            File.separator + "images" + File.separator + "thumbnail"
                            + File.separator + name + ".jpg");
                }

                BasicImageDownloader.writeToDisk(imageFile, result, new BasicImageDownloader.OnBitmapSaveListener() {
                    @Override
                    public void onBitmapSaved() {
                        if(isImage){
                            galleryAddPic(imageFile.getAbsolutePath());
                            //Toast.makeText(mContext, "Image Saved", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onBitmapSaveError(BasicImageDownloader.ImageError error) {
                        Toast.makeText(mContext, "Error code " + error.getErrorCode() + ": " +
                                error.getMessage(), Toast.LENGTH_LONG).show();
                        error.printStackTrace();
                    }
                }, mFormat, true);
                if(isImage){
                    pd.dismiss();
                    if(finalThumbFile!=null){
                        holder.download_btn.setVisibility(View.GONE);
                        holder.image_message.startAnimation(AnimationUtils.loadAnimation(mContext, android.R.anim.fade_in));
                        setGlideImage(imageFile, finalThumbFile, holder);
                        chat.setImageExists(true);
                        holder.image_message.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                Bundle extras = new Bundle();
                                extras.putString("URL", imageFile.getAbsolutePath());
                                extras.putString("type", "image");
                                mContext.startActivity(new Intent(mContext, ImageViewActivity.class).putExtras(extras));
                            }
                        });
                    }
                }
            }
        });
        boolean displayProgress;
        displayProgress = isImage;
        downloader.download(imageURL, displayProgress);
    }

    public void downloadVideo(final String videoName, final File thumbFile, final ViewHolder holder, final Chat chat) {
        final ProgressDialog pd = new ProgressDialog(mContext);
        StorageReference videoRef = FirebaseStorage.getInstance().getReference().child("messages")
                .child("video").child(videoName + ".mp4");
        if (ContextCompat.checkSelfPermission(mContext,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            CheckPermission.checkPermission(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE, STORAGE_PERMISSION_CODE, mContext);
        }
        if (!sbDirectory.exists()) {
            sbDirectory.mkdirs();
        }
        final File vidFile = new File(sbDirectory, videoName + ".mp4");

        videoRef.getFile(vidFile).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                // Local temp file has been created
                pd.dismiss();
                setGlideVideo(vidFile, thumbFile, holder);
                addVideo(videoName, vidFile);
                chat.setImageExists(true);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                Toast.makeText(mContext, exception.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                pd.dismiss();
                // Handle any errors
            }
        }).addOnProgressListener(new OnProgressListener<FileDownloadTask.TaskSnapshot>() {
            @Override
            public void onProgress(@NonNull FileDownloadTask.TaskSnapshot taskSnapshot) {
                double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
                Log.d("PROGRESS", "Upload is " + progress + "% done");
                int currentprogress = (int) Math.round(progress);
                pd.setMessage("Downloading:"+currentprogress+"%");
                pd.show();
            }
        });
    }

    public void downloadAudio(final String audioName, final ViewHolder holder, final Chat chat) {
        final ProgressDialog pd = new ProgressDialog(mContext);
        StorageReference audioRef = FirebaseStorage.getInstance().getReference().child("messages")
                .child("audio").child(audioName + ".3gp");
        if (ContextCompat.checkSelfPermission(mContext,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            CheckPermission.checkPermission(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE, STORAGE_PERMISSION_CODE, mContext);
        }
        if (!audioDirectory.exists()) {
            audioDirectory.mkdirs();
        }
        final File vidFile = new File(audioDirectory, audioName + ".3gp");

        audioRef.getFile(vidFile).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                // Local temp file has been created
                pd.dismiss();
                holder.play_audio.setText("Play Audio");
                chat.setImageExists(true);
                holder.play_audio.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (holder.play_audio.getText().equals("Play Audio")) {
                            holder.play_audio.setText("Stop Audio");
                            startPlaying(vidFile.getAbsolutePath());
                            player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                                @Override
                                public void onCompletion(MediaPlayer mediaPlayer) {
                                    holder.play_audio.setText("Play Audio");
                                }
                            });
                        } else if (holder.play_audio.getText().equals("Stop Audio")) {
                            holder.play_audio.setText("Play Audio");
                            stopPlaying();
                        }
                    }
                });
                if (player == null) {
                    if(chat.isImageExists()){
                        holder.play_audio.setText("Play Audio");
                    }else{
                        holder.play_audio.setText("Download");
                    }
                }

            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                Toast.makeText(mContext, exception.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                pd.dismiss();
                // Handle any errors
            }
        }).addOnProgressListener(new OnProgressListener<FileDownloadTask.TaskSnapshot>() {
            @Override
            public void onProgress(@NonNull FileDownloadTask.TaskSnapshot taskSnapshot) {
                double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
                Log.d("PROGRESS", "Upload is " + progress + "% done");
                int currentprogress = (int) Math.round(progress);
                pd.setMessage("Downloading:"+currentprogress+"%");
                pd.show();
            }
        });
    }

    public void addVideo(String videoName, File videoFile) {
        ContentValues values = new ContentValues(3);
        values.put(MediaStore.Video.Media.TITLE, "VID_"+videoName);
        values.put(MediaStore.Video.Media.MIME_TYPE, "video/mp4");
        values.put(MediaStore.Video.Media.DATA, videoFile.getAbsolutePath());
        mContext.getContentResolver().insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values);
    }

    public void setGlideThumbnail(File file, ViewHolder holder){
        if(isValidContextForGlide(mContext)){
            Glide.with(mContext).load(file).apply(new RequestOptions()
                    .fitCenter()
                    .diskCacheStrategy(DiskCacheStrategy.ALL))
                    .thumbnail(0.5f)
                    .into(holder.image_message);
        }
    }

    public void setGlideImage(File imagefile, File thumbFile, ViewHolder holder){
        Drawable drawable = Drawable.createFromPath(thumbFile.getAbsolutePath());
        if(isValidContextForGlide(mContext)){
            Glide.with(mContext).load(imagefile).apply(new RequestOptions()
                    .fitCenter()
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .override(1200, 1200))
                    .placeholder(drawable)
                    .thumbnail(0.05f)
                    .into(holder.image_message);
        }
    }

    public void setGlideVideo(File videoFile, File thumbFile, ViewHolder holder){
        Drawable drawable = Drawable.createFromPath(thumbFile.getAbsolutePath());
        if (isValidContextForGlide(mContext)) {
                        long interval = 100 * 1000;
                        RequestOptions options = new RequestOptions().frame(interval)
                                .diskCacheStrategy(DiskCacheStrategy.ALL);
                        Glide.with(mContext).asBitmap()
                                .load(videoFile)
                                .apply(options)
                                .placeholder(drawable)
                                .thumbnail(0.05f)
                                .into(holder.image_message);
                    }
    }

    public static boolean isValidContextForGlide(final Context context) {
        if (context == null) {
            return false;
        }
        if (context instanceof Activity) {
            final Activity activity = (Activity) context;
            return !activity.isDestroyed() && !activity.isFinishing();
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

    private void galleryAddPic(String currentPhotoPath) {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File f = new File(currentPhotoPath);
        Uri contentUri = Uri.fromFile(f);
        mediaScanIntent.setData(contentUri);
        mContext.sendBroadcast(mediaScanIntent);
    }
}
