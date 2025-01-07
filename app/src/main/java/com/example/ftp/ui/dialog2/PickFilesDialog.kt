package com.example.ftp.ui.dialog2

import android.animation.ObjectAnimator
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
import android.widget.LinearLayout
import android.widget.PopupWindow
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
import com.example.ftp.databinding.ItemListNameDialogBinding
import com.example.ftp.databinding.ItemSortNameBinding
import com.example.ftp.databinding.PopupWindowSortFileBinding
import com.example.ftp.event.ClientMessageEvent
import com.example.ftp.provider.GetProvider
import com.example.ftp.ui.toReadableFileSize
import com.example.ftp.utils.MySPUtil
import com.example.ftp.utils.formatTimeWithSimpleDateFormat
import com.example.ftp.utils.getIcon4File
import com.example.ftp.utils.getScreenSizeHeight
import com.example.ftp.utils.getScreenSizeWidth
import org.greenrobot.eventbus.EventBus
import timber.log.Timber
import java.io.File

open class PickFilesDialog(outCancel: Boolean) : DialogFragment() {

    private var backPressedTime: Long = 0
    private val doubleBackToExitInterval: Long = 2000 // 2秒
    private var d: MutableList<File>? = null
    private lateinit var viewModel: PickFilesViewModel
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

    private var popupWindow: PopupWindow? = null

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
            if (viewModel.showMultiSelectIcon.value == true) {
                viewModel.showMultiSelectIcon.value = false
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


        mBinding!!.layoutTitleFileDialog.ivSelect.setOnClickListener {
            // show select-all
            viewModel.showMultiSelectIcon.value = true
        }

        mBinding!!.layoutTitleFileDialog.tvSelectAll.setOnClickListener {
            //select-all
            viewModel.showSelectAll.value = true
        }

        mBinding!!.layoutTitleFileDialog.tvCancel.setOnClickListener {
            //select cancel
            viewModel.showMultiSelectIcon.value = false
        }

        mBinding!!.layoutTitleFileDialog.llSort.setOnClickListener {
            // show sort
            showSortPopupWindow(it)
            mBinding!!.layoutTitleFileDialog.ivSort.run {
                // 创建旋转动画，参数是旋转角度
                val rotationAnimator = ObjectAnimator.ofFloat(this, "rotation", this.rotation, this.rotation + 180f)
                rotationAnimator.duration = 300
                rotationAnimator.start()
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
            if (files.size > 0) {
                EventBus.getDefault().post(ClientMessageEvent.UploadFileList("", files, viewModel.getCurrentFilePath()))
            } else {

            }
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

        viewModel.showMultiSelectIcon.observe(viewLifecycleOwner) {
            if (it) {
                mBinding!!.layoutTitleFileDialog.llRegular.visibility = View.GONE
                mBinding!!.layoutTitleFileDialog.llSelect.visibility = View.VISIBLE

                listFileAdapter!!.checkList.clear()
                listFileAdapter!!.checkList.addAll(MutableList(listFileAdapter!!.items.size) { false })
            } else {
                mBinding!!.layoutTitleFileDialog.llRegular.visibility = View.VISIBLE
                mBinding!!.layoutTitleFileDialog.llSelect.visibility = View.GONE
            }
            listFileAdapter?.notifyDataSetChanged()
        }


        viewModel.showSelectAll.observe(viewLifecycleOwner) {
            listFileAdapter!!.checkList.clear()
            if (it) {
                listFileAdapter!!.checkList.addAll(MutableList(listFileAdapter!!.items.size) { true })
            } else {
                listFileAdapter!!.checkList.addAll(MutableList(listFileAdapter!!.items.size) { false })

            }
            listFileAdapter?.notifyDataSetChanged()
        }

        viewModel.changeSelectType.observe(viewLifecycleOwner) {
            if (it < viewModel.sortTypes.size){
                mBinding!!.layoutTitleFileDialog.tvSort.text = viewModel.sortTypes[it]
                MySPUtil.getInstance().clientSortType = it
                //show
                d = viewModel.listFileData ?: mutableListOf()
                listFileAdapter!!.items.clear()
                sortFiles()
                listFileAdapter!!.items.addAll(d!!)
                listFileAdapter!!.checkList.addAll(MutableList(d!!.size) { false })
                //binding.rv.adapter?.notifyItemRangeChanged(0, d.size-1)
                listFileAdapter!!.notifyDataSetChanged()
            }
        }
    }

    private fun sortFiles() {
        //排序
        //        "按名称",
        //        "按类型",
        //        "按大小升序",
        //        "按大小降序",
        //        "按时间升序",
        //        "按时间降序",
        when (viewModel.changeSelectType.value) {
            0 -> {
                d?.sortBy { data ->
                    data.name
                }
            }

            1 -> {
                d?.sortBy { data ->
                    val extension = data.name.substringAfterLast('.', "")
                    if (TextUtils.isEmpty(extension)) {
                        data.name
                    } else {
                        extension
                    }
                }
            }

            2 -> {
                d?.sortBy { data ->
                    data.length()
                }
            }

            3 -> {
                d?.sortByDescending { data ->
                    data.length()
                }
            }

            4 -> {
                d?.sortBy { data ->
                    data.lastModified()
                }
            }

            5 -> {
                d?.sortByDescending { data ->
                    data.lastModified()
                }
            }

            else -> {
                d?.sortBy { data ->
                    data.name
                }
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
                    if (viewModel.showMultiSelectIcon.value == true){
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
                    binding.tvName.setTextColor(GetProvider.get().context.getColor(R.color.color_1296db))
                } else {
                    binding.tvName.setTextColor(GetProvider.get().context.getColor(R.color.color_7F000000))
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
                    binding.ivIcon.setImageResource(R.drawable.svg_dir_icon)
                    if (viewModel.showMultiSelectIcon.value == true){
                        binding.ivSelect.visibility = View.VISIBLE
                        binding.cl.setOnClickListener {
                            checkList[adapterPosition] = !checkList[adapterPosition]
                            if (checkList[adapterPosition]) {
                                binding.ivSelect.setImageResource(R.drawable.svg_select_icon)
                            } else {
                                binding.ivSelect.setImageResource(R.drawable.svg_unselect_icon)
                            }
                        }
                    }else{
                        binding.ivSelect.visibility = View.GONE
                        binding.cl.setOnClickListener {
                            viewModel.listFile(item.absolutePath.removePrefix(Environment.getExternalStorageDirectory().absolutePath))
                        }
                        binding.cl.setOnLongClickListener {
                            viewModel.showMultiSelectIcon.value = true
                            true
                        }
                    }
                } else {
                    if (item.isFile) {
                        // 加上文件大小
                        binding.tvTime.text = binding.tvTime.text.toString() + "   ${item.length().toReadableFileSize()}"
                    }
                    binding.ivIcon.setImageDrawable(getIcon4File(GetProvider.get().context, item.name))
                    if (viewModel.showMultiSelectIcon.value == true){
                        binding.ivSelect.visibility = View.VISIBLE
                        binding.cl.setOnClickListener {
                            checkList[adapterPosition] = !checkList[adapterPosition]
                            if ( checkList[adapterPosition]) {
                                binding.ivSelect.setImageResource(R.drawable.svg_select_icon)
                            } else {
                                binding.ivSelect.setImageResource(R.drawable.svg_unselect_icon)
                            }
                        }
                    }else{
                        binding.ivSelect.visibility = View.GONE
                        binding.cl.setOnClickListener {
                            // other
                        }
                        binding.cl.setOnLongClickListener {
                            viewModel.showMultiSelectIcon.value = true
                            true
                        }
                    }

                }

                if ( checkList[adapterPosition]) {
                    binding.ivSelect.setImageResource(R.drawable.svg_select_icon)
                } else {
                    binding.ivSelect.setImageResource(R.drawable.svg_unselect_icon)
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
    private fun showSortPopupWindow(anchorView: View) {
        // Inflate the popup_layout.xml
        val inflater = LayoutInflater.from(requireContext())
        val popupView = PopupWindowSortFileBinding.inflate(inflater, null, false)

        // Initialize the PopupWindow
        popupWindow = PopupWindow(
            popupView.root,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            true // Focusable
        )

        // Set PopupWindow background (required for dismissing on outside touch)
        popupWindow?.setBackgroundDrawable(resources.getDrawable(R.drawable.bg_popup_window))
        popupWindow?.isOutsideTouchable = true


        val recyclerView = popupView.rv
        // 设置 RecyclerView 的适配器
        recyclerView.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)

        val adapter = SortFileAdapter(viewModel.sortTypes)
        recyclerView.adapter = adapter

        popupWindow?.setOnDismissListener {
            mBinding?.layoutTitleFileDialog?.ivSort?.run {
                // 创建旋转动画，参数是旋转角度
                val rotationAnimator = ObjectAnimator.ofFloat(this, "rotation", this.rotation, this.rotation - 180f)
                rotationAnimator.duration = 300
                rotationAnimator.start()
            }
        }
        // Show the PopupWindow
        popupWindow?.showAsDropDown(anchorView, 0, 10) // Adjust position relative to the anchor view

        // Alternatively, use showAtLocation for custom positioning
        // popupWindow.showAtLocation(anchorView, Gravity.CENTER, 0, 0)
    }
    inner class SortFileAdapter(val items: MutableList<String>) :
        RecyclerView.Adapter<SortFileAdapter.ViewHolder>() {
        inner class ViewHolder(private val binding: ItemSortNameBinding) :
            RecyclerView.ViewHolder(binding.root) {

            fun bind(item: String) {
                binding.executePendingBindings()
                binding.tvName.text = item
                binding.ll.setOnClickListener {
                    viewModel.changeSelectType.value = adapterPosition
                    binding.tvName.setTextColor(Color.BLUE)
                    binding.ll.post {
                        notifyDataSetChanged()
                    }
                    binding.ll.postDelayed({popupWindow?.dismiss()},100)
                }
                if (viewModel.changeSelectType.value == adapterPosition) {
                    binding.tvName.setTextColor(Color.BLUE)
                }else{
                    binding.tvName.setTextColor(Color.BLACK)
                }

            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding =
                ItemSortNameBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(items[position])
        }

        override fun getItemCount(): Int = items.size
    }
}
