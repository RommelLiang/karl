Kotlin在Android中的比重越来越高。已经从Java替代品发展成为一个完整的生态系统，Google也早已宣布支持Kotlin为Android头等支持语言，同时内部项目也在迁移Kotlin。在开发中感触最多的就是Kotlin的代码变的更加随处可见了。Kotlin在带给我们编码的便利性的同时，其“下面”也并不完全和“水面上”一样都是平静的。其中可能一不小心就会掉入性能开销的漩涡中。本篇文章的目的就是分享一下我在项目中遇到过的常见的隐形性能开销的坑。

### lazy
lazy()函数是接受一个lambda并返回一个实例的函数，它提供一种基于委托的[延迟属性](https://www.kotlincn.net/docs/reference/delegated-properties.html#%E6%A0%87%E5%87%86%E5%A7%94%E6%89%98)。官方对它的解释如下：

> 返回的实例可以作为实现延迟属性的委托： 第一次调用 get() 会执行已传递给 lazy() 的 lambda 表达式并记录结果， 后续调用 get() 只是返回记录的结果。

lazy()为我们提供了一种更加便捷的懒加载方式：

```
private val valueLazy by lazy {
    "Lazy init"
}
```

然而，这看似便捷的操作，却可能带来不小的性能损耗。lazy提供了多个重载方法：

![](https://github.com/RommelLiang/karl/blob/main/kotlin_patterns/image/lazy_function.jpg?raw=true)

其中，除非特别指明，否则LazyThreadSafetyMode都默认为`LazyThreadSafetyMode.SYNCHRONIZED`——这就是问题的关键所在。

我们首先看一下lazy()的最终实现：

```
private class SynchronizedLazyImpl<out T>(initializer: () -> T, lock: Any? = null) : Lazy<T>, Serializable {
    private var initializer: (() -> T)? = initializer
    @Volatile private var _value: Any? = UNINITIALIZED_VALUE
    // final field is required to enable safe publication of constructed instance
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

代码逻辑很简单，就是通过一个典型的懒汉式单例。这会有什么额外的性能开销呢？注意其中get方法的`synchronized`关键字——如果你的代码逻辑只在单线程中运行（例如，只在UI主线程中）。那么它就会带来不必要的线程同步开销。而Kotlin的作者显然也知道这一点。Kotlin提供了三种`LazyThreadSafetyMode`：

* LazyThreadSafetyMode.SYNCHRONIZED：双重检测单例，且借助synchronized关键字实现线程安全。
* LazyThreadSafetyMode.PUBLICATION：允许多个线程同时执行，但只有第一个初始化的值会被当做最终值，并返回给所有调用者使用。借助原子属性实现。
* LazyThreadSafetyMode.NONE:不做任何线程安全的操作

所以，在使用lazy函数实现延迟属性的时候。如果确保变量不会涉及到多线程操作，应采取类似下面的调用方式，以避免不必要的线程同步开销：

```
private val valueLazy by lazy(LazyThreadSafetyMode.NONE) {
	"Lazy init"
}
```

### Lambda表达式

我在Android中有的第一个Lambda表达式在setOnClick：

```
button.setOnClickListener {
	printlin("按钮被点击了")
}
```

Kotlin将函数提升为了"一等公民"，可以将它们赋值给变量或者作为参数传递：

```
val funcValue = { i: Int -> i + 1 }

fun func(i: Int):Int {
    return  i + 1
}

fun method(i:Int,func:(Int)->Int){
    func(i)
}

fun execute(){
    method(1,funcValue)
    method(1,::func)
}
```

然而，Java JVM7之后才开始支持lambda表达式。再此之前，lambdas和匿名函数都会被编译为Function对象。

```
class Comp {
    private fun change(name: String, func: (String) -> Unit) {
        func(name)
    }

    fun executeCapturing() {
        var i = 2
        change("捕获式Lambda") {
            println(it)
            println(i)
        }
    }
}
```

例如，上面的代码通过字节码转换为Java代码如下：

```
public final class Comp {
   private final void change(String name, Function1 func) {
      func.invoke(name);
   }

   public final void executeCapturing() {
      final IntRef i = new IntRef();
      i.element = 2;
      this.change("捕获式Lambda", (Function1)(new Function1() {
        public Object invoke(Object var1) {
            this.invoke((String)var1);
            return Unit.INSTANCE;
         }
         
         public final void invoke(@NotNull String it) {
            Intrinsics.checkNotNullParameter(it, "it");
            boolean var2 = false;
            System.out.println(it);
            int var4 = i.element;
            boolean var3 = false;
            System.out.println(var4);
         }
      }));
   }
}
```

。在最终的dex文件中，lambda实际上会通过Function匿名内部类的方式实现——这势必会带来一些性能开销。幸运的是，并不总是如此：

#### 非捕获式Lambda

对于捕获式Lambda表达式，Lambda在作为参数时，每次传递都会创建新的对象实例
对于非捕获式Lambda表达式，采用的是可服用的单例函数的实例。

> [捕获和非捕获的Lambda表达式](https://blog.csdn.net/doctor_who2004/article/details/46734263)
> 当Lambda表达式访问一个定义在Lambda表达式体外的非静态变量或者对象时，这个Lambda表达式称为“捕获的”。

对比一下两个函数：

```
fun executeCapturing() {
    var i = 2
    change("捕获式Lambda") {
        println(it)
        println(i)
    }
}

fun executeNonCapturing() {
    change("非捕获式Lambda表达式") {
        var i = 2
        println(it)
        println(i)
    }
}
```

它们的的字节码转换为Java之后的代码如下：

```
public final void executeCapturing() {
    final IntRef i = new IntRef();
    i.element = 2;
    this.change("捕获式Lambda", (Function1)(new Function1() {
        // $FF: synthetic method
        // $FF: bridge method
        public Object invoke(Object var1) {
            this.invoke((String)var1);
            return Unit.INSTANCE;
        }

        public final void invoke(@NotNull String it) {
            Intrinsics.checkNotNullParameter(it, "it");
            boolean var2 = false;
            System.out.println(it);
            int var4 = i.element;
            boolean var3 = false;
            System.out.println(var4);
        }
    }));
}

public final void executeNonCapturing() {
    this.change("非捕获式Lambda表达式", (Function1)null.INSTANCE);
}
```

#### inline内联

我们当然不能寄希望于所有的Lambda都是非捕获式的。而对于非捕获式的还有另外一种解决方案，就是内联。对上文中的change函数使用inline关键字修饰：

```
private inline fun change(name: String, func: (String) -> Unit) {
    func(name)
}
```

executeCapturing和executeNonCapturing字节码转换为Java后的代码如下：

```
public final void executeCapturing() {
    int i = 2;
    String name$iv = "捕获式Lambda";
    int $i$f$change = false;
    int var6 = false;
    boolean var7 = false;
    System.out.println(name$iv);
    boolean var8 = false;
    System.out.println(i);
}

public final void executeNonCapturing() {
    String name$iv = "非捕获式Lambda表达式";
    int $i$f$change = false;
    int var5 = false;
    int i = 2;
    boolean var7 = false;
    System.out.println(name$iv);
    var7 = false;
    System.out.println(i);
}
```

内联的本质看起来就像是：把要调用的代码粘贴复制到调用处，事实上也是如此。然而，对于非高阶函数，内联所提升的性能几乎是微不足道。能发挥最大价值的地方还是在函数作为参数的函数中，也就是[高阶函数](https://juejin.cn/post/7065982112767148068)中。最典型的例子：Kotlin内置的高阶函数[let、with、run、also和apply](https://juejin.cn/post/7065982112767148068#heading-6)等，都被inline关键字所修饰。

需要注意的是：如果采用`::`的方式进行函数传参，无论是否引用外部变量，参数都将被当做Function对象处理。例如下面代码：

```
fun funcCapturing(value:String){
    println(value)
}

fun execute(){
    change("funcCapturing",::funcCapturing)
}
```

即使funcCapturing没有引用外部变量，它仍会被JVM当做类处理。除非你使用内联。

最后，附上一篇其他人的扩展阅读：[inline，包治百病的性能良药？](https://juejin.cn/post/6844904201353429006)

### 局部函数
Kotlin函数中也可以声明函数，这就是局部函数：

```
fun out() {
    var a = 1

    fun innerOne(i: Int) = i + 1
    fun innerTwo(i: Int) = i + a

    val innerOneNum = innerOne(2)
    println(innerOneNum)

    val innerTwoNum = innerTwo(2)
    println(innerTwoNum)
}
```

上面代码中定义了两个局部函数innerOne和innerTwo。它们的逻辑都是对传入的Int值执行加一操作。唯一不同的是前者直接使用+1，后置借助局部函数外的变量a。这就导致了它们的实现不再相同。字节码转换为Java代码如下：

```
final IntRef a = new IntRef();
a.element = 1;
//innerOne函数
<undefinedtype> $fun$innerOne$1 = null.INSTANCE;

//innerTwo函数
<undefinedtype> $fun$innerTwo$2 = new Function1() {
    // $FF: synthetic method
    // $FF: bridge method
    public Object invoke(Object var1) {
        return this.invoke(((Number)var1).intValue());
    }

    public final int invoke(int i) {
        return i + a.element;
    }
};

int innerOneNum = $fun$innerOne$1.invoke(2);
boolean var4 = false;
System.out.println(innerOneNum);
int innerTwoNum = $fun$innerTwo$2.invoke(2);
boolean var5 = false;
System.out.println(innerTwoNum);
```

可见，局部函数和lambda表达式类似：如果函数内引用了外部变量，就是借助类实现的，否则复用单例函数实例。

不幸的是，局部函数不支持内联！这种情况下捕获式局部函数（我自己类比lambda发明的名词）没有任何可优化的方法避免性能开销。所以，使用局部函数时，要慎重考虑。尽量不要在函数体内引用外部变量。


### vararg可变数量参数

Kotlin也允许可变数量参数的函数，和Java一样，这些参数会被编译为指定类型的数组：

```
fun numbers(vararg nums:Int){
    nums.forEach {
        println(it)
    }
}
```

有三种调用方式：多个参数；单个数组；数组加参数混合：

```
numbers(1,2,3,4)

val intArrayOf = intArrayOf(1, 2, 3, 4)
numbers(*intArrayOf)

numbers(*intArrayOfTwo, 4, 5, 6)
numbers(*intArrayOfTwo,4,5,6)
```

将字节码转换为Java代码如下：

```
Comp comp = new Comp();
//多个参数
comp.numbers(new int[]{1, 2, 3, 4});

//单个数组
int[] intArrayOf = new int[]{1, 2, 3, 4};
comp.numbers(Arrays.copyOf(intArrayOf, intArrayOf.length));

//数组加单个参数混合
Iint[] intArrayOfTwo = new int[]{1, 2, 3, 4};
IntSpreadBuilder var10001 = new IntSpreadBuilder(4);
var10001.addSpread(intArrayOfTwo);
var10001.add(4);
var10001.add(5);
var10001.add(6);
comp.numbers(var10001.toArray());
```

对于传递多个参数，Kotlin最终的实现和Java别无二致，都是将多个参数转换为新数组。这里性能当然会受到到数组的创建和初始化的影响，然而，已经没什么办法了。

在当使用数组的传递时，会首先通过Arrays.copyOf复制当前数据内容并创建新的数组再进行传参。这里发生了不同：

```
int[] nums = {1, 2, 3, 4};
new Varargs().numbers(nums);
for (int num : nums) {
    System.out.print(num);
}

public class Varargs {
    public void numbers(int[] args) {
        args[0] = 0;
    }
}
//Java代码输出为：0234


KotlinVararg().numbers(*nums)
nums.forEach {
    print(it)
}

class KotlinVararg {

    public fun numbers(vararg args:Int) {
        args[0] = 0
    }
}

//Kotlin代码输出为：1234
```

Java和Kotlin在这里发生了分歧，Kotlin不像Java那样直接传递数组的引用。而是对数组进行了复制——这带来两个效果：被调用者对数据进行的操作不会影响调用处的原始数据，于此同时，带来了新的性能开销。

而数组和当参数混合的传递方式，也符合这一准则——会创建先的数据，并且依次将单个参数或者数组里的数据。只不过这里的开销更大了，不仅需要创建新的数组，还需要一个对象生成器来填充数据。

### 数组

#### arrayOf
Kotlin提供了arrayOf可以方便我们创建任意类型的数组，然而当类型为原始类形时却往往让我们得不偿失。观察下面的代码：

```
val intOne = arrayOf(1, 2, 3)
val intTwo = intArrayOf(1, 2, 3)

val doubleOne = arrayOf(1.0,2.0,3.0)
val doubleTwo = doubleArrayOf(1.0,2.0,3.0)

val longOne = arrayOf(1L,2L,3L)
val longTwo = longArrayOf(1,2,3)

val charOne = arrayOf('a', 'b', 'c')
val charTwo = charArrayOf('a', 'b', 'c')
```

通过字节码转换为Java代码后，如下：

```
Integer[] var10000 = new Integer[]{1, 2, 3};
int[] var9 = new int[]{1, 2, 3};

Double[] var10 = new Double[]{1.0D, 2.0D, 3.0D};
double[] var11 = new double[]{1.0D, 2.0D, 3.0D};

Long[] var12 = new Long[]{1L, 2L, 3L};
long[] var13 = new long[]{1L, 2L, 3L};

Character[] var14 = new Character[]{'a', 'b', 'c'};
char[] var15 = new char[]{'a', 'b', 'c'};
```

arrayOf会对基本类型进行装箱，使用它们的包装类进行数组创建——每当你使用arrayOf创建数组时：原始类型byte、 short、char、int、long、float、double和boolean 最终会被装箱为Byte、Short、Character、Integer、Long、Float、Double和Boolean。

幸运的是，Kotlin为我们提供了避免装箱和拆箱操作的数据创建方式：`intArrayOf`、`doubleArrayOf`等

#### Array

自动装箱的问题同样会出现在Arrayz上，对比下面的代码：

```
val arrayInteger = Array(3) { 1;2;3 }
val arrayInt = IntArray(3) { 1;2;3 }
```

都是创建整型数组，但是对应的字节码实现却截然不同：

```
byte var10 = 3;
Integer[] var11 = new Integer[var10];

for(int var12 = 0; var12 < var10; ++var12) {
    int var14 = false;
    Integer var18 = 3;
    var11[var12] = var18;
}

byte var19 = 3;
int[] var20 = new int[var19];

for(int var13 = 0; var13 < var19; ++var13) {
    int var15 = false;
    byte var28 = 3;
    var20[var13] = var28;
}
```

Array总会自作主张的替我们执行装箱操作。

所以在创建数组的时候，一定要避免使用arrayOf和Array的方式。如果你需要一个基本数据类型的数组，最好使用类似`intArrayOf`和`IntArray`的方式创建。






