class KApp {
    val user1 = KotlinUserBuilder("Karl", 18)
    val user2 = KotlinUserBuilder(age = 18, name = "Karl")
    val user3 = KotlinUserBuilder(name = "Karl", age = 18)
    val user4 = KotlinUserBuilder("Karl", 18, address = "SZ")
    val user5 = KotlinUserBuilder("Karl", 18, nationality = "CN", address = "SZ")
    val user6 = KotlinUserBuilder("Karl", 18, "SZ", "男")
    val user7 = KotlinUserBuilder("Karl", 18).apply {
        address = "SZ"
        gender = "男"
        nationality = "CN"
    }
    val user8 = KotlinUserBuilder("Karl", 18).szMeal()


    val ontology = KotlinPrototype("Karl", 18, "SZ", "MEAl")
    val copy1 = ontology.copy()
    val copy2 = ontology.copy(address = "BJ")

    val gas :GasCar= KCar.build(KType.TypeGAS) as GasCar
    val ev :EV= KCar.build(KType.TypeEV) as EV
    val phev : PhEv = KCar.build(KType.TypePhev) as PhEv
    val gas2: GasCar = KCar.Factory(KType.TypeGAS) as GasCar
    val ev2: EV = KCar.invoke(KType.TypeEV) as EV
    val phev2: PhEv = KCar.Factory(KType.TypePhev) as PhEv
    val gas3: GasCar = KCar.Factory<GasCar>() as GasCar
    val ev3: EV = KCar.invoke<EV>() as EV

}

fun main() {
    val phev2: PhEv = KCar.Factory(KType.TypePhev) as PhEv
    println("--------${phev2.replenishingEnergy()}")
}

val szMeal: KotlinUserBuilder.() -> Unit = {
    address = "SZ"
    gender = "男"
    nationality = "CN"
}