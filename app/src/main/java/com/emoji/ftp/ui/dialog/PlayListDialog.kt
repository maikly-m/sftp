package com.emoji.ftp.ui.dialog

import android.content.Context
import android.content.DialogInterface
import android.content.res.Configuration
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
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.emoji.ftp.R
import com.emoji.ftp.databinding.DialogPlayListBinding
import com.emoji.ftp.databinding.ItemListPlayNameBinding
import com.emoji.ftp.ui.player.FullPlayerViewModel
import com.emoji.ftp.ui.view.SpaceItemDecoration
import com.emoji.ftp.utils.DisplayUtils
import com.emoji.ftp.utils.getScreenSizeWidth

class PlayListDialog : DialogFragment() {
    private lateinit var mViewModel: FullPlayerViewModel
    private val mDimAmount = 0.2f
    private val mOutCancel = true
    private val mWidthPercent = 0.4f
    private val mGravity = Gravity.END or Gravity.CENTER_VERTICAL
    private val mColorDrawable: ColorDrawable? = null
    private var onDismissListener: OnDismissListener? = null
    protected var mBinding: DialogPlayListBinding? = null
    private val mForceFullScreen = true //强制全屏显示
    protected var paddingLeft: Int = 0
    protected var paddingTop: Int = 0
    protected var paddingRight: Int = 0
    protected var paddingBottom: Int = 0

    override fun onAttach(context: Context) {
        super.onAttach(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 绑定到PlayPracticeFragment中
        mViewModel =
            ViewModelProvider(requireActivity()).get(FullPlayerViewModel::class.java)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        mBinding = DataBindingUtil.inflate(inflater, R.layout.dialog_play_list, container, false)

        // 初始化列表
        mViewModel.playList?.let {
            val rv = mBinding!!.rv
            // 设置 RecyclerView 的适配器
            rv.layoutManager = LinearLayoutManager(requireContext())
            val spaceDecoration = SpaceItemDecoration(DisplayUtils.dp2px(requireContext(), 3f)) // 设置间距
            rv.addItemDecoration(spaceDecoration)
            rv.adapter = ListFileAdapter(it)
        }
        return mBinding!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        mBinding!!.lifecycleOwner = viewLifecycleOwner
        mBinding!!.rv.scrollToPosition(mViewModel.index)
    }

    override fun onStart() {
        super.onStart()
        initParams()
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
            params.width = (getScreenSizeWidth(requireActivity()) * mWidthPercent).toInt()
            //设置dialog高度
            params.height = WindowManager.LayoutParams.MATCH_PARENT

            //设置dialog动画
            window.setWindowAnimations(R.style.dialog_right_anim)
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
        fun newInstance(): PlayListDialog {
            return PlayListDialog()
        }
    }

    inner class ListFileAdapter(
        val items: List<String>
    ) : RecyclerView.Adapter<ListFileAdapter.ViewHolder>() {
        inner class ViewHolder(private val binding: ItemListPlayNameBinding) :
            RecyclerView.ViewHolder(binding.root) {

            init {
                binding.tvName.setOnClickListener {
                    // 播放
                    mViewModel.seekPos.postValue(adapterPosition)
                    notifyDataSetChanged()
                    // 关闭
                    dismissAllowingStateLoss()
                }
            }

            fun bind(item: String) {
                // 强制立即更新绑定数据到视图
                binding.executePendingBindings()
                val fileName = item.substringAfterLast("/")
                binding.tvName.text = fileName
                if (adapterPosition == mViewModel.index) {
                    // 选中
                    binding.tvName.setTextColor(requireActivity().resources.getColor(R.color.color_1296db))
                } else {
                    binding.tvName.setTextColor(requireActivity().resources.getColor(R.color.white))
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding =
                ItemListPlayNameBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(items[position])
        }

        override fun getItemCount(): Int = items.size
    }
}
