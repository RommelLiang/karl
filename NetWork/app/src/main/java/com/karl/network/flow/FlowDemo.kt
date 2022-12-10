package com.karl.network.flow

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class FlowDemo {
    var count = 0
    val mutableStateFlow = MutableStateFlow<Num>(Num(count++))
    val mutableSharedFlow = MutableSharedFlow<Num>(3,0)
    fun changeValue(){

        mutableStateFlow.value = Num(count++)
    }

    fun changeShare(){
        GlobalScope.launch {
            mutableSharedFlow.emit(Num(count++))
        }
    }
}

data class Num(val count:Int)