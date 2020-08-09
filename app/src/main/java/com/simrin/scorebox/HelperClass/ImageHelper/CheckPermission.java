package com.simrin.scorebox.HelperClass.ImageHelper;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.simrin.scorebox.MessageActivity;

public class CheckPermission {
    public static void checkPermission(String permission, int requestCode, Context context) {
        if (ContextCompat.checkSelfPermission(context, permission)
                == PackageManager.PERMISSION_DENIED) {
            // Requesting the permission
            ActivityCompat.requestPermissions((Activity)context,
                    new String[]{permission},
                    requestCode);
        }
    }
}
