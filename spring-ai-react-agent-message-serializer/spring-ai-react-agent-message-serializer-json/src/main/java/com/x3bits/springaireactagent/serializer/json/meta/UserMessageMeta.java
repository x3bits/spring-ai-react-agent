package com.x3bits.springaireactagent.serializer.json.meta;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.ai.chat.messages.UserMessage;

public class UserMessageMeta {

    @JsonProperty("text")
    private final String text;

    public static UserMessageMeta fromUserMessage(UserMessage userMessage) {
        return new UserMessageMeta(userMessage.getText());
    }

    public UserMessage toUserMessage() {
        return UserMessage.builder().text(text).build();
    }

    public UserMessageMeta(@JsonProperty("text") String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }

}
