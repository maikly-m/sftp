/*
 * Copyright 2017-2023 Guilin Zhishen.
 * All Rights Reserved.
 */
package com.example.ftp.utils;

import android.text.TextUtils;

import com.example.ftp.utils.gson.GsonUtil;
import com.example.ftp.utils.gson.Gsons;


public class MySPUtil {
    public static final String FILE_NAME = "my";
    private static MySPUtil instance;
    private SPUtils spUtils;

    private MySPUtil() {
        spUtils = SPUtils.getInstance(FILE_NAME);
    }

    public static MySPUtil getInstance() {
        if (instance == null) {
            synchronized (MySPUtil.class) {
                if (instance == null) {
                    instance = new MySPUtil();
                }
            }
        }
        return instance;
    }

    private <T> void setObject(String key, T object) {
        if (object == null) {
            spUtils.put(key, "");
        } else {
            spUtils.put(key, GsonUtil.toJson(object));
        }
    }

    private <T> T getObject(String key, Class<T> clazz) {
        String content = spUtils.getString(key, "");
        if (!TextUtils.isEmpty(content)) {
            return Gsons.deserialization(clazz, content);
        }
        return null;
    }

    public static final String SERVER_IP = "server_ip";

    public void setServerIp(String s){
        spUtils.put(SERVER_IP, s);
    }
    public String getServerIp(){
        return spUtils.getString(SERVER_IP, "");
    }
}
