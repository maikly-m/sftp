package com.example.ftp.ui.local

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.ftp.bean.FileInfo
import com.example.ftp.databinding.FragmentLocalFileBinding
import com.example.ftp.databinding.ItemFileGridItemViewBinding
import com.example.ftp.databinding.ItemFileGridViewBinding
import com.example.ftp.databinding.ItemGridListNameBinding
import com.example.ftp.databinding.ItemNoMoreViewBinding
import com.example.ftp.databinding.ItemTitleViewBinding
import com.example.ftp.room.bean.FileTrack
import com.example.ftp.ui.MainViewModel
import com.example.ftp.ui.view.GridSpacingItemDecoration
import com.example.ftp.ui.view.SpaceItemDecoration
import com.example.ftp.utils.DisplayUtils
import com.example.ftp.utils.formatTimeWithDay
import com.example.ftp.utils.sortFiles

class LocalFileFragment : Fragment() {

    private lateinit var adapter: FileAdapter
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

        binding.layoutTitle.ivBack.setOnClickListener {
            findNavController().popBackStack()
        }
        type = arguments?.getSerializable("type") as FileInfo
        binding.layoutTitle.tvName.text = type?.name?:"文件"

        initView()

        return root
    }

    private fun initView() {

        type?.let {
            mainViewModel.fileMap[it.type]?.let { data ->
                if (data.size == 0){
                    return
                }
                val recyclerView = binding.rv
                // 设置 RecyclerView 的适配器
                recyclerView.layoutManager = LinearLayoutManager(requireContext())
                // 分类数据
                sortFiles(data, 5)
                // 切割数据，按照天算
                val fileItems = mutableListOf<FileItem>()

                var day = formatTimeWithDay(data[0].mTime)
                fileItems.add(FileItem.TitleData(day))
                var fileGridItems = mutableListOf<FileTrack>()
                fileItems.add(FileItem.DataFileItem(fileGridItems))

                for (d in data){
                    val current = formatTimeWithDay(d.mTime)
                    if (current == day){
                        // add
                        fileGridItems.add(d)
                    }else{
                        // next
                        day = current
                        fileItems.add(FileItem.TitleData(day))
                        fileGridItems = mutableListOf()
                        fileItems.add(FileItem.DataFileItem(fileGridItems))
                    }
                }

                adapter = FileAdapter(fileItems)
                val spaceDecoration = SpaceItemDecoration(DisplayUtils.dp2px(requireContext(), 6f)) // 设置间距
                recyclerView.addItemDecoration(spaceDecoration)
                recyclerView.adapter = adapter
            }
        }

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

    inner class FileAdapter(val data: MutableList<FileItem>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

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
                rv.addItemDecoration(GridSpacingItemDecoration(4, DisplayUtils.dp2px(requireContext(), 10f), false))
                val fileItemAdapter = FileItemAdapter(data.fileInfo)
                rv.adapter = fileItemAdapter
            }
        }

        inner class NoMoreDataViewHolder(private val binding: ItemNoMoreViewBinding) : RecyclerView.ViewHolder(binding.root) {
            init {

            }

            fun bind(s: String) {
                binding.item = s
                binding.textView.text = s
                binding.executePendingBindings()
                // Timber.d("bind adapterPosition=${adapterPosition}, layoutPosition=${layoutPosition}")
            }
        }

        inner class TitleViewHolder(private val binding: ItemTitleViewBinding) : RecyclerView.ViewHolder(binding.root) {
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
                    val view = ItemFileGridViewBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                    FileViewHolder(view)
                }

                TYPE_NO_MORE_DATA -> {
                    val view = ItemNoMoreViewBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                    NoMoreDataViewHolder(view)
                }

                TYPE_TITLE_DATA -> {
                    val view = ItemTitleViewBinding.inflate(LayoutInflater.from(parent.context), parent, false)
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

    inner class FileItemAdapter(val items: List<FileTrack>) :
        RecyclerView.Adapter<FileItemAdapter.ViewHolder>() {
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
                ItemFileGridItemViewBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(items[position])
        }

        override fun getItemCount(): Int = items.size
    }
}


sealed class FileItem {
    data class DataFileItem(val fileInfo: List<FileTrack>) : FileItem()
    data object NoMoreData : FileItem()  // 无数据项
    data class TitleData(val data: String) : FileItem()  // 无数据项
}