package create

object KotlinStaticSingleton {
    var mKey: String? = null
}

val name:String by lazy(LazyThreadSafetyMode.NONE) { "Karl" }
