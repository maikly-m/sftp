/*
 * Copyright 2017-2023 Guilin Zhishen.
 * All Rights Reserved.
 */
package com.example.ftp.utils;

import android.text.TextUtils;

import com.example.ftp.bean.ConnectInfo;
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

    public static final String SORT_TYPE = "sort_type";
    public static final String SERVER_PORT = "server_port";
    public static final String SERVER_NAME = "server_name";
    public static final String SERVER_PW = "server_pw";

    public static final String SERVER_CONNECT_INFO = "server_connect_info";
    public static final String CLIENT_CONNECT_INFO = "client_connect_info";

    public void setServerConnectInfo(ConnectInfo info){
        setObject(SERVER_CONNECT_INFO, info);
    }
    public ConnectInfo getServerConnectInfo(){
        return getObject(SERVER_CONNECT_INFO, ConnectInfo.class);
    }

    public void setClientConnectInfo(ConnectInfo info){
        setObject(CLIENT_CONNECT_INFO, info);
    }
    public ConnectInfo getClientConnectInfo(){
        return getObject(CLIENT_CONNECT_INFO, ConnectInfo.class);
    }

    public void setSortType(int type){
        spUtils.put(SORT_TYPE, type);
    }
    public int getSortType(){
        return spUtils.getInt(SORT_TYPE, 0);
    }

}
