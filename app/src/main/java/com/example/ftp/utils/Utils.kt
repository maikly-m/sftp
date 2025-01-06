package com.example.ftp.utils

import android.Manifest
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Point
import android.graphics.Rect
import android.media.MediaScannerConnection
import android.net.Uri
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Size
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import com.example.ftp.provider.GetProvider
import com.example.ftp.utils.thread.AppExecutors
import com.google.zxing.BarcodeFormat
import com.google.zxing.WriterException
import com.google.zxing.qrcode.QRCodeWriter
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

fun recover(window: Window?, config: androidx.core.util.Pair<Int?, Int>) {
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

fun isIPAddress(address: String): Boolean {
    val ipv4Regex = Regex("^((25[0-5]|2[0-4][0-9]|[0-1]?[0-9]{1,2})\\.){3}(25[0-5]|2[0-4][0-9]|[0-1]?[0-9]{1,2})$")
    val ipv6Regex = Regex("^([0-9a-fA-F]{1,4}:){1,7}[0-9a-fA-F]{1,4}$")
    return ipv4Regex.matches(address) || ipv6Regex.matches(address)
}

