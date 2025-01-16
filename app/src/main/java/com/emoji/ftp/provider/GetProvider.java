package com.emoji.ftp.provider;

import android.app.Application;
import android.content.Context;

public class GetProvider {

    private static volatile GetProvider sInstance = null;

    /** 程序上下文 */
    private Context mContext;

    public static void initialize(Context context) {
        get().mContext = context;
    }

    private GetProvider(Context context) {
        this.mContext = context;
    }

    public static GetProvider get() {
        if (sInstance == null) {
            synchronized (GetProvider.class) {
                if (sInstance == null) {
                    if (MyProvider.context == null) {
                        throw new IllegalStateException("context == null");
                    }
                    sInstance = new GetProvider(MyProvider.context);
                }
            }
        }

        return sInstance;
    }

    /** 获取当前程序的上下文 */
    public Context getContext() {
        return mContext;
    }

    /**
     * 获取当前程序的application对象
     *
     * @return application
     */
    public Application getApplication() {
        return (Application) mContext.getApplicationContext();
    }
}
