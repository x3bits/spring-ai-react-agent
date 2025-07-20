package com.x3bits.springaireactagent.web.entity;

import java.time.LocalDateTime;

/**
 * Thread实体类，对应数据库中的threads表
 */
public class Thread {

    private Long id;
    private String userId;
    private String threadId;
    private String title;
    private String agent;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 默认构造函数
    public Thread() {
    }

    // 带参构造函数
    public Thread(String userId, String threadId, String title, String agent) {
        this.userId = userId;
        this.threadId = threadId;
        this.title = title;
        this.agent = agent;
    }

    // Getter和Setter方法
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getThreadId() {
        return threadId;
    }

    public void setThreadId(String threadId) {
        this.threadId = threadId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAgent() {
        return agent;
    }

    public void setAgent(String agent) {
        this.agent = agent;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return "Thread{" +
                "id=" + id +
                ", userId='" + userId + '\'' +
                ", threadId='" + threadId + '\'' +
                ", title='" + title + '\'' +
                ", agent='" + agent + '\'' +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}