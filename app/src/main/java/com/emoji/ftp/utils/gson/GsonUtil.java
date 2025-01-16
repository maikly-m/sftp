package com.emoji.ftp.utils.gson;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GsonUtil {
    public static Gson gson = null;

    static {
        //            gson = new Gson();
        //保留空字段
        gson = new GsonBuilder().serializeNulls().create();
    }

    private GsonUtil() {
    }

    /**
     * 转成json
     *
     * @param object
     * @return
     */
    public static String toJson(Object object) {
        String gsonString = null;
        if (gson != null) {
            try {
                gsonString = gson.toJson(object);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return gsonString;
    }

    /**
     * 转成bean
     *
     * @param gsonString
     * @param cls
     * @return
     */
    public static <T> T jsonToBean(String gsonString, Class<T> cls) {
        T t = null;
        if (gson != null) {
            try {
                t = gson.fromJson(gsonString, cls);
            } catch (JsonSyntaxException e) {
                e.printStackTrace();
            }
        }
        return t;
    }


    /**
     * 转成list
     * 解决泛型问题
     *
     * @param json
     * @param cls
     * @param <T>
     * @return
     */
    public static <T> List<T> jsonToList(String json, Class<T> cls) {
        if (json == null || json.equals("")) {
            return new ArrayList<T>();
        }
        List<T> list = new ArrayList<T>();
        try {
            JsonArray array = JsonParser.parseString(json).getAsJsonArray();
            if (gson != null) {
                for (final JsonElement elem : array) {
                    list.add(gson.fromJson(elem, cls));
                }
            }
        } catch (JsonSyntaxException e) {
            e.printStackTrace();
        }
        return list;
    }

    /**
     * 转成list中有map的
     *
     * @param gsonString
     * @return
     */
    public static <T> List<Map<String, T>> jsonToListMaps(String gsonString) {
        List<Map<String, T>> list = null;
        if (gson != null) {
            try {
                list = gson.fromJson(gsonString, new TypeToken<List<Map<String, T>>>() {
                }.getType());
            } catch (JsonSyntaxException e) {
                e.printStackTrace();
            }
        }
        return list;
    }

    /**
     * 转成map的
     *
     * @param gsonString
     * @return
     */
    public static <T> Map<String, T> jsonToMaps(String gsonString) {
        Map<String, T> map = null;
        if (gson != null) {
            try {
                map = gson.fromJson(gsonString, new TypeToken<Map<String, T>>() {
                }.getType());
            } catch (JsonSyntaxException e) {
                e.printStackTrace();
            }
        }
        return map;
    }

    /**
     * 将Map转化为Json
     *
     * @param map
     * @return String
     */
    public static <T> String mapToJson(Map<String, T> map) {
        String jsonStr = null;
        if (gson != null) {
            try {
                jsonStr = gson.toJson(map);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return jsonStr;
    }

    /**
     * 将Json字符串转换成对象
     */
    public static Object jSONToObject(String json, Class<?> beanClass) {
        Object res = null;
        if (gson != null) {
            try {
                res = gson.fromJson(json, beanClass);
            } catch (JsonSyntaxException e) {
                e.printStackTrace();
            }
        }
        return res;
    }

}
