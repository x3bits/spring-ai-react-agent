package com.x3bits.springaireactagent.web.dto;

public record TextContentData(
        String type,
        String content) {
    public static TextContentData of(String content) {
        return new TextContentData("text", content);
    }
}