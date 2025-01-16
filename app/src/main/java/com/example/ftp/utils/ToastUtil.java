package com.example.ftp.utils;

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.example.ftp.databinding.LayoutToastBinding;

public class ToastUtil {

    private static Toast sToast;
    public static String tempSftpPlayerErrorToast = "";

    public static void showToast(Context context, int strId) {
        showToast(context, context.getResources().getString(strId));
    }

    public static void showToast(Context context, String content) {
        showToast(context, content, 1500);
    }

    public static void showToast(Context context, String content, int duration) {
        if (sToast == null) {
            sToast = new Toast(context.getApplicationContext());
            LayoutInflater layoutInflater = LayoutInflater.from(context.getApplicationContext());
            LayoutToastBinding binding = LayoutToastBinding.inflate(layoutInflater);
            binding.tvContent.setText(content);
            sToast.setView(binding.getRoot());
            sToast.setGravity(Gravity.CENTER, 0, 0);
            sToast.setDuration(duration);
            sToast.show();
        } else {
            View view = sToast.getView();
            if (view instanceof TextView) {
                ((TextView) view).setText(content);
                sToast.setView(view);
                sToast.show();
            }
        }

    }
}
