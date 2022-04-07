package com.karl.network.bean

data class Repo(val id:Int, val name:String, val html_url:String, val description:String){
    override fun toString(): String {
        println("id:$id,name:$name")
        return super.toString()
    }
}

