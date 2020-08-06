package com.fy.cv.threadpool;

import com.fy.cv.service.BizConvertTaskService;
import com.fy.cv.task.ConvertTask;
import com.fy.cv.utils.SpringContextUtil;
import org.springframework.beans.BeanUtils;

import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * @author 方焱
 */
public class RejectedHandler implements RejectedExecutionHandler {

    @Override
    public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
        doLog(r, e);
    }

    private void doLog(Runnable r, ThreadPoolExecutor e) {
        if (r instanceof ConvertTask) {
            BizConvertTask bizConvertTask = new BizConvertTask();
            BeanUtils.copyProperties(r, bizConvertTask);
            bizConvertTask.setErrMsg("线程池拒绝");
            bizConvertTask.setConvertStatus("TYPE_ERROR");
            BizConvertTaskService bizConvertTaskService = SpringContextUtil.getBean(BizConvertTaskService.class);
            bizConvertTaskService.update(bizConvertTask);
        } else {
            System.err.println(r.toString() + " rejected");
        }
    }
}
