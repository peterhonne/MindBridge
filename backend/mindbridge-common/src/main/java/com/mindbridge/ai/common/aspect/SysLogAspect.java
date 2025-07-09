package com.mindbridge.ai.common.aspect;


import com.mindbridge.ai.common.entity.SysLog;
import com.mindbridge.ai.common.event.SysLogEvent;
import com.mindbridge.ai.common.utils.SecurityUtils;
import com.mindbridge.ai.common.utils.SysLogUtils;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
public class SysLogAspect {
    private final ApplicationEventPublisher publisher;
    private final ThreadLocal<SysLog> CONTEXT_HOLDER = new ThreadLocal<>();

    public SysLogAspect(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    @Pointcut("@annotation(com.mindbridge.ai.common.annotation.SysLog)")
    public void init() {
    }

    @SneakyThrows
    @Around(value = "init() && @annotation(sysLog)")
    public Object around(ProceedingJoinPoint point, com.mindbridge.ai.common.annotation.SysLog sysLog) {
        String strClassName = point.getTarget().getClass().getName();
        String strMethodName = point.getSignature().getName();
        log.debug("[class]:{},[method]:{}", strClassName, strMethodName);
        SysLog logVo = SysLogUtils.getSysLog(point.getArgs());
        logVo.setTitle(sysLog.value());

        Long startTime = System.currentTimeMillis();
        logVo.setStartTime(startTime);
        CONTEXT_HOLDER.set(logVo);
        Object obj = point.proceed();
        logVo.setCreateBy(SecurityUtils.getUsername());
        logVo.setCreateId(SecurityUtils.getUserId());
        publishEvent(logVo);
        return obj;
    }

    @AfterThrowing(pointcut = "init()", throwing = "e")
    public void afterThrowing(Exception e) {
        SysLog logVo = CONTEXT_HOLDER.get();
        logVo.setException(e.getMessage());
        publishEvent(logVo);
    }

    private void publishEvent(SysLog logVo) {
        Long endTime = System.currentTimeMillis();
        logVo.setDuration(endTime - logVo.getStartTime());
        publisher.publishEvent(new SysLogEvent(logVo));
        CONTEXT_HOLDER.remove();
    }



}
