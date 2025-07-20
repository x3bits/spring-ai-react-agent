package com.x3bits.springaireactagent.serializer.json.meta;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.ai.chat.messages.AssistantMessage;

import java.util.List;
import java.util.Map;

/**
 * AssistantMessage的序列化Meta类
 */
public class AssistantMessageMeta {

    @JsonProperty("content")
    private final String content;

    @JsonProperty("toolCalls")
    private final List<ToolCallMeta> toolCalls;

    public AssistantMessageMeta(@JsonProperty("content") String content,
            @JsonProperty("toolCalls") List<ToolCallMeta> toolCalls) {
        this.content = content;
        this.toolCalls = toolCalls;
    }

    public static AssistantMessageMeta fromAssistantMessage(AssistantMessage assistantMessage) {
        List<ToolCallMeta> toolCallMetas = null;
        if (assistantMessage.hasToolCalls()) {
            toolCallMetas = assistantMessage.getToolCalls().stream()
                    .map(ToolCallMeta::fromToolCall)
                    .toList();
        }
        return new AssistantMessageMeta(assistantMessage.getText(), toolCallMetas);
    }

    public AssistantMessage toAssistantMessage() {
        List<AssistantMessage.ToolCall> toolCallsList = null;
        if (toolCalls != null) {
            toolCallsList = toolCalls.stream()
                    .map(ToolCallMeta::toToolCall)
                    .toList();
        }
        return new AssistantMessage(content, Map.of(), toolCallsList == null ? List.of() : toolCallsList);
    }

    public String getContent() {
        return content;
    }

    public List<ToolCallMeta> getToolCalls() {
        return toolCalls;
    }

}