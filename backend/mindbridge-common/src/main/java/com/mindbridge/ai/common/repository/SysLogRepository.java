package com.mindbridge.ai.common.repository;

import com.mindbridge.ai.common.entity.SysLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SysLogRepository extends JpaRepository<SysLog, String> {
}
