package com.example.ftp.ui.dialog2

import android.content.Context
import android.content.DialogInterface
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.text.TextUtils
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.activity.addCallback
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.ftp.R
import com.example.ftp.databinding.DialogPickFilesBinding
import com.example.ftp.databinding.ItemListFileBinding
import com.example.ftp.databinding.ItemListNameBinding
import com.example.ftp.databinding.ItemListNameDialogBinding
import com.example.ftp.event.ClientMessageEvent
import com.example.ftp.ui.sftp.ClientSftpViewModel
import com.example.ftp.utils.formatTimeWithSimpleDateFormat
import com.example.ftp.utils.getScreenSizeHeight
import com.example.ftp.utils.getScreenSizeWidth
import com.jcraft.jsch.ChannelSftp
import org.greenrobot.eventbus.EventBus
import timber.log.Timber
import java.io.File
import java.util.Vector

open class PickFilesDialog(outCancel: Boolean) : DialogFragment() {

    private var backPressedTime: Long = 0
    private val doubleBackToExitInterval: Long = 2000 // 2秒
    private var d: MutableList<File>? = null
    private lateinit var viewModel: PickFilesViewModel
    private var showDownloadIcon = false
    private  var listFileAdapter: ListFileAdapter? = null
    private var nameFileAdapter: ListNameAdapter? = null
    private var mDimAmount = 0.5f
    private var mAnimStyle = 0
    private var mOutCancel = true
    private var mGravity = Gravity.CENTER
    private var mColorDrawable: ColorDrawable? = null
    private var onDismissListener: OnDismissListener? = null
    protected var mBinding: DialogPickFilesBinding? = null
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
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewModel =
            ViewModelProvider(this).get(PickFilesViewModel::class.java)
        mBinding = DataBindingUtil.inflate(inflater, R.layout.dialog_pick_files, container, false)
        return mBinding!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        mBinding!!.lifecycleOwner = viewLifecycleOwner
    }

    override fun onStart() {
        super.onStart()
        initParams()
        initView()
        viewModel.listFile("/")
    }

    private fun initView() {

        mBinding!!.layoutTitleBrowser.ivBack.setOnClickListener {
            // 模拟返回键按下
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        // 监听返回键操作
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner){
            if (showDownloadIcon){
                return@addCallback
            }
            if (System.currentTimeMillis() - backPressedTime < doubleBackToExitInterval) {
                // 防止快速点击
                return@addCallback
            }
            backPressedTime = System.currentTimeMillis()

            if (TextUtils.isEmpty(viewModel.getCurrentFilePath())
                || TextUtils.equals(viewModel.getCurrentFilePath(), "/")){

            }else{
                val path = viewModel.getCurrentFilePath().removeSuffix("/").substringBeforeLast("/")+"/"
                Timber.d("onBackPressedDispatcher path=${path}")
                viewModel.listFile(path)
            }
        }

        val rvName = mBinding!!.layoutTitleBrowser.rvName
        // 设置 RecyclerView 的适配器
        rvName.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        nameFileAdapter = ListNameAdapter(mutableListOf(""))
        rvName.adapter = nameFileAdapter

        val rv = mBinding!!.rv
        // 设置 RecyclerView 的适配器
        rv.layoutManager = LinearLayoutManager(requireContext())
        listFileAdapter = ListFileAdapter(mutableListOf<File>(), mutableListOf())
        rv.adapter = listFileAdapter

        mBinding!!.btnSelect.setOnClickListener {
            if (!showDownloadIcon) {
                showDownloadIcon = true
                listFileAdapter?.checkList?.clear()
                listFileAdapter?.checkList?.addAll(MutableList(listFileAdapter?.items?.size?:0){false})
            } else {
                showDownloadIcon = false
            }
            listFileAdapter?.notifyDataSetChanged()
        }

        mBinding!!.btnCancel.setOnClickListener {
            dismissAllowingStateLoss()
        }

        mBinding!!.btnOk.setOnClickListener {
            //下载
            val files = mutableListOf<File>()
            listFileAdapter?.checkList?.forEachIndexed { index, b ->
                if (b) {
                    // 选定
                    files.add(listFileAdapter!!.items[index])
                }
            }
            // 返回后上传
            EventBus.getDefault().post(ClientMessageEvent.UploadFileList("", files, viewModel.getCurrentFilePath()))
            dismissAllowingStateLoss()
        }

        viewModel.listFile.observe(viewLifecycleOwner){
            if (it==1) {
                //show
                d = viewModel.listFileData?: mutableListOf()
                listFileAdapter?.items?.clear()
                listFileAdapter?.items?.addAll(d!!)
                listFileAdapter?.checkList?.addAll(MutableList(d!!.size){false})
                //binding.rv.adapter?.notifyItemRangeChanged(0, d.size-1)
                listFileAdapter?.notifyDataSetChanged()


                nameFileAdapter?.items?.clear()
                val p = viewModel.getCurrentFilePath()
                if (TextUtils.equals("/", p)){
                    nameFileAdapter?.items?.add("")
                }else if (p.endsWith("/")) {
                    nameFileAdapter?.items?.addAll(p.removeSuffix("/").split("/"))
                }else{
                    nameFileAdapter?.items?.addAll(p.split("/"))
                }
                nameFileAdapter?.notifyDataSetChanged()
            } else {

            }
        }

        viewModel.listFileLoading.observe(viewLifecycleOwner){
            // todo
            if (it==1) {
            } else {
            }
        }
    }


    private fun initParams() {
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
            params.width = (getScreenSizeWidth(requireActivity()) * 0.9f).toInt()

            //设置dialog高度
            params.height = (getScreenSizeHeight(requireActivity()) * 0.92f).toInt()

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
        fun newInstance(outCancel: Boolean): PickFilesDialog {
            return PickFilesDialog(outCancel)
        }
    }

    inner class ListNameAdapter(val items: MutableList<String>) : RecyclerView.Adapter<ListNameAdapter.ViewHolder>() {
        inner class ViewHolder(private val binding: ItemListNameDialogBinding) : RecyclerView.ViewHolder(binding.root) {
            fun bind(item: String) {
                binding.executePendingBindings()
                binding.tvName.text = item
                binding.cl.setOnClickListener {
                    if (showDownloadIcon){
                        return@setOnClickListener
                    }
                    if (items.size - 1 > adapterPosition && adapterPosition > 0){
                        val subList = items.subList(1, adapterPosition+1)
                        val path = "/"+subList.joinToString("/")
                        viewModel.listFile(path)
                    }else{
                        viewModel.listFile("/")
                    }
                }
                if (items.first() == item){
                    // 第一项
                    binding.tvName.text = "sdcard"
                }

                if (items.last() == item) {
                    // 最后一项
                    binding.tvName.setTextColor(Color.BLUE)
                }else{
                    binding.tvName.setTextColor(Color.RED)
                }
            }
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = ItemListNameDialogBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(items[position])
        }

        override fun getItemCount(): Int = items.size
    }

    inner class ListFileAdapter(val items: MutableList<File>, val checkList: MutableList<Boolean>)
        : RecyclerView.Adapter<ListFileAdapter.ViewHolder>() {
        inner class ViewHolder(private val binding: ItemListFileBinding) : RecyclerView.ViewHolder(binding.root) {

            init {
            }

            fun bind(item: File) {
                // 强制立即更新绑定数据到视图
                binding.executePendingBindings()
                binding.tvName.text = item.name
                binding.tvTime.text = formatTimeWithSimpleDateFormat(item.lastModified())
                if (item.isDirectory) {
                    binding.ivIcon.setImageResource(R.drawable.format_folder_smartlock)
                    if (showDownloadIcon){
                        binding.ivSelect.visibility = View.VISIBLE
                        binding.cl.setOnClickListener {
                            checkList[adapterPosition] = !checkList[adapterPosition]
                            if (checkList[adapterPosition]) {
                                binding.ivSelect.setImageResource(R.drawable.abc_btn_radio_to_on_mtrl_015)
                            } else {
                                binding.ivSelect.setImageResource(R.drawable.abc_btn_radio_to_on_mtrl_000)
                            }
                        }
                    }else{
                        binding.ivSelect.visibility = View.GONE
                        binding.cl.setOnClickListener {
                            viewModel.listFile(item.absolutePath.removePrefix(Environment.getExternalStorageDirectory().absolutePath))
                        }
                    }
                } else {
                    binding.ivIcon.setImageResource(R.drawable.format_unknown)
                    if (showDownloadIcon){
                        binding.ivSelect.visibility = View.VISIBLE
                        binding.cl.setOnClickListener {
                            checkList[adapterPosition] = !checkList[adapterPosition]
                            if ( checkList[adapterPosition]) {
                                binding.ivSelect.setImageResource(R.drawable.abc_btn_radio_to_on_mtrl_015)
                            } else {
                                binding.ivSelect.setImageResource(R.drawable.abc_btn_radio_to_on_mtrl_000)
                            }
                        }
                    }else{
                        binding.ivSelect.visibility = View.GONE
                        binding.cl.setOnClickListener {
                            // other
                        }
                    }

                }

                if ( checkList[adapterPosition]) {
                    binding.ivSelect.setImageResource(R.drawable.abc_btn_radio_to_on_mtrl_015)
                } else {
                    binding.ivSelect.setImageResource(R.drawable.abc_btn_radio_to_on_mtrl_000)
                }

            }
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = ItemListFileBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(items[position])
        }

        override fun getItemCount(): Int = items.size
    }
}
