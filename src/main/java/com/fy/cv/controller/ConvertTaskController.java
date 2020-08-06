package com.fy.cv.controller;


import com.fy.cv.apicommon.ApiResponse;
import com.fy.cv.service.BizConvertTaskService;
import com.fy.cv.task.ConvertTask;
import com.fy.cv.task.VideoConvertScheduleTask;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author 方焱
 */
@RestController
@RequestMapping("/converttask")
public class ConvertTaskController {

    @Autowired
    BizConvertTaskService bizConvertTaskService;

    @GetMapping("/index")
    public ModelAndView index() {
        ModelAndView mv = new ModelAndView("index");
        return mv;
    }

    @GetMapping("/queuetask")
    public ModelAndView queuetask() {
        ModelAndView mv = new ModelAndView("queueTask");
        return mv;
    }

    @GetMapping("/conventtask")
    public ModelAndView conventtask() {
        ModelAndView mv = new ModelAndView("conventTask");
        return mv;
    }

    @GetMapping("/threadpool")
    public ModelAndView threadpool() {
        ModelAndView mv = new ModelAndView("threadPool");
        return mv;
    }

    @PostMapping("/threadpoolinfo")
    public ApiResponse threadpoolinfo() {
        Map result = new LinkedHashMap();
        //当前排队任务（线程）数
        result.put("QueueSize", VideoConvertScheduleTask.executor.getQueue().size());
        //当前活动任务（线程）数
        result.put("ActiveCount", VideoConvertScheduleTask.executor.getActiveCount());
        //当前执行完成任务（线程）数
        result.put("CompletedTaskCount", VideoConvertScheduleTask.executor.getCompletedTaskCount());
        //当前线程（排队线程数 + 活动线程数 +  执行完成线程数）数
        result.put("TaskCount", VideoConvertScheduleTask.executor.getTaskCount());
        //曾经创建过的最大任务（线程）数
        result.put("LargestPoolSize", VideoConvertScheduleTask.executor.getLargestPoolSize());
        //线程池是否关闭
        result.put("isShutdown", VideoConvertScheduleTask.executor.isShutdown());
        //线程池是否终止
        result.put("isTerminated", VideoConvertScheduleTask.executor.isTerminated());
        return ApiResponse.success(result);
    }

    @PostMapping("/conventtaskinfo")
    public ApiResponse threadinfo(@RequestParam(defaultValue = "1") Integer currentPage, @RequestParam(defaultValue = "10") Integer pageSize) {
        Map res = new LinkedHashMap();
        List list = new ArrayList<>(VideoConvertScheduleTask.executor.getWorkers());
        //起始下标
        int fromIndex = pageSize * (currentPage - 1);
        //终止下标
        int toIndex = fromIndex + pageSize;
        if (toIndex >= list.size()) {
            toIndex = list.size();
        }
        res.put("conventTaskInfo", list.subList(fromIndex, toIndex));
        res.put("total", list.size());
        return ApiResponse.success(res);
    }

    @PostMapping("/queueinfo")
    public ApiResponse queueinfo(@RequestParam(defaultValue = "1") Integer currentPage, @RequestParam(defaultValue = "10") Integer pageSize) {
        Map res = new LinkedHashMap();
        List list = new ArrayList<>(VideoConvertScheduleTask.executor.getQueue());
        //起始下标
        int fromIndex = pageSize * (currentPage - 1);
        //终止下标
        int toIndex = fromIndex + pageSize;
        if (toIndex >= list.size()) {
            toIndex = list.size();
        }
        res.put("queueInfo", list.subList(fromIndex, toIndex));
        res.put("total", list.size());
        return ApiResponse.success(res);
    }

    @GetMapping("/conventtaskinfo/{taskId}")
    public ApiResponse threadinfo(@PathVariable String taskId) {
        List<Runnable> collect = VideoConvertScheduleTask.executor.getWorkers().parallelStream().filter(task -> {
            if (task instanceof ConvertTask) {
                ConvertTask convertTask = (ConvertTask) task;
                if (taskId.equals(convertTask.getTaskJobId())) {
                    return true;
                }
            }
            return false;
        }).collect(Collectors.toList());
        if (!collect.isEmpty()) {
            return ApiResponse.success(collect.get(0));
        } else {
            return ApiResponse.fail("未找到任务id为：" + taskId + "的任务！");
        }
    }
}
