package com.x3bits.springaireactagent.web.dto;

/**
 * 聊天请求
 */
public record ChatRequest(
        String threadId,
        String userMessage,
        String checkpointId,
        String agentBeanName) {
}