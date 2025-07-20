package com.x3bits.springaireactagent.core.event;

import org.springframework.ai.chat.messages.Message;

public record LlmMessageEvent (Message message, String id) implements ReActAgentEvent {
}
