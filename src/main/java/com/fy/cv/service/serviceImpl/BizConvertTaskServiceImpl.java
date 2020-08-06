package com.fy.cv.service.serviceImpl;


import com.fy.cv.service.BizConvertTaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Example;
import org.springframework.stereotype.Service;

/**
 * @author 方焱
 */
@Service
public class BizConvertTaskServiceImpl implements BizConvertTaskService {

    @Autowired
    BizConvertTaskMapper bizConvertTaskMapper;

    @Override
    public int update(BizConvertTask task) {
        return bizConvertTaskMapper.updateByBizCode(task,task.getBizCode());
    }

    @Override
    public List<BizConvertTask> findByExample(Example example){
        return bizConvertTaskMapper.selectByExample(example);
    }
}
