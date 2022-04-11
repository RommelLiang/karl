# 探索Kotlin语言特性下常用设计模式的实现

随着Kotlin在项目中的大量应用，它的相比于Java新增的高阶函数、委托机制和空安全等特性大大提高了开发效率。不仅仅降低了代码量，也使得代码逻辑更加清晰明了。同时，也愈发感觉到在Kotlin下，一些设计模式的实现方式有了巨大的变化。虽然Kotlin和Java具有互操作性，可以将Java“直译”为Kotlin。但是，有些设计模式在用Kotlin实现的时候有了很大“简化”和便利性。更有甚者，直接在语言层面就被实现了，根本无需自己去设计。接下来我们就看看一些常用的设计模式用Kotlin实现会是什么样子，也和Java下的实现进行对比。这样不仅可以加深对这些常用设计模式的理解和认识，也能了解一下Koltin的一些语言特性以及这些特性对设计模式的实现带来的影响。

### 创建型模式
#### 单例模式 Singleton
单例模式可以说是整个设计中最简单的模式之⼀。在开发中经常会遇到这样的场景：需要保证⼀个类只有⼀个实例，并需要提供⼀个全局访问此实例的方式。Android中的`LayoutInflater`就采用了单例模式

以最常简单的静态单例为例，它在Java中的实现如下：

```
public class JavaStaticSingleton {
    public static final JavaStaticSingleton INSTANCE = new JavaStaticSingleton();

    private JavaStaticSingleton() {
    }

    public String mKey = "";

    //other ...
}
```

通过私有构造函数避免外部创建实例，然后提供静态常量作为访问入口。

而在Kotlin中，实现方法就简单的多了，直接使用`object`关键字：

```
object KotlinStaticSingleton {
    var mKey: String? = null
}
```

如果你你将Kotlin生成的字节码转换成Java文件，它的内容如下：

```
public final class KotlinStaticSingleton {
   @Nullable
   private static String mKey;
   public static final KotlinStaticSingleton INSTANCE;

   @Nullable
   public final String getMKey() {
      return mKey;
   }

   public final void setMKey(@Nullable String var1) {
      mKey = var1;
   }

   private KotlinStaticSingleton() {
   }

   static {
      KotlinStaticSingleton var0 = new KotlinStaticSingleton();
      INSTANCE = var0;
   }
}

```

这里不难看出object的关键字的作用：它会自动创建出 这个类以及它的一个单例。而且是以“饿汉模式”创建。

而对于另外一个同时兼顾线程安全和性能的双重锁校验(的单例模式，Java中对应的实现如下：

```
public class JavaDoubleCheck {
    private volatile static JavaDoubleCheck singleton;

    private JavaDoubleCheck() {
    }

    public static JavaDoubleCheck getSingleton() {
        if (singleton == null) {
            synchronized (JavaDoubleCheck.class) {
                if (singleton == null) {
                    singleton = new JavaDoubleCheck();
                }
            }
        }
        return singleton;
    }
}
```

而在Kotlin中提供了更加便捷的实现：

```
val name:String by lazy { "Karl" }
```

`lazy`函数，它是一个[高阶函数](https://juejin.cn/post/7065982112767148068)：

```
public actual fun <T> lazy(initializer: () -> T): Lazy<T> = SynchronizedLazyImpl(initializer)
```

至于`SynchronizedLazyImpl`的具体实现，你看了一定会觉得很面熟

```

private class SynchronizedLazyImpl<out T>(initializer: () -> T, lock: Any? = null) : Lazy<T>, Serializable {
    private var initializer: (() -> T)? = initializer
    @Volatile private var _value: Any? = UNINITIALIZED_VALUE
    private val lock = lock ?: this

    override val value: T
        get() {
            val _v1 = _value
            if (_v1 !== UNINITIALIZED_VALUE) {
                @Suppress("UNCHECKED_CAST")
                return _v1 as T
            }

            return synchronized(lock) {
                val _v2 = _value
                if (_v2 !== UNINITIALIZED_VALUE) {
                    @Suppress("UNCHECKED_CAST") (_v2 as T)
                } else {
                    val typedValue = initializer!!()
                    _value = typedValue
                    initializer = null
                    typedValue
                }
            }
        }

    override fun isInitialized(): Boolean = _value !== UNINITIALIZED_VALUE

    override fun toString(): String = if (isInitialized()) value.toString() else "Lazy value not initialized yet."

    private fun writeReplace(): Any = InitializedLazyImpl(value)
}
```

几乎就是Java版本的直翻。但却使得Kotlin在语言特性上支持了这种单例的模式的实现，无需使用者再去写繁琐的代码。

这里需要注意一下：`lazy`函数默认是线程安全的，而且是通过加锁实现的。如果你的变量不会涉及到多线程，那么请务必使用`LazyThreadSafetyMode.NONE`参数，避免不必要的性能开销。

```
val name:String by lazy(LazyThreadSafetyMode.NONE) { "Karl" }
```

`lazy`函数有几种不同的重载，其中接受`LazyThreadSafetyMode.NONE`参数的函数最终使用`UnsafeLazyImpl`实现单例，它和`SynchronizedLazyImpl`逻辑基本一致，唯一的区别是它不是线程安全的：

```
internal class UnsafeLazyImpl<out T>(initializer: () -> T) : Lazy<T>, Serializable {
    private var initializer: (() -> T)? = initializer
    private var _value: Any? = UNINITIALIZED_VALUE

    override val value: T
        get() {
            if (_value === UNINITIALIZED_VALUE) {
                _value = initializer!!()
                initializer = null
            }
            @Suppress("UNCHECKED_CAST")
            return _value as T
        }

    override fun isInitialized(): Boolean = _value !== UNINITIALIZED_VALUE

    override fun toString(): String = if (isInitialized()) value.toString() else "Lazy value not initialized yet."

    private fun writeReplace(): Any = InitializedLazyImpl(value)
}

```

所以**在使用lazy函数的时候，一定要考虑好你所声明的变量是否涉及到多线程。否则的话，一定要传入`LazyThreadSafetyMode.NONE`参数，避免不必要的加锁带来的性能开销**。


#### 建造者模式 Builder

建造者模式所完成的内容就是通过将多个简单对象通过⼀步步的组装构建出⼀个复杂对象的过程，且构建的过程是一条链式调用，逻辑简单清晰。当⼀些基本信息不会变，⽽其组合经常变化的时候 ，就可以选择这样的设计模式来构建代码。例如，在Android中，AlertDialog和Retrofit的构建，都是借助建造者模式实现的。Builder可以避免臃肿的构造函数参数列表。试想一下你有如下类需要初始化：

```
public class JavaBuilderUser {
    private String name;
    private int age;
    private String address;
    private String gender;
    private boolean isMarried;
    private String education;
    private String nationality;
    private String belief;
    private String phone;

    public JavaBuilderUser(String name, int age, String address, String gender, boolean isMarried, String education, String nationality, String belief, String phone) {
        this.name = name;
        this.age = age;
        this.address = address;
        this.gender = gender;
        this.isMarried = isMarried;
        this.education = education;
        this.nationality = nationality;
        this.belief = belief;
        this.phone = phone;
    }
}
```

如此多的成员变量组成的构造函数在进行初始化时简直是一种噩梦。你需要通过如下方式去初始化：

```
new JavaBuilderUser("Karl", 18, "sz", "man", false, "本科", "CN", "康米","123");
```

这么多的参数不仅降低了代码的可读性，更可怕的是它可能导致错误的传参引入BUG。我们可以用如下办法对下进行优化：

```
public class JavaBuilderUser {
  
    //...

    public static final class Builder{
        private String name;
        private int age;
        private String address;

        public Builder() {

        }

        public Builder buildName(String name){
            this.name = name;
            return this;
        }

        public Builder buildAddress(String address){
            this.address = address;
            return this;
        }

        public Builder buildAge(int age){
            this.age = age;
            return this;
        }

        public JavaBuilderUser build(){
            return new JavaBuilderUser(name, age, address, "man", false, "本科", "CN", "康米","123");
        }
    }
}
```

通过定义一个新的类作为建造者去构建我们的User。上面代码里展示的一种静态内部类的实现方式（为了缩减篇幅我省略掉了好多成员），接下来就可以用如下方法实现User的创建了：

```
new JavaBuilderUser.Builder().buildName("karl").buildAddress("SZ").buildAge(10).build();
```

而在Kotlin中，具名可选参数的出现使得一切都变的简单了。“具名”使得我们再也不需要为了区分臃肿的构造函数参数列表而引入新的类了。同时，“可选”使得我们无需实参和形参一一对应。我们可以通过为形参设置默认值来标记参数为可选参数，这样在你进行函数调用时就不会强制要求你传入对应的实参了。当你需要传入可选参数的实参时，你只需要显式的标明它的参数名并传入实参即可。当然，无论是可选参数还是必需参数，都不会强制你在传参时显式的标记它的参数名。如果你没有指定参数名，那么你的入参顺序必需和形参顺序保持一直。

```
class KotlinUserBuilder(
    val name: String,
    val age: Int,
    var address: String? = null,
    var gender: String? = null,
    var isMarried: Boolean = false,
    var education: String = "",
    var nationality: String = "CN"
) {}
```

上面的代码中，除了name和age是必需参数外，其它都是可选参数。我们可以这样使用它：

```
val user1 = KotlinUserBuilder("Karl",18)
val user2 = KotlinUserBuilder(age = 18,name = "Karl")
val user3 = KotlinUserBuilder(name = "Karl",age = 18)
val user4 = KotlinUserBuilder("Karl",18,address = "SZ")
val user5 = KotlinUserBuilder("Karl",18,nationality = "CN",address = "SZ")
val user6 = KotlinUserBuilder("Karl",18,"SZ","男")R
```

一个构造函数居然可以有这么多种调用方式！不仅可以有不同的参数长度，甚至连它们的顺序都可以随心所以的自由调整。

当然，不仅仅有具名可选参数这一种方法。也可以借助高阶函数来实现，比如下面的代码就是借助apply实现类似建造者模式的效果：

```
val user7 = KotlinUserBuilder("Karl",18).apply {
    address = "SZ"
    gender = "男"
    nationality = "CN"
}
```

另外，我们还可以借助函数替代类，提前定义好创建同一类型实例的函数。比如定义一个创建深圳男性User的扩展函数：

```
val szMeal: KotlinUserBuilder.() -> Unit = {
    address = "SZ"
    gender = "男"
    nationality = "CN"
}

val user8 = KotlinUserBuilder("Karl", 18).szMeal()
```

#### 原型模式

原型模式主要解决的问题就是创建重复对象，⽽这部分对象内容可能本身⽐较复杂，后者⽣成过程比较繁琐，因此采⽤克隆的⽅式节省时间。它的实现也很简单：

```
public class JavaPrototype implements  Cloneable {
    private String name;
    private int age;
    private String address;
    private String gender;

    public JavaPrototype(String name, int age, String address, String gender) {
        this.name = name;
        this.age = age;
        this.address = address;
        this.gender = gender;
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        Object clone = null;
        try {
            clone = super.clone();
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }
        return clone;
    }
}
```

只需要我们实现Cloneable并重写clone方法即可，使用也很简单：

```
JavaPrototype ontology = new JavaPrototype("Karl",18,"","");
JavaPrototype clone = (JavaPrototype) ontology.clone();
```

Android中的Intent就实现了Cloneable接口，但是它的clone却是通过new直接创建了一个新的实例。

虽然clone的方式有很多缺陷，但今天我们的终点不在这里。我们来看一下Kotlin如何实现clone。Kotlin新增了data关键字，它自身就支持clone操作：

```
data class KotlinPrototype(val name:String,val age:Int,val address:String,val gender:String)

val ontology = KotlinPrototype("Karl", 18,"SZ","MEAl")
val copy1 = ontology.copy()
val copy2 = ontology.copy(address = "BJ")
```

它自己内置了copy函数，并且它同样也支持具名可选参数。你可以再clone时随心所欲的调整你所需要的参数。

需要主要的是：**data关键字提供的是一种浅拷贝机制。**

####  工厂模式 Factory
首先从比较简单的普通工厂模式入手。它定义一个创建对象的接口，让其子类自己决定实例化哪一个工厂类，使其创建过程延迟到子类进行。它避免了创建者与具体的产品逻辑耦合，每⼀个业务逻辑实现都在所属⾃⼰的类中完成。

用Java代码实现如下（为了节省篇幅都是用内部类的方式展现）:

```
public class JavaSimpleFactory {

    public interface Car {
        void replenishingEnergy();
    }

    public class GasCar implements Car {

        @Override
        public void replenishingEnergy() {
            System.out.println("加95");
        }
    }

    public class EV implements Car {

        @Override
        public void replenishingEnergy() {
            System.out.println("充电");
        }
    }

    public class PHEV implements Car {

        @Override
        public void replenishingEnergy() {
            System.out.println("充电和加油");
        }
    }

    public enum Type{
        GAS,
        EV,
        PHEV
    }

    public class CarFactory {
        public Car buildCar(Type type){
            switch (type){
                case GAS: return new GasCar();
                case EV: return new EV();
                case PHEV: return new PHEV();
            }
            return null;
        }
    }
}

```
上面的代码定义了一个Car接口，并有三种车，分别是汽油车、纯电车和插电混动车。用户无需知道车辆是如何创建的，只需要根据枚举传入自己想要的车的类型，就可以拿到自己想要的车：

```
JavaSimpleFactory.GasCar gasCar = (JavaSimpleFactory.GasCar) new JavaSimpleFactory().new CarFactory().buildCar(JavaSimpleFactory.Type.GAS);
JavaSimpleFactory.EV evCar = (JavaSimpleFactory.EV) new JavaSimpleFactory().new CarFactory().buildCar(JavaSimpleFactory.Type.EV);
    }
```

而在Kotlin中，我们借助`companion`关键字和操作符重写可直接在接口定义实现工厂类：

```
interface KCar {
    fun replenishingEnergy()

    companion object Factory {
        operator fun invoke(type: KType) =
            when (type) {
                KType.GAS -> GasCar()
                KType.EV -> EV()
                KType.PHEV -> PHEV()
            }
    }
}
enum class KType {
    GAS, EV, PHEV
}
```

代码一下子就简单了很多，避免了引入新的类型。使用起来也很简单：

```
val gas2: GasCar = KCar.Factory(KType.GAS) as GasCar
val ev2: Ev = KCar.invoke(KType.EV) as Ev
val phev2: PHEV = KCar.Factory(KType.PHEV) as PHEV
```

也可以进一步优化，不要工厂类了，之通过一个函数实现：

```
interface KCar {
    fun replenishingEnergy()
    companion object{
        fun build(type: KType)  = 
            when(type){
                KType.GAS -> GasCar()
                KType.EV -> EV()
                KType.PHEV -> PHEV()
            }
    }
}

```


```
val gas :GasCar= KCar.build(KType.GAS) as GasCar
val ev :Ev= KCar.build(KType.EV) as Ev
val phev : PHEV = KCar.build(KType.PHEV) as PHEV
```

还可以进一步借助内联函数和reified进一步进行优化，这次连枚举都不需要了：

```
interface KCar {
    fun replenishingEnergy()
    companion object Factory {
        inline operator fun <reified T : KCar> invoke() =
            when (T::class) {
                GasCar::class -> GasCar()
                EV::class -> EV()
                PHEV::class -> PHEV()
                else -> throw IllegalArgumentException()
            }

    }
}
```
直接通过类型推断出所需要的类，使用起来也更加简单了：

```
val gas3: GasCar = KCar.Factory<GasCar>() as GasCar
val ev3: EV = KCar.invoke<EV>() as EV
```

内联函数同样也可以应用到抽象工厂模式的简化，使用方式和简单工厂模式基本类似。这里就不展开了，感兴趣的可以查看[相关代码](https://github.com/RommelLiang/karl)

### 行为型模式
#### 模板方法模式 Template Method

模板模式的核⼼在于：使用抽象类中定义抽象⽅法的执⾏顺序，而这些抽象方法在子类中实现。用白话讲就是：定义出方法和它们的执行顺序，但方法的实现交给子类负责。在Android中，Activity的生命周期，AsyncTask以及View的绘制都有模板方法的思想。最常用的就是[Activity的生命周期](https://juejin.cn/post/7064901478934118430)了，我们只需处理各个生命周期里的逻辑即可，无需关系它们的的执行流程。

要在Java中实现模板方法也很简单：只需要定义抽象方法，并定义好它们的执行顺序，剩下的任务交给子类就行了：

```
public class JavaTemplate {

    public void run() {
        new Template().init();
    }

    abstract class TemplateAbstract {
        abstract void first();

        abstract void second();

        abstract void third();

        void init() {
            first();
            second();
            third();
        }
    }

    class Template extends TemplateAbstract {

        @Override
        void first() {
            System.out.println("第一个");
        }

        @Override
        void second() {
            System.out.println("第二个");
        }

        @Override
        void third() {
            System.out.println("第三个");
        }
    }
}
```

而在Kotlin中，我们完全可以借助高阶函数实现对函数执行顺序的约束：

```
class KotlinTemplate {

    fun func1() = println("第一个")
    fun func2() = println("第二个")
    fun func3() = println("第三个")
}

fun initData(first: () -> Unit, second: () -> Unit, third: () -> Unit){
    first()
    second()
    third()
}

fun main() {
    val template = KotlinTemplate()
    initData(template::func1, template::func2, template::func3)
}

```

我们之需要一个顶级函数就能实现对函数执行流程的限制！这一点和策略模式很类似。

#### 策略模式 Strategy
策略模式可用于一个类的行为或其算法可以在运行时更改的场景下。它可以帮助我们定义了一系列的算法，并将每一个算法封装起来，而且使他们还可以相互替换。策略模式让算法独立于使用它的客户而独立变化。Android中的ListAdapter和时间差值器TimeInterpolator都是借助策略模式实现的。我们以采用不同的交通工具上班模拟一下策略模式：

```
public class JavaStrategy {

    public void run() {
        new Works(new Proletariat()).go();
        new Works(new MiddleClass()).go();
        new Works(new Capitalist()).go();
    }
    public interface GoToWork{
        public void toWork();
    }

    public class Proletariat implements GoToWork{

        @Override
        public void toWork() {
            System.out.println("无产阶级只能骑自行车");
        }
    }

    public class MiddleClass implements GoToWork{

        @Override
        public void toWork() {
            System.out.println("中产可以开小汽车");
        }
    }

    public class Capitalist implements GoToWork{

        @Override
        public void toWork() {
            System.out.println("资本家不需要上班，因为他们挂在路灯上");
        }
    }

    public class Works {
        private GoToWork goToWork;

        public Works(GoToWork goToWork) {
            this.goToWork = goToWork;
        }

        public void go(){
            goToWork.toWork();
        }
    }
}

```

上面的代码就通过不同阶层上班的方式模拟了策略模式，使用方式如下。而在Kotlin中，我们依旧可以借助高阶函数对它进行简化：

```
fun main() {
    GoToWork(::proletariat).go()
    GoToWork(::middleClass).go()
    GoToWork(::capitalist).go()
}

class GoToWork(val way: () -> Unit) {
    fun go() {
        way()
    }
}

fun proletariat() {
    println("无产阶级只能骑自行车")
}

fun middleClass() {
    println("中产可以开小汽车")
}

fun capitalist() {
    println("资本家不需要上班，因为他们挂在路灯上")
}
```

和模板方法模式类似，核心都是使用函数去替换类。


#### 迭代器模式 Iterator
迭代器对于Android和Java开发者来说是老熟人了，List等数据结构都内置了迭代器便于我们按顺序的访问其中的各个元素。虽然在实际开发中很少遇到需要我们自己去实现迭代器的场景，但是我们几乎每天都在使用Java内置的迭代器去遍历List集合。（在Java中，虽然for循环可以用来访问数据，但是它不是迭代器模式），迭代器模式的特点是实现 Iterable 接⼝，通过 next 的⽅式获
取集合元素，同时具备对元素的删除等操作（遍历的同事进行删除操作，for循环显然是做不到的）。由于大多数时候我们都无需自己去实现迭代器，所以这里我们就仅仅从使用时的异同来对比一下Java和Kotlin下的迭代器模式。首先看Java下的使用：

```
public class JavaIterable {
    public static void main(String[] args) {
        UserList userList = new UserList(new ArrayList(Arrays.asList("K", "A", "R", "L")));
        while (userList.hasNext()) {
            String next = userList.next();
            System.out.println(next);
            if (next.equals("A")) {
                userList.remove();
            }
        }
        userList.println();
    }

    static class UserList implements Iterator<String>{

        private List<String> names;
        private Iterator<String>  mIterator;

        public UserList(List<String> names) {
            this.names = names;
            this.mIterator = names.iterator();
        }

        @Override
        public boolean hasNext() {
            return mIterator.hasNext();
        }

        @Override
        public String next() {
            return mIterator.next();
        }

        @Override
        public void remove() {
            mIterator.remove();
        }

        public void println(){
            for (String name : names) {
                System.out.print(name);
            }

        }
    }

}
```

通过迭代器开始迭代，而且在迭代过程在可以删除数据。而在Kotlin中，我们可以借助委托机制实现以及运算符重载简化操作：

```
fun main() {
    val delegateUserList = DelegateUserList(arrayListOf("K", "A", "R", "L"))
    val operatorUserList = OperatorUserList(arrayListOf("K", "A", "R", "L"))
    val mutableIteratorUserList = MutableIteratorUserList(arrayListOf("K", "A", "R", "L"))

    println("委托机制：")
    for (name in delegateUserList) {
        print(name)
    }
    println("\n运算符重载机制：")
    for (name in operatorUserList) {
        print(name)
    }

    println("\nMutableIterator：")
    for (name in mutableIteratorUserList) {
        print(name)
        if (name == "K") mutableIteratorUserList.remove()
    }

    mutableIteratorUserList.printlnElement()


}

/**
 * 委托机制实现
 * */
class DelegateUserList(private val names: List<String>) : Iterator<String> by names.iterator()

/**
* 运算符重载
* */
class OperatorUserList(private val names: List<String>) {
    operator fun iterator(): Iterator<String> = names.iterator()
}

class MutableIteratorUserList(private val names: List<String>):MutableIterator<String> by names.iterator() as MutableIterator<String> {
    fun printlnElement(){
        println("\n$names")
    }
}
```

我们几乎不需要去实现任何东西了！需要注意的是：Kotlin中的Iterator取消了remove方法，转而在它的子类MutableIterator中定义。

### 结构型模式

#### 装饰器模式 Decorator
装饰器的核⼼就是再不改原有类的基础上给类新增功能（这简直就是扩展函数的别称啊！！！）。在Android中，上下文Context的实现就是一个装饰器模式。我们借助给汽车扩展充电功能的例子用Java实现一下：

```
public class JavaDecorator {

    public void test() {
        new Phev(new Car()).refuel();
    }

    interface Oil {
        public void refuel();
    }

    class Car implements Oil {
        @Override
        public void refuel() {
            System.out.println("98加满");
        }
    }

    abstract class CarDecorator implements Oil {
        private Car car;

        public CarDecorator(Car car) {
            this.car = car;
        }

        @Override
        public void refuel() {
            car.refuel();
        }
    }

    class Phev extends CarDecorator {

        public Phev(Car car) {
            super(car);
        }

        @Override
        public void refuel() {
            super.refuel();
            System.out.println("再去冲一个小时的电");

        }
    }
}
```

实现起来并不复杂，记下来就看Kotlin的实现。我们可以借助扩展函数将那些类替换掉：

```
fun main() {
    Car().phevRefuel()
    Car().refuel()

}

interface Oil{
    fun refuel()
}

class Car:Oil{
    override fun refuel() {
        println("98加满")
    }

}

fun Car.phevRefuel(){
    this.refuel()
    println("开始充电")
}

/**
 * 同名扩展函数*/
fun Car.refuel(){
    println("我不会被执行，因为永远会以成员函数优先")
}
```

使用Kotlin扩展函数之后，代码简化了好多，但需要注意扩展函数的一些特性。首先看上面的代码执行结果如下：

```
98加满
开始充电
98加满
```

同名扩展函数`Car.refuel`永远不会被调用，因为在重名的情况下，成员函数的优先级要高于扩展函数。

### 尾声
可以看到Kotlin的语言特性和语法糖带来的不仅是“敲代码”阶段的改变，更深刻的影响了设计模式的实现。这篇文章与其说是在讲设计模式，不如说是在帮Kotlin“秀”操作。然而这仅仅是我在开发中遇到的一些设计模式，并将结合我所掌握的Kotlin语言的一部分特性相结合的结果。还存在着很多其他的设计模式，以及一些我还没那么了解的Kotlin语言特性，还需要不断的探索尝试。希望这篇文章能起到抛砖引玉的作用，大家集思广益，发掘出Kotlin更多的特性极其应用。

