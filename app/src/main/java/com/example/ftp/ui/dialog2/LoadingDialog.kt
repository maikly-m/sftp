/*
 * Copyright 2017-2023 Guilin Zhishen.
 * All Rights Reserved.
 */
package com.example.ftp.ui.dialog2

import android.content.Context
import android.content.DialogInterface
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import com.airbnb.lottie.LottieDrawable
import com.example.ftp.R
import com.example.ftp.databinding.DialogLoadingBinding
import com.example.ftp.utils.getScreenSizeWidth

class LoadingDialog(outCancel: Boolean) : DialogFragment() {
    private var mDimAmount = 0f
    private var mAnimStyle = 0
    private var mOutCancel = true
    private var mWidth = 0
    private var mWidthPercent = 0.25f
    private var mGravity = Gravity.CENTER
    private var mHeight = 0
    private var mColorDrawable: ColorDrawable? = null
    private var onDismissListener: OnDismissListener? = null
    protected var mBinding: DialogLoadingBinding? = null
    private var mForceFullScreen = false //强制全屏显示
    protected var paddingLeft: Int = 0
    protected var paddingTop: Int = 0
    protected var paddingRight: Int = 0
    protected var paddingBottom: Int = 0

    init {
        mOutCancel = outCancel
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 设置 DialogFragment 使用的自定义主题
        setStyle(STYLE_NORMAL, R.style.DialogTheme)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        mBinding = DataBindingUtil.inflate(inflater, R.layout.dialog_loading, container, false)

        return mBinding!!.getRoot()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        mBinding!!.lifecycleOwner = viewLifecycleOwner
    }

    override fun onStart() {
        super.onStart()
        initParams()
        start()
    }

    override fun onResume() {
        super.onResume()
        // 设置沉浸式状态栏，确保状态栏颜色为黑色
//        dialog?.window?.let {
//            setStatusBarAndNavBar(it, Color.WHITE, true)
//        }
    }

    fun start() {
        val animationView = mBinding!!.lottieAnimationView
        animationView.playAnimation()
        animationView.repeatCount = LottieDrawable.INFINITE // 无限循环
    }

    protected fun initParams() {
        if (dialog == null) {
            return
        }
        val window = dialog!!.window
        if (window != null) {
            val params = window.attributes
            params.dimAmount = mDimAmount
            if (mColorDrawable != null) {
                window.setBackgroundDrawable(mColorDrawable)
            } else {
                window.setBackgroundDrawable(ColorDrawable(0x00000000))
            }
            //设置dialog宽度
            if (mWidth == 0) {
                params.width = (getScreenSizeWidth(requireActivity()) * mWidthPercent).toInt()
            } else {
                params.width = mWidth
            }

            //设置dialog高度
            if (mHeight == 0) {
                params.height = WindowManager.LayoutParams.WRAP_CONTENT
            } else {
                params.height = mHeight
            }

            //设置dialog动画
            if (mAnimStyle != 0) {
                window.setWindowAnimations(mAnimStyle)
            }
            window.setGravity(mGravity)
            window.decorView.setPadding(paddingLeft, paddingTop, paddingRight, paddingBottom)
            window.attributes = params
            if (mForceFullScreen) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    // 延伸显示区域到刘海
                    val lp = window.attributes
                    lp.layoutInDisplayCutoutMode =
                        WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                    window.attributes = lp
                }
                // 全屏处理,兼容一些机型显示问题（三星note9不适配的话，会显示出状态栏）
                val option = (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_FULLSCREEN)
                window.decorView.systemUiVisibility = option
            }
        }
        dialog!!.setCanceledOnTouchOutside(mOutCancel)
        isCancelable = mOutCancel
        setAnimStyle(R.anim.alpha_pop_in)
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        if (onDismissListener != null) {
            onDismissListener!!.onDismiss()
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        initParams()
    }

    protected fun setWidth(mWidth: Int) {
        this.mWidth = mWidth
    }

    protected fun setWidthPercent(mWidthPercent: Float) {
        this.mWidthPercent = mWidthPercent
    }

    protected fun setHeight(mHeight: Int) {
        this.mHeight = mHeight
    }

    protected fun setBgColorDrawable(colorDrawable: ColorDrawable?) {
        mColorDrawable = colorDrawable
    }

    protected fun setOutCancel(mOutCancel: Boolean) {
        this.mOutCancel = mOutCancel
    }

    protected fun setForceFullScreen(forceFullScreen: Boolean) {
        mForceFullScreen = forceFullScreen
    }

    protected fun setAnimStyle(mAnimStyle: Int) {
        this.mAnimStyle = mAnimStyle
    }

    fun setDimAmount(mDimAmount: Float) {
        this.mDimAmount = mDimAmount
    }

    fun setGravity(gravity: Int) {
        this.mGravity = gravity
    }

    fun setOnDismissListener(onDismissListener: OnDismissListener?) {
        this.onDismissListener = onDismissListener
    }

    fun show(activity: FragmentActivity) {
        show(activity.supportFragmentManager, "")
    }

    fun show(fragmentManager: FragmentManager) {
        show(fragmentManager, "")
    }

    fun show(fragment: Fragment) {
        show(fragment.childFragmentManager, "")
    }

    interface OnDismissListener {
        fun onDismiss()
    }

    companion object {
        fun newInstance(outCancel: Boolean): LoadingDialog {
            return LoadingDialog(outCancel)
        }
    }
}
