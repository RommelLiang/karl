
import android.util.Log
import androidx.annotation.Nullable
import retrofit2.Call
import retrofit2.CallAdapter

import retrofit2.Retrofit
import java.io.IOException
import java.lang.reflect.Type


class StringDataAdapterFactory : CallAdapter.Factory() {

    companion object {
        val INSTANCE: StringDataAdapterFactory by lazy(LazyThreadSafetyMode.NONE) {
            StringDataAdapterFactory()
        }
    }
    @Nullable
    override fun get(
        returnType: Type,
        annotations: Array<Annotation?>?,
        retrofit: Retrofit?
    ): CallAdapter<*, *>? {
        return if (returnType === String::class.java) StringCallAdapter() else null
    }

    internal inner class StringCallAdapter : CallAdapter<String?, String?> {
        override fun responseType(): Type {
            return String::class.java
        }

        override fun adapt(call: Call<String?>): String {
            try {
                return "${call.execute().body()}"
            } catch (e: IOException) {
                Log.e("--------",e.toString())
            }
            return ""
        }
    }
}