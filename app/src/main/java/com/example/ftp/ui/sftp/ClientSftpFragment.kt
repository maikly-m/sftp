package com.example.ftp.ui.sftp

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.ftp.R
import com.example.ftp.bean.ConnectInfo
import com.example.ftp.databinding.FragmentClientSftpBinding
import com.example.ftp.databinding.ItemListFileBinding
import com.example.ftp.databinding.ItemListNameBinding
import com.example.ftp.databinding.ItemSortNameBinding
import com.example.ftp.databinding.PopupWindowBottomBinding
import com.example.ftp.databinding.PopupWindowSortFileBinding
import com.example.ftp.event.ClientMessageEvent
import com.example.ftp.provider.GetProvider
import com.example.ftp.service.ClientType
import com.example.ftp.service.SftpClientService
import com.example.ftp.ui.dialog.LoadingDialog
import com.example.ftp.ui.dialog.PickFilesDialog
import com.example.ftp.ui.dialog.ProgressDialog
import com.example.ftp.ui.format
import com.example.ftp.ui.local.FileItem
import com.example.ftp.ui.toReadableFileSize
import com.example.ftp.ui.toReadableFileSizeFormat1
import com.example.ftp.utils.DisplayUtils
import com.example.ftp.utils.MySPUtil
import com.example.ftp.utils.formatTimeWithSimpleDateFormat
import com.example.ftp.utils.getIcon4File
import com.example.ftp.utils.isFileNameValid
import com.example.ftp.utils.isFolderNameValid
import com.example.ftp.utils.showCustomAlertDialog
import com.example.ftp.utils.showCustomFileInfoDialog
import com.example.ftp.utils.showCustomInputDialog
import com.example.ftp.utils.showToast
import com.example.ftp.utils.sortFiles
import com.jcraft.jsch.ChannelSftp
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import timber.log.Timber
import java.util.Vector
import kotlin.math.roundToInt

class ClientSftpFragment : Fragment() {

    private var transferAnimator: ValueAnimator? = null
    private var popupWindow: PopupWindow? = null
    private var pickFilesDialog: PickFilesDialog? = null
    private lateinit var nameFileAdapter: ListNameAdapter
    private lateinit var listFileAdapter: ListFileAdapter
    private lateinit var d: Vector<ChannelSftp.LsEntry>
    private var connectInfo: ConnectInfo? = null
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

        // back
        binding.layoutTitleFile.ivBack.setOnClickListener {
            val uploadActive = viewModel.getUploadFileInputStreamJob()?.isActive?:false
            val downloadActive = viewModel.getDownloadFileJob()?.isActive?:false
            if (uploadActive && downloadActive){
                // 拦截任务
                showCustomAlertDialog(requireContext(), "提示", "正在上传和下载，是否退出", {
                    // cancel
                }){
                    // ok
                    // 关闭任务
                    viewModel.uploadFileInputStreamJobCancel()
                    viewModel.downloadFileJobCancel()
                    findNavController().popBackStack()
                }
                return@setOnClickListener
            }else if (uploadActive) {
                // 拦截任务
                showCustomAlertDialog(requireContext(), "提示", "正在上传，是否退出", {
                    // cancel
                }){
                    // ok
                    // 关闭任务
                    viewModel.uploadFileInputStreamJobCancel()
                    findNavController().popBackStack()
                }
                return@setOnClickListener
            }else if (downloadActive) {
                // 拦截任务
                showCustomAlertDialog(requireContext(), "提示", "正在下载，是否退出", {
                    // cancel
                }){
                    // ok
                    // 关闭任务
                    viewModel.downloadFileJobCancel()
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
        binding.fab.setOnClickListener {
            if (popupWindow != null && popupWindow!!.isShowing) {
                popupWindow!!.dismiss()
            } else {
                showBottomPopupWindow(binding.root)
            }
        }

        binding.layoutTitleFile.llSort.setOnClickListener {
            // show sort
            showSortPopupWindow(it)
            binding.layoutTitleFile.ivSort.run {
                // 创建旋转动画，参数是旋转角度
                val rotationAnimator = ObjectAnimator.ofFloat(this, "rotation", this.rotation, this.rotation + 180f)
                rotationAnimator.duration = 300
                rotationAnimator.start()
            }
        }

        binding.layoutTitleBrowser.ivBack.setOnClickListener {
            // 模拟返回键按下
            requireActivity().onBackPressedDispatcher.onBackPressed()
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
            if (TextUtils.isEmpty(viewModel.getCurrentFilePath()) || TextUtils.equals(
                    viewModel.getCurrentFilePath(),
                    "/"
                )
            ) {

                val uploadActive = viewModel.getUploadFileInputStreamJob()?.isActive?:false
                val downloadActive = viewModel.getDownloadFileJob()?.isActive?:false
                if (uploadActive && downloadActive){
                    // 拦截任务
                    showCustomAlertDialog(requireContext(), "提示", "正在上传和下载，是否退出", {
                        // cancel
                    }){
                        // ok
                        // 关闭任务
                        viewModel.uploadFileInputStreamJobCancel()
                        viewModel.downloadFileJobCancel()
                        findNavController().popBackStack()
                    }
                    return@addCallback
                }else if (uploadActive) {
                    // 拦截任务
                    showCustomAlertDialog(requireContext(), "提示", "正在上传，是否退出", {
                        // cancel
                    }){
                        // ok
                        // 关闭任务
                        viewModel.uploadFileInputStreamJobCancel()
                        findNavController().popBackStack()
                    }
                    return@addCallback
                }else if (downloadActive) {
                    // 拦截任务
                    showCustomAlertDialog(requireContext(), "提示", "正在下载，是否退出", {
                        // cancel
                    }){
                        // ok
                        // 关闭任务
                        viewModel.downloadFileJobCancel()
                        findNavController().popBackStack()
                    }
                    return@addCallback
                }
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
                sortFiles(d, viewModel.changeSelectType.value)
                listFileAdapter.items.addAll(d)
                listFileAdapter.checkList.addAll(MutableList(d.size) { false })
                //binding.rv.adapter?.notifyItemRangeChanged(0, d.size-1)
                listFileAdapter.notifyDataSetChanged()


                nameFileAdapter.items.clear()
                val p = viewModel.getCurrentFilePath()
                Timber.d("p = ${p}")
                if (TextUtils.equals("/", p)) {
                    nameFileAdapter.items.add("")
                    binding.layoutTitleFile.tvName.text = "sdcard"
                } else if (p.endsWith("/")) {
                    nameFileAdapter.items.addAll(p.removeSuffix("/").split("/"))
                    binding.layoutTitleFile.tvName.text = nameFileAdapter.items[nameFileAdapter.items.size-1]
                } else {
                    nameFileAdapter.items.addAll(p.split("/"))
                    binding.layoutTitleFile.tvName.text = nameFileAdapter.items[nameFileAdapter.items.size-1]
                }
                nameFileAdapter.notifyDataSetChanged()
            } else {

            }
        }

        viewModel.listFileLoading.observe(viewLifecycleOwner) {
            // 添加view stub 拦截加载时的点击操作
            if (it == 1) {
                binding.clLoading.visibility = View.VISIBLE
            } else {
                binding.clLoading.visibility = View.GONE
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

        viewModel.downloadFile.observe(viewLifecycleOwner) {
            if (it == 1) {
                showToast("下载成功")
                // 刷新列表
            } else {
                showToast("下载失败")
            }


            binding.cvLlSimpleDown.visibility = View.GONE
            binding.cvLlSimpleDownTv.text = "传输"
            binding.tvTransferDownCount.text = "0/0"
            binding.tvTransferDownSize.text = "0/0"
            binding.tvTransferDownProgress.text = "0%"
            binding.cvClDown.visibility = View.GONE
            if (binding.cvClUp.visibility == View.GONE){
                binding.cvLlSimpleTip.visibility = View.VISIBLE
                binding.cvClTip.visibility = View.VISIBLE
            }

            viewModel.showMultiSelectIcon.value = false
        }

        viewModel.downloadFileProgress.observe(viewLifecycleOwner) {
            if (it.progress >= 0f) {
                //show
                binding.cvLlSimpleDownTv.text =  "${it.progress.format(0)}%"

                binding.tvTransferDownCount.text = "${ it.currentCount}/${it.count}"
                binding.tvTransferDownSize.text = "${it.currentFileSizes.toReadableFileSizeFormat1()}/${it.fileSizes.toReadableFileSizeFormat1()}"
                binding.tvTransferDownProgress.text = binding.cvLlSimpleDownTv.text
                binding.pbTransferDownProgress.progress = it.progress.roundToInt()

            } else if (it.progress == -1f){
                // start
                binding.cvLlSimpleDownTv.text =  "0%"
                binding.cvClDown.visibility = View.VISIBLE
                binding.cvClTip.visibility = View.GONE
                binding.pbTransferDownProgress.progress = it.progress.roundToInt()
                binding.cvLlSimpleDown.visibility = View.VISIBLE
                binding.cvLlSimpleTip.visibility = View.GONE
            }else{

            }
        }

        viewModel.deleteFile.observe(viewLifecycleOwner) {
            if (it == 1) {
                //showToast("删除成功")
            } else {
                showToast("删除失败")
            }
            // 刷新列表
            viewModel.listFile(sftpClientService, viewModel.getCurrentFilePath())

            viewModel.showMultiSelectIcon.value = false
        }

        viewModel.renameFile.observe(viewLifecycleOwner) {
            if (it == 1) {
                //showToast("重命名成功")
            } else {
                // showToast("重命名失败")
            }
            // 刷新列表
            viewModel.listFile(sftpClientService, viewModel.getCurrentFilePath())

            viewModel.showMultiSelectIcon.value = false
        }

        viewModel.mkdir.observe(viewLifecycleOwner) {
            if (it == 1) {
                // showToast("重命名成功")
            } else {
                // showToast("重命名失败")
            }
            // 刷新列表
            viewModel.listFile(sftpClientService, viewModel.getCurrentFilePath())
        }

        viewModel.showMultiSelectIcon.observe(viewLifecycleOwner) {
            if (it) {
                binding.layoutTitleFile.llRegular.visibility = View.GONE
                binding.layoutTitleFile.llSelect.visibility = View.VISIBLE

                binding.layoutBottomSelect.container.visibility = View.VISIBLE
                listFileAdapter.checkList.clear()
                listFileAdapter.checkList.addAll(MutableList(listFileAdapter.items.size) { false })
            } else {
                binding.layoutTitleFile.llRegular.visibility = View.VISIBLE
                binding.layoutTitleFile.llSelect.visibility = View.GONE

                binding.layoutBottomSelect.container.visibility = View.GONE
            }

            listFileAdapter?.notifyDataSetChanged()
        }

        viewModel.changeSelectCondition.observe(viewLifecycleOwner) {
            if (it >= 0 && listFileAdapter.checkList.size > it) {
                listFileAdapter.checkList[it] = true
                listFileAdapter?.notifyDataSetChanged()
            } else {

            }
        }


        viewModel.showSelectAll.observe(viewLifecycleOwner) {
            listFileAdapter.checkList.clear()
            if (it) {
                listFileAdapter.checkList.addAll(MutableList(listFileAdapter.items.size) { true })
            } else {
                listFileAdapter.checkList.addAll(MutableList(listFileAdapter.items.size) { false })

            }
            listFileAdapter?.notifyDataSetChanged()
        }

        viewModel.changeSelectType.observe(viewLifecycleOwner) {
            if (it < viewModel.sortTypes.size){
                binding.layoutTitleFile.tvSort.text = viewModel.sortTypes[it]
                MySPUtil.getInstance().serverSortType = it
                //show
                d = viewModel.listFileData ?: Vector<ChannelSftp.LsEntry>()
                listFileAdapter.items.clear()
                sortFiles(d, viewModel.changeSelectType.value)
                listFileAdapter.items.addAll(d)
                listFileAdapter.checkList.addAll(MutableList(d.size) { false })
                //binding.rv.adapter?.notifyItemRangeChanged(0, d.size-1)
                listFileAdapter.notifyDataSetChanged()
            }
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



        binding.layoutBottomSelect.btnDownload.setOnClickListener {
            //下载
            val files = mutableListOf<ChannelSftp.LsEntry>()
            listFileAdapter.checkList.forEachIndexed { index, b ->
                if (b) {
                    // 选定
                    files.add(listFileAdapter.items[index])
                }
            }
            viewModel.downloadFile(sftpClientService, files)
            binding.layoutBottomSelect.container.visibility = View.GONE
            viewModel.showMultiSelectIcon.value = false
        }

        binding.layoutBottomSelect.btnDelete.setOnClickListener {
            // 删除
            val files = mutableListOf<ChannelSftp.LsEntry>()
            listFileAdapter.checkList.forEachIndexed { index, b ->
                if (b) {
                    // 选定
                    files.add(listFileAdapter.items[index])
                }
            }
            if (files.size == 0){
                showToast("至少选择一个文件删除")
                return@setOnClickListener
            }
            // 确认
            showCustomAlertDialog(requireContext(), "提示", "是否删除?", {
                // cancel
                viewModel.showMultiSelectIcon.value = false
            }){
                viewModel.deleteFiles(sftpClientService, files)
            }
            binding.layoutBottomSelect.container.visibility = View.GONE
        }

        binding.layoutBottomSelect.btnRename.setOnClickListener {
            val files = mutableListOf<ChannelSftp.LsEntry>()
            listFileAdapter.checkList.forEachIndexed { index, b ->
                if (b) {
                    // 选定
                    files.add(listFileAdapter.items[index])
                }
            }
            if (files.size == 1) {
                showCustomInputDialog(requireContext(), "重命名", "请输入名字", {

                }){
                    if (TextUtils.isEmpty(it)){
                        showToast("请输入名字")
                        false
                    }else{
                        // 检验是否合规
                        if (files[0].attrs.isReg) {
                            // 文件
                            if (!isFileNameValid(it)){
                                showToast("名字非法")
                                return@showCustomInputDialog false
                            }
                            viewModel.renameFile(sftpClientService, files[0], it)
                            true
                        } else if(files[0].attrs.isDir){
                            // 文件夹
                            if (!isFolderNameValid(it)){
                                showToast("名字非法")
                                return@showCustomInputDialog false
                            }
                            viewModel.renameFile(sftpClientService, files[0], it)
                            true
                        }else{
                            true
                        }
                    }
                }

            } else if (files.size == 0){
                showToast("重命名需要选择一个文件")
            }else{
                showToast("重命名只能选择一个文件")
            }
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

    private fun showBottomPopupWindow(anchorView: View) {
        // Inflate the popup_layout.xml
        val inflater = LayoutInflater.from(requireContext())
        val popupView = PopupWindowBottomBinding.inflate(inflater, null, false)

        // Initialize the PopupWindow
        popupWindow = PopupWindow(
            popupView.root,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            true // Focusable
        )

        // Set PopupWindow background (required for dismissing on outside touch)
        popupWindow?.setBackgroundDrawable(resources.getDrawable(R.drawable.bg_popup_window_2))
        popupWindow?.isOutsideTouchable = true

        popupView.btnUpload.setOnClickListener {
            // 打开dialog选择
            pickFilesDialog = PickFilesDialog.newInstance(false)
            pickFilesDialog!!.show(requireActivity())
            popupWindow?.dismiss()
        }

        popupView.btnMkdir.setOnClickListener {
            showCustomInputDialog(requireContext(), "文件夹", "请输入名字", {

            }){
                if (TextUtils.isEmpty(it)){
                    showToast("请输入名字")
                    false
                }else{
                    // 检验是否合规
                    // 文件夹
                    if (!isFolderNameValid(it)){
                        showToast("名字非法")
                        return@showCustomInputDialog false
                    }
                    viewModel.mkdir(sftpClientService, it)
                    true
                }
            }
        }

        popupWindow?.setOnDismissListener {


        }
        // Show the PopupWindow
        popupWindow?.showAsDropDown(anchorView,
            (DisplayUtils.getScreenWidth(GetProvider.get().context) -DisplayUtils.dp2px(GetProvider.get().context, 160f)) /2,
            -DisplayUtils.dp2px(GetProvider.get().context, 80f)) // Adjust position relative to the anchor view

        // Alternatively, use showAtLocation for custom positioning
        // popupWindow.showAtLocation(anchorView, Gravity.CENTER, 0, 0)
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

    override fun onStart() {
        super.onStart()
        // todo 启动
        connectInfo = MySPUtil.getInstance().clientConnectInfo
        if (connectInfo != null) {
            startFtpClient()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        requireContext().unbindService(serviceConnection)
        EventBus.getDefault().unregister(this)
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

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            Timber.d("serviceConnection ..")
            val binder = service as SftpClientService.LocalBinder
            sftpClientService = binder.getService()
            connectInfo?.let {
                sftpClientService!!.connect(it.ip, it.port, it.name, it.pw)
            }
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
                Timber.d("SftpConnected ..")
                // list root dir
                if (event.clientType is ClientType.BaseClient){
                    viewModel.listFile(sftpClientService, "/")
                }
            }

            is ClientMessageEvent.SftpConnectFail -> {
                if (event.clientType is ClientType.BaseClient){
                    showToast(event.message)
                }
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
                    if (viewModel.showMultiSelectIcon.value == true) {
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
                    binding.tvName.setTextColor(GetProvider.get().context.getColor(R.color.color_1296db))
                } else {
                    binding.tvName.setTextColor(GetProvider.get().context.getColor(R.color.color_7F000000))
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
                binding.tvTime.text = formatTimeWithSimpleDateFormat(item.attrs.mTime * 1000L)
                if (item.attrs.isDir) {
                    binding.ivIcon.setImageResource(R.drawable.svg_dir_icon)
                    if (viewModel.showMultiSelectIcon.value == true) {
                        binding.ivSelect.visibility = View.VISIBLE
                        binding.cl.setOnClickListener {
                            checkList[adapterPosition] = !checkList[adapterPosition]
                            if (checkList[adapterPosition]) {
                                binding.ivSelect.setImageResource(R.drawable.svg_select_icon)
                            } else {
                                binding.ivSelect.setImageResource(R.drawable.svg_unselect_icon)
                            }
                        }
                        binding.cl.setOnLongClickListener {true}
                    } else {
                        binding.ivSelect.visibility = View.GONE
                        binding.cl.setOnClickListener {
                            val fullPath = viewModel.getCurrentFilePath()
                                .removeSuffix("/") + "/" + item.filename
                            viewModel.listFile(sftpClientService, fullPath)
                        }
                        binding.cl.setOnLongClickListener {
                            val p = adapterPosition
                            viewModel.showMultiSelectIcon.value = true
                            viewModel.changeSelectCondition.postValue(p)
                            true
                        }
                    }
                } else {
                    if (item.attrs.isReg) {
                        // 加上文件大小
                        binding.tvTime.text = binding.tvTime.text.toString() + "   ${item.attrs.size.toReadableFileSize()}"
                    }
                    binding.ivIcon.setImageDrawable(getIcon4File(GetProvider.get().context, item.filename))
                    if (viewModel.showMultiSelectIcon.value == true) {
                        binding.ivSelect.visibility = View.VISIBLE
                        binding.cl.setOnClickListener {
                            checkList[adapterPosition] = !checkList[adapterPosition]
                            if (checkList[adapterPosition]) {
                                binding.ivSelect.setImageResource(R.drawable.svg_select_icon)
                            } else {
                                binding.ivSelect.setImageResource(R.drawable.svg_unselect_icon)
                            }
                        }
                        binding.cl.setOnLongClickListener {true}
                    } else {
                        binding.ivSelect.visibility = View.GONE
                        binding.cl.setOnClickListener {
                            showCustomFileInfoDialog(requireContext(), "文件信息"){ b ->
                                b.ivName.setImageDrawable(getIcon4File(GetProvider.get().context, item.filename))
                                b.tvName.text = item.filename
                                val extend = item.filename.substringAfterLast('.', "").lowercase()
                                if (!TextUtils.isEmpty(extend)){
                                    b.tvType.text = extend
                                }
                                b.tvTime.text = formatTimeWithSimpleDateFormat(item.attrs.mTime * 1000L)
                                b.tvSize.text = item.attrs.size.toReadableFileSize()
                            }
                        }
                        binding.cl.setOnLongClickListener {
                            val p = adapterPosition
                            viewModel.showMultiSelectIcon.value = true
                            viewModel.changeSelectCondition.postValue(p)
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