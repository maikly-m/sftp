/*
 * Copyright 2017-2023 Guilin Zhishen.
 * All Rights Reserved.
 */
package com.example.ftp.ui.dialog2;

import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

import com.airbnb.lottie.LottieAnimationView;
import com.airbnb.lottie.LottieDrawable;
import com.example.ftp.R;
import com.example.ftp.databinding.DialogLoadingProgressBinding;
import com.example.ftp.utils.DisplayUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import static com.example.ftp.utils.UtilsKt.getScreenSizeWidth;


public class ProgressDialog extends DialogFragment {
    private float mDimAmount = 0;
    private int mAnimStyle = 0;
    private boolean mOutCancel = true;
    private int mWidth;
    private float mWidthPercent = 0.25f;
    private int mGravity = Gravity.CENTER;
    private int mHeight;
    private ColorDrawable mColorDrawable;
    private OnDismissListener onDismissListener;
    protected DialogLoadingProgressBinding mBinding;
    private boolean mForceFullScreen = false; //强制全屏显示
    protected int paddingLeft = 0;
    protected int paddingTop = 0;
    protected int paddingRight = 0;
    protected int paddingBottom = 0;

    public ProgressDialog(boolean outCancel) {
        mOutCancel = outCancel;
    }

    public static ProgressDialog newInstance (boolean outCancel) {
        return new ProgressDialog(outCancel);
    }

    @Override
    public void onAttach (@NonNull Context context) {
        super.onAttach(context);
    }

    @Override
    public void onCreate (@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView (@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                              @Nullable Bundle savedInstanceState) {
        mBinding = DataBindingUtil.inflate(inflater, R.layout.dialog_loading_progress, container, false);

        return mBinding.getRoot();
    }

    @Override
    public void onViewCreated (@NonNull View view, @Nullable Bundle savedInstanceState) {
        mBinding.setLifecycleOwner(getViewLifecycleOwner());
    }

    @Override
    public void onStart () {
        super.onStart();
        initParams();
        start();
    }

    void start () {
        LottieAnimationView animationView = mBinding.lottieAnimationView;
        animationView.playAnimation();
        animationView.setRepeatCount(LottieDrawable.INFINITE);  // 无限循环
    }

    public void setProgress (String s) {
        mBinding.tvProgress.setText(s);
    }

    protected void initParams () {
        if (getDialog() == null){
            return;
        }
        Window window = getDialog().getWindow();
        mWidth = DisplayUtils.dp2px(requireContext(), 110f);
        if (window != null){
            WindowManager.LayoutParams params = window.getAttributes();
            params.dimAmount = mDimAmount;
            if (mColorDrawable != null){
                window.setBackgroundDrawable(mColorDrawable);
            } else{
                window.setBackgroundDrawable(new ColorDrawable(0x00000000));
            }
            //设置dialog宽度
            if (mWidth == 0){
                params.width = (int) (getScreenSizeWidth(requireActivity()) * mWidthPercent);
            } else{
                params.width = mWidth;
            }

            //设置dialog高度
            if (mHeight == 0){
                params.height = WindowManager.LayoutParams.WRAP_CONTENT;
            } else{
                params.height = mHeight;
            }

            //设置dialog动画
            if (mAnimStyle != 0){
                window.setWindowAnimations(mAnimStyle);
            }
            window.setGravity(mGravity);
            window.getDecorView().setPadding(paddingLeft, paddingTop, paddingRight, paddingBottom);
            window.setAttributes(params);
            if (mForceFullScreen){
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P){
                    // 延伸显示区域到刘海
                    WindowManager.LayoutParams lp = window.getAttributes();
                    lp.layoutInDisplayCutoutMode =
                            WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
                    window.setAttributes(lp);
                }
                // 全屏处理,兼容一些机型显示问题（三星note9不适配的话，会显示出状态栏）
                int option = View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN;
                window.getDecorView().setSystemUiVisibility(option);
            }
        }
        getDialog().setCanceledOnTouchOutside(mOutCancel);
        setCancelable(mOutCancel);
        setAnimStyle(R.anim.alpha_pop_in);
    }

    @Override
    public void onDismiss (@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);
        if (onDismissListener != null){
            onDismissListener.onDismiss();
        }
    }

    @Override
    public void onConfigurationChanged (@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        initParams();
    }

    protected void setWidth (int mWidth) {
        this.mWidth = mWidth;
    }

    protected void setWidthPercent (float mWidthPercent) {
        this.mWidthPercent = mWidthPercent;
    }

    protected void setHeight (int mHeight) {
        this.mHeight = mHeight;
    }

    protected void setBgColorDrawable (ColorDrawable colorDrawable) {
        mColorDrawable = colorDrawable;
    }

    protected void setOutCancel (boolean mOutCancel) {
        this.mOutCancel = mOutCancel;
    }

    protected void setForceFullScreen (boolean forceFullScreen) {
        mForceFullScreen = forceFullScreen;
    }

    protected void setAnimStyle (int mAnimStyle) {
        this.mAnimStyle = mAnimStyle;
    }

    public void setDimAmount (float mDimAmount) {
        this.mDimAmount = mDimAmount;
    }

    public void setGravity (int gravity) {
        this.mGravity = gravity;
    }

    public void setOnDismissListener (OnDismissListener onDismissListener) {
        this.onDismissListener = onDismissListener;
    }

    public void show (FragmentActivity activity) {
        show(activity.getSupportFragmentManager(), "");
    }

    public void show (FragmentManager fragmentManager) {
        show(fragmentManager, "");
    }

    public void show (Fragment fragment) {
        show(fragment.getChildFragmentManager(), "");
    }

    public interface OnDismissListener {
        void onDismiss ();
    }
}
