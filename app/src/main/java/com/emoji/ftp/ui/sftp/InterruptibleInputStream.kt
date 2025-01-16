package com.emoji.ftp.ui.sftp

import java.io.FileInputStream

class InterruptibleInputStream(val p:String) : FileInputStream(p) {
    companion object{
        const val INTERRUPT_MSG = "my_sftp_interrupted"
    }
    @Volatile
    var interrupted = false // 标志位，控制中断

    override fun read(): Int {
        if (interrupted) throw InterruptedException(INTERRUPT_MSG)
        return super.read()
    }

    override fun read(b: ByteArray, off: Int, len: Int): Int {
        if (interrupted) throw InterruptedException(INTERRUPT_MSG)
        return super.read(b, off, len)
    }

    override fun read(b: ByteArray?): Int {
        if (interrupted) throw InterruptedException(INTERRUPT_MSG)
        return super.read(b)
    }
}