package com.example.ftp.service

import com.jcraft.jsch.UserInfo
import timber.log.Timber

class CustomUserInfo : UserInfo {
        override fun getPassphrase(): String? {
            return null
        }

        override fun getPassword(): String? {
            return null
        }

        override fun promptPassword(message: String?): Boolean {
            return false
        }

        override fun promptPassphrase(message: String?): Boolean {
            return false
        }

        override fun promptYesNo(message: String?): Boolean {
            // 通过返回 true 来接受主机的公钥
            Timber.d("promptYesNo: ${message}")
            return true  // 允许接受公钥
        }

        override fun showMessage(message: String?) {
            // 用于显示消息
            Timber.d("showMessage: ${message}")
        }
    }