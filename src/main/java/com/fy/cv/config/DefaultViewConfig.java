package com.fy.cv.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * @author 方焱
 */
@Configuration
public class DefaultViewConfig implements WebMvcConfigurer {
    /**
     * @Description 配置默认首页跳转，在浏览器输入ip+端口后（如果有项目名称则带上）自动跳转(如果是跳转静态页面，则需要把静态页面放到static下面，springboot默认static中放静态页面，而templates中放动态页面)
     * @Return
     **/
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/").setViewName("forward:/converttask/index");
        registry.setOrder(Ordered.HIGHEST_PRECEDENCE);
    }
}
