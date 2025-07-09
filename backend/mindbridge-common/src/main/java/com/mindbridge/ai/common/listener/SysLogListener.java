package com.mindbridge.ai.common.listener;


import com.mindbridge.ai.common.entity.SysLog;
import com.mindbridge.ai.common.event.SysLogEvent;
import com.mindbridge.ai.common.repository.SysLogRepository;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Component;


@Slf4j
@Component
@EnableAsync
@RequiredArgsConstructor
public class SysLogListener {

    private final SysLogRepository sysLogRepository;

    @Async
    @Order
    @EventListener(SysLogEvent.class)
    public void saveSysLog(SysLogEvent event) {
        SysLog sysLog = event.sysLog();
        log.debug("SysLog: {}-{}",Thread.currentThread().getName(), sysLog);
        sysLogRepository.save(sysLog);
    }
}
