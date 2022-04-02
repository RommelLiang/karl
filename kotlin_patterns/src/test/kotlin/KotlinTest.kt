import org.junit.Test

class KotlinTest {

    @Test
    fun testSingleton(){
        JavaStaticSingleton.INSTANCE.mKey = "KARL"
        println("--------${JavaStaticSingleton.INSTANCE.mKey}")

        KotlinStaticSingleton.mKey = "KARL"
        println("--------${KotlinStaticSingleton.mKey}")


    }
}