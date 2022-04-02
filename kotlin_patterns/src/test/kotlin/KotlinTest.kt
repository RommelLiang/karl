import org.junit.Test

class KotlinTest {

    @Test
    fun testSingleton(){
        JavaStaticSingleton.INSTANCE.mKey = "KARL"
        println("--------${JavaStaticSingleton.INSTANCE.mKey}")

        KotlinStaticSingleton.mKey = "KARL"
        println("--------${KotlinStaticSingleton.mKey}")

        val phev2: Phev = KCar.Factory(KType.PHEV) as Phev
        println("--------${phev2.replenishingEnergy()}")

    }
}