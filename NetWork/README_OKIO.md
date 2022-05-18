### 简介
> 对语言设计人员来说，创建好的输入/输出系统是一项特别困难的任务。

[Okio官方地址](https://square.github.io/okio/)

Okio是java.io和java.nio的增强补充库，使访问、存储和处理数据更加容易。它一开始是[OkHttp](https://github.com/square/okhttp)的一个组件。接下来我们就基于[2.2.2版本](https://github.com/square/okio/releases/tag/2.2.2)窥探一下Okio的基本原理。

开发中有三种不同的种类的IO：文件、控制台、网络连接。而且往往需要大量不同的方式与它们进行通讯（顺序、随机访问、二进制、字符、按行、按字节等等）。由于存在大量不同的设计方案，覆盖所有可能性因素的困难性是显而易见的。Java库通过大量的类来攻克这个难题。因此，如此多的类导致IO系统，产生令人无奈的复杂代码。

例如，如果我们像读取一个文件，流程如下：

```
fun ioRead() {
    var inputStream: FileInputStream? = null
    try {
        val file = File(path)//创建文件
        inputStream = FileInputStream(file)//创建输入流
        val readBytes = inputStream.readBytes()//读取数据
        val text = String(readBytes, Charsets.UTF_8)//获取文本信息
        println(text)
    } catch (e: IOException) {
        e.printStackTrace()
    } finally {
        inputStream?.close()//关闭输入流
    }
}
```

接下来，我们看一下Okio的优势：
### 优势

首先要知道，Okio并不是用来替代IO体系的，其本质上也是一IO为核心的，只不是进行了升级优化。它相比于IO有以下三个显著的优点：

1. 精简且全面的Api
2. 性能高效的缓存处理功能
3. 超时机制

接下来我们逐条分析，顺便熟悉一下Okio的基本使用方法。

首先是简洁全面的Api——就是使用方便。首先看上文中的文本内容读取，借助Okio实现相同功能，代码如下：

```
fun okIoRead() {
    var source: Source? = null
    try {
        val file = File(path)//创建文件
        source = Okio.source(file)//创建source
        val buffer = Okio.buffer(source)//创建buffer
        val readUtf8 = buffer.readUtf8()//读取文本
        println(readUtf8)
    } catch (e: IOException) {
        e.printStackTrace()
    } finally {
        source?.close()
    }
}
```

看着并没有太简单。实际上，上面的代码是会报错的：Okio.source这种Okio.xxx方法已经被废弃，okio已经全面使用kotlin扩展函数实现了类似功能。采用扩展函数后，代码如下：

```
fun okIoRead() {
    var source: Source? = null
    try {
        val file = File(path)//创建文件
        source = file.source().apply {
            println(buffer().readUtf8())
        }
    } catch (e: IOException) {
        e.printStackTrace()
    } finally {
        source?.close()
    }
}
```

实际上，用这段代码做例子说明Okio的简洁对IO来说并不公平——因为这里面的简洁至少有一半是Kotlin的功劳！然而，okio的简洁远不止于此。

首先，它提供了一种链式调用（当然，你也可以为IO写扩展方法，这也是okio提供的功能之一），读取文本的代码可以从File起而终于String，代码如下（为了缩减代码篇幅，之后的IO操作都不再书写捕获异常的代码）：

```
fun chainExecution(){
    val readUtf8 = File(path).source().buffer().readUtf8()
}
```

当然，这里面也有Kotlin高阶函数的功劳。但是，`Source`和`Sink`接口的出现，完全解放了我们投入在InputStream和OutputStream的劳动力——前者要用来读取数据，后者写数据。而且，我们不再需要关心数据源了。无论是内存、磁盘还是网络，这两个接口都已经为我们处理好了各种各样的情况。于是我们的代码可以像下面这个样子了：

```
fun chainExecution(){
    val readUtf8 = File(path).source().buffer().readUtf8()
    val readUtf81 = Socket().source().buffer().readUtf8()
    val readUtf82 = FileInputStream(File(path)).source().buffer().readUtf8()

    File(path).sink().buffer().writeUtf8("文本信息")
    Socket().sink().buffer().writeUtf8("文本信息")
    FileOutputStream(File(path)).sink().buffer().writeUtf8("文本信息")
}
```

你再也不用去创建对应的输入输出流了，只需要拿着“数据源”一股脑的向下调用就行了！

还没完：如果你需要读取的数据是由大于一个字节的java基础类型构成，如int、 long、Boolean、double等。在IO中，DataInputStream可以帮助你让InputStream读取Java基本类型来代替原始的字节。你只需要这样做：

```
val dataInputStream = DataInputStream(
        FileInputStream(File(pathInt))
        )
val num = dataInputStream.readInt()
println("$num")
```

使用`DataInputStream `对输入流包装一下即可，输出流也是同理。在OKIO中BufferedSink/BufferedSource就具有以上基本所有的功能，不需要再串上一系列的装饰类：

```
val readInt = File(pathInt).source().buffer().readInt()
println(readInt)
```

接下来看一下超时，我们用下面的代码演示一个Socket通讯：

```
//服务端
fun server() {
    ServerSocket(9090).use { serverSocket ->
        var socket: Socket?
        while (true) {
            socket = serverSocket.accept()
            val source = socket.source()
            val sink = socket.sink()
            println("接收到客户端的消息：${source.buffer().readUtf8()}")
            sink.buffer().writeUtf8("我是服务端").flush()
            socket.shutdownOutput()
        }
    }
}

//客户端
fun timeOut() {
    val socket = Socket("localhost", 9090)
    try {
        socket.sink().buffer().writeUtf8("我是客户端").flush()
        socket.shutdownOutput()
        val source = socket.source()
        println("来自服务端的消息：${source.buffer().readUtf8()}")
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

//output:
//接收到客户端的消息：我是客户端
//来自服务端的消息：我是服务端
```

此时，如果我们把服务端输出的代码去掉，也就是：

```
sink.buffer().writeUtf8("我是服务端").flush()
socket.shutdownOutput()
```

客户端会怎样？答案是一直等下去！此时我们将代码稍加修改：

```
fun timeOut() {
    val socket = Socket("localhost", 9090)
    try {
        socket.sink().buffer().writeUtf8("我是客户端").flush()
        socket.shutdownOutput()
        val source = socket.source()
        source.timeout().timeout(2000,TimeUnit.MILLISECONDS)
        println("来自服务端的消息：${source.buffer().readUtf8()}")
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
```
这样两秒后，就可以在catch捕获到Timeout异常（你当然可以在Socket里设置超时时间，这里使用Socket进行演示只是为了方便。当你在进行文件读写时，这确实是一个不错的选择）

最后说一下性能高效的缓存处理功能这个优点。IO中BufferedOutputStream等缓存流的处理逻辑在Okio看来太过于简单粗暴：每个buffer流中维护一个默认大小8192的数组并自行负责创建和销毁。当缓存满时，将缓存的数据全部输出，然后重新缓存。而且，如果一次操作的数据大于缓存的大小，缓存将不可用！更要命的是：缓存和缓存之间没有任何“沟通交流”，也就是说缓存之间不存在数据交互和交换。例如从一个输入到输出，要经历如下过程：input缓存->临时bty数据->output缓存。数据要拷贝两次。

而okio利用Segment使用双向链表作为缓存结构。并通过数据池的方式管理Segment，防止频繁的创建和销毁，尽可能少的去申请内存，同时也就降低了GC的频率。同时，通过控制流中的head、tail指向代替传统IO中的数据拷贝工作实现数据的转移。

### IO操作源码和流程分析
上面只对Okio的优势做一些简短的介绍，在接下来的源码和流程分析中将进行更加细致的探究。

在开始分析之前，首先看一下Okio的重要构成：

首先是`Source`、`Sink`。这两个接口是Okio中输入流接口和输出流接口，和IO的InputStream、OutputStream对应。**前者定义了read方法用于将当前的数据转移到到传入的Sink的Buffer缓存中，后者定义了write方法用于将数据从传入的Source中转移给自己**。

```
interface Source : Closeable {
  fun read(sink: Buffer, byteCount: Long): Long
  fun timeout(): Timeout
  override fun close()
}

interface Sink : Closeable, Flushable {
  fun write(source: Buffer, byteCount: Long)
  override fun flush()
  fun timeout(): Timeout
  override fun close()
}

```

`Source`、`Sink`只是两个简单的接口。而`BufferedSource`和`BufferedSink`接口则对它们两个提供了丰富的功能扩展。它们分别继承自分别继承自`Source`和`Sink`，提供了一系列方法的读写操作，例如`readUtf8`、`readByteArray`、`writeUtf8`、`writeByte`等等DataInputStream和DataOutputStream具备的基本数据类型读取方法，这里基本都有。当然`BufferedSource`和`BufferedSink`提供的只是扩展功能的定义，具体的实现还是交给了`RealBufferedSource`和`RealBufferedSink`。而`RealBufferedSource`和`RealBufferedSink`的具体实现方法则是将任务交给`Buffer`进行的。

`Buffer`可以说是Okio的核心了，它同时实现了`BufferedSource`和`BufferedSink`。不仅可以用来缓存和管理缓存，同时支持数据转移——不仅能进行读取，又能进行写入数据操作，不同的Buffer也可以进行转移。而这一切的核心，都是基于`Segment`进行的。
`Segment`是Buffer的数据载体，采用双向链表的数据结构，每个载体可存储8K的数据。而`Segment`的创建、销毁和管理则是交由`SegmentPool`控制。

接下来，我们分析一下Okio的基本流程：

#### IO读操作
我们以`file.source().buffer().readUtf8()`为例来看okio的文件读取流程，它等价于Java代码:`Okio.buffer(Okio.source(file)).readUtf8();`。本质上是Kotlin的扩展函数：

```
//Source扩展函数，用于创建BufferedSource
fun Source.buffer(): BufferedSource = RealBufferedSource(this)

//File扩展函数，创建InputStream并调用扩展函数InputStream.source()
fun File.source(): Source = inputStream().source()

//File扩展函数，创建InputStream
public inline fun File.inputStream(): FileInputStream {
    return FileInputStream(this)
}

//InputStream扩展函数，创建Source(InputStreamSource)
fun InputStream.source(): Source = InputStreamSource(this, Timeout())
```

如果你对Kotlin不太熟悉，上面的代码可用如下Java代码展示(实际上由于访问控制权限的问题，下面的代码是无法编译的)：

```
void javaCode() throws Exception {
    File file = new File("");
    FileInputStream fileInputStream = new FileInputStream(file);
    Source inputStreamSource = new InputStreamSource(fileInputStream, new Timeout());
    BufferedSource realBufferedSource = new RealBufferedSource(inputStreamSource);
    realBufferedSource.readUtf8();
}
```

它们的UML关系图大致如下：

![](https://github.com/RommelLiang/karl/blob/main/NetWork/img/input_uml.jpg?raw=true)

明显的一个持有关系：BufferedSource->Source->Stream。而BufferedSource持有Buffer的实例。上面的关键类分析里提到过，BufferedSource对Source进行了功能扩展，实际上都是借助Buffer完成的。接着看`realBufferedSource.readUtf8()`的流程。readUtf8方法在BufferedSource中定义，RealBufferedSource中的实现如下：

```
override fun readUtf8(): String {
    buffer.writeAll(source)
    return buffer.readUtf8()
}
```

首先调用了`buffer.writeAll`，该方法的实现如下：

```
override fun writeAll(source: Source): Long {
    var totalBytesRead = 0L
    while (true) {
          val readCount = source.read(this, Segment.SIZE.toLong())
          if (readCount == -1L) break
          totalBytesRead += readCount
  }
  return totalBytesRead
}
```

代码逻辑很简单，不断的调用`source.read`方法，并累加readCount。直到`source.read`返回-1（大胆猜测一下，这里和inputStream的读取有点像，在不知文件大小的情况下，循环读取文件，当返回值为-1时表示读取结束）。根据上面UML图，我们知道，这里的`source`就是`InputStreamSource`的实例，查看它的`read`方法如下：

```
override fun read(sink: Buffer, byteCount: Long): Long {
    //检查byteCount
    if (byteCount == 0L) return 0
    require(byteCount >= 0) { "byteCount < 0: $byteCount" }
    try {
        timeout.throwIfReached()
        //获取Segment
        val tail = sink.writableSegment(1)
        val maxToCopy = minOf(byteCount, Segment.SIZE - tail.limit).toInt()
        //读取文件
        val bytesRead = input.read(tail.data, tail.limit, maxToCopy)
        if (bytesRead == -1) return -1
        tail.limit += bytesRead
        sink.size += bytesRead
        return bytesRead.toLong()
  } catch (e: AssertionError) {
      if (e.isAndroidGetsocknameError) throw IOException(e)
      throw e
  }
}
```
首先，检查byteCount的大小，这里传入的是`Segment.SIZE.toLong()`，也就是8K。紧接着设置了timeout并从`sink`中获取Segment，不难发现，这个sink就是RealBufferedSource中持有的Buffer。这里我们之关系文件读取流程，timeout、Segment和Buffer我们稍后再讲。不过你需要提前知道：在读数据过程中，Buffer中的Segment负责缓存读取到的数据，作用类似于IO中利用InputSteam读取数据的byte数组。

紧接着也是最关键的`val bytesRead = input.read(tail.data, tail.limit, maxToCopy)`。到这里有用到了IO，input即是上文中`FileInputStream`的实例，read方法就是基本的IO操作了，三个参数分别是数据缓存数组、数据写入缓存数组的起始位置和读取数据的最大字节数。这些都是IO的常规操作。

到这里，情况就变的明朗了：`Okio就是通过Source不断的循环调用InputSream的read方法，并且将数据缓存到Buffer中，从而实现文件的读操作`。

接下来的`buffer.readUtf8()`方法逻辑也就能猜到了——读取buffer缓存并转成Utf8：

```
override fun readUtf8() = readString(size, Charsets.UTF_8)

fun readString(byteCount: Long, charset: Charset): String {
    require(byteCount >= 0 && byteCount <= Integer.MAX_VALUE) { "byteCount: $byteCount" }
    if (size < byteCount) throw EOFException()
    if (byteCount == 0L) return ""
    //此处的head即是Buffer中的Segment
    val s = head!!
    if (s.pos + byteCount > s.limit) {
        return String(readByteArray(byteCount), charset)
    }
    val result = String(s.data, s.pos, byteCount.toInt(), charset)
    s.pos += byteCount.toInt()
    size -= byteCount
    if (s.pos == s.limit) {
        head = s.pop()
        SegmentPool.recycle(s)
    }
    return result
}
```
这里的Buffer和Segment缓存处理我们稍后再讲，重点在Okio的读取流程。Okio还提供了多种不同的的读取方法。例如：`readByte()、readLong()、readByteArray()
`等，设置还有`indexOf()`这样的操作。虽然他们的返回结果不同，但是基本思路和readUtf8原理是一致的，细微的差别在于读取的数据量不同。以`readByte`为例，其源码如下：

```
override fun readByte(): Byte {
    require(1)
    return buffer.readByte()
}

override fun require(byteCount: Long) {
    if (!request(byteCount)) throw EOFException()
}

override fun request(byteCount: Long): Boolean {
    require(byteCount >= 0) { "byteCount < 0: $byteCount" }
    check(!closed) { "closed" }
    while (buffer.size < byteCount) {
          if (source.read(buffer, Segment.SIZE.toLong()) == -1L) return false
    }
    return true
}
```

可以看到，最终还是通过`source.read`读取文件，唯一不同的就是这里不再是读取到文件结束位才停止，而是一旦发现长度大于一个字节就终止（实际上真正写入到Buffer缓存中的数据可不止一个字节）。之后调用`buffer.readByte()`从缓存中取出一个字节长度的数据（同时会将标记读取位置的偏移量向后移动，以便下次调用时可以读取当前位置之后的数据，这里的设计思路和IO一致）。这里留意一下request方法。它的作用和`writeAll`方法一样，都是读取文件并将数据写入到Buffer缓存中，区别就在于`request`是部分读取，`writeAll `是全量读取。在okio中，除非是获取全部内容（例如，`readByteString`、`readByteArray`、`readUtf8`等），其他的都是部分读取，如`readByte`、`readByteString(byteCount: Long)`、`readLong`等，本质上没有区别，只不过不同的方法规定了不同最小读取长度以确保能拿到正确的数据类型的数据。

还记得开头讲okio优势的时候提到过的`DataInputStream`包装类读取Java基本类型的问题吗？再以`readInt`来看一下okio是如何实现这一功能的：

```
override fun readInt(): Int {
    require(4)
    return buffer.readInt()
}
```

方法几乎和readByte，但是rquire的入参从1变成了4。这也很好理解，毕竟一个Int类型长度为32位，也就是四个字节。而buffer.readInt()和buffer.readByte()的唯一区别也显而易见——前者从缓存中取四个字节，后者取一个（这个稍后也会在Buffer小节里进行分析）。

基本上，okio的读取流程都是这样的流程：借助IO的InputStream将数据的写入到缓存Buffer，读取的字节长度依据调用者的入参决定。但是实际上写入Buffer中的缓存数量却往往和调用者的传入的长度不同，因为okio借助Segment进行缓存，每次读取都会尽可能的将一个Segment写满，而Segment的长度为8K（8192）。而我们真正返还给我们数据的并不是IO而是okio的Buffer缓存，再读取完数据后并将其放入缓存中后。紧接着会按照调用者的需求读取响应长度的数据，并按照要求进行类型转换后返还给调用者正确的数据。

概括一下整体流程：

![](https://github.com/RommelLiang/karl/blob/main/NetWork/img/1652405875953.jpg?raw=true)

* 通过IO的InputStream输入流读取目标数据（read）
* 将数据写入到Buffer缓存（write。没错，就是write。此时对于缓存来说，是被InputStream写入数据）
* 最后，从缓存中取数据返还给调用者

#### IO写操作

根据上一小节对读数据的操作，大致猜测一下：写数据操作应该和上面的读数据操作思想一致。是这样吗？以`File(pathString).sink().buffer().writeByte(0)`为例，我们看一下它的调用流程：

```
fun File.sink(append: Boolean = false): Sink = FileOutputStream(this, append).sink()

fun OutputStream.sink(): Sink = OutputStreamSink(this, Timeout())

fun Sink.buffer(): BufferedSink = RealBufferedSink(this)
```
而`RealBufferedSink`的`writeByte`方法如下：

```
override fun writeByte(b: Int): BufferedSink {
    check(!closed) { "closed" }
    buffer.writeByte(b)
    return emitCompleteSegments()
}
```

这简直是和Source从一个模子刻出来的！（此处就不展示UML和用Java代码表示了，自行对应替换即可）。这里大胆预想一下，这里的逻辑和读文件一致：首先将文字写入到Buffer缓存，然后通过OutputStream写入到文件。okio确实是这么做的，但是这里有一个问题——`File(pathString).sink().buffer().writeByte(0)执行完之后，目标文件里空空如也！！！！为什么会这样？跟着源码去看一下。首先是`buffer. writeByte`，看一下Buffer的writeByte方法：

```
override fun writeByte(b: Int): Buffer {
    val tail = writableSegment(1)
    tail.data[tail.limit++] = b.toByte()
    size += 1L
    return this
}
```

猜对了一半，确实是将数据先写入到Buffer缓存。而且依旧是像读数据时那样：通过Segment承载字节数组，每次添加新的字节后，将偏移量向后移动一位。以便之后将新的字节放置到正确的位置。但是却有有一点不一样，没有对OutputStream的操作啊？难道，只将数据写入缓存？当然不是，注意上文中的`emitCompleteSegments()`方法：

```
override fun emitCompleteSegments(): BufferedSink {
    check(!closed) { "closed" }
    val byteCount = buffer.completeSegmentByteCount()
    if (byteCount > 0L) sink.write(buffer, byteCount)
    return this
}
```

代码逻辑很简单，首先判断缓存内的数据是否大于0，如果大于0的话就调用`sink.write(buffer, byteCount)`方法。这里的`sink.write(buffer, buffer.size)`是一个关键，这里和Source的读数据流程对上了，数据就是在这里实现了从Buffer到文件的写流程。此处的`sink`是`OutputStreamSink`的实例，它的关键代码如下：

```
private class OutputStreamSink(
  private val out: OutputStream,
  private val timeout: Timeout
) : Sink {

  override fun write(source: Buffer, byteCount: Long) {
    checkOffsetAndCount(source.size, 0, byteCount)
    var remaining = byteCount
    while (remaining > 0) {
      timeout.throwIfReached()
      //注释一
      val head = source.head!!
      val toCopy = minOf(remaining, head.limit - head.pos).toInt()
      //注释二
      out.write(head.data, head.pos, toCopy)

      head.pos += toCopy
      remaining -= toCopy
      source.size -= toCopy
	//注释三
      if (head.pos == head.limit) {
        source.head = head.pop()
        SegmentPool.recycle(head)
      }
    }
  }
  override fun flush() = out.flush()
}
```
首先来看`write`方法，主体是一个循环：注释一处获取到Buffer缓存的头部节点，然后再注释二处通过OutputStream执行IO操作将缓存内容写入到文件，并且缓存链表的节点信息。注释三出则是对缓存节点的处理和回收，稍后再讲。

而`shik`的flush操作也很简单，就是执行了`OutputStream`的flush操作，通过调用flush()方法，把流中缓存数据刷新到磁盘(或者网络，以及其他任何形式的目标媒介)中。

`BufferedSink.close`方法和`BufferedSink.flush`方法的不同点在于前者会执行OutputStream的close操作。主要流程还是通过OutputStream执行IO操作，这里就不再赘述了。

那么`emitCompleteSegments`的逻辑就很简单了，它就是负责将Buffer缓存数据写入到输出流缓冲区中的。而且写入是有条件的。也就是`completeSegmentByteCount`方法：

```
fun completeSegmentByteCount(): Long {
    //判断缓存中有几个节点
    var result = size
    if (result == 0L) return 0L

    val tail = head!!.prev!!
    //如果尾节点没有满，则将其从基数中去掉
    if (tail.limit < Segment.SIZE && tail.owner) {
        result -= (tail.limit - tail.pos).toLong()
    }

    return result
}

```
代码逻辑很简单，判断有几个满了的节点。只有在缓存中至少有一个满员的节点才会将数据写入到输出流缓冲区中。也就是说：写入Buffer中的缓存并不会立即写入到输出流的缓冲区中，而是当发现Buffer中有了满员的节点才会开始写入，也就是至少要有8k数据。但是如果数据量不到8k呢？

这时候，我们需要手动调用`BufferedSink.flush`或者`BufferedSink.close`方法。它俩不精能实现数据从未满员Buffer缓存到输出流的写入操作，还包含了输出流到文件的操作。`flush`的实现如下：

```
override fun flush() {
    check(!closed) { "closed" }
    if (buffer.size > 0L) {
        sink.write(buffer, buffer.size)
    }
    sink.flush()
}
```
`sink.flush()`的代码如下：

```
override fun flush() = out.flush()
override fun close() = out.close()
```
逻辑很简单，触发流的flush操作，强制将缓冲区中的数据发送出去,不必等到缓冲区满（在FileOutputStream中就是写入到文件中）。`close`和它类似，不同的就是分别调用IO的flush和close的操作。`BufferedSink.flush`和`BufferedSink.close`方法在执行IO的close和fluse多了一层操作：就是检查缓存中是否还有需要写入到输出流中的数据。

可见，Sink的写操作和Source的读操作核心思想一致：都是需要将目标数据写入到Buffer缓存中，然后再将缓存数据返回给执行“读”操作的调用者，或者“写”到文件等目标媒介中。而Sink在数据量不足8k时需要通过手动触发`BufferedSink.close`或者`BufferedSink.flush`方法触发缓存到输出流缓冲区的数据写入，以及OutputStream执行IO操作后才能真正的讲数据写入到目标位置。`Source`则无需任何手动操作，会直接通过InputSream执行IO操作将数据放置到Buffer缓冲区，之后再自动的从Buffer缓存取出数据返还给调用者。

### Buffer缓存和Segment

讲完okio的读写操作，你可能会有疑问：难道okio只是对InputStream和OutputStream的简单Api封装吗？简单强大的Api操作确实可以通过整合方法和接口实现，但是前面提到的**性能高效的缓存处理功能**该怎么实现？答案就是Buffer和Segment！okio当然不可能只是简单的api整合——Buffer和Segment才是他的核心和精华。对缓存的处理是整个okio最精彩的篇章，上文中每次提到Segment时都是一带而过，就是因为它相对来说比较占据篇幅。这节里我们会详细介绍它们两个。

首先看一下Buffer：Buffer缓冲区是okio提供的一个新的数据类型。用户不需要关心的缓冲区大小。每个缓冲区都像是一个队列:遵循FIFO先进先出的原则：写数据在最后、读数据在最前。

它同时实现了BufferedSource和BufferedSink接口。每个`RealBufferedSource`和`RealBufferedSink`持有一个常量`buffer`作为`Buffer`的实例，从而实现对缓存数据的访问。虽然buffer同时具备写和读的操作。但是，由于Source和Sink分别只提供读和写操作，你依旧无法通过`RealBufferedSource`或`RealBufferedSink`同时进行读写操作。虽然你可以通过`buffer.outputStream()`和`buffer.inputStream()`同时获取输入和输出流，这时可以通过buffer同时实现对Buffer缓存读写操作了。但是你依旧无法同时实现IO的读写操作，因为`buffer.outputStream()`和`buffer.inputStream()`返回的输入输出流经过了重写，以`outputStream()`为例代码如下：

```
override fun outputStream(): OutputStream {
    return object : OutputStream() {
        override fun write(b: Int) {
            writeByte(b)
        }

        override fun write(data: ByteArray, offset: Int, byteCount: Int) {
            this@Buffer.write(data, offset, byteCount)
        }

        override fun flush() {}

        override fun close() {}

        override fun toString(): String = "${this@Buffer}.outputStream()"
    }
}
```

该方法返回了一个匿名内部类，重写了`OutputStream`的`write`方法——不再进行IO操作，而是调用了Buffer本身的`write`操作。而Buffer里的所有读写操作，全部都是对缓存的读写，基本上就是在处理`Segment`。

以`readByte`和`writeByte `为例：

```
override fun readByte(): Byte {
    if (size == 0L) throw EOFException()

    val segment = head!!
    var pos = segment.pos
    val limit = segment.limit

    val data = segment.data
    val b = data[pos++]
    size -= 1L

    if (pos == limit) {
        head = segment.pop()
        SegmentPool.recycle(segment)
    } else {
        segment.pos = pos
    }

    return b
}


override fun writeByte(b: Int): Buffer {
    val tail = writableSegment(1)
    tail.data[tail.limit++] = b.toByte()
    size += 1L
    return this
}
```

它们都是在对Segment进行处理！而真正实现缓存和文件的读写，还是需要我们调用我们传入的传入的输入流或者输出流。对输入输出流的操作在上文的读/写小节里已经讲过了。还记得上文中讲到写文件时必须调用`BufferedSink.close`或者`BufferedSink.flush`方法吗？

因为`InputStreamSource`和`OutputStreamSink`才是真正的IO输入输出流的执行者，在上文的实例中，就是它们所持有的`FileInputStream`和`FileOutputStream`。`BufferedSink`的任何写都是对缓存Buffer的操作，所有的IO操作必须借助Sink，也就是`RealBufferedSink`所持有的`OutputStreamSink`，这里需要我们手动调用。而`BufferedSource`的读操作也是如此，通过Buffer进行的读操作，都是读的缓存。而之所以缓存中有数据，还是需要`RealBufferedSink`所持有的`OutputStreamSink`。上文中我们讲过，`BufferedSource`在从缓存中取数据返还给调用者之前，会调用Buffer的`Buffer.writeAll(source: Source)`或者BufferedSource的`BufferedSource.request(byteCount: Long)`。它们的代码分别如下：

```
override fun writeAll(source: Source): Long {
    var totalBytesRead = 0L
    while (true) {
        val readCount = source.read(this, Segment.SIZE.toLong())
        if (readCount == -1L) break
        totalBytesRead += readCount
    }
    return totalBytesRead
}

override fun request(byteCount: Long): Boolean {
    require(byteCount >= 0) { "byteCount < 0: $byteCount" }
    check(!closed) { "closed" }
    while (buffer.size < byteCount) {
        if (source.read(buffer, Segment.SIZE.toLong()) == -1L) return false
    }
    return true
}
```

它们的一大共同点是都调用了`source.read`方法，而这个source就是`InputStreamSource`的实例，而它持有真的的InputStream——与Sink不同，source在我们读取Buffer缓存首先自动实现了对IO操作的调用。

**由于Buffer实现了Sink和Source接口，方法的多态导致Buffer、Source和Sink之间存在大量的同名函数。这对梳理流程有较大的阻碍，建议打断点分布调试，以理清它们之间的关系。**

讲这么多是为了明确Buffer的职责，排除IO操作对缓存操作的影响。在正式开始分析前，先了解一下Segment的结构：

```
internal class Segment {
  //存储缓存的字节数组
  @JvmField val data: ByteArray
  //数据读取的起始点
  @JvmField var pos: Int = 0
  //当前Segment的数据量大小
  @JvmField var limit: Int = 0
  @JvmField var shared: Boolean = false
  @JvmField var owner: Boolean = false
  //下一个节点
  @JvmField var next: Segment? = null
  //上一个节点
  @JvmField var prev: Segment? = null


  constructor() {
    this.data = ByteArray(SIZE)
    this.owner = true
    this.shared = false
  }


  //弹出当前Segment并返回下一个Segment
  //主要用于当前节点数据被读取完之后，从链表里删除该节点
  fun pop(): Segment? {
    val result = if (next !== this) next else null
    prev!!.next = next
    next!!.prev = prev
    next = null
    prev = null
    return result
  }

  //插入一个Segment
  //主要用于当向链表写入数据时增加节点
  fun push(segment: Segment): Segment {
    segment.prev = this
    segment.next = next
    next!!.prev = segment
    next = segment
    return segment
  }
  companion object {
    /** The size of all segments in bytes.  */
    const val SIZE = 8192
    /** Segments will be shared when doing so avoids `arraycopy()` of this many bytes.  */
    const val SHARE_MINIMUM = 1024
  }
}  
```

分析一下`Segment`中重要变量的含义：

* data：存储字节的数组
* prev和next：前置节点和后置节点，用来支持双向链表
* limit：当前存储的数据量，Segment使用字节数组存储数据。最大长度为8192（可用容量即为：8192-limit）。
* pos：读数据时的起点，也就是从data数组的哪个位置开始读数据。



### Buffer缓存的读写操作
分析完结构，我们看一下Buffer的缓存的读写操作。注意这里的读写操作要和上文中的IO读写区分开了。无论IO的读或者写操作，对于Buffer而言，都需要一次执行写和读操作——上文已经分析过了，okio并不会允许我们直接进行IO操作，而是借助Buffer缓存。我们任何的IO操作，都是要在缓存基础上。对于“读操作”，首先需要InputStream将数据写入到缓存，然后，调用者再从缓存中读数据。对于“写操作”，首先需要调用者将数据写入到Buffer缓存，然后手动调用close或者flush方法，从缓存中读取数据并使用OutputStream进行IO操作。
#### 将数据写入到Buffer缓存
接下来正式看Buffer的缓存操作处理。首先看Source对它的写入操作。

上文中在讲`BufferedSource.readUtf8()`时，最终是在`InputStreamSource.read`方法中执行InputStream的IO操作，并将数据写入缓存，关键代码如下：

```
override fun read(sink: Buffer, byteCount: Long): Long {
    //...
    //从Buffer中拿到Segment
    val tail = sink.writableSegment(1)
    //根据要读取数据的长度和Segment剩余容量确定本轮要读取的数据长度
    val maxToCopy = minOf(byteCount, Segment.SIZE - tail.limit).toInt()
    //InputStream输入流中读取数据到Segment的数组中
    val bytesRead = input.read(tail.data, tail.limit, maxToCopy)
    if (bytesRead == -1) return -1
    //调整当前Segment容量
    tail.limit += bytesRead
    //调整Buffer数据量
    sink.size += bytesRead
    return bytesRead.toLong()
    //...
}
```

我们重点看`val tail = sink.writableSegment(1)`，取出`Segment`的逻辑：

```
internal fun writableSegment(minimumCapacity: Int): Segment {
    require(minimumCapacity >= 1 && minimumCapacity <= Segment.SIZE) { "unexpected capacity" }

    //检查链表头节点，如果为空，创建一个新的Segment作为头结点
    //并且将这个节点的前置节点和后置节点都指向它自己
    if (head == null) {
        val result = SegmentPool.take() 
        head = result
        result.prev = result
        result.next = result
        return result
    }
    //取出尾节点，也就是head的前置节点
    var tail = head!!.prev
    //如果tail已经存满了数据，那么添加一个新节点
    if (tail!!.limit + minimumCapacity > Segment.SIZE || !tail.owner) {
        tail = tail.push(SegmentPool.take())
    }
    return tail
}
```

代码首先判断Segment 链表的头结点head是否为空，如果为空则通过为head赋值（`SegmentPool`的逻辑就不展开了，就是一个简单的链表结构，用来存储和复用Segment实例。避免重复的创建和销毁对象带来的内存开销，源码很简单，只有一个take方法用于取（或者创建）数据，recycle用于回收数据）。然后将头结点的前置节点和尾节点都执行它自己。下图可以帮助你更好的理解Segment双向链表的数据结构：

![](https://github.com/RommelLiang/karl/blob/main/NetWork/img/segment.jpg?raw=true)

上面分辨展示了一个、两个和多个（三个）节点的链表情况。它是一个双向链表，同时头结点的前置节点指向最后一个节点也就是尾节点，尾节点的后置节点指向第一个节点也就是头节点。如果只有有个节点，那么头尾都指向自己。

到这里，Buffer的数据写入就很明确了：Buffer采用双向链表的形式保存缓存。每次要写入数据时，都会返回尚未被写入过数据的Segment节点，也就是尾节点。如果没有，则创建新的Segment节点，添加到链表的尾部。每次对Buffer缓存进行写操作都是对Buffer中的Segment尾结点进行操作。

至于一次写入多少数据，则由Segment的节点可用大小和数据量共同决定。也就是`InputStreamSource.read`中的`val maxToCopy = minOf(byteCount, Segment.SIZE - tail.limit).toInt()`。逻辑很简单，取最小值。如果节点容量大于数据量，则直接从输入流中取出所有数据写入缓存。如果节点剩余空间不足以存储，则只从输入流中取出剩余空间的最大容量，并写入。其余的数据则在下一个循环内放入新的节点中。而每当Buffer缓存被写入新的数据之后，都会记录和调整当前Segment被用掉的长度(Segment.limit)和链表里数据的总长度(Buffer.size)。`Segment.limit`会直接影响下一次从输入流写入数据的量以及是否需要创建新的节点。

而对于Sink，其操作也是如此，都是通过`writableSegment`获取到尾节点，然后进行数据写入。两者的唯一不同之处在于数据源不同，SInk是用户提供的，而Source则是InputStream。

到此，Buffer缓存写入已经分析完毕，逻辑概括如下：采用双向链表存储数据，每次都想尾结点的字节数组里写数据，一次最多写入长度为8K。每当执行完一次写入操作后会更新链表数据总量以及当前节点使用掉的空间，以此来决定下一次写入的数据量以及是否需要创建新的节点。

#### 读取Buffer缓存数据



还是以`BufferedSource.readUtf8()`来看读取Buffer缓存的操作，`Buffer

```

//读取全部数据
override fun readUtf8() = readString(size, Charsets.UTF_8)

override fun readString(byteCount: Long, charset: Charset): String {
    //判断要读取数据的长度是否正确
    require(byteCount >= 0 && byteCount <= Integer.MAX_VALUE) { "byteCount: $byteCount" }
    if (size < byteCount) throw EOFException()
    if (byteCount == 0L) return ""
    //关键代码①
    //获取缓存节点头部
    val s = head!!
    //如果要读取的数据长度大于一个Segment的长度，则使用readByteArray读取数据
    if (s.pos + byteCount > s.limit) {
        //关键代码②
        return String(readByteArray(byteCount), charset)
    }
     //关键代码③
    //如果读取数据的长度不大于一个Segment内存储的数据
    //则直接从当前节点中指定的起始位置pos读取指定长度的数据
    val result = String(s.data, s.pos, byteCount.toInt(), charset)
    //关键代码④
    s.pos += byteCount.toInt()
    size -= byteCount
    //如果当前节点的数据被读完了，则回收节点
    if (s.pos == s.limit) {
        head = s.pop()
        SegmentPool.recycle(s)
    }

    return result
}


override fun readByteArray(byteCount: Long): ByteArray {
    require(byteCount >= 0 && byteCount <= Integer.MAX_VALUE) { "byteCount: $byteCount" }
    //关键代码⑤
    if (size < byteCount) throw EOFException()
    //创建数据存取区，长度为要读取数据的容量
    val result = ByteArray(byteCount.toInt())
    //开始读取数据
    readFully(result)
    return result
}

@Throws(EOFException::class)
override fun readFully(sink: ByteArray) {
    //关键代码⑥
    var offset = 0
    //开始循环读取链表，知道拿到的数据等于需要读取的数据量
    //此处的sink命名很有迷惑性，注意他不是Sink，而是一个自己数组
    while (offset < sink.size) {
        //读取数据
        val read = read(sink, offset, sink.size - offset)
        if (read == -1) throw EOFException()
        offset += read
    }
}


override fun read(sink: ByteArray, offset: Int, byteCount: Int): Int {
    checkOffsetAndCount(sink.size.toLong(), offset.toLong(), byteCount.toLong())
     //关键代码⑦
    //获取头部节点
    val s = head ?: return -1
    //计算需要读取的数据量
    val toCopy = minOf(byteCount, s.limit - s.pos)
    //从Segment节点拷贝数据到目标数组
    System.arraycopy(s.data, s.pos, sink, offset, toCopy)

    //更新节点数据起始点和Buffer缓存大小
    s.pos += toCopy
    size -= toCopy.toLong()

    //如果当前节点的数据被读完了，则回收节点
    if (s.pos == s.limit) {
        head = s.pop()
        SegmentPool.recycle(s)
    }

    return toCopy
}
```

读取缓存的流程如下：
首先是获取链表头部（关键代码①），然后判断当前Segment里的数据容量是否满足调用者所需要的容量。这里的计算方式很简单，通过pos和limit的差值计算。pos和limit在前面Segment结构分析里已经介绍过了。就是两个数组Index角标，通过和调用者传入的数据长度对比，以确定一个Segment里的数据能否满足要求。

如果满足要求。则直接从数组里区指定区间的数据（关键代码③）。这里因为是获取utf8字符串，所以会通过`String(bytes: ByteArray, offset: Int, length: Int, charset: Charset)`方法从字节数组内获取数据并根据传入的编码格式进行转换（readInt等其他方法可能在数据读取长度和转换方式上有区别，但是在Buffer链表层的操作原理都一致，这里就不展开分析了）。然后就是关键代码④处，更新当前Segment的pos和Buffer的整体size，以便下一次数据读取。紧接着会通过pos和limit判断Segment是否已经被读取完了，如果读取完了就会将节点从链表移除并回收节点。

如果一个Segment里的数据量不足以满足调用者要求的数据量，则采取另外一种读取操作。也就是关键代码②处。执行`readByteArray(byteCount: Long)`方法。该方法的代码逻辑很简单，它根据调用者所需要读取的字节长度创建一个等长度的用于暂存数据的字节数组，并通过`readFully(sink: ByteArray)`为该数组赋值并返回（关键代码⑤）。

紧接着，`readFully`开启循环，不断的调用`read(sink: ByteArray, offset: Int, byteCount: Int)`，并不断累加所读取的字节数。直到所读取的字节数满足调用者传入的ByteArray长度（也就是一开始的`byteCount`所代表的要读取的字节数量）。这里可以看到，最终对Segment的操作是在`read(sink: ByteArray, offset: Int, byteCount: Int)`方法里进行的（关键代码⑦）。这里的逻辑和关键代码④处的大同小异，都是通过pos和limit确定读取数据的范围，然后判断是否需要回收节点。唯一不同的就是这里采用了`System.arraycopy`这种**效率更高的方式**将节点中的大段数据拷贝到用于暂存数据的字节数组中。

这里需要注意一下：readFully和read里的sink局部变量，它并不是上文中的`SInk`。它只是一个ByteArray字节数组，用来暂存读取到的数据。

Buffer缓存的读取流程大致就是如此：从链表的头结点开始读取数据，没读取完一个Segment就将它从链表内移除。而每个Segment节点里都通过pos字段记录当前节点被读取的数据位置，以便作为下次读取的开始位置。通过pos起始位置和limit当前节点存储的数据是否满足调用者要求的数量。如果不满足要求，则首先创建一个和调用者要求的数据量同等大小的字节数组作为数据暂存数组。然后开启循环，不断的在链表里读取数据，直到数据暂存数组被填满。之后，进行响应的格式调整后返回给调用者。如果一个节点的数据量满足调用者的需求，则直接从头结点里取数据。最后，在读取完数据后都谁更新Buffer的大小和节点的起始位pos。

最后，看一下`Buffer.readByte`和`Buffer.readInt`方法作为参考：

```
override fun readByte(): Byte {
    //..
    val segment = head!!
    var pos = segment.pos
    val limit = segment.limit

    val data = segment.data
    val b = data[pos++]
    size -= 1L

    if (pos == limit) {
        head = segment.pop()
        SegmentPool.recycle(segment)
    } else {
        segment.pos = pos
    }

    return b
}

override fun readInt(): Int {
    if (size < 4L) throw EOFException()

    val segment = head!!
    var pos = segment.pos
    val limit = segment.limit

    if (limit - pos < 4L) {
        return (readByte() and 0xff shl 24
                or (readByte() and 0xff shl 16)
                or (readByte() and 0xff shl  8) // ktlint-disable no-multi-spaces
                or (readByte() and 0xff))
    }

    val data = segment.data
    val i = (data[pos++] and 0xff shl 24
            or (data[pos++] and 0xff shl 16)
            or (data[pos++] and 0xff shl 8)
            or (data[pos++] and 0xff))
    size -= 4L

    if (pos == limit) {
        head = segment.pop()
        SegmentPool.recycle(segment)
    } else {
        segment.pos = pos
    }

    return i
}
```

`readByte`很简单，取出Segment自己数组内起始位的一个字节，然后更新起始位pos并判断是否需要销毁当前节点。`readInt`比较复杂，因为一个int占四个字节，所以会首先判断当前节点的数据量是否大于四个字节。如果大于四个字节，则依次取出四个。否则交由`readByte`，依次读取每个自己直到没有数据。剩下的更新起始位和节点回收的代码都大同小异（Buffer中其他的读取方法基本都遵循这个逻辑，这里就不详细赘述了）。

#### Buffer缓存的优化

上文中对Buffer缓存的操作都只是单独的读或者写操作。但实际上我们可能遇到需要同时进行读写操作的需求。例如将文件A的内容拷贝到B里。在IO中的实现如下：

```
fun ioCopy() {
    val inputStream = FileInputStream(File(pathText))
    val outPutStream = FileOutputStream(File(pathString))
    val bytes = ByteArray(1024)
    var size: Int
    while (inputStream.read(bytes).also { size = it } != -1) {
        outPutStream.write(bytes, 0, size)
    }
    outPutStream.flush()
}
```

逻辑很简单，就是用一个缓存数据作为临时数据存储区。边读边写。而okio也提供了类似的操作：

```
fun okIoCopy() {
    val sink = File(pathString).sink().buffer()
    val source = File(pathText).source()
    sink.writeAll(source)
    sink.flush()
}
```

代码相对简单些，关键代码是`sink.writeAll(source)`：

```
override fun writeAll(source: Source): Long {
    var totalBytesRead = 0L
    while (true) {
        val readCount: Long = source.read(buffer, Segment.SIZE.toLong())
        if (readCount == -1L) break
        totalBytesRead += readCount
        emitCompleteSegments()
    }
    return totalBytesRead
}
```

代码逻辑很简单，就是已当前将要执行写IO曹组的Sink的Buffer作为读IO操作的Source的数据缓存。大致原理和IO类似，都是直接将InputStream读取的数据存储后交给OutputStream。唯一不同的是，okio帮我们把这一步封装好了。

接下来我们借助Pipe操作看一下okio最令人拍案惊奇的操作。传统IO中，通过Pipes实现两个线程通信的代码如下：

```
fun ioPipe() {
    val pipedOutputStream = PipedOutputStream()
    val pipedInputStream = PipedInputStream(pipedOutputStream)
    Thread {
        pipedOutputStream.write("A spectre is haunting Europe — the spectre of communism".toByteArray())
    }.start()
    Thread {
        val bytes = ByteArray(1024)
        val size = pipedInputStream.read()
        pipedInputStream.read(bytes, 0, size)
        println(String(bytes, 0, size))
    }.start()
}
```
而借助okio可以实现同样的操作：

```
fun okIoPope(){
    val pipe = Pipe(1024)

    Thread {
        pipe.sink.buffer().apply {
            writeUtf8("A spectre is haunting Europe — the spectre of communism")
            close()
        }
    }.start()

    Thread {
        val readUtf8 = pipe.source.buffer().readUtf8()
        println(readUtf8)
    }.start()
}
```

代码中的`pipe.sink.buffer()`和`pipe.source.buffer()`获取到的依旧是`RealBufferedSink`和`RealBufferedSource`的实例。然而它们所持有的成员变量sink和source却不再是OutputStreamSink和InputStreamSource的实例了。而是在Pipe中的匿名内部类：

```
class Pipe(internal val maxBufferSize: Long) {
    internal val buffer = Buffer()

    @get:JvmName("sink")
    val sink = object : Sink {


        override fun write(source: Buffer, byteCount: Long) {

            synchronized(buffer) {
                check(!sinkClosed) { "closed" }

                while (byteCount > 0) {
                    关键代码一
                    buffer.write(source, bytesToWrite)
                    byteCount -= bytesToWrite
                    (buffer as Object).notifyAll() // Notify the source that it can resume reading.
                }
            }

            delegate?.forward { write(source, byteCount) }
        }

    }

    @get:JvmName("source")
    val source = object : Source {
        override fun read(sink: Buffer, byteCount: Long): Long {
            synchronized(buffer) {

                while (buffer.size == 0L) {
                    if (sinkClosed) return -1L
                    timeout.waitUntilNotified(buffer) // Wait until the sink fills the buffer.
                }

                val result = buffer.read(sink, byteCount)
                (buffer as Object).notifyAll() // Notify the sink that it can resume writing.
                return result
            }
        }
        
    }
}
```

代码删减后如上。逻辑很简单，通过阻塞线程实现线程间的单向流动。整个通讯的流程大致如下：

String->RealBufferedSink.buffer->Pipe.buffer->RealBufferedSource.buffer->String。从流程上看，数据的传递方式貌似和IO中的一致，数据都是经过复制传递的。然而okio采用的却并不是在流之间拷贝数据。而是改变Buffer的指向。注意Pipe中的关键代码一。这里的操作就是将RealBufferedSink.buffer中的缓存数据`转移`到Pipe的Buffer中。在分析Buffer的`write(source: Buffer, byteCount: Long)`代码之前，首先要了解Segment的几个方法：

```
//该方法用于将节点按指定长度进行分割
fun split(byteCount: Int): Segment {
    require(byteCount > 0 && byteCount <= limit - pos) { "byteCount out of range" }
    val prefix: Segment

    //分情况处理创建节点
    //这么做主要是基于两点考虑
    //-通过共享Segment避免赋值数据
    //-避免短的共享数据断导致链表过长（大量的Segment节点）
    //如果要分割的数据长度
    //byteCount大于SHARE_MINIMUM（1024）
    if (byteCount >= SHARE_MINIMUM) {
        //创建新的Segment
        prefix = sharedCopy()
    } else {
        //如果分割的数据小于1024，从缓冲池中取节点
        //然后再为节点设置元素
        prefix = SegmentPool.take()
        data.copyInto(prefix.data, startIndex = pos, endIndex = pos + byteCount)
    }

    //将分割后产生的新节点添加到链表
    prefix.limit = prefix.pos + byteCount
    pos += byteCount
    prev!!.push(prefix)
    return prefix
}

//该方法用于尝试将当前阶段与它的前置节点合并
fun compact() {
    check(prev !== this) { "cannot compact" }
    // 如果前置节点无法执行写操作，直接返回
    if (!prev!!.owner) return
    //计算当前节点的数据量
    val byteCount = limit - pos
    //计算前置节点的可用空间
    val availableByteCount = SIZE - prev!!.limit + if (prev!!.shared) 0 else prev!!.pos
    //如果前置节点的剩余容量不足以存储当前节点的数据，直接返回
    if (byteCount > availableByteCount) return
    //将当前节点的数据写入到前置节点
    writeTo(prev!!, byteCount)
    //从链表中弹出节点，并回收
    pop()
    SegmentPool.recycle(this)
}


fun writeTo(sink: Segment, byteCount: Int) {
    check(sink.owner) { "only owner can write" }
    if (sink.limit + byteCount > SIZE) {
        //如果当前pos位置也就是数据的起始位（字节数组的Index）向后不足以容纳数据
        //那么将所有数据前移，并将pos位置为0
        if (sink.shared) throw IllegalArgumentException()
        if (sink.limit + byteCount - sink.pos > SIZE) throw IllegalArgumentException()
        sink.data.copyInto(sink.data, startIndex = sink.pos, endIndex = sink.limit)
        sink.limit -= sink.pos
        sink.pos = 0
    }

    //数组的拷贝，将当前数组放入到前置节点的数组中
    data.copyInto(
        sink.data, destinationOffset = sink.limit, startIndex = pos,
        endIndex = pos + byteCount
    )
    sink.limit += byteCount
    pos += byteCount
}

fun sharedCopy(): Segment {
    shared = true
    return Segment(data, pos, limit, true, false)
}
```

首先是`split`和`compact`，它们分别负责执行将一个节点分割和合并的操作。详细流程已经写在注释里。里面最精妙的就是当遇到较大数据时（1024），采用直接改变数据引用的方式，而数据量较小时，则采用数组赋值的方式迁移数据。分割和合并数据是为了在缓存之间转移数据时能够确保目标缓存中不会存在未满员的Segment节点。

接下来就是`buffer.write(source: Buffer, byteCount: Long)`方法：


```
//source为RealBufferedSink.buffer。byteCount是要转移的数据量
//write为Buffer的方法，该Buffer即为Pipe.buffer
override fun write(source: Buffer, byteCount: Long) {
    var byteCount = byteCount

    require(source !== this) { "source == this" }
    checkOffsetAndCount(source.size, 0, byteCount)

    while (byteCount > 0L) {
        //首先判断source中第一个节点的数据长度是否source->Pipe.buffer的数据量
        if (byteCount < source.head!!.limit - source.head!!.pos) {
            //source中第一个节点数据量足够
            //如果Pipe.buffer的head不为空，返回Pipe.buffer的尾节点。否则返回null
            val tail = if (head != null) head!!.prev else null
            if (tail != null && tail.owner &&
                byteCount + tail.limit - (if (tail.shared) 0 else tail.pos) <= Segment.SIZE) {
                //如果Pipe.buffer的尾节点足够容纳数据，则调用buffer.writeTo方法将数据拷贝过去
                //之后调整相关buffer的数据
                source.head!!.writeTo(tail, byteCount.toInt())
                source.size -= byteCount
                size += byteCount
                //此时说明然后返回，结束数据迁移的流程。
                return
            } else {
                //如果Pipe.buffer的尾节点长度不足以容纳所有数据
                //则将目标节点分割以便将Pipe.buffer的尾节点填满
                source.head = source.head!!.split(byteCount.toInt())
            }
        }

        //获取source的头节点，并计算头结点内的数据容量
        val segmentToMove = source.head
        val movedByteCount = (segmentToMove!!.limit - segmentToMove.pos).toLong()
        //将source的头结点移除链表
        source.head = segmentToMove.pop()
        //如果Pipe.buffer的head为空，直接改变头结点的指向
        if (head == null) {
            head = segmentToMove
            segmentToMove.prev = segmentToMove
            segmentToMove.next = segmentToMove.prev
        } else {
            //如果头结点不为空，在尾结点向链表添加节点
            var tail = head!!.prev
            tail = tail!!.push(segmentToMove)
            //合并节点，避免链表尾部出现两个容量不满的节点
            tail.compact()
        }
        source.size -= movedByteCount
        size += movedByteCount
        byteCount -= movedByteCount
    }
}
```


这里就是Buffer缓存处理中的精华操作，它的作用将一个Buffer的数据转移到另一个Buffer中。首先是判断当前Segment是否包含全部需要转移的数据。如果容纳的下，则直接将数据复制过去。如果不满足，则首先判断目标节点的剩余容量，将要转移的数据进行分割。然后采用复制的方式将目标节点的剩余空间填满。剩下的节点则采用直接移动的方式加入到目标链表，直接从soucrce中移除头部的Segment，然后添加到目标Buffer的Segment尾部。紧接着判断尾节点前一个Segment是否满员，如果不满员。则进行节点合并。

这里的操作非常精妙。而且平衡了两个相互矛盾的需求——不浪费CPU，也不浪费内存（square官方倒是对此很骄傲，甚至不惜用大幅超过源码量的注释来讲述这断代码的精妙）。对于目标Segment容纳的下的小段数据，采用直接复制的方法，而大段的Segment数据，只是一个引用指向的变化，将整个段从一个缓冲区重新分配到另一个缓冲区。这就是不浪费cpu的根源，大段数据直接改变链表的指向，而不是复制数据——复制大量数据的成本很高！

缓冲区中的相节点需要至少有50%的数据，除了头部和尾部（因为我们要从头部取数据，而从尾部添加数据）。不浪费内存是指当将一个缓冲区写入另一个缓冲区时，okio更倾向于重新分配整个Segment段而不是字节复制这种更加紧凑的形式。假设有一个缓冲区的节点数量分别为[91%, 61%]，此时添加一个72%容量的节点。链表中的节点情况会是 [91%, 61%, 72%]，而不是[100%，100%，24%]。因此不会产生字节复制操作。如果我们要将一个[99%, 3%] 的Buffer转移到一个[100%, 2%]的节点。情况会有些不同，新的缓存链表为：[100%, 2%, 99%, 3%]而不是 进行字节拷贝操作生成内存使用更高效的[100%, 100%, 4%]链表，就是为了尽量不进行多余拷贝操作。当合并节点时，将压缩相邻的Segment节点。例如，向[100%, 40%]的链表添加[30%, 80%], 结果链表是 [100%, 70%, 80%]。

总之，就是疯狂的在字节复制和改变节点指向之间疯狂的寻求一种平衡的操作。

而至于Pipe.buffer->source.buffer的操作。和sink.buffer->Pipe.buffer的操作原理一样，最终都是借助`buffer.write(source: Buffer, byteCount: Long)`方法。唯一不同的是数据交换方发生了改变:sink->Pipe.buffer是Pipe.buffer.write(sink.buffer,byteCount)，而Pipe.buffer->source.buffer则是source.buffer.write(Pipe.buffer,byteCount)，仅此而已！

### TimeOut超时机制

超时机制在sourc和sink中均有应用，而且原理类似。这里以`InputStreamSource`为例：
```
private class InputStreamSource(
    private val input: InputStream,
    private val timeout: Timeout
) : Source {

    override fun read(sink: Buffer, byteCount: Long): Long {
        //..
        try {
            timeout.throwIfReached()
            val tail = sink.writableSegment(1)
            val maxToCopy = minOf(byteCount, Segment.SIZE - tail.limit).toInt()
            val bytesRead = input.read(tail.data, tail.limit, maxToCopy)
            if (bytesRead == -1) return -1
            tail.limit += bytesRead
            sink.size += bytesRead
            return bytesRead.toLong()
        } catch (e: AssertionError) {
            if (e.isAndroidGetsocknameError) throw IOException(e)
            throw e
        }
    }
}
```
其read方法在开始执行InputStream操作前会执行`timeout.throwIfReached()`方法：

```
open fun throwIfReached() {
    //如果线程已经中断了，抛出线程中断异常
    if (Thread.interrupted()) {
        Thread.currentThread().interrupt() // Retain interrupted status.
        throw InterruptedIOException("interrupted")
    }

    //如果有超时限制，并且超过了超时时间，则抛出超时异常
    if (hasDeadline && deadlineNanoTime - System.nanoTime() <= 0) {
        throw InterruptedIOException("deadline reached")
    }
}
```

原理很简单，就是进行时间判断，通过抛出异常的方式结束整个IO操作。


### 结尾

本文简单分析了一下okio的主要流程、缓存的高效操作方法以及超时机制的处理。但是okio中还提供了其他功能，例如Gzip压缩异步，AsyncTimeout异步超时，ByteString字节序列等。这些都是为了弥补Java IO的不足从而使得IO操作更加高效简洁而存在的。由于篇幅原因，本文之关注一些核心流程。这些内容就不展开了。































