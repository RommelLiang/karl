package com.karl.network.net.adapter_fatory

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import retrofit2.*
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type


class LiveDataCallAdapterFactory : CallAdapter.Factory() {
    companion object {
        val INSTANCE: LiveDataCallAdapterFactory by lazy(LazyThreadSafetyMode.NONE) {
            LiveDataCallAdapterFactory()
        }
    }

    override fun get(
        returnType: Type,
        annotations: Array<out Annotation>,
        retrofit: Retrofit
    ): CallAdapter<*, *>? {
        if (getRawType(returnType) != LiveData::class.java) {
            return null
        }

        //val responseType = getParameterUpperBound(0, returnType as ParameterizedType)
        return LiveDataBodyCallAdapter<Any>(getParameterUpperBound(0, returnType as ParameterizedType))
    }

    private inner class LiveDataBodyCallAdapter<R>(private val responseType: Type) : CallAdapter<R, LiveData<R>> {
        override fun responseType(): Type {
            return responseType
        }

        override fun adapt(call: Call<R>): LiveData<R> =
            MutableLiveData<R>().apply {
                call.enqueue(object : Callback<R> {
                    override fun onResponse(call: Call<R>, response: Response<R>) {
                        if (call.isCanceled) return
                        if (response.isSuccessful) {
                            postValue(response.body())
                        } else {
                            postValue(null)
                        }
                    }

                    override fun onFailure(call: Call<R>, t: Throwable?) {
                        if (call.isCanceled) return
                        postValue(null)
                    }
                })
            }
    }

}

