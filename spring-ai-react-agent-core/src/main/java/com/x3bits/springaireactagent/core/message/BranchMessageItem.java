package com.x3bits.springaireactagent.core.message;

import java.util.Map;

import org.springframework.ai.chat.messages.Message;

public record BranchMessageItem(Message message, String id, String previousId, Map<String, Object> metadata) {
}