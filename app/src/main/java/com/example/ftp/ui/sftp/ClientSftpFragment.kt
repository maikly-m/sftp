package com.example.ftp.ui.sftp

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.ftp.R
import com.example.ftp.databinding.FragmentClientSftpBinding
import com.example.ftp.databinding.ItemListFileBinding
import com.example.ftp.databinding.ItemListNameBinding
import com.example.ftp.event.ClientMessageEvent
import com.example.ftp.service.SftpClientService
import com.example.ftp.ui.format
import com.example.ftp.utils.MySPUtil
import com.example.ftp.utils.getFileNameFromPath
import com.example.ftp.utils.getFileSize
import com.example.ftp.utils.showToast
import com.jcraft.jsch.ChannelSftp
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import timber.log.Timber
import java.io.InputStream
import java.util.Vector

class ClientSftpFragment : Fragment() {

    private var showDownloadIcon = false
    private var progressDialog: AlertDialog? =null
    private lateinit var nameFileAdapter: ListNameAdapter
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
                listFileAdapter.checkList.addAll(MutableList(d.size){false})
                //binding.rv.adapter?.notifyItemRangeChanged(0, d.size-1)
                listFileAdapter.notifyDataSetChanged()


                nameFileAdapter.items.clear()
                val p = viewModel.getCurrentFilePath()
                if (TextUtils.equals("/", p)){
                    nameFileAdapter.items.add("")
                }else if (p.endsWith("/")) {
                    nameFileAdapter.items.addAll(p.removeSuffix("/").split("/"))
                }else{
                    nameFileAdapter.items.addAll(p.split("/"))
                }
                nameFileAdapter.notifyDataSetChanged()
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
            progressDialog?.dismiss()
        }

        viewModel.uploadFileProgress.observe(viewLifecycleOwner){
            if (it > 0f) {
                //show
                if (progressDialog != null && progressDialog!!.isShowing){
                    progressDialog!!.setTitle("上传中... ${it.format(2)}"+"%")
                    return@observe
                }
                val builder = AlertDialog.Builder(requireContext())
                progressDialog = builder.setTitle("上传中...").create()
                progressDialog?.show()
            } else {

            }
        }

        viewModel.downloadFile.observe(viewLifecycleOwner){
            if (it==1) {
                showToast("下载成功")
                // 刷新列表
            } else {
                showToast("下载失败")
            }
            progressDialog?.dismiss()

            showDownloadIcon = false
            listFileAdapter.notifyDataSetChanged()
        }

        viewModel.downloadFileProgress.observe(viewLifecycleOwner){
            if (it > 0f) {
                //show
                if (progressDialog != null && progressDialog!!.isShowing){
                    progressDialog!!.setTitle("下载中... ${it.format(2)}"+"%")
                    return@observe
                }
                val builder = AlertDialog.Builder(requireContext())
                progressDialog = builder.setTitle("下载中...").create()
                progressDialog?.show()
            } else {

            }
        }
    }

    private fun initView() {
        val rvName = binding.layoutTitleBrowser.rvName
        // 设置 RecyclerView 的适配器
        rvName.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        nameFileAdapter = ListNameAdapter(mutableListOf(""))
        rvName.adapter = nameFileAdapter

        val rv = binding.rv
        // 设置 RecyclerView 的适配器
        rv.layoutManager = LinearLayoutManager(requireContext())
        listFileAdapter = ListFileAdapter(Vector<ChannelSftp.LsEntry>(), mutableListOf())
        rv.adapter = listFileAdapter

        binding.btnUpload.setOnClickListener {
            openFile()
        }

        binding.btnDownload.setOnClickListener {
            //下载
            val files = mutableListOf<ChannelSftp.LsEntry>()
            listFileAdapter.checkList.forEachIndexed { index, b ->
                if (b){
                    // 选定
                    files.add(listFileAdapter.items[index])
                }
            }
            viewModel.downloadFile(sftpClientService, files)
        }
        binding.btnSelect.setOnClickListener {
            if (!showDownloadIcon) {
                showDownloadIcon = true
                listFileAdapter.checkList.clear()
                listFileAdapter.checkList.addAll(MutableList(listFileAdapter.items.size){false})
            } else {
                showDownloadIcon = false
            }
            listFileAdapter.notifyDataSetChanged()
        }
    }

    override fun onStart() {
        super.onStart()
        serverIp = MySPUtil.getInstance().serverIp
        if (!TextUtils.isEmpty(serverIp)) {
            startFtpClient()
        }
    }

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

    private fun handleSelectedFiles(fileUris: MutableList<Uri>) {
        // check remote the same name files
        val sameNames = mutableListOf<Uri>()
        viewModel.listFileData?.forEach {
            fileUris.forEach { uri ->
                val p = getFileNameFromPath(uri)?:"${System.currentTimeMillis()}_未知文件"
                if (TextUtils.equals(it.filename, p)){
                    // the same file has been found
                    sameNames.add(uri)
                }
            }
        }

        val block = {
            val inputStreams = mutableListOf<InputStream>()
            val names = mutableListOf<String>()
            var allSize = 0L
            var hadSize = true
            fileUris.forEach { uri ->
                // 打开文件输入流
                val size = getFileSize(requireContext(), uri)
                if (size != null){
                    allSize += size
                }else{
                    hadSize = false
                }
                requireContext().contentResolver.openInputStream(uri)?.let {
                    inputStreams.add(it)
                    val p = getFileNameFromPath(uri)?:"${System.currentTimeMillis()}_未知文件"
                    val fullPath = viewModel.getCurrentFilePath().removeSuffix("/")+"/"+p
                    names.add(fullPath)
                }
            }
            if (!hadSize){
                // 只要有一个文件是拿不到大小的，就不计算文件大小
                allSize = 0
            }

            if (fileUris.size == 0){
                // none of files to be uploaded
                showToast("没有需要上传的文件")
            }else{
                fileUris.forEachIndexed { i, uri ->
                    Timber.d("uploadFileInputStream fileUris[${i}] = ${uri}")
                    Timber.d("uploadFileInputStream names[${i}] = ${names[i]}")
                }
                viewModel.uploadFileInputStream(sftpClientService, inputStreams, names, allSize, fileUris.size)
            }

        }

        if (sameNames.size > 0){
            // show dialog
            AlertDialog.Builder(requireContext())
                .setTitle("提示")
                .setMessage("有相同的文件，是否覆盖")
                .setPositiveButton("确定") { dialog, _ ->
                    block()
                    dialog.dismiss()
                }.setNegativeButton("取消") { dialog, _ ->
                    // remove the same name files
                    val b = fileUris.removeAll(sameNames)
                    Timber.d("removeAll ${b}")
                    Timber.d("fileUris.size ${fileUris.size}")
                    block()
                    dialog.dismiss()
                }
                .setCancelable(false)
                .create()
                .show()
        }else{
            block()
        }
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

    inner class ListNameAdapter(val items: MutableList<String>) : RecyclerView.Adapter<ListNameAdapter.ViewHolder>() {
        inner class ViewHolder(private val binding: ItemListNameBinding) : RecyclerView.ViewHolder(binding.root) {

            init {
            }

            fun bind(item: String) {
                binding.executePendingBindings()
                binding.tvName.text = item
                binding.cl.setOnClickListener {

                    if (items.size - 1 > adapterPosition && adapterPosition > 0){
                        val subList = items.subList(1, adapterPosition+1)
                        val path = "/"+subList.joinToString("/")
                        viewModel.listFile(sftpClientService, path)
                    }else{
                        viewModel.listFile(sftpClientService, "/")
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
            val binding = ItemListNameBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(items[position])
        }

        override fun getItemCount(): Int = items.size
    }

    inner class ListFileAdapter(val items: Vector<ChannelSftp.LsEntry>, val checkList: MutableList<Boolean>) : RecyclerView.Adapter<ListFileAdapter.ViewHolder>() {
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
                            val fullPath = viewModel.getCurrentFilePath().removeSuffix("/")+"/"+item.filename
                            viewModel.listFile(sftpClientService, fullPath)
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