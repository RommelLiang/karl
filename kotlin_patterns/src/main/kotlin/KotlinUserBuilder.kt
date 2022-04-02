class KotlinUserBuilder(
    val name: String,
    val age: Int,
    var address: String? = null,
    var gender: String? = null,
    var isMarried: Boolean = false,
    var education: String = "",
    var nationality: String = "CN"
) {


    override fun toString(): String {
        return "KotlinUserBuilder(name='$name', age=$age, address=$address, gender=$gender, isMarried=$isMarried, education='$education', nationality='$nationality')"
    }
}