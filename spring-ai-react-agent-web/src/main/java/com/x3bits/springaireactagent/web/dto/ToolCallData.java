package com.x3bits.springaireactagent.web.dto;

public record ToolCallData(
        String type,
        String id,
        String name,
        Object args) {
    public static ToolCallData of(String id, String name, Object args) {
        return new ToolCallData("toolCall", id, name, args);
    }
}