package com.emoji.ftp.utils.thread;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import androidx.annotation.NonNull;

public class SingleThreadExecutor implements Executor {

    private final ExecutorService mSingle;

    public SingleThreadExecutor() {
        mSingle = Executors.newSingleThreadExecutor();
    }

    @Override
    public void execute(@NonNull Runnable command) {
        mSingle.execute(command);
    }

    public void close() {
        mSingle.shutdown();
        try {
            if (!mSingle.awaitTermination(200, TimeUnit.MILLISECONDS)) {
                mSingle.shutdownNow();
            }
        } catch (InterruptedException e) {
            mSingle.shutdownNow();
        }
    }
}

