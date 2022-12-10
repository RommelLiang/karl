package com.karl.network.livedata

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel

class UserViewModel: ViewModel() {
    val mapLiveData = Transformations.map(Repository().liveData){
        2*it
    }

    val switchLiveData = Transformations.switchMap(Repository().liveData){

        MutableLiveData("")
    }


}