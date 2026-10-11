
package com.example.tractordiary;

import android.app.Application;
import com.google.firebase.database.FirebaseDatabase;

public class TractorDiaryApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // Enable persistent local storage for Firebase data.
        FirebaseDatabase.getInstance()
                .setPersistenceEnabled(true);
    }
}
