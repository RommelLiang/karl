package com.karl.network.bean

data class Repo(val id:Int = 0, val name:String = "", val htmlUrl:String="", val description:String = ""){

    override fun toString(): String {
        println("id:$id,name:$name")
        return super.toString()
    }
}

