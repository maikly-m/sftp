package com.emoji.ftp.utils.gson;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import androidx.annotation.Nullable;

public class Gsons {

    @NotNull
    private static GsonBuilder getGsonBuilder() {
        // 注册AutoValue
        return new GsonBuilder().registerTypeAdapterFactory(AutoValueGsonAdapterFactory.create());
    }

    @NotNull
    private static Gson getGson() {
        return getGsonBuilder().create();
    }

    @Nullable
    public static <K, V> Map<K, V> deserializationForMap(Type type, String json) {
        Gson gson = getGsonBuilder().enableComplexMapKeySerialization().create();
        try {
            return gson.fromJson(json, type);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static <K, V> String serialization(Type type, Map<K, V> map) {
        Gson gson = getGsonBuilder().enableComplexMapKeySerialization().create();
        return gson.toJson(map, type);
    }

    public static <T> String serialization(Type type, List<T> list) {
        return getGson().toJson(list, type);
    }

    public static <T> String serialization(Class<T> clazz, T jObj) {
        Gson gson = getGsonBuilder().create();
        return gson.toJson(jObj, clazz);
    }

    public static <T> String serializationBySpecial(Class<T> clazz, T jObj) {
        Gson gson = getGsonBuilder().serializeSpecialFloatingPointValues().create();
        return gson.toJson(jObj, clazz);
    }

    public static <T> List<T> deserialization(Type type, String json) {
        try {
            return getGson().fromJson(json, type);
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    @Nullable
    public static <T> T deserialization(Class<T> clazz, String json) {
        Gson gson = getGsonBuilder().create();
        try {
            return gson.fromJson(json, clazz);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
