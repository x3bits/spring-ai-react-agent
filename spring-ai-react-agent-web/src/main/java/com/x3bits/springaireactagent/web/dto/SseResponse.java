package com.x3bits.springaireactagent.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record SseResponse(
        String id,
        String type,
        String text,
        String callId,
        String content,
        Object data) {

    public static SseResponse userEventId(String id) {
        return new SseResponse(id, "userEventId", null, null, null, null);
    }

    public static SseResponse assistantStart(String id) {
        return new SseResponse(id, "assistantStart", null, null, null, null);
    }

    public static SseResponse assistantContent(Object data) {
        return new SseResponse(null, "assistantContent", null, null, null, data);
    }

    public static SseResponse toolResult(String id, String callId, String content) {
        return new SseResponse(id, "toolResult", null, callId, content, null);
    }

    public static SseResponse assistantPartialText(String text) {
        return new SseResponse(null, "assistantPartialText", text, null, null, null);
    }
}