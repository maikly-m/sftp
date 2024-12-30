package com.example.ftp.utils

import android.Manifest
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Point
import android.graphics.Rect
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Size
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
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

