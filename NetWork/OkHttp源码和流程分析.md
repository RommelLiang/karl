### 前言
OKHttp是Square推出的一款网络请求框架，是目前市面上使用最多的网络框架之一。大名鼎鼎的Retrofit就是基于它设计的。而它曾经的底层IO组件，目前也是独立成为了一款优秀的IO开源框架——[Okio](https://square.github.io/okio/)，该框架的源码分析详见[Okio源码和流程分析](https://juejin.cn/post/7098916408932237349)。

OkHttp是一个高效的执行HTTP的客户端，可以在节省带宽的同时更加快速的加载网络内容：

* 它支持HTTP/2 、允许链接到同一主机的请求公用一个Scoket
* 如果 HTTP/2 不可用，通过连接池减少请求的延迟
* 通过GZIP压缩减少传输数据的大小
* 通过缓存避免了网络重复请求

OkHttp的简单使用方法如下：

```
fun okHttpCall(name: String) {
    //创建OkHttpClient对象
    val client = OkHttpClient()
    //创建Request请求对象
    var request: Request = Request.Builder()
        .url("https://api.github.com/users/$name/repos")
        .method("GET", null)
        .build()
    //同步请求,获取获取Response对象
    val response = client.newCall(request).execute()
    println(response.body()?.source()?.readUtf8())

    ////异步请求,获取获取Response对象
    client.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            e.printStackTrace()
        }

        override fun onResponse(call: Call, response: Response) {
            println(response.body()?.source()?.readUtf8())
        }
    })
}
```
OkHttp请求网络的过程也很简介：创建OkHttpClient对象；创建Request请求对象；获取Response对象。

大致可以猜测到：Request里包含了我们的请求信息，包括url、请求方法、参数等；OkHttpClient则负责发送我们的请求信息到目标服务器，并接收返回信息Response。

### 基本角色介绍

上文的示例代码中，我们展示了OkHttp的同步和异步两种请求。首先从最简单的同步请求看起，精简后流程如下：

```
fun synchronizeCall(name: String) {
    var request: Request = Request.Builder()
        .url("https://api.github.com/users/$name/repos")
        .method("GET", null)
        .build()
    val client = OkHttpClient()
    val response = client.newCall(request).execute()
    println(response.body()?.source()?.readUtf8())
}
```

流程分为四步：

* 创建Request请求体；
* 获取OkHttpClient实例；
* 发送请求并得到Response；
* 解析Response数据。

首先看一下Request的代码：

#### Request
```
public final class Request {
  //请求路径
  final HttpUrl url;
  //请求方法，如：GET、POST..
  final String method;
  //请求头
  final Headers headers;
  //请求体
  final @Nullable RequestBody body;

  Request(Builder builder) {
    this.url = builder.url;
    this.method = builder.method;
    this.headers = builder.headers.build();
    this.body = builder.body;
    this.tags = Util.immutableMap(builder.tags);
  }

  public HttpUrl url() {
    return url;
  }

  //..
  public @Nullable String header(String name) {
    return headers.get(name);
  }

  public List<String> headers(String name) {
    return headers.values(name);
  }

  public @Nullable RequestBody body() {
    return body;
  }
  //..
  public Builder newBuilder() {
    return new Builder(this);
  }

  public static class Builder {
    @Nullable HttpUrl url;
    String method;
    Headers.Builder headers;
    @Nullable RequestBody body;

    public Builder() {
      this.method = "GET";
      this.headers = new Headers.Builder();
    }

    Builder(Request request) {
      this.url = request.url;
      this.method = request.method;
      this.body = request.body;
      this.tags = request.tags.isEmpty()
          ? Collections.<Class<?>, Object>emptyMap()
          : new LinkedHashMap<>(request.tags);
      this.headers = request.headers.newBuilder();
    }

    public Builder url(HttpUrl url) {
      if (url == null) throw new NullPointerException("url == null");
      this.url = url;
      return this;
    }

  }
}
```
Request的逻辑和职责很明确——它包含了所有的请求信息，是请求的载体，主要包括了请求的Url地址、请求方法、请求头和请求体等信息。采用了建造者模式，通过静态内部类Builder来创建。

#### OkHttpClient
接下来看OkHttpClient，它也采用了建造者模式。

```
public class OkHttpClient implements Cloneable, Call.Factory, WebSocket.Factory {

  //调度器
  final Dispatcher dispatcher;
  //拦截器
  final List<Interceptor> interceptors;
  final List<Interceptor> networkInterceptors;
  //
  final EventListener.Factory eventListenerFactory;
  final ProxySelector proxySelector;
  //cookie设置
  final CookieJar cookieJar;
  //缓存设置
  final @Nullable Cache cache;
  final @Nullable InternalCache internalCache;
  final SocketFactory socketFactory;
  final SSLSocketFactory sslSocketFactory;
  final CertificateChainCleaner certificateChainCleaner;
  final HostnameVerifier hostnameVerifier;
  final CertificatePinner certificatePinner;
  final Authenticator proxyAuthenticator;
  final Authenticator authenticator;
  //连接池
  final ConnectionPool connectionPool;
  //DNS设置
  final Dns dns;
  //是否从HTTP重定向到HTTPS
  final boolean followSslRedirects;
  final boolean followRedirects;
  final boolean retryOnConnectionFailure;
  //连接、读写等超时时间
  final int callTimeout;
  final int connectTimeout;
  final int readTimeout;
  final int writeTimeout;

  public OkHttpClient() {
    this(new Builder());
  }

  OkHttpClient(Builder builder) {
    this.dispatcher = builder.dispatcher;
    this.proxy = builder.proxy;
    this.protocols = builder.protocols;
    //..
    this.connectTimeout = builder.connectTimeout;
    this.readTimeout = builder.readTimeout;
    this.writeTimeout = builder.writeTimeout;
    this.pingInterval = builder.pingInterval;

  }

  @Override public Call newCall(Request request) {
    return RealCall.newRealCall(this, request, false /* for web socket */);
  }
  public static final class Builder {
    Dispatcher dispatcher;
    /*
    * 参数基本和外部类保持一直
    */
    int callTimeout;
    int connectTimeout;
    int readTimeout;
    int writeTimeout;
    int pingInterval;

    public Builder() {
      dispatcher = new Dispatcher();
      protocols = DEFAULT_PROTOCOLS;
      connectionSpecs = DEFAULT_CONNECTION_SPECS;
      eventListenerFactory = EventListener.factory(EventListener.NONE);
      //..
      readTimeout = 10_000;
      writeTimeout = 10_000;
      pingInterval = 0;
    }

    Builder(OkHttpClient okHttpClient) {
      this.dispatcher = okHttpClient.dispatcher;
      //..
      this.pingInterval = okHttpClient.pingInterval;
    }
  }
}
```

删减过后的OkHttpClient代码如上。它有超多成员变量，主要包括：

* 发送请求相关：调度器、连接池和拦截器等；
* Http基础配置：缓存、DNS、代理、重定向、超时等。

它和Request一样，都是采用了建造者模式，并持有了大量的配置信息。不同之处在于Request的配置是服务于一次特定的网络请求的。而OkHttpClient则是主要作用于Http的基础配置，是针对“一批”请求的（当然，你也可以为每个请求都穿件一个OkHttpClient实例，并采用不同的配置）。类比到现实中具象的事物，可以把OkHttpClient理解为铁路系统，而Request则是一列一列的车厢。铁路系统负责告知车厢在那条线跑（线程调度），规定装货卸货和运输时间（读写和传输超时时间）等一些公共规则。但是拉什么货（head、body）去哪里（url），用什么车拉（方法），则完全交由列车（Request）车厢。当然，铁路网合理的运行，离不开调度部门的统一调度。这些工作都是交由Dispatcher执行的。


#### Dispatcher

调度器Dispatcher由OkHttpClient持有，它负责异步请求的执行策略。每个调度器内部都有一个ExecutorService用来执行`Call`。由此可预测，Dispatcher是通过线程池进行线程调度的。它的主要结构如下：

```

public final class Dispatcher {
  //同一时间允许并发执行网络请求数量
   private int maxRequests = 64;
  //同一Host下的最大同时请求数
  private int maxRequestsPerHost = 5;
  private @Nullable Runnable idleCallback;

  //线程池
  private @Nullable ExecutorService executorService;

  //已经做好准备，等待发起的异步请求队列
  private final Deque<AsyncCall> readyAsyncCalls = new ArrayDeque<>();

  //正在运行的异步请求队列
  private final Deque<AsyncCall> runningAsyncCalls = new ArrayDeque<>();

  //正在运行的同步请求队列
  private final Deque<RealCall> runningSyncCalls = new ArrayDeque<>();

  //获取线程池，如果是第一次获取executorService为空，那么则先创建
  public synchronized ExecutorService executorService() {
    if (executorService == null) {
      executorService = new ThreadPoolExecutor(0, Integer.MAX_VALUE, 60, TimeUnit.SECONDS,
          new SynchronousQueue<Runnable>(), Util.threadFactory("OkHttp Dispatcher", false));
    }
    return executorService;
  }
}
```
调度器里维持着几个ArrayDeque双端队列，用来存储不同的请求（同步/异步/执行状态）。并且会提供一个线程池。该线程池没有核心线程，最大线程数Integer.MAX_VALUE可以理解为没有上限。并采用SynchronousQueue作为阻塞队列。

不仅如此，铁路系统（OkHttpClient）还要给车厢提供运货的动力——车头（Call）。我们需要把装满货物的车厢挂在车头上，并命令车头发车才能开始物资运输。

#### Call
我们需要通过`client.newCall(request)`获取到Call之后再调用`execute()`方法才能执行一次同步请求。`client.newCall `方法如下：

```
@Override public Call newCall(Request request) {
    return RealCall.newRealCall(this, request, false /* for web socket */);
}
```

该方法返回了一个RealCall实例，它实现Call接口。

```
public interface Call extends Cloneable {
  Request request();
  //同步请求
  Response execute() throws IOException;
  //异步请求
  void enqueue(Callback responseCallback);
  void cancel();
  //请求是否已经执行
  boolean isExecuted();
  boolean isCanceled();
}
```
该接口定义了同步和异步请求、请求是否正在执行和克隆等方法，这里是真正执行请求的地方。它只有一个实现就是RealCall。接下来的工作就是调度器通知开车了，不过整个工作流程我们稍后会专门分析。这里我们首先看一下Response


#### Response
无论是同步请求还是异步请求，最终拿到的都是Response。它有一下成员变量：

```
public final class Response implements Closeable {
  final Request request;
  final Protocol protocol;
  final int code;
  final String message;
  final @Nullable Handshake handshake;
  final Headers headers;
  final @Nullable ResponseBody body;
  final @Nullable Response networkResponse;
  final @Nullable Response cacheResponse;
  final @Nullable Response priorResponse;
  final long sentRequestAtMillis;
  final long receivedResponseAtMillis;

  private volatile @Nullable CacheControl cacheControl; // Lazily initialized.
}
```
可见，Response包含着我们基本请求信息Request，以及Http返回的头部信息、状态码code和message，以及我们需要的正文body。ResponseBody本身是一个从服务器到我们客户端的一个一次性流，其中的正文以字节的形式存储。在它的一个实现RealResponseBody中，字节以缓存的形式存储在`BufferedSource`。这里就涉及到了Okio的知识，详情可以看[Okio源码和流程分析](https://juejin.cn/post/7098916408932237349)中的[Buffer缓存的读写操作](https://juejin.cn/post/7098916408932237349#heading-6)。

### 主流程分析

上文对OkHttp的各个角色进行了简单的介绍。接下来我们开始对网络请求的主流程进行分析。重点在于线程的分配、请求的发起和请求结果响应。至于请求Request的创建，Client的创建就不深入分析了。

首先看同步请求，也就是`RealCall.execute()`方法：

```
@Override public Response execute() throws IOException {
    synchronized (this) {
        //判断是否已经被执行了, 确保只能执行一次，如果已经执行过，则抛出异常
        if (executed) throw new IllegalStateException("Already Executed");
        executed = true;
    }
    captureCallStackTrace();
    timeout.enter();
    //开启请求监听
    eventListener.callStart(this);
    try {
    	  //将当前的Call加入到调度器的runningSyncCalls队列中
    	  //表明当前请求正在进行中
        client.dispatcher().executed(this);
        //发起请求并获取返回结果
        Response result = getResponseWithInterceptorChain();
        if (result == null) throw new IOException("Canceled");
        return result;
    } catch (IOException e) {
        e = timeoutExit(e);
        eventListener.callFailed(this, e);
        throw e;
    } finally {
        client.dispatcher().finished(this);
    }
}
```

代码逻辑已经卸载注释里了。Dispatcher只是将Call 添加到runningSyncCalls中并在请求结束后将它移除。

我们暂时不分析`getResponseWithInterceptorChain()`里的逻辑，因为异步操作最终也是在这里执行的，稍后我们会重点对它进行分析。先看一下异步请求的流程.。也就是`RealCall.enqueue()`方法：

```
@Override public void enqueue(Callback responseCallback) {
    synchronized (this) {
        if (executed) throw new IllegalStateException("Already Executed");
        executed = true;
    }
    captureCallStackTrace();
    //开启请求监听
    eventListener.callStart(this);
    //创建AsyncCall对象，通过调度器enqueue方法加入到readyAsyncCalls队列中
    client.dispatcher().enqueue(new AsyncCall(responseCallback));
}
```
该方法接受一个Callback对象，该对象通常以匿名内部类的方式创建，它有两个方法`onResponse`和`onFailure`分别对应请求成功和失败。方法首先判断请求是否已经执行了，并将Call的状态标记为已经执行。紧接着开启请求监听。这里的操作和同步请求里的基本一致。接下来，就和同步请求里的不同了——创建一个`AsyncCall`的实例，通过OkHttpClient执行调度器的`enqueue`方法。接下来就是线程调度和请求的执行流程了。

#### 线程的调度

`Dispatcher.enqueue`的代码和流程如下：

```
void enqueue(AsyncCall call) {
    synchronized (this) {
        readyAsyncCalls.add(call);
    }
    promoteAndExecute();
}

private boolean promoteAndExecute() {
    assert (!Thread.holdsLock(this));

    List<AsyncCall> executableCalls = new ArrayList<>();
    boolean isRunning;
    synchronized (this) {
        for (Iterator<AsyncCall> i = readyAsyncCalls.iterator(); i.hasNext(); ) {
        AsyncCall asyncCall = i.next();

        if (runningAsyncCalls.size() >= maxRequests) break; // Max capacity.
        if (runningCallsForHost(asyncCall) >= maxRequestsPerHost) continue; // Host max capacity.

        i.remove();
        executableCalls.add(asyncCall);
        runningAsyncCalls.add(asyncCall);
    }
        isRunning = runningCallsCount() > 0;
    }

    for (int i = 0, size = executableCalls.size(); i < size; i++) {
        AsyncCall asyncCall = executableCalls.get(i);
        asyncCall.executeOn(executorService());
    }
    return isRunning;
}
```

`Dispatcher.enqueue`方法的逻辑就是将将AsyncCall加入到readyAsyncCalls队列中，表明AsyncCall已经准备好要执行了，然后调用promoteAndExecute方法，promoteAndExecute方法会遍历readyAsyncCalls中所有待执行的AsyncCall。在遍历的同时，会检查当前需要发起请求的数量(64)和当前指向同一Host的请求数(5)，r如果条件满足，则将Call依次取出来。紧接着就是执行`AsyncCall.executeOn`方法了。

注：这里可能有一个疑问。为什么要多次一举借助readyAsyncCalls队列，而且为了确保线程安全，还要对它加锁。目的是**为了方便统计当前所有正在运行的请求总数以及统一Host的请求数量以及能够取消所有请求。**

接下来的流程又要回到AsyncCall中了，AsyncCall是RealCall的一个内部类。每次异步请求时都会降一个AsyncCall实例交给Dispatcher调度器。调度器在对它进行调度后，在通过`AsyncCall.executeOn`方法执行这个AsyncCall的时候，会通过`executorService()`传给它一个线程池实例（该方法在Dispatcher介绍中已经讲过了）。

`AsyncCall.executeOn`源码和流程如下：

```
final class AsyncCall extends NamedRunnable {
    //请求解雇回调
    private final Callback responseCallback;
    

    AsyncCall(Callback responseCallback) {
        super("OkHttp %s", redactedUrl());
        this.responseCallback = responseCallback;
    }

    //开始执行，将任务加入到线程池中
    void executeOn(ExecutorService executorService) {
        assert (!Thread.holdsLock(client.dispatcher()));
        boolean success = false;
        try {
            executorService.execute(this);
            success = true;
        } catch (RejectedExecutionException e) {
            InterruptedIOException ioException = new InterruptedIOException("executor rejected");
            ioException.initCause(e);
            eventListener.callFailed(RealCall.this, ioException);
            responseCallback.onFailure(RealCall.this, ioException);
        } finally {
            if (!success) {
                client.dispatcher().finished(this); // This call is no longer running!
            }
        }
    }

    //任务执行的地方
    @Override protected void execute() {
        boolean signalledCallback = false;
        timeout.enter();
        try {
            Response response = getResponseWithInterceptorChain();
            if (retryAndFollowUpInterceptor.isCanceled()) {
                signalledCallback = true;
                responseCallback.onFailure(RealCall.this, new IOException("Canceled"));
            } else {
                signalledCallback = true;
                responseCallback.onResponse(RealCall.this, response);
            }
        } catch (IOException e) {
            e = timeoutExit(e);
            if (signalledCallback) {
                // Do not signal the callback twice!
                Platform.get().log(INFO, "Callback failure for " + toLoggableString(), e);
            } else {
                eventListener.callFailed(RealCall.this, e);
                responseCallback.onFailure(RealCall.this, e);
            }
        } finally {
            client.dispatcher().finished(this);
        }
    }
}
```
`AsyncCall`的父类NamedRunnable是一个抽象类，它实现了`Runnable`接口，并重写了run方法。在run方法中调用了execute方法：

```
public abstract class NamedRunnable implements Runnable {
  protected final String name;

  public NamedRunnable(String format, Object... args) {
    this.name = Util.format(format, args);
  }

  @Override public final void run() {
    String oldName = Thread.currentThread().getName();
    Thread.currentThread().setName(name);
    try {
      execute();
    } finally {
      Thread.currentThread().setName(oldName);
    }
  }

  protected abstract void execute();
}
```

这是一个典型的线程池操作——AsyncCall本身就是一个Runable，它的executeOn方法讲自己作为任务提交给Dispatcher调度器提供的线程池中。一旦线程池开始执行AsyncCall这个`Runable`任务时，它重写的run方法就会执行`AsyncCall. execute()`方法，而`execute()`中和上文中的同步执行请求一样，都是借助`getResponseWithInterceptorChain`来执行的。

总结一下：同步请求和异步请求本质上都是借助RealCall，并由调度器管理，最终通过`getResponseWithInterceptorChain`来发起。而异步请求多了一步线程的调度：借助RealCall里实现了Runable接口的内部类AsyncCall，将任务添加到调度器Dispatcher提供的线程池里，从而将`getResponseWithInterceptorChain`放到线程池中执行实现线程的切换。到此，整体流程如下：

![](https://github.com/RommelLiang/karl/blob/main/NetWork/img/thread_dispatch.jpg?raw=true)

#### 责任链模式和拦截器

OkHttp真正的核心是它的拦截器。Interceptor可以说是OkHttp最重要的东西了，它不仅负责OkHttp的核心功能。而且还提供了一些用户自定义的功能。相信你一定使用过日志拦截器`HttpLoggingInterceptor`，使用如下代码就可以为OkHttp添加日志拦截器`val client = OkHttpClient().newBuilder().addInterceptor(HttpLoggingInterceptor()).build()`。
你也可以自己实现`Interceptor`接口自定义拦截器，例如自定义一个打印请求信息的拦截器：

```
class RequestInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        Log.e("RequestInterceptor","-----------${Thread.currentThread().name}")
        Log.e("Request-Method","-----------${request.method()}")
        Log.e("Request-Host","-----------${request.url()}")
        for (headName in request.headers().names()){
            Log.e("Request-Head:$headName","-----------${request.header(headName)}")
        }
        Log.e("Request-Body","-----------${request.body()}")
        return chain.proceed(request)
    }
}
```

只需要重写`intercept `方法，并通过`addInterceptor`将拦截器添加到`OkHttpClient `中即可。拦截器不仅可以提供给用户在网络请求发起前做一些统一的事情，例如打印信息，添加cookie，日志记录、请求拦截等。整个网络请求都是借助拦截器进行的，OkHttp借助拦截器，通过[责任链模式](https://www.runoob.com/design-pattern/chain-of-responsibility-pattern.html)巧妙的将网络请求的各个任务拆分开来，每个Interceptor只负责自己关心的操作，它的定义如下：

```
public interface Interceptor {
  Response intercept(Chain chain) throws IOException;

  interface Chain {
    
  }  
}

```

Interceptor是一个接口，只定义了一个方法`intercept(Chain chain)`和一个内部接口。

现在看一下上文中提到过的`getResponseWithInterceptorChain`方法：

```
Response getResponseWithInterceptorChain() throws IOException {
    // Build a full stack of interceptors.
    List<Interceptor> interceptors = new ArrayList<>();
    //注释一
    interceptors.addAll(client.interceptors());
    interceptors.add(retryAndFollowUpInterceptor);
    interceptors.add(new BridgeInterceptor(client.cookieJar()));
    interceptors.add(new CacheInterceptor(client.internalCache()));
    interceptors.add(new ConnectInterceptor(client));
    //注释二
    if (!forWebSocket) {
        interceptors.addAll(client.networkInterceptors());
    }
    interceptors.add(new CallServerInterceptor(forWebSocket));

    Interceptor.Chain chain = new RealInterceptorChain(interceptors, null, null, null, 0,
        originalRequest, this, eventListener, client.connectTimeoutMillis(),
        client.readTimeoutMillis(), client.writeTimeoutMillis());

    return chain.proceed(originalRequest);
}
```

代码逻辑主要逻辑是将所有的Interceptor作为一个集合，并创建一个`RealInterceptorChain `对象，然后执行它的`proceed `方法。其中，注释一处的`client.interceptors()`就是我们通过`addInterceptor`添加的拦截器，而`client.networkInterceptors()`则是通过`addNetworkInterceptor`方法添加的。它们的区别我们稍后再讲。首先看一下这些拦截器都是干什么的：

* 自定义拦截器（应用拦截器）：提供给用户的定制的拦截器。
* 失败和重试拦截器（RetryAndFollowUpInterceptor）：负责请求失败的重试工作与重定向，同时它会对连接做一些初始化工作。
* 桥接拦截器（BridgeInterceptor）：主要用来构造请求，客户端与服务器之间的沟通桥梁，负责将用户构建的请求转换为服务器需要的请求。
* 缓存拦截器（CacheInterceptor）：通过OkHttpClient.cache来配置缓存，缓存拦截器通过CacheStrategy来判断是使用网络还是缓存来构建response。
* 连接拦截器（ConnectInterceptor）：负责客户端与服务器真正建立起连接。
* 网络拦截器 （networkInterceptors）：和interceptors一样也是由用户自定义的，它们的不同源自它们的位置不同。**应用拦截器处在拦截器的首要位置，每次请求都必定会执行，而且只会执行一次。而网络拦截位置比较靠后，它可能因为异常而不会执行，同时，也可能由于RetryAndFollowUpInterceptor不断重试，导致执行多次。**
* 网络请求拦截器（CallServerInterceptor）：负责发起网络请求解析网络返回的数据


接下来分析`RealInterceptorChain`是怎么处理`Interceptor`的。`RealInterceptorChain`实现了`Interceptor.Chain`接口，该接口定义在`Interceptor`接口中，`RealInterceptorChain`删减后，串联起所有拦截器的关键代码如下：

```
public final class RealInterceptorChain implements Interceptor.Chain {
  private final List<Interceptor> interceptors;
  private final StreamAllocation streamAllocation;
  private final RealConnection connection;
  private final int index;

  //构造方法
  public RealInterceptorChain(List<Interceptor> interceptors, StreamAllocation streamAllocation,
      HttpCodec httpCodec, RealConnection connection, int index, Request request, Call call,
      EventListener eventListener, int connectTimeout, int readTimeout, int writeTimeout) {
    this.interceptors = interceptors;
    this.connection = connection;
    this.streamAllocation = streamAllocation;
    this.index = index;
    this.request = request;
  }


  @Override public Response proceed(Request request) throws IOException {
    return proceed(request, streamAllocation, httpCodec, connection);
  }

  public Response proceed(Request request, StreamAllocation streamAllocation, HttpCodec httpCodec,
      RealConnection connection) throws IOException {
    //计算index角标是否小于interceptors的
    if (index >= interceptors.size()) throw new AssertionError();

    calls++;

    //创建一个新的RealInterceptorChain实例，参数基本不变
    //但是index和call都自增传入新的值（+1）
    RealInterceptorChain next = new RealInterceptorChain(interceptors, streamAllocation, httpCodec,
        connection, index + 1, request, call, eventListener, connectTimeout, readTimeout,
        writeTimeout);
    //根据角标回去到拦截器，然后执行拦截器的intercept方法
    Interceptor interceptor = interceptors.get(index);
    Response response = interceptor.intercept(next);
 
    return response;
  }
}
```

拦截器的执行时靠着`RealInterceptorChain.proceed`方法推动的。创建`RealInterceptorChain`实例，并调用它的`proceed `方法（始于`getResponseWithInterceptorChain`），`proceed`会根据一个累加的角标index获取到`interceptors`集合中对应的拦截器，同时会创建一个新的`RealInterceptorChain `，并将它作为实参执行拦截器的`Interceptor.intercept `方法。而`Interceptor.intercept`返回值类型为`Response`，需要通过`RealInterceptorChain.proceed`方法获取。以此实现对新创建的`RealInterceptorChain `的`proceed`的调用，从而实现对拦截器的链式调用。整体的调用时序图大致如下(省略掉用户自定义的拦截器)：

![](https://github.com/RommelLiang/karl/blob/main/NetWork/img/intercept.jpg?raw=true)

接下来看一下各个拦截器的功能，拦截器的核心代码都在`Interceptor.intercept`中，所以接下来的重点就是各个拦截器的`Interceptor.intercept`方法。首先从`RetryAndFollowUpInterceptor`入手：

#### RetryAndFollowUpInterceptor

从它的名字可以得知：RetryAndFollowUpInterceptor负责请求失败的重试工作与重定向的后续请求工作。其代码如下（[完整代码](https://github.com/square/okhttp/blob/parent-3.12.0/okhttp/src/main/java/okhttp3/internal/http/RetryAndFollowUpInterceptor.java)）：

```
@Override public Response intercept(Chain chain) throws IOException {
    Request request = chain.request();
    RealInterceptorChain realChain = (RealInterceptorChain) chain;
    Call call = realChain.call();
    EventListener eventListener = realChain.eventListener();

    //第①步
    //创建StreamAllocation实例
    StreamAllocation streamAllocation = new StreamAllocation(client.connectionPool(),
        createAddress(request.url()), call, eventListener, callStackTrace);
    this.streamAllocation = streamAllocation;

    int followUpCount = 0;
    Response priorResponse = null;
    while (true) {
        if (canceled) {
            streamAllocation.release();
            throw new IOException("Canceled");
        }

        Response response;
        boolean releaseConnection = true;
        try {
            //第②步
            //通过Chain调用下一个拦截器
            response = realChain.proceed(request, streamAllocation, null, null);
            releaseConnection = false;
        } catch (RouteException e) {
            // The attempt to connect via a route failed. The request will not have been sent.
            if (!recover(e.getLastConnectException(), streamAllocation, false, request)) {
                throw e.getFirstConnectException();
            }
            releaseConnection = false;
            continue;
        } catch (IOException e) {
            // An attempt to communicate with a server failed. The request may have been sent.
            boolean requestSendStarted = !(e instanceof ConnectionShutdownException);
            if (!recover(e, streamAllocation, requestSendStarted, request)) throw e;
            releaseConnection = false;
            continue;
        } finally {
            // We're throwing an unchecked exception. Release any resources.
            if (releaseConnection) {
                streamAllocation.streamFailed(null);
                streamAllocation.release();
            }
        }

        //..

        Request followUp;
        try {
            //第③步
            //判断是否需要重新
            followUp = followUpRequest(response, streamAllocation.route());
        } catch (IOException e) {
            streamAllocation.release();
            throw e;
        }

        if (followUp == null) {
            //第④步
            //无需重新请求，释放资源并返回response
            streamAllocation.release();
            return response;
        }


        //记录重定向次数，大于21次后抛出异常
        if (++followUpCount > MAX_FOLLOW_UPS) {
            streamAllocation.release();
            throw new ProtocolException("Too many follow-up requests: " + followUpCount);
        }

        if (followUp.body() instanceof UnrepeatableRequestBody) {
            streamAllocation.release();
            throw new HttpRetryException("Cannot retry streamed HTTP body", response.code());
        }

        if (!sameConnection(response, followUp.url())) {
            streamAllocation.release();
            streamAllocation = new StreamAllocation(client.connectionPool(),
            createAddress(followUp.url()), call, eventListener, callStackTrace);
            this.streamAllocation = streamAllocation;
        } else if (streamAllocation.codec() != null) {
            throw new IllegalStateException("Closing the body of " + response
                    + " didn't close its backing stream. Bad interceptor?");
        }
        //重新设置请求
        request = followUp;
        priorResponse = response;
    }
}
```

RetryAndFollowUpInterceptor通过开启一个while循环实现请求重试的功能，当满足一下条件时会继续循环：

* 请求内部抛出异常时（后面的拦截器发生异常），判定是否需要重试（第②步的try异常捕获）
* 根据响应结果的返回码，判断是否需要重新构建新请求并发送（第③步的返回结果判断）

并用MAX_FOLLOW_UPS限制了重试的次数。注意上面代码中第二步后所有的代码都要等所有拦截器都执行完并返回结果或者抛出异常才能够被执行。需要注意的是OkHttpClien的retryOnConnectionFailure参数设置为false或者请求的body已经发送出去了，则不会重试。

注意这里就是Interceptors和NetworkInterceptors的区别的根源所在：Interceptors在RetryAndFollowUpInterceptor前面，而NetworkInterceptors在RetryAndFollowUpInterceptor的后面。Interceptors处在所有拦截器的前面，里面的拦截器肯定会执行，而且只会执行一次。但是就不同了，它可能不会执行(在它之前的拦截器发生了异常，请求终止了)，也可能被执行多次（RetryAndFollowUpInterceptor重试或者重定向了多次）。

另外，这里需要注意`RetryAndFollowUpInterceptor`不仅负责重试和重定向，它还创建了`StreamAllocation`实例，并通过`RealInterceptorChain.proceed()`方法将它传递给后续的拦截器和RealInterceptorChain。也正是从`RetryAndFollowUpInterceptor `拦截器，RealInterceptorChain里的成员变量streamAllocation才开始不为空。StreamAllocation内维持着连接池，负责创建管理连接，稍后我们会专门有一节对它进行分析。

#### BridgeInterceptor

BridgeInterceptor([完整代码](https://github.com/square/okhttp/blob/parent-3.12.0/okhttp/src/main/java/okhttp3/internal/http/BridgeInterceptor.java))是客户端与服务器之间的沟通桥梁，负责将用户构建的请求转换为服务器需要的请求：

```
@Override public Response intercept(Chain chain) throws IOException {
    Request userRequest = chain.request();
    Request.Builder requestBuilder = userRequest.newBuilder();

    //设置请求头的各种参数
    RequestBody body = userRequest.body();
    if (body != null) {
        MediaType contentType = body.contentType();
        if (contentType != null) {
            requestBuilder.header("Content-Type", contentType.toString());
        }

	 //传输长度和编码设置
        long contentLength = body.contentLength();
        if (contentLength != -1) {
            requestBuilder.header("Content-Length", Long.toString(contentLength));
            requestBuilder.removeHeader("Transfer-Encoding");
        } else {
            requestBuilder.header("Transfer-Encoding", "chunked");
            requestBuilder.removeHeader("Content-Length");
        }
    }

    if (userRequest.header("Host") == null) {
        requestBuilder.header("Host", hostHeader(userRequest.url(), false));
    }

    if (userRequest.header("Connection") == null) {
        requestBuilder.header("Connection", "Keep-Alive");
    }

    boolean transparentGzip = false;
    if (userRequest.header("Accept-Encoding") == null && userRequest.header("Range") == null) {
        transparentGzip = true;
        requestBuilder.header("Accept-Encoding", "gzip");
    }

    List<Cookie> cookies = cookieJar.loadForRequest(userRequest.url());
    //添加Cookie信息
    if (!cookies.isEmpty()) {
        requestBuilder.header("Cookie", cookieHeader(cookies));
    }

    if (userRequest.header("User-Agent") == null) {
        requestBuilder.header("User-Agent", Version.userAgent());
    }

    Response networkResponse = chain.proceed(requestBuilder.build());

    HttpHeaders.receiveHeaders(cookieJar, userRequest.url(), networkResponse.headers());
    //处理Response
    Response.Builder responseBuilder = networkResponse.newBuilder()
        .request(userRequest);

    if (transparentGzip
        && "gzip".equalsIgnoreCase(networkResponse.header("Content-Encoding"))
        && HttpHeaders.hasBody(networkResponse)) {
        GzipSource responseBody = new GzipSource(networkResponse.body().source());
        Headers strippedHeaders = networkResponse.headers().newBuilder()
            .removeAll("Content-Encoding")
            .removeAll("Content-Length")
            .build();
        responseBuilder.headers(strippedHeaders);
        String contentType = networkResponse.header("Content-Type");
        responseBuilder.body(new RealResponseBody(contentType, -1L, Okio.buffer(responseBody)));
    }

    return responseBuilder.build();
}
```

如代码中的注释，BridageInterceptor 拦截器的具体功能如下：

1. 设置请求头信息，例如Content-Type、Host、Keep-alive等
2. 添加Cookie和设置内容长度（Content-Length）和编码（Transfer-Encoding）
3. 将服务器返回的Response进行一些转换，提高可读性
4. 处理服务器压缩后的response

#### CacheInterceptor

CacheInterceptor主要是处理HTTP请求缓存的，通过缓存拦截器可以有效的使用缓存减少网络请求：

```
@Override public Response intercept(Chain chain) throws IOException {
    Response cacheCandidate = cache != null
    ? cache.get(chain.request())
    : null;

    long now = System.currentTimeMillis();

    //创建一个缓存策略，用来规定怎么使用缓存
    CacheStrategy strategy = new CacheStrategy.Factory(now, chain.request(), cacheCandidate).get();
    //为空表示不使用网络，反之，则表示使用网络
    Request networkRequest = strategy.networkRequest;
    //为空表示不使用缓存，反之，则表示使用缓存
    Response cacheResponse = strategy.cacheResponse;

    if (cache != null) {
        cache.trackResponse(strategy);
    }

    if (cacheCandidate != null && cacheResponse == null) {
        closeQuietly(cacheCandidate.body()); // The cache candidate wasn't applicable. Close it.
    }

    // 如果网络被禁止，切缓存为空，直接返回一个空的Response
    if (networkRequest == null && cacheResponse == null) {
        return new Response.Builder()
            .request(chain.request())
            .protocol(Protocol.HTTP_1_1)
            .code(504)
            .message("Unsatisfiable Request (only-if-cached)")
            .body(Util.EMPTY_RESPONSE)
            .sentRequestAtMillis(-1L)
            .receivedResponseAtMillis(System.currentTimeMillis())
            .build();
    }

    // 如果网络被禁止，缓存不为空，直接返回缓存
    if (networkRequest == null) {
        return cacheResponse.newBuilder()
            .cacheResponse(stripBody(cacheResponse))
            .build();
    }

    //网络没被禁止，使用正常流程，通过访问网络获得数据
    Response networkResponse = null;
    try {
        networkResponse = chain.proceed(networkRequest);
    } finally {
        // If we're crashing on I/O or otherwise, don't leak the cache body.
        if (networkResponse == null && cacheCandidate != null) {
            closeQuietly(cacheCandidate.body());
        }
    }

    // 如果本地已经有缓存，切返回的状态码为304，则对缓存进行一些更新，例如head信息，请求和接受的时间等
    if (cacheResponse != null) {
        if (networkResponse.code() == HTTP_NOT_MODIFIED) {
            Response response = cacheResponse.newBuilder()
                .headers(combine(cacheResponse.headers(), networkResponse.headers()))
                .sentRequestAtMillis(networkResponse.sentRequestAtMillis())
                .receivedResponseAtMillis(networkResponse.receivedResponseAtMillis())
                .cacheResponse(stripBody(cacheResponse))
                .networkResponse(stripBody(networkResponse))
                .build();
            networkResponse.body().close();

            // Update the cache after combining headers but before stripping the
            // Content-Encoding header (as performed by initContentStream()).
            cache.trackConditionalCacheHit();
            cache.update(cacheResponse, response);
            return response;
        } else {
            closeQuietly(cacheResponse.body());
        }
    }

    Response response = networkResponse.newBuilder()
        .cacheResponse(stripBody(cacheResponse))
        .networkResponse(stripBody(networkResponse))
        .build();

    if (cache != null) {
        if (HttpHeaders.hasBody(response) && CacheStrategy.isCacheable(response, networkRequest)) {
            // 保存缓存
            CacheRequest cacheRequest = cache.put(response);
            return cacheWritingResponse(cacheRequest, response);
        }

        if (HttpMethod.invalidatesCache(networkRequest.method())) {
            try {
                cache.remove(networkRequest);
            } catch (IOException ignored) {
              
            }
        }
    }

    return response;
}
```

CacheInterceptor会通过Request尝试到Cache中拿缓存，默认没有缓存，需要通过`OkHttpClient.setInternalCache`方法设置。这些缓存会通过`CacheInterceptor`的构造方法做饭参数被传递到拦截器中。大致流程如下：

* 如果缓存为空，而且禁用了网络(可通过`Request.cacheControl`设置)则直接返回一个返回码为504的Response。
* 有缓存而且禁用网络，则返回缓存。
* 如果以上都没命中，则走网络请求流程（chain.proceed，执行后续的拦截器）。
* 如果网络请求返回304而且本地有缓存，则直接使用本地缓存
* 之后就是构建Response，如果OkHttpClient中配置了缓存，则将Resposne缓存起来，并返回给调用者

#### ConnectInterceptor
ConnectInterceptor和CallServerInterceptor可以说是最重要的两个拦截器了，ConnectInterceptor负责Dns解析和Socket连接。它的代码非常简单：

```

@Override public Response intercept(Chain chain) throws IOException {
    RealInterceptorChain realChain = (RealInterceptorChain) chain;
    Request request = realChain.request();
    StreamAllocation streamAllocation = realChain.streamAllocation();

    // We need the network to satisfy this request. Possibly for validating a conditional GET.
    boolean doExtensiveHealthChecks = !request.method().equals("GET");
    HttpCodec httpCodec = streamAllocation.newStream(client, chain, doExtensiveHealthChecks);
    RealConnection connection = streamAllocation.connection();

    return realChain.proceed(request, streamAllocation, httpCodec, connection);
}
```
仅仅是获取到调用链中的streamAllocation，然后通过streamAllocation获取到RealConnection即进入到下一个拦截器中了。拦截器基本上将所有的操作都交给了streamAllocation，我们稍后再对它进行详细的分析。这里只展示一下这些拦截器的主要职责。

#### CallServerInterceptor
CallServerInterceptor是最后一个拦截器了，前面的拦截器将请求都封装好了，客户端和服务端的连接也打通了。CallServerInterceptor就是进行数据传输的地方了：

```
@Override public Response intercept(Chain chain) throws IOException {
    //获取在调用链中传递的HttpCodec、StreamAllocation和RealConnection
    RealInterceptorChain realChain = (RealInterceptorChain) chain;
    HttpCodec httpCodec = realChain.httpStream();
    StreamAllocation streamAllocation = realChain.streamAllocation();
    RealConnection connection = (RealConnection) realChain.connection();
    Request request = realChain.request();

    long sentRequestMillis = System.currentTimeMillis();

    realChain.eventListener().requestHeadersStart(realChain.call());
    //向服务器发送请求头
    httpCodec.writeRequestHeaders(request);
    realChain.eventListener().requestHeadersEnd(realChain.call(), request);

    Response.Builder responseBuilder = null;
    //如果请求有body（POST请求）
    //封装body并将其发送给服务器
    if (HttpMethod.permitsRequestBody(request.method()) && request.body() != null) {
        //当请求头为"Expect: 100-continue"时，在发送请求体之前需要等待服务器返回"HTTP/1.1 100 Continue" 的response，如果没有等到该response，就不发送请求体。
        //POST请求，先发送请求头，在获取到100继续状态后继续发送请求体
        if ("100-continue".equalsIgnoreCase(request.header("Expect"))) {
            //这里本质上是Socket IO操作，
            //强制将数据从缓冲区写入目标位置（服务器）
            httpCodec.flushRequest();
            realChain.eventListener().responseHeadersStart(realChain.call());
            //解析响应头
            responseBuilder = httpCodec.readResponseHeaders(true);
        }

        //将请求体写入服务器
        if (responseBuilder == null) {
            realChain.eventListener().requestBodyStart(realChain.call());
            long contentLength = request.body().contentLength();
            CountingSink requestBodyOut =
            new CountingSink(httpCodec.createRequestBody(request, contentLength));
            BufferedSink bufferedRequestBody = Okio.buffer(requestBodyOut);

            //写入nody
            request.body().writeTo(bufferedRequestBody);
            bufferedRequestBody.close();
            realChain.eventListener()
                .requestBodyEnd(realChain.call(), requestBodyOut.successfulCount);
        } else if (!connection.isMultiplexed()) {
            streamAllocation.noNewStreams();
        }
    }

    //强制刷新
    httpCodec.finishRequest();

    if (responseBuilder == null) {
        realChain.eventListener().responseHeadersStart(realChain.call());
        responseBuilder = httpCodec.readResponseHeaders(false);
    }

    //构建response
    Response response = responseBuilder
            .request(request)
        .handshake(streamAllocation.connection().handshake())
        .sentRequestAtMillis(sentRequestMillis)
        .receivedResponseAtMillis(System.currentTimeMillis())
        .build();

    int code = response.code();
    if (code == 100) {
        // server sent a 100-continue even though we did not request one.
        // try again to read the actual response
        responseBuilder = httpCodec.readResponseHeaders(false);

        response = responseBuilder
            .request(request)
            .handshake(streamAllocation.connection().handshake())
            .sentRequestAtMillis(sentRequestMillis)
            .receivedResponseAtMillis(System.currentTimeMillis())
            .build();

        code = response.code();
    }

    //..

    return response;
}
```

代码主要逻辑如下：

* 向服务器发送请求头
* 如果有请求体，接着发送请求体
* 读取返回头并构建Response对象
* 如果有返回体，则再次构建新的Response对象

所有的逻辑都是借助HttpCodec完成的，我们马上对它进行分析。

### 拦截器的基石——链接的建立、管理和数据的传输
上文中讲到的ConnectInterceptor和CallServerInterceptor，我们只是简述了一下它们的职责。并没有深入的探讨其实现。接下来就是探寻这其中更深的奥秘了。
首先是看StreamAllocation，它在RetryAndFollowUpInterceptor中创建，并一直到ConnectInterceptor拦截器里才被使用。在ConnectInterceptor小节提到过，它负责Dns解析和Socket连接，而这些任务都是交给StreamAllocation执行的。其中关键代码就如下两句：

```
//...
HttpCodec httpCodec = streamAllocation.newStream(client, chain, doExtensiveHealthChecks);
RealConnection connection = streamAllocation.connection();

return realChain.proceed(request, streamAllocation, httpCodec, connection);
```
ConnectInterceptor创建了HttpCodec和RealConnection的实例，并将它们作为参数添加到责任链的调用中，传向了下一个拦截器。其中`streamAllocation.connection()`方法就是直接返回了StreamAllocation的一个成员变量。而且，StreamAllocation是在`RetryAndFollowUpInterceptor`中初始化的，它的构造函数除了为一些成员变量赋值之外没有做任何操作：

```
//RetryAndFollowUpInterceptor.java
StreamAllocation streamAllocation = new StreamAllocation(client.connectionPool(),
        createAddress(request.url()), call, eventListener, callStackTrace);
        
//StreamAllocation.java        
public StreamAllocation(ConnectionPool connectionPool, Address address, Call call,EventListener eventListener, Object callStackTrace) {
    //连接池
    this.connectionPool = connectionPool;
    //连接到服务器的内容详情
    this.address = address;
    //请求Call
    this.call = call;
    this.eventListener = eventListener;
    this.routeSelector = new RouteSelector(address, routeDatabase(), call, eventListener);
    this.callStackTrace = callStackTrace;
}
```

StreamAllocation的关键成员变量如下：

* connectionPool：管理HTTP的连接，并负责它们的重用(由OkHttpClient默认创建并提供)
* address：连接到服务器的内容详情，一般包括hostname、port、proxy等


接下来就是重点分析`StreamAllocation.newStream`方法的内容和流程了

#### HTTP连接的管理和创建

```
public HttpCodec newStream(OkHttpClient client, Interceptor.Chain chain, boolean doExtensiveHealthChecks) {
    int connectTimeout = chain.connectTimeoutMillis();
    //...
    boolean connectionRetryEnabled = client.retryOnConnectionFailure();

    try {
        RealConnection resultConnection = findHealthyConnection(connectTimeout, readTimeout,
        writeTimeout, pingIntervalMillis, connectionRetryEnabled, doExtensiveHealthChecks);
        HttpCodec resultCodec = resultConnection.newCodec(client, chain, this);

        synchronized (connectionPool) {
            codec = resultCodec;
            return resultCodec;
        }
    } catch (IOException e) {
        throw new RouteException(e);
    }
}


private RealConnection findHealthyConnection(int connectTimeout, int readTimeout,int writeTimeout, int pingIntervalMillis, boolean connectionRetryEnabled,boolean doExtensiveHealthChecks) throws IOException {
   //循环遍历可用的链接
    while (true) {
        RealConnection candidate = findConnection(connectTimeout, readTimeout, writeTimeout,
        pingIntervalMillis, connectionRetryEnabled);
        synchronized (connectionPool) {
           //如果是新创建的链接，则直接跳过检查连接是否可用的逻辑
            if (candidate.successCount == 0) {
                return candidate;
            }
        }
        //检查连接是否可用
        if (!candidate.isHealthy(doExtensiveHealthChecks)) {
       	 noNewStreams();
      		 continue;
      }

        //...
    }
}

private RealConnection findConnection(int connectTimeout, int readTimeout, int writeTimeout,int pingIntervalMillis, boolean connectionRetryEnabled) throws IOException {
    boolean foundPooledConnection = false;
    RealConnection result = null;
    Route selectedRoute = null;
    Connection releasedConnection;
    Socket toClose;
    synchronized (connectionPool) {
        //..

        //是否存在之前已分配的连接，如果有则尝试使用
        //在进行RetryAndFollowUpInterceptor进行重试和重定向的时候可能会触发此处的逻辑
        //StreamAllocation实例是在RetryAndFollowUpInterceptor循环尝试的之前进行初始化的
        //后续ConnectInterceptor拦截器在执行此处时，可能因为异常或者重定向多次导致多次调用该方法
        releasedConnection = this.connection;
        toClose = releaseIfNoNewStreams();
        if (this.connection != null) {
            // 确定已经存在连接并且是可用的
            result = this.connection;
            releasedConnection = null;
        }
        //..

        //正常顺利的情况下走此处的逻辑，根据adress去连接池里获取连接
        if (result == null) {
            // Attempt to get a connection from the pool.
            Internal.instance.get(connectionPool, address, this, null);
            if (connection != null) {
                foundPooledConnection = true;
                result = connection;
            } else {
                selectedRoute = route;
            }
        }
    }
    closeQuietly(toClose);

    //...
    if (result != null) {
        //从连接池里拿到了可用的连接，则直接使用
        return result;
    }

    //如果需要进行路由选择，则进行一次路由选择
    boolean newRouteSelection = false;
    if (selectedRoute == null && (routeSelection == null || !routeSelection.hasNext())) {
        newRouteSelection = true;
        routeSelection = routeSelector.next();
    }

    synchronized (connectionPool) {
        if (canceled) throw new IOException("Canceled");

        if (newRouteSelection) {
            // 路由选择只会拿到了一组IP，再次尝试获从连接池中获取
            List<Route> routes = routeSelection.getAll();
            for (int i = 0, size = routes.size(); i < size; i++) {
                Route route = routes.get(i);
                Internal.instance.get(connectionPool, address, this, route);
                if (connection != null) {
                    foundPooledConnection = true;
                    result = connection;
                    this.route = route;
                    break;
                }
            }
        }

        //如果依旧没有获得连接，则开始创建连接 
        if (!foundPooledConnection) {
            if (selectedRoute == null) {
                selectedRoute = routeSelection.next();
            }

            route = selectedRoute;
            refusedStreamCount = 0;
            //创建新的连接
            result = new RealConnection(connectionPool, selectedRoute);
            acquire(result, false);
        }
    }

    //.. 

    // 执行TCP+TLS,(此处是一个阻塞过程)
    result.connect(connectTimeout, readTimeout, writeTimeout, pingIntervalMillis,
        connectionRetryEnabled, call, eventListener);
    routeDatabase().connected(result.route());
  
    synchronized (connectionPool) {
        reportedAcquired = true;

        //将创建成功的连接加入到连接池中
        Internal.instance.put(connectionPool, result);

        //..
    }
  
    eventListener.connectionAcquired(call, result);
    return result;
}
```

获取链接的流程可以简化如下：

* 首先尝试获取当前StreamAllocation中已经存在的连接，由于重试和重定向的功能。StreamAllocation的方法可能会执行多次，后续重试请求可以先尝试使用之前Call创建的连接
* 如果当前没有连接，则尝试从连接池里获取。连接池由OkHttpClient提供
* 如果仍没有连接，则进行路由选择，并再次尝试获取
* 获取不到，则构建RealConnection实例，并执行TCP + TLS握手
* 最后，将创建好的链接放入到连接池中

需要注意一点，上面中的`findHealthyConnection`方法并不是拿到连接就结束工作。它循环操作，会检查拿到的连接是否可用。具体代码如下：

```
public boolean isHealthy(boolean doExtensiveChecks) {
    //检查Socket是否可用
    //Socket是否关闭、输入输出流是否关闭
    if (socket.isClosed() || socket.isInputShutdown() || socket.isOutputShutdown()) {
        return false;
    }

    //HTTP/2连接是否关闭
    if (http2Connection != null) {
        return !http2Connection.isShutdown();
    }

    //..

    return true;
}
```

会检查Socket、输入输出流和Http2连接是否关闭。

先附上StreamAllocation[完整代码链接](https://github.com/square/okhttp/blob/parent-3.12.0/okhttp/src/main/java/okhttp3/internal/connection/StreamAllocation.java)

现在知道了大致流程，但还是有几个问题：

* 链接是如何创建并连接的？
* 连接池如何实现的，是如何管理连接的？

首先看连接的创建：

##### 连接的创建

在上文中，已经展示过连接创建的代码了：

```
result = new RealConnection(connectionPool, selectedRoute);

result.connect(connectTimeout, readTimeout, writeTimeout, pingIntervalMillis,
        connectionRetryEnabled, call, eventListener);
```

创建RealConnection实例，然后执行`connect `方法。其中初始化中的connectionPool参数的主要作用是用来作为锁使用的。`connect `精简后的代码如下：

```
public void connect(int connectTimeout, int readTimeout, int writeTimeout,int pingIntervalMillis, boolean connectionRetryEnabled, Call call,EventListener eventListener) {
    //...
    while (true) {
        try {
            if (route.requiresTunnel()) {
                //HTTP隧道
                if (rawSocket == null) {
                    break;
                }
            } else {
                //创建Socket链接
                connectSocket(connectTimeout, readTimeout, call, eventListener);
            }
             //Https请求的tls建立过程      
            establishProtocol(connectionSpecSelector, pingIntervalMillis, call, eventListener);
            //..
            break;
        } catch (IOException e) {
            //..
        }
    }

    //...
}

private void connectTunnel(int connectTimeout, int readTimeout, int writeTimeout, Call call,EventListener eventListener) throws IOException {
    Request tunnelRequest = createTunnelRequest();
    HttpUrl url = tunnelRequest.url();
    for (int i = 0; i < MAX_TUNNEL_ATTEMPTS; i++) {
         //创建Socket链接
        connectSocket(connectTimeout, readTimeout, call, eventListener);
        tunnelRequest = createTunnel(readTimeout, writeTimeout, tunnelRequest, url);
        if (tunnelRequest == null) break;
        closeQuietly(rawSocket);
        rawSocket = null;
        sink = null;
        source = null;
        eventListener.connectEnd(call, route.socketAddress(), route.proxy(), null);
    }
}
```

链接的建立首先会经过是否使用隧道技术，如果使用了则先调用`connectTunnel`方法做一些协议交换工作，再调用`connectSocket`方法进行socket链接。否则直接使用`connectSocket`方法进行Socket链接。由于隧道链接不是这篇文章的重点（实际上是因为我也不会），我们直接分析`connectSocket`：

```

private void connectSocket(int connectTimeout, int readTimeout, Call call,EventListener eventListener) throws IOException {
    Proxy proxy = route.proxy();
    Address address = route.address();

    //创建Socket
    rawSocket = proxy.type() == Proxy.Type.DIRECT || proxy.type() == Proxy.Type.HTTP
    ? address.socketFactory().createSocket()
    : new Socket(proxy);

    //..
    try {
        //链接Socket
        Platform.get().connectSocket(rawSocket, route.socketAddress(), connectTimeout);
    } catch (ConnectException e) {
        ConnectException ce = new ConnectException("Failed to connect to " + route.socketAddress());
        ce.initCause(e);
        throw ce;
    }

    try {
        //Okio创建输入输出流
        source = Okio.buffer(Okio.source(rawSocket));
        sink = Okio.buffer(Okio.sink(rawSocket));
    } catch (NullPointerException npe) {
        if (NPE_THROW_WITH_NULL.equals(npe.getMessage())) {
            throw new IOException(npe);
        }
    }
}
```

`connectSocket`方法的核心逻辑只有三个：

* 创建Socket实例
* 链接Socket
* 使用Okio为Socket创建输入输出流

其中Socket的创建分两种，一种是直接通过`Socket(Proxy proxy)`构造方法创建，一种是借助`Address`中的socketFactory创建，该`socketFactory`在OkHttpClient中被初始化，默认是使用`DefaultSocketFactory`，其关键代码如下：

```
class DefaultSocketFactory extends SocketFactory {

    public Socket createSocket() {
        return new Socket();
    }

    public Socket createSocket(String host, int port)
    throws IOException, UnknownHostException
    {
        return new Socket(host, port);
    }

    public Socket createSocket(InetAddress address, int port)
    throws IOException
    {
        return new Socket(address, port);
    }

    public Socket createSocket(String host, int port,
        InetAddress clientAddress, int clientPort)
    throws IOException, UnknownHostException
    {
        return new Socket(host, port, clientAddress, clientPort);
    }

    public Socket createSocket(InetAddress address, int port,
        InetAddress clientAddress, int clientPort)
    throws IOException
    {
        return new Socket(address, port, clientAddress, clientPort);
    }
}

```

它的主要功能就是提供各种方法，实现对Socket不同构造函数的调用。紧接着就是Socket的连接：

```
public void connectSocket(Socket socket, InetSocketAddress address, int connectTimeout)
      throws IOException {
    socket.connect(address, connectTimeout);
}
```

也很简单，就是调用`Socket.connect`方法

接下来就是Okio，本质就是IO流。详情可见[Okio源码和流程分析](https://juejin.cn/post/7098916408932237349)。

可见，OkHttp链接最终还是Socket，并借助Okio实现IO操作，从而实现数据传输。还是最基本的套接字和IO操作。

##### 连接池的实现和连接的管理

`ConnectionPool`内部通过一个双端队列`connections`管理现有连接，并限制了统一地址的最大空闲连接数和空闲连接的最大存活时间：

```
public final class ConnectionPool {
  //...
  //最大空闲连接数
  private final int maxIdleConnections;
  //空闲的连接数存活时间
  private final long keepAliveDurationNs;

  //所有连接
  private final Deque<RealConnection> connections = new ArrayDeque<>();
  
  boolean cleanupRunning;

  public ConnectionPool() {
    this(5, 5, TimeUnit.MINUTES);
  }

  public ConnectionPool(int maxIdleConnections, long keepAliveDuration, TimeUnit timeUnit) {
    this.maxIdleConnections = maxIdleConnections;
    this.keepAliveDurationNs = timeUnit.toNanos(keepAliveDuration);

    if (keepAliveDuration <= 0) {
      throw new IllegalArgumentException("keepAliveDuration <= 0: " + keepAliveDuration);
    }
  }
   //...
}
```

通过源码可以看到，`ConnectionPool`默认同一地址的最大连接数为5个，默认存活时间为5分钟。

从连接池中获取连接的代码如下：
```
@Nullable RealConnection get(Address address, StreamAllocation streamAllocation, Route route) {
    assert (Thread.holdsLock(this));
    for (RealConnection connection : connections) {
        if (connection.isEligible(address, route)) {
            streamAllocation.acquire(connection, true);
            return connection;
        }
    }
    return null;
}
```

通过遍历所有连接，然后比对地址和路由确定连接是否可用。

向连接池中加入连接的代码如下：

```
void put(RealConnection connection) {
    assert (Thread.holdsLock(this));
    if (!cleanupRunning) {
        cleanupRunning = true;
        executor.execute(cleanupRunnable);
    }
    connections.add(connection);
}

```

在向链表中添加连接之前，还执行了一些线程池的操作。相关代码如下：

```
public final class ConnectionPool {
  
  private static final Executor executor = new ThreadPoolExecutor(0 /* corePoolSize */,
      Integer.MAX_VALUE /* maximumPoolSize */, 60L /* keepAliveTime */, TimeUnit.SECONDS,
      new SynchronousQueue<Runnable>(), Util.threadFactory("OkHttp ConnectionPool", true));


  private final int maxIdleConnections;
  private final long keepAliveDurationNs;
  private final Runnable cleanupRunnable = new Runnable() {
    @Override public void run() {
      while (true) {
        long waitNanos = cleanup(System.nanoTime());
        if (waitNanos == -1) return;
        if (waitNanos > 0) {
          long waitMillis = waitNanos / 1000000L;
          waitNanos -= (waitMillis * 1000000L);
          synchronized (ConnectionPool.this) {
            try {
              ConnectionPool.this.wait(waitMillis, (int) waitNanos);
            } catch (InterruptedException ignored) {
            }
          }
        }
      }
    }
  };
}
```

每次添加新的连接到连接池中时，都会检查executor线程池是否在运行。如果没有运行，在开启线程池。而线程池执行的任务也很简单。不断的调用`cleanup`方法，并根据该方法返回的wait的秒数，调用wait方法进入阻塞。从而实现不断且有规律的对`cleanup`方法的调用。接着看一下`cleanup`方法：

```
long cleanup(long now) {
    int inUseConnectionCount = 0;
    int idleConnectionCount = 0;
    RealConnection longestIdleConnection = null;
    long longestIdleDurationNs = Long.MIN_VALUE;

    
    synchronized (this) {
        //遍历连接
        for (Iterator<RealConnection> i = connections.iterator(); i.hasNext(); ) {
        RealConnection connection = i.next();

        //统计连接的引用数量        
        if (pruneAndGetAllocationCount(connection, now) > 0) {
            //标记正在被使用的连接数量
            inUseConnectionCount++;
            continue;
        }
        //标记空闲连接的数量
        idleConnectionCount++;

        //找出空闲连接中，空闲时间最长的连接
        long idleDurationNs = now - connection.idleAtNanos;
        if (idleDurationNs > longestIdleDurationNs) {
            longestIdleDurationNs = idleDurationNs;
            longestIdleConnection = connection;
        }
    }

        if (longestIdleDurationNs >= this.keepAliveDurationNs
            || idleConnectionCount > this.maxIdleConnections) {
            //如果空闲连接数大于允许的最大连接数，
            //或者空闲时间大于允许的最大空闲时间。则将最老的空闲连接清除掉
            connections.remove(longestIdleConnection);
        } else if (idleConnectionCount > 0) {
            // 如果最大空闲数量和最久空闲时间都不满足条件，根据最老的连接计算下一次清理时间并返回
            return keepAliveDurationNs - longestIdleDurationNs;
        } else if (inUseConnectionCount > 0) {
            //当前连接都在使用，直接返回最长存活时间
            return keepAliveDurationNs;
        } else {
            cleanupRunning = false;
            return -1;
        }
    }

    //关闭空闲最久的连接
    closeQuietly(longestIdleConnection.socket());
    return 0;
}
```

代码逻辑如下：

* 通过`pruneAndGetAllocationCount`方法统计正在使用的连接和空闲的连接，并找出空闲最久的连接；
* 如果空闲最久的连接空闲时间大于规定时间`keepAliveDurationNs`（默认五分钟）或者空闲连接数量大于规定数量`maxIdleConnections`（默认五个）则将最老的空闲线程清除掉；
* 如果不满足清理条件，则计算下一次清理时间并返回（如果连接都在使用，则返回最大存活时间）
* 如果没用连接，则将`cleanupRunning`设置为false，并返回-1。标记自动清理没有运行。

结合cleanupRunnable和cleanup方法可知，每次添加新的连接到连接池中之后。连接池就会通过cleanupRunnable开启循环不断的调用cleanup来尝试清理连接。当不满足清理条件时，cleanupRunnable会通过cleanup所返回的时间进入等待状态，直到所有的连接，cleanupRunnable会终止并修改cleanupRunning标记等待新的连接进来，再次开启循环。


#### 数据的传输

网络请求的最关键的步骤到了，就是数据的传输。上文讲到了连接的建立，我们知道了OkHttp最终还是通过Socket进行数据传输的，而且是借助Okio执行IO操作的。接下来我们看一下具体流程。首先让我们看一下`HttpCodec`，该对象实例是紧跟着RealConnection之后初始化的：`HttpCodec resultCodec = resultConnection.newCodec(client, chain, this);`

```
public HttpCodec newCodec(OkHttpClient client, Interceptor.Chain chain,StreamAllocation streamAllocation) throws SocketException {
    if (http2Connection != null) {
        return new Http2Codec (client, chain, streamAllocation, http2Connection);
    } else {
        socket.setSoTimeout(chain.readTimeoutMillis());
        source.timeout().timeout(chain.readTimeoutMillis(), MILLISECONDS);
        sink.timeout().timeout(chain.writeTimeoutMillis(), MILLISECONDS);
        return new Http1Codec (client, streamAllocation, source, sink);
    }
}
```

根据`http2Connection`是否为null返回`Http2Codec`或者`Http1Codec`的实例，它们都实现`HttpCodec`接口。`Http1Codec` 的初始化非常简单，接收了OkHttpClien实例，streamAllocation 实例和`source`, `sink`。后面两个是Okio中的对象，分别对应输入流和输出流。`http2Connection`表明是http/2的连接，它在`establishProtocol`中会连接地址协议中是否包含HTTP/2进行创建。其初始化方法如下：

```
private void startHttp2(int pingIntervalMillis) throws IOException {
    socket.setSoTimeout(0); // HTTP/2 connection timeouts are set per-stream.
    http2Connection = new Http2Connection.Builder(true)
        .socket(socket, route.address().url().host(), source, sink)
        .listener(this)
        .pingIntervalMillis(pingIntervalMillis)
        .build();
    http2Connection.start();
}
```
不难发现，`Http2Codec`的初始化借助了http2Connection，而http2Connection中包含着`source`, `sink`。可以大胆猜测数据的传输就是借助这两个分别进行读取数据和写入数据的操作的。

为了验证这一点，我们在看一下`CallServerInterceptor`拦截器的想服务器发送数据的流程：

* 向服务器发送请求头
* 如果有请求体，接着发送请求体
* 读取返回头并构建Response对象
* 如果有返回体，则再次构建新的Response对象

##### 向服务器发送数据
首先抽取发送请求的代码：

```
//将请求头写入到输出流缓存
httpCodec.writeRequestHeaders(request);
//强制刷新，将数据写入到服务器中
httpCodec.flushRequest();

//创建CountingSink，本质上也是个Sink（输出流）
CountingSink requestBodyOut =
            new CountingSink(httpCodec.createRequestBody(request, contentLength));
//创建输出流缓存
BufferedSink bufferedRequestBody = Okio.buffer(requestBodyOut);
//将请求的Body写入到输出流缓存
request.body().writeTo(bufferedRequestBody);
//关闭输出流，结束请求的发动流程
bufferedRequestBody.close();
httpCodec.finishRequest();
```

大致流程如上。
首先分析`Http1Codec`中的实现，其中`HttpCodec.writeRequestHeaders()`代码如下。

```
@Override public void writeRequestHeaders(Request request) throws IOException {
    String requestLine = RequestLine.get(
            request, streamAllocation.connection().route().proxy().type());
    writeRequest(request.headers(), requestLine);
}

public void writeRequest(Headers headers, String requestLine) throws IOException {
    if (state != STATE_IDLE) throw new IllegalStateException("state: " + state);
    sink.writeUtf8(requestLine).writeUtf8("\r\n");
    for (int i = 0, size = headers.size(); i < size; i++) {
        sink.writeUtf8(headers.name(i))
            .writeUtf8(": ")
            .writeUtf8(headers.value(i))
            .writeUtf8("\r\n");
    }
    sink.writeUtf8("\r\n");
    state = STATE_OPEN_REQUEST_BODY;
}
```

代码逻辑很简单，提取出Request中的Headers后通过Okio.sink执行对服务器的IO写操作。

紧接着是`createRequestBody`方法：

```

@Override public Sink createRequestBody(Request request, long contentLength) {
    if ("chunked".equalsIgnoreCase(request.header("Transfer-Encoding"))) {
        return newChunkedSink();
    }
    if (contentLength != -1) {
        return newFixedLengthSink(contentLength);
    }
}

public Sink newChunkedSink() {
    if (state != STATE_OPEN_REQUEST_BODY) throw new IllegalStateException("state: " + state);
    state = STATE_WRITING_REQUEST_BODY;
    return new ChunkedSink();
}

public Sink newFixedLengthSink(long contentLength) {
    if (state != STATE_OPEN_REQUEST_BODY) throw new IllegalStateException("state: " + state);
    state = STATE_WRITING_REQUEST_BODY;
    return new FixedLengthSink(contentLength);
}
```
根据是否知道请求体的长度分别返回`ChunkedSink `和`FixedLengthSink `的实例，它们是`Http1Codec`的内部类，都实现了`Sink`接口可用来执行IO的输出流操作。

而紧接着都是通过Okio执行的输出流操作，最后执行`httpCodec.finishRequest()`方法，在`finishRequest`中的实现如下：

```
@Override public void finishRequest() throws IOException {
    sink.flush();
}
```
就是最基本的强制输出流刷新操作。

接下来看`Http2Codec`中的实现，首先是`writeRequestHeaders`方法：

```
@Override public void writeRequestHeaders(Request request) throws IOException {
    if (stream != null) return;

    boolean hasRequestBody = request.body() != null;
    List<Header> requestHeaders = http2HeadersList(request);
    stream = connection.newStream(requestHeaders, hasRequestBody);
    stream.readTimeout().timeout(chain.readTimeoutMillis(), TimeUnit.MILLISECONDS);
    stream.writeTimeout().timeout(chain.writeTimeoutMillis(), TimeUnit.MILLISECONDS);
}
```

执行逻辑交给了`Http2Stream`的实例stream进行处理了：

```
public Http2Stream newStream(List<Header> requestHeaders, boolean out) throws IOException {
    return newStream(0, requestHeaders, out);
}

private Http2Stream newStream(int associatedStreamId, List<Header> requestHeaders, boolean out) throws IOException {
    boolean outFinished = !out;
    boolean inFinished = false;
    boolean flushHeaders;
    Http2Stream stream;
    int streamId;

    synchronized (writer) {
        synchronized (this) {
            // 计算当前Stream的id
            if (nextStreamId > Integer.MAX_VALUE / 2) {
                shutdown(REFUSED_STREAM);
            }
            if (shutdown) {
                throw new ConnectionShutdownException();
            }
            streamId = nextStreamId;
            nextStreamId += 2;
            stream = new Http2Stream(streamId, this, outFinished, inFinished, null);
            flushHeaders = !out || bytesLeftInWriteWindow == 0L || stream.bytesLeftInWriteWindow == 0L;
            if (stream.isOpen()) {
                streams.put(streamId, stream);
            }
        }
        if (associatedStreamId == 0) {
            writer.synStream(outFinished, streamId, associatedStreamId, requestHeaders);
        } else if (client) {
            throw new IllegalArgumentException("client streams shouldn't have associated stream IDs");
        } else { // HTTP/2 has a PUSH_PROMISE frame.
            writer.pushPromise(associatedStreamId, streamId, requestHeaders);
        }
    }

    if (flushHeaders) {
        writer.flush();
    }

    return stream;
}

```
这里的逻辑主要是用来处理`PUSH_PROMISE`帧操作的，最终会执行`Http2Writer`的`synStream`或者`pushPromise`方法，最终还是通过Okio.sink完成输出流操作，向服务器写入数据。

而它的`finishRequest`方法本质上是调用了SInk的close方法：

```
@Override public void finishRequest() throws IOException {
    stream.getSink().close();
}
```

##### 接受服务器返回的数据

抽取`CallServerInterceptor`拦截器中的接收服务器中数据的代码如下：

```
//读取响应头
Response.Builder responseBuilder = httpCodec.readResponseHeaders(false);
//根据响应头构建Response
response = responseBuilder
              .request(request)
              .handshake(streamAllocation.connection().handshake())
              .sentRequestAtMillis(sentRequestMillis)
              .receivedResponseAtMillis(System.currentTimeMillis())
              .build();


//如果有响应体，则读取body
response = response.newBuilder()
          .body(httpCodec.openResponseBody(response))
          .build();
```

可见，数据的读取也是分heades和body两部分。首先看`httpCodec.readResponseHeaders `在`Http1Codec`中的实现：

```
@Override public Response.Builder readResponseHeaders(boolean expectContinue) throws IOException {
    //...
    try {
        //读取返回头
        StatusLine statusLine = StatusLine.parse(readHeaderLine());
        //构建Response
        Response.Builder responseBuilder = new Response.Builder()
            .protocol(statusLine.protocol)
            .code(statusLine.code)
            .message(statusLine.message)
            .headers(readHeaders());
        //..
        return responseBuilder;
    } catch (EOFException e) {
        //...
    }
}

private String readHeaderLine() throws IOException {
    //执行IO读操作
    String line = source.readUtf8LineStrict(headerLimit);
    headerLimit -= line.length();
    return line;
}
```
可见，最终是借助`readHeaderLine`通过Okio.source实现的。

接着看`openResponseBody`的实现：

```
@Override public ResponseBody openResponseBody(Response response) throws IOException {
    streamAllocation.eventListener.responseBodyStart(streamAllocation.call);
    String contentType = response.header("Content-Type");

    if (!HttpHeaders.hasBody(response)) { 
      Source source = newFixedLengthSource(0);
      return new RealResponseBody(contentType, 0, Okio.buffer(source));
    }

    if ("chunked".equalsIgnoreCase(response.header("Transfer-Encoding"))) {
      Source source = newChunkedSource(response.request().url());
      return new RealResponseBody(contentType, -1L, Okio.buffer(source));
    }

    long contentLength = HttpHeaders.contentLength(response);
    if (contentLength != -1) {
      Source source = newFixedLengthSource(contentLength);
      return new RealResponseBody(contentType, contentLength, Okio.buffer(source));
    }

    return new RealResponseBody(contentType, -1L, Okio.buffer(newUnknownLengthSource()));
}
```

和请求的body写入到服务器重有异曲同工之妙，都是根据body的长度采取不同的方案，`newFixedLengthSource`和`newChunkedSource `代码如下：

```
 public Source newFixedLengthSource(long length) throws IOException {
    if (state != STATE_OPEN_RESPONSE_BODY) throw new IllegalStateException("state: " + state);
    state = STATE_READING_RESPONSE_BODY;
    return new FixedLengthSource(length);
}
public Source newChunkedSource(HttpUrl url) throws IOException {
    if (state != STATE_OPEN_RESPONSE_BODY) throw new IllegalStateException("state: " + state);
    state = STATE_READING_RESPONSE_BODY;
    return new ChunkedSource(url);
}
```

`FixedLengthSource`和`ChunkedSource`都继承自`AbstractSource`，本质上都是Source输入流。

接下来看`Http2Codec`中的实现，首先是`readResponseHeaders`：

```
@Override
public Response.Builder readResponseHeaders(boolean expectContinue) throws IOException {
    Headers headers = stream.takeHeaders();
    Response.Builder responseBuilder = readHttp2HeadersList(headers, protocol);
    if (expectContinue && Internal.instance.code(responseBuilder) == HTTP_CONTINUE) {
        return null;
    }
    return responseBuilder;
}

```
stream.takeHeaders负责获取到了响应中的Headers信息，readHttp2HeadersList则负责构建Response。我们重点关注`stream.takeHeaders`：

```
public synchronized Headers takeHeaders() throws IOException {
    readTimeout.enter();
    try {
      while (headersQueue.isEmpty() && errorCode == null) {
        waitForIo();
      }
    } finally {
      readTimeout.exitAndThrowIfTimedOut();
    }
    if (!headersQueue.isEmpty()) {
      return headersQueue.removeFirst();
    }
    throw new StreamResetException(errorCode);
}
```

真正进行IO操作的不在这里，这里通过一个双端队列headersQueue中获取数据，`waitForIo()`负责阻塞当前流程，等待从队列中获取数据。而真正的读取数据的操作在哪里呢？

 Http2Codec连接启动时，会创建新的线程不断地进行数据读取，读到数据后再向下分发。在Http2Codec启动时，会有一下调用逻辑：RealConnection.establishProtocol->RealConnection.startHttp2->Http2Connection.start。最终执行的代码逻辑如下：
 
 ```
 
public void start() throws IOException {
    start(true);
}

void start(boolean sendConnectionPreface) throws IOException {
    if (sendConnectionPreface) {
      writer.connectionPreface();
      writer.settings(okHttpSettings);
      int windowSize = okHttpSettings.getInitialWindowSize();
      if (windowSize != Settings.DEFAULT_INITIAL_WINDOW_SIZE) {
        writer.windowUpdate(0, windowSize - Settings.DEFAULT_INITIAL_WINDOW_SIZE);
      }
    }
    new Thread(readerRunnable).start(); // Not a daemon thread.
}


Http2Connection(Builder builder) {
  readerRunnable = new ReaderRunnable(new Http2Reader(builder.source, client));
}
 ```
 
 此处的关键逻辑创建一个新的线程开始执行`readerRunnable`，它是一个`ReaderRunnable`的实例，具体定义如下：
 
 ```
 class ReaderRunnable extends NamedRunnable implements Http2Reader.Handler {
    final Http2Reader reader;

    @Override protected void execute() {
        ErrorCode connectionErrorCode = ErrorCode.INTERNAL_ERROR;
        ErrorCode streamErrorCode = ErrorCode.INTERNAL_ERROR;
        try {
            reader.readConnectionPreface(this);
             //注释①
            while (reader.nextFrame(false, this)) {
            }
            connectionErrorCode = ErrorCode.NO_ERROR;
            streamErrorCode = ErrorCode.CANCEL;
        } catch (IOException e) {
            connectionErrorCode = ErrorCode.PROTOCOL_ERROR;
            streamErrorCode = ErrorCode.PROTOCOL_ERROR;
        } finally {
            try {
                close(connectionErrorCode, streamErrorCode);
            } catch (IOException ignored) {
            }
            Util.closeQuietly(reader);
        }
    }
}
 ```
 
 这端代码的关键是注释①处，不断的调用`Http2Reader.nextFrame`方法:
 ```
 public boolean nextFrame(boolean requireSettings, Handler handler) throws IOException {
    try {
        source.require(9); // Frame header size
    } catch (IOException e) {
        return false; // This might be a normal socket close.
    }

    int length = readMedium(source);
    if (length < 0 || length > INITIAL_MAX_FRAME_SIZE) {
        throw ioException("FRAME_SIZE_ERROR: %s", length);
    }
    byte type = (byte) (source.readByte() & 0xff);
    if (requireSettings && type != TYPE_SETTINGS) {
        throw ioException("Expected a SETTINGS frame but was %s", type);
    }
    byte flags = (byte) (source.readByte() & 0xff);
    int streamId = (source.readInt() & 0x7fffffff); // Ignore reserved bit.
    if (logger.isLoggable(FINE)) logger.fine(frameLog(true, streamId, length, type, flags));

    switch (type) {
        case TYPE_DATA:
        readData(handler, length, flags, streamId);
        break;

        case TYPE_HEADERS:
        readHeaders(handler, length, flags, streamId);
        break;

        case TYPE_PRIORITY:
        readPriority(handler, length, flags, streamId);
        break;

        case TYPE_RST_STREAM:
        readRstStream(handler, length, flags, streamId);
        break;

        case TYPE_SETTINGS:
        readSettings(handler, length, flags, streamId);
        break;

        case TYPE_PUSH_PROMISE:
        readPushPromise(handler, length, flags, streamId);
        break;

        case TYPE_PING:
        readPing(handler, length, flags, streamId);
        break;

        case TYPE_GOAWAY:
        readGoAway(handler, length, flags, streamId);
        break;

        case TYPE_WINDOW_UPDATE:
        readWindowUpdate(handler, length, flags, streamId);
        break;

        default:
        // Implementations MUST discard frames that have unknown or unsupported types.
        source.skip(length);
    }
    return true;
}
 ```
 
 这里不精执行了source的读操作，并且对不同的数据类型进行了判断，并调用了不同的方法处理读取到的数据。而接下来就是我们的Headers读取的流程了:Http2Reader.readHeaders:
 
 ```
 private void readHeaders(Handler handler, int length, byte flags, int streamId)
      throws IOException {
    //..
    List<Header> headerBlock = readHeaderBlock(length, padding, flags, streamId);

    handler.headers(endStream, streamId, -1, headerBlock);
}
 ```
 来到了熟悉的Handler环节，看来这里就是线程间通讯，将`Http2Connection`开启的线程切换到我们请求执行的线程中？但是这个Handler却是`Http2Reader.Handler`：还记得上文中的`ReaderRunnable`吗，它作为Runable被传入到了`Http2Connection `线程中，它不仅实现了Runable接口，还实现了Handler接口`Http2Reader.Handler`，`Http2Reader.Handler `的`headers`在`ReaderRunnable `中的实现最终调用了`Http2Stream.receiveHeaders`方法：
 
 ```
 void receiveHeaders(List<Header> headers) {
    assert (!Thread.holdsLock(Http2Stream.this));
    boolean open;
    synchronized (this) {
        hasResponseHeaders = true;
        //向队列内添加数据
        headersQueue.add(Util.toHeaders(headers));
        open = isOpen();
        notifyAll();
    }
    if (!open) {
        connection.removeStream(id);
    }
}
 ```
 
 就是在这里，数据被添加到队列中了。而上文中的`takeHeaders`只需不断的从队列headersQueue中取数据，就可以获得到响应头。
 
 接下来是响应体Body的读取，`openResponseBody`在`Http2Codec`中的实现如下：

```
@Override public ResponseBody openResponseBody(Response response) throws IOException {
    streamAllocation.eventListener.responseBodyStart(streamAllocation.call);
    String contentType = response.header("Content-Type");
    long contentLength = HttpHeaders.contentLength(response);
    Source source = new StreamFinishingSource(stream.getSource());
    return new RealResponseBody(contentType, contentLength, Okio.buffer(source));
}

class StreamFinishingSource extends ForwardingSource {
    boolean completed = false;
    long bytesRead = 0;

    StreamFinishingSource(Source delegate) {
      super(delegate);
    }

    @Override public long read(Buffer sink, long byteCount) throws IOException {
      try {
        long read = delegate().read(sink, byteCount);
        if (read > 0) {
          bytesRead += read;
        }
        return read;
      } catch (IOException e) {
        endOfInput(e);
        throw e;
      }
    }

    @Override public void close() throws IOException {
      super.close();
      endOfInput(null);
    }

    private void endOfInput(IOException e) {
      if (completed) return;
      completed = true;
      streamAllocation.streamFinished(false, Http2Codec.this, bytesRead, e);
    }
}
```

这里通过一个代理，实际上最终是`FramingSource`对象。它是`Http2Stream`的一个内部类。我们最终获得到到Response.body本身就是一个`Source`对象，通过readUtf8方法获取内容，最终会执行`FramingSource`的read方法：

```
@Override public long read(Buffer sink, long byteCount) throws IOException {
    if (byteCount < 0) throw new IllegalArgumentException("byteCount < 0: " + byteCount);

    while (true) {
        //..

        synchronized (Http2Stream.this) {
            readTimeout.enter();
            try {
                if (errorCode != null) {
                    // Prepare to deliver an error.
                    errorCodeToDeliver = errorCode;
                }

                if (closed) {
                    throw new IOException("stream closed");

                } else if (!headersQueue.isEmpty() && headersListener != null) {
                    // Prepare to deliver headers.
                    headersToDeliver = headersQueue.removeFirst();
                    headersListenerToNotify = headersListener;

                } else if (readBuffer.size() > 0) {
                    // 读取数据
                    readBytesDelivered = readBuffer.read(sink, Math.min(byteCount, readBuffer.size()));
                    unacknowledgedBytesRead += readBytesDelivered;

                    if (errorCodeToDeliver == null
                        && unacknowledgedBytesRead
                        >= connection.okHttpSettings.getInitialWindowSize() / 2) {
                        // Flow control: notify the peer that we're ready for more data! Only send a
                        // WINDOW_UPDATE if the stream isn't in error.
                        connection.writeWindowUpdateLater(id, unacknowledgedBytesRead);
                        unacknowledgedBytesRead = 0;
                    }
                } else if (!finished && errorCodeToDeliver == null) {
                    // Nothing to do. Wait until that changes then try again.
                    waitForIo();
                    continue;
                }
            } finally {
                readTimeout.exitAndThrowIfTimedOut();
            }
        }


        if (headersToDeliver != null && headersListenerToNotify != null) {
            headersListenerToNotify.onHeaders(headersToDeliver);
            continue;
        }

        if (readBytesDelivered != -1) {
            // Update connection.unacknowledgedBytesRead outside the synchronized block.
            updateConnectionFlowControl(readBytesDelivered);
            return readBytesDelivered;
        }
        //...
        return -1; // This source is exhausted.
    }
}
```


### 总结
Okhttp的核心有以下几点：

* 异步请求通过线程池切换线程；
* 整体采用责任链模式，通过拦截器分层处理请求和响应结果；
* 通过连接池的复用实现对连接的管理和避免频繁/重复创建；
* 连接通过Socket建立，底层通过Okio实现IO操作

最后说明一下。本文的重点在于OkHttp的主流程，其中涉及到大量的Http相关的知识，比如多路复用、DNS、代理、路由等都没有详细的进行分析。一来是因为篇幅有限，更重要的原因是我对这些知识点掌握的也不够透彻完备，知识储备不足以提供输出。同时，如果有发现错误之处，希望大家不吝赐教，及时指出问题，大家共同学习进步。

 
 
 










































