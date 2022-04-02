interface ACar
class Gas : ACar
class Ev : ACar
class Phev : ACar

abstract class AbstractFactory {
    abstract fun build(): ACar

    companion object Factory {
        inline fun <reified T : AbstractFactory> invoke() =
            when (T::class) {
                GasFactory::class -> GasFactory()
                EvFactory::class -> EvFactory()
                PhevFactory::class -> PhevFactory()
                else -> throw IllegalArgumentException()
            }
    }
}

class GasFactory : AbstractFactory() {
    override fun build(): ACar = Gas()
}

class EvFactory : AbstractFactory() {
    override fun build(): ACar = Ev()
}

class PhevFactory : AbstractFactory() {
    override fun build(): ACar = Phev()
}

