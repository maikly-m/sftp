package com.emoji.ftp.utils.gson;

import com.google.auto.value.AutoValue;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;

@AutoValue
public abstract class GsonValueBean {
    // The public static method returning a TypeAdapter<Foo> is what
    // tells auto-value-gson to create a TypeAdapter for Foo.
    public static TypeAdapter<GsonValueBean> typeAdapter(Gson gson) {
        return new AutoValue_GsonValueBean.GsonTypeAdapter(gson);
    }
}
