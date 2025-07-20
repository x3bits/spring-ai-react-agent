package com.x3bits.springaireactagent.serializer.json.meta;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.ai.chat.messages.SystemMessage;

/**
 * SystemMessage的序列化Meta类
 */
public class SystemMessageMeta {

    @JsonProperty("content")
    private final String content;

    public SystemMessageMeta(@JsonProperty("content") String content) {
        this.content = content;
    }

    public static SystemMessageMeta fromSystemMessage(SystemMessage systemMessage) {
        return new SystemMessageMeta(systemMessage.getText());
    }

    public SystemMessage toSystemMessage() {
        return new SystemMessage(content);
    }

    public String getContent() {
        return content;
    }

}