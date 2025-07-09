package com.mindbridge.ai.common.entity;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;


@Entity
@Table(name = "sys_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SysLog {

	@Id
	private String id;

	private String threadId;

//	private String logType;

	private String title;

	// creator ID
	private String createId;

	// creator name
	private String createBy;

	private LocalDateTime createTime;

	@Transient
	private Long startTime;

	private String remoteAddr;

	private String userAgent;

	private String requestUri;

	private String method;

	private String params;

	private Long duration;

	@Column(columnDefinition = "TEXT")
	private String exception;

	private boolean deleted = false;

	public void setStartTime(Long startTime){
		this.startTime = startTime;
		this.createTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(startTime), ZoneId.systemDefault());
	}

	@PrePersist
	public void prePersist() {
		if (this.id == null) {
			this.id = UUID.randomUUID().toString();
		}
	}


}
