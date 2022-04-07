package com.karl.network.net

import androidx.lifecycle.LiveData
import com.karl.network.bean.Repo
import com.karl.network.net.adapter_fatory.FlowCallAdapterFactory
import com.karl.network.net.adapter_fatory.LiveDataCallAdapterFactory
import kotlinx.coroutines.flow.Flow
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path



val retrofit: Retrofit by lazy(LazyThreadSafetyMode.NONE) {
    Retrofit.Builder()
        .baseUrl("https://api.github.com/")
        .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
        .addCallAdapterFactory(LiveDataCallAdapterFactory.INSTANCE)
        .addCallAdapterFactory(FlowCallAdapterFactory.INSTANCE)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
}

interface GitHubService {

    companion object {
        val service: GitHubService = retrofit.create(GitHubService::class.java)
    }

    @GET("users/{user}/repos")
    fun listRepos(@Path("user") user: String?): Call<List<Repo?>?>

    @GET("users/{user}/repos")
    fun listReposLiveData(@Path("user") user: String?): LiveData<List<Repo>?>

    @GET("users/{user}/repos")
    fun listReposFlow(@Path("user") user: String?): Flow<List<Repo>?>
}