package com.simrin.scorebox;

import android.app.Application;

import com.google.firebase.database.FirebaseDatabase;


public class Scorebox extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        FirebaseDatabase.getInstance().setPersistenceEnabled(true);
    }
}
