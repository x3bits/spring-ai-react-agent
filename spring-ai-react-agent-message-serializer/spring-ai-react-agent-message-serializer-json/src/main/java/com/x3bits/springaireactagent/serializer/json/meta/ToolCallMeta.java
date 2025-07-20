package com.x3bits.springaireactagent.serializer.json.meta;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.ai.chat.messages.AssistantMessage;

/**
 * ToolCall的序列化Meta类
 */
public class ToolCallMeta {

    @JsonProperty("id")
    private final String id;

    @JsonProperty("type")
    private final String type;

    @JsonProperty("name")
    private final String name;

    @JsonProperty("arguments")
    private final String arguments;

    public ToolCallMeta(@JsonProperty("id") String id,
            @JsonProperty("type") String type,
            @JsonProperty("name") String name,
            @JsonProperty("arguments") String arguments) {
        this.id = id;
        this.type = type;
        this.name = name;
        this.arguments = arguments;
    }

    public static ToolCallMeta fromToolCall(AssistantMessage.ToolCall toolCall) {
        return new ToolCallMeta(
                toolCall.id(),
                toolCall.type(),
                toolCall.name(),
                toolCall.arguments());
    }

    public AssistantMessage.ToolCall toToolCall() {
        return new AssistantMessage.ToolCall(id, type, name, arguments);
    }

    public String getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public String getArguments() {
        return arguments;
    }
}