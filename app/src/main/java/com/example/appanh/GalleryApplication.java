package com.example.appanh;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import com.google.firebase.auth.FirebaseAuth;
import java.util.ArrayList;
import java.util.List;

/** Warm up the cached authentication state without blocking the first frame. */
public final class GalleryApplication extends Application {
    private final Handler main = new Handler(Looper.getMainLooper());
    private final List<Runnable> waiting = new ArrayList<>();
    private boolean ready;
    private RuntimeException initializationError;

    @Override public void onCreate() {
        super.onCreate();
        ThemeManager.initialize(this);
        new Thread(() -> {
            try { FirebaseAuth.getInstance(); }
            catch (RuntimeException error) { initializationError = error; }
            main.post(() -> {
                ready = true;
                for (Runnable callback : new ArrayList<>(waiting)) callback.run();
                waiting.clear();
            });
        }, "authentication-startup").start();
    }

    public void whenReady(Runnable callback) {
        if (ready) callback.run(); else waiting.add(callback);
    }

    public RuntimeException getInitializationError() { return initializationError; }
}
