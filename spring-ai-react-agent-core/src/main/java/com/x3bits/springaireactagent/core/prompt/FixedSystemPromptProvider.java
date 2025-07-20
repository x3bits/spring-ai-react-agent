package com.x3bits.springaireactagent.core.prompt;

import java.util.Map;

public class FixedSystemPromptProvider implements SystemPromptProvider {

    private final String systemPrompt;

    public FixedSystemPromptProvider(String systemPrompt) {
        this.systemPrompt = systemPrompt;
    }

    @Override
    public String getSystemPrompt(Map<String, Object> context) {
        return systemPrompt;
    }
}
