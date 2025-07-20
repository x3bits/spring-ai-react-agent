package com.x3bits.springaireactagent.serializer.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.x3bits.springaireactagent.serializer.MessageSerializer;
import com.x3bits.springaireactagent.serializer.json.meta.AssistantMessageMeta;
import com.x3bits.springaireactagent.serializer.json.meta.SystemMessageMeta;
import com.x3bits.springaireactagent.serializer.json.meta.ToolResponseMessageMeta;
import com.x3bits.springaireactagent.serializer.json.meta.UserMessageMeta;
import org.springframework.ai.chat.messages.*;

public class JsonMessageSerializer implements MessageSerializer {

    private final ObjectMapper objectMapper;

    public JsonMessageSerializer() {
        this.objectMapper = new ObjectMapper();
    }

    public JsonMessageSerializer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public String serialize(Message message) {
        try {
            return switch (message.getMessageType()) {
                case USER -> objectMapper.writeValueAsString(
                        UserMessageMeta.fromUserMessage((UserMessage) message));
                case ASSISTANT -> objectMapper.writeValueAsString(
                        AssistantMessageMeta.fromAssistantMessage((AssistantMessage) message));
                case SYSTEM -> objectMapper.writeValueAsString(
                        SystemMessageMeta.fromSystemMessage((SystemMessage) message));
                case TOOL -> objectMapper.writeValueAsString(
                        ToolResponseMessageMeta.fromToolResponseMessage((ToolResponseMessage) message));
                default -> throw new IllegalArgumentException("Unsupported message type: " + message.getMessageType());
            };
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize message: " + e.getMessage(), e);
        }
    }

    @Override
    public Message deserialize(MessageType messageType, String str) {
        try {
            return switch (messageType) {
                case USER -> {
                    UserMessageMeta meta = objectMapper.readValue(str, UserMessageMeta.class);
                    yield meta.toUserMessage();
                }
                case ASSISTANT -> {
                    AssistantMessageMeta meta = objectMapper.readValue(str, AssistantMessageMeta.class);
                    yield meta.toAssistantMessage();
                }
                case SYSTEM -> {
                    SystemMessageMeta meta = objectMapper.readValue(str, SystemMessageMeta.class);
                    yield meta.toSystemMessage();
                }
                case TOOL -> {
                    ToolResponseMessageMeta meta = objectMapper.readValue(str, ToolResponseMessageMeta.class);
                    yield meta.toToolResponseMessage();
                }
                default -> throw new IllegalArgumentException("Unsupported message type: " + messageType);
            };
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to deserialize message: " + e.getMessage(), e);
        }
    }
}
