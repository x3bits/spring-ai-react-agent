package com.x3bits.springaireactagent.core.prompt;

import java.util.Map;

public interface SystemPromptProvider {
    String getSystemPrompt(Map<String, Object> context);
}
