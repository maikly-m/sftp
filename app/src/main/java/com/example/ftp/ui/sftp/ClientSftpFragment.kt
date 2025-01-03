package com.example.ftp.ui.sftp

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Color
import android.os.Bundle
import android.os.Environment
import android.os.IBinder
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.activity.addCallback
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
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
import com.example.ftp.ui.dialog2.LoadingDialog
import com.example.ftp.ui.dialog2.PickFilesDialog
import com.example.ftp.ui.dialog2.ProgressDialog
import com.example.ftp.ui.format
import com.example.ftp.utils.MySPUtil
import com.example.ftp.utils.isFileNameValid
import com.example.ftp.utils.isFolderNameValid
import com.example.ftp.utils.showToast
import com.jcraft.jsch.ChannelSftp
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import timber.log.Timber
import java.util.Vector

class ClientSftpFragment : Fragment() {

    private var pickFilesDialog: PickFilesDialog? = null
    private var loadingDialog: LoadingDialog? = null

    private var progressDialog: ProgressDialog? = null
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
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            if (viewModel.showDownloadIcon.value == true) {
                viewModel.showDownloadIcon.value = false
                return@addCallback
            }
            if (System.currentTimeMillis() - backPressedTime < doubleBackToExitInterval) {
                // 防止快速点击
                return@addCallback
            }
            backPressedTime = System.currentTimeMillis()
            if (TextUtils.isEmpty(viewModel.getCurrentFilePath()) || TextUtils.equals(
                    viewModel.getCurrentFilePath(),
                    "/"
                )
            ) {
                findNavController().popBackStack()
            } else {
                val path =
                    viewModel.getCurrentFilePath().removeSuffix("/").substringBeforeLast("/") + "/"
                Timber.d("onBackPressedDispatcher path=${path}")
                viewModel.listFile(sftpClientService, path)
            }
        }

        initView()
        initListener()

        return root
    }

    private fun initListener() {
        viewModel.listFile.observe(viewLifecycleOwner) {
            if (it == 1) {
                //show
                d = viewModel.listFileData ?: Vector<ChannelSftp.LsEntry>()
                listFileAdapter.items.clear()
                listFileAdapter.items.addAll(d)
                listFileAdapter.checkList.addAll(MutableList(d.size) { false })
                //binding.rv.adapter?.notifyItemRangeChanged(0, d.size-1)
                listFileAdapter.notifyDataSetChanged()


                nameFileAdapter.items.clear()
                val p = viewModel.getCurrentFilePath()
                if (TextUtils.equals("/", p)) {
                    nameFileAdapter.items.add("")
                } else if (p.endsWith("/")) {
                    nameFileAdapter.items.addAll(p.removeSuffix("/").split("/"))
                } else {
                    nameFileAdapter.items.addAll(p.split("/"))
                }
                nameFileAdapter.notifyDataSetChanged()
            } else {

            }
        }

        viewModel.listFileLoading.observe(viewLifecycleOwner) {
            if (it == 1) {
                loadingDialog = LoadingDialog.newInstance(false)
                loadingDialog!!.show(requireActivity())
            } else {
                loadingDialog?.dismissAllowingStateLoss()
            }
        }

        viewModel.uploadFileInputStream.observe(viewLifecycleOwner) {
            if (it == 1) {
                showToast("上传成功")
                // 刷新列表
                viewModel.listFile(sftpClientService, viewModel.getCurrentFilePath())
            } else {
                showToast("上传失败")
            }
            progressDialog?.dismissAllowingStateLoss()
        }

        viewModel.uploadFileProgress.observe(viewLifecycleOwner) {
            if (it > 0f) {
                //show
                if (progressDialog != null && progressDialog!!.isVisible) {
                    progressDialog!!.setProgress(it.format(2) + "%")
                    return@observe
                } else {
                    progressDialog?.dismissAllowingStateLoss()
                    progressDialog = ProgressDialog.newInstance(false)
                    progressDialog!!.show(requireActivity())
                }
            } else {
            }
        }

        viewModel.downloadFile.observe(viewLifecycleOwner) {
            if (it == 1) {
                showToast("下载成功")
                // 刷新列表
            } else {
                showToast("下载失败")
            }
            progressDialog?.dismissAllowingStateLoss()

            viewModel.showDownloadIcon.value = false
        }

        viewModel.downloadFileProgress.observe(viewLifecycleOwner) {
            if (it > 0f) {
                //show
                if (progressDialog != null && progressDialog!!.isVisible) {
                    progressDialog!!.setProgress(it.format(2) + "%")
                    return@observe
                } else {
                    progressDialog?.dismissAllowingStateLoss()
                    progressDialog = ProgressDialog.newInstance(false)
                    progressDialog!!.show(requireActivity())
                }
            } else {
            }
        }

        viewModel.deleteFile.observe(viewLifecycleOwner) {
            if (it == 1) {
                showToast("删除成功")
            } else {
                showToast("删除失败")
            }
            // 刷新列表
            viewModel.listFile(sftpClientService, viewModel.getCurrentFilePath())

            viewModel.showDownloadIcon.value = false
        }

        viewModel.renameFile.observe(viewLifecycleOwner) {
            if (it == 1) {
                showToast("重命名成功")
            } else {
                // showToast("重命名失败")
            }
            // 刷新列表
            viewModel.listFile(sftpClientService, viewModel.getCurrentFilePath())

            viewModel.showDownloadIcon.value = false
        }

        viewModel.showDownloadIcon.observe(viewLifecycleOwner) {
            if (it) {
                binding.clBottomClick.visibility = View.VISIBLE
                listFileAdapter.checkList.clear()
                listFileAdapter.checkList.addAll(MutableList(listFileAdapter.items.size) { false })
            } else {
                binding.clBottomClick.visibility = View.GONE
            }
            listFileAdapter?.notifyDataSetChanged()
        }
    }

    private fun initView() {
        val rvName = binding.layoutTitleBrowser.rvName
        // 设置 RecyclerView 的适配器
        rvName.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        nameFileAdapter = ListNameAdapter(mutableListOf(""))
        rvName.adapter = nameFileAdapter

        val rv = binding.rv
        // 设置 RecyclerView 的适配器
        rv.layoutManager = LinearLayoutManager(requireContext())
        listFileAdapter = ListFileAdapter(Vector<ChannelSftp.LsEntry>(), mutableListOf())
        rv.adapter = listFileAdapter

        binding.btnUpload.setOnClickListener {
            // 打开dialog选择
            pickFilesDialog = PickFilesDialog.newInstance(false)
            pickFilesDialog!!.show(requireActivity())
            // openFile()
        }

        binding.btnDownload.setOnClickListener {
            //下载
            val files = mutableListOf<ChannelSftp.LsEntry>()
            listFileAdapter.checkList.forEachIndexed { index, b ->
                if (b) {
                    // 选定
                    files.add(listFileAdapter.items[index])
                }
            }
            viewModel.downloadFile(sftpClientService, files)
        }

        binding.btnDel.setOnClickListener {
            //删除
            val files = mutableListOf<ChannelSftp.LsEntry>()
            listFileAdapter.checkList.forEachIndexed { index, b ->
                if (b) {
                    // 选定
                    files.add(listFileAdapter.items[index])
                }
            }
            viewModel.deleteFiles(sftpClientService, files)
        }

        binding.btnRename.setOnClickListener {
            val files = mutableListOf<ChannelSftp.LsEntry>()
            listFileAdapter.checkList.forEachIndexed { index, b ->
                if (b) {
                    // 选定
                    files.add(listFileAdapter.items[index])
                }
            }
            if (files.size == 1) {
                // show dialog
                showInputDialog(requireContext(), "请输入名字",
                    onConfirm = {
                        if (TextUtils.isEmpty(it)){
                            showToast("请输入名字")
                        }else{
                            // 检验是否合规
                            if (files[0].attrs.isReg) {
                                // 文件
                                if (!isFileNameValid(it)){
                                    showToast("名字非法")
                                    return@showInputDialog
                                }
                                viewModel.renameFile(sftpClientService, files[0], it)
                            } else if(files[0].attrs.isDir){
                                // 文件夹
                                if (!isFolderNameValid(it)){
                                    showToast("名字非法")
                                    return@showInputDialog
                                }
                                viewModel.renameFile(sftpClientService, files[0], it)
                            }
                        }
                    },
                    onCancel = {

                    }, )

            } else if (files.size == 0){
                showToast("重命名需要选择一个文件")
            }else{
                showToast("重命名只能选择一个文件")
            }
        }
    }

    fun showInputDialog(
        context: Context,
        title: String,
        onConfirm: (input: String) -> Unit,
        onCancel: () -> Unit
    ) {
        val editText = EditText(context).apply {
            hint = "请输入名字" // 设置提示文字
        }

        val dialog = AlertDialog.Builder(context)
            .setTitle(title) // 设置标题
            .setView(editText) // 添加输入框
            .setPositiveButton("确定") { _, _ ->
                val inputText = editText.text.toString()
                onConfirm(inputText) // 确定按钮回调
            }
            .setNegativeButton("取消") { _, _ ->
                onCancel() // 取消按钮回调
            }
            .setCancelable(false)
            .create()

        dialog.show()
    }

    override fun onStart() {
        super.onStart()
        serverIp = MySPUtil.getInstance().serverIp
        if (!TextUtils.isEmpty(serverIp)) {
            startFtpClient()
        }
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

            is ClientMessageEvent.SftpDisconnect -> {

            }
            is ClientMessageEvent.UploadFileList -> {
                viewModel.uploadLocalFiles(sftpClientService, event.currentPath, event.list)
            }
        }
    }

    inner class ListNameAdapter(val items: MutableList<String>) :
        RecyclerView.Adapter<ListNameAdapter.ViewHolder>() {
        inner class ViewHolder(private val binding: ItemListNameBinding) :
            RecyclerView.ViewHolder(binding.root) {

            init {
            }

            fun bind(item: String) {
                binding.executePendingBindings()
                binding.tvName.text = item
                binding.cl.setOnClickListener {
                    if (viewModel.showDownloadIcon.value == true) {
                        return@setOnClickListener
                    }
                    if (items.size - 1 > adapterPosition && adapterPosition > 0) {
                        val subList = items.subList(1, adapterPosition + 1)
                        val path = "/" + subList.joinToString("/")
                        viewModel.listFile(sftpClientService, path)
                    } else {
                        viewModel.listFile(sftpClientService, "/")
                    }
                }
                if (items.first() == item) {
                    // 第一项
                    binding.tvName.text = "sdcard"
                }

                if (items.last() == item) {
                    // 最后一项
                    binding.tvName.setTextColor(Color.BLUE)
                } else {
                    binding.tvName.setTextColor(Color.RED)
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding =
                ItemListNameBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(items[position])
        }

        override fun getItemCount(): Int = items.size
    }

    inner class ListFileAdapter(
        val items: Vector<ChannelSftp.LsEntry>,
        val checkList: MutableList<Boolean>
    ) : RecyclerView.Adapter<ListFileAdapter.ViewHolder>() {
        inner class ViewHolder(private val binding: ItemListFileBinding) :
            RecyclerView.ViewHolder(binding.root) {

            init {
            }

            fun bind(item: ChannelSftp.LsEntry) {
                // 强制立即更新绑定数据到视图
                binding.executePendingBindings()
                binding.tvName.text = item.filename
                binding.tvTime.text = item.attrs.mtimeString
                if (item.attrs.isDir) {
                    binding.ivIcon.setImageResource(R.drawable.format_folder_smartlock)
                    if (viewModel.showDownloadIcon.value == true) {
                        binding.ivSelect.visibility = View.VISIBLE
                        binding.cl.setOnClickListener {
                            checkList[adapterPosition] = !checkList[adapterPosition]
                            if (checkList[adapterPosition]) {
                                binding.ivSelect.setImageResource(R.drawable.abc_btn_radio_to_on_mtrl_015)
                            } else {
                                binding.ivSelect.setImageResource(R.drawable.abc_btn_radio_to_on_mtrl_000)
                            }
                        }
                    } else {
                        binding.ivSelect.visibility = View.GONE
                        binding.cl.setOnClickListener {
                            val fullPath = viewModel.getCurrentFilePath()
                                .removeSuffix("/") + "/" + item.filename
                            viewModel.listFile(sftpClientService, fullPath)
                        }
                        binding.cl.setOnLongClickListener {
                            viewModel.showDownloadIcon.value = true
                            true
                        }
                    }
                } else {
                    binding.ivIcon.setImageResource(R.drawable.format_unknown)
                    if (viewModel.showDownloadIcon.value == true) {
                        binding.ivSelect.visibility = View.VISIBLE
                        binding.cl.setOnClickListener {
                            checkList[adapterPosition] = !checkList[adapterPosition]
                            if (checkList[adapterPosition]) {
                                binding.ivSelect.setImageResource(R.drawable.abc_btn_radio_to_on_mtrl_015)
                            } else {
                                binding.ivSelect.setImageResource(R.drawable.abc_btn_radio_to_on_mtrl_000)
                            }
                        }
                    } else {
                        binding.ivSelect.visibility = View.GONE
                        binding.cl.setOnClickListener {
                            // other
                        }
                    }

                }

                if (checkList[adapterPosition]) {
                    binding.ivSelect.setImageResource(R.drawable.abc_btn_radio_to_on_mtrl_015)
                } else {
                    binding.ivSelect.setImageResource(R.drawable.abc_btn_radio_to_on_mtrl_000)
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