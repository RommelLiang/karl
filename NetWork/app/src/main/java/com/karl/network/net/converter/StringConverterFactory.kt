package com.karl.network.net.converter

import androidx.annotation.Nullable
import okhttp3.ResponseBody

import retrofit2.Converter
import retrofit2.Retrofit
import java.io.IOException
import java.lang.reflect.Type


class StringConverterFactory : Converter.Factory() {

    companion object{
        private val instance :StringConverterFactory by lazy {
            StringConverterFactory()
        }
        fun create(): StringConverterFactory = instance
    }
    @Nullable
    override fun responseBodyConverter(
        type: Type,
        annotations: Array<Annotation>,
        retrofit: Retrofit
    ): Converter<ResponseBody, *>? {
        return if (type === String::class.java) {
            StringConverter()
        } else null
    }

    internal inner class StringConverter :
        Converter<ResponseBody, String> {
        @Throws(IOException::class)
        override fun convert(value: ResponseBody): String {
            return value.string()
        }
    }
}