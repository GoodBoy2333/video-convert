package com.fy.cv.threadpool;

import com.fy.cv.config.ConvertConfig;
import com.fy.cv.service.BizConvertTaskService;
import com.fy.cv.task.ConvertTask;
import com.fy.cv.utils.SpringContextUtil;
import org.springframework.beans.BeanUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Date;
import java.util.TreeSet;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author 方焱
 */
public class MonitorThreadPoolExecutor extends ThreadPoolExecutor {

    // 核心线程数
    private static int corePoolSize = ConvertConfig.corePoolSize;

    //最大线程数
    private static int maximumPoolSize = ConvertConfig.corePoolSize;

    private static long keepAliveTime = 5;

    private static TimeUnit unit = TimeUnit.SECONDS;

    // 无界优先级队列
    private static BlockingQueue<Runnable> workQueue = new PriorityBlockingQueue<>();

    // 自定义线程工厂 方便调错
    private static ThreadFactory threadFactory = new NameTreadFactory();

    // 拒绝策略
    private static RejectedExecutionHandler handler = new RejectedHandler();

    private final TreeSet<Runnable> workers = new TreeSet<>();

    public MonitorThreadPoolExecutor() {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit,
                workQueue, threadFactory, handler);
    }

    private final ThreadLocal<Long> startTime = new ThreadLocal<>();

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
                ConvertTask task = (ConvertTask) r;
                BizConvertTask bizConvertTask = new BizConvertTask();
                BeanUtils.copyProperties(task, bizConvertTask);
                bizConvertTask.setBeginTime(new Date(startTime.get()));
                bizConvertTask.setEndTime(new Date());

                // 判断是否正常结束
                if (task.isCompleted() && task.isTransferCompleted()) {
                    bizConvertTask.setErrMsg("");
                    bizConvertTask.setConvertStatus("TYPE_SUCCESS");
                } else {
                    // 转换过程已经设置错误消息
                    bizConvertTask.setConvertStatus("TYPE_FAIL");
                }
                // 删除转码后的源文件
                Files.deleteIfExists(Paths.get(task.getTargetPath()));
                BizConvertTaskService bizConvertTaskService = SpringContextUtil.getBean(BizConvertTaskService.class);
                bizConvertTaskService.update(bizConvertTask);
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
