# 视频转码服务

## 技术要点

1. ffmpeg

2. 线程池

3. 线程本地变量

## 主要实现

- 线程池

  ```
  // 继承ThreadPoolExecutor重写线程池代码
  public class MonitorThreadPoolExecutor extends ThreadPoolExecutor {
      // 核心线程数
      private static int corePoolSize = ConvertConfig.corePoolSize;
      // 最大线程数
      private static int maximumPoolSize = ConvertConfig.corePoolSize;
      // 空闲时间
      private static long keepAliveTime = 5;
      private static TimeUnit unit = TimeUnit.SECONDS;
      // 无界优先级队列
      private static BlockingQueue<Runnable> workQueue = new PriorityBlockingQueue<>();
      // 自定义线程工厂 方便调错
      private static ThreadFactory threadFactory = new NameTreadFactory();
      // 拒绝策略
      private static RejectedExecutionHandler handler = new RejectedHandler();
      // 工作队列
      private final TreeSet<Runnable> workers = new TreeSet<>();
  
      public MonitorThreadPoolExecutor() {
          super(corePoolSize, maximumPoolSize, keepAliveTime, unit,
                  workQueue, threadFactory, handler);
      }
  	// 记录开始时间
      private final ThreadLocal<Long> startTime = new ThreadLocal<>();
  	// 可重入锁
      private ReentrantLock lock = new ReentrantLock();
  
      @Override
      protected void beforeExecute(Thread t, Runnable r) {
          try {
              lock.lock();
              startTime.set(System.currentTimeMillis());
              workers.add(r);
          } catch (Exception e) {
              workers.remove(r);
              e.printStackTrace();
          } finally {
              lock.unlock();
          }
      }
  
      @Override
      protected void afterExecute(Runnable r, Throwable t) {
          try {
              lock.lock();
              if (r instanceof ConvertTask) {
                  // 逻辑代码处理
              }
              workers.remove(r);
              startTime.remove();
          } catch (IOException e) {
              e.printStackTrace();
          } finally {
              lock.unlock();
          }
      }
      
      @Override
      protected void terminated() {
          try {
              System.out.println("线程池关闭");
          } finally {
              super.terminated();
          }
      }
      
      public TreeSet<Runnable> getWorkers() {
          return workers;
      }
  }
  
  ```

- 线程池

  ```
  // 实现Runnable接口，SftpProgressMonitor文件传输，Comparable优先级
  public class ConvertTask implements Runnable, SftpProgressMonitor, Comparable<ConvertTask> {
      @Override
      public void run() {
          try {
          	// 启动任务转码
              isCompleted = process();
              if (isCompleted) {
              	// 传输文件
                  transferFile();
              }
          } catch (Exception e) {
              setErrMsg(e.getMessage());
              return;
          }
      }
  }
  ```

- 定时任务
  ```
  @Configuration
  public class VideoConvertScheduleTask {
  
      @Autowired
      public BizConvertTaskService bizConvertTaskService;
  
      @Autowired
      private RedissonClient redissonClient;
  
      /**
       * 锁实例名
       */
      private String convertLock = ConvertConfig.convertLock;
  
      public static MonitorThreadPoolExecutor executor = new MonitorThreadPoolExecutor();
  
      @Scheduled(fixedRate = 10000)
      private void configureTasks() {
          //获取锁实例
          RLock rLock = redissonClient.getLock(convertLock);
          boolean isLock = false;
          try {
              // 上锁
              isLock = rLock.tryLock();
              if (isLock) {
              	// 启动转码任务
                  execTask();
              }
          } catch (Exception e) {
              e.printStackTrace();
          } finally {
              if (isLock) {
                  // 解锁
                  rLock.unlock();
              }
          }
      }
  
      @Bean
      public TaskScheduler scheduledExecutorService() {
          ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
          scheduler.setPoolSize(8);
          scheduler.setThreadNamePrefix("scheduled-thread-");
          return scheduler;
      }
  }
  
  ```