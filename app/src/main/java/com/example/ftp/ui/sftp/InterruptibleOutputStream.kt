package com.example.ftp.ui.sftp

import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream

class InterruptibleOutputStream(val p:String) : FileOutputStream(p) {
    companion object{
        const val INTERRUPT_MSG = "my_sftp_interrupted"
    }
    @Volatile
    var interrupted = false // 标志位，控制中断

    override fun write(b: Int) {
        if (interrupted) throw InterruptedException(INTERRUPT_MSG)
        super.write(b)
    }

    override fun write(b: ByteArray?) {
        if (interrupted) throw InterruptedException(INTERRUPT_MSG)
        super.write(b)
    }

    override fun write(b: ByteArray?, off: Int, len: Int) {
        if (interrupted) throw InterruptedException(INTERRUPT_MSG)
        super.write(b, off, len)
    }
}