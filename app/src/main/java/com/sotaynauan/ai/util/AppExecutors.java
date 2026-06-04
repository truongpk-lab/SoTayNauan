package com.sotaynauan.ai.util;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class AppExecutors {
    private static final ExecutorService IO = Executors.newFixedThreadPool(2);
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private AppExecutors() {
    }

    public interface Task<T> {
        T run() throws Exception;
    }

    public interface Callback<T> {
        void onResult(T result);
    }

    public interface ErrorCallback {
        void onError(Exception exception);
    }

    public static <T> void runOnIo(Task<T> task, Callback<T> success, ErrorCallback error) {
        IO.execute(() -> {
            try {
                T result = task.run();
                MAIN.post(() -> success.onResult(result));
            } catch (Exception exception) {
                MAIN.post(() -> error.onError(exception));
            }
        });
    }

    public static void runOnIo(Runnable runnable) {
        IO.execute(runnable);
    }
}
