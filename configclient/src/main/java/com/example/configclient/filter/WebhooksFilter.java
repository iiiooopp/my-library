package com.example.configclient.filter;

import com.example.configclient.wrapper.CustomerRequestWrapper;
import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class WebhooksFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;
        HttpServletResponse httpServletResponse = (HttpServletResponse) servletResponse;

        String url = new String(httpServletRequest.getRequestURI());

        //只过滤/actuator/refresh
        if (!url.endsWith("/actuator/refresh")) {
            filterChain.doFilter(servletRequest, httpServletResponse);
            return;
        }

        //使用HttpServletRequest包装原始请求达到修改post请求中body内容的目的
        CustomerRequestWrapper requestWrapper = new CustomerRequestWrapper(httpServletRequest);

        filterChain.doFilter(requestWrapper, servletResponse);
    }

    @Override
    public void destroy() {

    }
}
