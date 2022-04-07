package com.karl.network.view_model

import androidx.lifecycle.LiveData

import androidx.lifecycle.ViewModel
import com.karl.network.bean.Repo
import com.karl.network.net.GitHubService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOn


class MainViewModel : ViewModel() {

    fun teach() =  GitHubService.service.listReposLiveData("RommelLiang")
    fun teachF()  = GitHubService.service.listReposFlow("RommelLiang")

}