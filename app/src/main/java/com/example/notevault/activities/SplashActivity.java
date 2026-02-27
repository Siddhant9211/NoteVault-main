package com.example.notevault.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.notevault.R;
import com.example.notevault.firebase.FirebaseManager;
import com.google.firebase.auth.FirebaseUser;

/**
 * Splash screen that briefly shows the logo and then routes
 * to either Login or Main depending on authentication state.
 */
public class SplashActivity extends AppCompatActivity {

    private static final long SPLASH_DELAY_MS = 1500L;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            FirebaseUser user = FirebaseManager.getInstance().getCurrentUser();
            if (user != null) {
                startActivity(new Intent(SplashActivity.this, FolderActivity.class));
            } else {
                startActivity(new Intent(SplashActivity.this, LoginActivity.class));
            }
            finish();
        }, SPLASH_DELAY_MS);
    }
}

