package com.example.ftp.ui.local

import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.MultiTransformation
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.example.ftp.R
import com.example.ftp.bean.FileInfo
import com.example.ftp.databinding.FragmentLocalFileBinding
import com.example.ftp.databinding.ItemFileGridItemViewBinding
import com.example.ftp.databinding.ItemFileGridViewBinding
import com.example.ftp.databinding.ItemListFileBinding
import com.example.ftp.databinding.ItemNoMoreViewBinding
import com.example.ftp.databinding.ItemTitleViewBinding
import com.example.ftp.provider.GetProvider
import com.example.ftp.room.bean.FileTrack
import com.example.ftp.ui.MainViewModel
import com.example.ftp.ui.toReadableFileSize
import com.example.ftp.ui.view.GridSpacingItemDecoration
import com.example.ftp.ui.view.SpaceItemDecoration
import com.example.ftp.utils.DisplayUtils
import com.example.ftp.utils.formatTimeWithDay
import com.example.ftp.utils.formatTimeWithSimpleDateFormat
import com.example.ftp.utils.getIcon4File
import com.example.ftp.utils.imageSuffixType
import com.example.ftp.utils.showCustomFileInfoDialog
import com.example.ftp.utils.sortFileTracks
import com.example.ftp.utils.videoSuffixType
import java.io.File

class LocalFileFragment : Fragment() {

    private lateinit var adapter: RecyclerView.Adapter<RecyclerView.ViewHolder>
    private lateinit var viewModel: LocalFileViewModel
    private lateinit var mainViewModel: MainViewModel
    private var type: FileInfo? = null
    private var _binding: FragmentLocalFileBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewModel =
            ViewModelProvider(this).get(LocalFileViewModel::class.java)
        mainViewModel =
            ViewModelProvider(requireActivity()).get(MainViewModel::class.java)
        _binding = FragmentLocalFileBinding.inflate(inflater, container, false)
        val root: View = binding.root

        type = arguments?.getSerializable("type") as FileInfo
        binding.layoutTitle.tvName.text = type?.name ?: "文件"

        binding.layoutTitle.ivBack.setOnClickListener {
            // 模拟返回键按下
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        // 监听返回键操作
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            if (viewModel.showMultiSelectIcon.value == true) {
                viewModel.showMultiSelectIcon.value = false
                return@addCallback
            }
            findNavController().popBackStack()
//            if (System.currentTimeMillis() - backPressedTime < doubleBackToExitInterval) {
//                // 防止快速点击
//                return@addCallback
//            }
//            backPressedTime = System.currentTimeMillis()
//            if (TextUtils.isEmpty(viewModel.getCurrentFilePath()) || TextUtils.equals(
//                    viewModel.getCurrentFilePath(),
//                    "/"
//                )
//            ) {
//
//                val uploadActive = viewModel.getUploadFileInputStreamJob()?.isActive?:false
//                val downloadActive = viewModel.getDownloadFileJob()?.isActive?:false
//                if (uploadActive && downloadActive){
//                    // 拦截任务
//                    showCustomAlertDialog(requireContext(), "提示", "正在上传和下载，是否退出", {
//                        // cancel
//                    }){
//                        // ok
//                        // 关闭任务
//                        viewModel.uploadFileInputStreamJobCancel()
//                        viewModel.downloadFileJobCancel()
//                        findNavController().popBackStack()
//                    }
//                    return@addCallback
//                }else if (uploadActive) {
//                    // 拦截任务
//                    showCustomAlertDialog(requireContext(), "提示", "正在上传，是否退出", {
//                        // cancel
//                    }){
//                        // ok
//                        // 关闭任务
//                        viewModel.uploadFileInputStreamJobCancel()
//                        findNavController().popBackStack()
//                    }
//                    return@addCallback
//                }else if (downloadActive) {
//                    // 拦截任务
//                    showCustomAlertDialog(requireContext(), "提示", "正在下载，是否退出", {
//                        // cancel
//                    }){
//                        // ok
//                        // 关闭任务
//                        viewModel.downloadFileJobCancel()
//                        findNavController().popBackStack()
//                    }
//                    return@addCallback
//                }
//                findNavController().popBackStack()
//            } else {
//                val path =
//                    viewModel.getCurrentFilePath().removeSuffix("/").substringBeforeLast("/") + "/"
//                Timber.d("onBackPressedDispatcher path=${path}")
//                viewModel.listFile(sftpClientService, path)
//            }
        }

        initView()
        initListener()

        return root
    }

    private fun initListener() {

        viewModel.showMultiSelectIcon.observe(viewLifecycleOwner) {
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
    }

    private fun initView() {
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
                    else -> {}
                }
            }
        }

    }

    private fun initZips(data: MutableList<FileTrack>) {
        val recyclerView = binding.rv
        // 设置 RecyclerView 的适配器
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        // 分类数据
        sortFileTracks(data, 5)
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
                rv.isNestedScrollingEnabled = false
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
                rv.isNestedScrollingEnabled = false
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
                    } else if (type in videoSuffixType){
                        // 视频
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
                    }else{
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
                        binding.cl.setOnClickListener {
                            // other
                            showCustomFileInfoDialog(requireContext(), "文件信息") { b ->
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