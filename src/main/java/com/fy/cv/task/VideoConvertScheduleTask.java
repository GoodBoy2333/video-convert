package com.fy.cv.task;

import com.fy.cv.utils.convertUtil;
import com.fy.cv.config.ConvertConfig;
import com.fy.cv.service.BizConvertTaskService;
import com.fy.cv.threadpool.MonitorThreadPoolExecutor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import tk.mybatis.mapper.entity.Example;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.RejectedExecutionException;
import java.util.stream.Collectors;

/**
 * 定时任务
 *
 * @author 方焱
 */
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

    public void execTask() {
        Example example = new Example(BizConvertTask.class);
        Example.Criteria criteria = example.createCriteria();
        criteria.andEqualTo("convertStatus", "TYPE_CONVERT");
        criteria.andEqualTo("isDelete", 0);
        List<BizConvertTask> convertTasks = bizConvertTaskService.findByExample(example);
        if (convertTasks.isEmpty()) {
            return;
        }

        List<BizConvertTask> failList = new LinkedList<>();
        List<BizConvertTask> successList = convertTasks.stream().filter(task -> {
            try {
                Map map = convertUtil.checkConvertTask(task);
                Integer checkStatus = (Integer) map.get("code");
                if (0 == checkStatus) {
                    String msg = (String) map.getOrDefault("msg", "参数验证失败");
                    task.setErrMsg(msg);
                    failList.add(task);
                    return false;
                } else if (1 == checkStatus) {
                    // 当前队列容量超出设置的值
                    if (executor.getQueue().size() >= ConvertConfig.queueSize) {
                        // 设置待转码状态
                        task.setConvertStatus("TYPE_CONVERT");
                        // 日志记录
                        task.setErrMsg("当前队列容量超出，拒绝执行。请等候。");
                        bizConvertTaskService.update(task);
                        return false;
                    }
                    int update = bizConvertTaskService.update(task);
                    if (update < 1) {
                        task.setErrMsg("数据库操作失败");
                        failList.add(task);
                        return false;
                    }
                    ConvertTask convertTask = new ConvertTask();
                    BeanUtils.copyProperties(task, convertTask);
                    executor.execute(convertTask);
                    return true;
                }
            } catch (RejectedExecutionException e) {
                task.setErrMsg("线程池拒绝");
                failList.add(task);
                return false;
            } catch (Exception e) {
                e.printStackTrace();
                task.setErrMsg("系统异常，请联系管理员。");
                failList.add(task);
                return false;
            }
            return false;
        }).collect(Collectors.toList());

        // 失败列表
        failList.parallelStream().forEach(task -> {
            task.setConvertStatus("TYPE_ERROR");
            bizConvertTaskService.update(task);
        });

        // 成功列表
        successList.parallelStream().forEach(task -> {
            task.setConvertStatus("TYPE_PROGRESS");
            bizConvertTaskService.update(task);
        });
    }

    @Bean
    public TaskScheduler scheduledExecutorService() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(8);
        scheduler.setThreadNamePrefix("scheduled-thread-");
        return scheduler;
    }
}
