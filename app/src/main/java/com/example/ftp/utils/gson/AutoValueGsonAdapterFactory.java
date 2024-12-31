

package com.example.ftp.utils.gson;

import com.google.gson.TypeAdapterFactory;
import com.ryanharter.auto.value.gson.GsonTypeAdapterFactory;


@GsonTypeAdapterFactory
public abstract class AutoValueGsonAdapterFactory implements TypeAdapterFactory {

    // Static factory method to access the package
    // private generated implementation
    public static TypeAdapterFactory create() {
        return new AutoValueGson_AutoValueGsonAdapterFactory();
    }
}
