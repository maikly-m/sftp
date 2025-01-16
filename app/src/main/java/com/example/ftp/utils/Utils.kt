package com.example.ftp.utils

import android.Manifest
import android.app.AlertDialog
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo.WindowLayout
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Point
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.graphics.drawable.InsetDrawable
import android.media.MediaMetadataRetriever
import android.media.MediaScannerConnection
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.text.TextUtils
import android.util.Size
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.webkit.MimeTypeMap
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.content.FileProvider
import androidx.fragment.app.FragmentActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.load.MultiTransformation
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.FutureTarget
import com.bumptech.glide.request.RequestOptions
import com.example.ftp.R
import com.example.ftp.databinding.DialogCustomAlertBinding
import com.example.ftp.databinding.DialogCustomFileInfoBinding
import com.example.ftp.databinding.DialogCustomInputBinding
import com.example.ftp.databinding.DialogCustomPlayerBinding
import com.example.ftp.provider.GetProvider
import com.example.ftp.room.bean.FileTrack
import com.example.ftp.utils.thread.AppExecutors
import com.google.zxing.BarcodeFormat
import com.google.zxing.WriterException
import com.google.zxing.qrcode.QRCodeWriter
import com.jcraft.jsch.ChannelSftp
import com.permissionx.guolindev.PermissionX
import id.zelory.compressor.Compressor
import id.zelory.compressor.constraint.format
import id.zelory.compressor.constraint.quality
import id.zelory.compressor.constraint.resolution
import id.zelory.compressor.constraint.size
import timber.log.Timber
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Vector

/**
 * 全屏显示，状态栏及导航栏均隐藏,均不占位
 *
 * <p>View.SYSTEM_UI_FLAG_LAYOUT_STABLE：全屏显示时保证尺寸不变。
 * View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN：Activity全屏显示，状态栏显示在Activity页面上面。
 * View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION：效果同View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
 * View.SYSTEM_UI_FLAG_HIDE_NAVIGATION：隐藏导航栏
 * View.SYSTEM_UI_FLAG_FULLSCREEN：Activity全屏显示，且状态栏被隐藏覆盖掉。
 * View.SYSTEM_UI_FLAG_VISIBLE：Activity非全屏显示，显示状态栏和导航栏。
 * View.INVISIBLE：Activity伸展全屏显示，隐藏状态栏。
 * View.SYSTEM_UI_LAYOUT_FLAGS：效果同View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
 * View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY：必须配合View.SYSTEM_UI_FLAG_FULLSCREEN和View
 * .SYSTEM_UI_FLAG_HIDE_NAVIGATION组合使用，达到的效果是拉出状态栏和导航栏后显示一会儿消失。
 *
 * @param window
 */
fun setFullScreen(window: Window?): Pair<Int, Int>? {
    if (window == null) return null
    val attributes = window.attributes
    var oldCutoutMode = 0
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        oldCutoutMode = attributes.layoutInDisplayCutoutMode
        attributes.layoutInDisplayCutoutMode =
            WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        window.attributes = attributes
    }
    val oldOption = window.decorView.systemUiVisibility
    val option = (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_FULLSCREEN)
    window.decorView.systemUiVisibility = option
    window.decorView.setOnSystemUiVisibilityChangeListener { visibility: Int ->
        if ((visibility and View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
            window.decorView.systemUiVisibility = option
        }
    }
    return Pair(oldCutoutMode, oldOption)
}

fun recover(window: Window?, config: Pair<Int?, Int>) {
    if (window == null) return
    val attributes = window.attributes
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        attributes.layoutInDisplayCutoutMode = config.first!!
        window.attributes = attributes
    }
    val option = config.second
    window.decorView.systemUiVisibility = option
    window.decorView.setOnSystemUiVisibilityChangeListener { visibility: Int -> }
}

fun showStatusBarAndNavBarFull(window: Window) {
    // 不隐藏状态栏和导航栏，状态栏在页面之上,页面是全屏显示，导航栏浮在页面上
    val option = (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION)

    window.decorView.systemUiVisibility = option
    window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
    window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION)
    window.statusBarColor = Color.TRANSPARENT
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        // 清除背景
        window.isNavigationBarContrastEnforced = false
    }
    window.navigationBarColor = Color.TRANSPARENT
}

fun showStatusBarAndNavBar(window: Window, color: Int) {
    // 不隐藏状态栏和导航栏，状态栏在页面之上,还需要设置相应的背景颜色值
    val option = (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)

    window.decorView.systemUiVisibility = option
    window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
    window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION)
    window.statusBarColor = Color.TRANSPARENT
    window.navigationBarColor = color
}

fun setStatusBarAndNavBar(window: Window, color: Int?, isBlack: Boolean?) {
    // 不隐藏状态栏和导航栏，状态栏在页面之上,还需要设置相应的背景颜色值
    var option = (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)

    if (isBlack != null) {
        if (isBlack) {
            option = option or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        } else {
            // nothing
        }
    }
    window.decorView.systemUiVisibility = option
    window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
    window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION)
    window.statusBarColor = Color.TRANSPARENT
    if (color != null) {
        window.navigationBarColor = color
    }
}

// Android 10（API 级别 29）以下使用
fun copyImageToGallery(context: Context, imageFileName: String) {
    // 获取应用内的图片路径
    val appImageFile = File(context.filesDir, "images/$imageFileName")

    // 确保图片文件存在
    if (!appImageFile.exists()) {
        Timber.d("Image file does not exist")
        return
    }

    // 获取公共Pictures目录路径
    val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
    val galleryFile = File(picturesDir, imageFileName)

    try {
        // 复制图片文件
        copyFile(appImageFile, galleryFile)

        // 更新相册（使用MediaScannerConnection）
        MediaScannerConnection.scanFile(
            context,
            arrayOf(galleryFile.absolutePath),
            null,
            null
        )

        Timber.d("Image copied to gallery successfully.")
    } catch (e: IOException) {
        e.printStackTrace()
        Timber.d("Error copying image: ${e.message}")
    }
}

fun copyFile(inputFile: File, outputFile: File) {
    var inputStream: InputStream? = null
    var outputStream: OutputStream? = null
    try {
        inputStream = FileInputStream(inputFile)
        outputStream = FileOutputStream(outputFile)

        val buffer = ByteArray(1024)
        var length: Int
        while (inputStream.read(buffer).also { length = it } > 0) {
            outputStream.write(buffer, 0, length)
        }
        outputStream.flush()
    } finally {
        inputStream?.close()
        outputStream?.close()
    }
}


// Android 10（API 级别 29）以上使用
fun saveImageToGallery(context: Context, imageFileName: String) {
    val contentValues = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, imageFileName)
        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/MyApp")  // 设置图片保存路径
    }

    val contentResolver: ContentResolver = context.contentResolver
    val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

    uri?.let { imageUri ->
        try {
            val inputStream = FileInputStream(File(context.filesDir, "images/$imageFileName"))
            val outputStream = contentResolver.openOutputStream(imageUri)

            inputStream.copyTo(outputStream!!)
            inputStream.close()
            outputStream.close()

            Timber.d("Image copied to gallery successfully.")
        } catch (e: Exception) {
            e.printStackTrace()
            Timber.d("Error copying image: ${e.message}")
        }
    }
}


fun cropToSquareCenter(imagePath: String): Bitmap? {
    // 1. 加载原始图片
    val originalBitmap = BitmapFactory.decodeFile(imagePath) ?: return null

    // 2. 计算裁切区域
    val width = originalBitmap.width
    val height = originalBitmap.height
    val size = Math.min(width, height) // 确定裁切区域的大小

    val left = (width - size) / 2
    val top = (height - size) / 2
    val right = left + size
    val bottom = top + size

    // 3. 创建一个新的 Bitmap，作为裁切后的结果
    val croppedBitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(croppedBitmap)

    // 4. 裁切并绘制到新的 Bitmap 上
    val rectSrc = Rect(left, top, right, bottom)
    val rectDst = Rect(0, 0, size, size)
    canvas.drawBitmap(originalBitmap, rectSrc, rectDst, Paint())

    // 5. 返回裁切后的 Bitmap
    return croppedBitmap
}


fun saveBitmapToFile(bitmap: Bitmap, filePath: String, quality: Int): Boolean {
    val file = File(filePath)
    return try {
        val outputStream = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
        outputStream.flush()
        outputStream.close()
        true
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}



fun compressImage(imagePath: String, outputPath: String, quality: Int): Boolean {
    // 1. 加载图片为 Bitmap
    val originalBitmap = BitmapFactory.decodeFile(imagePath)

    // 2. 创建压缩文件输出流
    val outputFile = File(outputPath)
    val fileOutputStream = FileOutputStream(outputFile)

    // 3. 使用 JPEG 格式进行压缩
    val isCompressed = originalBitmap.compress(Bitmap.CompressFormat.JPEG, quality, fileOutputStream)

    // 4. 关闭输出流
    fileOutputStream.flush()
    fileOutputStream.close()

    // 5. 返回压缩是否成功
    return isCompressed
}

suspend fun compressImageWithCompressor(context: Context, imagePath: String): File {
    val file = File(imagePath)
    return Compressor.compress(context, file) {
//        default(
//            width = 612,
//            height = 816,
//            format = Bitmap.CompressFormat.JPEG,
//            quality = 80
//        )
        resolution(1280, 720)
        quality(80)
        format(Bitmap.CompressFormat.JPEG)
        size(2_097_152) // 2 MB
    }
}


fun getScreenWidth(context: Context): Int {
    val displayMetrics = context.resources.displayMetrics
    return displayMetrics.widthPixels
}

fun getScreenSizeWidth(context: Context): Int {
    return getScreenSize(context).getWidth()
}

fun getScreenSizeHeight(context: Context): Int {
    return getScreenSize(context).getHeight()
}

fun getScreenSize(context: Context): Size {
    val windowManager = context.getSystemService(WindowManager::class.java)
    val size: Size
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val windowMetrics = windowManager.currentWindowMetrics
        size = Size(windowMetrics.bounds.width(), windowMetrics.bounds.height())
    } else {
        val display = windowManager.defaultDisplay
        val point = Point()
        display.getRealSize(point)
        size = Size(point.x, point.y)
    }
    return size
}

fun grantCamera(activity: FragmentActivity, block: (b:Boolean)-> Unit): Unit {
    val permissions = mutableListOf<String>()
    if (activity.checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED
    ) {
        permissions.add(Manifest.permission.CAMERA)
    }
    if (permissions.size > 0) {
        PermissionX.init(activity)
            .permissions(permissions)
            .setDialogTintColor(Color.parseColor("#1972e8"), Color.parseColor("#8ab6f5"))
            .onExplainRequestReason { scope, deniedList, beforeRequest ->
                val message = "PermissionX needs following permissions to continue"
                scope.showRequestReasonDialog(deniedList, message, "Allow", "Deny")
            }
            .onForwardToSettings { scope, deniedList ->
                val message = "Please allow following permissions in settings"
                scope.showForwardToSettingsDialog(deniedList, message, "Allow", "Deny")
            }
            .request { allGranted, grantedList, deniedList ->
                if (allGranted) {
                    Toast.makeText(activity, "All permissions are granted", Toast.LENGTH_SHORT)
                        .show()
                    block(true)
                } else {
                    Toast.makeText(
                        activity,
                        "The following permissions are denied：$deniedList",
                        Toast.LENGTH_SHORT
                    ).show()
                    block(false)
                }
            }
    }else{
        block(true)
    }
}

fun grantExternalStorage(activity: FragmentActivity, block: (b:Boolean)-> Unit): Unit {
    val permissions = mutableListOf<String>()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        // android 13及其以上版本
        if (activity.checkSelfPermission(Manifest.permission.READ_MEDIA_IMAGES)
            == PackageManager.PERMISSION_DENIED
        ) {
            permissions.add(Manifest.permission.READ_MEDIA_IMAGES)
        }
    } else {
        if (activity.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
            == PackageManager.PERMISSION_DENIED ||
            activity.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            == PackageManager.PERMISSION_DENIED
        ) {
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
    }
    if (permissions.size > 0) {
        PermissionX.init(activity)
            .permissions(permissions)
            .setDialogTintColor(Color.parseColor("#1972e8"), Color.parseColor("#8ab6f5"))
            .onExplainRequestReason { scope, deniedList, beforeRequest ->
                val message = "PermissionX needs following permissions to continue"
                scope.showRequestReasonDialog(deniedList, message, "Allow", "Deny")
            }
            .onForwardToSettings { scope, deniedList ->
                val message = "Please allow following permissions in settings"
                scope.showForwardToSettingsDialog(deniedList, message, "Allow", "Deny")
            }
            .request { allGranted, grantedList, deniedList ->
                if (allGranted) {
                    Toast.makeText(activity, "All permissions are granted", Toast.LENGTH_SHORT)
                        .show()
                    block(true)
                } else {
                    Toast.makeText(
                        activity,
                        "The following permissions are denied：$deniedList",
                        Toast.LENGTH_SHORT
                    ).show()
                    block(false)
                }
            }
    }else{
        block(true)
    }
}

fun isConnectedToWifi(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI)
        return networkInfo?.isConnected == true
    } else {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(network)
        return capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
    }
}

fun getLocalIpAddress(context: Context): String {
    val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    val connectionInfo: WifiInfo = wifiManager.connectionInfo
    val ipAddress = connectionInfo.ipAddress
    return intToIp(ipAddress)
}

// 将 int 类型的 IP 地址转换为字符串格式
fun intToIp(i: Int): String {
    return (i and 0xFF).toString() + "." +
            ((i shr 8) and 0xFF) + "." +
            ((i shr 16) and 0xFF) + "." +
            (i shr 24 and 0xFF)
}

fun showToast(s: String): Unit {
    AppExecutors.globalAppExecutors()?.mainThread()?.execute{
        ToastUtil.showToast(GetProvider.get().context, s)
    }
}

fun getFileNameFromPath(uri: Uri): String? {
    return getPathFromUri(GetProvider.get().context, uri)?.substringAfterLast("/")
    //return uri.path?.substringAfterLast("/")
}

fun getFileSize(context: Context, uri: Uri): Long? {
    return when (uri.scheme) {
        "content" -> {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (sizeIndex != -1) cursor.getLong(sizeIndex) else null
                } else {
                    null
                }
            }
        }
        "file" -> {
            uri.path?.let { path ->
                File(path).length()
            }
        }
        else -> null // 其他类型 Uri 可能需要自定义处理
    }
}

fun getPathFromUri(context: Context, uri: Uri): String? {
    val scheme = uri.scheme
    return when {
        // 如果 URI 是文件类型的 URI（如 content:// ）
        ContentResolver.SCHEME_CONTENT == scheme -> {
            if (DocumentsContract.isDocumentUri(context, uri)) {
                // 处理 DocumentProvider 类型的 URI（如 Google Drive）
                if (isExternalStorageDocument(uri)) {
                    val docId = DocumentsContract.getDocumentId(uri)
                    val split = docId.split(":")
                    val type = split[0]
                    if ("primary" == type) {
                        return context.getExternalFilesDir(null)?.path + "/" + split[1]
                    }
                }
            } else {
                // 处理其他的 content 类型的 URI
                val projection = arrayOf(MediaStore.Images.Media.DATA)
                val cursor: Cursor? = context.contentResolver.query(uri, null, null, null, null)
                cursor?.use {
                    val columnIndex = it.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                    if (it.moveToFirst()) {
                        return it.getString(columnIndex)
                    }
                }
            }
            null
        }
        // 如果是文件类型 URI（如 file://）
        ContentResolver.SCHEME_FILE == scheme -> uri.path
        else -> null
    }
}

// 检查是否是外部存储文档
fun isExternalStorageDocument(uri: Uri): Boolean {
    return "com.android.externalstorage.documents" == uri.authority
}

fun generateQRCode(content: String): Bitmap? {
    val size = 500 // QR code size in pixels
    val qrCodeWriter = QRCodeWriter()

    return try {
        val bitMatrix = qrCodeWriter.encode(content, BarcodeFormat.QR_CODE, size, size)
        val width = bitMatrix.width
        val height = bitMatrix.height
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
        for (x in 0 until width) {
            for (y in 0 until height) {
                bitmap.setPixel(x, y, if (bitMatrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
            }
        }
        bitmap
    } catch (e: WriterException) {
        e.printStackTrace()
        null
    }
}

fun formatTimeWithDay(timestamp: Long): String {
    val format = SimpleDateFormat("yyyy-MM-dd") // 定义格式
    val date = Date(timestamp) // 将 Long 转为 Date
    return format.format(date) // 格式化为字符串
}

fun formatTimeWithSimpleDateFormat(timestamp: Long): String {
    val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss") // 定义格式
    val date = Date(timestamp) // 将 Long 转为 Date
    return format.format(date) // 格式化为字符串
}

fun normalizeFilePath(filePath: String): String {
    // 使用正则表达式替换多个连续的 "/" 为单个 "/"
    return filePath.replace(Regex("/{2,}"), "/")
}

fun ensureLocalDirectoryExists(path: String): Boolean {
    val file = File(path)
    return if (file.exists()) {
        if (file.isDirectory) {
            true // 路径已存在且是文件夹
        } else {
            false // 路径已存在但不是文件夹
        }
    } else {
        file.mkdirs() // 创建文件夹，包括中间路径
    }
}
fun delFile(filePath: String): Boolean {
    val file = File(filePath)
    return if (file.exists() && file.isFile) {
        file.delete()
    } else {
        false
    }
}
fun deleteDirectory(directoryPath: String): Boolean {
    val directory = File(directoryPath)
    if (directory.exists() && directory.isDirectory) {
        val files = directory.listFiles()
        if (files != null) {
            for (file in files) {
                if (file.isDirectory) {
                    deleteDirectory(file.absolutePath) // 递归删除子目录
                } else {
                    file.delete() // 删除文件
                }
            }
        }
    }
    return directory.delete() // 删除空目录
}

fun isFileNameValid(fileName: String): Boolean {
    // 定义文件名合法性规则的正则表达式
    val regex = Regex("^[^<>:\"/\\\\|?*]+$") // 文件名不能包含 < > : " / \ | ? *
    val maxLength = 255 // 通常文件名的最大长度为 255 个字符

    return fileName.isNotBlank() && // 文件名不能为空
            fileName.length <= maxLength && // 文件名不能超过最大长度
            regex.matches(fileName) // 文件名必须符合正则表达式规则
}
fun isFolderNameValid(folderName: String): Boolean {
    // 检测文件夹名是否为空或全是空白字符
    if (folderName.isBlank()) {
        return false
    }

    // 定义非法字符的正则表达式
    val invalidCharacters = Regex("[\\\\/:*?\"<>|]")

    // 检测是否包含非法字符
    if (invalidCharacters.containsMatchIn(folderName)) {
        return false
    }

    // 检测长度是否符合（通常文件夹名称长度不要超过255个字符）
    if (folderName.length > 255) {
        return false
    }

    // 文件夹名称合规
    return true
}
fun getFileNameFromPath(path: String): String {
    return path.trimEnd('/').substringAfterLast('/')
}

fun isFullFolderNameValid(fullFolderName: String): Boolean {
    // 检测全路径文件夹名是否为空或全是空白字符
    if (fullFolderName.isBlank()) {
        return false
    }

    // 定义非法字符的正则表达式
    val invalidCharacters = Regex("[\\\\:*?\"<>|]")

    // 检测是否包含非法字符
    if (invalidCharacters.containsMatchIn(fullFolderName)) {
        return false
    }

    // 文件夹名称合规
    return true
}

fun isIPAddress(address: String): Boolean {
    val ipv4Regex = Regex("^((25[0-5]|2[0-4][0-9]|[0-1]?[0-9]{1,2})\\.){3}(25[0-5]|2[0-4][0-9]|[0-1]?[0-9]{1,2})$")
    val ipv6Regex = Regex("^([0-9a-fA-F]{1,4}:){1,7}[0-9a-fA-F]{1,4}$")
    return ipv4Regex.matches(address) || ipv6Regex.matches(address)
}

fun showCustomPlayerDialog(context: Context, title: String, block: (binding: DialogCustomPlayerBinding) -> Unit): AlertDialog? {
    // 加载自定义布局
    val binding = DialogCustomPlayerBinding.inflate(LayoutInflater.from(context), null, false)
    val dialogView = binding.root

    binding.tvTitle.text = title

    // 创建 AlertDialog
    val alertDialog = AlertDialog.Builder(context)
        .setView(dialogView)
        .setCancelable(false) // 点击外部是否可以取消
        .create()
    block(binding)
    // 显示对话框
    alertDialog.show()

    // 设置对话框的宽高
    val w = DisplayUtils.getScreenWidth(alertDialog.context)
    alertDialog.window?.setLayout(w, WindowManager.LayoutParams.WRAP_CONTENT)
    // 调整对话框透明度
    alertDialog.window?.attributes = alertDialog.window?.attributes?.apply {
        alpha = 1f // 对话框本身的透明度
    }
    alertDialog.window?.setDimAmount(0.4f) // 背景模糊透明度
    // 设置对话框背景为圆角 drawable
    alertDialog.window?.setBackgroundDrawableResource(R.drawable.rounded_white_background_16)
    return alertDialog
}

fun showCustomFileInfoDialog(context: Context, title: String, block: (binding: DialogCustomFileInfoBinding) -> Unit): AlertDialog? {
    // 加载自定义布局
    val binding = DialogCustomFileInfoBinding.inflate(LayoutInflater.from(context), null, false)
    val dialogView = binding.root

    binding.tvTitle.text = title

    // 创建 AlertDialog
    val alertDialog = AlertDialog.Builder(context)
        .setView(dialogView)
        .setCancelable(true) // 点击外部是否可以取消
        .create()
    block(binding)
    // 显示对话框
    alertDialog.show()

    // 设置对话框的宽高
    val w = DisplayUtils.getScreenWidth(alertDialog.context) * 3 / 4
    alertDialog.window?.setLayout(w, WindowManager.LayoutParams.WRAP_CONTENT)
    // 调整对话框透明度
    alertDialog.window?.attributes = alertDialog.window?.attributes?.apply {
        alpha = 1f // 对话框本身的透明度
    }
    alertDialog.window?.setDimAmount(0.4f) // 背景模糊透明度
    // 设置对话框背景为圆角 drawable
    alertDialog.window?.setBackgroundDrawableResource(R.drawable.rounded_white_background_16)
    return alertDialog
}


fun showCustomInputDialog(context: Context, title: String, hint: String, cancel: () -> Unit, ok: (s:String) -> Boolean,) {
    // 加载自定义布局
    val binding = DialogCustomInputBinding.inflate(LayoutInflater.from(context), null, false)
    val dialogView = binding.root

    replaceCursorStyle(context, binding.etInput)
    binding.tvTitle.text = title
    binding.etInput.setHint(hint)

    // 创建 AlertDialog
    val alertDialog = AlertDialog.Builder(context)
        .setView(dialogView)
        .setCancelable(false) // 点击外部是否可以取消
        .create()

    // 处理按钮点击事件
    binding.btnCancel.setOnClickListener {
        cancel()
        alertDialog.dismiss() // 关闭对话框
    }
    binding.btnOk.setOnClickListener {
        val input =  binding.etInput.text.toString()
        if (ok(input)) {
            alertDialog.dismiss() // 关闭对话框
        } else {

        }
    }
    // 显示对话框
    alertDialog.show()

    // 设置对话框的宽高
    val w = DisplayUtils.getScreenWidth(alertDialog.context) * 3 / 4
    alertDialog.window?.setLayout(w, WindowManager.LayoutParams.WRAP_CONTENT)
    // 调整对话框透明度
    alertDialog.window?.attributes = alertDialog.window?.attributes?.apply {
        alpha = 1f // 对话框本身的透明度
    }
    alertDialog.window?.setDimAmount(0.4f) // 背景模糊透明度
    // 设置对话框背景为圆角 drawable
    alertDialog.window?.setBackgroundDrawableResource(R.drawable.rounded_white_background_16)
}

fun showCustomAlertDialog(context: Context, title: String, message: String, cancel: () -> Unit, ok: () -> Unit,) {
    // 加载自定义布局
    val binding = DialogCustomAlertBinding.inflate(LayoutInflater.from(context), null, false)
    val dialogView = binding.root

    binding.tvTitle.text = title
    binding.tvMsg.text = message

    // 创建 AlertDialog
    val alertDialog = AlertDialog.Builder(context)
        .setView(dialogView)
        .setCancelable(false) // 点击外部是否可以取消
        .create()

    // 处理按钮点击事件
    binding.btnCancel.setOnClickListener {
        cancel()
        alertDialog.dismiss() // 关闭对话框
    }
    binding.btnOk.setOnClickListener {
        ok()
        alertDialog.dismiss() // 关闭对话框
    }

    // 显示对话框
    alertDialog.show()

    // 设置对话框的宽高
    var w = DisplayUtils.getScreenWidth(alertDialog.context) * 3 / 4
    val isLandscape = context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    if (isLandscape) {
        // 当前是横屏
        w = DisplayUtils.getScreenHeight(alertDialog.context) * 3 / 4
    } else {
        // 当前是竖屏
    }
    alertDialog.window?.setLayout(w, WindowManager.LayoutParams.WRAP_CONTENT)
    // 调整对话框透明度
    alertDialog.window?.attributes = alertDialog.window?.attributes?.apply {
        alpha = 1f // 对话框本身的透明度
    }
    alertDialog.window?.setDimAmount(0.4f) // 背景模糊透明度
    // 设置对话框背景为圆角 drawable
    alertDialog.window?.setBackgroundDrawableResource(R.drawable.rounded_white_background_16)

//    val layoutParams = window?.attributes
//    layoutParams?.gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL // 设置对话框在屏幕顶部居中
//    layoutParams?.y = 200 // 距离顶部的偏移量（单位：像素）
//    window?.attributes = layoutParams

//    // 自定义布局控制位置
//    val window = dialog.window
//    window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
//    window?.setGravity(Gravity.NO_GRAVITY)
}


fun replaceCursorStyle(context: Context, et: EditText) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        // For Android 10 and above (API 29+)
        val cursorDrawable = context.resources.getDrawable(R.drawable.custom_cursor, null)
        et.textCursorDrawable = cursorDrawable
    } else {
        // For Android 9 and below
        try {
            val editorField = EditText::class.java.getDeclaredField("mEditor")
            editorField.isAccessible = true
            val editor = editorField.get(et)

            val cursorDrawable = context.resources.getDrawable(R.drawable.custom_cursor)
            val cursorField = editor.javaClass.getDeclaredField("mCursorDrawable")
            cursorField.isAccessible = true
            cursorField.set(editor, arrayOf(cursorDrawable, cursorDrawable))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

val textSuffixType = listOf("text", "txt", "rtf", "md")
val zipSuffixType = listOf("zip", "rar", "tar", ".gz", "7z")
val docSuffixType = listOf("doc", "docx")
val excSuffixType = listOf("xls", "xlsx", "csv")
val pptSuffixType = listOf("ppt")
val pdfSuffixType = listOf("pdf")
val musicSuffixType = listOf("mp3","m4a","flac","wav","aac")
val videoSuffixType = listOf("mp4","mkv","avi","mov","flv", "rm", "rmvb", "wmv","webm")
val imageSuffixType = listOf("png","jpeg","jpg","gif","bmp")
val apkSuffixType = listOf("apk")
val otherSuffixType = listOf(
    "java",
    "py",
    "js",
    "html",
    "css",
    "json",
    "xml",
    "c",
    "cpp",
    "bat",
    "sh",

    "exe",
    "dll",
    "iso",

    "log",
    "config",
    "ini",
)

fun getIcon4File(context: Context, filename: String): Drawable {
    val extend = filename.substringAfterLast('.', "").lowercase()
    val i = when (extend){
         ""-> {
             context.resources.getDrawable(R.drawable.svg_file_unknown_icon)
         }
         in textSuffixType-> {
             context.resources.getDrawable(R.drawable.svg_text_icon)
         }
         in zipSuffixType-> {
             context.resources.getDrawable(R.drawable.svg_zip_icon)
         }
         in docSuffixType-> {
             context.resources.getDrawable(R.drawable.svg_word_icon)
         }
        in excSuffixType-> {
            context.resources.getDrawable(R.drawable.svg_excel_icon)
        }
        in pptSuffixType-> {
            context.resources.getDrawable(R.drawable.svg_ppt_icon)
        }
        in pdfSuffixType-> {
            context.resources.getDrawable(R.drawable.svg_pdf_icon)
        }
        in musicSuffixType-> {
            context.resources.getDrawable(R.drawable.svg_music_icon)
        }
        in videoSuffixType-> {
            context.resources.getDrawable(R.drawable.svg_media_icon)
        }
        in imageSuffixType-> {
            context.resources.getDrawable(R.drawable.svg_image_icon)
        }
        in apkSuffixType-> {
            context.resources.getDrawable(R.drawable.svg_apk_icon)
        }
        in otherSuffixType-> {
            context.resources.getDrawable(R.drawable.svg_file_unknown_icon)
        }
        else -> {
            context.resources.getDrawable(R.drawable.svg_file_unknown_icon)
        }
    }
    // 设置内边距：左、上、右、下
    val padding = DisplayUtils.dp2px(context, 3f)
    val insetDrawable = InsetDrawable(i, padding, padding, padding, padding)
    return insetDrawable
}

fun sortFiles(d: Vector<ChannelSftp.LsEntry>, value: Int?) {
    //排序
    //        "按名称",
    //        "按类型",
    //        "按大小升序",
    //        "按大小降序",
    //        "按时间升序",
    //        "按时间降序",
    when (value) {
        0 -> {
            d.sortBy { data ->
                data.filename
            }
        }

        1 -> {
            d.sortBy { data ->
                val extension = data.filename.substringAfterLast('.', "")
                if (TextUtils.isEmpty(extension)) {
                    data.filename
                } else {
                    extension
                }
            }
        }

        2 -> {
            d.sortBy { data ->
                data.attrs.size
            }
        }

        3 -> {
            d.sortByDescending { data ->
                data.attrs.size
            }
        }

        4 -> {
            d.sortBy { data ->
                data.attrs.mTime
            }
        }

        5 -> {
            d.sortByDescending { data ->
                data.attrs.mTime
            }
        }

        else -> {
            d.sortBy { data ->
                data.filename
            }
        }
    }
}

fun sortFileTracks(d: MutableList<FileTrack>, value: Int?) {
    //排序
    //        "按名称",
    //        "按类型",
    //        "按大小升序",
    //        "按大小降序",
    //        "按时间升序",
    //        "按时间降序",
    when (value) {
        0 -> {
            d.sortBy { data ->
                data.name
            }
        }

        1 -> {
            d.sortBy { data ->
                val extension = data.name.substringAfterLast('.', "")
                if (TextUtils.isEmpty(extension)) {
                    data.name
                } else {
                    extension
                }
            }
        }

        2 -> {
            d.sortBy { data ->
                data.size
            }
        }

        3 -> {
            d.sortByDescending { data ->
                data.size
            }
        }

        4 -> {
            d.sortBy { data ->
                data.mTime
            }
        }

        5 -> {
            d.sortByDescending { data ->
                data.mTime
            }
        }

        else -> {
            d.sortBy { data ->
                data.name
            }
        }
    }
}

fun sortFiles(d: MutableList<File>, value: Int?) {
    //排序
    //        "按名称",
    //        "按类型",
    //        "按大小升序",
    //        "按大小降序",
    //        "按时间升序",
    //        "按时间降序",
    when (value) {
        0 -> {
            d.sortBy { data ->
                data.name
            }
        }

        1 -> {
            d.sortBy { data ->
                val extension = data.name.substringAfterLast('.', "")
                if (TextUtils.isEmpty(extension)) {
                    data.name
                } else {
                    extension
                }
            }
        }

        2 -> {
            d.sortBy { data ->
                data.length()
            }
        }

        3 -> {
            d.sortByDescending { data ->
                data.length()
            }
        }

        4 -> {
            d.sortBy { data ->
                data.lastModified()
            }
        }

        5 -> {
            d.sortByDescending { data ->
                data.lastModified()
            }
        }

        else -> {
            d.sortBy { data ->
                data.name
            }
        }
    }
}

fun createFileWithPath(filePath: String): File? {
    try {
        val file = File(filePath)

        if (file.parentFile == null){
            return null
        }
        // 确保父目录存在
        if (!file.parentFile!!.exists()) {
            file.parentFile!!.mkdirs()
        }

        // 创建文件
        if (!file.exists()) {
            file.createNewFile()
        }

        return file
    } catch (e: IOException) {
        e.printStackTrace()
    }
    return null
}

fun removeFileExtension(filePath: String): String {
    val file = File(filePath)
    val fileName = file.name
    val lastDotIndex = fileName.lastIndexOf('.')

    return if (lastDotIndex != -1) {
        file.absolutePath.substring(0, file.absolutePath.lastIndexOf('.'))
    } else {
        file.absolutePath // 如果没有后缀名，则返回完整路径
    }
}

fun saveDrawableAsJPG(drawable: Drawable, outputFile: File) {
    try {
        // 创建一个 Bitmap 对象，并且其大小与 Drawable 的宽高一致
        val width = drawable.intrinsicWidth
        val height = drawable.intrinsicHeight
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        // 创建一个 Canvas，将 Drawable 绘制到 Bitmap 上
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        // 保存 Bitmap 到文件（JPG 格式）
        FileOutputStream(outputFile).use { outputStream ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream) // 设置压缩质量
        }

    } catch (e: IOException) {
        e.printStackTrace()
    }
}

fun saveBitmapToFile(bitmap: Bitmap, outputFile: File) {
    try {
        // 创建 FileOutputStream
        FileOutputStream(outputFile).use { outputStream ->
            // 压缩 Bitmap 为 JPEG 格式，质量设置为 90（0 到 100）
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
        }
    } catch (e: IOException) {
        e.printStackTrace()
    }
}

fun saveVideoThumbnail(context: Context, videoPath: String, outputFileName: String): File? {
    try {
        // 使用 Glide 提取视频的缩略图
        val requestOptions = RequestOptions().override(200, 200) // 设置缩略图大小
        val futureTarget: FutureTarget<Bitmap> = Glide.with(context)
            .asBitmap()
            .load(videoPath)
            .apply(requestOptions)
            .transform(
                MultiTransformation(
                    CenterCrop(),
                    RoundedCorners(DisplayUtils.dp2px(context, 2f))
                )
            )
            .frame(0) // 提取第 0 毫秒的帧
            .submit()

        // 获取 Bitmap
        val bitmap = futureTarget.get()

        val outputFile = createFileWithPath(outputFileName)

        // 将 Bitmap 保存到文件
        val outputStream = FileOutputStream(outputFile)
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream) // 压缩并保存
        outputStream.flush()
        outputStream.close()

        // 释放资源
        Glide.with(context).clear(futureTarget)

        return outputFile
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return null
}
fun saveVideoThumbnailWithOriginalSize(context: Context, videoPath: String, outputFileName: String): File? {
    try {
        // 使用 MediaMetadataRetriever 获取视频的宽高
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(videoPath)
        val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toInt() ?: 0
        val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toInt() ?: 0
        retriever.release()

        if (width == 0 || height == 0){
            return null
        }
        val scale = height.toFloat() / width
        // 使用 Glide 提取视频的第一帧
        val futureTarget: FutureTarget<Bitmap> = Glide.with(context)
            .asBitmap()
            .load(videoPath)
            .apply(RequestOptions().override(200, (200*scale).toInt())) // 设置与视频尺寸一致的宽高
            .frame(0) // 提取第 0 毫秒的帧
            .submit()

        // 获取 Bitmap
        val bitmap = futureTarget.get()

        val outputFile = createFileWithPath(outputFileName) ?: return null

        // 将 Bitmap 保存到文件
        val outputStream = FileOutputStream(outputFile)
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream) // 压缩并保存
        outputStream.flush()
        outputStream.close()

        // 释放资源
        Glide.with(context).clear(futureTarget)

        return outputFile
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return null
}

fun openFileWithSystemApp(context: Context, file: File) {
    val appId = context.packageName
    val authority = "${appId}.fileprovider"
    try {
        // 判断文件是否存在
        if (!file.exists()) {
            showToast("文件不存在")
            return
        }

        // 获取文件后缀名
        val extension = file.extension
        // 推断 MIME 类型
        val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
            ?: "*/*" // 如果无法识别后缀，使用通配符

        // 获取文件的 Uri
        val uri: Uri = FileProvider.getUriForFile(context, authority, file)

        // 创建 Intent
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) // 授予临时读取权限
        }

        // 启动系统应用
        context.startActivity(intent)
    } catch (e: Exception) {
        e.printStackTrace()
        showToast("打开失败")
    }
}
