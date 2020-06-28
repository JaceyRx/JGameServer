package com.jacey.game.gm.config;

import com.jacey.game.gm.filter.RequestInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * @Description: 拦截器配置
 * @Author: JaceyRuan
 * @Email: jacey.ruan@outlook.com
 */
@Configuration
public class FilterConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        //注册TestInterceptor拦截器
        InterceptorRegistration registration = registry.addInterceptor(new RequestInterceptor());
        registration.addPathPatterns("/gm/**");                      //所有路径都被拦截
        //添加不拦截路径
        registration.excludePathPatterns("/gateway", "/gm/gmUserLogin");
    }
}
