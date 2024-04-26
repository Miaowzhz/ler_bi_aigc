## 架构图



[![img](README.assets/68747470733a2f2f63646e2e6e6c61726b2e636f6d2f79757175652f302f323032342f6a7065672f34303931383934342f313730393938353835333835302d66323562646466352d326336622d343137382d396235392d3733646463313165616263302e6a706567.jpeg)](https://camo.githubusercontent.com/ee57b8b831e21a16666c7fb8fbee8c0df467ea720f0ee9cbd99ecc9b1becab3a/68747470733a2f2f63646e2e6e6c61726b2e636f6d2f79757175652f302f323032342f6a7065672f34303931383934342f313730393938353835333835302d66323562646466352d326336622d343137382d396235392d3733646463313165616263302e6a706567)



## 功能





### 智能分析：



**用户输入目标和原始数据以及图表类型，可以自动生成图表和分析结论**
步骤：
1、构造用户请求（用户消息、csv 数据、图表类型）
2、调用鱼聪明 sdk，得到 AI 响应结果
3、从 AI 响应结果中，取出需要的信息
4、保存图表到数据库

### 图表管理：





#### 安全性



- 校验文件的大小
- 校验文件的后缀

#### 限流



**用户在短时间内疯狂使用，导致服务器资源被占满，其他用户无法使用 => 限流**
步骤：
0、本地安装 Redis（可以参考星球项目 - 伙伴匹配系统中的 Redis 使用）
1、 引入 Redisson 代码包：

```xml
<dependency>
    <groupId>org.redisson</groupId>
    <artifactId>redisson</artifactId>
    <version>3.21.3</version>
</dependency>
```



2、创建 RedissonConfig 配置类，用于初始化 RedissonClient 对象单例：

```java
@Configuration
@ConfigurationProperties(prefix = "spring.redis")
@Data
public class RedissonConfig {

	private Integer database;

	private String host;

	private Integer port;

	private String password;

	@Bean
	public RedissonClient redissonClient() {
		Config config = new Config();
		config.useSingleServer()
		.setDatabase(database)
		.setAddress("redis://" + host + ":" + port);
		RedissonClient redisson = Redisson.create(config);
		return redisson;
	}
}
```



3、编写 RedisLimiterManager：
什么是 Manager？专门提供 RedisLimiter 限流基础服务的

```java
/**
 * 专门提供 RedisLimiter 限流基础服务的（提供了通用的能力）
 */
@Service
public class RedisLimiterManager {

    @Resource
    private RedissonClient redissonClient;

    /**
     * 限流操作
     *
     * @param key 区分不同的限流器，比如不同的用户 id 应该分别统计
     */
    public void doRateLimit(String key) {
        // 创建一个名称为user_limiter的限流器，每秒最多访问 2 次
        RRateLimiter rateLimiter = redissonClient.getRateLimiter(key);
        rateLimiter.trySetRate(RateType.OVERALL, 2, 1, RateIntervalUnit.SECONDS);
        // 每当一个操作来了后，请求一个令牌
        boolean canOp = rateLimiter.tryAcquire(1);
        if (!canOp) {
            throw new BusinessException(ErrorCode.TOO_MANY_REQUEST);
        }
    }
}
```



4、单元测试：

```java
@SpringBootTest
class RedisLimiterManagerTest {

    @Resource
    private RedisLimiterManager redisLimiterManager;

    @Test
    void doRateLimit() throws InterruptedException {
        String userId = "1";
        for (int i = 0; i < 2; i++) {
            redisLimiterManager.doRateLimit(userId);
            System.out.println("成功");
        }
        Thread.sleep(1000);
        for (int i = 0; i < 5; i++) {
            redisLimiterManager.doRateLimit(userId);
            System.out.println("成功");
        }
    }
}
```



5、应用到要限流的方法中，比如智能分析接口：

```java
// ...
User loginUser = userService.getLoginUser(request);
// 限流判断，每个用户一个限流器
redisLimiterManager.doRateLimit("genChartByAi_" + loginUser.getId());
// ...
```





### 图表生成异步化：





#### 1、线程池实现





##### 参数



```java
public ThreadPoolExecutor(     @Range(from = 0, to = Integer.MAX_VALUE)  int corePoolSize,
                          @Range(from = 1, to = Integer.MAX_VALUE)  int maximumPoolSize,
                          @Range(from = 0, to = Long.MAX_VALUE)  long keepAliveTime,
                          @NotNull  TimeUnit unit,
                          @NotNull  BlockingQueue<Runnable> workQueue,
                          @NotNull  ThreadFactory threadFactory,
                          @NotNull  RejectedExecutionHandler handler )
```



**corePoolSize：核心线程池大小**
这个参数指定了线程池中核心线程的数量。核心线程是一直存活的线程，即使它们处于空闲状态。线程池会保持这些核心线程的数量不变，除非你显式地修改它们。
**maximumPoolSize：最大线程池大小**
这个参数指定了线程池中允许存在的最大线程数量。当工作队列已满并且有新的任务提交时，线程池会创建新的线程，但数量不会超过这个最大值。
**keepAliveTime：线程空闲时间**
这个参数指定了非核心线程空闲时在终止之前等待新任务的最长时间。当线程池中的线程数量超过核心线程数时，多余的空闲线程会在达到指定的空闲时间后被终止。
**unit：时间单位**
这个参数指定了 keepAliveTime 的时间单位，通常是秒、毫秒、分钟等。
**workQueue：工作队列**
这个参数指定了用于保存等待执行的任务的队列。当所有核心线程都在忙碌时，新的任务会被放入这个队列中等待执行。
**threadFactory：线程工厂**
这个参数用于创建新线程的工厂。线程工厂负责创建线程，并可以指定线程的名称、优先级、是否为守护线程等属性。
**handler：拒绝执行处理器**
当线程池已经达到最大线程数，并且工作队列也已满时，新的任务将无法被执行。这时，拒绝执行处理器定义了当线程池无法执行任务时的处理策略。常见的处理策略包括抛出异常、丢弃任务、丢弃队列头部的任务等。



##### 实现



**自定义线程池**：

```java
@Configuration
public class ThreadPoolExecutorConfig {

    @Bean
    public ThreadPoolExecutor threadPoolExecutor() {
        ThreadFactory threadFactory = new ThreadFactory() {
            private int count = 1;

            @Override
            public Thread newThread(@NotNull Runnable r) {
                Thread thread = new Thread(r);
                thread.setName("线程" + count);
                count++;
                return thread;
            }
        };
        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(2, 4, 100, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(4), threadFactory);
        return threadPoolExecutor;
    }
}
```



**提交任务到线程池**：

```java
CompletableFuture.runAsync(() -> {
    System.out.println("任务执行中：" + name + "，执行人：" + Thread.currentThread().getName());
    try {
        Thread.sleep(60000);
    } catch (InterruptedException e) {
        e.printStackTrace();
    }
}, threadPoolExecutor);
```



**测试**：

```java
@RestController
@RequestMapping("/queue")
@Slf4j
public class QueueController {

    @Resource
    private ThreadPoolExecutor threadPoolExecutor;

    @GetMapping("/add")
    public void add(String name) {
        CompletableFuture.runAsync(() -> {
            log.info("任务执行中：" + name + "，执行人：" + Thread.currentThread().getName());
            try {
                Thread.sleep(600000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }, threadPoolExecutor);
    }

    @GetMapping("/get")
    public String get() {
        Map<String, Object> map = new HashMap<>();
        int size = threadPoolExecutor.getQueue().size();
        map.put("队列长度", size);
        long taskCount = threadPoolExecutor.getTaskCount();
        map.put("任务总数", taskCount);
        long completedTaskCount = threadPoolExecutor.getCompletedTaskCount();
        map.put("已完成任务数", completedTaskCount);
        int activeCount = threadPoolExecutor.getActiveCount();
        map.put("正在工作的线程数", activeCount);
        return JSONUtil.toJsonStr(map);
    }
}
```





#### 2、消息队列实现（RabbitMQ）



**原来**：
把任务提交到线程池，在线程池内排队。如果程序中断，任务就丢失了
**改造后**：

1. 将任务提交改为向队列发送消息
2. 写一个专门的接收消息的程序，处理任务
3. 如果程序中断，消息未被确认，还会重发
4. 消息全部集中发布到消息队列，可以部署多个后端，都从一个地方取任务，从而实现了分布式负载均衡

**线程池适用于需要多个线程处理任务**
**MQ更适用于服务通讯与应用解耦 **
**实现**：
1、引入依赖
注意，使用的版本一定要和你的 springboot 版本一致！！！！！！！

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-amqp</artifactId>
    <version>2.7.2</version>
</dependency>
```



2、在 yml 中引入配置：

```yaml
spring:
    rabbitmq:
        host: localhost
        port: 5672
        password: guest
        username: guest
```



3、创建交换机和队列

```java
/**
 * 用于创建测试程序用到的交换机和队列（只用在程序启动前执行一次）
 */
public class MqInitMain {

    public static void main(String[] args) {
        try {
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost("localhost");
            Connection connection = factory.newConnection();
            Channel channel = connection.createChannel();
            String EXCHANGE_NAME = "code_exchange";
            channel.exchangeDeclare(EXCHANGE_NAME, "direct");

            // 创建队列，随机分配一个队列名称
            String queueName = "code_queue";
            channel.queueDeclare(queueName, true, false, false, null);
            channel.queueBind(queueName, EXCHANGE_NAME, "my_routingKey");
        } catch (Exception e) {

        }

    }
}
```



4）生产者代码

```java
@Component
public class MyMessageProducer {

    @Resource
    private RabbitTemplate rabbitTemplate;

    public void sendMessage(String exchange, String routingKey, String message) {
        rabbitTemplate.convertAndSend(exchange, routingKey, message);
    }

}
```



5）消费者代码

```java
@Component
@Slf4j
public class MyMessageConsumer {

    // 指定程序监听的消息队列和确认机制
    @SneakyThrows
    @RabbitListener(queues = {"code_queue"}, ackMode = "MANUAL")
    public void receiveMessage(String message, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        log.info("receiveMessage message = {}", message);
        channel.basicAck(deliveryTag, false);
    }

}
```



6）单元测试执行

```java
@SpringBootTest
class MyMessageProducerTest {

    @Resource
    private MyMessageProducer myMessageProducer;

    @Test
    void sendMessage() {
        myMessageProducer.sendMessage("code_exchange", "my_routingKey", "你好呀");
    }
}
```
