package com.simrin.scorebox.HelperClass.ImageHelper;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;

import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.simrin.scorebox.HelperClass.FileExtension;
import com.simrin.scorebox.HelperClass.UriPath;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

import static com.simrin.scorebox.HelperClass.ImageHelper.FastBlur.fastblur;

public class Thumbnail {
    private static StorageReference storageReference = FirebaseStorage.getInstance().getReference("messages");

    public static StorageReference storeThumbnail(Context context, String type, String timeStamp, Uri thumbUri){

        String extension = FileExtension.getFileExtension(thumbUri, context);
        if(extension == null) extension = "jpg";
        StorageReference thumbRef;
        if(type.equals("image")){
            thumbRef = storageReference.child("images")
                    .child("thumbnails").child("THUMB_"+timeStamp
                            + "." +extension);
        }else {
            thumbRef = storageReference.child("video")
                    .child("thumbnails").child("THUMB_"+timeStamp
                            + "." +extension);
        }
        return thumbRef;
    }

    public static File createThumbnail(Uri mediaUri, Context context, String timestamp, String userid) {
        Bitmap bitmap;
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inMutable = true;
        bitmap = BitmapFactory.decodeFile(UriPath.getRealPathFromURI(mediaUri.toString(), context), options);
        bitmap = fastblur(bitmap, 0.5f, 20);
        return writeBitmap(bitmap, context, timestamp, userid);
    }

    private static File writeBitmap(Bitmap image, Context context, String timeStamp, String userid) {
        String imageFileName = "THUMB_" + timeStamp + ".jpg";
        File storageDir = new File(context.getFilesDir() + File.separator + userid +
                File.separator + "images" + File.separator + "thumbnail");
        boolean success = true;
        if (!storageDir.exists()) {
            success = storageDir.mkdirs();
        }
        if (success) {
            File thumbFile = new File(storageDir, imageFileName);
            String savedThumbPath = thumbFile.getAbsolutePath();
            try {
                OutputStream fOut = new FileOutputStream(thumbFile);
                image.compress(Bitmap.CompressFormat.JPEG, 20, fOut);
                fOut.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return thumbFile;
        }
        return null;
    }
}
