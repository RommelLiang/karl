/*interface KCar {
    fun replenishingEnergy()
    companion object{
        fun build(type: KType) {
            when(type){
                KType.GAS -> GasCar()
                KType.EV -> EV()
                KType.PHEV -> PHEV()
            }
        }
    }
}*/

interface KCar {
    fun replenishingEnergy()

    companion object Factory {
        operator fun invoke(type: KType) =
            when (type) {
                KType.TypeGAS -> GasCar()
                KType.TypeEV -> EV()
                KType.TypePhev -> PhEv()
            }

        fun build(type: KType) =
            when (type) {
                KType.TypeGAS -> GasCar()
                KType.TypeEV -> EV()
                KType.TypePhev -> PhEv()
            }

        inline operator fun <reified T : KCar> invoke() =
            when (T::class) {
                GasCar::class -> GasCar()
                EV::class -> EV()
                PhEv::class -> PhEv()
                else -> throw IllegalArgumentException()
            }

    }
}

class GasCar : KCar {
    override fun replenishingEnergy() {
        println("加95")
    }
}

class EV : KCar {
    override fun replenishingEnergy() {
        println("充电")
    }
}

class PhEv : KCar {
    override fun replenishingEnergy() {
        println("充电和加油")
    }
}

enum class KType {
    TypeGAS, TypeEV, TypePhev
}
