package com.example.ftp.ui.sftp

import android.app.Activity
import android.content.ComponentName
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import android.provider.MediaStore
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.ftp.R
import com.example.ftp.databinding.FragmentClientSftpBinding
import com.example.ftp.databinding.ItemListFileBinding
import com.example.ftp.event.ClientMessageEvent
import com.example.ftp.service.SftpClientService
import com.example.ftp.utils.MySPUtil
import com.example.ftp.utils.showToast
import com.jcraft.jsch.ChannelSftp
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import timber.log.Timber
import java.io.FileInputStream
import java.io.InputStream
import java.util.Vector

class ClientSftpFragment : Fragment() {

    private lateinit var listFileAdapter: ListFileAdapter
    private lateinit var d: Vector<ChannelSftp.LsEntry>
    private var serverIp: String? = null
    private var sftpClientService: SftpClientService? = null
    private var isBound: Boolean = false
    private lateinit var viewModel: ClientSftpViewModel
    private var _binding: FragmentClientSftpBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    private var backPressedTime: Long = 0
    private val doubleBackToExitInterval: Long = 2000 // 2秒

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        EventBus.getDefault().register(this)
        viewModel =
            ViewModelProvider(this).get(ClientSftpViewModel::class.java)

        _binding = FragmentClientSftpBinding.inflate(inflater, container, false)
        val root: View = binding.root

        binding.layoutTitleBrowser.ivBack.setOnClickListener {
            // 模拟返回键按下
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        // 监听返回键操作
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner){
            if (System.currentTimeMillis() - backPressedTime < doubleBackToExitInterval) {
                // 防止快速点击
                return@addCallback
            }
            backPressedTime = System.currentTimeMillis()

            if (TextUtils.isEmpty(viewModel.getCurrentFilePath()) || TextUtils.equals(viewModel.getCurrentFilePath(), "/")){
                findNavController().popBackStack()
            }else{
                val path = viewModel.getCurrentFilePath().removeSuffix("/").substringBeforeLast("/")+"/"
                Timber.d("onBackPressedDispatcher path=${path}")
                viewModel.listFile(sftpClientService, path)
            }
        }

        initView()
        initListener()

        return root
    }

    private fun initListener() {
        viewModel.listFile.observe(viewLifecycleOwner){
            if (it==1) {
                //show
                d = viewModel.listFileData?:Vector<ChannelSftp.LsEntry>()
                listFileAdapter.items.clear()
                listFileAdapter.items.addAll(d)
                //binding.rv.adapter?.notifyItemRangeChanged(0, d.size-1)
                listFileAdapter.notifyDataSetChanged()
                binding.layoutTitleBrowser.tvName.text = viewModel.getCurrentFilePath()
            } else {

            }
        }

        viewModel.uploadFileInputStream.observe(viewLifecycleOwner){
            if (it==1) {
                showToast("上传成功")
                // 刷新列表
                viewModel.listFile(sftpClientService, viewModel.getCurrentFilePath())
            } else {
                showToast("上传失败")
            }
        }
    }

    private fun initView() {

        val recyclerView = binding.rv
        // 设置 RecyclerView 的适配器
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        listFileAdapter = ListFileAdapter(Vector<ChannelSftp.LsEntry>())
        recyclerView.adapter = listFileAdapter

        binding.btnUpload.setOnClickListener {
            openFile()
        }
    }

    override fun onStart() {
        super.onStart()
        serverIp = MySPUtil.getInstance().serverIp
        if (!TextUtils.isEmpty(serverIp)) {
            startFtpClient()
        }
    }

    // 获取系统相册图片
    private val pickFileLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val fileUris = mutableListOf<Uri>()

                result.data?.let { data ->
                    if (data.clipData != null) {
                        // 多选模式
                        val count = data.clipData!!.itemCount
                        for (i in 0 until minOf(count, 4)) { // 限制最多 4 张
                            fileUris.add(data.clipData!!.getItemAt(i).uri)
                        }
                    } else if (data.data != null) {
                        // 单选模式
                        fileUris.add(data.data!!)
                    }
                }

                // 处理选中的图片
                handleSelectedFiles(fileUris)
            }
        }

    private fun handleSelectedFiles(imageUris: List<Uri>) {
        val inputStreams = mutableListOf<InputStream>()
        val names = mutableListOf<String>()
        imageUris.forEach { uri ->
            // 打开文件输入流
            requireContext().contentResolver.openInputStream(uri)?.let {
                inputStreams.add(it)
                val p = getFileNameFromPath(uri)?:"${System.currentTimeMillis()}_未知文件"
                val fullPath = viewModel.getCurrentFilePath().removeSuffix("/")+"/"+p
                names.add(fullPath)
            }
        }

        viewModel.uploadFileInputStream(sftpClientService, inputStreams, names)
    }

    fun getFileNameFromPath(uri: Uri): String? {
        return uri.path?.substringAfterLast("/")
    }

    private fun openFile() {
        // 打开系统管理器
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true) // 启用多选
        }
        pickFileLauncher.launch(Intent.createChooser(intent, "Select Files"))
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        requireContext().unbindService(serviceConnection)
        EventBus.getDefault().unregister(this)
    }

    // 启动 FTP 服务器
    private fun startFtpClient() {
        if (isBound) {
            return
        }
        // 绑定服务
        Timber.d("startFtpClient ..")
        val intent = Intent(requireContext(), SftpClientService::class.java)
        requireContext().bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            Timber.d("serviceConnection ..")
            val binder = service as SftpClientService.LocalBinder
            sftpClientService = binder.getService()
            sftpClientService!!.connect(serverIp!!, 2222, "ftpuser", "12345")
            isBound = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            sftpClientService = null
            isBound = false
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(event: ClientMessageEvent) {
        // 处理事件
        when (event) {
            is ClientMessageEvent.SftpConnected -> {
                // list root dir
                viewModel.listFile(sftpClientService, "/")
            }
            is ClientMessageEvent.SftpConnectFail -> {
               showToast(event.message)
            }
            is ClientMessageEvent.SftpDisconnect ->{

            }
        }
    }

    inner class ListFileAdapter(val items: Vector<ChannelSftp.LsEntry>) : RecyclerView.Adapter<ListFileAdapter.ViewHolder>() {
        inner class ViewHolder(private val binding: ItemListFileBinding) : RecyclerView.ViewHolder(binding.root) {

            init {
            }

            fun bind(item: ChannelSftp.LsEntry) {
                // 强制立即更新绑定数据到视图
                binding.executePendingBindings()
                binding.tvName.text = item.filename
                binding.tvTime.text = item.attrs.mtimeString
                if (item.attrs.isDir) {
                    binding.ivIcon.setImageResource(R.drawable.format_folder_smartlock)
                    binding.cl.setOnClickListener {
                        // next
                        val fullPath = viewModel.getCurrentFilePath().removeSuffix("/")+"/"+item.filename
                        viewModel.listFile(sftpClientService, fullPath)
                    }
                } else {
                    binding.ivIcon.setImageResource(R.drawable.format_unknown)
                    binding.cl.setOnClickListener {
                        // other
                    }
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