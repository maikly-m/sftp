package com.example.ftp.ui.local

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.app.AlertDialog
import android.content.ClipData.Item
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.os.IBinder
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.PopupWindow
import androidx.activity.addCallback
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.MultiTransformation
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.example.ftp.R
import com.example.ftp.bean.FileInfo
import com.example.ftp.databinding.FragmentLocalFileBinding
import com.example.ftp.databinding.ItemFileGridItemViewBinding
import com.example.ftp.databinding.ItemFileGridViewBinding
import com.example.ftp.databinding.ItemListFileBinding
import com.example.ftp.databinding.ItemNoMoreViewBinding
import com.example.ftp.databinding.ItemSortNameBinding
import com.example.ftp.databinding.ItemTitleViewBinding
import com.example.ftp.databinding.PopupWindowSortFileBinding
import com.example.ftp.event.ClientMessageEvent
import com.example.ftp.provider.GetProvider
import com.example.ftp.room.bean.FileTrack
import com.example.ftp.service.ClientType
import com.example.ftp.service.SftpClientService
import com.example.ftp.ui.MainViewModel
import com.example.ftp.ui.format
import com.example.ftp.ui.toReadableFileSize
import com.example.ftp.ui.toReadableFileSizeFormat1
import com.example.ftp.ui.view.GridSpacingItemDecoration
import com.example.ftp.ui.view.SpaceItemDecoration
import com.example.ftp.utils.DisplayUtils
import com.example.ftp.utils.createFileWithPath
import com.example.ftp.utils.formatTimeWithDay
import com.example.ftp.utils.formatTimeWithSimpleDateFormat
import com.example.ftp.utils.getIcon4File
import com.example.ftp.utils.imageSuffixType
import com.example.ftp.utils.normalizeFilePath
import com.example.ftp.utils.openFileWithSystemApp
import com.example.ftp.utils.removeFileExtension
import com.example.ftp.utils.saveBitmapToFile
import com.example.ftp.utils.saveDrawableAsJPG
import com.example.ftp.utils.showCustomAlertDialog
import com.example.ftp.utils.showCustomFileInfoDialog
import com.example.ftp.utils.showToast
import com.example.ftp.utils.sortFileTracks
import com.example.ftp.utils.videoSuffixType
import com.jcraft.jsch.ChannelSftp
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import timber.log.Timber
import java.io.File
import kotlin.math.roundToInt

class LocalFileFragment : Fragment() {

    private lateinit var adapter: RecyclerView.Adapter<RecyclerView.ViewHolder>
    private lateinit var viewModel: LocalFileViewModel
    private lateinit var mainViewModel: MainViewModel
    private var type: FileInfo? = null
    private var _binding: FragmentLocalFileBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    private var popupWindow: PopupWindow? = null
    private var backPressedTime: Long = 0
    private val doubleBackToExitInterval: Long = 2000 // 2秒

    private var transferAnimator: ValueAnimator? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        EventBus.getDefault().register(this)
        viewModel =
            ViewModelProvider(this).get(LocalFileViewModel::class.java)
        mainViewModel =
            ViewModelProvider(requireActivity()).get(MainViewModel::class.java)
        _binding = FragmentLocalFileBinding.inflate(inflater, container, false)
        val root: View = binding.root

        type = arguments?.getSerializable("type") as FileInfo
        binding.layoutTitleFile.tvName.text = type?.name ?: "文件"

        initView()
        initListener()

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.changeSelectType.postValue(1)
    }

    override fun onStart() {
        super.onStart()
        startFtpClient()
    }

    private fun initListener() {

        viewModel.showMultiSelectIcon.observe(viewLifecycleOwner) {

            if (it) {
                binding.layoutTitleFile.llRegular.visibility = View.GONE
                binding.layoutTitleFile.llSelect.visibility = View.VISIBLE

                binding.layoutBottomSelectLocal.container.visibility = View.VISIBLE
            } else {
                binding.layoutTitleFile.llRegular.visibility = View.VISIBLE
                binding.layoutTitleFile.llSelect.visibility = View.GONE

                binding.layoutBottomSelectLocal.container.visibility = View.GONE
            }

            if (it) {
                if (adapter is ListFileAdapter) {
                    (adapter as ListFileAdapter).data.forEach { d ->
                        if (d is FileItem.DataFileItem) {
                            // clear all
                            d.adapter.checkList.clear()
                            // reset
                            d.adapter.checkList.addAll(MutableList(d.adapter.items.size) { false })
                        } else if (d is FileItem.TitleData) {
                            d.mutableList.clear()
                            d.mutableList.add(0)
                        }
                    }

                } else if (adapter is GridFileAdapter) {
                    (adapter as GridFileAdapter).data.forEach { d ->
                        if (d is FileItem.DataFileItem) {
                            // clear all
                            d.adapter.checkList.clear()
                            // reset
                            d.adapter.checkList.addAll(MutableList(d.adapter.items.size) { false })
                        } else if (d is FileItem.TitleData) {
                            d.mutableList.clear()
                            d.mutableList.add(0)
                        }
                    }
                } else {

                }
            } else {
                // hide
            }
            adapter.notifyDataSetChanged()
        }
        viewModel.changeSelectCondition.observe(viewLifecycleOwner) {
            if (it >= 0) {
                if (adapter is ListFileAdapter) {
                    val data = (adapter as ListFileAdapter).data
                    if (it < data.size) {
                        // 选中数据类型
                        val selected = data[it]
                        var selectedTitle: FileItem.TitleData? = null
                        if (it - 1 >= 0 && data[it - 1] is FileItem.TitleData) {
                            selectedTitle = data[it - 1] as FileItem.TitleData
                        }
                        if (selected is FileItem.DataFileItem) {
                            // 数据类型
                            // 表示要标记title
                            var checkSize = 0
                            for (b in selected.adapter.checkList) {
                                if (b) {
                                    checkSize += 1
                                }
                            }
                            if (checkSize == 0) {
                                // 没有选择
                                selectedTitle?.let { i ->
                                    i.mutableList.clear()
                                    i.mutableList.add(0)
                                }
                            } else if (checkSize == selected.adapter.checkList.size) {
                                // 全选
                                selectedTitle?.let { i ->
                                    i.mutableList.clear()
                                    i.mutableList.add(1)
                                }
                            } else {
                                // 部分选择
                                selectedTitle?.let { i ->
                                    i.mutableList.clear()
                                    i.mutableList.add(2)
                                }
                            }
                        } else if (selected is FileItem.TitleData) {
                            // 选中title类型
                            val select = data[it] as FileItem.TitleData
                            var selectedData: FileItem.DataFileItem? = null
                            if (it + 1 < data.size && data[it + 1] is FileItem.DataFileItem) {
                                selectedData = data[it + 1] as FileItem.DataFileItem
                            }
                            selectedData?.let { s ->
                                val c = select.mutableList[0]
                                if (c == 0) {
                                    // 没有选择
                                    s.adapter.checkList.clear()
                                    s.adapter.checkList.addAll(MutableList(s.adapter.items.size) { false })
                                } else if (c == 1) {
                                    // 全选
                                    s.adapter.checkList.clear()
                                    s.adapter.checkList.addAll(MutableList(s.adapter.items.size) { true })
                                } else {
                                    // 选中部分，按照不选择处理
                                    s.adapter.checkList.clear()
                                    s.adapter.checkList.addAll(MutableList(s.adapter.items.size) { false })
                                }
                            }
                        }
                    }

                }
                adapter.notifyDataSetChanged()
            } else {

            }
        }

        viewModel.showSelectAll.observe(viewLifecycleOwner) {
            if (adapter is ListFileAdapter) {
                val data = (adapter as ListFileAdapter).data
                for (datum in data) {
                    if (datum is FileItem.DataFileItem) {
                        datum.adapter.checkList.clear()
                        if (it) {
                            datum.adapter.checkList.addAll(MutableList(datum.adapter.items.size) { true })
                        } else {
                            datum.adapter.checkList.addAll(MutableList(datum.adapter.items.size) { false })
                        }
                    } else if (datum is FileItem.TitleData) {
                        datum.mutableList.clear()
                        if (it) {
                            datum.mutableList.add(1)
                        } else {
                            datum.mutableList.add(0)
                        }
                    }
                }
            }
            adapter.notifyDataSetChanged()
        }
        viewModel.changeSelectType.observe(viewLifecycleOwner) {
            if (it < viewModel.sortTypes.size) {
                //show
                initList()
            }
        }
        viewModel.uploadFileProgress.observe(viewLifecycleOwner) {
            if (it.progress >= 0f) {
                //show
                binding.cvLlSimpleUpTv.text =  "${it.progress.format(0)}%"

                binding.tvTransferUpCount.text = "${ it.currentCount}/${it.count}"
                binding.tvTransferUpSize.text = "${it.currentFileSizes.toReadableFileSizeFormat1()}/${it.fileSizes.toReadableFileSizeFormat1()}"
                binding.tvTransferUpProgress.text = binding.cvLlSimpleUpTv.text
                binding.pbTransferUpProgress.progress = it.progress.roundToInt()

            } else if (it.progress == -1f){
                // start
                binding.cvLlSimpleUpTv.text =  "0%"
                binding.cvClUp.visibility = View.VISIBLE
                binding.cvClTip.visibility = View.GONE
                binding.pbTransferUpProgress.progress = it.progress.roundToInt()
                binding.cvLlSimpleUp.visibility = View.VISIBLE
                binding.cvLlSimpleTip.visibility = View.GONE
            }else{

            }
        }
        viewModel.uploadFileInputStream.observe(viewLifecycleOwner) {
            if (it == 1) {
                showToast("上传成功")
                // 刷新列表
            } else {
                showToast("上传失败")
            }

            binding.cvLlSimpleUp.visibility = View.GONE
            binding.cvLlSimpleUpTv.text = "传输"
            binding.tvTransferUpCount.text = "0/0"
            binding.tvTransferUpSize.text = "0/0"
            binding.tvTransferUpProgress.text = "0%"
            binding.cvClUp.visibility = View.GONE
            if (binding.cvClDown.visibility == View.GONE){
                binding.cvLlSimpleTip.visibility = View.VISIBLE
                binding.cvClTip.visibility = View.VISIBLE
            }
        }
    }

    private fun initView() {
        binding.layoutTitleFile.ivBack.setOnClickListener {
            val uploadActive = viewModel.getUploadFileInputStreamJob()?.isActive ?: false
            if (uploadActive) {
                // 拦截任务
                showCustomAlertDialog(requireContext(), "提示", "正在上传，是否退出", {
                    // cancel
                }) {
                    // ok
                    // 关闭任务
                    viewModel.uploadFileInputStreamJobCancel()
                    findNavController().popBackStack()
                }
                return@setOnClickListener
            }
            findNavController().popBackStack()
        }
        binding.layoutTitleFile.ivSelect.setOnClickListener {
            // show select-all
            viewModel.showMultiSelectIcon.value = true
            viewModel.showSelectAll.value = false
            binding.layoutTitleFile.tvSelectAll.text = "全选"
        }

        binding.layoutTitleFile.tvSelectAll.setOnClickListener {
            //select-all
            if (viewModel.showSelectAll.value == false) {
                viewModel.showSelectAll.value = true
                binding.layoutTitleFile.tvSelectAll.text = "取消全选"
            } else {
                viewModel.showSelectAll.value = false
                binding.layoutTitleFile.tvSelectAll.text = "全选"
            }
        }

        binding.layoutTitleFile.tvCancel.setOnClickListener {
            //select cancel
            viewModel.showMultiSelectIcon.value = false
        }

        // 监听返回键操作
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            if (viewModel.showMultiSelectIcon.value == true) {
                viewModel.showMultiSelectIcon.value = false
                return@addCallback
            }

            if (System.currentTimeMillis() - backPressedTime < doubleBackToExitInterval) {
                // 防止快速点击
                return@addCallback
            }
            backPressedTime = System.currentTimeMillis()
            val uploadActive = viewModel.getUploadFileInputStreamJob()?.isActive ?: false
            if (uploadActive) {
                // 拦截任务
                showCustomAlertDialog(requireContext(), "提示", "正在上传，是否退出", {
                    // cancel
                }) {
                    // ok
                    // 关闭任务
                    viewModel.uploadFileInputStreamJobCancel()
                    findNavController().popBackStack()
                }
                return@addCallback
            }
            findNavController().popBackStack()
        }

        binding.layoutTitleFile.llSort.setOnClickListener {
            // show sort
            showSortPopupWindow(it)
            binding.layoutTitleFile.ivSort.run {
                // 创建旋转动画，参数是旋转角度
                val rotationAnimator =
                    ObjectAnimator.ofFloat(this, "rotation", this.rotation, this.rotation + 180f)
                rotationAnimator.duration = 300
                rotationAnimator.start()
            }
        }

        binding.layoutBottomSelectLocal.btnUpload.setOnClickListener {
            //上传
            val files = mutableListOf<File>()
            if (adapter is ListFileAdapter) {
                (adapter as ListFileAdapter).data.forEach { d ->
                    if (d is FileItem.DataFileItem) {
                        d.adapter.checkList.forEachIndexed { index, b ->
                            if (b) {
                                if (d.adapter.items.size > index) {
                                    files.add(d.adapter.items[index])
                                }
                            }
                        }
                    }
                }

            }
            // 加上前缀
            viewModel.uploadLocalFiles(sftpClientService, "/${type?.type ?: ""}", files)
            binding.layoutBottomSelectLocal.container.visibility = View.GONE
            viewModel.showMultiSelectIcon.value = false
        }


        binding.cvTransfer.setOnClickListener {
            // todo
            if (transferAnimator?.isRunning == true){
                return@setOnClickListener
            }

            if (binding.cvLlSimple.visibility == View.VISIBLE) {
                // 动画切换
                animateWidth(binding.cvTransfer, DisplayUtils.dp2px(requireContext(), 70f),
                    DisplayUtils.dp2px(requireContext(), 240f))
                binding.cvLlSimple.visibility = View.GONE
                binding.cvLlExpand.visibility = View.VISIBLE
            } else {
                animateWidth(binding.cvTransfer, DisplayUtils.dp2px(requireContext(), 240f),
                    DisplayUtils.dp2px(requireContext(), 70f))
                binding.cvLlSimple.visibility = View.VISIBLE
                binding.cvLlExpand.visibility = View.GONE
            }
        }

    }
    private fun animateWidth(view: View, startWidth: Int, endWidth: Int) {
        transferAnimator = ValueAnimator.ofInt(startWidth, endWidth)
        transferAnimator?.duration = 300 // 动画持续时间 (毫秒)
        transferAnimator?.addUpdateListener { animation ->
            val animatedValue = animation.animatedValue as Int
            val layoutParams = view.layoutParams
            layoutParams.width = animatedValue
            view.layoutParams = layoutParams
        }
        transferAnimator?.start()
    }

    private fun initList() {
        type?.let {
            mainViewModel.fileMap[it.type]?.let { data ->
                if (data.size == 0) {
                    return
                }
                // 按照类型初始化布局
                when (it.type) {
                    "image" -> initImages(data)
                    "video" -> initVideos(data)
                    "music" -> initMusics(data)
                    "apk" -> initApks(data)
                    "text" -> initTexts(data)
                    "zip" -> initZips(data)
                    "doc" -> initZips(data)
                    "ppt" -> initZips(data)
                    "pdf" -> initZips(data)
                    "other" -> initZips(data)
                    else -> {}
                }
            }
        }
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
            binding.layoutTitleFile.ivSort.run {
                // 创建旋转动画，参数是旋转角度
                val rotationAnimator =
                    ObjectAnimator.ofFloat(this, "rotation", this.rotation, this.rotation - 180f)
                rotationAnimator.duration = 300
                rotationAnimator.start()
            }
        }
        // Show the PopupWindow
        popupWindow?.showAsDropDown(
            anchorView,
            0,
            10
        ) // Adjust position relative to the anchor view

        // Alternatively, use showAtLocation for custom positioning
        // popupWindow.showAtLocation(anchorView, Gravity.CENTER, 0, 0)
    }

    private fun initZips(data: MutableList<FileTrack>) {
        Timber.d("initZips start")
        val recyclerView = binding.rv
        // 设置 RecyclerView 的适配器
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        // 分类数据
        val value = viewModel.changeSelectType.value?.run {
            if (this == 0) {
                0
            } else {
                1
            }
        } ?: 1
        binding.layoutTitleFile.tvSort.text = viewModel.sortTypes[value]
        sortFileTracks(data, value + 4)
        // 切割数据，按照天算
        val fileItems = mutableListOf<FileItem>()

        val map = hashMapOf<String, MutableList<FileTrack>>()
        for (d in data) {
            val day = formatTimeWithDay(d.mTime)
            if (map[day] == null) {
                map[day] = mutableListOf()
            }
            map[day]?.add(d)
        }
        // 对键排序
        val sortedKeys = if (value == 0) {
            map.keys.sorted()
        } else {
            map.keys.sortedDescending()
        }
        // 按照排序后的键输出值
        sortedKeys.forEach { key ->
            map[key]?.let { u ->
                val files = mutableListOf<File>()
                val checkFiles = mutableListOf<Boolean>()
                var type = ""
                if (u.isNotEmpty()) {
                    type = u.first().type
                }
                u.forEach { p ->
                    val f = File(p.path)
                    if (f.exists() && f.isFile) {
                        files.add(f)
                        checkFiles.add(false)
                    }
                }
                if (files.size > 0) {
                    fileItems.add(FileItem.TitleData(key, fileItems.size, MutableList(1) { 0 }))
                    fileItems.add(
                        FileItem.DataFileItem(
                            u,
                            ListFileItemAdapter(type, fileItems.size, files, checkFiles)
                        )
                    )
                }
            }
        }
        map.forEach { (t, u) ->
            val files = mutableListOf<File>()
            val checkFiles = mutableListOf<Boolean>()
            var type = ""
            if (u.isNotEmpty()) {
                type = u.first().type
            }
            u.forEach { p ->
                val f = File(p.path)
                if (f.exists() && f.isFile) {
                    files.add(f)
                    checkFiles.add(false)
                }
            }
            if (files.size > 0) {
                fileItems.add(FileItem.TitleData(t, fileItems.size, MutableList(1) { 0 }))
                fileItems.add(
                    FileItem.DataFileItem(
                        u,
                        ListFileItemAdapter(type, fileItems.size, files, checkFiles)
                    )
                )
            }
        }

        adapter = ListFileAdapter(fileItems)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        // val spaceDecoration = SpaceItemDecoration(DisplayUtils.dp2px(requireContext(), 6f)) // 设置间距
        // recyclerView.addItemDecoration(spaceDecoration)
        recyclerView.adapter = adapter
        Timber.d("initZips end")
    }

    private fun initTexts(data: MutableList<FileTrack>) {
        initZips(data)
    }

    private fun initApks(data: MutableList<FileTrack>) {
        initZips(data)
    }

    private fun initMusics(data: MutableList<FileTrack>) {
        initZips(data)
    }

    private fun initVideos(data: MutableList<FileTrack>) {
        initZips(data)

    }

    private fun initImages(data: MutableList<FileTrack>) {
        initZips(data)
//        val recyclerView = binding.rv
//        // 设置 RecyclerView 的适配器
//        recyclerView.layoutManager = LinearLayoutManager(requireContext())
//        // 分类数据
//        sortFileTracks(data, 5)
//        // 切割数据，按照天算
//        val fileItems = mutableListOf<FileItem>()
//
//        val map = hashMapOf<String, MutableList<FileTrack>>()
//        for (d in data) {
//            val day = formatTimeWithDay(d.mTime)
//            if (map[day] == null) {
//                map[day] = mutableListOf()
//            }
//            map[day]?.add(d)
//        }
//        map.forEach { (t, u) ->
//            val files = mutableListOf<File>()
//            val checkFiles = mutableListOf<Boolean>()
//            u.forEach { p ->
//                val f= File(p.path)
//                if (f.exists() && f.isFile){
//                    files.add(f)
//                    checkFiles.add(false)
//                }
//            }
//            if (files.size > 0){
//                fileItems.add(FileItem.TitleData(t, fileItems.size, MutableList(1){0}))
//                fileItems.add(FileItem.DataFileItem(u, ListFileItemAdapter(fileItems.size, files, checkFiles)))
//            }
//        }
//
//        adapter = GridFileAdapter(fileItems)
//        val spaceDecoration = SpaceItemDecoration(DisplayUtils.dp2px(requireContext(), 6f)) // 设置间距
//        recyclerView.addItemDecoration(spaceDecoration)
//        recyclerView.adapter = adapter

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        requireContext().unbindService(serviceConnection)
        EventBus.getDefault().unregister(this)
    }

    companion object {
        private const val TYPE_DATA = 1
        private const val TYPE_NO_MORE_DATA = 2
        private const val TYPE_TITLE_DATA = 3
    }

    inner class ListFileAdapter(val data: MutableList<FileItem>) :
        RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        inner class FileViewHolder(private val binding: ItemFileGridViewBinding) :
            RecyclerView.ViewHolder(binding.root) {
            var item: FileItem.DataFileItem? = null

            init {

            }

            fun bind(data: FileItem.DataFileItem) {
                item = data
                val rv = binding.rv
                // 设置 RecyclerView 的适配器
                rv.layoutManager = LinearLayoutManager(requireContext())
                if (rv.itemDecorationCount == 1) {
                } else {
                    val spaceDecoration =
                        SpaceItemDecoration(DisplayUtils.dp2px(requireContext(), 6f)) // 设置间距
                    rv.addItemDecoration(spaceDecoration)
                }
                rv.adapter = data.adapter
                // 展开
                //rv.isNestedScrollingEnabled = false
            }
        }

        inner class NoMoreDataViewHolder(private val binding: ItemNoMoreViewBinding) :
            RecyclerView.ViewHolder(binding.root) {
            init {

            }

            fun bind(s: String) {
                binding.item = s
                binding.textView.text = s
                binding.executePendingBindings()
                // Timber.d("bind adapterPosition=${adapterPosition}, layoutPosition=${layoutPosition}")
            }
        }

        inner class TitleViewHolder(private val binding: ItemTitleViewBinding) :
            RecyclerView.ViewHolder(binding.root) {
            init {

            }

            fun bind(item: FileItem.TitleData) {
                binding.item = item.data
                binding.executePendingBindings()
                binding.textView.text = item.data
                if (viewModel.showMultiSelectIcon.value == true) {
                    binding.ivSelect.visibility = View.VISIBLE
                    binding.cl.setOnClickListener {
                        var c = item.mutableList[0]
                        c = if (c == 0) {
                            1
                        } else {
                            0
                        }
                        if (c == 1) {
                            binding.ivSelect.setImageResource(R.drawable.svg_select_icon)
                        } else {
                            binding.ivSelect.setImageResource(R.drawable.svg_unselect_icon)
                        }
                        item.mutableList[0] = c
                        viewModel.changeSelectCondition.postValue(item.index)
                    }
                    binding.cl.setOnLongClickListener { true }
                } else {
                    binding.cl.setOnLongClickListener {
                        viewModel.showMultiSelectIcon.value = true
                        item.mutableList[0] = 1
                        viewModel.changeSelectCondition.value = item.index
                        true
                    }
                    binding.ivSelect.visibility = View.GONE
                }
                if (item.mutableList[0] == 1) {
                    binding.ivSelect.setImageResource(R.drawable.svg_select_icon)
                } else if (item.mutableList[0] == 0) {
                    binding.ivSelect.setImageResource(R.drawable.svg_unselect_icon)
                } else {
                    binding.ivSelect.setImageResource(R.drawable.svg_select_part)
                }
            }
        }

        // 创建新的卡片视图
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            return when (viewType) {
                TYPE_DATA -> {
                    val view = ItemFileGridViewBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false
                    )
                    FileViewHolder(view)
                }

                TYPE_NO_MORE_DATA -> {
                    val view = ItemNoMoreViewBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false
                    )
                    NoMoreDataViewHolder(view)
                }

                TYPE_TITLE_DATA -> {
                    val view = ItemTitleViewBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false
                    )
                    TitleViewHolder(view)
                }

                else -> throw IllegalArgumentException("Invalid view type")
            }
        }

        // 绑定数据到卡片视图
        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            when (holder) {
                is FileViewHolder -> {
                    holder.bind(data[position] as FileItem.DataFileItem)
                }

                is TitleViewHolder -> {
                    holder.bind(data[position] as FileItem.TitleData)
                }

                is NoMoreDataViewHolder -> {
                    holder.bind("")
                }
            }
        }

        override fun getItemViewType(position: Int): Int {
            return when (data[position]) {
                is FileItem.DataFileItem -> TYPE_DATA
                is FileItem.TitleData -> TYPE_TITLE_DATA
                is FileItem.NoMoreData -> TYPE_NO_MORE_DATA
            }
        }

        override fun getItemCount(): Int {
            return data.size
        }
    }

    inner class GridFileAdapter(val data: MutableList<FileItem>) :
        RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        inner class FileViewHolder(private val binding: ItemFileGridViewBinding) :
            RecyclerView.ViewHolder(binding.root) {
            var item: FileItem.DataFileItem? = null

            init {

            }

            fun bind(data: FileItem.DataFileItem) {
                item = data
                val rv = binding.rv
                // 设置 RecyclerView 的适配器
                rv.layoutManager = GridLayoutManager(requireContext(), 4)
                if (rv.itemDecorationCount == 1) {
                } else {
                    rv.addItemDecoration(
                        GridSpacingItemDecoration(
                            4,
                            DisplayUtils.dp2px(requireContext(), 10f),
                            false
                        )
                    )
                }
                val gridFileItemAdapter = GridFileItemAdapter(data.fileInfo)
                rv.adapter = gridFileItemAdapter
                // 展开
                //rv.isNestedScrollingEnabled = false
            }
        }

        inner class NoMoreDataViewHolder(private val binding: ItemNoMoreViewBinding) :
            RecyclerView.ViewHolder(binding.root) {
            init {

            }

            fun bind(s: String) {
                binding.item = s
                binding.textView.text = s
                binding.executePendingBindings()
                // Timber.d("bind adapterPosition=${adapterPosition}, layoutPosition=${layoutPosition}")
            }
        }

        inner class TitleViewHolder(private val binding: ItemTitleViewBinding) :
            RecyclerView.ViewHolder(binding.root) {
            init {

            }

            fun bind(item: FileItem.TitleData) {
                binding.item = item.data
                binding.textView.text = item.data
                binding.executePendingBindings()
                // Timber.d("bind adapterPosition=${adapterPosition}, layoutPosition=${layoutPosition}")
            }
        }

        // 创建新的卡片视图
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            return when (viewType) {
                TYPE_DATA -> {
                    val view = ItemFileGridViewBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false
                    )
                    FileViewHolder(view)
                }

                TYPE_NO_MORE_DATA -> {
                    val view = ItemNoMoreViewBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false
                    )
                    NoMoreDataViewHolder(view)
                }

                TYPE_TITLE_DATA -> {
                    val view = ItemTitleViewBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false
                    )
                    TitleViewHolder(view)
                }

                else -> throw IllegalArgumentException("Invalid view type")
            }
        }

        // 绑定数据到卡片视图
        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            when (holder) {
                is FileViewHolder -> {
                    holder.bind(data[position] as FileItem.DataFileItem)
                }

                is TitleViewHolder -> {
                    holder.bind(data[position] as FileItem.TitleData)
                }

                is NoMoreDataViewHolder -> {
                    holder.bind("")
                }
            }
        }

        override fun getItemViewType(position: Int): Int {
            return when (data[position]) {
                is FileItem.DataFileItem -> TYPE_DATA
                is FileItem.TitleData -> TYPE_TITLE_DATA
                is FileItem.NoMoreData -> TYPE_NO_MORE_DATA
            }
        }

        override fun getItemCount(): Int {
            return data.size
        }
    }

    inner class GridFileItemAdapter(val items: List<FileTrack>) :
        RecyclerView.Adapter<GridFileItemAdapter.ViewHolder>() {
        inner class ViewHolder(private val binding: ItemFileGridItemViewBinding) :
            RecyclerView.ViewHolder(binding.root) {

            init {


            }

            fun bind(item: FileTrack) {
                binding.executePendingBindings()

                binding.tv.text = item.name
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding =
                ItemFileGridItemViewBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(items[position])
        }

        override fun getItemCount(): Int = items.size
    }

    inner class ListFileItemAdapter(
        val type: String,
        val index: Int,
        val items: MutableList<File>,
        val checkList: MutableList<Boolean>
    ) : RecyclerView.Adapter<ListFileItemAdapter.ViewHolder>() {
        inner class ViewHolder(private val binding: ItemListFileBinding) :
            RecyclerView.ViewHolder(binding.root) {

            init {
            }

            fun bind(item: File) {
                // 强制立即更新绑定数据到视图
                binding.executePendingBindings()

                binding.tvName.text = item.name
                binding.tvTime.text = formatTimeWithSimpleDateFormat(item.lastModified())
                if (item.isDirectory) {
                } else {
                    if (item.isFile) {
                        // 加上文件大小
                        binding.tvTime.text = binding.tvTime.text.toString() + "   ${
                            item.length().toReadableFileSize()
                        }"
                    }
                    if (type in imageSuffixType) {
                        // 图片
                        Glide.with(requireContext())
                            .load(item.absolutePath)
                            .transform(
                                MultiTransformation(
                                    CenterCrop(),
                                    RoundedCorners(DisplayUtils.dp2px(requireContext(), 2f))
                                )
                            )
                            .placeholder(
                                getIcon4File(
                                    GetProvider.get().context,
                                    item.name
                                )
                            )
                            .into(binding.ivIcon)
                    } else if (type in videoSuffixType) {
                        Glide.with(requireContext())
                            .asBitmap()
                            .frame(0)
                            .load(item.absolutePath)
                            .transform(
                                MultiTransformation(
                                    CenterCrop(),
                                    RoundedCorners(DisplayUtils.dp2px(requireContext(), 2f))
                                )
                            )
                            .placeholder(
                                getIcon4File(
                                    GetProvider.get().context,
                                    item.name
                                )
                            )
                            .into(binding.ivIcon)
                        // 视频
//                        var path = item.absolutePath
//                        val f = File(normalizeFilePath(mainViewModel.getSppSdcard()+removeFileExtension(item.absolutePath)+".jpg"))
//                        if (f.exists() && f.isFile) {
//                            path = f.absolutePath
//                            Glide.with(requireContext())
//                                .load(path)
//                                .transform(
//                                    MultiTransformation(
//                                        CenterCrop(),
//                                        RoundedCorners(DisplayUtils.dp2px(requireContext(), 2f))
//                                    )
//                                )
//                                .placeholder(
//                                    getIcon4File(
//                                        GetProvider.get().context,
//                                        item.name
//                                    )
//                                )
//                                .into(binding.ivIcon)
//                        }else{
//                            Glide.with(requireContext())
//                                .asBitmap()
//                                .frame(0)
//                                .load(path)
//                                .transform(
//                                    MultiTransformation(
//                                        CenterCrop(),
//                                        RoundedCorners(DisplayUtils.dp2px(requireContext(), 2f))
//                                    )
//                                )
//                                .placeholder(
//                                    getIcon4File(
//                                        GetProvider.get().context,
//                                        item.name
//                                    )
//                                )
//                                .listener(object : RequestListener<Bitmap> {
//                                    override fun onLoadFailed(
//                                        e: GlideException?,
//                                        model: Any?,
//                                        target: Target<Bitmap>?,
//                                        isFirstResource: Boolean
//                                    ): Boolean {
//                                        return false
//                                    }
//
//                                    override fun onResourceReady(
//                                        resource: Bitmap?,
//                                        model: Any?,
//                                        target: Target<Bitmap>?,
//                                        dataSource: DataSource?,
//                                        isFirstResource: Boolean
//                                    ): Boolean {
//                                        // 如果没有，就保存一下
//                                        viewModel.saveDrawableAsJPG{
//                                            resource?.run {
//                                                createFileWithPath(f.absolutePath)?.let { ff ->
//                                                    Timber.d("saveDrawableAsJPG start")
//                                                    saveBitmapToFile(resource, ff)
//                                                    Timber.d("saveDrawableAsJPG end")
//                                                }
//                                            }
//                                        }
//                                        return false
//                                    }
//                                })
//                                .into(binding.ivIcon)
//                        }


                    } else {
                        binding.ivIcon.setImageDrawable(
                            getIcon4File(
                                GetProvider.get().context,
                                item.name
                            )
                        )
                    }
                    if (viewModel.showMultiSelectIcon.value == true) {
                        binding.ivSelect.visibility = View.VISIBLE
                        binding.cl.setOnClickListener {
                            checkList[adapterPosition] = !checkList[adapterPosition]
                            if (checkList[adapterPosition]) {
                                binding.ivSelect.setImageResource(R.drawable.svg_select_icon)
                            } else {
                                binding.ivSelect.setImageResource(R.drawable.svg_unselect_icon)
                            }
                            viewModel.changeSelectCondition.postValue(index)
                        }
                        binding.cl.setOnLongClickListener { true }
                    } else {
                        binding.ivSelect.visibility = View.GONE
                        var d:AlertDialog? = null
                        binding.cl.setOnClickListener {
                            // other
                            d = showCustomFileInfoDialog(requireContext(), "文件信息") { b ->
                                b.ivName.setImageDrawable(
                                    getIcon4File(
                                        GetProvider.get().context,
                                        item.name
                                    )
                                )
                                b.tvName.text = item.name
                                val extend = item.name.substringAfterLast('.', "").lowercase()
                                if (!TextUtils.isEmpty(extend)) {
                                    b.tvType.text = extend
                                }
                                b.tvTime.text = formatTimeWithSimpleDateFormat(item.lastModified())
                                b.tvSize.text = item.length().toReadableFileSize()

                                b.llFullPath.visibility = View.VISIBLE
                                b.tvFullPath.text = item.absolutePath
                                // 可以复制
                                b.tvFullPath.setTextIsSelectable(true)

                                b.llOpen.visibility = View.VISIBLE
                                b.btnOpen.setOnClickListener {
                                    openFileWithSystemApp(GetProvider.get().context, item.absoluteFile)
                                    d?.dismiss()
                                }
                            }
                        }
                        binding.cl.setOnLongClickListener {
                            viewModel.showMultiSelectIcon.value = true
                            checkList[adapterPosition] = true
                            viewModel.changeSelectCondition.value = index
                            true
                        }
                    }

                }

                if (checkList[adapterPosition]) {
                    binding.ivSelect.setImageResource(R.drawable.svg_select_icon)
                } else {
                    binding.ivSelect.setImageResource(R.drawable.svg_unselect_icon)
                }

            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding =
                ItemListFileBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(items[position])
        }

        override fun getItemCount(): Int = items.size
    }

    inner class SortFileAdapter(val items: MutableList<String>) :
        RecyclerView.Adapter<SortFileAdapter.ViewHolder>() {
        inner class ViewHolder(private val binding: ItemSortNameBinding) :
            RecyclerView.ViewHolder(binding.root) {

            fun bind(item: String) {
                binding.executePendingBindings()
                binding.tvName.text = item
                binding.ll.setOnClickListener {
                    viewModel.changeSelectType.postValue(adapterPosition)
                    binding.tvName.setTextColor(Color.BLUE)
                    binding.ll.post {
                        notifyDataSetChanged()
                    }
                    binding.ll.postDelayed({ popupWindow?.dismiss() }, 100)
                }
                if (viewModel.changeSelectType.value == adapterPosition) {
                    binding.tvName.setTextColor(Color.BLUE)
                } else {
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

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(event: ClientMessageEvent) {
        // 处理事件
        when (event) {
            is ClientMessageEvent.SftpConnected -> {
                Timber.d("SftpConnected ..")
            }

            is ClientMessageEvent.SftpConnectFail -> {
                if (event.clientType is ClientType.BaseClient) {
                    showToast(event.message)
                }
            }

            is ClientMessageEvent.SftpDisconnect -> {

            }

            is ClientMessageEvent.UploadFileList -> {
            }
        }
    }

    private fun startFtpClient() {
        if (isBound) {
            return
        }
        // 绑定服务
        Timber.d("startFtpClient ..")
        val intent = Intent(requireContext(), SftpClientService::class.java)
        requireContext().bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    private var sftpClientService: SftpClientService? = null
    private var isBound: Boolean = false
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            Timber.d("serviceConnection ..")
            val binder = service as SftpClientService.LocalBinder
            sftpClientService = binder.getService()
            isBound = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            sftpClientService = null
            isBound = false
        }
    }
}


sealed class FileItem {
    data class DataFileItem(
        val fileInfo: List<FileTrack>,
        val adapter: LocalFileFragment.ListFileItemAdapter
    ) : FileItem()

    data object NoMoreData : FileItem()  // 无数据项
    data class TitleData(val data: String, val index: Int, val mutableList: MutableList<Int>) :
        FileItem()  // 无数据项
}