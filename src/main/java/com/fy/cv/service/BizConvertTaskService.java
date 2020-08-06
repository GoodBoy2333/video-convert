package com.fy.cv.service;


import org.springframework.data.domain.Example;

/**
 * @author 方焱
 */
public interface BizConvertTaskService {
    int update(BizConvertTask task);
    List<BizConvertTask> findByExample(Example example);
}
