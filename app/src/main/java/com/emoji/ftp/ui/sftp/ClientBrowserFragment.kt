package com.emoji.ftp.ui.sftp

import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.emoji.ftp.R
import com.emoji.ftp.bean.FileInfo
import com.emoji.ftp.databinding.FragmentClientBrowserBinding
import com.emoji.ftp.databinding.ItemGridListNameBinding
import com.emoji.ftp.provider.GetProvider
import com.emoji.ftp.ui.MainViewModel
import com.emoji.ftp.ui.setRoundedCorners
import com.emoji.ftp.ui.view.GridSpacingItemDecoration
import com.emoji.ftp.utils.DisplayUtils

class ClientBrowserFragment : Fragment() {

    private lateinit var nameFileAdapter: ListNameAdapter
    private lateinit var viewModel: ClientBrowserViewModel
    private lateinit var mainViewModel: MainViewModel
    private var _binding: FragmentClientBrowserBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewModel =
            ViewModelProvider(this).get(ClientBrowserViewModel::class.java)
        mainViewModel =
            ViewModelProvider(requireActivity()).get(MainViewModel::class.java)

        _binding = FragmentClientBrowserBinding.inflate(inflater, container, false)
        val root: View = binding.root

        binding.layoutTitle.ivBack.setOnClickListener {
            findNavController().popBackStack()
        }
        binding.layoutTitle.tvName.text = getString(R.string.text_file)


        initView()

        return root
    }

    private fun initView() {
        val rv = binding.rv
        // 设置 RecyclerView 的适配器
        rv.layoutManager = GridLayoutManager(requireContext(), 4)
        rv.addItemDecoration(GridSpacingItemDecoration(4, DisplayUtils.dp2px(requireContext(), 10f), false))
        nameFileAdapter = ListNameAdapter(mainViewModel.fileInfos)
        rv.adapter = nameFileAdapter

        binding.cv.setOnClickListener {
            findNavController().navigate(R.id.action_client_browser2client_sftp)
        }
        if (mainViewModel.listFile.value == 1){
            // 更新数据
            mainViewModel.getAllFile()
        }else{
            // 没有初始化过
            mainViewModel.listFile(Environment.getExternalStorageDirectory().absolutePath)
            mainViewModel.listFile.observe(viewLifecycleOwner){
                if (it==1) {
                    // 更新数据
                    mainViewModel.getAllFile()
                } else {
                }
            }
        }

        mainViewModel.getAllFile.observe(viewLifecycleOwner){
            if (it==1) {
                // 更新数据
                nameFileAdapter.notifyDataSetChanged()
                // 更新缩略图
                mainViewModel.updateThumbs()
            } else {

            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    inner class ListNameAdapter(val items: MutableList<FileInfo>) :
        RecyclerView.Adapter<ListNameAdapter.ViewHolder>() {
        inner class ViewHolder(private val binding: ItemGridListNameBinding) :
            RecyclerView.ViewHolder(binding.root) {

            init {


            }

            fun bind(item: FileInfo) {
                binding.executePendingBindings()

                binding.ivLocalFile.setRoundedCorners(DisplayUtils.dp2px(GetProvider.get().context, 5f).toFloat())
                binding.ivLocalFile.setImageResource(item.icon)
                binding.tvLocalFileName.text = item.name
                binding.tvLocalFileCount.text = "${item.count}${getString(R.string.text_count)}"

                binding.cv.setOnClickListener {
                    val bundle = Bundle()
                    bundle.putSerializable("type", item)
                    findNavController().navigate(R.id.action_client_browser2local_file, bundle)
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding =
                ItemGridListNameBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(items[position])
        }

        override fun getItemCount(): Int = items.size
    }
}