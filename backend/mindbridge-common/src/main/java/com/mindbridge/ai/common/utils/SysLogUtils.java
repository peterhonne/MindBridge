package com.mindbridge.ai.common.utils;


import com.mindbridge.ai.common.entity.SysLog;
import jakarta.servlet.http.HttpServletRequest;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Arrays;
import java.util.Objects;


@Slf4j
@UtilityClass
public class SysLogUtils {


    public SysLog getSysLog(Object[] args) {
        HttpServletRequest request = ((ServletRequestAttributes) Objects
                .requireNonNull(RequestContextHolder.getRequestAttributes())).getRequest();
        SysLog sysLog = new SysLog();
        sysLog.setThreadId(Thread.currentThread().getName());

        sysLog.setRemoteAddr(WebUtils.getIP(request));
        sysLog.setRequestUri(request.getRequestURI());
        sysLog.setMethod(request.getMethod());
        sysLog.setUserAgent(request.getHeader("user-agent"));
        sysLog.setParams(Arrays.toString(args));

        return sysLog;
    }



}
